#!/usr/bin/env python3
"""The Astrologer's look for the MDVLCraft skill trees.

Gives every subject in the trees (each spell, Binder ability or technique, Epic Fight skill, bonus
attribute and Classes-tab archetype) its own star-chart icon drawn from that subject's original art,
redraws every node frame as an astrolabe ring, swaps the background for a night sky, recolours the
connections as constellation lines and restyles the window and tabs. Tree layouts are not touched.

Usage (from mdvlcraft-binder/):
  python3 tools/astro_theme.py <vanilla 1.20.1 client jar>

Mod textures are read from libs/modpack (tools/fetch_mods.py) and ../modpack/mods. Writes:
  src/main/resources/assets/mdvlcraft/textures/gui/astro/<kind>/<name>.png   one icon per subject
  src/main/resources/assets/mdvlcraft/textures/gui/skills/*                   frames, sky, window, tabs
  the icon of every node in data/mdvlcraft/puffish_skills/categories/*/definitions.json
  build/astro/icons.csv and build/astro/sheet.png                               what went where
"""
import colorsys
import csv
import glob
import hashlib
import io
import json
import math
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

BINDER = Path(__file__).resolve().parent.parent
RES = BINDER / 'src/main/resources'
CATS = RES / 'data/mdvlcraft/puffish_skills/categories'
GUI = RES / 'assets/mdvlcraft/textures/gui'
OUT_ICONS = GUI / 'astro'
REPORT = BINDER / 'build/astro'
JARS = sorted(glob.glob(str(BINDER / 'libs/modpack/*.jar'))) + sorted(glob.glob(str(BINDER.parent / 'modpack/mods/*.jar'))) + sys.argv[1:2]
SIZE = 32

# starlight colour per kind: (core, glow)
KIND_TINT = {
    'spell': ((236, 244, 255), (120, 170, 255)),
    'technique': ((246, 236, 255), (176, 128, 255)),
    'skill': ((255, 246, 222), (255, 196, 96)),
    'bonus': ((232, 238, 248), (150, 168, 210)),
    'archetype': ((240, 246, 255), (140, 200, 255)),
    'tab': ((240, 246, 255), (140, 200, 255)),
}
# ring colour per class family (frame file prefix)
FAMILY_TINT = {'mage': (130, 175, 255), 'warrior': (255, 170, 120), 'rogue': (196, 150, 255),
               'ranger': (140, 230, 170), 'neutral': (200, 212, 235)}
SKY = (8, 11, 30)
INK = (6, 8, 24)

# item icons that are rendered from an entity texture: (texture, face box)
ENTITY_ART = {
    'minecraft:creeper_head': ('minecraft:textures/entity/creeper/creeper.png', (8, 8, 16, 16)),
    'minecraft:zombie_head': ('minecraft:textures/entity/zombie/zombie.png', (8, 8, 16, 16)),
    'minecraft:skeleton_skull': ('minecraft:textures/entity/skeleton/skeleton.png', (8, 8, 16, 16)),
    'minecraft:wither_skeleton_skull': ('minecraft:textures/entity/skeleton/wither_skeleton.png', (8, 8, 16, 16)),
    'minecraft:shield': ('minecraft:textures/entity/shield_base_nopattern.png', (2, 2, 14, 24)),
}
_zips = {}


def load_texture(rl):
    ns, path = rl.split(':', 1)
    local = RES / 'assets' / ns / path
    if local.exists():
        return Image.open(local).convert('RGBA')
    name = f'assets/{ns}/{path}'
    for jar in JARS:
        z = _zips.get(jar) or _zips.setdefault(jar, zipfile.ZipFile(jar))
        try:
            return Image.open(io.BytesIO(z.read(name))).convert('RGBA')
        except KeyError:
            continue
    raise SystemExit(f'texture {rl} not found in the Binder, libs/modpack or the vanilla jar')


