# MDVLCraft Binder (source)

Forge 1.20.1 (47.4.10) workspace for `mdvlcraft-0.7.1.jar`, the glue mod of the MDVLCraft pack: class and
archetype skill trees, the ability wheel, Epic Fight skill grants, custom attributes, and control over Iron's
Spells and Cursed Fate content.

- How the mod works, class by class: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Skill-tree data: `src/main/resources/data/mdvlcraft/puffish_skills/`

## Layout

| Path | What |
|---|---|
| `src/main/java` | decompiled from the 0.7.1 jar (Vineflower 1.11.1). Minecraft members are still SRG names (`m_21133_`), see step 3 |
| `src/main/resources` | everything else from the jar: `mods.toml`, mixin config, assets, data, built-in resource packs |
| `modpack/*.modrinth.index.json` | file lists of the MDVLCraft 1.9.9 client and server packs |
| `tools/fetch_mods.py` | downloads the pack's mods into `libs/` and works out which ones the Binder needs |
| `tools/remap_srg.py` | renames SRG names in the source to Mojang's official names |
| `tools/original.refmap.json` | the refmap from the released jar, for reference (the build makes a new one) |

## Setup

Needs JDK 17, Python 3.11+, and network access to `maven.minecraftforge.net`, `repo.spongepowered.org`,
`piston-meta.mojang.com`, `piston-data.mojang.com`, `libraries.minecraft.net`,
`resources.download.minecraft.net` and `cdn.modrinth.com` (plus Gradle's own hosts).

1. **Fetch the modpack's mods** (all ~170 jars into `libs/modpack`; writes `libs/core.txt` and `libs/nested.txt`):
   ```
   python3 tools/fetch_mods.py                 # uses ../*.mrpack for override jars (epic_fight_ponder)
   python3 tools/fetch_mods.py path/to/MDVLCraft-1.9.9-client.mrpack
   ```
2. **Set up Forge**: `./gradlew build` (the first run downloads Minecraft, Forge and the mappings, and
   deobfuscates the dependency jars).
3. **Remap the source to official names** (run this once):
   ```
   python3 tools/remap_srg.py --check   # list anything it cannot map
   python3 tools/remap_srg.py
   ```
   String literals are left alone. The one mixin that targets a method by its SRG name
   (`VatanseverItemMixin`, `remap = false`) lists both names (`"use", "m_7203_"`) so it works in dev and in
   the released jar.
4. `./gradlew build` → `build/libs/mdvlcraft-0.7.1.jar` (reobfuscated, with `mdvlcraft.refmap.json`).
   `./gradlew runClient` / `runServer` start a dev game with the Binder and its dependencies.

## Notes

- Dev runs load only the Binder's dependency closure (`libs/core.txt`), not the whole pack.
- Ponder, Flywheel and MixinExtras ship inside `epic_fight_ponder` (jar-in-jar). They are on the compile
  classpath from `libs/nested`. Whether FML loads them from the deobfuscated parent at dev runtime still needs
  checking on the first `runClient`.
- Decompiled code can need small fixes (generic casts, lambdas) before it compiles. They will show up at step 4.
