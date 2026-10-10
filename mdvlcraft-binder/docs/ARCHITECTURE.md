# MDVLCraft Binder: how it works

`mdvlcraft-0.7.1.jar` (modId `mdvlcraft`, display name "MDVLCraft Binder") is the glue mod of the
MDVLCraft pack (Forge 1.20.1 / 47.4.10). It has 97 classes (~5,300 lines decompiled) and a large data pack.
The source in `src/main/java` was decompiled from the released jar with Vineflower 1.11.1.

It does six jobs:

1. **Class and archetype skill trees** on top of Pufferfish's Skills (`puffish_skills`). These are mostly
   data, plus two custom reward types and the logic that opens archetype tabs.
2. **The ability wheel**: one 8-slot loadout that can hold Iron's Spells spells, Cursed Fate techniques,
   Epic Fight "slot skills" and the Samurai Stance. The trees unlock what can go on it.
3. **Epic Fight skills from the trees**: tree nodes teach or remove Epic Fight skills, and the mod has its
   own equip UI.
4. **14 custom attributes** that the trees grant (greatsword/katana/dagger/rapier damage, backstab,
   riposte, parry, dodge, plunder, saturation, nature regen, technique cost).
5. **Content control**: it turns off Iron's Spells' and Cursed Fate's own progression (spellbooks, scrolls,
   skill books, spawns, recipes, loot, trades, keybinds, packets) so the trees are the only way in.
6. **Polish and compat**: the "Slate" GUI style, a restyle of Villager Recruits' screens, Ponder scenes for
   weapons and ships, a BD&Hill model fix, and Distant Horizons/Chunky and Epic Fight Nightfall fixes.

---

## 1. Dependencies

Every mandatory dependency in `mods.toml`, with the jar that provides it in the pack and what the Binder uses
from it (from the imports):

| modId | Jar in pack | Used for |
|---|---|---|
| `epicfight` | epic-fight-20.14.17 | weapon categories, player patch + event listeners, skills/slots, network packets, animations |
| `irons_spellbooks` | irons_spellbooks-1.20.1-3.16.3 | `AbstractSpell`, `SpellRegistry`, `MagicData` (mana), cast API, `AttributeRegistry.MAX_MANA`, client magic data |
| `puffish_skills` | puffish_skills-0.19.1 | `SkillsAPI` (rewards, unlock/lock events, categories), `SkillsScreen` (mixin) |
| `puffish_attributes` | puffish_attributes-0.8.2 | data only: attributes in tree rewards |
| `cursedfate` | cursedfate-1.0.39 | `PlayerVariables` capability, `AbilitysProcedure`, domain entities, procedures/keybinds (mixins) |
| `cataclysm` | L_Enders_Cataclysm-3.31 | logging only (`ModEntities`); data: weapon capabilities |
| `naoyaaddon` | BaldasProjectionSorcery-2.0.1.2 (`com.bless.naoyaaddon`) | projection-sorcery variables and procedures (mixins) |
| `sword_soaring` | sword_soaring-20.14.2.8 (`net.p1nero.ss`) | flight item and keybind mixins; its skills are cast from the wheel |
| `efn` | EpicFight Nightfall-3.4.2 (`com.hm.efn`) | keybind and server-side VFX mixins; `efn:execution` cast from the wheel |
| `epic_fight_ponder` | overrides/mods/epic_fight_ponder-1.0.0 (`org.arc.epic_ponder`) | weapon Ponder scenes. It also carries **Ponder** and **Flywheel** inside it (jar-in-jar) |
| `iss_ponder` | iss_ponder-1.0.3 (`com.p1nero.iss_ponder`) | spell preview on hold-Ponder-key |
| `alekiships` | alekiNiftyShips-1.0.14 | ship Ponder scenes (item ids only) |
| `distanthorizons` | DistantHorizons-3.3.3 | mixin into its Chunky accessor |
| `chunky` | Chunky-1.3.146 | accessor for `ChunkyProvider.instance` |

Referenced only by data (tree rewards, weapon capabilities): Weapons of Miracles (`wom:`), Epic Fight Extra
(`epicfightx:`), EF-IS compat (`efs_iss:`), Combat Evolution, Dodge Parry Reward, Villager Recruits / Recruits
(GUI restyle), BD&Hill (resource fix).

Forge 47.4 features it relies on: `@Mod` constructor taking `FMLJavaModLoadingContext`, and
`ResourceLocation.fromNamespaceAndPath` / `ResourceLocation.parse` (backported helpers).

---

## 2. Bootstrap (`MDVLBinder`)

Mod constructor:
- `DisabledContentSetup.register`: biome modifier serializer `mdvlcraft:strip_disabled`; on common setup it
  unregisters 8 Cursed Fate event listeners (quests, technique-on-join, death/chest/damage procedures).