def source_image(icon):
    if icon['type'] == 'texture':
        im = load_texture(icon['data']['texture'])
    elif icon['type'] == 'item':
        ns, item = icon['data']['item'].split(':')
        item = {'enchanted_golden_apple': 'golden_apple'}.get(item, item)  # same picture, glint added in game
        if f'{ns}:{item}' in ENTITY_ART:  # heads and shields are drawn from entity skins
            rl, box = ENTITY_ART[f'{ns}:{item}']
            return load_texture(rl).crop(box)
        candidates = [f'{ns}:textures/item/{item}.png', f'{ns}:textures/block/{item}.png',
                      f'{ns}:textures/block/{item}_front.png', f'{ns}:textures/block/{item}_side.png',
                      f'{ns}:textures/item/{item}_head.png', f'{ns}:textures/block/{item}_top.png']
        if item.endswith('_block'):  # e.g. snow_block is drawn with block/snow
            candidates.append(f'{ns}:textures/block/{item[:-6]}.png')
        im = None
        for rl in candidates:
            try:
                im = load_texture(rl)
                break
            except SystemExit:
                continue
        if im is None:
            raise SystemExit(f'no texture for item {ns}:{item}')
    else:
        raise SystemExit(f'icon type {icon["type"]} is not handled')
    if im.height > im.width:  # animated strip: first frame
        im = im.crop((0, 0, im.width, im.width))
    return im


# ------------------------------------------------------------------------------------------- icons
def lum(px):
    r, g, b = px[:3]
    return (0.3 * r + 0.59 * g + 0.11 * b) / 255


def drop_specks(shape, smallest):
    """Remove bits of a binary shape smaller than `smallest` pixels."""
    px = shape.load()
    seen = set()
    for y in range(SIZE):
        for x in range(SIZE):
            if px[x, y] and (x, y) not in seen:
                group, stack = [], [(x, y)]
                seen.add((x, y))
                while stack:
                    cx, cy = stack.pop()
                    group.append((cx, cy))
                    for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                        if 0 <= nx < SIZE and 0 <= ny < SIZE and px[nx, ny] and (nx, ny) not in seen:
                            seen.add((nx, ny))
                            stack.append((nx, ny))
                if len(group) < smallest:
                    for c in group:
                        px[c] = 0
    return shape


def glyph_mask(src):
    """The shape worth keeping from an icon, as (intensity 0-255, binary shape, colour source).
    Cut-out art keeps its own silhouette; art painted on a full square keeps its brightest part."""
    im = src.resize((SIZE, SIZE), Image.NEAREST if src.width <= SIZE else Image.LANCZOS)
    px = im.load()
    cells = [(x, y) for y in range(SIZE) for x in range(SIZE)]
    opaque = sum(px[c][3] > 200 for c in cells) / len(cells)
    mask = Image.new('L', (SIZE, SIZE))
    mp = mask.load()
    if opaque > 0.85:
        lums = sorted(lum(px[c]) for c in cells)
        cut = lums[int(len(lums) * 0.60)]
        top = max(lums[-1], cut + 0.05)
        for x, y in cells:
            mp[x, y] = int(255 * max(0.0, min(1.0, (lum(px[x, y]) - cut) / (top - cut))))
        shape = mask.point(lambda a: 255 if a > 0 else 0)
        shape = shape.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3)).filter(ImageFilter.MedianFilter(3))
        shape = drop_specks(shape, 12)
    else:
        lums = [lum(px[c]) for c in cells if px[c][3] > 0] or [1.0]
        lo, hi = min(lums), max(max(lums), min(lums) + 0.05)
        for x, y in cells:
            a = px[x, y][3] / 255
            mp[x, y] = int(255 * a * (0.55 + 0.45 * (lum(px[x, y]) - lo) / (hi - lo))) if a > 0.15 else 0
        shape = mask.point(lambda a: 255 if a > 0 else 0)
    return mask, shape, im


