# MDVLCraft changelog

## Unreleased (Binder 0.7.3)

Source changes only: the Binder jar and the packs have not been rebuilt or tested in game yet.

### Combat

- **Inverted Spear of Heaven** strips every effect, good or bad, from whatever it hits *before* the hit's
  damage is worked out (so Resistance no longer softens that hit), then **Seals** the target for 4 s: no
  effect can be applied to it. Every hit strips and re-seals.
- **Cursed Fate techniques** (Blitz, Surprise Attack, Dismantle, Cleave, ...) scale with your melee damage:
  ×1 with an iron sword (6 damage), more with stronger weapons, Strength and Sharpness, never below ×0.5.
- **Samurai katana bonus** also counts Satsujin, Nightfall's Yamato and HF Murasama, the tachis and Cursed
  Fate's katanas, plus anything tagged `#mdvlcraft:katanas`.
- **Absolute Deflection** (Nightfall's parry) is now known by every class, and the Water stance lengthens
  its parry window like it does for Parrying.
- Everyone's Roll and Step are replaced by Nightfall's **souls-like dodge and step**. Whatever you had in the
  dodge slot is swapped for its souls-like counterpart.
- Epic Fight skills never require another skill first.
- Skill tree tooltips show the **point cost** of each node.

### Skill trees

- Every rogue archetype (Assassin, Phantom, Scout, Thief) teaches **Precise Parry**.
- Assassin: Celestial Array: Purge is gone (its node now teaches **Spider Techniques**); new **Shadow Step**
  node; **Blood Step is level 5**.
- Phantom: Babylonian Armory is gone (its node now teaches **All Eyes on You**); new **Doppelganger** ability
  (below).
- Samurai: new **Parry Master** node. Knight: new **Gravity Stomp** (ability wheel). Flame: new **Avatar of
  Might**. Berzerker: new **Dread Full Buster**. Cursed: new **Wither Skull** (level 3). Ice: **Frost Step is
  level 5**.
- Points spent on the removed Celestial Array and Babylonian Armory nodes are refunded.

### Doppelganger (Phantom)

Cast it to summon a double of yourself (your skin, armour and weapons) that stays at your side, flanks your
target and repeats your attacks with your damage. It costs 1 mana per second and fades when you run out.
Cast again while looking at a creature to teleport behind it (like Yamato's Trick); sneak and cast to dismiss
the double.

### World

- **Weapons of Miracles and Cursed Fate mobs no longer spawn on their own** (natural spawns, spawners,
  structures, patrols). Summons, spawn eggs and commands still work.
- **Hostile mobs are better equipped**: zombies, husks, drowned, zombie villagers and wither skeletons have a
  40% chance of an Epic Fight or Epic Knights melee weapon (iron-tier or weaker); those and skeletons/strays
  have a 40% chance of armour, including Epic Knights armour, never better than iron. Settings under
  `[mobGear]` in `config/mdvlcraft-common.toml`.
- **Villager Recruits soldiers no longer get free buffs** (morale Strength/Resistance, elite permanent
  Strength/Fire Resistance, leader and travel auras). Potions and spells still work on them. Toggle:
  `stripRecruitBuffs`.

## 1.9.10

Build with `python3 modpack/build_mrpack.py 1.9.10` after `./gradlew build` in `mdvlcraft-binder/`.

### Fixed

- **Server crash when a recruit swings a Golem Heart** (all six "Ticking entity" crash reports from
  8–9 October). Super Golem's Golem Heart attacks reference bones from the golem's own skeleton
  (`garm_down_1_R`). A Villager Recruits soldier holding one swings it with a human skeleton, Epic
  Fight throws "Failed to get joint path index", and the server crashes on every tick that recruit
  is loaded. Such swings now hit nothing instead (Binder `ColliderMixin`). Reproduced on a test
  server with the old Binder (crashes within seconds) and checked fixed with the new one.
- **Ender Obscuris freezing the server / game.** Weapons of Miracles looks for a spot behind the
  target with a loop that has no limit and treats water, slabs, path blocks, farmland, snow layers
  and the void as solid. With the target standing on or in one of those, the server walks block by
  block (generating chunks as it goes) and can hang for minutes or forever. The teleport is now a
  bounded search with a proper collision check, and is skipped when nothing fits (Binder
  `ReuseableEventsMixin`). Reproduced in game: the old version froze the server thread inside
  `ReuseableEvents.lambda$static$23`; the new one teleports behind the target normally and does not
  hang.
- **Invisibility not working in combat mode.** Iron's Spells hides a truly invisible player by
  cancelling the render event, but Epic Fight's battle-mode renderer runs first, so armour and the
  held weapon stayed visible. The Binder now hides them first (`TrueInvisibilityRender`). Checked
  in game in both modes.
- **Iron's Spells Ponder previews doing nothing.** Epic Fight x Iron's Spells Compat refuses any
  cast within a short delay of the caster's last Epic Fight action; the preview's stand-in caster
  has never acted, so every preview failed with "This spell could not acquire a valid target".
  Preview casts are allowed again (Binder `PreviewCasts`). Checked in game: the Shield preview now
  completes.
- **Tu Di Gong crashing the server.** Its structure search runs on the main server thread with a
  100-chunk radius and skips structures already found, so a search could take over a minute and
  the server watchdog killed the server (crash report of 8 October 10:24). Radius 100 → 50 chunks,
  `avoid_duplicate_searches` off.

### Changed

- **Villager Recruits no longer build their own bases.** AI factions do not lay out or construct
  city buildings (Binder; `stopAiVillageBuilding` in `config/mdvlcraft-common.toml`, default on)
  and do not found new villages (`expansion.enabled = false` in
  `config/village_recruits/politics.toml`). Player factions still build when their leader allows
  it.

### Added

- AppleSkin 2.5.1 (client and server)
- Inventory Profiles Next 1.10.20 and libIPN 4.0.2 (client)
- Backpacked 3.0.9 and Framework 0.8.0 (client and server, shipped in the pack)

### Binder 0.7.2

The Binder is now built from source in `mdvlcraft-binder/`.
