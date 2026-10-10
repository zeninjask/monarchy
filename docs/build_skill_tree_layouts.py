#!/usr/bin/env python3
"""Build alternative layouts of the skill-tree document (same look as MDVLCraft-Skill-Trees.pdf), into
docs/layouts/. Shorter than the main document: one-line summaries, class-wide bonuses listed once per class,
and the core stats only in the overview table. Run from the repository root:
python3 docs/build_skill_tree_layouts.py <pack version>
"""
import html, subprocess, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_skill_tree_doc as base  # noqa: E402  (loads the mod names; reads the version from argv)

e = html.escape
OUT = base.ROOT / 'docs/layouts'
CORE = ['Max Mana', 'Mana Regen', 'Max Health', 'Natural Regeneration', 'Armor', 'Cooldown Reduction',
        'Movement Speed', 'Attack Damage', 'Max Stamina', 'Stamina Regen']
HEADS = ['Max<br>Mana', 'Mana<br>Regen', 'Max<br>Health', 'Health<br>Regen', 'Armor', 'Cooldown<br>Red.',
         'Move<br>Speed', 'Attack<br>Dmg', 'Max<br>Stamina', 'Stamina<br>Regen']


def collect():
    data = {a: base.tree(a) for c in base.CLASSES.values() for a in c[2]}
    for a, skills in base.class_rewards().items():
        data[a]['ef'] += skills
    shared = {}
    for cid, (_, _, archs) in base.CLASSES.items():
        shared[cid] = set.intersection(*[{(b[0], b[1]) for b in data[a]['bonuses']} for a in archs])
    for cid, (_, _, archs) in base.CLASSES.items():
        for a in archs:
            t = data[a]
            t['class'] = cid
            t['tagline'] = base.DESCRIPTIONS[a].split('. ')[0].rstrip('.') + '.'
            t['own'] = [(n, v) for n, v, _, _ in t['bonuses'] if (n, v) not in shared[cid] and n not in CORE]
            t['core'] = {n: v for n, v, _, _ in t['bonuses'] if n in CORE}
    return data, shared


def tag(cid):
    name, colour, _ = base.CLASSES[cid]
    return f"<span class=tag style='background:{colour}'>{name}</span>"


def lst(items):
    return e(', '.join(items)) if items else '–'


def overview(data):
    rows = []
    for cid, (_, _, archs) in base.CLASSES.items():
        for a in archs:
            t = data[a]
            rows.append(f"<tr><td><b>{a.title()}</b></td><td>{tag(cid)}</td><td class=n>{t['skills']}</td><td class=n>{t['cost']}</td>"
                        + ''.join(f"<td class=n>{t['core'].get(c, '–')}</td>" for c in CORE)
                        + f"<td class=n>{len(t['spells'])}</td><td class=n>{len(t['techniques'])}</td><td class=n>{len(t['ef'])}</td></tr>")
    return (f"<table class=overview><tr><th>Archetype</th><th>Class</th><th>Skills</th><th>Cost</th>{''.join(f'<th>{h}</th>' for h in HEADS)}"
            f"<th>Spells</th><th>Techn.</th><th>EF<br>skills</th></tr>{''.join(rows)}</table>")


def class_line(cid, shared):
    items = sorted(f'{n} {v}' for n, v in shared[cid] if n not in CORE)
    return f"<p class=shared>{tag(cid)} every {base.CLASSES[cid][0]} archetype: {e(', '.join(items)) or '–'}</p>"


def intro(data):
    return f"""<h1>MDVLCraft skill trees</h1>
<p>Version {e(base.VERSION)}. Totals for the <b>whole</b> tree. Pick 2 archetypes; each tree gives 1 point to start and 1 per level.
Percentages add to the base; Max Health is in half-hearts. Core stats are in this table only; bonuses shared by a whole class are listed once per class.
Everyone knows Guard, Parrying, Absolute Deflection and the souls-like dodge and step; Assassin, Phantom, Scout and Thief also get Precise Parry.</p>
{overview(data)}"""


def card(a, t, cls='card'):
    own = ''.join(f"<tr><td>{e(n)}</td><td class=n>{e(v)}</td></tr>" for n, v in t['own'])
    return f"""<div class={cls} style='border-left:5px solid {base.CLASSES[t['class']][1]}'>
<h2>{a.title()} {tag(t['class'])} <span class=meta>{t['skills']} skills · {t['cost']} pts</span></h2>
<p class=desc>{e(t['tagline'])}</p>
<table class=info><tr><th>Spells</th><td>{lst(t['spells'])}</td></tr><tr><th>Techniques</th><td>{lst(t['techniques'])}</td></tr>
<tr><th>Epic Fight</th><td>{lst(t['ef'])}</td></tr><tr><th>Bonuses</th><td>{e(', '.join(f'{n} {v}' for n, v in t['own'])) or '–'}</td></tr></table>
</div>"""


# Layout A: one page per class, its archetypes side by side
def layout_class_sheets(data, shared):
    pages = []
    for cid, (_, _, archs) in base.CLASSES.items():
        head = ''.join(f"<th>{a.title()}<br><span class=meta>{data[a]['skills']} skills · {data[a]['cost']} pts</span></th>" for a in archs)
        def row(label, fn):
            return f"<tr><th class=rh>{label}</th>{''.join(f'<td>{fn(data[a])}</td>' for a in archs)}</tr>"
        pages.append(f"""<section class=page>{class_line(cid, shared)}
<table class=sheet><tr><th></th>{head}</tr>
{row('Role', lambda t: e(t['tagline']))}
{row('Spells', lambda t: lst(t['spells']))}
{row('Techniques', lambda t: lst(t['techniques']))}
{row('Epic Fight', lambda t: lst(t['ef']))}
{row('Bonuses', lambda t: '<br>'.join(e(f'{n} {v}') for n, v in t['own']) or '–')}
</table></section>""")
    return ''.join(pages)


