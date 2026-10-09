#!/usr/bin/env python3
"""Build the MDVLCraft client and server .mrpack files from this folder.

  modpack/client.modrinth.index.json   mods downloaded by the launcher (client pack)
  modpack/server.modrinth.index.json   the same for the server pack
  modpack/overrides/                   files both packs ship (config, kubejs, ...)
  modpack/client-overrides/            files only the client pack ships
  modpack/server-overrides/            files only the server pack ships
  modpack/mods/                        mod jars shipped inside both packs (not on Modrinth)
  the MDVLCraft Binder jar             mdvlcraft-binder/build/libs/mdvlcraft-<version>.jar

Usage:
  python3 modpack/build_mrpack.py 1.9.10          writes dist/MDVLCraft-1.9.10-client.mrpack
                                                  and dist/MDVLCraft-1.9.10-server.mrpack
"""
import json
import sys
import zipfile
from pathlib import Path

PACK = Path(__file__).resolve().parent
ROOT = PACK.parent
BINDER_LIBS = ROOT / "mdvlcraft-binder" / "build" / "libs"
DIST = ROOT / "dist"
# Fixed timestamp so rebuilding unchanged inputs gives an identical file
STAMP = (2026, 1, 1, 0, 0, 0)


def binder_jar() -> Path:
    jars = sorted(j for j in BINDER_LIBS.glob("mdvlcraft-*.jar") if not j.name.endswith(("-sources.jar", "-slim.jar")))
    if len(jars) != 1:
        raise SystemExit(f"expected exactly one Binder jar in {BINDER_LIBS}, found {[j.name for j in jars]}; run ./gradlew build")
    return jars[0]


def add(archive: zipfile.ZipFile, name: str, data: bytes) -> None:
    info = zipfile.ZipInfo(name, STAMP)
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o644 << 16
    archive.writestr(info, data)


def add_tree(archive: zipfile.ZipFile, source: Path, prefix: str, written: set) -> None:
    for path in sorted(p for p in source.rglob("*") if p.is_file()):
        name = prefix + path.relative_to(source).as_posix()
        if name in written:
            raise SystemExit(f"{name} is shipped twice")
        written.add(name)
        add(archive, name, path.read_bytes())


def build(side: str, version: str, binder: Path) -> Path:
    index = json.loads((PACK / f"{side}.modrinth.index.json").read_text())
    index["versionId"] = f"{version}-{side}"
    DIST.mkdir(exist_ok=True)
    target = DIST / f"MDVLCraft-{version}-{side}.mrpack"
    written = set()
    with zipfile.ZipFile(target, "w") as archive:
        add(archive, "modrinth.index.json", (json.dumps(index, indent=2) + "\n").encode())
        add_tree(archive, PACK / "overrides", "overrides/", written)
        add_tree(archive, PACK / f"{side}-overrides", "overrides/", written)
        add_tree(archive, PACK / "mods", "overrides/mods/", written)
        add(archive, f"overrides/mods/{binder.name}", binder.read_bytes())
    return target


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    version = sys.argv[1]
    binder = binder_jar()
    for side in ("client", "server"):
        target = build(side, version, binder)
        print(f"{target.relative_to(ROOT)}  ({target.stat().st_size // 1024} KiB, Binder {binder.name})")


if __name__ == "__main__":
    main()
