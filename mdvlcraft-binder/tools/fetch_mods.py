#!/usr/bin/env python3
"""Download every mod in the MDVLCraft modpack into libs/modpack and work out
which of them the Binder needs in the dev environment.

Sources:
  modpack/client.modrinth.index.json and modpack/server.modrinth.index.json
      (copied out of MDVLCraft 1.9.9; every file is fetched from cdn.modrinth.com
      and checked against its sha512)
  any *.mrpack given on the command line, or ../*.mrpack by default
      (for jars shipped as overrides, e.g. epic_fight_ponder, which is not on Modrinth)

Writes:
  libs/modpack/<jar>         every mod in the pack except the Binder itself
  libs/core.txt              the Binder's mandatory dependencies plus everything
                             they in turn require, one jar name per line;
                             build.gradle puts these on the compile and runtime classpath
  libs/nested/, nested.txt   jar-in-jar libraries (e.g. Ponder inside epic_fight_ponder)
                             that the Binder compiles against

Needs network access to cdn.modrinth.com.
"""
import hashlib
import http.client
import io
import re
import json
import sys
import time
import tomllib
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
INDEXES = [ROOT / "modpack" / "client.modrinth.index.json", ROOT / "modpack" / "server.modrinth.index.json"]
OUT = ROOT / "libs" / "modpack"
CORE = ROOT / "libs" / "core.txt"
NESTED = ROOT / "libs" / "nested"
NESTED_LIST = ROOT / "libs" / "nested.txt"
SELF = "mdvlcraft"
PROVIDED = {"forge", "minecraft", "javafml", "lowcodefml"}


def sha512(data: bytes) -> str:
    return hashlib.sha512(data).hexdigest()


def download(entry: dict, target: Path) -> None:
    expected = entry["hashes"]["sha512"]
    if target.exists() and sha512(target.read_bytes()) == expected:
        return
    last_error = None
    for url in entry["downloads"]:
        for attempt in range(4):
            try:
                with urllib.request.urlopen(url, timeout=120) as response:
                    data = response.read()
            except (OSError, http.client.HTTPException) as error:
                last_error = error
                time.sleep(2 ** (attempt + 1))
                continue
            if sha512(data) != expected:
                raise SystemExit(f"sha512 mismatch for {url}")
            target.write_bytes(data)
            return
    raise SystemExit(f"could not download {target.name}: {last_error}")


def fetch_index() -> None:
    seen = set()
    for index in INDEXES:
        for entry in json.loads(index.read_text())["files"]:
            path = entry["path"]
            if not path.startswith("mods/") or path in seen:
                continue
            seen.add(path)
            name = path.split("/", 1)[1]
            print(f"  {name}")
            download(entry, OUT / name)


def extract_overrides(packs: list[Path]) -> None:
    for pack in packs:
        with zipfile.ZipFile(pack) as archive:
            for name in archive.namelist():
                if name.startswith("overrides/mods/") and name.endswith(".jar"):
                    jar = name.rsplit("/", 1)[1]
                    if jar.startswith(SELF + "-"):
                        continue
                    print(f"  {jar} (override from {pack.name})")
                    (OUT / jar).write_bytes(archive.read(name))


def mods_toml(archive: zipfile.ZipFile) -> dict | None:
    try:
        return tomllib.loads(archive.read("META-INF/mods.toml").decode("utf-8"))
    except KeyError:
        return None
    except tomllib.TOMLDecodeError as error:
        print(f"  warning: unreadable mods.toml ({error})", file=sys.stderr)
        return None


def describe(jar: Path) -> tuple[dict[str, str | None], set[str]]:
    """Mod ids a jar provides, mapped to the nested jar that holds them (None for
    the jar itself), and the mandatory ids it needs from elsewhere."""
    provided, required = {}, set()

    def visit(archive: zipfile.ZipFile, nested: str | None) -> None:
        toml = mods_toml(archive)
        if toml:
            for mod in toml.get("mods", []):
                provided.setdefault(mod["modId"], nested)
            for deps in toml.get("dependencies", {}).values():
                for dep in deps:
                    if dep.get("mandatory", dep.get("type") == "required"):
                        required.add(dep["modId"])
        for name in archive.namelist():
            if name.startswith("META-INF/jarjar/") and name.endswith(".jar"):
                with zipfile.ZipFile(io.BytesIO(archive.read(name))) as inner:
                    visit(inner, name)

    with zipfile.ZipFile(jar) as archive:
        visit(archive, None)
    return provided, required - provided.keys()


def dependency_closure() -> tuple[list[str], list[str]]:
    """Jars from the pack the Binder needs, and every jar nested inside them
    (extracted to libs/nested so the Binder can compile against them)."""
    needs = {}
    providers = {}
    for jar in sorted(OUT.glob("*.jar")):
        provided, required = describe(jar)
        needs[jar.name] = required
        for mod_id, nested in provided.items():
            providers.setdefault(mod_id, (jar.name, nested))

    own = tomllib.loads((ROOT / "src/main/resources/META-INF/mods.toml").read_text())
    wanted = [dep["modId"] for dep in own["dependencies"][SELF] if dep.get("mandatory")]
    chosen, nested_jars, missing, done = [], [], set(), set()
    while wanted:
        mod_id = wanted.pop()
        if mod_id in PROVIDED or mod_id in done:
            continue
        done.add(mod_id)
        if mod_id not in providers:
            missing.add(mod_id)
            continue
        jar, _ = providers[mod_id]
        if jar not in chosen:
            chosen.append(jar)
            wanted.extend(needs[jar])
    for jar in chosen:
        with zipfile.ZipFile(OUT / jar) as archive:
            for name in archive.namelist():
                if name.startswith("META-INF/jarjar/") and name.endswith(".jar"):
                    NESTED.mkdir(parents=True, exist_ok=True)
                    target = name.rsplit("/", 1)[1]
                    (NESTED / target).write_bytes(archive.read(name))
                    nested_jars.append(target)
    if missing:
        print(f"  not found in the pack (assumed optional or built in): {', '.join(sorted(missing))}", file=sys.stderr)
    return sorted(chosen), newest(nested_jars)


def newest(jars: list[str]) -> list[str]:
    """Several mods can bundle different versions of one library; keep the newest of each."""
    def split(name: str):
        match = re.match(r"(.+?)-(\d[\w.+-]*)\.jar$", name)
        return (match.group(1), match.group(2)) if match else (name, "")

    def key(version: str):
        return [int(part) if part.isdigit() else part for part in re.split(r"[.+-]", version)]

    best = {}
    for jar in set(jars):
        artifact, version = split(jar)
        if artifact not in best or key(version) > key(split(best[artifact])[1]):
            best[artifact] = jar
    return sorted(best.values())


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    packs = [Path(p) for p in sys.argv[1:]] or sorted(ROOT.parent.glob("*.mrpack"))
    print("Downloading modpack files")
    fetch_index()
    print("Extracting overrides")
    extract_overrides(packs)
    print("Resolving the Binder's dependencies")
    core, nested = dependency_closure()
    CORE.write_text("\n".join(core) + "\n")
    NESTED_LIST.write_text("\n".join(nested) + "\n")
    print(f"{len(core)} jars listed in {CORE.relative_to(ROOT)}, {len(nested)} nested jars in {NESTED_LIST.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