# Layout B: two archetypes per page
def layout_two_up(data, shared):
    out, cards = [], []
    for cid, (_, _, archs) in base.CLASSES.items():
        for i, a in enumerate(archs):
            cards.append((card(a, data[a], 'half'), class_line(cid, shared) if i == 0 else ''))
    for i in range(0, len(cards), 2):
        pair = cards[i:i + 2]
        out.append("<section class=page>" + ''.join(c[1] for c in pair) + "<div class=two>" + ''.join(c[0] for c in pair) + "</div></section>")
    return ''.join(out)


# Layout C: master tables (abilities for everyone, then a bonus matrix per class)
def layout_tables(data, shared):
    rows = []
    for cid, (_, _, archs) in base.CLASSES.items():
        for a in archs:
            t = data[a]
            rows.append(f"<tr><td><b>{a.title()}</b><br>{tag(cid)}</td><td class=role>{e(t['tagline'])}</td><td>{lst(t['spells'])}</td><td>{lst(t['techniques'])}</td><td>{lst(t['ef'])}</td></tr>")
    abil = f"<section class=page><h3>Abilities</h3><table class=master><tr><th>Archetype</th><th>Role</th><th>Spells</th><th>Techniques</th><th>Epic Fight skills</th></tr>{''.join(rows)}</table></section>"
    mats = []
    for cid, (_, _, archs) in base.CLASSES.items():
        names = sorted({n for a in archs for n, _ in data[a]['own']})
        lookup = {a: dict(data[a]['own']) for a in archs}
        body = ''.join(f"<tr><td>{e(n)}</td>{''.join(f'<td class=n>{e(lookup[a].get(n, chr(8211)))}</td>' for a in archs)}</tr>" for n in names)
        mats.append(f"<div class=mat>{class_line(cid, shared)}<table class=matrix><tr><th>Bonus</th>{''.join(f'<th class=n>{a.title()}</th>' for a in archs)}</tr>{body}</table></div>")
    return abil + "<section class=page><h3>Bonuses by class</h3><div class=mats>" + ''.join(mats) + "</div></section>"


# Layout D: 2 x 2 cards per page, a class per page (Mage takes two)
def layout_quadrants(data, shared):
    out = []
    for cid, (_, _, archs) in base.CLASSES.items():
        for i in range(0, len(archs), 4):
            group = archs[i:i + 4]
            out.append(f"<section class=page>{class_line(cid, shared) if i == 0 else ''}<div class=quad>{''.join(card(a, data[a], 'qcard') for a in group)}</div></section>")
    return ''.join(out)


EXTRA_CSS = """
body { font-size: 8pt; } .tag { font-size: 6.5pt; } .meta { font-size: 7pt; color: #777; font-weight: normal; }
.page { padding-left: 0; } p.shared { margin: 0 0 6px; color: #333; } h2 { font-size: 11pt; }
.info { width: 100%; } .info th { width: 62px; } .desc { font-style: italic; margin: 0 0 4px; }
table.sheet { width: 100%; table-layout: fixed; } table.sheet th.rh { width: 60px; } table.sheet td { font-size: 7.2pt; }
.two { display: flex; gap: 12px; align-items: flex-start; } .half { flex: 1; padding-left: 8px; }
.quad { display: grid; grid-template-columns: 1fr 1fr; gap: 10px 12px; } .qcard { padding-left: 8px; } .qcard td, .qcard th { font-size: 7pt; }
table.master { width: 100%; table-layout: fixed; } table.master td { font-size: 6.8pt; } table.master td.role { font-style: italic; }
table.master th:nth-child(1) { width: 70px; } table.master th:nth-child(2) { width: 120px; } table.master th:nth-child(4) { width: 130px; } table.master th:nth-child(5) { width: 150px; }
.mats { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 14px; } table.matrix td, table.matrix th { font-size: 6.8pt; padding: 1px 4px; }
"""

LAYOUTS = {'A-class-sheets': layout_class_sheets, 'B-two-up': layout_two_up, 'C-tables': layout_tables, 'D-quadrants': layout_quadrants}


def main():
    data, shared = collect()
    chrome = next((c for c in base.CHROMES if Path(c).exists()), None)
    OUT.mkdir(exist_ok=True)
    for key, fn in LAYOUTS.items():
        doc = f"""<!doctype html><html><head><meta charset=utf-8><title>MDVLCraft Skill Trees</title><style>
@page {{ size: A4 landscape; margin: 10mm 12mm; }}
table {{ border-collapse: collapse; }} .n {{ text-align: right; }} .page {{ page-break-before: always; }}
{base.CSS}{EXTRA_CSS}</style></head><body>{intro(data)}{fn(data, shared)}</body></html>"""
        html_path = OUT / f'MDVLCraft-Skill-Trees-{key}.html'
        html_path.write_text(doc)
        if chrome:
            subprocess.run([chrome, '--headless', '--no-sandbox', '--disable-gpu', '--no-pdf-header-footer',
                            '--print-to-pdf=' + str(html_path.with_suffix('.pdf')), html_path.as_uri()], check=True, capture_output=True)
        print('wrote', html_path.with_suffix('.pdf').relative_to(base.ROOT))


if __name__ == '__main__':
    main()