- `BinderAttributes.register`: 14 attributes, all added to `EntityType.PLAYER`.
- Client only: `ClientContentSetup` (Ponder plugins, drops Cursed Fate's `CtrlDashHandler`) and
  `ClientAbilitySetup` (keys, HUD overlay, Slate/Recruits/BD&Hill hooks, spell-preview ticker).
- `EpicFightIntegration` / `CataclysmIntegration`: log counts at setup.

Common setup: `BinderNetwork.register()`, then on the main thread `AbilityReward.register()`,
`EpicSkillReward.register()`, `ArchetypeTabs.register()`.

`@EventBusSubscriber` (Forge bus): `AttributeEffects`, `ArchetypeTabs`, `AbilityEvents`, `Stances`,
`EpicSkillEvents`, `ContentEvents`, `MdvlCommand`.

---

## 3. Skill trees (the main system)

### 3.1 Data layout

`data/mdvlcraft/puffish_skills/config.json` lists 17 categories: `classes` plus 16 archetypes.
Each category folder has `category.json`, `skills.json` (node positions), `definitions.json` (what a node
is and grants), `connections.json` (edges, all `normal.bidirectional`) and, for archetypes, `experience.json`.

**`classes` tab**: unlocked by default, 2 starting points, `spent_points_limit: 2`.
- 4 class roots (`warrior`, `mage`, `rogue`, `ranger`, cost 0, no rewards), joined to 18 archetype nodes (cost 1, no rewards).
- `ArchetypeTabs.onLogin` force-unlocks the 4 class roots on every login.
- Unlocking an archetype node (`SkillsAPI.registerSkillUnlockEvent`) unlocks the category
  `mdvlcraft:<archetype>`. Locking it (respec) **erases** that category (`Category.erase`). An archetype
  node with no matching category throws `IllegalStateException`.

| Class | Archetypes |
|---|---|
| Warrior | knight, samurai, berzerker, monk |
| Mage | holy, flame, lightning, necromancy, ice, water |
| Rogue | assassin, phantom, scout, thief |
| Ranger | wanderer, stalker, druid, cursed |

**Archetype tabs**: `unlocked_by_default: false`, `starting_points: 1`. Experience comes from
`puffish_skills:kill_entity`: `dropped_xp + max_health / 5`, anti-farming 20 kills per chunk per 300 s.
Level curve: `25 + level * 5`, no cap. Each tree has 347–443 nodes on a radial layout (radius ~1,500).

Node taxonomy (the frame texture and `extra_description` show it):

| Kind | Frame | Size | Cost | Content |
|---|---|---|---|---|
| root | `frame_<class>_root` | 1.5 | 1 | archetype's signature bonus |
| twig | `frame_<class>_twig` | 0.75 | 1 | small generic bonus ("Twig bonus (every archetype has a few of these)"): Vitality, Might, Endurance, Swiftness, Hearty, Bounce, Mending, Meditation, Attunement, Second Breath, Willpower, Readiness, Focus, Hardened, Footing … |
| task | `frame_<class>_task` | 1.5 | 1–2 | spoke steps: core spokes (every archetype), class spokes (every archetype of that class), archetype stat spokes |
| goal | `frame_<class>_goal` | 1.5–2.0 | 2–3 | `…_mastery_N` / `…_mastery_i/ii/iii` nodes, and Epic Fight skills |
| challenge | `frame_<class>_challenge` | 2.0 | 2–5 | spells, techniques, top Epic Fight skills |

Across all 16 trees there are 6,263 nodes. Rewards: 6,066 `puffish_skills:attribute` (84 distinct attributes,
`addition` or `multiply_base`), plus the Binder's two custom reward types below. The PDF
`MDVLCraft-Skill-Trees.pdf` totals these per tree.

### 3.2 Custom reward `mdvlcraft:ability` (`AbilityReward`)

```json
{ "type": "mdvlcraft:ability", "data": { "ability": "irons_spellbooks:firebolt", "level": 3 } }
```
- `ability` is resolved with `Ability.byId` (Technique → SlotSkillAbility → Stance → Iron's spell).
  Unknown ids fail the data load.
- `level`: spells allow 1..∞; every other ability only allows 1.
- `update(ctx)`: `AbilityGrants.set(player, reward, count > 0)`. Granting a spell that `requiresLearning()`
  also learns it in Iron's `SyncedSpellData`. Removing the Stance reward turns the stance off.
- When several nodes grant the same ability, the **highest level** wins (`AbilityGrants.of` merges with `max`).
- Grants live in memory (`Map<UUID, Set<AbilityReward>>`). Puffish replays `update` on login, and dirty
  players are synced at the end of each server tick (`SyncAbilitiesPacket` + technique cooldowns).

### 3.3 Custom reward `mdvlcraft:epicfight_skill` (`EpicSkillReward`)

```json
{ "type": "mdvlcraft:epicfight_skill", "data": { "skill": "wom:ender_obscuris" } }
```
- Needs a registered Epic Fight skill whose category is learnable. Each one is added to `TREE_SKILLS`.
- `EpicSkillGrants.sync` (end of tick, for dirty players): for every tree skill, learn it if granted and not
  learned (`SPAddLearnedSkill`). Forget it if learned and not granted: unequip it, remove it, send
  `SPRemoveSkillAndLearn`. Every player always learns Nightfall's souls-like dodge and step (`efn:efn_dodge`,
  `efn:efn_step`), Guard, Parrying and Absolute Deflection (`efn:efn_parry`); Dodge and Guard slots are filled
  with the souls-like dodge and Guard when empty. Roll and Step (what everyone got before 0.7.3) are forgotten
  unless a tree grants them, and a dodge slot holding one gets its souls-like counterpart.
- So **tree skills can only be held through the tree**: a tree skill learned any other way is removed.

### 3.4 Tree UI tweaks (client mixins on Puffish)

- `SkillsScreenMixin`: Slate window/tab textures (`textures/gui/skills/window.png`, `tabs.png`), grey tints
  on node icons by state (locked/available/affordable/excluded), title text colour. When a node is hovered it
  calls `SpellPreviews.hoverSkill(category, skillId)`, and holding the Ponder key for 12 ticks opens an Iron's
  spell preview. Which nodes have a preview is set in `assets/mdvlcraft/spell_previews.json`
  (`"mdvlcraft:<tab>/<node>": {spell, level}`). Hovered nodes show their point cost after the title
  (redirect of `ClientSkillDefinitionConfig.title()` in `lambda$drawContentWithCategory$21`, Puffish 0.19.1).
- `ConnectionBatchedRendererMixin`: bidirectional connections are drawn as quadratic Bézier curves with a
  deterministic bend (hash of the endpoints), 3 px stroke + 1 px fill, ~6 px segments.

---

## 4. Ability wheel

### 4.1 Types (`Ability`, sealed)

| Type | Ids | Cast path | Cost | Cooldown |
|---|---|---|---|---|
| `SpellAbility(AbstractSpell)` | any Iron's spell id | `spell.attemptInitiateCast(EMPTY, level, …, CastSource.SPELLBOOK, …)`. Cancels a different spell already casting | Iron's own | Iron's own |
| `Technique` (enum) | `mdvlcraft:dismantle, cleave, malevolent_shrine, open_malevolent_shrine, boogie_woogie, block_clap, blitz, surprise_attack, follow_up_kick, phantom_movement` | runs **Cursed Fate**'s `AbilitysProcedure` with `AbilityNum1 = <Cursed Fate ability id>` | Binder mana, reduced by `technique_efficiency` | Binder's own, stored in player NBT |
| `SlotSkillAbility` (enum) | `mdvlcraft:myriad_blades` (`sword_soaring:wan_jian_gui_zong`), `babylonian_armory` (`sword_soaring:babylon`), `celestial_array` (`sword_soaring:rain_sword`), `soul_hunt` (`efn:execution`), `gravity_stomp` (`efn:stomp`) | equips the Epic Fight skill in its category's slot if needed, then `skill.executeOnServer` | flat mana | the Epic Fight skill's own |
| `StanceAbility.INSTANCE` | `mdvlcraft:stance` | `Stances.press` | 0 | none |
| `DoppelgangerAbility.INSTANCE` | `mdvlcraft:doppelganger` | `Doppelgangers.press`: summon; again while looking at a creature, teleport behind it; sneak, dismiss | 1 mana per second while out | none |

Technique table (`Technique` constructor: name, Cursed Fate ability id, mana, cooldown s, held, icon):

| Technique | CF id | Mana | CD | Held | Notes |
|---|---|---|---|---|---|
| Dismantle | 26 | 12 | 1 | yes | re-casts every time its cooldown ends while held; "slash", feeds the shrine |
| Cleave | 27 | 20 | 3 | yes | same |
| Malevolent Shrine | 31 | 100 | 120 | no | closed domain (variant 0); free when near your open domain; recast closes it |
| Malevolent Shrine: Open | 31 | 100 | 120 | no | variant 1 |
| Boogie Woogie | 32 | 10 | 2 | no | |
| Block Clap | 33 | 10 | 2 | no | |
| Blitz | 60 | 15 | 8 | no | weapon-free (see `ItemStackMixin`) |
| Surprise Attack | 66 | 15 | 15 | no | weapon-free |
| Follow Up Kick | 114 | 15 | 6 | no | naoyaaddon projection; sneak+cast toggles Normal/Flow |
| Phantom Movement | 118 | 20 | 0 | no | naoyaaddon projection; sneak+cast is a free mode toggle |

### 4.2 Flow

```
Client: AbilityKeys (Caps Lock = cast, [ = open abilities, ` = wheel; GLFW 280/91/96)  →  CastPacket(pressed, selectedSlot)
Server: AbilityActions.press → level check (AbilityGrants) → by type:
          SpellAbility → Iron's cast        Technique → TechniqueRunner.press
          SlotSkillAbility → .cast           StanceAbility → Stances.press
        CastPacket(false) → AbilityActions.release → TechniqueRunner.release (clears held + CF hold flags)
