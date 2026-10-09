# MDVLCraft Binder (source)

Forge 1.20.1 (47.4.10) workspace for `mdvlcraft-0.7.1.jar`, the glue mod of the MDVLCraft pack: class and
archetype skill trees, the ability wheel, Epic Fight skill grants, custom attributes, and control over Iron's
Spells and Cursed Fate content.

- How the mod works, class by class: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Skill-tree data: `src/main/resources/data/mdvlcraft/puffish_skills/`

## Layout

| Path | What |
|---|---|
| `src/main/java` | decompiled from the 0.7.1 jar (Vineflower 1.11.1), remapped to official names, plus the 0.7.2 fixes |
| `src/main/resources` | everything else from the jar: `mods.toml`, mixin config, assets, data, built-in resource packs |
| `tools/fetch_mods.py` | downloads the pack's mods into `libs/` and works out which ones the Binder needs |
| `tools/remap_srg.py` | renames SRG names in the source to Mojang's official names |
| `tools/original.refmap.json` | the refmap from the released 0.7.1 jar, for reference (the build makes a new one) |
| `../modpack/` | the MDVLCraft pack itself: indexes, overrides, extra jars, `build_mrpack.py`, `CHANGELOG.md` |

## Setup

Needs JDK 17, Python 3.11+, and network access to `maven.minecraftforge.net`, `repo.spongepowered.org`,
`piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`,
`resources.download.minecraft.net` and `cdn.modrinth.com` (plus Gradle's own hosts).

1. **Fetch the modpack's mods**: `python3 tools/fetch_mods.py` downloads every mod in
   `../modpack/*.modrinth.index.json` into `libs/modpack`, copies `../modpack/mods/*.jar`, and writes
   `libs/core.txt`, `libs/nested.txt` and `libs/compat.txt`.
2. **Build**: `./gradlew build` → `build/libs/mdvlcraft-<version>.jar` (reobfuscated, with
   `mdvlcraft.refmap.json`). The first run downloads Minecraft, Forge and the mappings and deobfuscates
   the dependency jars. `./gradlew runClient` / `runServer` start a dev game with the Binder and its
   dependencies.
3. **Build the pack**: `python3 ../modpack/build_mrpack.py 1.9.10` →
   `../dist/MDVLCraft-1.9.10-{client,server}.mrpack`.

The source has already been remapped (`tools/remap_srg.py` did that once; it is kept for reference).
The one mixin that targets a method by its SRG name (`VatanseverItemMixin`, `remap = false`) lists
both names (`"use", "m_7203_"`) so it works in dev and in the released jar.

If Maven Central rate-limits you (HTTP 429), point Gradle at a mirror with an init script, e.g.
`https://maven-central.storage-download.googleapis.com/maven2/`.

## Notes

- Dev runs load only the Binder's dependency closure (`libs/core.txt`), not the whole pack.
- Ponder, Flywheel and MixinExtras ship inside `epic_fight_ponder` (jar-in-jar). They are on the compile
  classpath from `libs/nested`. Whether FML loads them from the deobfuscated parent at dev runtime still needs
  checking on the first `runClient`.
- The rebuilt 0.7.1 matched the released jar: same files, identical refmap and class signatures.
