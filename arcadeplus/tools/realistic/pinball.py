import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw, ImageFilter
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
U = 1 / 16.0
random.seed(7)

def grad(w, h, top, bot):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bot)) + (255,))
    return img

def stars(img, n=300, area=1.0):
    d = ImageDraw.Draw(img); w, h = img.size
    for _ in range(n):
        x, y, r = random.random() * w, random.random() * h * area, random.random() * 1.8 + 0.4
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, random.randint(120, 255)))

def planet(img, cx, cy, r, c1, c2, ring=False):
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0)); d = ImageDraw.Draw(lay)
    for k in range(int(r), 0, -2):
        t = k / r; col = tuple(int(a * t + b * (1 - t)) for a, b in zip(c1, c2)) + (255,)
        d.ellipse([cx - k + (r - k) * 0.4, cy - k + (r - k) * 0.4, cx + k + (r - k) * 0.4, cy + k + (r - k) * 0.4], fill=col)
    if ring: d.ellipse([cx - r * 1.8, cy - r * 0.35, cx + r * 1.8, cy + r * 0.35], outline=(230, 220, 200, 220), width=max(3, int(r / 10)))
    img.alpha_composite(lay)

def moon(img, cx, cy, r):
    lay = Image.new("RGBA", img.size, (0, 0, 0, 0)); d = ImageDraw.Draw(lay)
    d.ellipse([cx - r * 1.6, cy - r * 1.6, cx + r * 1.6, cy + r * 1.6], fill=(200, 220, 160, 50))
    img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(r / 3)))
    d = ImageDraw.Draw(img); d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(240, 240, 210, 255))

def bat(d, x, y, s, col=(10, 6, 14, 255)):
    pts = [(x, y), (x - s, y - s * 0.5), (x - s * 1.6, y - s * 0.1), (x - s * 1.2, y + s * 0.15), (x - s * 0.8, y), (x - s * 0.5, y + s * 0.3), (x, y + s * 0.15),
           (x + s * 0.5, y + s * 0.3), (x + s * 0.8, y), (x + s * 1.2, y + s * 0.15), (x + s * 1.6, y - s * 0.1), (x + s, y - s * 0.5)]
    d.polygon(pts, fill=col)

def ship(d, x, y, s):
    d.polygon([(x - s, y), (x + s, y), (x + s * 0.75, y + s * 0.35), (x - s * 0.7, y + s * 0.35)], fill=(60, 30, 14, 255))
    d.line([(x, y), (x, y - s * 1.3)], fill=(40, 20, 8, 255), width=max(2, int(s / 18)))
    d.polygon([(x + 4, y - s * 1.2), (x + s * 0.6, y - s * 0.75), (x + 4, y - s * 0.3)], fill=(240, 236, 220, 255))
    d.polygon([(x - 4, y - s * 1.1), (x - s * 0.55, y - s * 0.7), (x - 4, y - s * 0.35)], fill=(240, 236, 220, 255))
    d.rectangle([x - 3, y - s * 1.45, x + s * 0.3, y - s * 1.3], fill=(20, 20, 20, 255))

def car(d, x, y, s, col):
    d.rounded_rectangle([x - s, y - s * 0.25, x + s, y + s * 0.15], radius=s * 0.15, fill=col)
    d.polygon([(x - s * 0.4, y - s * 0.25), (x - s * 0.1, y - s * 0.5), (x + s * 0.35, y - s * 0.5), (x + s * 0.5, y - s * 0.25)], fill=col)
    d.polygon([(x - s * 0.3, y - s * 0.27), (x - s * 0.05, y - s * 0.45), (x + s * 0.3, y - s * 0.45), (x + s * 0.4, y - s * 0.27)], fill=(140, 200, 255, 255))
    for wx in (x - s * 0.6, x + s * 0.6):
        d.ellipse([wx - s * 0.2, y - s * 0.05, wx + s * 0.2, y + s * 0.35], fill=(20, 20, 20, 255)); d.ellipse([wx - s * 0.09, y + s * 0.06, wx + s * 0.09, y + s * 0.24], fill=(200, 200, 210, 255))

def checker(d, x0, y0, x1, y1, n):
    cw = (x1 - x0) / n
    for i in range(n):
        for j in range(int((y1 - y0) / cw) + 1):
            if (i + j) % 2 == 0: d.rectangle([x0 + i * cw, y0 + j * cw, x0 + (i + 1) * cw, min(y1, y0 + (j + 1) * cw)], fill=(240, 240, 240, 255))
            else: d.rectangle([x0 + i * cw, y0 + j * cw, x0 + (i + 1) * cw, min(y1, y0 + (j + 1) * cw)], fill=(20, 20, 20, 255))