```
- **Loadout** (`Loadout`): 8 slots in `persistentData.PlayerPersisted["mdvlcraft:loadout"]` (a ListTag of
  ids, `""` = empty). This survives death. Assigning needs the ability to be granted.
- **Selection** is client-only (`ClientAbilities.selected`), chosen with the wheel. The server never
  stores it; each `CastPacket` carries the slot.
- **TechniqueRunner.cast** (server): shrine checks (wind-up, recast closes the domain) → cooldown →
  mana (creative is free) → writes Cursed Fate `PlayerVariables`: `CursedEnegry = max(CE, CEMax)`,
  `InnateMastery = 0`, `AbilityNum1`, `holdability(1) = true` → for projection techniques sets naoyaaddon
  variables → runs `AbilitysProcedure.execute` (inside `WeaponFreeCasts.run` for Blitz and Surprise Attack) →
  starts the cooldown, unless it was the Phantom Movement toggle → for shrines, saves the variant and starts
  `DomainUpkeep`.
- **DomainUpkeep** (each server tick): tracks shrines through a 100-tick wind-up, then keeps the owner's
  Cursed Energy topped up to `DomainCECost + CEMax` for up to 400 ticks (20 s) while the domain entity exists
  within 27 blocks.
- **Shrine slashes**: `ShrineOnTickProcedureMixin` and `SureHitEffectInDomainAreaProcedureMixin` suppress
  the domain's automatic sure-hit unless the owner is currently **holding** Dismantle or Cleave.
- **Mana** for techniques and slot skills comes out of Iron's `MagicData` and is synced with `SyncManaPacket`.

### 4.3 Stance (Samurai)

`StanceElement`: Fire +15% attack speed (`MULTIPLY_TOTAL`), Wind +0.4 `mdvlcraft:dodge_distance`,
Water +0.5 `mdvlcraft:parry_window`, Earth +15% attack damage. The element is a byte at
`PlayerPersisted.mdvlcraft_stance`. Active state is in memory (`Stances.ACTIVE`), so relog or respawn turns it
off. Cast toggles it, sneak+cast cycles the element. `StanceSyncPacket` goes to tracking players and the
player; the client recolours Epic Fight weapon trails (`AnimationTrailParticleMixin`) in the element colour.

### 4.4 Client UI

- `AbilityScreen` (key `[`): granted abilities grid (8 columns), the 8 wheel slots (click ability then slot;
  right-click clears), and on the right the `CombatSkillsPanel`, which equips Epic Fight skills per learnable
  slot (sends `EquipSkillPacket`; the server checks category, learned state and replace cooldown).
- `WheelScreen` (hold `` ` ``): radial picker. Releasing the key selects the hovered slot; right-click
  opens `AbilityScreen`.
