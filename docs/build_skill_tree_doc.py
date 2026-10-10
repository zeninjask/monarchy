#!/usr/bin/env python3
"""Build docs/MDVLCraft-Skill-Trees*.pdf from the Binder's skill-tree data, in four styles: classic,
astrologer (matching the in-game skill screen), manuscript and fieldguide.

Every bonus, spell, technique and Epic Fight skill in each archetype tree, added up over the whole
tree. Run from the repository root:  python3 docs/build_skill_tree_doc.py <pack version>
Needs the pack's mod jars in mdvlcraft-binder/libs/modpack (for spell and skill names), and
Chromium (or Chrome) to print the PDF.
"""
import base64, collections, html, json, re, subprocess, sys, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / 'mdvlcraft-binder/src/main/resources'
CATS = RES / 'data/mdvlcraft/puffish_skills/categories'
LIBS = ROOT / 'mdvlcraft-binder/libs/modpack'
VERSION = sys.argv[1] if len(sys.argv) > 1 else 'dev'
CLASSES = {'warrior': ('Warrior', '#e05545', ['knight', 'samurai', 'berzerker']),
           'mage': ('Mage', '#4a8fe0', ['holy', 'flame', 'lightning', 'necromancy', 'ice', 'water']),
           'rogue': ('Rogue', '#9b5de0', ['assassin', 'phantom', 'scout', 'thief']),
           'ranger': ('Ranger', '#3fb36b', ['wanderer', 'stalker', 'druid', 'cursed'])}