def scene(theme, w, h):
    """Theme illustration used on the playfield, backglass and cabinet sides."""
    if theme == 'classic':
        img = grad(w, h, (10, 20, 70), (25, 60, 160)); stars(img, 80)
        d = ImageDraw.Draw(img)
        for k, col in enumerate([(235, 40, 40), (245, 245, 255), (60, 140, 255)]):
            d.arc([-w * 0.4, h * (0.5 + 0.06 * k), w * 1.4, h * (1.5 + 0.06 * k)], 190, 350, fill=col + (255,), width=max(4, w // 60))
    elif theme == 'galaxy':
        img = grad(w, h, (4, 6, 24), (20, 16, 70)); stars(img, int(w * h / 1200))
        planet(img, w * 0.72, h * 0.28, w * 0.18, (255, 140, 60), (120, 30, 10), ring=True)
        planet(img, w * 0.22, h * 0.62, w * 0.09, (120, 200, 255), (20, 40, 120))
    elif theme == 'haunted':
        img = grad(w, h, (12, 6, 30), (40, 70, 60)); stars(img, int(w * h / 3000), 0.5)
        moon(img, w * 0.7, h * 0.22, w * 0.11)
        d = ImageDraw.Draw(img)
        d.polygon([(0, h), (0, h * 0.75), (w * 0.15, h * 0.72), (w * 0.2, h * 0.55), (w * 0.27, h * 0.72), (w * 0.45, h * 0.7), (w * 0.5, h * 0.48), (w * 0.56, h * 0.7), (w, h * 0.78), (w, h)], fill=(14, 10, 20, 255))
        for _ in range(6): bat(d, random.random() * w, random.random() * h * 0.45, w * 0.04)
    elif theme == 'pirate':
        img = grad(w, h, (255, 170, 80), (40, 90, 150))
        d = ImageDraw.Draw(img)
        d.ellipse([w * 0.35, h * 0.30, w * 0.65, h * 0.60], fill=(255, 220, 120, 255))
        d.rectangle([0, h * 0.55, w, h], fill=(20, 70, 120, 255))
        for k in range(30):
            y = h * (0.57 + 0.015 * k); d.line([(random.random() * w * 0.3, y), (w * (0.7 + random.random() * 0.3), y)], fill=(60, 130, 190, 255), width=2)
        ship(d, w * 0.5, h * 0.56, w * 0.22)
    else:  # speedway
        img = grad(w, h, (60, 120, 220), (180, 220, 255))
        d = ImageDraw.Draw(img)
        d.rectangle([0, h * 0.55, w, h], fill=(50, 50, 56, 255))
        for k in range(8): d.rectangle([w * k / 8 + 10, h * 0.76, w * k / 8 + w / 16, h * 0.78], fill=(240, 240, 240, 255))
        checker(d, 0, h * 0.55, w * 0.12, h, 4)
        car(d, w * 0.36, h * 0.70, w * 0.16, (220, 30, 30, 255)); car(d, w * 0.72, h * 0.86, w * 0.14, (40, 100, 230, 255))
    return img

TITLES = {'classic': "DUSTIN'S CLASSIC", 'galaxy': 'GALAXY RUN', 'haunted': 'HAUNTED HOLLOW', 'pirate': "PIRATE'S GOLD", 'speedway': 'TURBO SPEEDWAY'}
GLOW = {'classic': (230, 30, 30), 'galaxy': (60, 140, 255), 'haunted': (120, 255, 120), 'pirate': (255, 190, 40), 'speedway': (230, 30, 30)}

def title(img, text, y, frac, glow):
    d = ImageDraw.Draw(img); w, h = img.size
    sz = int(h * frac)
    while d.textlength(text, font=tex.font(sz)) > w * 0.9: sz -= 3
    tex.glow_text(img, (w / 2, y), text, tex.font(sz), (255, 255, 255, 255), glow + (255,), blur=max(4, sz // 8), stroke=3, stroke_fill=tuple(int(c * 0.5) for c in glow) + (255,))

for k, im in {'black': tex.black_gloss(), 'chrome': tex.chrome(), 'red': tex.solid((210, 24, 24)), 'badge': tex.badge(),
              'navy': tex.solid((14, 26, 70))}.items():
    save(k, im)

for th in TITLES:
    # playfield: portrait, top of the image = far end of the table, painted lanes and inserts
    pf = scene(th, 512, 1024).transpose(Image.FLIP_TOP_BOTTOM) if False else scene(th, 512, 1024)
    d = ImageDraw.Draw(pf)
    dim = Image.new("RGBA", pf.size, (0, 0, 0, 70)); pf.alpha_composite(dim)
    ROLLS = {'classic': [3.6, 6.2, 8.8], 'galaxy': [6.2, 8.8], 'haunted': [3.6, 6.2, 8.8], 'pirate': [2.6, 5.0, 7.4, 9.8], 'speedway': [3.6, 6.2, 8.8]}
    PX = lambda u: 512 * (1 - u / 14); PY = lambda v: 1024 * (1 - v / 28)
    for u in ROLLS[th]:                                                  # top lanes for this table
        d.rounded_rectangle([PX(u) - 26, PY(26) - 50, PX(u) + 26, PY(26) + 50], radius=20, outline=(255, 255, 255, 200), width=4)
    ORBITS = {'galaxy': [(2.7, 12.5, 2.7, 21.5)], 'speedway': [(2.6, 12.5, 2.6, 21.0), (9.8, 12.5, 9.8, 21.0)]}
    for (u0, v0, u1, v1) in ORBITS.get(th, []):                          # painted arrows up the orbit lanes
        cx = PX(u0 - 1.2 if u0 < 7 else u0 + 1.2)
        for k in range(4):
            y = PY(v0 + 1.5 + k * 2); d.polygon([(cx - 22, y + 14), (cx, y - 14), (cx + 22, y + 14)], fill=GLOW[th] + (200,))
    for k in range(5):
        y = 760 + k * 24; d.line([(120, y), (392, y)], fill=GLOW[th] + (90,), width=3)
    title(pf, TITLES[th], 600, 0.06, GLOW[th])
    tex.gloss(pf, 35); save(f'pf_{th}', pf)
    # backglass art (top) and cabinet side art
    bg = scene(th, 1024, 512); title(bg, TITLES[th], 512 * 0.62, 0.22, GLOW[th]); tex.gloss(bg, 45); save(f'pbg_{th}', bg)
    sd = scene(th, 1024, 384); title(sd, TITLES[th], 384 * 0.5, 0.28, GLOW[th]); save(f'pside_{th}', sd)
    ap = tex.marquee(TITLES[th], 1024, 200); save(f'pap_{th}', ap)

    m = Mesh()
    for (x, z) in ((-7, -7.2), (7, -7.2), (-7, 23), (7, 23)):                          # chrome legs with feet
        cylinder_y(m, 'static', 'chrome', x * U, z * U, 0.0, 10.4 * U, 0.05, seg=12, r_top=0.04)
        cylinder_y(m, 'static', 'black', x * U, z * U, 0.0, 0.02, 0.065, seg=12)
    rounded_box(m, 'static', 'black', -8 * U, 10 * U, -8 * U, 8 * U, 12.6 * U, 24 * U, r=0.03, front_mat=f'pap_{th}', side_mat=f'pside_{th}')   # tub with side art
    for s in (-1, 1):
        rounded_box(m, 'static', 'chrome', s * 7.6 * U - 0.02, 13.0 * U, -8 * U, s * 7.6 * U + 0.02, 16.0 * U, 23 * U, r=0.012)              # side rails
    rounded_box(m, 'static', 'chrome', -8 * U, 13.0 * U, -8.3 * U, 8 * U, 13.8 * U, -6.4 * U, r=0.02)                                      # lockdown bar
    rounded_box(m, 'static', 'black', -8 * U, 12.6 * U, -6.5 * U, 8 * U, 13.2 * U, -6.0 * U, r=0.01)                                       # apron
    # tilted playfield with art: u 0..14 -> x -7..7, v 0..28 -> z -6..22, y = 12.85 + 0.09v (px)
    P = lambda u, v: ((u - 7) * U, (12.85 + v * 0.09) * U - 0.004, (v - 6) * U)
    m.add('playfield', f'pf_{th}', quad(P(14, 0), P(0, 0), P(0, 28), P(14, 28), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 1.0, 0.0)))
    # backbox: score window below, lit art panel above
    rounded_box(m, 'static', 'black', -8 * U, 12.6 * U, 22 * U, 8 * U, 39 * U, 26 * U, r=0.04, side_mat=f'pside_{th}')
    m.add('static', 'black', quad((7.4 * U, 19.4 * U, 21.97 * U), (-7.4 * U, 19.4 * U, 21.97 * U), (-7.4 * U, 30.4 * U, 21.97 * U), (7.4 * U, 30.4 * U, 21.97 * U), n=(0.0, 0.0, -1.0)))
    rounded_box(m, 'static', 'chrome', -7.6 * U, 30.4 * U, 21.8 * U, 7.6 * U, 30.8 * U, 22.1 * U, r=0.005)
    rounded_box(m, 'static', 'chrome', -7.6 * U, 19.0 * U, 21.8 * U, 7.6 * U, 19.4 * U, 22.1 * U, r=0.005)
    m.add('backglass', f'pbg_{th}', quad((7.5 * U, 30.9 * U, 21.97 * U), (-7.5 * U, 30.9 * U, 21.97 * U), (-7.5 * U, 38.5 * U, 21.97 * U), (7.5 * U, 38.5 * U, 21.97 * U), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, -1.0)))
    rounded_box(m, 'static', 'chrome', 4.0 * U, 13.0 * U, -8.4 * U, 6.4 * U, 14.0 * U, -7.9 * U, r=0.01)                                    # plunger housing
    for s in (-1, 1):
        cylinder_y(m, 'static', 'red', 0, 0, 0, 0, 0)  if False else None
        rounded_box(m, 'static', 'red', s * 8.0 * U - 0.008, 13.0 * U, -5.0 * U, s * 8.0 * U + 0.008, 14.2 * U, -3.6 * U, r=0.004)   # flipper buttons
    m.write(f'{OUT}/models/pinball_{th}.obj')
print('ok')