def plate_icon(src, kind, key):
    """For art painted on a full square: the whole picture as a round starlight plate (night blue to
    the kind's starlight colour by brightness), so the subject stays recognisable."""
    core, glow = KIND_TINT[kind]
    im = src.resize((SIZE, SIZE), Image.NEAREST if src.width <= SIZE else Image.LANCZOS)
    px = im.load()
    lums = sorted(lum(px[x, y]) for y in range(SIZE) for x in range(SIZE))
    lo, hi = lums[int(len(lums) * 0.03)], max(lums[int(len(lums) * 0.98)], lums[0] + 0.1)
    rnd = random.Random(hashlib.md5(key.encode()).hexdigest())
    out = Image.new('RGBA', (SIZE, SIZE))
    op = out.load()
    c = (SIZE - 1) / 2
    radius = SIZE / 2 - 1.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - c, y - c)
            if d > radius:
                continue
            t = max(0.0, min(1.0, (lum(px[x, y]) - lo) / (hi - lo))) ** 1.15
            if t < 0.5:  # night blue to starlight colour
                u = t / 0.5
                col = [SKY[i] + (glow[i] - SKY[i]) * u for i in range(3)]
            else:  # starlight colour to white-hot core
                u = (t - 0.5) / 0.5
                col = [glow[i] + (core[i] - glow[i]) * u for i in range(3)]
            if t < 0.12 and rnd.random() < 0.035:  # faint stars in the dark
                col = [200, 210, 240]
            op[x, y] = (*[int(v) for v in col], 255)
    # brass-free starlight rim
    d = ImageDraw.Draw(out)
    d.ellipse((c - radius, c - radius, c + radius, c + radius), outline=(*glow, 255), width=1)
    halo = Image.new('RGBA', (SIZE, SIZE), (*glow, 0))
    halo.putalpha(out.split()[3].filter(ImageFilter.GaussianBlur(1.2)).point(lambda a: int(a * 0.5)))
    halo.alpha_composite(out)
    return halo