DESCRIPTIONS = {
    'knight': "An armoured wall. Knights soak up hits with damage and projectile resistance, armour toughness and stun armour, and hit back with greatswords. Shield and Fortify let them shrug off fights other classes would run from, Lingering Strain leaves a weakening cloud behind, Gravity Stomp slams the ground around them, Impact Guard blocks almost anything, Endurance keeps them swinging through hits, Magic Immunity shrugs off spells, Auto Heal turns mana into health, Pain Anticipation braces for the next blow, Shield Counter turns a block into a stun and Combat Mastery weaves dodges into attacks.",
    'samurai': "A disciplined duellist who lives on timing, with no spells at all. Every katana strike hits harder and cuts through armour, every perfect parry refills stamina and reflects damage, and heavy impacts stagger enemies. A Stance channels fire, wind, water or earth (water lengthens the parry window), Blitz and Surprise Attack strike out of nowhere, and Technician, Strength I, Revelation, Manipulator and Parry Master reward perfect timing. Dancing Blade, Dopamine, Swordmaster, Hasty Casting, Spider Techniques and Combat Mastery round out a restless fighting style, and at the very end of a path waits Myriad Blades Convergence.",
    'berzerker': "Pure aggression. Berzerkers stack raw attack damage, axe damage and life steal, hit more targets per swing and knock them flying. Fang Strike tears the ground open, Burning Dash and Stomp close the gap, Blood Howl roars for blood, Heartstop lets them ignore death for a few seconds, and Berserker, Forbidden Strength and Adrenaline turn wounds into power. Essence Extortion heals on every hit, Lethal Focus builds with every blow, Punishment Kick knocks foes out, Dread Full Buster breaks through any guard, Demolition Leap crashes into a fight, and Adrenaline Fiend, Stamina Pillager, Hyper Vitality and Combat Mastery keep the onslaught going.",
    'holy': "A healer and support caster. Holy power strengthens light spells, healing received rises, and holy resistance protects against dark magic. Guiding Bolt, Heal, Greater Heal, Blessing of Life, Healing Circle, Cloud of Regeneration, Wisp, Cleanse and Sunbeam keep a party alive, Divine Smite brings the light down on undead, Adaptive Skin hardens against whatever hurts you, Second Wind spends your mana to cheat death and Mana Shield pays for hits with mana.",
    'flame': "Burst damage and constant pressure. Fire power and cooldown reduction keep the flames coming, from Firebolt, Fire Breath and Scorch to Magma Bomb, Lava Bomb, Wall of Fire, Blaze Storm, Heat Surge, Fireball, Meteor Storm and Raise Hell, and Annihilation levels everything in front of you. Ember skin resists fire, Avatar of Might blasts enemies away, Vengeance burns back at whoever hurt you, Adrenaline flares up near death, Reserve Mana refills an empty pool and Mana Shield pays for hits with mana.",
    'lightning': "A mobile storm caster. Lightning power grows, casting barely slows you down, and grounding resists shocks. Electrocute, Ball Lightning, Chain Lightning, Lightning Lance, Shockwave, Lightning Bolt and Thunderstorm punish anything in reach, while Charge, Volt Strike and Demolition Leap keep you moving. Rapid Chant speeds every cast, Reserve Mana refills an empty pool and Mana Shield pays for hits with mana.",
    'necromancy': "Blood magic and the risen dead. Blood power and summon damage feed Wither Skull, Blood Slash, Blood Needles, Ray of Siphoning, Devour, Raise Dead, Summon Vex, Sacrifice, Cursed Revenants and Axe Of The Doomed, and Stamina Pillager feeds on every kill, turning every fight into a war of attrition. Blood into Mana casts with health, and Soul Protection and Mana Shield ward off blows.",
    'ice': "Control and protection. Ice power, magic resistance and cold resistance back up Icicle, Cone of Cold, Ray of Frost, Frostwave, Frost Step, Ice Spikes, Ice Block, Frostbite, Blizzard, Halberd Horizon and Cursed Blast, which slow, freeze and shatter anything that comes close. Hyper Vitality fuels weapon skills with stamina, Aqua Maneuver speeds you through water and Mana Shield pays for hits with mana.",
    'water': "Tides, torrents and the crushing deep. Aqua power and aqua resistance drive Hydroshot, Bubble Spray, Aqua Missiles, Flood Slash, Coral Barrage, Tidal Grasp, Rainfall, The Howling Tempest, Serpentide, Tsunami, Floodgate, Vortex Of The Deep and Skypiercer, Jet Steam hurls you clear of trouble, Hyper Vitality fuels weapon skills with stamina, Aqua Maneuver speeds you through water and Mana Shield pays for hits with mana.",
    'assassin': "Strike first, strike from behind. Daggers, backstab damage, armour negation and protection shred make every opening lethal. Echoing Strikes, Acupuncture and Bonebreaker punish a single target, Throw hurls your blade at whatever runs, Stamina Pillager feeds on kills, and Blood Step, Spectral Blink, Shadow Step, Invisibility, Spider Techniques and Ender Obscuris get you in and out unseen.",
    'phantom': "A rapier duellist built around the dodge. Rapiers (a sword with an empty off hand) grow sharper, dodges cost less stamina, and the first hit after a dodge lands as a riposte. Evasion turns incoming blows into blinks, Slow drags enemies down, Invisibility hides you and Blackout plunges enemies into darkness. Ender Step, Ender Obscuris and Dodge Master replace the roll, Natural Sprinter and Phantom Ascent lend speed, Catharsis, Technician and All Eyes on You reward every dodge and block, Swordmaster and Stamina Pillager keep the blade busy, and the Doppelganger, a double of yourself, fights at your side, copies your attacks and lets you teleport behind any enemy.",
    'scout': "Speed, range and awareness. Scouts swim faster, hit harder with tridents, break through terrain, resist ender magic and empower ender spells. Haste, Gust, Natural Sprinter, Phantom Ascent's mid-air jumps and Planar Sight let them reach, flee and see what others cannot, Cleanse clears ailments, Dopamine turns dash attacks into stamina, Stamina Pillager feeds on kills and Touch Dig tunnels through anything. Crows scout and strike for them: Silent Appraisal, Blind Investment, Bird Strike and Controlled Collapse, and Arcane Sustenance turns mana into food.",
    'thief': "Luck and loot. Thieves gain luck, double drops, extra Fortune, longer reach, cheaper anvil repairs and a cutthroat's sword arm. Boogie Woogie claps swap places with creatures or blocks to confuse fights, Lob Creeper and Chain Creeper blow open a fight, Scapegoat, Firecracker, Teleport and Natural Sprinter cover a getaway, Emergency Escape dodges out of any hit and Astral Sense reveals what lurks nearby.",
    'wanderer': "Built for long journeys. Food lasts longer, mounts run faster, pets and horses hit harder and survive longer, and you step up full blocks. Summon Horse, Summon Polar Bear, Gluttony and Counterspell keep the road open, Ashen Breath scorches whatever blocks the way, Arcane Sustenance turns mana into food, Adaptive Skin hardens against whatever hurts you, Adrenaline Fiend gets you back on your feet after every kill, Natural Sprinter covers ground, Swordmaster quickens the blade and Voodoo Magic trades health and stamina. Wanderers cannot be poisoned or given Hunger.",
    'stalker': "Bows and crossbows. Ranged damage, arrow and bolt speed and evocation power make every shot count. Arrow Volley rains arrows on a whole area, Poison Arrow leaves a lingering toxic cloud, Fire Arrow burns, Revelation turns well-timed defence into a stunning counter, Critical Knowledge lets any hit, arrows included, crit for double damage, Bodkin Points let arrows ignore part of the target's armour, and Shadow Step and Invisibility keep the hunter unseen.",
    'druid': "Nature magic and the land's healing. Nature power and resistance strengthen Acid Orb, Root, Firefly Swarm, Blight, Poison Splash, Spider Aspect, Oakskin, Earthquake and Aerial Collapse, Meteor Slam brings the sky down, Adaptive Skin hardens against whatever hurts you, and standing on natural ground slowly heals you. Druids cannot be poisoned or given Hunger.",
    'cursed': "Cursed techniques fuelled by mana instead of cursed energy, each with its own cooldown. Dismantle and Cleave cut enemies apart, and the Malevolent Shrine domain, closed and then open, shreds everything around you while you keep cutting. Cursed Control makes techniques cheaper and Wither Skull hurls death from afar; melee resistance and toughness shred round it out, with Swordmaster and Death Harvest for the blade, Vengeance against whoever hurt you, Devour to feed on the fallen and Paralyzing Skreech to shred armour with pulses of sound.",
}
CHROMES = ['/opt/pw-browsers/chromium-1194/chrome-linux/chrome', '/usr/bin/chromium', '/usr/bin/chromium-browser', '/usr/bin/google-chrome']
ROMAN = ['', 'I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X']


