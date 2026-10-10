"""Origins datapack for MDVLCraft: which races exist and how they are changed. Writes the overrides in
modpack/overrides/kubejs/data. Usage: python3 modpack/origins_overrides.py <dir with the unzipped Origins,
Medieval Origins Revival, Origins++ and Origins: Classes jars, one sub-folder per jar>."""
import copy, glob, json, shutil
from pathlib import Path
X = Path(__import__('sys').argv[1])  # folder holding the unzipped origin mod jars, one sub-folder each
OUT = Path(__file__).resolve().parent / 'overrides/kubejs/data'
PRIORITY = 100  # above every mod's loading_priority, so these files win
written = []

def src(ns, kind, path):
    hits = glob.glob(str(X / '*' / 'data' / ns / kind / f'{path}.json'))
    assert hits, (ns, kind, path)
    return json.load(open(hits[0]))

def write(ns, kind, path, data):
    p = OUT / ns / kind / f'{path}.json'
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    written.append(p)

def pack_mod_ids():
    import re, zipfile
    ids = set()
    for jar in (Path(__file__).resolve().parent.parent / 'mdvlcraft-binder/libs/modpack').glob('*.jar'):
        try:
            toml = zipfile.ZipFile(jar).read('META-INF/mods.toml').decode('utf-8', 'replace')
        except (KeyError, zipfile.BadZipFile):
            continue
        ids.update(re.findall(r'^\s*\[\[mods\]\][^\[]*?modId\s*=\s*"([^"]+)"', toml, re.M | re.S))
    return ids

PACK_MODS = pack_mod_ids()

def condition_holds(c):
    """Evaluate a power file's forge:conditions (mod_loaded / not / and / or) against the pack's mods."""
    if isinstance(c, list):
        return all(condition_holds(x) for x in c)
    t = c.get('type')
    if t == 'forge:mod_loaded': return c['modid'] in PACK_MODS
    if t == 'forge:not': return not condition_holds(c['value'])
    if t == 'forge:and': return all(condition_holds(x) for x in c['values'])
    if t == 'forge:or': return any(condition_holds(x) for x in c['values'])
    return True

def power_loads(p):
    ns, path = p.split(':')
    hits = glob.glob(str(X / '*' / 'data' / ns / 'powers' / (path + '.json')))
    return bool(hits) and condition_holds(json.load(open(hits[0])).get('forge:conditions', []))

def origin(oid, remove=(), add=(), name=None, description=None, drop_upgrades=False):
    ns, path = oid.split(':')
    d = src(ns, 'origins', path)
    powers = [p for p in d.get('powers', []) if p not in remove]
    missing = set(remove) - set(d.get('powers', []))
    assert not missing, (oid, missing)
    # drop powers from mods the pack does not have (each would log a warning)
    powers = [p for p in powers if p.split(':')[0] in ('origins', 'medievalorigins', 'origins-plus-plus', 'origins-classes', 'mdvlcraft')]
    # and powers that only exist with other mods (e.g. Wood Elf's Zenith archery power)
    powers = [p for p in powers if p.startswith('mdvlcraft:') or power_loads(p)]
    d['powers'] = powers + list(add)
    d['loading_priority'] = PRIORITY
    if name: d['name'] = name
    if description: d['description'] = description
    if drop_upgrades: d.pop('upgrades', None)
    write(ns, 'origins', path, d)

REACH = 'reach-entity-attributes:reach'  # not in this pack; Forge's own block reach does the same job

def use_forge_reach(node):
    """Swap the Reach Entity Attributes reach attribute (and its tooltip name) for Forge's block reach."""
    if isinstance(node, dict):
        return {k: ('forge:block_reach' if v == REACH else 'forge.block_reach' if v == 'attribute.name.generic.reach-entity-attributes.reach'
                    else use_forge_reach(v)) for k, v in node.items()}
    if isinstance(node, list):
        return [use_forge_reach(v) for v in node]
    return node

def power(pid, data):
    ns, path = pid.split(':')
    data = use_forge_reach(dict(data)); data['loading_priority'] = PRIORITY
    write(ns, 'powers', path, data)

# ---------------------------------------------------------------- the race list
ALLOWED = ['origins:human', 'origins:feline', 'origins:merling',
           'medievalorigins:arachnae', 'medievalorigins:dwarf', 'medievalorigins:wood_elf', 'medievalorigins:goblin',
           'medievalorigins:siren', 'medievalorigins:ogre',
           'origins-plus-plus:land_shark', 'origins-plus-plus:kirin', 'origins-plus-plus:raptus', 'origins-plus-plus:rat',
           'origins-plus-plus:shadow', 'origins-plus-plus:sporeling']