- `AbilityHud`: overlay above the hotbar (bottom right) with the selected slot, icon, name, mana bar and
  cast-progress bar.
- `AbilityDisplay`: icons, cooldown sweep, spell tooltips through Iron's `TooltipsUtils`.
- `SlateGui`: the shared flat "Slate" drawing kit (panel, slot, bar, disc, button, palette).

---

## 5. Custom attributes (`BinderAttributes`)

All are `RangedAttribute(default 0, min 0, max …)`, synced, and added to players.

| Attribute | Max | Effect (where) |
|---|---|---|
| `greatsword_damage` | 5 | +x melee damage with EF GREATSWORD (`AttributeEffects.onHurt`) |
| `katana_damage` | 5 | UCHIGATANA or TACHI |
| `dagger_damage` | 5 | DAGGER |
| `rapier_damage` | 5 | SWORD with empty off-hand |
| `backstab_damage` | 5 | attacker more than ~120° behind the target's facing |
| `riposte_damage` | 5 | first hit within 60 ticks of an EF `DODGE_SUCCESS_EVENT` |
| `parry_stamina` | 1 | fraction of max stamina restored on a parried hit (EF `TAKE_DAMAGE_EVENT_ATTACK`) |
| `parry_window` | 3 | scales `ParryingSkill.PARRY_WINDOW` (`ParryingSkillMixin`) |
| `dodge_distance` | 2 | scales dodge-animation horizontal movement (`ActionAnimationMixin`) |
| `dodge_efficiency` | 0.9 | cuts stamina cost of `DodgeSkill` (EF `SKILL_CONSUME_EVENT`) |
| `saturation_bonus` | 5 | extra saturation on finishing food |
| `plunder` | 1 | chance to duplicate one random drop of a non-player kill |
| `nature_regen` | 10 | heal x every 100 ticks when standing on `BlockTags.DIRT` (`f_144274_`) under open sky (verify the tag after remapping) |
| `technique_efficiency` | 0.9 | cuts technique mana cost |
| `unarmed_damage` | 20 | Monk: flat damage added to melee hits that are unarmed (`monk.Fists.unarmed`: empty hand, a non-weapon item, `#mdvlcraft:unarmed_weapons`), before the multipliers |
| `fist_mastery` | 5 | Monk: +x melee damage when fisted (unarmed, `#mdvlcraft:gloves`, or an Epic Fight FIST-category weapon) |
| `unarmed_parry` | 1 | Monk: 1 lets Absolute Deflection be raised with fists (`monk.UnarmedParry`) |