def load_lang():
    names = {}
    for jar in LIBS.glob('*.jar'):
        try:
            with zipfile.ZipFile(jar) as z:
                for n in z.namelist():
                    if n.endswith('lang/en_us.json') and n.startswith('assets/'):
                        try:
                            names.update(json.loads(z.read(n).decode('utf-8', 'replace'), strict=False))
                        except ValueError:
                            pass
        except zipfile.BadZipFile:
            pass
    names.update(json.load(open(RES / 'assets/mdvlcraft/lang/en_us.json')))
    return names


LANG = load_lang()
for jar in (ROOT / 'modpack/mods').glob('*.jar'):
    with zipfile.ZipFile(jar) as z:
        for n in z.namelist():
            if n.endswith('lang/en_us.json'):
                LANG.update(json.loads(z.read(n).decode('utf-8', 'replace'), strict=False))


def clean(text):
    return re.sub(r'§.', '', text).strip(' 『』')


def spell_name(rid):
    ns, path = rid.split(':')
    return clean(LANG.get(f'spell.{ns}.{path}', path.replace('_', ' ').title()))


def fmt(v):
    v = round(v, 1) if abs(v) >= 1 else round(v, 2)
    return f'{v:g}'


def tree(cat):
    defs = json.load(open(CATS / cat / 'definitions.json'))
    skills = json.load(open(CATS / cat / 'skills.json'))
    totals = collections.OrderedDict(); percent = {}; names = {}
    spells, techniques, ef = [], [], []
    cost = 0
    for node in skills.values():
        d = defs[node['definition']]
        cost += d.get('cost', 1)
        attr_rewards = [r for r in d.get('rewards', []) if r['type'] == 'puffish_skills:attribute']
        for r in attr_rewards:
            a = r['data']['attribute']
            totals[a] = totals.get(a, 0) + r['data']['value']
            if len(attr_rewards) == 1 and a not in names:
                m = re.match(r'^\+?-?[\d.]+(%?)\s+(.+)$', d.get('description', ''))
                if m:
                    names[a] = m.group(2); percent[a] = m.group(1) == '%'
        for r in d.get('rewards', []):
            if r['type'] == 'mdvlcraft:ability':
                ab = r['data']['ability']
                if ab.startswith('mdvlcraft:'):
                    techniques.append(clean(LANG.get('ability.mdvlcraft.' + ab.split(':')[1], d['title'])))
                else:
                    spells.append(f"{spell_name(ab)} {ROMAN[r['data'].get('level', 1)]}")
            elif r['type'] == 'mdvlcraft:epicfight_skill':
                m = re.match(r'Teaches (.+?), an Epic Fight (\w+) skill', d.get('description', ''))
                ef.append(f'{m.group(1)} ({m.group(2)})' if m else d['title'])
    bonuses = []
    for a, v in totals.items():
        name = names.get(a, a.split(':')[1].replace('_', ' ').title())
        pct = percent.get(a, False)
        bonuses.append((name, f"+{fmt(v * 100)}%" if pct else f"+{fmt(v)}", a, v * (100 if pct else 1)))
    bonuses.sort()
    return {'skills': len(skills), 'cost': cost, 'bonuses': bonuses, 'spells': list(dict.fromkeys(spells)),
            'techniques': list(dict.fromkeys(techniques)), 'ef': list(dict.fromkeys(ef))}


