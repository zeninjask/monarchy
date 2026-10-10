#!/usr/bin/env python3
"""The Astrologer's look for the MDVLCraft skill trees.

Gives every subject in the trees (each spell, Binder ability or technique, Epic Fight skill, bonus
attribute and Classes-tab archetype) its own star-chart icon drawn from that subject's original art as
Minecraft-style pixel art: one texture pixel per GUI pixel at the size the node draws it (12, 24 or 32 pixels;
tab icons 16), solid pixels only. Every icon is redrawn from tools/astro_sources.json (the original art of each
subject) on every run, so the result does not depend on earlier runs. It also
redraws every node frame as an astrolabe ring, swaps the background for a night sky, recolours the
connections as constellation lines and restyles the window and tabs. Tree layouts are not touched.

Usage (from mdvlcraft-binder/):
  python3 tools/astro_theme.py <vanilla 1.20.1 client jar>

Mod textures are read from libs/modpack (tools/fetch_mods.py) and ../modpack/mods. Writes:
  src/main/resources/assets/mdvlcraft/textures/gui/astro/<kind>/<px>/<name>.png   one icon per subject and size
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
SOURCES = Path(__file__).with_name('astro_sources.json')  # each subject's original art (kind/name -> icon)
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


# ------------------------------------------------------------------------------- pixel-art icons
def _ramp(kind, steps):
    """Night sky to the kind's starlight colour to its white-hot core, in `steps` solid colours."""
    core, glow = KIND_TINT[kind]
    base = tuple(int(SKY[i] + (glow[i] - SKY[i]) * 0.28) for i in range(3))
    out = []
    for i in range(steps):
        t = i / (steps - 1)
        if t < 0.6:
            u = t / 0.6
            out.append(tuple(int(base[i2] + (glow[i2] - base[i2]) * u) for i2 in range(3)))
        else:
            u = (t - 0.6) / 0.4
            out.append(tuple(int(glow[i2] + (core[i2] - glow[i2]) * u) for i2 in range(3)))
    return out