def star_icon(src, kind, key):
    """Engraved starlight: the subject's outline and main inner edges glow, the inside is a faint wash,
    with a soft halo and a tiny constellation of its own. Painted squares become plates instead."""
    core, glow = KIND_TINT[kind]
    probe = src.convert('RGBA').resize((SIZE, SIZE), Image.NEAREST)
    if sum(1 for a in probe.split()[3].getdata() if a > 200) > 0.85 * SIZE * SIZE:
        return plate_icon(src, kind, key)
    mask, shape, colour_src = glyph_mask(src)
    outline = ImageChops.subtract(shape, shape.filter(ImageFilter.MinFilter(3)))
    # inner edges: where brightness changes sharply inside the shape
    luma = colour_src.convert('L')
    edges = luma.filter(ImageFilter.FIND_EDGES).point(lambda v: 255 if v > 90 else 0)
    edges = ImageChops.multiply(edges, shape.filter(ImageFilter.MinFilter(3)))
    lines = ImageChops.lighter(outline, edges)
    out = Image.new('RGBA', (SIZE, SIZE))
    op = out.load()
    lp, sp, mp = lines.load(), shape.load(), mask.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if lp[x, y]:
                t = 0.55 + 0.45 * (mp[x, y] / 255)
                op[x, y] = (*[int(core[i] * t + glow[i] * (1 - t)) for i in range(3)], 255)
            elif sp[x, y]:
                t = mp[x, y] / 255
                op[x, y] = (*glow, int(40 + 70 * t))
    solid = out.split()[3].point(lambda a: 255 if a > 30 else 0)
    ring = ImageChops.subtract(solid.filter(ImageFilter.MaxFilter(3)), solid)
    halo = Image.new('RGBA', (SIZE, SIZE), (*glow, 0))
    halo.putalpha(lines.filter(ImageFilter.GaussianBlur(1.4)).point(lambda a: int(a * 0.7)))
    final = Image.new('RGBA', (SIZE, SIZE))
    final.paste(Image.new('RGBA', (SIZE, SIZE), (*INK, 170)), (0, 0), ring)
    final.alpha_composite(halo)
    final.alpha_composite(out)
    rnd = random.Random(hashlib.md5(key.encode()).hexdigest())
    fp = final.load()
    placed = 0
    for _ in range(60):
        x, y = rnd.choice([(rnd.randrange(1, 7), rnd.randrange(1, 7)), (rnd.randrange(25, 31), rnd.randrange(1, 7)),
                           (rnd.randrange(1, 7), rnd.randrange(25, 31)), (rnd.randrange(25, 31), rnd.randrange(25, 31))])
        if all(fp[x + dx, y + dy][3] == 0 for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            fp[x, y] = (*core, 230)
            placed += 1
            if placed == 3:
                break
    return final


# ------------------------------------------------------------------------------------------ frames
def astrolabe(size, kind, state, tint):
    """One node frame: an astrolabe ring, ticked for milestones, glowing once learned."""
    scale = 4
    s = size * scale
    im = Image.new('RGBA', (s, s))
    d = ImageDraw.Draw(im)
    dim = {'locked': 0.38, 'available': 0.62, 'affordable': 0.85, 'unlocked': 1.0, 'excluded': 0.25}[state]
    ring = tuple(int(c * dim + 30 * (1 - dim)) for c in tint)
    if state == 'excluded':
        ring = (90, 60, 80)
    c = s / 2
    if kind == 'twig':
        r = s * 0.3
        d.ellipse((c - r, c - r, c + r, c + r), fill=(*SKY, 235), outline=ring, width=scale)
        if state == 'unlocked':
            r2 = s * 0.12
            d.ellipse((c - r2, c - r2, c + r2, c + r2), fill=(240, 246, 255))
    else:
        r = s / 2 - scale * 2
        d.ellipse((c - r, c - r, c + r, c + r), fill=(*SKY, 235), outline=ring, width=scale * (2 if kind != 'task' else 1) + scale // 2)
        inner = r - scale * 3
        d.ellipse((c - inner, c - inner, c + inner, c + inner), outline=(*ring, 110), width=max(1, scale // 2))
        ticks = {'task': 4, 'root': 8, 'goal': 8, 'challenge': 12}[kind]
        for i in range(ticks):
            a = i * 2 * math.pi / ticks - math.pi / 2
            r1 = r - scale * (2.5 if i % 2 == 0 else 1.5)
            d.line((c + r1 * math.cos(a), c + r1 * math.sin(a), c + r * math.cos(a), c + r * math.sin(a)), fill=ring, width=scale)
        if kind in ('challenge', 'root'):  # a star point on top for the big milestones
            p = [(c, c - r - scale * 2), (c + scale * 2, c - r + scale), (c - scale * 2, c - r + scale)]
            d.polygon(p, fill=ring)
        if state == 'affordable':
            d.arc((c - r - scale, c - r - scale, c + r + scale, c + r + scale), 200, 340, fill=(255, 214, 120), width=scale)
    im = im.resize((size, size), Image.LANCZOS)
    if state == 'unlocked':
        glow = Image.new('RGBA', im.size, (*tint, 0))
        glow.putalpha(im.split()[3].filter(ImageFilter.GaussianBlur(size / 14)).point(lambda a: int(a * 0.6)))
        glow.alpha_composite(im)
        im = glow
    return im


def sky_tile(n=128):
    rnd = random.Random(7)
    im = Image.new('RGBA', (n, n), (*SKY, 255))
    px = im.load()
    for y in range(n):
        for x in range(n):
            v = rnd.uniform(-2, 2)
            px[x, y] = (int(SKY[0] + v), int(SKY[1] + v), int(SKY[2] + 2 * v), 255)
    for _ in range(70):
        x, y = rnd.randrange(n), rnd.randrange(n)
        b = rnd.choice([70, 90, 120, 160, 210])
        px[x, y] = (b, b, min(255, b + 30), 255)
        if b > 150:  # a few brighter stars with a soft cross
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = ((x + dx) % n, (y + dy) % n)
                r, g, bb, _ = px[q]
                px[q] = (min(255, r + 25), min(255, g + 25), min(255, bb + 35), 255)
    return im


def restyle_chrome(path):
    """Window and tabs: keep the shape, repaint in midnight blue with a brass edge."""
    im = Image.open(path).convert('RGBA')
    px = im.load()
    alpha = im.split()[3]
    edge = ImageChops.subtract(alpha.point(lambda a: 255 if a > 0 else 0),
                               alpha.point(lambda a: 255 if a > 0 else 0).filter(ImageFilter.MinFilter(3)))
    ep = edge.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = lum((r, g, b))
            if ep[x, y]:
                px[x, y] = (170, 140, 80, a)
            else:
                px[x, y] = (int(14 + 40 * l), int(18 + 46 * l), int(44 + 70 * l), a)
    im.save(path)


# --------------------------------------------------------------------------------------- inventory
def subject(cat, node, defn):
    rewards = defn.get('rewards', [])
    if cat == 'classes':
        return 'archetype', node
    for r in rewards:
        if r['type'] == 'mdvlcraft:ability':
            rid = r['data']['ability']
            return ('technique' if rid.startswith('mdvlcraft:') else 'spell'), rid.replace(':', '_')
    for r in rewards:
        if r['type'] == 'mdvlcraft:epicfight_skill':
            return 'skill', r['data']['skill'].replace(':', '_')
    attrs = [r['data']['attribute'] for r in rewards if r['type'] == 'puffish_skills:attribute']
    if attrs:
        return 'bonus', attrs[0].replace(':', '_').replace('.', '_')
    raise SystemExit(f'{cat}/{node} grants nothing the theme knows how to draw: {rewards}')


def main():
    config = json.loads((CATS.parent / 'config.json').read_text())
    subjects = {}  # (kind, name) -> source icon, preferring the larger "attribute" art for bonuses
    plan = []
    for cat in config['categories']:
        defs = json.loads((CATS / cat / 'definitions.json').read_text())
        skills = json.loads((CATS / cat / 'skills.json').read_text())
        for node, placed in skills.items():
            defn = defs[placed['definition']]
            kind, name = subject(cat, node, defn)
            icon = defn['icon']
            if kind == 'spell':  # draw spells from the spell's own icon, whatever the node showed
                ns, spell = name.split('_', 1) if name.startswith('traveloptics_') else ('irons_spellbooks', name[len('irons_spellbooks_'):])
                icon = {'type': 'texture', 'data': {'texture': f'{ns}:textures/gui/spell_icons/{spell}.png'}}
            prev = subjects.get((kind, name))
            if prev is None or ('/twig/' in json.dumps(prev) and '/twig/' not in json.dumps(icon)):  # prefer full-size art
                subjects[(kind, name)] = icon
            plan.append((cat, node, placed['definition'], kind, name))
    # icons
    REPORT.mkdir(parents=True, exist_ok=True)
    made = {}
    for (kind, name), icon in sorted(subjects.items()):
        out = OUT_ICONS / kind / f'{name}.png'
        out.parent.mkdir(parents=True, exist_ok=True)
        star_icon(source_image(icon), kind, f'{kind}/{name}').save(out)
        made[(kind, name)] = f'mdvlcraft:textures/gui/astro/{kind}/{name}.png'
    # point every node at its icon
    rows = []
    for cat in config['categories']:
        path = CATS / cat / 'definitions.json'
        raw = path.read_text()
        defs = json.loads(raw)
        for c, node, definition, kind, name in plan:
            if c != cat:
                continue
            old = json.dumps(defs[definition]['icon'])
            defs[definition]['icon'] = {'type': 'texture', 'data': {'texture': made[(kind, name)]}}
            rows.append((cat, node, defs[definition]['title'], kind, name, made[(kind, name)], old))
        path.write_text(json.dumps(defs, indent=2, ensure_ascii='\\u' in raw) + ('\n' if raw.endswith('\n') else ''))
        # sky and constellation lines
        cpath = CATS / cat / 'category.json'
        craw = cpath.read_text()
        cdef = json.loads(craw)
        cdef['background'] = {'texture': 'mdvlcraft:textures/gui/skills/astro_sky.png', 'width': 128, 'height': 128, 'position': 'tile'}
        cdef['colors'] = {
            'connections': {
                'locked': {'fill': '#26305a', 'stroke': '#05070f'},
                'available': {'fill': '#4c5c96', 'stroke': '#05070f'},
                'affordable': {'fill': '#e0c27a', 'stroke': '#05070f'},
                'unlocked': {'fill': '#d6e4ff', 'stroke': '#05070f'},
                'excluded': {'fill': '#1a1e36', 'stroke': '#05070f'},
            },
            'points': {'fill': '#d6e4ff', 'stroke': '#05070f'},
        }
        cpath.write_text(json.dumps(cdef, indent=2, ensure_ascii='\\u' in craw) + ('\n' if craw.endswith('\n') else ''))
    # tab icons
    (OUT_ICONS / 'tab').mkdir(parents=True, exist_ok=True)
    for tab in sorted((GUI / 'icons/tab').glob('*.png')):
        star_icon(Image.open(tab).convert('RGBA'), 'tab', f'tab/{tab.stem}').save(OUT_ICONS / 'tab' / f'{tab.stem}.png')
    for cat in config['categories']:
        cpath = CATS / cat / 'category.json'
        craw = cpath.read_text()
        cdef = json.loads(craw)
        tex = cdef['icon']['data'].get('texture', '')
        stem = tex.rsplit('/', 1)[-1][:-4]
        if (OUT_ICONS / 'tab' / f'{stem}.png').exists():
            cdef['icon'] = {'type': 'texture', 'data': {'texture': f'mdvlcraft:textures/gui/astro/tab/{stem}.png'}}
            cpath.write_text(json.dumps(cdef, indent=2, ensure_ascii='\\u' in craw) + ('\n' if craw.endswith('\n') else ''))
    # frames, sky, window
    skills_gui = GUI / 'skills'
    for f in sorted(skills_gui.glob('frame_*.png')):
        _, family, kind, state = f.stem.split('_')
        astrolabe(Image.open(f).width, kind, state, FAMILY_TINT[family]).save(f)
    sky_tile().save(skills_gui / 'astro_sky.png')
    for chrome in ('window.png', 'tabs.png'):
        restyle_chrome(skills_gui / chrome)
    # report
    with open(REPORT / 'icons.csv', 'w', newline='') as fh:
        w = csv.writer(fh)
        w.writerow(['tab', 'node', 'title', 'kind', 'subject', 'icon', 'old icon'])
        w.writerows(rows)
    kinds = {}
    for (kind, name) in made:
        kinds.setdefault(kind, []).append(name)
    cell = 40
    cols = 24
    total = len(made)
    sheet = Image.new('RGBA', (cols * cell, ((total + cols - 1) // cols) * cell), (*SKY, 255))
    for i, ((kind, name), _) in enumerate(sorted(made.items())):
        ic = Image.open(OUT_ICONS / kind / f'{name}.png')
        sheet.alpha_composite(ic, ((i % cols) * cell + 4, (i // cols) * cell + 4))
    sheet.save(REPORT / 'sheet.png')
    print(f'{len(rows)} nodes in {len(config["categories"])} tabs -> {total} icons:',
          ', '.join(f'{len(v)} {k}' for k, v in sorted(kinds.items())))


if __name__ == '__main__':
    main()
