#!/usr/bin/env python3
"""Builds the Monk skill tree (Warrior) from the Berzerker tree's layout.

The Monk takes Berzerker's node positions and connections, and gives each node a Monk version: the stat lines
are swapped for the Monk's (unarmed damage for attack damage, fist mastery for axe damage, attack speed for life
steal, movement speed for attack knockback, stun armour for extra strikes; a little less health, armour and
knockback resistance), and the ability nodes, taken in order of distance from the root, become the Monk's arts.
It also places the Monk on the Classes tab, adds it to config.json and gives its subjects their original art in
tools/astro_sources.json. Run tools/astro_theme.py afterwards to draw the icons.

Usage (from mdvlcraft-binder/):  python3 tools/monk_tree.py
"""
import collections
import json
import math
import re
from pathlib import Path

BINDER = Path(__file__).resolve().parent.parent
SKILLS = BINDER / 'src/main/resources/data/mdvlcraft/puffish_skills'
CATS = SKILLS / 'categories'
SOURCES = Path(__file__).with_name('astro_sources.json')
BASE = 'berzerker'

# Berzerker stat -> Monk stat: (attribute, operation, scale or {Berzerker value: Monk value}, {old family title: new family title})
STATS = {
    'minecraft:generic.attack_damage': ('mdvlcraft:unarmed_damage', 'addition', {0.1: 0.1, 0.25: 0.3, 0.5: 0.5},
                                        {'Bloodrage': 'Empty Hand', 'Fury': 'Iron Fist', 'Might': 'Knuckles'}),
    'puffish_attributes:axe_damage': ('mdvlcraft:fist_mastery', 'addition', 1.0, {'Axe': 'Open Palm'}),
    'puffish_attributes:life_steal': ('minecraft:generic.attack_speed', 'multiply_base', 1.5, {'Bloodlust': 'Flurry'}),
    'minecraft:generic.attack_knockback': ('minecraft:generic.movement_speed', 'multiply_base', 0.15 / 2.4, {'Brutal': 'Wind Step'}),
    'epicfight:max_strikes': ('epicfight:stun_armor', 'addition', 2.0, {'Rampage': 'Iron Body'}),
    'minecraft:generic.max_health': (None, None, {0.3333: 0.25, 0.6667: 0.5}, {}),
    'minecraft:generic.armor': (None, None, {0.25: 0.15, 0.5: 0.35, 1.0: 0.65}, {'Bulk': 'Conditioning', 'Hardened': 'Iron Skin'}),
    'minecraft:generic.knockback_resistance': (None, None, {0.01: 0.005, 0.03: 0.02, 0.06: 0.04}, {}),
    'irons_spellbooks:mana_regen': (None, None, 1.0, {'Meditation': 'Stillness'}),   # Meditation is a Monk skill
    'irons_spellbooks:cooldown_reduction': (None, None, 1.0, {'Focus': 'Clarity'}),  # Focus is a Monk art
}
# descriptions of the Monk's own stats (value -> text); others reuse the wording the trees already use
OWN_TEXT = {
    'mdvlcraft:unarmed_damage': lambda v: f'+{fmt(v)} Unarmed Damage (empty main hand)',
    'mdvlcraft:fist_mastery': lambda v: f'+{fmt(v * 100)}% Fist Damage (empty hand or glove; also the Monk\'s Cursed Fate arts)',
    'epicfight:stun_armor': lambda v: f'+{fmt(v)} Stun Armor',
}


def ef(skill, title, kind, text, cost):
    return dict(title=title, cost=cost, rewards=[{'type': 'mdvlcraft:epicfight_skill', 'data': {'skill': skill}}],
                description=f'Teaches {title}, an Epic Fight {kind} skill: {text}. Equip it in the Combat Skills box of the abilities screen')


def art(ability, title, mana, cooldown, cost, extra, held=False):
    cd = 'no cooldown' if cooldown == 0 else f'{cooldown} s cooldown'
    return dict(title=title, cost=cost, rewards=[{'type': 'mdvlcraft:ability', 'data': {'ability': ability, 'level': 1}}],
                description=f'Unlocks {title} ({mana} mana, {cd}) on the ability wheel{" (hold)" if held else ""}',
                extra_description=extra)