def class_rewards():
    defs = json.load(open(CATS / 'classes/definitions.json'))
    extra = {}
    for arch, d in defs.items():
        for r in d.get('rewards', []):
            if r['type'] == 'mdvlcraft:epicfight_skill':
                extra.setdefault(arch, []).append('Precise Parry (Passive, from the Classes tab)' if r['data']['skill'] == 'efn:precise_parry' else r['data']['skill'])
    return extra


def main():
    data = {a: tree(a) for c in CLASSES.values() for a in c[2]}
    extra = class_rewards()
    for a, skills in extra.items():
        data[a]['ef'] += skills
    def val(a, name):
        for n, text, _, _ in data[a]['bonuses']:
            if n == name:
                return text
        return '–'
    cols = ['Max Mana', 'Mana Regen', 'Max Health', 'Natural Regeneration', 'Armor', 'Cooldown Reduction', 'Movement Speed', 'Attack Damage', 'Max Stamina', 'Stamina Regen']
    heads = ['Max<br>Mana', 'Mana<br>Regen', 'Max<br>Health', 'Health<br>Regen', 'Armor', 'Cooldown<br>Red.', 'Move<br>Speed', 'Attack<br>Dmg', 'Max<br>Stamina', 'Stamina<br>Regen']
    e = html.escape
    rows = []
    for cid, (cname, colour, archs) in CLASSES.items():
        for a in archs:
            t = data[a]
            rows.append(f"<tr><td><b>{a.title()}</b></td><td><span class=tag style='background:{colour}'>{cname}</span></td><td class=n>{t['skills']}</td><td class=n>{t['cost']}</td>"
                        + ''.join(f"<td class=n>{val(a, c)}</td>" for c in cols)
                        + f"<td class=n>{len(t['spells'])}</td><td class=n>{len(t['techniques'])}</td><td class=n>{len(t['ef'])}</td></tr>")
    # class spokes: bonuses every archetype of a class shares at the same total, that not every archetype has
    every = set.intersection(*[{b[0] for b in data[a]['bonuses']} for a in data])
    spokes = []
    for cid, (cname, colour, archs) in CLASSES.items():
        common = set.intersection(*[{(b[0], b[1]) for b in data[a]['bonuses']} for a in archs])
        items = sorted(f'{n} {v}' for n, v in common if n not in every)
        spokes.append(f"<tr><td><span class=tag style='background:{colour}'>{cname}</span></td><td>{e(', '.join(items)) or '–'}</td></tr>")
    pages = []
    for cid, (cname, colour, archs) in CLASSES.items():
        for a in archs:
            t = data[a]
            def cell(items):
                return e(', '.join(items)) if items else '–'
            bonus_rows = ''.join(f"<tr><td>{e(n)}</td><td class=n>{e(v)}</td></tr>" for n, v, _, _ in t['bonuses'])
            pages.append(f"""<section class=page style='border-left:6px solid {colour}'>
<h2>{a.title()} <span class=tag style='background:{colour}'>{cname}</span></h2>
<p class=desc>{e(DESCRIPTIONS[a])}<br>{t['skills']} skills, {t['cost']} points to unlock everything.</p>
<div class=cols><table class=info><tr><th>Spells</th><td>{cell(t['spells'])}</td></tr><tr><th>Techniques</th><td>{cell(t['techniques'])}</td></tr><tr><th>Epic Fight skills</th><td>{cell(t['ef'])}</td></tr></table>
<table class=bonus><tr><th>Bonus</th><th class=n>Total</th></tr>{bonus_rows}</table></div></section>""")
    chrome = next((c for c in CHROMES if Path(c).exists()), None)
    for style, css in STYLES.items():
        doc = render(style, css, rows, spokes, pages, heads)
        stem = 'MDVLCraft-Skill-Trees' + ('' if style == 'classic' else f'-{style}')
        out_html = ROOT / f'docs/{stem}.html'
        out_html.write_text(doc)
        if chrome is None:
            print(f'no Chromium found; open docs/{stem}.html in a browser and print it to PDF')
            continue
        subprocess.run([chrome, '--headless', '--no-sandbox', '--disable-gpu', '--no-pdf-header-footer',
                        '--print-to-pdf=' + str(ROOT / f'docs/{stem}.pdf'), out_html.as_uri()],
                       check=True, capture_output=True)
        print(f'wrote docs/{stem}.pdf')