layer = json.load(open(X / 'origins-forge/data/origins/origin_layers/origin.json'))
assert 'origins:human' in layer['origins']
layer.update({'replace': True, 'origins': ALLOWED, 'loading_priority': PRIORITY})
write('origins', 'origin_layers', 'origin', layer)
write('medievalorigins', 'origin_layers', 'magic_subclasses', {'replace': True, 'enabled': False, 'origins': [], 'loading_priority': PRIORITY})

# Medieval Origins races that are not on offer: no powers, so they do not log the powers they cannot load
for f in sorted(glob.glob(str(X / '*' / 'data' / 'medievalorigins' / 'origins' / '*.json'))):
    oid = 'medievalorigins:' + Path(f).stem
    if oid not in ALLOWED:
        d = json.load(open(f)); d.update({'powers': [], 'loading_priority': PRIORITY})
        d.pop('upgrades', None)
        write('medievalorigins', 'origins', Path(f).stem, d)

power('medievalorigins:dwarf/mythril_resonance', src('medievalorigins', 'powers', 'dwarf/mythril_resonance'))

# ---------------------------------------------------------------- Origins
power('mdvlcraft:feline/soft_landing', {
    'type': 'origins:modify_damage_taken', 'name': 'Soft Landing',
    'description': 'You always land on your feet: fall damage is reduced by 25%.',
    'damage_condition': {'type': 'origins:in_tag', 'tag': 'minecraft:is_fall'},
    'modifier': {'operation': 'multiply_base_multiplicative', 'value': -0.25}})
origin('origins:feline', remove=['origins:fall_immunity', 'origins:weak_arms'], add=['mdvlcraft:feline/soft_landing'],
       description='With their cat-like appearance, the Feline scare creepers away. With the dexterity of cats, they land softly on their feet.')
power('mdvlcraft:merling/gills', {
    'type': 'origins:action_over_time', 'name': 'Gills', 'interval': 10,
    'description': 'You can breathe underwater, and on land just like anyone else.',
    'condition': {'type': 'origins:submerged_in', 'fluid': 'minecraft:water'},
    'entity_action': {'type': 'origins:apply_effect', 'effect': {
        'effect': 'minecraft:water_breathing', 'duration': 30, 'amplifier': 0,
        'is_ambient': True, 'show_particles': False, 'show_icon': False}}})
origin('origins:merling', remove=['origins:water_breathing', 'origins:air_from_potions'], add=['mdvlcraft:merling/gills'],
       description='These natural inhabitants of the ocean breathe water as easily as air.')

# ---------------------------------------------------------------- Medieval Origins Revival
origin('medievalorigins:arachnae', remove=['medievalorigins:arachnae/brittle'],
       description='-§l Overview§r: \n§o§2+ Poison, Crowd Control\n+ Wall Climbing§r\n§o§6• Carnivore§r\n§o§c- Hunger, Health§r\nA grotesque amalgamation of spider and human, with venomous fangs and bristled feet yet the ability to walk upright and wield weaponry.')
origin('medievalorigins:dwarf', remove=['medievalorigins:dwarf/darkness_dweller', 'medievalorigins:dwarf/potent_brew', 'medievalorigins:dwarf/miniature'],
       description='-§l Overview§r: \n§o§2+ Mining, Caving§r\n§o§c- Can\'t swim§r\n§lDwarves§r are practical, stocky, and prideful. They excel at mining and living underground. Rock and stone, brother.')
origin('medievalorigins:wood_elf', remove=['medievalorigins:wood_elf/forest_vision', 'medievalorigins:wood_elf/elegant', 'medievalorigins:wood_elf/towering'],
       description='-§l Overview§r: \n§o§2+ Ranged Bonuses, Speed§r\n§o§6• Vegetarian§r\n§o§c- Melee Damage§r\n§lWood Elves§r are a wise, long-lived race who live in harmony with the Earth. They are quick with a sword and bow.')
greedy = src('medievalorigins', 'powers', 'goblin/greedy')
for key in ('golden_weapon_boosts', 'golden_weapon_boosts_offhand'):
    mods = [m for m in greedy[key]['modifiers'] if m['attribute'] != 'minecraft:generic.attack_damage']
    assert len(mods) == len(greedy[key]['modifiers']) - 1
    greedy[key]['modifiers'] = mods
