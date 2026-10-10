# Curios slots in MDVLCraft

Players currently have **2 Ring** slots, **1 Necklace** slot and **1 Head** slot (Head turned on in 1.9.13). All other slots are switched off by
`modpack/overrides/kubejs/data/irons_spellbooks/curios/entities/iss_entities.json` (carried over from the
1.9.9 sources). Because it uses `"replace": true`, it replaces every other mod's list of player slots with just
`ring`, `necklace` and `head`. This was checked in game on a test server: with the file removed, players get every slot
below.

Mark each slot **keep off** or **turn on**. Turning a slot on gives every player that many slots of it.

## Switched off now

| Slot | Slots per player | Added by | Items that go in it |
| --- | --- | --- | --- |
| Spellbook | 1 | Iron's Spells | Wimpy Spell Book, Flimsy Journal, Apprentice's Spell Book, Ironbound Tome, Enchanted Spell Book, Ancient Codex, Blaze Instruction Manual, Dragonskin Spell Book, Druidic Tome, Frostbranded Book, Grimoire of Evokation, Necronomicon, Rotten Spell Book, Vampiric Spell Book, Villager Bible, Legendary Spell Book; T.O Magic: Archive Of Abyssal Secrets, Chronicles Of The Firelord, Codex Of The Crushing Depths, Guide To Watery Whispers, The Accused Codex, Shellbound |
| Talent | 2 | T.O Magic | Aetherial Despair Ring, Firestorm Ring, Amulet Of Spectral Shift, Energy Unbound Necklace, Sigil of the Spider Sorcerer, Azure Ignition Bracelet, Cryostorm Bracelet, Hydrocharge Bracelet, Nightstalker's Band, Bottled Raincloud (the T.O Magic rings, necklaces, bracelets and charm can go here as well as in their own slot) |
| Bracelet | 2 | T.O Magic | Azure Ignition Bracelet, Cryostorm Bracelet, Hydrocharge Bracelet, Nightstalker's Band |
| Charm | 1 | T.O Magic | Bottled Raincloud and the ten Echoes (Aqua, Blood, Eldritch, Ender, Evocation, Fire, Holy, Ice, Lightning, Nature) |
| Belt | 1 | T.O Magic | Elytra-Jetpack Component, Pocket Black Hole |
| Hands | 2 | L_Ender's Cataclysm | Blazing Grips, Chitin Claw, Sticky Gloves |
| Rings (Cataclysm) | 2 | L_Ender's Cataclysm | Ring of Grudged (a separate slot from the Iron's/T.O ring slot) |
| Waist | 1 | L_Ender's Cataclysm | Belt of Beginner, Belt of Monstrosity |
| Feet | 1 | L_Ender's Cataclysm | Sturdy Boots |
| Talisman | 0 | L_Ender's Cataclysm | Unbreakable Skull. Cataclysm sets this slot to 0 itself, so it stays empty even when turned on unless its size is raised. |

## Switched on now

| Slot | Slots per player | Items that go in it |
| --- | --- | --- |
| Ring | 2 | Iron's Spells: Ring of Affinity, Signet of the Betrayer, Ring of Expediency, Ring of Recovery, Emerald Stoneplate Ring, Ring of Expulsion, Fireward Ring, Frostward Ring, Ring of Invisibility, Ring of the Lurker, Ring of Mana, Poisonward Ring, Silver Ring, Ring of Visibility, Wicked Bone Ring; T.O Magic: Aetherial Despair Ring, Firestorm Ring |
| Head | 1 | Cataclysm: Aptrgangr Head, Draugr Head, Kobolediator Skull |
| Necklace | 1 | Iron's Spells: Amethyst Resonance Charm, Amulet of Concentration, Conjurer's Talisman, Greater Conjurer's Talisman, Amulet of Warding, Heavy Chain, Amulet of Teleportation; T.O Magic: Amulet Of Spectral Shift, Energy Unbound Necklace, Sigil of the Spider Sorcerer; Cataclysm: Berserker Soul Amulet, Vitality Ankh |

## Not used by any item

Curios also defines **Back**, **Body** and the generic **Curio** slot, but no mod in the pack gives them to
players or puts items in them, so they never appear.

Villager Recruits' recruits have their own Curios slots; those are not affected by any of this.
