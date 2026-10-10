# MDVLCraft changelog

## Unreleased (Binder 0.7.8)

### Fixes

- **Night vision from races really works now** (Shadow, and Dwarf's and Arachnae's toggles). The real cause:
  Alex's Caves (installed only for T.O Magic 'n Extras) replaces the whole lightmap update with its own copy for
  its "biome ambient light colouring", and that copy has no Origins night-vision hook. (It is meant to step aside
  when Distant Horizons is installed, but does not.) Its ambient light and colouring are off
  (`config/alexscaves-client.toml`); its cave biomes are switched off in this pack anyway. Tested on the test
  server: as Shadow with no potion, the lightmap is now the same as with the Night Vision potion (before, the
  power changed nothing). Turning BadOptimizations' lightmap caching off (1.9.16) stays: it would still delay
  night-vision powers.
- Shadow's *Behind You* tested: G teleports you behind the creature you look at, then the 10 s cooldown bar shows.
- **True Herobrine now turns his head to follow you.** His only look AI was vanilla's look-at-player goal: it
  starts on a 2% chance per tick, stops after 2-4 seconds and needs a clear line of sight, which the trees he
  stands among nearly always block, so he stared straight ahead. The Binder now points his head at the nearest
  player every tick (`compat/HerobrineLook.java`). Tested: his head turns to face the player as they move round him.
- **PlayerRevive's give-up key is rebindable**: while bleeding out you used to hold the attack key (left click) to
  give up, which could not be changed on its own. It is now its own key, **Give Up (hold, while bleeding out)**
  under *MDVLCraft* in Controls, default **Z** (free since the old Character Status key went). The on-screen hint
  names it. Binder `mixin/playerrevive/ReviveEventClientMixin`. Tested: holding Z for the set time gives up.
- Simple Voice Chat's "Press **Not bound** to set up" hint (its key now opens from the menu) now reads "Open Voice
  Chat from the MDVLCraft menu to set it up" (MDVLCraft-Astrologer-UI resource pack).

## 1.9.16 (Binder 0.7.8)

Packs built (`python3 modpack/build_mrpack.py 1.9.16`). Not yet tested in game: the pixel-art icons, Despair, the
Origins fixes and the 75% render scale.

### Skill trees

- **Ice**: Cursed Blast is replaced by **Despair** (T.O Magic 'n Extras, Level 5: an ice axe blade that pierces
  everything in its path, recast up to 4 times; 54 mana, 13.5 s cooldown with the pack's T.O scaling of 0.6x mana
  and 0.75x cooldown), same place and cost (3 points). Halberd Horizon stays.
- **Water**: Cursed Blast and Halberd Horizon removed.
- Despair has its own Astrologer icon (`tools/astro_theme.py`); Cursed Blast's icon and mana/cooldown config are
  gone.

### Astrologer icons: Minecraft pixel size

- Every Astrologer icon is redrawn as Minecraft-style pixel art: **one texture pixel per GUI pixel** at the size
  it is shown, solid pixels only (no soft glow or half-transparent edges), a dark one-pixel outline and the
  starlight colours. Before, every skill-tree icon was 32x32 and was shrunk to 24 or 12 pixels on most nodes,
  and the race and class icons were 32x32 items shown at 16, so they looked finer than the rest of the game.
  - Skill trees: 32x32 on the big spell/technique/skill nodes, 24x24 on the normal nodes, 12x12 on the small
    bonus nodes (a bonus used on both gets both sizes). The trees open fully zoomed in, which is 1:1.
  - Tree tabs, races and classes: 16x16, the size of an item.
  - Node frames were already 1:1 and are unchanged.
- `tools/astro_theme.py` now redraws every icon from its original art (`tools/astro_sources.json`, recovered
  from the trees as they were before the Astrologer look) on every run, so the result no longer depends on
  earlier runs; `tools/astro_origins.py` uses the same pixel renderer. Icons now live under
  `textures/gui/astro/<kind>/<px>/`; the list is `docs/astrologer-icons.csv`, all of them on
  `docs/astrologer-icons.png`.

### Races (Origins)

- **Shadow**: *Fragile* takes 3 max health instead of 8.
- **Night vision from races works again** (Shadow, and the toggled night vision of Dwarf and Arachnae).
  BadOptimizations only redraws the lightmap when the vanilla Night Vision *effect* (or gamma, dimension, game
  time...) changes, so an Origins night-vision *power* was ignored until the next refresh, and never shown while
  the day cycle is stopped. Its lightmap caching is off (`config/badoptimizations.txt`), which is vanilla
  behaviour.

- **Shadow**: *Behind You* (teleport behind the creature you look at) was on vanilla's *Save Hotbar Activator* key,
  which is unbound, so it could never be used. It is now on the Origins primary key (**G**).