# this Origins build has no Dig Speed attribute; the modifier only made the power fail to load fully
greedy['golden_tool_boosts']['modifiers'] = [m for m in greedy['golden_tool_boosts']['modifiers'] if 'dig_speed' not in m['attribute']]
greedy['description'] = 'Hobgoblins have a special relationship with treasure: they find more loot with golden weapons and take less damage in a full set of golden armor.'
power('medievalorigins:goblin/greedy', greedy)
origin('medievalorigins:goblin', remove=['medievalorigins:goblin/nippy', 'medievalorigins:goblin/stunted'], name='Hobgoblin',
       description='-§l Overview§r: \n§o§2+ Gold Gear Bonuses§r\n§o§2+ Luck§r \n§o§c- Health, Defense§r\n§lHobgoblins§r are obsessed with gold and treasure and tend to find a little more loot wherever they look, but they are frail in combat.')
depth = src('medievalorigins', 'powers', 'siren/out_of_your_depth')
power('medievalorigins:siren/out_of_your_depth', {
    'type': 'origins:modify_healing', 'condition': depth['condition'],
    'name': 'Out of Your Depth', 'description': 'While on land, Sirens heal and regenerate at half the usual rate.',
    'modifier': {'operation': 'multiply_base_multiplicative', 'value': -0.5}})
origin('medievalorigins:siren', remove=['medievalorigins:siren/flammable'],
       description='-§l Overview§r: \n§o§2+ Water-Based, Utility §r\n§o§6• Weaker on Land§r\n§lSirens§r are said to be a type of Merfolk. They have an ensnaring aura and their irresistible beauty is said to be the downfall of many a foolish sailor.')
origin('medievalorigins:ogre', remove=['medievalorigins:ogre/gargantuan', 'medievalorigins:ogre/sluggish'], name='Half-Ogre',
       description='-§l Overview§r: \n§o§2+ Axe Bonuses, Health §r\n§o§c- Hunger §r\n§lHalf-Ogres§r carry the blood of the large, short-tempered and brutish Ogres. They are human-sized, but keep much of their raw strength and toughness.')

# ---------------------------------------------------------------- Origins++
origin('origins-plus-plus:land_shark', remove=['origins-plus-plus:land-shark/fins'], name='Sharkfolk',
       description='A ferocious predator at home in both water and land.')
exhaustion = src('origins-plus-plus', 'powers', 'kirin/exhaustion')
exhaustion['exhaustion'] = exhaustion['exhaustion'] / 2
exhaustion['description'] = 'You get hungry a little faster.'
power('origins-plus-plus:kirin/exhaustion', exhaustion)
origin('origins-plus-plus:kirin', remove=['origins-plus-plus:kirin/dislike_of_water', 'origins-plus-plus:kirin/cat_size'])
origin('origins-plus-plus:raptus', remove=['origins-plus-plus:raptus/light_armor', 'origins-plus-plus:raptus/small'], name='Saurusfolk',
       description='Descendants of the great raptors, these creatures can be found hunting in packs or resting.')
origin('origins-plus-plus:rat', remove=['origins-plus-plus:rat/nocturnal_eyes', 'origins-plus-plus:rat/slow_swimmer', 'origins-plus-plus:rat/one_block_height'],
       name='Ratfolk', description='A quick critter of the night. You hunt and appear filthy to others.')
origin('origins-plus-plus:shadow', remove=['origins-plus-plus:shadow/cloud', 'origins-plus-plus:shadow/sun_weakness',
                                           'origins-plus-plus:shadow/no_food', 'origins-plus-plus:shadow/hunger'])
origin('origins-plus-plus:sporeling', remove=['origins-plus-plus:sporeling/light_armor', 'origins-plus-plus:sporeling/small'], drop_upgrades=True,
       description='A floaty mushroom-person that came out of the ground and started doing stuff.')

# ---------------------------------------------------------------- Origins: Classes
origin('origins-classes:merchant', remove=['origins-classes:rare_wandering_loot'])
stealth = src('origins-classes', 'powers', 'stealth_core')
del stealth['damage_boost']
stealth['description'] = 'While in stealth, you make less sound. You exit Stealth when you stop sneaking.'
power('origins-classes:stealth_core', stealth)

print(len(written), 'files')
for p in written: print(' ', p.relative_to(OUT))
