#!/usr/bin/env python3
"""Rename the SRG names left in the decompiled Binder source (m_21133_, f_8906_ and
so on) to Mojang's official names, which ForgeGradle uses in the dev environment.

The released jar is reobfuscated, so every Minecraft method and field it calls
decompiles to its SRG id. SRG ids are unique across the game, so a plain token
replacement over the source is exact once the two mapping files are joined:

  joined.tsrg   obfuscated -> SRG     (MCPConfig, raw.githubusercontent.com)
  client.txt    official -> obfuscated (Mojang, piston-meta / piston-data.mojang.com)

Usage:
  tools/remap_srg.py                 download both files and rewrite src/main/java in place
  tools/remap_srg.py --check         only report SRG names that would remain
  tools/remap_srg.py --tsrg F --mojang F   use local copies instead of downloading
"""
import argparse
import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCES = ROOT / "src" / "main" / "java"
CACHE = ROOT / "build" / "mappings"
TSRG_URL = "https://raw.githubusercontent.com/MinecraftForge/MCPConfig/master/versions/release/1.20.1/joined.tsrg"
MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"
VERSION = "1.20.1"
SRG = re.compile(r"\b[mf]_\d+_\b")
STRING = re.compile(r'"(?:\\.|[^"\\])*"')
PRIMITIVES = {"void": "V", "boolean": "Z", "byte": "B", "char": "C", "short": "S", "int": "I", "long": "J", "float": "F", "double": "D"}


def fetch(url: str) -> bytes:
    with urllib.request.urlopen(url, timeout=120) as response:
        return response.read()


def cached(name: str, url_for) -> Path:
    path = CACHE / name
    if not path.exists():
        CACHE.mkdir(parents=True, exist_ok=True)
        path.write_bytes(fetch(url_for()))
    return path


def mojang_url() -> str:
    manifest = json.loads(fetch(MANIFEST_URL))
    version = next(v for v in manifest["versions"] if v["id"] == VERSION)
    return json.loads(fetch(version["url"]))["downloads"]["client_mappings"]["url"]


def read_mojang(path: Path):
    """official class -> obf class, and per obf class: obf field -> official,
    (obf method, official argument types) -> official."""
    classes, fields, methods = {}, {}, {}
    current = None
    for line in path.read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        if not line.startswith(" "):
            official, obf = line.rstrip(":").split(" -> ")
            classes[official] = obf
            current = obf
            fields[current], methods[current] = {}, {}
            continue
        member, obf = line.strip().split(" -> ")
        member = member.split(":")[-1]
        kind, rest = member.split(" ", 1)
        if "(" in rest:
            name, args = rest[:-1].split("(", 1)
            methods[current][(obf, tuple(a for a in args.split(",") if a), kind)] = name
        else:
            fields[current][obf] = rest
    return classes, fields, methods


def descriptor(java_type: str, classes: dict) -> str:
    dims = java_type.count("[]")
    base = java_type.replace("[]", "")
    if base in PRIMITIVES:
        code = PRIMITIVES[base]
    else:
        code = "L" + classes.get(base, base).replace(".", "/") + ";"
    return "[" * dims + code


def srg_to_official(tsrg: Path, mojang: Path) -> dict:
    classes, fields, methods = read_mojang(mojang)
    method_index = {}
    for owner, members in methods.items():
        for (obf, args, ret), name in members.items():
            desc = "(" + "".join(descriptor(a, classes) for a in args) + ")" + descriptor(ret, classes)
            method_index[(owner, obf, desc)] = name

    names, owner = {}, None
    for line in tsrg.read_text().splitlines()[1:]:
        if not line.startswith("\t"):
            owner = line.split(" ")[0]
        elif not line.startswith("\t\t"):
            parts = line.strip().split(" ")
            if len(parts) == 3:
                obf, srg, _ = parts
                official = fields.get(owner, {}).get(obf)
            else:
                obf, desc, srg, _ = parts
                official = method_index.get((owner, obf, desc))
            if official and SRG.fullmatch(srg) and official != srg:
                previous = names.setdefault(srg, official)
                if previous != official:
                    raise SystemExit(f"{srg} maps to both {previous} and {official}")
    return names


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--tsrg", type=Path)
    parser.add_argument("--mojang", type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()

    tsrg = args.tsrg or cached("joined.tsrg", lambda: TSRG_URL)
    mojang = args.mojang or cached("client.txt", mojang_url)
    names = srg_to_official(tsrg, mojang)

    unresolved, changed = set(), 0
    for source in sorted(SOURCES.rglob("*.java")):
        text = source.read_text()
        # String literals are left alone: a mixin target written as an SRG name is deliberate
        pieces = STRING.split(text)
        literals = STRING.findall(text)
        for piece in pieces:
            unresolved.update(token for token in SRG.findall(piece) if token not in names)
        if not args.check:
            pieces = [SRG.sub(lambda m: names.get(m.group(0), m.group(0)), piece) for piece in pieces]
            renamed = "".join(p + (literals[i] if i < len(literals) else "") for i, p in enumerate(pieces))
            if renamed != text:
                source.write_text(renamed)
                changed += 1
    if unresolved:
        print("No official name for: " + ", ".join(sorted(unresolved)))
    if args.check:
        print(f"{len(names)} SRG names known")
    else:
        print(f"Rewrote {changed} files; {len(names)} SRG names known")


if __name__ == "__main__":
    main()