- **Kirin**: *Climb* was switched on with the same unbound key (and Kirin's G and H are taken), so it never worked.
  Kirin now climbs walls whenever they walk into them.
- **No diet restrictions**: Arachnae, Sharkfolk, Kirin and Saurusfolk lose *Carnivore* (meat only), and the Wood Elf
  loses *Steward*, which made them vegetarian. Their descriptions no longer mention it. No class has a diet
  restriction.

### Other

- The new-skill-point chat message now says to assign it from the Skills page of the MDVLCraft menu (it named the
  removed skill-tree key, which showed as unbound). Set in the MDVLCraft-Astrologer-UI resource pack.
- **Client (Performance)** renders the 3D world at 75% resolution with smooth upscaling (RenderScale,
  `config/renderscale.json5`); the HUD and menus stay sharp. The regular client stays at 100%.

## 1.9.15 (Binder 0.7.7)

Packs built (`python3 modpack/build_mrpack.py 1.9.15`). The menu, every screen it opens and the Status screen were
tested on the test server, as were 7 rejoins (5 after a kick, 1 after a disconnect, 1 after a server restart).

### Rejoin fix

- Joining a server could fail with *"Internal Exception: java.lang.NullPointerException: null value in entry:
  _hostile_when_hit=null"* (seen on the first reconnect after the server restarted; the next try worked). Origins'
  library Calio decodes powers on the network thread and creates registry holders for "multiple" powers' sub-powers
  while the main thread is resetting and filling the same registry; the two race on the registry's map and a
  holder can come back null. The Binder now creates those holders under the lock the main thread already holds
  while it fills the registry (`compat/CalioHolders.java`, mixins `calio.HolderCodecMixin` and
  `calio.CalioCodecHelperMixin`). `tools/fetch_mods.py` now also extracts Calio from the Origins jar so the
  Binder compiles against it.

### Keys

- The menu key defaults to **M** and Xaero's world map to **F** (`options.txt` in both client packs).

### MDVLCraft menu

- New **menu key** (M, rebindable; "Open Menu" under *MDVLCraft* in Controls) opens the MDVLCraft menu, in the
  Astrologer look: **Status**, **Abilities**, **Skills** (Pufferfish skill trees), **Advancements**, **Origin**,
  **Brewing** (Ars Elixirum collection), **Recruits ›** (a page with **Claim Map** and **Faction**), **Voice Chat**
  and **Map ›** (a page with **World Map Settings** and **Minimap Settings**). Claim Map works in the Overworld only,
  as before. A button greys out when its mod is missing. Tested in game: every button opens its screen and Back
  returns to the menu page it came from.
- Every page has a **Back** button, and so does every screen opened from the menu (bottom left). The Back is drawn
  and clicked through Forge's screen events, so it also works on screens that draw themselves (the skill trees,
  Better Advancements). The Xaero settings screens use their own Back, which returns to the menu. Esc still closes everything; pressing the menu key again closes it.
- The keys these screens used to have are **removed from the game**, not just unbound: they are gone from the
  Controls list and from the key lookup, so the menu is the only way in. Removed: Advancements, Pufferfish
  *Open Skills*, Origins *View Origin*, Ars Elixirum *Collection*, Recruits *Faction screen* and *Map screen*,
  Voice Chat *Voice Chat*, Xaero *World Map settings* and *Minimap settings*, and the Binder's own *Open
  Abilities* and *Character Status* keys. Binder `client/RemovedKeys.java` (with `OptionsAccessor` and
  `KeyMappingAccessor`).