"Melee" means damage type `PLAYER_ATTACK` (`f_268464_`, verify after remapping) with the direct entity
being the player. The weapon bonuses add together into one multiplier: `amount * (1 + sum)`.

---

## 6. Content control (`content` package + mixins)

`DisabledContent`:
- **Disabled namespaces**: `irons_spellbooks`, `cursedfate`. Their natural spawns are cancelled
  (`FinalizeSpawn`) and removed from biomes (`StripDisabledBiomeModifier`, REMOVE phase), their advancements
  lose `display` (hidden), and their **serverbound custom packets are dropped** (`NetworkHooksMixin`).
- **Content namespaces** (`CONTENT_NAMESPACES`, `isContentDisabled`): `traveloptics` (T.O Magic 'n Extras,
  installed for its spells) and `alexscaves` (its dependency). All their items count as forbidden (below),
  all their recipes are dropped by id, and their advancements are hidden. Their structure sets are emptied
  and Alex's Caves biomes switched off in the pack's `kubejs/data` and `config/alexscaves_biome_generation`.
  T.O spell mana/cooldown overrides live in `data/traveloptics/irons_spellbooks_spell_config`.
- **Untradeable items** (`isUntradeable`): forbidden items plus `irons_spellbooks:hither_thither_wand`,
  removed from merchant offers only.
- **No-spawn namespaces** (`isSpawnBlocked`): `cursedfate`, `wom`, `traveloptics`, `alexscaves`. Removed from biomes, and cancelled for
  every spawn nobody asked for (natural, chunk generation, structure, spawner, patrol, event, reinforcement,
  jockey). Summons, spawn eggs and commands still work.
- **Forbidden items**: any `ISpellbook`, `IScroll`, `EldritchManuscript`, EF `SkillBookItem`, plus
  `irons_spellbooks:scroll_forge`, `inscription_table`, `cursedfate:cursed_shards`, `minecraft:elytra`.
  They are removed from recipes (`RecipeManagerMixin`, matched on the recipe JSON `result`), loot
  (`ForgeHooksMixin.modifyLoot`), mob drops, villager trades (cleared when a merchant is interacted with),
  and creative tabs. Right-clicking a block whose item is forbidden is cancelled.
- `CursedFateTerrain` + `LevelMixin.setBlock`: on the server, refuses to set a non-air block to air when a
  `cursedfate.*`/`cursefate.*` class is on the call stack (no terrain destruction from techniques), unless the
  block is Cursed Fate's own. It uses `StackWalker` on **every** server air-set, so it costs some performance.
- Keybind takeover (client mixins + `ForeignKeyMappings.unbind`): unbinds and does not register Cursed Fate's
  ability, menu and combat keys, Iron's spell wheel, cast and quick-cast keys, Epic Fight's `SKILL_EDIT`, Sword
  Soaring's take-off, acceleration, switch-mode and sword-skill keys, and EFN's arts key.
- `SpellSelectionManagerMixin`: Iron's spell selection never lists spellbook or item spells (casting is wheel-only).
- `CPChangeSkillMixin`: ignores Epic Fight's own client "change skill" packet (equip only via the Binder UI).
- `VatanseverItemMixin`: Sword Soaring's flying sword can't take off or elytra-fly.
- naoyaaddon mixins: no addon cooldowns (the Binder has its own), Barrage reads Output=100 and CE=1,000,000,
  "Flow" for Dodge comes from the Binder toggle, projection dash has no screen shake or wind flash, and the
  projection-style toggle packet is ignored (Naobito style only).