# the Monk's arts, from the root outwards (Berzerker's ability nodes in order of distance from its root)
ARTS = [
    art('mdvlcraft:heavy_blow', 'Heavy Blow', 15, 5, 2, "Cursed Fate's melee art: punch very hard. No weapon needed."),
    ef('wom:precise_roll', 'Precise Roll', 'Dodge', 'a faster, further and cheaper roll', 2),
    ef('wom:inner_growth', 'Inner Growth', 'Passive', 'regenerate 2 innate power every second', 2),
    ef('wom:shooting_style', 'Shooting Style', 'Identity', 'with an empty hand, hold sneak while attacking to kick (a 3-kick combo)', 2),
    art('mdvlcraft:uppercut', 'Uppercut', 15, 9, 2, "Cursed Fate's melee art: an uppercut that throws targets into the air."),
    dict(key='unarmed_parry', title='Empty-Hand Deflection', cost=3,
         rewards=[{'type': 'puffish_skills:attribute', 'data': {'attribute': 'mdvlcraft:unarmed_parry', 'value': 1.0, 'operation': 'addition'}}],
         description='Your guard (Parry - Absolute Deflection, Guard or Parrying) can be raised with bare hands, claws or a glove',
         extra_description='Every class has these guards, but only with a weapon; the Monk guards and parries with bare hands.'),
    art('mdvlcraft:barrage', 'Barrage', 20, 8, 3, "Cursed Fate's melee art: hold to throw a flurry of punches.", held=True),
    ef('dodge_parry_reward:heal2', 'Instant Heal II', 'Passive', 'a perfect dodge or a perfect parry instantly restores some health', 2),
    art('mdvlcraft:follow_up_punch', 'Follow Up Punch', 20, 15, 2, "Cursed Fate's melee art: teleport behind the last creature you hit."),
    ef('wom:meditation', 'Meditation', 'Passive',
       'sneak still for 2 s to meditate; the longer you meditate, the longer the buff (stage 1: +40% damage; stage 2: +30% attack speed and Speed I; stage 3: 50% less damage taken and Regeneration I)', 3),
    ef('wom:heart_shield', 'Heart Shield', 'Passive', 'a 20-point shield that recovers while you take no action', 3),
    art('mdvlcraft:knockout', 'Knockout', 25, 20, 3,
        'A straight punch at the creature in reach: 1.5x damage, and Blackout (its magic is suppressed) for 6 s.'),
    ef('wom:mindset', 'Mindset', 'Passive', 'above 80% health, deal 30% more damage and take 20% less', 3),
    art('mdvlcraft:leaping_crush', 'Leaping Crush', 30, 12, 3, "Cursed Fate's Heavenly Restriction art: leap high and crash down on your foes. No weapon needed."),
    dict(title='Haste', cost=2, rewards=[{'type': 'mdvlcraft:ability', 'data': {'ability': 'irons_spellbooks:haste', 'level': 2}}],
         description='Unlocks Haste (Level 2) (36 mana, 120 s cooldown) on the ability wheel. Hold the Ponder key (W) here to preview it'),
    art('mdvlcraft:focus', 'Focus', 30, 30, 4,
        'Your next strike within 10 s (a punch, a kick or one of your arts) is a guaranteed Black Flash: 2.5x damage and a short stun.'),
]
LEFTOVER = dict(title='Ki Reservoir', attribute='irons_spellbooks:max_mana', value=25.0)
SOURCES_NEW = {
    'archetype/monk': 'mdvlcraft:textures/gui/icons/archetype/monk.png',
    'bonus/mdvlcraft_unarmed_damage': 'mdvlcraft:textures/gui/icons/attribute/mdvlcraft_unarmed_damage.png',
    'bonus/mdvlcraft_fist_mastery': 'mdvlcraft:textures/gui/icons/attribute/mdvlcraft_fist_mastery.png',
    'bonus/mdvlcraft_unarmed_parry': 'efn:textures/gui/skills/guard/efn_parry.png',
    'skill/wom_inner_growth': 'wom:textures/gui/skills/passive/inner_growth.png',
    'skill/wom_meditation': 'wom:textures/gui/skills/passive/meditation.png',
    'skill/wom_precise_roll': 'wom:textures/gui/skills/dodge/precise_roll.png',
    'skill/wom_mindset': 'wom:textures/gui/skills/passive/mindset.png',
    'skill/wom_shooting_style': 'wom:textures/gui/skills/identity/shooting_style.png',
    'skill/wom_heart_shield': 'wom:textures/gui/skills/passive/heart_shield.png',
    'skill/dodge_parry_reward_heal2': 'minecraft:textures/mob_effect/regeneration.png',
}
SOURCES_NEW.update({f'technique/mdvlcraft_{a}': f'mdvlcraft:textures/gui/icons/ability/{a}.png'
                    for a in ('heavy_blow', 'barrage', 'uppercut', 'follow_up_punch', 'leaping_crush', 'focus', 'knockout')})