def icon_uri(arch, style):
    """The archetype's tab icon: the Astrologer's star icon for that style, the plain one otherwise."""
    path = RES / 'assets/mdvlcraft/textures/gui' / ('astro/tab' if style == 'astrologer' else 'icons/tab') / f'{arch}.png'
    if not path.exists():
        return ''
    return 'data:image/png;base64,' + base64.b64encode(path.read_bytes()).decode()


def render(style, css, rows, spokes, pages, heads):
    e = html.escape
    if style == 'astrologer':
        def add_icon(m):
            uri = icon_uri(m.group(1).lower(), style)
            return f"<h2><img class=ico src='{uri}' alt=''>{m.group(1)}" if uri else m.group(0)
        pages = [re.sub(r"<h2>(\w+)", add_icon, pg, count=1) for pg in pages]
    return f"""<!doctype html><html><head><meta charset=utf-8><title>MDVLCraft Skill Trees</title><style>
@page {{ size: A4 landscape; margin: 12mm 14mm; }}
table {{ border-collapse: collapse; }} .n {{ text-align: right; }} .page {{ page-break-before: always; }}
.cols {{ display: flex; gap: 14px; align-items: flex-start; }} .info {{ width: 48%; }} .bonus {{ width: 40%; }}
img.ico {{ width: 22px; height: 22px; image-rendering: pixelated; vertical-align: -5px; margin-right: 6px; }}
{css}
</style></head><body>
<h1>MDVLCraft skill trees</h1>
<p>Version {e(VERSION)}. Every bonus, spell, technique and Epic Fight skill in each archetype tree, added up over the <b>whole</b> tree. Players pick 2 archetypes; each tree gives 1 point to start and 1 per level, with no level cap. Percentages add to the base; Max Health is in half-hearts. Spells and techniques go on the ability wheel; Epic Fight skills are equipped in the Combat Skills box of the abilities screen.</p>
<h3>Overview</h3>
<table class=overview><tr><th>Archetype</th><th>Class</th><th>Skills</th><th>Cost</th>{''.join(f'<th>{h}</th>' for h in heads)}<th>Spells</th><th>Techn.</th><th>EF<br>skills</th></tr>{''.join(rows)}</table>
<section class=page><h3>Class bonuses</h3>
<p>Every archetype of a class has all of its class's bonuses below (whole-tree totals). Every archetype also has the core bonuses (health, speed, mana and so on); both are included in each archetype's totals.</p>
<table><tr><th>Class</th><th>Shared by every archetype of the class</th></tr>{''.join(spokes)}</table>
<h3>Everyone</h3>
<p>Every player knows Guard, Parrying, Parry – Absolute Deflection and the souls-like dodge and step (in place of Roll and Step), and starts with the souls-like dodge equipped. Assassin, Phantom, Scout and Thief also teach Precise Parry when picked in the Classes tab. Epic Fight skills never need another skill learned first.</p>
<p>On first joining, players also pick a race (Origins: Human, Feline, Merling, Arachnae, Dwarf, Wood Elf, Hobgoblin, Siren, Half-Ogre, Sharkfolk, Kirin, Saurusfolk, Ratfolk, Shadow or Sporeling, all at normal player size) and a class (Origins: Classes). Races and classes are separate from the archetype trees.</p>
</section>
{''.join(pages)}
</body></html>"""


