#!/usr/bin/env python3
"""The faint astrolabe drawn behind the character screen (textures/gui/character/astrolabe.png), in the
Astrologer palette of the skill trees. Usage (from mdvlcraft-binder/): python3 tools/astro_character.py"""
import math
from pathlib import Path
from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parent.parent / 'src/main/resources/assets/mdvlcraft/textures/gui/character/astrolabe.png'
S, C = 256, 128
GOLD = (170, 140, 80)

im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
for r, a in ((124, 70), (118, 40), (92, 55), (60, 45), (26, 60)):
    d.ellipse((C - r, C - r, C + r, C + r), outline=GOLD + (a,), width=1)
for i in range(72):  # degree ticks on the outer ring
    t = i * math.pi / 36
    inner = 118 if i % 6 else 110
    d.line((C + inner * math.cos(t), C + inner * math.sin(t), C + 124 * math.cos(t), C + 124 * math.sin(t)), fill=GOLD + (50,))
for i in range(4):  # cross hairs
    t = i * math.pi / 4
    d.line((C - 124 * math.cos(t), C - 124 * math.sin(t), C + 124 * math.cos(t), C + 124 * math.sin(t)), fill=GOLD + (28,))
d.ellipse((C - 60, C - 92, C + 60, C + 28), outline=GOLD + (35,))  # the ecliptic, off centre
for x, y in ((C + 70, C - 40), (C - 52, C + 66), (C + 18, C + 92), (C - 88, C - 30)):  # a few stars
    d.ellipse((x - 2, y - 2, x + 2, y + 2), fill=(220, 230, 255, 110))
OUT.parent.mkdir(parents=True, exist_ok=True)
im.save(OUT)
print('wrote', OUT.relative_to(OUT.parents[6]))