- `EffekUnitsMixin`: EFN VFX are off on dedicated servers. `AbstractChunkyAccessorMixin`: Distant Horizons
  waits for Chunky's instance before first-time setup. `AnimationEventMixin`: swallows `ClassCastException`
  from player-only animation events played on Ponder dummy players.

---

## 7. Ponder, resource packs, misc

- `WeaponPonderPlugin`: for every Epic Fight weapon type not already covered by Epic Fight Ponder, adds a
  combo showcase scene (bows and crossbows get an aim/shoot/reload scene). `WeaponTypeReloadListenerMixin` and
  `ClientContentSetup.onLoggingIn` re-index Ponder when weapon types sync.
- `ShipPonderPlugin` / `ShipPonderScenes`: rowboat and sloop slipway build guides for Aleki's Nifty Ships
  (`assets/mdvlcraft/ponder/*.nbt`).
- Built-in resource packs: `resourcepacks/slate` (Recruits GUI textures; always on, top) and
  `resourcepacks/bdhill_fix` (added when a "BDHill" pack is in the selected list).
- `SlateRecruits` + `AbstractButtonMixin` + `GuiGraphicsMixin`: while a Recruits screen is rendering, buttons,
  fills and the default label colour are swapped to the Slate palette.
- `/mdvl` (op level 2): `abilities <player>`, `assign <player> <1-8> <ability>`, `clear <player> <slot>`,
  `cast <player> <slot>`, `release <player>`, `weapons` (writes `weapons.tsv`: every weapon-like item and its
  Epic Fight support).
- Data overrides shipped in the jar: Iron's spell configs (83 spells, mana multiplier and cooldown),
  Epic Fight weapon capabilities for Iron's, Cursed Fate and Cataclysm weapons, and EF mob patches for Iron's
  summons.

---

## 7a. Compatibility fixes (added in 0.7.2)

| Class | Fixes |
|---|---|
| `mixin.epicfight.ColliderMixin` | An attack animation that names a joint the attacker's armature does not have (Super Golem's Golem Heart swung by a recruit) now hits nothing instead of throwing and crashing the server. |
| `mixin.wom.ReuseableEventsMixin` + `compat.wom.EnderObscurisTeleport` | Replaces WoM 2.0.171's Ender Obscuris teleport events (`lambda$static$23`, `lambda$static$21`), whose unbounded search loop could hang the server. Each handler checks the animation it fired for, so a WoM update that renumbers lambdas does not hijack other events. |
| `client.TrueInvisibilityRender` | HIGHEST-priority `RenderLivingEvent.Pre` listener that hides entities with Iron's `true_invisibility` before Epic Fight's battle-mode renderer draws them. |
| `compat.issponder.PreviewCasts` | Un-cancels `SpellPreCastEvent` for ISS Ponder's FakePlayer in `iss_ponder:spell_preview` (EF x Iron's Compat cancelled every preview cast). |
| `mixin.villagerecruits.*` + `compat.villagerecruits.AiVillageBuilding` | Redirects `VillageFactionManager.autoBuildsCity` in the city planner and the builder manager so AI factions do not build. Toggle: `stopAiVillageBuilding` in `config/mdvlcraft-common.toml` (`config.BinderConfig`). |

WoM and Village Recruits are compile-only dependencies (`libs/compat.txt`); their mixins are
`@Pseudo` with `require = 0`, so the Binder still loads without them.

## 7b. Gameplay changes (added in 0.7.3)