- Default keybinds (`options.txt` in both client packs): `M` opens the menu; the removed keys' lines are gone.
- Made by `client/MenuScreen.java`, `client/MenuNav.java` (opening other mods' screens and adding the Back button)
  and `client/AstroGui.java` (the shared Astrologer panel and button).

### Character screen

- New **Character Status** screen (from the menu's Status button), laid out like Elden Ring's Status page in the skill trees'
  Astrologer look (night sky, gold frame, a faint astrolabe):
  - **Level / Archetype / Experience / Next Level In**: the highest-level picked archetype (by level, then
    experience); "n/a" until an archetype is picked.
  - **Attributes**: Attack Damage, Attack Speed, Crit Chance, Crit Damage, Life Steal, Spell Power, Cooldown
    Reduction, Mana Regen, Movement Speed.
  - **Base Stats**: HP, Mana and Stamina (current / max), Poise (Epic Fight stun armour), Luck.
  - **Attack Power (DPS)**: every weapon on the hotbar, as attack damage x attack speed with that weapon in hand.
  - **Defence / Dmg Negation**: armour and the share of a 10-damage hit stopped (Physical), Armor Toughness, and
    the share stopped of Projectile, Fire, Blast and Fall hits (armour and protection enchantments, using Apothic
    Attributes' formulas), Knockback Resistance, Dodge Chance.
  - Labels too long for their row are shortened (Mana Regen, Knockback Res.) so they never run into the value.
  - Made by `client/CharacterScreen.java`; the astrolabe by `tools/astro_character.py`.
- Apothic Attributes' attributes button next to the player in the inventory is off
  (`Enable Attributes GUI=false` in `config/attributeslib.cfg`).

## 1.9.14 (Binder 0.7.6)

Packs built (`python3 modpack/build_mrpack.py 1.9.14`). A fresh install of the server pack boots, pre-generates chunks
and reloads cleanly; the client was tested on the test server (race picker, icons, alphabetical order).

### Requested changes

- **Races and classes are listed alphabetically** in the race and class pickers (Origins sorted them by impact,
  then by each mod's own order). Binder `ChooseOriginScreenMixin`.
- **No natural Village Recruits villages**, in both packs: the tower village no longer replaces vanilla villages
  in new chunks (`kubejs/data/minecraft/worldgen/structure_set/villages.json`, Village Recruits' own "no natural
  villages" set), and towers already generated in existing chunks no longer found a village
  (`naturalVillagesEnabled = false` in `config/village_recruits/politics.toml`). Vanilla villages are unchanged.
- **Shaders are off by default** (`enableShaders=false` in `config/oculus.properties`; MakeUp UltraFast stays
  selected, so turning shaders on in the video settings uses it).
- **Your key bindings are the default** in both client packs (from the options.txt you sent): ability wheel Tab,
  abilities screen on the second extra key, siege machine use right mouse, Xaero world map F, Ares HUD F1, and so on.
- **Map Atlases removed** (client and server), with its configs, its first-join atlas and its keys.
- **Fantasy Armor wears out** (`enableDurability = true`).
- **Recruits deal normal damage to players.** Village Recruits halved recruit damage to players (a quarter from
  guns) and capped one hit at 25% of max health, and Recruits Epic Fight Compat halved it again; all of that is
  off (`config/village_recruits/military.toml`, `defaultconfigs/recruits_epicfight_compat-server.toml`). An
  existing world keeps its own copy of the second file in `world/serverconfig`, which has to be edited by hand.
- **Samurai has Blink II again** (T.O Magic's Blink, linked from Stance where it was before), with an
  Astrologer icon like every other spell.
- **Skill tree document**: a new Origins part lists every race and class in the game, alphabetically, with
  each one's powers.

### Race and class pickers: the Astrologer's look

- Every race gets a simple star-chart silhouette drawn in the same style (Human, Feline, Merling, Arachnae,
  Dwarf, Wood Elf, Hobgoblin, Siren, Half-Ogre, Sharkfolk, Kirin, Saurusfolk, Ratfolk, Shadow, Sporeling), so
  the three race mods no longer look different; every class gets a gold star icon drawn from its own item. Only
  races and classes that are in the game have icons. Made by `mdvlcraft-binder/tools/astro_origins.py` and
  shown through a hidden Binder item (`mdvlcraft:origin_icon`) that `origins_overrides.py` points each race and
  class at (`modpack/origin_icons.json`).
- The picker panel, the origin list and the power badges are redrawn in the night-sky palette (navy and gold
  frames, a starry background, astrolabe badges), in the resource pack `MDVLCraft-Astrologer-UI.zip` that both
  client packs ship and enable at the top of the resource pack list. Existing installs keep their own resource
  pack list: enable it under Options > Resource Packs.
- Checked in game: the picker shows the new frames, background and icons, in alphabetical order.

### Java arguments

- Client (`MDVLCraft-Java-Arguments.txt`) and server (`user_jvm_args.txt`): `-Dforge.readTimeout=180`, so a player
  joining with the pack's ~200 mods has 3 minutes instead of 30 seconds to finish the login handshake. Memory and
  garbage-collector flags are unchanged (the client log showed no memory or lag trouble).

### Fixed (from the client log of 10 October)

- 44 client-side config files the pack never shipped (Shoulder Surfing, Horseman, Origins, Alex's Caves, Farmer's
  Delight, Moonlight, Falling Leaves, Dynamic FPS and others) are now in `client-overrides/config`, so the
  first start no longer rewrites them with warnings. `vix-client.toml` was for a different VIX version and is
  replaced by the one VIX 1.1.0 writes.
- **Indestructible**: `replace_health_bar` is 0 (the setting that can crash with Epic Fight).
- **Broken tags**: four T.O Magic entity tags named Cataclysm mobs that no longer exist, and five common tags from
  the Sounds mod named 1.21 items; a tag with one missing entry fails completely (for example T.O's Spectral
  Blink and Spectral Shift blacklists and the shared `c:foods` tag were empty). Replacements in `kubejs/data` keep
  every entry but make each one optional.
- **Medieval Siege Machines' advancements** all failed to load (icons and item checks in the 1.20.5 format);
  1.20.1 copies are in `kubejs/data/siegemachines/advancements`.
- **Origins**: Wood Elf listed an archery power that only loads without Apothic Attributes (the pack has it, so
  the Apothic version is used); Dwarf's Mythril Resonance and Hobgoblin's Greedy used a reach attribute from a mod
  that is not in the pack (now Forge's own block reach); the 13 Medieval Origins races that are not on offer
  no longer list powers they cannot load. `origins_overrides.py` now evaluates the mods' load conditions.
- The Binder reported itself as 0.7.4 whatever its version (`mods.toml` now takes the jar's version).
- `tools/astro_theme.py` can be re-run safely: icons it already made and the restyled window are kept instead of
  being redrawn from themselves.

Left as they are (in the mods themselves): missing sounds and models in several mods, animations Epic Fight
cannot read in two Epic Fight add-ons, recipes for items that do not exist (Cataclysm, Naoya's add-on), the
original Medieval Origins files of the races that are not on offer, Distant Horizons' own "mapTest" setting, and
mods probing for optional integrations.

## 1.9.13 (Binder 0.7.5)

Packs built (`python3 modpack/build_mrpack.py 1.9.13`). A fresh install of the server pack boots, pre-generates
chunks and reloads cleanly; the client was tested on the test server (Herobrine, Fantasy Armor in battle mode).

### Skill tree document

- New layout for `docs/MDVLCraft-Skill-Trees.pdf` (layout A of the four offered), same style: the overview
  table, then one page per class with its archetypes side by side (5 pages instead of 19). Spells and
  techniques are listed together as Abilities; summaries are one line, class-wide bonuses are listed once at
  the top of the class's page and the core stats only in the overview.
- Negative bonuses showed as "+-60%" (Thief's Anvil Repair Cost); now "-60%".

### Mods added

- **Fantasy Armor (Medieval Series) 1.2.4** (client and server). Checked with Epic Fight: its 29 armour sets are
  GeckoLib models with the standard armour bones, which Epic Fight's built-in GeckoLib support puts on its
  animated body, and the author ships a separate cape for Epic Fight (shown by default). Tested in game in
  battle mode: the Dragonslayer set follows the body through a sword swing. Its armour is indestructible by
  default (`enableDurability = false` in `config/fantasy_armor-common.toml`), left as the mod ships it.
- **Medieval Siege Machines 1.39** (mortar, culverin, trebuchet, catapult, ballista, battering ram, siege
  ladder), with **Recruits Siege Compatibility 2.1.0**, which lets recruit bowmen aim and fire ranged siege
  engines (command keys below). Recruits may also mount the machines (`MountWhitelist`).
- **AstikorCarts Redux 1.2.5** (supply cart, animal cart, hand cart, plough, seed drill, reaper).
- **No Mob Farm 1.6.6** (server only; it was already on the live server). Slows spawns at places where many
  mobs die in a short time and stops iron golem and raid farming; defaults kept
  (`server-overrides/config/nomobfarm.properties`).
- Ars Elixirum was already in the pack (since 1.9.12).
- No known incompatibilities were found for these with the pack's mods; every new mod's config is shipped so
  the first start does not log "is not correct" warnings. Server boots cleanly with all five.

### Config review

- **Key conflicts from the new mods**, fixed in both client packs' default `options.txt` (new installs):
  cart attach/detach C (was R, the ability cast key), cart slow unbound (was Z, Xaero's map zoom), siege
  machine inventory X (was I, the Recruits command screen), siege machine use stays F and Map Atlases' minimap
  toggle moves to keypad 7, the siege command keys (fire, ram swing, ram jump, ladder dismount, command screen)
  move to keypad 1–5 (were M, J, K, L, B: world map, swap shoulder, skills, advancements, backpack). The
  Client (Performance) pack's `options.txt` was also missing 9 of the key changes made in 1.9.12; both packs
  now have the same key bindings.
- **Does It Tick** froze every entity more than 64 blocks from a player, including recruits on patrol,
  village workers and builders, Village Recruits' forced-loaded battles, siege machines and carts. Their mods
  are now whitelisted (`config/does_it_tick-common.toml`).
- **Entity Culling** no longer skips ticks of carts and siege machines that are out of view (a pulled cart
  behind the player jittered), as it already did for boats.
- **Distant Horizons on the server** (`server-overrides/config/DistantHorizons.toml`, server pack only): 2
  generation threads at half duty (it scaled with the host's cores and competed with the main thread), and
  clients can request LOD generation up to 256 chunks away instead of 4,096 (the pack's clients use 128).
- **Item merging**: ServerCore, Get It Together Drops and Village Recruits all merged dropped items; ServerCore's
  merge radius is back to vanilla so the two dedicated mods do the job.
- **Chunky** resumes an unfinished pre-generation after a restart.
- **Client (Performance) pack**: fewer Streams Reflowing water particles (100 per tick within 32 blocks,
  none on vanilla water) and lighter Sound Physics ray tracing (16 rays, 2 bounces).
- `build_mrpack.py`: a client- or server-only override now replaces the shared file of the same name.

### Server log review (logs of 7–9 October)

The live server was still on 1.9.9 (Binder 0.7.1) plus You Shall Not Spawn, Necronomicon and No Mob Farm
added by hand. Every crash and watchdog shutdown in these logs (recruits swinging a Golem Heart, Tu Di Gong's
structure search) was already fixed in 1.9.10; updating the server fixes them. One start failed because You
Shall Not Spawn was present without Necronomicon, and several joins were refused because the player's client
had a different Library of Exile version from the server (client and server packs out of step).

Fixed:

- **Nether log spam (470,000 warnings, "Empty height range: biased[...]").** WWOO changes the Overworld lava
  springs to stop 194 blocks below the top of the world; in the Nether (128 blocks of terrain) that range is
  empty, so every generated Nether chunk logged warnings, also from Distant Horizons' generator threads. The
  springs now stop at Y 125, the same height as before in the Overworld (`kubejs/data/minecraft/worldgen/
  placed_feature/spring_lava*.json`). Test: 441 new Nether chunks, 3,592 warnings before, none after.
- **Villages re-planned every 5 seconds forever** (2,000 log lines an hour). When a planned plot overlaps a
  village's tower, Village Recruits generates the whole city plan again, which can overlap again (and a built
  plot always does), so it repeated endlessly, re-saving each time. Built plots no longer count and unbuilt
  plots still overlapping after a re-plan are dropped, so a village is re-planned at most once (Binder
  `CityPlanManagerMixin`).
- **Mobs and containers in 13 structures spawning empty.** L_Ender's Cataclysm (Deepling, Deepling Brute,
  Deepling Priest, Koboleton, Drowned Host with sword or trident, the occupied desert village, the desert site)
  and Iron's Spells (four Catacombs rooms, the Pyromancer tower basement) save items in the format of newer
  Minecraft versions, which 1.20.1 cannot read: the mobs lost their weapons and armour and item frames,
  chests and spawners came out empty. The Binder ships converted copies (made with
  `mdvlcraft-binder/tools/fix_structure_items.py`). Test: a Deepling now holds its coral spear and a Drowned
  Host wears its chainmail (both empty-handed before).
- **Chunky** reports progress every 30 seconds instead of every second (22,850 lines during pre-generation).
- **Curios: Head slot** turned on for players (Ring 2, Necklace 1, Head 1).

Left as they are (harmless or not fixable from the pack): item frames in YUNG's structures logging "Hanging
entity at invalid position" while chunks generate, Lithostitched's empty template pool notice, Kill Cam
re-writing its FOV setting on every start (a rounding bug in the mod), Village Recruits' debug output and its
aircraft altitude notice, Streams Reflowing's terrain statistics, ModernFix skipping a mount event for
Striders, and "moved too quickly" during lag.

### Curios slots

- `docs/Curios-Slots.md` lists every Curios slot in the pack, which items go in each, and which are switched
  off. Ring (2) and Necklace (1) were on (Head added below): the Iron's Spells override in
  `kubejs/data/irons_spellbooks/curios/entities/iss_entities.json` uses `"replace": true`, which removes every
  other mod's player slots. Nothing is changed yet.

### True Herobrine

- **True Herobrine 1.1** added (client and server). Herobrine appears now and then in the Overworld above
  Y 60, watches from a distance and vanishes when a player comes within 25 blocks or after 2 minutes; he does
  not break or place blocks. The mod's defaults are shipped in `config/True Herobrine.toml`.
- Herobrine is drawn with the pack's own skin (`assets/mdvlcraft/textures/entity/herobrine.png`) on the full
  wide-arm player model, so the jacket, sleeve and trouser layers show; the mod's own model only has the hat
  layer. The mod's glowing eyes are kept and sit on the skin's eyes. Done by the Binder
  (`client/HerobrineRender.java`), which loads after True Herobrine and replaces its renderer, so the mod's
  jar is used unmodified. Tested on a dedicated server with a client: the skin, the outer layers and the
  glowing eyes render correctly.

## 1.9.12 (Binder 0.7.4)

Source changes only: the packs have not been built or tested in game yet.

### Skill trees: the Astrologer's look

Every tree keeps its exact layout but is redrawn as a star chart (generated by
`mdvlcraft-binder/tools/astro_theme.py`; the full list is `docs/astrologer-icons.csv`, all icons on one sheet
in `docs/astrologer-icons.png`):

- **An icon for every subject, 292 in all**, each drawn from that subject's own art: 111 spells (starlight
  plates of the spell's Iron's Spells / T.O icon, pale blue), 20 Binder abilities and techniques (violet),
  53 Epic Fight skills (engraved gold), 87 bonus attributes (silver line glyphs) and the 21 archetype and
  class nodes of the Classes tab, plus the 18 tab icons. Every one of the 6,729 nodes was checked to point
  at its new icon; nodes granting the same thing share an icon (for example every Endurance node).
- **Frames** are astrolabe rings, tinted by class family as before (mage blue, warrior amber, rogue violet,
  ranger green): faint when locked, brighter when available, a gold arc when affordable, glowing when
  learned; milestones carry ticks and the big ones a star point.
- **Background** is a night sky; **connections** are constellation lines (dim blue, gold when the next
  node can be bought, pale starlight once learned); the window and tabs are midnight blue with a brass
  edge, and locked or available icons are tinted night blue instead of grey.

### Skill trees

- **Druid and Wanderer** get **Adaptive Skin** (Epic Fight passive, same node as Holy's), and picking either
  archetype on the Classes tab now makes you **immune to Poison and Hunger** (new attribute
  `mdvlcraft:affliction_immunity`; the effects are refused when applied, so food poisoning, poison
  arrows and spells all fail).
- **Stalker**: two new nodes, **Bodkin Points I and II**, each +15% **Arrow Armor Penetration** (new
  attribute `mdvlcraft:arrow_penetration`: arrows and bolts you shoot ignore that share of the target's
  armour, through Epic Fight's armour negation). Stalker also gets **Critical Knowledge** (Weapons of
  Miracles passive). It already works with bows: Epic Fight turns arrow damage into its own damage
  type, which is what the skill listens for, so arrow hits roll the same 20% crit for x2 damage (more
  chance with Fire Protection, more damage with Blast Protection).
- **Arcane Sustenance**, a new Binder ability for **Scout** and **Wanderer**: 50 mana for 1 hunger shank
  and 4 saturation. It does nothing (and costs nothing) when you are completely full.
- **Cursed** gets **Paralyzing Skreech** (T.O `violent_skreech`, level 3, channelled: pulses that shred
  armour, chill and damage everything nearby). T.O only registers it when Alex's Mobs is installed, which
  the pack does not have; the spell only borrows that mod's sounds and particle, so the patched T.O jar
  registers it anyway and uses the Warden's sonic charge, sonic boom and particle instead.
- **Thief** gets **Astral Sense** (T.O, level 1, 60 mana, 180 s cooldown).
- **Scout gets four crow techniques** from **Cursed Fate: Black Bird Manipulation 1.1.2** (new, CurseForge,
  MIT; shipped in `modpack/mods`): **Silent Appraisal** (20 mana, 15 s), **Blind Investment** (15 mana,
  15 s), **Bird Strike** (20 mana, 15 s) and **Controlled Collapse** (40 mana, 40 s). Like the other
  Cursed Fate techniques they cost mana instead of cursed energy and are cast from the ability wheel,
  always as the regular version (never chanted, Flow or Maximum). The rest of the addon is off: its
  technique is never given to players, Liquidation Cycle and the Black Market Sky domain are not granted,
  its crows never spawn on their own, its blocks/items are removed from recipes, loot and creative tabs.
  The addon casts Cursed Fate abilities 113-118, the same numbers as Projection Sorcery (Follow Up Kick,
  Phantom Movement); the Binder only switches it on while one of its techniques is held, so a cast never
  fires both addons.

### Origins (races and classes)

New: **Origins (Forge) 1.10.0.9**, **Medieval Origins Revival 6.6.0**, **Origins++ 2.4**, **Origins: Classes 1.2.1**,
**Pehkui** and **Caelus** (required by them) and **Alternate Origin GUI** (client: a nicer race picker). Players
pick a race and a class when they first join. All changes are a datapack in `modpack/overrides/kubejs/data`,
generated by `modpack/origins_overrides.py`:

- **Races on offer** (every other race from these mods is hidden): Human, Feline, Merling; Arachnae,
  Dwarf, Wood Elf, Hobgoblin, Siren, Half-Ogre; Sharkfolk, Kirin, Saurusfolk, Ratfolk, Shadow, Sporeling.
- **No size changes**: every race is normal player size. The powers that shrank or grew players are
  removed outright (Dwarf Stocky/miniature, Wood Elf Towering, Goblin Stunted, Ogre Gargantuan, Kirin Cat
  Size, Raptus Small, Rat Small Boi, Sporeling Tiny), so nothing is resized and then corrected.
- **Feline**: fall damage reduced by 25% instead of immunity; no Weak Arms (natural stone mining penalty).
- **Merling**: breathes underwater and on land (no more suffocating out of water or air-from-potions).
- **Arachnae**: no Brittle. **Dwarf**: no Darkness Dweller (sunlight blindness), no Potent Brew.
  **Wood Elf**: no Forest Vision, no Elegant (less health and heavy-armour slowness).
- **Goblin is now Hobgoblin**: golden weapons no longer add damage (the loot bonuses and golden armour
  set bonus stay); no Nimble (speed bonus).
- **Siren**: no Flammable; Out of Your Depth now halves healing on land instead of stopping it.
- **Ogre is now Half-Ogre**: Stocky stays at +10 health (1.5x a human's), no Gargantuan size, no Sluggish.
- **Land Shark is now Sharkfolk**: no Slowness on land. **Kirin**: no Dislike of Water, Exhaustion halved.
  **Raptus is now Saurusfolk**: no armour limit. **Rat is now Ratfolk**: no daylight Blindness, no Slow
  Swimmer. **Shadow**: no Shadow Form, no Weakness of the Sun, can eat food (No Food and Shadow Snack
  removed). **Sporeling**: no armour limit, no longer evolves into Shroomling.
- **Classes**: Merchant no longer gets rare wandering-trader stock (Charisma); Rogue's Stealth no longer
  doubles damage from behind.
- Medieval Origins Revival is held at 6.6.0: the 6.7.x Forge builds target the Fabric Origins API (they
  crashed the test server at start) and need Icarus. 6.6.0 is the last build for Origins (Forge).
- Icarus (listed by Medieval Origins Revival for its winged races) is not installed: no race that needs
  it is on offer, and it would add flight.

### Other mods

- **Ars Elixirum 0.12.1** (with Fragmentum and Archivist) added.
- **Xaero's Minimap (Fair-Play) FP24.2.0** and **Xaero's World Map 1.38.8** added (client), and taken off
  the mod blacklist in `config/mod_whitelist-config.json`; the regular Xaero's Minimap stays blocked.
  FP24.2.0 is the last fair-play release, so the world map is the version released alongside it.
- **NiftyCarts** was not added: it only exists for Fabric. No cart mod (such as AstikorCarts) replaces it.
- `check_packs.py`: a library bundled in several jars now counts at its highest version, as Forge loads it.

### Performance

- **Noisium** has no safe replacement: its fork Noisiumed has the same code, both write terrain straight into
  chunk sections in a way the server's anti-xray mod also hooks, and the only other world-gen accelerator
  for Forge (C2ME's Forge port) is an alpha. Pre-generating the world with Chunky (already installed)
  remains the best way to avoid generation lag.
- New: **Smooth Boot (Reloaded)** (both);
  server: **Async Locator** (structure searches off the main thread), **Ksyxis** (faster world loading),
  **Let Me Despawn** (with Almanac: mobs that picked up items can still despawn), **Get It Together,
  Drops!** (merges dropped items); client: **Dynamic FPS** (throttles when the window is unfocused or
  minimised), **Cull Less Leaves Reforged**.
- **Canary** (Lithium port) was tried and dropped: on the test server its mixins failed against methods other
  mods make public, stopping the game from starting.
- ServerCore's **dynamic performance** is on: when the server averages more than 35 ms per tick it lowers
  view distance (not below 6), simulation and chunk-tick distance (not below 4) and mob caps (not below
  50%), and raises them again when it recovers.

### Client (Performance) pack

- A third pack, **MDVLCraft-<version>-client-performance.mrpack**, for weaker PCs. It has every gameplay
  mod the normal client has (so it joins the same server) but leaves out Oculus and the shader packs,
  Particle Rain, Falling Leaves, Sodium Dynamic Lights, AmbientSounds, AAA Particles World and the
  Better Leaves, Fancy Crops and Os' Colorful Grasses resource packs. Its defaults are lighter: Fast
  graphics, 8 chunk render distance, 6 simulation distance, reduced particles, no clouds or entity
  shadows, no biome blend, 2 mipmap levels, VSync off with a 90 FPS cap, fast leaves and weather, and
  Distant Horizons at 48 chunks, low quality, without SSAO or anti-aliasing.
- Defined by `modpack/client-performance.json` (entries removed from the client index) and
  `modpack/client-performance-overrides/` (files replacing the client's own); `build_mrpack.py` builds it
  next to the other two and `check_packs.py` checks it as a third pack.

### Compatibility

- **Jade Addons** (more Jade info, e.g. Lootr's per-player chests) and **Polymorph** (pick the result
  when two mods' recipes collide) added. Checked and skipped: Epic Knights already ships Epic Fight
  movesets, Farmer's Delight already ships Serene Seasons crop tags.

### Server test and log clean-up

Booted a fresh install of the server pack, pre-generated 2,601 chunks with Chunky (no errors, about 8
chunks/s on 4 cores), reloaded datapacks (no KubeJS errors) and checked TPS (20, 0.7 ms per tick idle).

- **Fewer warnings on a fresh install** (1,003 down to about 180): the pack now ships the config files
  mods otherwise create, and complain about, on first start (`overrides/config`, and `defaultconfigs`
  for new worlds).
- **Epic Knights pikes**: their Epic Fight moveset named a hit particle that does not exist (34 warnings);
  the Binder ships the moveset with Epic Fight's blade hit particle.
- **Domestication Innovation**: a filename typo meant its Blazing enchanted book never appeared in
  Nether fortress chests; the Binder ships the file under the name the mod looks for.
- **Iron's Spells 3.16.3** ships two chest loot tables in the 1.21 format (Catacombs crypt loot, Citadel
  tomes) that failed to load, leaving those chests without that loot; the Binder ships 1.20.1 versions.
- T.O Magic's Abyssal Ruins loot addition referenced an unregistered item and failed to load; it is
  emptied (T.O's structures and items are off anyway).
- Hobgoblin's Greedy no longer references a Dig Speed attribute this Origins build lacks.
- **Server now exits after `stop`.** Once a player had joined, a mod's thread pool kept the Java process
  alive after the world was saved, so restart scripts and hosts saw a hung server. The Binder now waits 15
  seconds after a dedicated server has fully stopped, logs which threads are still running and exits.

Client test (joined the test server with the client pack): the race picker (Alternate Origin GUI) lists
exactly the 15 races, renamed ones show their new names, Xaero's fair-play minimap and world map load,
and armour was checked in Epic Fight battle mode, including mid-attack: Cursed Fate (Fallen Sorcerer,
Shinjuku Gojo, Sorcerer Killer, Blessed, Prestigious, Brotherly Curse, Crow Sorcerer, feminine Jujutsu),
Epic Knights (Knight, Gothic) and Cataclysm (Ignitium) all render on the animated body. Fixed from the
client test:

- **Key conflicts from the new mods**: Y opened Xaero's minimap settings as well as switching Epic Fight
  mode, R opened Ars Elixirum's collection as well as casting abilities, Left Alt was both Epic Fight dodge
  and Shoulder Surfing free look. New defaults: Ars Elixirum collection N, Origins active powers G and H,
  view race ' (apostrophe), Xaero new waypoint , (comma) and waypoint list ; (semicolon), Shoulder
  Surfing swap shoulder J, Map Atlases pin End; Xaero minimap settings (use ]), Shoulder Surfing free look
  and camera nudges, Backpacked management, T.O armour abilities and Alex's Caves' special ability are
  unbound. Only affects new installs (options.txt is a default).
- **BD&Hill resource pack** is now saved as `BDHill-Reforge-the-Sword-Edge-2.0_1.20.1.zip`: its original
  name starts with full-width brackets, which failed to open on the test system and can on Windows
  systems that do not use UTF-8.
- Distant Horizons no longer posts mod-compatibility notes in chat on every join.
- **Smaller packs** (34 MB to about 22 MB): the patched T.O Magic jar no longer carries 13 MB of music for
  content the pack disables (the Nightwarden boss themes and the Eldritch Abyssamorph music disc); those
  three tracks are a short silent clip (`mdvlcraft-binder/tools/silence.ogg`).

- Remaining warnings come from mods themselves (client-only classes probed on the server, optional
  integrations for mods not installed, Medieval Origins powers for races that are not on offer) and do
  not affect play.

### Fixed

- **Game would not start (crash report 9 Oct 19:50, T.O Magic 'n Extras: `NoClassDefFoundError:
  DungeonEyeItem`).** T.O Magic 5.5.0 (and every newer T.O version) was built for an older
  L_Ender's Cataclysm and Iron's Spells; Cataclysm 3.31 moved or reshaped 12 classes it uses and Iron's
  3.16 moved 2. The pack now ships a T.O jar rewritten by `mdvlcraft-binder/tools/PatchTravelOptics.java`:
  moved classes are renamed in place, and the two particle types whose constructors changed and two
  removed Cataclysm config values go through the Binder (`compat.traveloptics.CataclysmCompat`). Every
  reference T.O makes into Cataclysm, Iron's Spells, Alex's Caves, Citadel, Apothic Attributes and
  Curios was checked to resolve.
- Skill tree document: only the regular style is kept (`docs/MDVLCraft-Skill-Trees.pdf`); the astrologer,
  manuscript and field guide variants and their code in `docs/build_skill_tree_doc.py` are removed.

## 1.9.11 (Binder 0.7.3)

Source changes only: the packs have not been built or tested in game. `python3 modpack/check_packs.py`
checks both packs statically (required dependencies and versions per side, client/server version match).

### Skill trees

- **Stamina bonuses doubled** in every tree: Endurance +0.5 Max Stamina (was +0.25), Evasion Training
  +1 (was +0.5), Evasion Training Mastery +2 (was +1); Second Breath +2% Stamina Regen (was +1%),
  Recovery +10% (was +5%), Recovery Mastery +20% (was +10%).
- **Samurai has no spells any more**: Blood Slash, Volt Strike, Flaming Strike and Blink are removed
  (their neighbouring nodes are joined so nothing is cut off; points spent on them are refunded). Its
  techniques (Stance, Blitz, Surprise Attack, Myriad Blades) stay.

### T.O Magic 'n Extras spells

- **T.O Magic 'n Extras 5.5.0** (Iron's Spells addon, CurseForge, shipped in `modpack/mods`) is installed only
  for its spells. 5.5.0 rather than the newest 6.3.0 because 6.x removed Cursed Blast. It requires
  **Alex's Caves 2.0.2**, which is installed for the same reason. For both mods everything else is off:
  - no structures (their structure sets are emptied in `kubejs/data`) and no Alex's Caves cave biomes
    (`config/alexscaves_biome_generation/*.json`, `disabled_completely`);
  - their mobs never spawn on their own (natural, spawners, structures, patrols, ...); creatures summoned
    by the spells below still appear;
  - their items are removed from recipes, loot, drops, trades and creative tabs, all of their recipes
    are removed, and their advancements are hidden.
- Spells, on the ability wheel through the skill trees like Iron's spells (mana ×0.6, cooldown ×1.5, as
  for Iron's own spells):
  - Berzerker: Blood Howl III. Assassin: Spectral Blink II. Phantom: Blackout I.
  - Wanderer: Ashen Breath V. Knight: Lingering Strain II. Druid: Aerial Collapse II.
  - Flame: Annihilation I, Meteor Storm III, Lava Bomb II. Ice: Cursed Blast I, Halberd Horizon III.
  - Necromancy: Cursed Revenants III, Axe Of The Doomed I.
- **New Water archetype** (Mage): Ice's tree with Aqua spell power and resistance in place of Ice's, and
  the Aqua spells that do not summon creatures: Hydroshot (III, and VI further in), Bubble Spray,
  Tidal Grasp, Flood Slash, Tsunami, Aqua Missiles, Coral Barrage, The Howling Tempest, Jet Steam,
  Serpentide, Rainfall, Floodgate, Vortex Of The Deep and Skypiercer. Echo of the Abyss (summons) is
  left out. The six Mage archetypes are spread evenly in the classes tab.
- Some T.O spells cost a lot of mana at this scaling (Annihilation 240, Vortex Of The Deep 120).

### Trades

- Wandering traders and villagers no longer sell Iron's Hither Thither Wand (it still exists otherwise).

### Mods added

Both packs: Apothic Attributes (AttributesLib) 1.3.7 + Placebo 8.6.3, Memory Leak Fix 1.1.5,
MmmMmmMmmMmm (target dummy) 2.0.12, Better Compatibility Checker 3.0.1 (set to "MDVLCraft 1.9.11" in
`config/bcc-common.toml`), PlayerRevive 2.0.31, Anvil Never Too Expensive 1.1, Stormie's Spiders 3.3.1.

Client only: Chat Animation [Smooth Chat] 1.3.4, Sounds 2.2.1 (+ YACL 3.6.6, MRU 1.0.4), Fastload
Reforged 3.4.0, World Play Time 1.2.3, Scribble 1.5.1, Smooth Gui 2.0.7, Neko's Enchanted Books 2.0.3,
Shut Up GL Error 2.0.0 (+ JamLib 1.3.6), RenderScale 1.0, Forgematica 0.1.13 (+ MaFgLib 0.1.14).

Server only: Better Than Mending 1.7.2, Too Fast 0.4.3.5, Does It Tick? 1.1.4 (+ TxniLib 1.0.24, Chunk
Activity Tracker 1.0.1), Gravestone x Curios API Compat 2.1.0, Loot Integrations 4.7 and Yung Structures
Addon for Loot Integrations 1.6 (CurseForge only, shipped in `server-overrides/mods`, like the YUNG
structure mods they extend).

Already in the pack: TerraBlender, Sound Physics Remastered, Clumps, Backpacked + Framework.
Not added: Backpacked: Wet Backpacks has no 1.20.1 version (1.21.1 and newer only).

### Mods removed

- **Tu Di Gong** (and its config). Existing Tu Di temples lose their Tu Di blocks.
- **Tectonic** (and its config). Terrain in existing worlds will not line up where new chunks meet old ones.

### Resource packs and shaders

- Default resource packs, bottom to top: Minecraft, **Ashen 16x**, mod resources (Ares HUD and other
  mods), **Fancy Crops**, **Better Leaves**, **Os' Colorful Grasses (Tall)**, **Low On Fire**,
  **Enchant Icons** (`options.txt`). All are pack format 15 (1.20.1).
- Shaders installed: **MakeUp - Ultra Fast** (on by default), **Photon** and **Complementary Reimagined**
  (pick in Video Settings > Shader Packs).

### Compatibility check

`check_packs.py`: every required dependency resolves at a valid version on both sides, no duplicate mod
ids, client and server ship the same versions, and no mod in either pack declares an incompatibility with
another. Things to watch in game (not tested):
- Memory Leak Fix overlaps with AllTheLeaks, which the pack already has.
- Apothic Attributes changes how some attributes and damage are calculated; check its config if armour or
  crits feel different.
- Stormie's Spiders reworks spiders; Epic Fight draws spiders with its own model in battle, so its look may
  not show there.
- RenderScale and shaders together can look blurry or misaligned; turn one off if so.

### Fixed

- **Crash rendering your own player (Antitheus; crash reports of 7 Oct 22:00, 22:31 and 9 Oct 16:10).**
  Epic Fight Compat inverts the player's Root joint; a Root scaled to nothing cannot be inverted and Epic
  Fight threw. The Binder now gives Epic Fight Compat "nothing to pose" instead (`ArmatureBinderMixin`).
- **Singleplayer/LAN crash from Nightfall's fire wind and summoned swords (9 Oct 09:00).** Nightfall
  expects Entity Culling 1.10's `Cullable`, which 1.11 moved. Entity Culling is pinned to 1.10.5 and the
  Binder only lets Nightfall use Entity Culling when that class exists (`CullableUtilMixin`).
- **Shooting Style did nothing on sneak + attack**: it captured its kick animations before they were
  registered (nulls). Refilled when the skill is equipped. The same bug broke the charge/Judgement Cut
  animations of Nightfall's Yamato, Ruins Greatsword and Meen Lance skills; fixed too.
- The 8 Oct 08:21 loading crash (Backpacked without Framework) was a 1.9.4 profile; 1.9.10 ships Framework.

### Changed

- Mining Fatigue is disabled.
- New players get a Book and Quill in their first-join kit.
- Entity Culling 1.11.2 → 1.10.5 (client).

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
