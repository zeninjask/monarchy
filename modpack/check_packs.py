#!/usr/bin/env python3
"""Check the client and server packs without starting Minecraft.

For each side it gathers the mods that side installs (the index entries not marked unsupported for
it, modpack/mods/*.jar and the Binder jar), reads every mods.toml (jar-in-jar included) and reports:

  - a mandatory dependency for that side that no mod provides, or provides at a version outside
    the required range (what Forge refuses to load with "Mod X requires Y");
  - two jars providing the same mod id;
  - a mod both packs install at different versions (clients could not join the server);
  - an index file whose download is missing locally or does not match its sha512.

Jars come from mdvlcraft-binder/libs/modpack (run mdvlcraft-binder/tools/fetch_mods.py first);
anything missing there is downloaded.

Usage:
  python3 modpack/check_packs.py          exit status 1 if anything is wrong
"""
import hashlib
import io
import json
import re
import sys
import tomllib
import urllib.request
import zipfile
from pathlib import Path

PACK = Path(__file__).resolve().parent
ROOT = PACK.parent
CACHE = ROOT / "mdvlcraft-binder" / "libs" / "modpack"
BINDER_LIBS = ROOT / "mdvlcraft-binder" / "build" / "libs"
PROVIDED = {"minecraft": "1.20.1", "forge": "47.4.10", "javafml": "47", "lowcodefml": "47"}


def version_key(version: str):
    parts = re.split(r"[.\-+_]", version.strip().lower())
    key = []
    for part in parts:
        for piece in re.findall(r"\d+|[a-z]+", part):
            key.append((0, int(piece)) if piece.isdigit() else (-1, piece))
    while key and key[-1] == (0, 0):
        key.pop()
    return key


def in_range(version: str, spec: str) -> bool:
    """Maven version range as used in mods.toml: "*", "[1,2)", "[1,)", "1.0" (= at least 1.0), "[1.0]"."""
    spec = spec.strip()
    if not spec or spec == "*" or not version:
        return True
    for single in re.findall(r"[\[(][^\])]*[\])]", spec) or [spec]:
        if not single.startswith(("[", "(")):
            if version_key(version) >= version_key(single):
                return True
            continue
        low, _, high = single[1:-1].partition(",")
        if "," not in single:
            high = low
        v = version_key(version)
        ok = True
        if low.strip():
            ok &= v >= version_key(low) if single[0] == "[" else v > version_key(low)
        if high.strip():
            ok &= v <= version_key(high) if single[-1] == "]" else v < version_key(high)
        if ok:
            return True
    return False


def manifest_version(archive: zipfile.ZipFile) -> str:
    try:
        manifest = archive.read("META-INF/MANIFEST.MF").decode("utf-8", "replace")
    except KeyError:
        return ""
    match = re.search(r"^Implementation-Version:\s*(\S+)", manifest, re.M)
    return match.group(1) if match else ""


def describe(data: bytes, name: str):
    """Mod ids provided (id -> version) and mandatory deps [(owner, id, range, side)] of a jar."""
    provided, deps = {}, []

    def visit(archive: zipfile.ZipFile, top: bool):
        try:
            toml = tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8"))
        except (KeyError, tomllib.TOMLDecodeError, UnicodeDecodeError):
            toml = None
        if toml:
            for mod in toml.get("mods", []):
                version = str(mod.get("version", ""))
                if "${" in version:
                    version = manifest_version(archive)
                provided.setdefault(mod["modId"], version)
            for owner, entries in toml.get("dependencies", {}).items():
                for dep in entries:
                    mandatory = dep.get("mandatory", dep.get("type", "required") == "required")
                    if mandatory:
                        deps.append((owner, dep["modId"], str(dep.get("versionRange", "*")), str(dep.get("side", "BOTH")).upper()))
        for inner in archive.namelist():
            if inner.startswith("META-INF/jarjar/") and inner.endswith(".jar"):
                with zipfile.ZipFile(io.BytesIO(archive.read(inner))) as nested:
                    visit(nested, False)

    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        visit(archive, True)
    return provided, deps