| Class | What |
|---|---|
| `combat.InvertedSpear` + `combat.BinderEffects` | A melee hit with `cursedfate:inverted_spearof_heaven` removes every effect from the target in `LivingAttackEvent` (LOWEST; before armour and Resistance are applied), then gives it `mdvlcraft:sealed`, which denies every other effect (`MobEffectEvent.Applicable`). Each hit strips and seals again. Length: `combat.invertedSpearSealSeconds` (4). |
| `combat.TechniqueScaling` | Damage of type `cursedfate:*` dealt by a player is multiplied by (player's attack damage + enchantment bonus) / `combat.techniqueReferenceDamage` (6 = iron sword), at least `combat.techniqueMinimumFactor` (0.5). |
| `combat.KatanaWeapons` | What counts for `mdvlcraft:katana_damage`: Uchigatana and Tachi categories, any category whose name contains katana/tachi/yamato/murasama/..., items in `#mdvlcraft:katanas`, and items whose id contains one of those words. |
| `compat.villagerecruits.RecruitBuffs` | Denies beneficial effects whose direct `addEffect` caller is in `com.talhanation.recruits` or `com.example.villagerecruits` (morale, elite, leader and travel buffs) on non-players; removes beneficial effects of an hour or more from recruits as they load. Potions, spells and beacons still work. Toggle: `villageRecruits.stripRecruitBuffs`. |
| `content.MobGear` | Mobs in `#mdvlcraft:armed_mobs` (zombies, husks, zombie villagers, drowned, wither skeletons) may get a melee weapon from the `epicfight` and `magistuarmory` (Epic Knights) namespaces; mobs in `#mdvlcraft:armoured_mobs` (those plus skeletons and strays) may get vanilla or Epic Knights armour no better than iron in defence, toughness and knockback resistance. Armour above that is swapped out. Weapons are limited to iron-tier or weaker unless `mobGear.weaponsUpToIron = false`. Marked in `FinalizeSpawn`, equipped on `EntityJoinLevelEvent` (after vanilla's own equipment roll). |
| `epicskill.EpicSkillGrants` | See 3.3: souls-like dodge/step replace Roll/Step; everyone learns Absolute Deflection. |
| `mixin.efn.EFNParryingSkillMixin` | Absolute Deflection's parry window × (1 + `mdvlcraft:parry_window`), like `ParryingSkillMixin` does for Parrying, so the Water stance lengthens both. |
| `mixin.epicfight.SkillBookScreenMixin` | No "You need to equip X first": `getPriorSkill()` reads as null in the skill book screen, the only place Epic Fight checks it. |
| `doppelganger.*`, `client.doppelganger.*` | The Phantom's Doppelganger: an invulnerable, unsaved `PathfinderMob` with an Epic Fight `HumanoidMobPatch` (biped armature). It copies its owner's gear every tick, keeps to the owner's side, flanks the owner's Epic Fight target (or whatever the owner hit in the last 5 s), and plays every animation the owner starts (`ACTION_EVENT_SERVER` listener). Its damage source and attacks go through the owner's patch. `Doppelgangers` tracks one per player, charges 1 mana per second and dismisses it when mana runs out, on logout, dimension change or death. Rendered with the owner's skin and arm width from the tab list (`DoppelgangerRenderer`, `PDoppelgangerRenderer` with the Biped or Alex mesh). |

Tree changes in 0.7.3 (data only): Ice Frost Step → level 5; Assassin Blood Step → level 5, Celestial Array node
replaced by Spider Techniques, Shadow Step added; Phantom Babylonian Armory node replaced by All Eyes on You,
Doppelganger added; Samurai + Parry Master; Knight + Gravity Stomp (wheel); Flame + Avatar of Might;
Berzerker + Dread Full Buster (`wom:buster_parade`); Cursed + Wither Skull (level 3); the classes-tab Assassin,
Phantom, Scout and Thief nodes also teach Precise Parry. The skill-tree edits were made with a one-off script;
new nodes hang off an existing node next to a related skill, placed in the clearest spot nearby.

## 7c. Monk (added after 0.7.8's first release)

| Class | What |
|---|---|
| `ability.MonkArts` | Focus (next strike within 10 s is a Black Flash: x2.5, Cursed Fate stun, sound, sparks) and Knockout (plays Epic Fight's `FIST_AUTO3`; what it hits in the next 12 ticks takes x1.5 and `traveloptics:blackout`; if nothing was hit, the looked-at target is hit directly). Also sets the damage of the Monk's Cursed Fate arts (Heavy Blow, Barrage, Uppercut, Follow Up Punch, Leaping Crush) to a multiple of the Monk's punch for a short window after the cast; `combat.TechniqueScaling` stays out of those hits. |
| `Technique` | Monk entries: CF ids 8 (Heavy Blow), 9 (Barrage, held), 10 (Uppercut), 11 (Follow Up Punch), 63 (Leaping Crush, weapon-free); Focus and Knockout have id -1 (`custom()`) and run in `MonkArts` through `TechniqueRunner`'s mana and cooldown. |
| `monk.Fists` | What counts as unarmed / fisted for the Monk's attributes. |
| `monk.UnarmedParry` | On load complete, adds FIST guard, parry and guard-break motions to Nightfall's `EFN_PARRY` and Epic Fight's `GUARD` and `PARRYING` (reflection on `GuardSkill`'s maps); they return null unless the player has `unarmed_parry`, so only Monks can guard with fists. |
| `mixin.invincible.InputManagerMixin` + `client.MonkGuardInput` | Weapons with an Invincible combo moveset (Feral Claws) take every press of their combo keys as combo input, right click included, which kept Guard from being raised; for a Monk with fists or claws the Guard key is left to Epic Fight. |
| `content.AddItemModifier` | Global loot modifier `mdvlcraft:add_item` (item, chance, conditions). Used for Feral Claws: 2% of `minecraft:chests/jungle_temple` (YUNG's temples use it for most chests too) and 3% of `betterjungletemples:chests/treasure`. |

The Monk tree is generated by `tools/monk_tree.py` from Berzerker's layout; rerun `tools/astro_theme.py` after it.

---

## 8. Network (channel `mdvlcraft:main`, protocol "5")

| # | Packet | Dir | Payload |
|---|---|---|---|
| 0 | `SyncAbilitiesPacket` | S→C | granted `Map<id, level>`, 8 × `Optional<id>` loadout |
| 1 | `AssignSlotPacket` | C→S | slot, `Optional<id>` |
| 2 | `CastPacket` | C→S | pressed, slot |
| 3 | `EquipSkillPacket` | C→S | EF slot universal ordinal, `Optional<skill id>` |
| 4 | `StanceSyncPacket` | S→C | entity id, element, active |
| 5 | `TechniqueCooldownsPacket` | S→C | `Map<technique id, remaining ticks>` |
| 6 | `RespecPacket` | C→S | archetype to reset, or from/to archetypes to change |

New packets must be appended (ids are positional) and the protocol string bumped.

## 9. Persistent player data

| Key | Where | Content |
|---|---|---|
| `PlayerPersisted.mdvlcraft:loadout` | `getPersistentData()` | ListTag of 8 ability ids |
| `PlayerPersisted.mdvlcraft_stance` | same | byte, `StanceElement` ordinal |
| `mdvlcraft:technique_ready` | `getPersistentData()` (top level, so **not** kept through death) | `{technique id: game time when ready}` |
| `mdvlcraft:kick_flow` | `getPersistentData()` (top level, not kept through death) | boolean |

Everything else (grants, held technique, active stance, domains, riposte windows) is in memory and is rebuilt
from Puffish on login.

---

## 10. Where to extend

| Goal | What to touch |
|---|---|
| New stat node / rebalance | the tree's `definitions.json` + `skills.json` + `connections.json`; any attribute id works in `puffish_skills:attribute` |
| New custom stat | `BinderAttributes` (register; added to players automatically) + its effect in `AttributeEffects` or a mixin + lang `attribute.mdvlcraft.<name>` + a twig icon |
| New Iron's spell on a tree | `mdvlcraft:ability` reward with the spell id and level (+ `spell_previews.json` entry; + `data/irons_spellbooks/irons_spellbooks_spell_config/<spell>.json` to tune) |
| New Cursed Fate / naoyaaddon technique | `Technique` enum entry (CF ability id, mana, cd, held, icon) + lang name/desc; special handling in `TechniqueRunner` if needed |
| New Epic Fight art on the wheel | `SlotSkillAbility` entry (skill id, mana, icon) + lang |
| New passive Epic Fight skill | `mdvlcraft:epicfight_skill` reward (any learnable skill id) |
| New archetype | category folder + entry in `config.json` + node in `classes` (skill id = category name) + connection to its class; `ArchetypeTabs` needs no change |
| New class | `classes` root node + add the id to `ArchetypeTabs.CLASS_SKILLS` (otherwise it is treated as an archetype and must have a tab) |
| Block more content | `DisabledContent.NAMESPACES` / `FORBIDDEN_ITEMS` |

The tree JSON (about 400 KB per tree) was clearly machine-generated. The generator is not in the jar. For big
tree changes it is worth writing a generator or editor (in `tools/`) rather than editing by hand.

---

## 11. Things noticed while reading (unverified until the dev environment runs)

- `AttributeEffects.onJoinLevel` adds three Epic Fight listeners on **every** `EntityJoinLevelEvent` for a
  player (dimension change, respawn). They use a fixed UUID, so whether they stack depends on Epic Fight's
  `addEventListener`. It also throws if the patch is missing.
- Stance modifiers go on with `AttributeInstance.m_22118_`. If that is `addPermanentModifier`, the
  modifier is saved in player NBT while `Stances.ACTIVE` is cleared on logout, so a stance left on at
  logout could stay applied but show as off. Check after remapping.
- Technique cooldowns are stored outside `PlayerPersisted`, so dying resets them (and the Follow Up Kick
  mode).
- Static maps (`AbilityGrants`, `EpicSkillGrants`, `TechniqueRunner.HELD`, `DomainUpkeep.DOMAINS`,
  `AttributeEffects.RIPOSTE_UNTIL`) are never cleared on server stop. That matters in single-player when
  switching worlds.
- `LevelMixin` stack-walks on every server-side block removal (see section 6).
- Known issues in the repo README ("Ender Obscurous crashes", "Antithisus skill crashes", "Invisibility not
  working in combat mode", "TuDi crashes the server", "punishment kick") touch tree-granted skills: Ender
  Obscuris is a `wom:` skill granted by the Assassin and Phantom trees, and Punishment Kick is granted by
  Berzerker. These are worth reproducing in the dev client once it runs.