def fmt(v):
    v = round(v, 4)
    return str(int(v)) if v == int(v) else f'{v:g}'


def frame_kind(defn):
    return defn['frame']['data']['locked'].rsplit('_', 2)[-2]


def wording():
    """attribute/operation -> (percent?, label), learnt from the descriptions in all the trees."""
    known = {}
    for path in CATS.glob('*/definitions.json'):
        for d in json.loads(path.read_text()).values():
            r = d.get('rewards', [])
            if len(r) != 1 or r[0]['type'] != 'puffish_skills:attribute':
                continue
            a = r[0]['data']
            m = re.match(r'\+([\d.]+)(%?) (.+)', d.get('description', ''))
            if m and (a['attribute'], a['operation']) not in known:
                known[(a['attribute'], a['operation'])] = (m.group(2) == '%', m.group(3))
    return known


def describe(known, attribute, operation, value):
    if attribute in OWN_TEXT:
        return OWN_TEXT[attribute](value)
    percent, label = known[(attribute, operation)]
    return f'+{fmt(value * 100)}% {label}' if percent else f'+{fmt(value)} {label}'


def main():
    base_defs = json.loads((CATS / BASE / 'definitions.json').read_text())
    skills = json.loads((CATS / BASE / 'skills.json').read_text())
    connections = json.loads((CATS / BASE / 'connections.json').read_text())
    known = wording()

    # ability nodes in order of distance from the root
    graph = collections.defaultdict(set)
    for a, b in connections['normal']['bidirectional']:
        graph[a].add(b)
        graph[b].add(a)
    root = next(k for k, v in skills.items() if v.get('root'))
    depth, queue = {root: 0}, collections.deque([root])
    while queue:
        n = queue.popleft()
        for m in graph[n]:
            if m not in depth:
                depth[m] = depth[n] + 1
                queue.append(m)
    ability_nodes = sorted((n for n, v in skills.items() if frame_kind(base_defs[v['definition']]) == 'challenge'),
                           key=lambda n: (depth[n], n))
    assert len(ability_nodes) >= len(ARTS), (len(ability_nodes), len(ARTS))

    defs = {}
    renamed = {}
    leftovers = 0
    for node in ability_nodes:
        old = base_defs[skills[node]['definition']]
        i = ability_nodes.index(node)
        if i < len(ARTS):
            new = dict(ARTS[i])
            data = new['rewards'][0]['data']
            key = new.pop('key', None) or data.get('ability', data.get('skill', '')).split(':')[1]
        else:
            leftovers += 1
            key = f'ki_reservoir_{leftovers}'
            new = dict(title=f'{LEFTOVER["title"]} {"I" * leftovers}', cost=2,
                       rewards=[{'type': 'puffish_skills:attribute',
                                 'data': {'attribute': LEFTOVER['attribute'], 'value': LEFTOVER['value'], 'operation': 'addition'}}],
                       description=describe(known, LEFTOVER['attribute'], 'addition', LEFTOVER['value']))
        new['frame'] = old['frame']
        new['size'] = old.get('size', 2.0)
        new['icon'] = old['icon']  # redrawn by astro_theme.py
        renamed[skills[node]['definition']] = key
        defs[key] = {k: new[k] for k in ('title', 'description', 'extra_description', 'icon', 'rewards', 'cost', 'frame', 'size') if k in new}

    for old_key, old in base_defs.items():
        if old_key in renamed:
            continue
        reward = old['rewards'][0]['data']
        attribute, operation, value = reward['attribute'], reward['operation'], reward['value']
        new_attr, new_op, scale, titles = STATS.get(attribute, (None, None, 1.0, {}))
        new_attr, new_op = new_attr or attribute, new_op or operation
        value = scale[round(value, 4)] if isinstance(scale, dict) else round(value * scale, 4)
        family = re.sub(r' (\d+|Mastery [IVX]+)$', '', old['title'])
        title = titles.get(family, family) + old['title'][len(family):]
        key = re.sub(r'[^a-z0-9]+', '_', title.lower()).strip('_')
        new = dict(old)
        new['title'] = title
        new['description'] = describe(known, new_attr, new_op, value)
        new['rewards'] = [{'type': 'puffish_skills:attribute', 'data': {'attribute': new_attr, 'value': value, 'operation': new_op}}]
        assert key not in defs, key
        renamed[old_key] = key
        defs[key] = new

    out = CATS / 'monk'
    out.mkdir(exist_ok=True)
    (out / 'definitions.json').write_text(json.dumps(defs, indent=2) + '\n')
    # node ids follow their (one-to-one) definitions, so saves and commands read as Monk
    node_id = {n: renamed[v['definition']] for n, v in skills.items()}
    assert len(set(node_id.values())) == len(node_id)
    (out / 'skills.json').write_text(json.dumps({node_id[n]: dict(v, definition=node_id[n]) for n, v in skills.items()}, indent=2) + '\n')
    connections = {'normal': {'bidirectional': [[node_id[a], node_id[b]] for a, b in connections['normal']['bidirectional']]}}
    (out / 'connections.json').write_text(json.dumps(connections, indent=2) + '\n')
    (out / 'experience.json').write_text((CATS / BASE / 'experience.json').read_text())
    category = json.loads((CATS / BASE / 'category.json').read_text())
    category['title'] = 'Monk'
    category['description'] = 'Unarmed martial arts: fists, gloves and ki.'
    category['icon'] = {'type': 'texture', 'data': {'texture': 'mdvlcraft:textures/gui/astro/tab/monk.png'}}
    (out / 'category.json').write_text(json.dumps(category, indent=2) + '\n')

    # Classes tab: the four Warrior archetypes evenly along the Warrior arc, Monk last
    classes = CATS / 'classes'
    cskills = json.loads((classes / 'skills.json').read_text())
    for name, angle in (('knight', 189), ('samurai', 213), ('berzerker', 237), ('monk', 261)):
        placed = cskills.get(name, {'definition': name})
        placed['x'] = round(187 * math.cos(math.radians(angle)))
        placed['y'] = round(187 * math.sin(math.radians(angle)))
        cskills[name] = placed
    (classes / 'skills.json').write_text(json.dumps(cskills, indent=2) + '\n')
    cdefs = json.loads((classes / 'definitions.json').read_text())
    monk = dict(cdefs['knight'])
    monk['title'] = 'Monk'
    monk['description'] = 'Unarmed martial arts: fists, gloves and ki. Key: Focus (Black Flash), Knockout, Parry: Absolute Deflection.'
    monk['icon'] = {'type': 'texture', 'data': {'texture': 'mdvlcraft:textures/gui/astro/archetype/24/monk.png'}}
    cdefs['monk'] = monk
    (classes / 'definitions.json').write_text(json.dumps(cdefs, indent=2) + '\n')
    cconn = json.loads((classes / 'connections.json').read_text())
    if ['warrior', 'monk'] not in cconn['normal']['bidirectional']:
        cconn['normal']['bidirectional'].append(['warrior', 'monk'])
    (classes / 'connections.json').write_text(json.dumps(cconn, indent=2) + '\n')

    config = json.loads((SKILLS / 'config.json').read_text())
    if 'monk' not in config['categories']:
        config['categories'].insert(config['categories'].index('berzerker') + 1, 'monk')
    (SKILLS / 'config.json').write_text(json.dumps(config, indent=2) + '\n')

    sources = json.loads(SOURCES.read_text())
    for k, tex in SOURCES_NEW.items():
        sources[k] = {'type': 'texture', 'data': {'texture': tex}}
    SOURCES.write_text(json.dumps(dict(sorted(sources.items())), indent=1) + '\n')

    totals = collections.defaultdict(float)
    for d in defs.values():
        for r in d['rewards']:
            if r['type'] == 'puffish_skills:attribute':
                totals[r['data']['attribute']] += r['data']['value'] * sum(1 for v in skills.values() if renamed[v['definition']] in defs and defs[renamed[v['definition']]] is d)
    print(f'monk: {len(skills)} nodes, {len(ARTS)} arts, {leftovers} Ki Reservoir nodes')
    for a, t in sorted(totals.items()):
        print(f'  {a}: {fmt(t)}')


if __name__ == '__main__':
    main()