STYLES = {
    # the original look
    'classic': """
body { font-family: 'DejaVu Sans', sans-serif; font-size: 8.5pt; color: #222; margin: 0; }
h1 { font-size: 18pt; margin: 0 0 4px; } h2 { font-size: 12pt; margin: 0 0 4px; } h3 { font-size: 11pt; margin: 10px 0 4px; }
p { margin: 0 0 6px; color: #444; } .page { padding-left: 10px; }
td, th { border: 1px solid #cfd3da; padding: 3px 5px; vertical-align: top; text-align: left; }
th { background: #eef0f4; font-weight: bold; }
.tag { color: white; border-radius: 3px; padding: 0 4px; font-size: 7pt; font-weight: bold; }
.overview td, .overview th { font-size: 7.5pt; } .info th { width: 70px; } .desc { font-size: 8pt; }
""",
    # night-blue star chart, matching the in-game skill screen
    'astrologer': """
html, body { background: #0a0e24; }
body { font-family: 'Carlito', 'Liberation Sans', sans-serif; font-size: 9pt; color: #d6e0f5; margin: 0;
  background-image: radial-gradient(#ffffff55 0.6px, transparent 0.8px), radial-gradient(#9fb8ff33 0.6px, transparent 0.8px);
  background-size: 37px 41px, 23px 29px; background-position: 0 0, 11px 17px; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
h1, h2, h3 { font-family: 'Caladea', 'Liberation Serif', serif; color: #f2e3b8; letter-spacing: 0.04em; font-weight: normal; }
h1 { font-size: 22pt; margin: 0 0 4px; } h2 { font-size: 15pt; margin: 0 0 6px; } h3 { font-size: 12pt; margin: 12px 0 4px; }
p { margin: 0 0 6px; color: #b9c6e4; } .page { padding: 4px 0 0 12px; }
td, th { border: 1px solid #2a3560; padding: 3px 6px; vertical-align: top; text-align: left; }
th { background: #141b3c; color: #f2e3b8; font-weight: normal; letter-spacing: 0.03em; }
td { background: #0d1330cc; }
.tag { color: #0a0e24; border-radius: 8px; padding: 0 6px; font-size: 7pt; font-weight: bold; }
.overview td, .overview th { font-size: 7.5pt; } .info th { width: 80px; } .desc { font-size: 8.5pt; }
""",
    # parchment and rubric, like an illuminated manuscript
    'manuscript': """
html, body { background: #f1e6c8; }
body { font-family: 'Bitstream Charter', 'Liberation Serif', serif; font-size: 9pt; color: #2b2116; margin: 0;
  -webkit-print-color-adjust: exact; print-color-adjust: exact; }
h1, h2, h3 { font-family: 'Bitstream Charter', serif; color: #8a1c14; font-weight: bold; }
h1 { font-size: 22pt; margin: 0 0 4px; border-bottom: 2px solid #b8902f; padding-bottom: 4px; }
h2 { font-size: 15pt; margin: 0 0 6px; border-bottom: 1px solid #b8902f; } h3 { font-size: 12pt; margin: 12px 0 4px; }
p { margin: 0 0 6px; } .desc::first-letter { font-size: 22pt; color: #8a1c14; float: left; line-height: 1; padding-right: 4px; font-weight: bold; }
.page { padding: 2px 0 0 12px; border-left-width: 3px !important; }
td, th { border: 1px solid #c9b17a; padding: 3px 6px; vertical-align: top; text-align: left; }
th { background: #e6d5a8; color: #5a1610; } td { background: #f7eed6; }
.tag { color: #fdf6e3; border-radius: 0; padding: 0 5px; font-size: 7pt; font-variant: small-caps; }
.overview td, .overview th { font-size: 7.5pt; } .info th { width: 80px; font-variant: small-caps; } .desc { font-size: 9pt; }
""",
    # a clean, modern field guide with class colour bands
    'fieldguide': """
body { font-family: 'Inter', 'Liberation Sans', sans-serif; font-size: 8pt; color: #1d2330; margin: 0; }
h1 { font-size: 20pt; font-weight: 800; letter-spacing: -0.02em; margin: 0 0 4px; }
h2 { font-size: 14pt; font-weight: 700; margin: 0 0 6px; } h3 { font-size: 10pt; text-transform: uppercase; letter-spacing: 0.08em; color: #5a6478; margin: 12px 0 4px; }
p { margin: 0 0 6px; color: #4a5263; } .page { padding: 8px 0 0 14px; border-left-width: 10px !important; }
td, th { border-bottom: 1px solid #e3e6ec; padding: 3px 6px; vertical-align: top; text-align: left; }
th { font-weight: 600; color: #5a6478; font-size: 7pt; text-transform: uppercase; letter-spacing: 0.05em; }
.tag { color: white; border-radius: 10px; padding: 1px 7px; font-size: 6.5pt; font-weight: 600; }
.overview td, .overview th { font-size: 7.5pt; } .info th { width: 80px; } .desc { font-size: 8.5pt; color: #2b3240; }
""",
}


if __name__ == '__main__':
    main()