def load_index_jar(entry: dict, problems: list) -> bytes | None:
    target = CACHE / entry["path"].split("/", 1)[1]
    expected = entry["hashes"]["sha512"]
    if not target.exists() or hashlib.sha512(target.read_bytes()).hexdigest() != expected:
        try:
            with urllib.request.urlopen(entry["downloads"][0], timeout=120) as response:
                data = response.read()
        except OSError as error:
            problems.append(f"cannot download {entry['path']}: {error}")
            return None
        if hashlib.sha512(data).hexdigest() != expected:
            problems.append(f"{entry['path']}: sha512 does not match the index")
            return None
        CACHE.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    return target.read_bytes()


def jars_for(side: str, problems: list) -> dict[str, bytes]:
    jars = {}
    index = json.loads((PACK / f"{side}.modrinth.index.json").read_text())
    for entry in index["files"]:
        if not entry["path"].startswith("mods/") or entry.get("env", {}).get(side) == "unsupported":
            continue
        data = load_index_jar(entry, problems)
        if data is not None:
            jars[entry["path"].split("/", 1)[1]] = data
    for jar in sorted((PACK / "mods").glob("*.jar")):
        jars[jar.name] = jar.read_bytes()
    binder = sorted(j for j in BINDER_LIBS.glob("mdvlcraft-*.jar") if not j.name.endswith(("-sources.jar", "-slim.jar")))
    if binder:
        jars[binder[-1].name] = binder[-1].read_bytes()
    else:
        problems.append("no Binder jar in mdvlcraft-binder/build/libs (run ./gradlew build)")
    return jars


def check_side(side: str, jars: dict[str, bytes], problems: list) -> dict[str, tuple[str, str]]:
    providers: dict[str, tuple[str, str]] = {}
    all_deps = []
    for name, data in jars.items():
        try:
            provided, deps = describe(data, name)
        except zipfile.BadZipFile:
            problems.append(f"[{side}] {name} is not a valid jar")
            continue
        for mod_id, version in provided.items():
            providers.setdefault(mod_id, (name, version))
        all_deps += [(name, *dep) for dep in deps]
    skip_side = "SERVER" if side == "client" else "CLIENT"
    for jar, owner, mod_id, spec, dep_side in all_deps:
        if dep_side == skip_side:
            continue
        if mod_id in PROVIDED:
            version = PROVIDED[mod_id]
        elif mod_id in providers:
            version = providers[mod_id][1]
        else:
            problems.append(f"[{side}] {owner} ({jar}) requires {mod_id} {spec}, which the {side} pack does not have")
            continue
        if not in_range(version, spec):
            problems.append(f"[{side}] {owner} ({jar}) requires {mod_id} {spec}, the pack has {version}")
    return providers


def top_level_duplicates(side: str, jars: dict[str, bytes], problems: list):
    seen = {}
    for name, data in jars.items():
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            try:
                toml = tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8"))
            except (KeyError, tomllib.TOMLDecodeError, UnicodeDecodeError):
                continue
        for mod in toml.get("mods", []):
            if mod["modId"] in seen:
                problems.append(f"[{side}] mod id {mod['modId']} is in both {seen[mod['modId']]} and {name}")
            seen[mod["modId"]] = name


def main() -> None:
    problems: list[str] = []
    providers = {}
    for side in ("client", "server"):
        jars = jars_for(side, problems)
        top_level_duplicates(side, jars, problems)
        providers[side] = check_side(side, jars, problems)
        print(f"{side}: {len(jars)} jars, {len(providers[side])} mod ids")
    for mod_id, (server_jar, server_version) in providers["server"].items():
        client = providers["client"].get(mod_id)
        if client and client[1] != server_version and mod_id not in PROVIDED:
            problems.append(f"{mod_id}: client pack has {client[1]} ({client[0]}), server pack has {server_version} ({server_jar})")
    for problem in problems:
        print("PROBLEM:", problem)
    print("OK" if not problems else f"{len(problems)} problem(s)")
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