def _fit(src, n, fill):
    """The source's content on an n x n canvas, one source pixel per canvas pixel when it already fits well
    (pixel art stays exact), otherwise resampled once to fill `fill` pixels."""
    src = src.convert('RGBA')
    box = src.split()[3].point(lambda a: 255 if a > 24 else 0).getbbox() or (0, 0, src.width, src.height)
    art = src.crop(box)
    w, h = art.size
    if max(w, h) <= n and max(w, h) >= 0.6 * n:
        scaled = art
    else:
        f = fill / max(w, h)
        size = (max(1, round(w * f)), max(1, round(h * f)))
        scaled = art.resize(size, Image.BOX if f < 1 else Image.BICUBIC)
    canvas = Image.new('RGBA', (n, n))
    canvas.alpha_composite(scaled, ((n - scaled.width) // 2, (n - scaled.height) // 2))
    return canvas


def _stars(im, kind, key, count):
    core = KIND_TINT[kind][0]
    n = im.width
    px = im.load()
    rnd = random.Random(hashlib.md5(key.encode()).hexdigest())
    corners = [(1, 1), (n - 2, 1), (1, n - 2), (n - 2, n - 2)]
    rnd.shuffle(corners)
    placed = 0
    for cx, cy in corners:
        if placed == count:
            break
        if all(px[x, y][3] == 0 for x in range(max(0, cx - 1), min(n, cx + 2)) for y in range(max(0, cy - 1), min(n, cy + 2))):
            px[cx, cy] = (*core, 255)
            placed += 1


def pixel_plate(src, kind, key, n):
    """Art painted on a full square: a round starlit disc of the picture, solid pixels only."""
    ramp = _ramp(kind, 6)
    glow = KIND_TINT[kind][1]
    im = src.convert('RGBA').resize((n, n), Image.BOX if src.width > n else Image.BICUBIC)
    px = im.load()
    c = (n - 1) / 2
    radius = n / 2 - 0.5
    inside = [(x, y) for y in range(n) for x in range(n) if math.hypot(x - c, y - c) <= radius]
    lums = sorted(lum(px[p]) for p in inside)
    lo = lums[int(len(lums) * 0.04)]
    hi = max(lums[int(len(lums) * 0.97)], lo + 0.1)
    out = Image.new('RGBA', (n, n))
    op = out.load()
    cells = set(inside)
    for x, y in inside:
        rim = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if rim:
            op[x, y] = (*glow, 255)
        else:
            t = max(0.0, min(1.0, (lum(px[x, y]) - lo) / (hi - lo)))
            op[x, y] = (*ramp[min(5, int(t * 5.999))], 255)
    return out


def pixel_icon(src, kind, key, n):
    """The Astrologer's icon as Minecraft-style pixel art: one texture pixel per GUI pixel at the size it is
    drawn (n x n), solid pixels only. The subject's silhouette glows along its edge, the inside keeps the art's
    shading in starlight tones, a dark one-pixel outline sets it off the night sky, and a star or two sits in
    the corners. Pictures painted on a full square become a starlit disc instead."""
    probe = src.convert('RGBA')
    if sum(1 for a in probe.split()[3].getdata() if a > 200) > 0.85 * probe.width * probe.height:
        out = pixel_plate(src, kind, key, n)
        if n >= 24:
            _stars(out, kind, key, 2)
        return out
    ramp = _ramp(kind, 5)
    art = _fit(src, n, n - 2)
    ap = art.load()
    shape = {(x, y) for y in range(n) for x in range(n) if ap[x, y][3] >= 128}
    if not shape:
        raise SystemExit(f'{key}: nothing left of the art at {n}px')
    lums = sorted(lum(ap[p]) for p in shape)
    lo = lums[int(len(lums) * 0.05)]
    hi = max(lums[int(len(lums) * 0.95)], lo + 0.08)  # flat art still gets a range
    level = {p: max(0.0, min(1.0, (lum(ap[p]) - lo) / (hi - lo))) for p in shape}
    out = Image.new('RGBA', (n, n))
    op = out.load()
    for (x, y) in shape:
        nbrs = [(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)]
        if any(q not in shape for q in nbrs):  # the silhouette's edge: starlight
            op[x, y] = (*ramp[4 if level[(x, y)] > 0.35 else 3], 255)
        else:
            idx = min(3, int(level[(x, y)] * 3.999))
            # inner edges (a sharp change in the art's shading) catch the light one step brighter
            if any(q in shape and level[(x, y)] - level[q] > 0.45 for q in nbrs):
                idx = min(4, idx + 1)
            op[x, y] = (*ramp[idx], 255)
    for y in range(n):  # the dark outline around it
        for x in range(n):
            if (x, y) not in shape and any((x + dx, y + dy) in shape for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
                op[x, y] = (*INK, 255)
    if n >= 24:
        _stars(out, kind, key, 2)
    return out


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


def icon_px(defn):
    """The size puffish_skills draws a node's texture icon at (16 GUI pixels x the node size, rounded as it does)."""
    return round(8 * defn.get('size', 1.0)) * 2


def main():
    config = json.loads((CATS.parent / 'config.json').read_text())
    sources = json.loads(SOURCES.read_text())
    needed = {}  # (kind, name, px) -> original art
    plan = []
    for cat in config['categories']:
        defs = json.loads((CATS / cat / 'definitions.json').read_text())
        skills = json.loads((CATS / cat / 'skills.json').read_text())
        for node, placed in skills.items():
            defn = defs[placed['definition']]
            kind, name = subject(cat, node, defn)
            icon = sources.get(f'{kind}/{name}')
            if icon is None and kind == 'spell':  # spells added later are drawn from the spell's own icon
                ns, spell = name.split('_', 1) if name.startswith('traveloptics_') else ('irons_spellbooks', name[len('irons_spellbooks_'):])
                icon = {'type': 'texture', 'data': {'texture': f'{ns}:textures/gui/spell_icons/{spell}.png'}}
            if icon is None:
                raise SystemExit(f'{cat}/{node}: no original art for {kind}/{name}; add it to {SOURCES.name}')
            px = icon_px(defn)
            needed[(kind, name, px)] = icon
            plan.append((cat, node, placed['definition'], kind, name, px))
    # icons: one per subject and size, drawn fresh from the original art every run
    REPORT.mkdir(parents=True, exist_ok=True)
    for old in OUT_ICONS.glob('*/*.png'):
        if old.parent.name != 'tab':
            old.unlink()
    made = {}
    for (kind, name, px), icon in sorted(needed.items()):
        out = OUT_ICONS / kind / str(px) / f'{name}.png'
        out.parent.mkdir(parents=True, exist_ok=True)
        pixel_icon(source_image(icon), kind, f'{kind}/{name}', px).save(out)
        made[(kind, name, px)] = f'mdvlcraft:textures/gui/astro/{kind}/{px}/{name}.png'
    # point every node at its icon
    rows = []
    for cat in config['categories']:
        path = CATS / cat / 'definitions.json'
        raw = path.read_text()
        defs = json.loads(raw)
        for c, node, definition, kind, name, px in plan:
            if c != cat:
                continue
            defs[definition]['icon'] = {'type': 'texture', 'data': {'texture': made[(kind, name, px)]}}
            rows.append((cat, node, defs[definition]['title'], kind, name, px, made[(kind, name, px)]))
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
        pixel_icon(Image.open(tab).convert('RGBA'), 'tab', f'tab/{tab.stem}', 16).save(OUT_ICONS / 'tab' / f'{tab.stem}.png')
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
    # the window and tab strip are recoloured in place, so only once (tools/.astro_chrome_done marks it)
    marker = Path(__file__).with_name('.astro_chrome_done')
    if not marker.exists():
        for chrome in ('window.png', 'tabs.png'):
            restyle_chrome(skills_gui / chrome)
        marker.write_text('window.png and tabs.png already restyled\n')
    # report
    with open(REPORT / 'icons.csv', 'w', newline='') as fh:
        w = csv.writer(fh)
        w.writerow(['tab', 'node', 'title', 'kind', 'subject', 'px', 'icon'])
        w.writerows(rows)
    kinds = {}
    for (kind, name, px) in made:
        kinds.setdefault(kind, []).append(name)
    cell = 40
    cols = 24
    total = len(made)
    sheet = Image.new('RGBA', (cols * cell, ((total + cols - 1) // cols) * cell), (*SKY, 255))
    for i, ((kind, name, px), _) in enumerate(sorted(made.items())):
        ic = Image.open(OUT_ICONS / kind / str(px) / f'{name}.png')
        sheet.alpha_composite(ic, ((i % cols) * cell + 4, (i // cols) * cell + 4))
    sheet.save(REPORT / 'sheet.png')
    print(f'{len(rows)} nodes in {len(config["categories"])} tabs -> {total} icons:',
          ', '.join(f'{len(v)} {k}' for k, v in sorted(kinds.items())))


if __name__ == '__main__':
    main()
