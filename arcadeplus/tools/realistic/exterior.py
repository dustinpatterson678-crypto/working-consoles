"""Dustin's Arcade storefront, futuristic edition. Model frame: X = -lx (so -X is your right as you walk in),
Y = ly (floor slab top = 1), Z = lz (the front skin is Z -1.5..-0.5; the door is X -1.5..1.5)."""
import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw, ImageFilter
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
random.seed(5)
HX, BACK, TOP = 12.5, 41.5, 10.0
ZF = -1.5                                                     # front face
TX, TZ, TR = 11.5, -1.6, 2.2                                  # the glass beacon tower (old corner tower spot)

def panel(w=512, h=512, base=(54, 58, 72)):
    img = Image.new("RGBA", (w, h), base + (255,)); d = ImageDraw.Draw(img)
    for k in range(0, h, h // 4): d.line([(0, k), (w, k)], fill=(30, 32, 42, 255), width=6)
    for k in range(0, w, w // 2): d.line([(k, 0), (k, h)], fill=(30, 32, 42, 255), width=6)
    tex.gloss(img, 30); return img
def glass(w=1024, h=1024):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=(int(20 + 30 * t), int(40 + 50 * t), int(80 + 70 * t), 255))
    ov = Image.new("RGBA", (w, h), (0, 0, 0, 0)); od = ImageDraw.Draw(ov)
    for k in range(-h, w, 260): od.polygon([(k, h), (k + 120, h), (k + 120 + h * 0.6, 0), (k + h * 0.6, 0)], fill=(200, 220, 255, 30))
    for i in range(30):                                       # the arcade glowing faintly through the glass
        x, y = random.random() * w, h * random.uniform(0.6, 0.95)
        od.ellipse([x - 30, y - 18, x + 30, y + 18], fill=random.choice([(255, 60, 60, 60), (60, 140, 255, 60), (255, 220, 80, 50)]))
    img.alpha_composite(ov.filter(ImageFilter.GaussianBlur(6)))
    for k in range(0, w + 1, w // 4): d.line([(k, 0), (k, h)], fill=(160, 170, 190, 255), width=8)
    for k in range(0, h + 1, h // 3): d.line([(0, k), (w, k)], fill=(160, 170, 190, 255), width=8)
    return img
def paving(n=512):
    img = Image.new("RGBA", (n, n), (70, 72, 80, 255)); d = ImageDraw.Draw(img)
    for k in range(0, n + 1, n // 4):
        d.line([(k, 0), (k, n)], fill=(52, 54, 62, 255), width=4); d.line([(0, k), (n, k)], fill=(52, 54, 62, 255), width=4)
    noise = Image.effect_noise((n, n), 14).convert("L")
    return Image.composite(img, Image.new("RGBA", (n, n), (64, 66, 74, 255)), noise.point(lambda v: 255 if v > 40 else 210))
def sign(w=2048, h=320):
    img = Image.new("RGBA", (w, h), (6, 8, 18, 255))
    tex.glow_text(img, (w / 2, h / 2), "DUSTIN'S ARCADE", tex.font(int(h * 0.62)), (255, 255, 255, 255), (230, 30, 30, 255), blur=14, stroke=4, stroke_fill=(150, 10, 10, 255))
    d = ImageDraw.Draw(img); d.rectangle([0, h - 12, w, h], fill=(60, 140, 255, 255)); d.rectangle([0, 0, w, 10], fill=(60, 140, 255, 255))
    return img
def door(w=512, h=768):
    img = Image.new("RGBA", (w, h), (20, 40, 80, 255)); d = ImageDraw.Draw(img)
    for k in range(-h, w, 120): d.line([(k, h), (k + h * 0.5, 0)], fill=(110, 160, 240, 255), width=8)
    d.rectangle([0, 0, w - 1, h - 1], outline=(220, 224, 232, 255), width=22); d.line([(w / 2, 0), (w / 2, h)], fill=(220, 224, 232, 255), width=16)
    tex.glow_text(img, (w / 2, h * 0.18), "ENTER", tex.font(70), (255, 255, 255, 255), (60, 140, 255, 255), blur=6)
    return img
for k, im in {'x_panel': panel(), 'x_white': tex.solid((236, 238, 244), noise=3), 'x_glass': glass(), 'x_paving': paving(), 'x_sign': sign(),
              'x_door': door(), 'x_dark': tex.solid((16, 18, 26), noise=3), 'x_red': tex.solid((255, 50, 60), noise=2),
              'x_blue': tex.solid((60, 150, 255), noise=2), 'x_wglow': tex.solid((235, 240, 255), noise=2), 'x_tower': glass(512, 1024)}.items():
    save(k, im)

m = Mesh()
def q(g, mat, a, b, c, d_, uv=((0, 0), (1, 0), (1, 1), (0, 1)), n=None): m.add(g, mat, quad(a, b, c, d_, uv=uv, n=n))

# ---- body: side and back walls (gunmetal panels), roof ----
def wall_x(mat, z, xa, xb, y0, y1, nz, step=2.0):
    x = xa
    while x < xb - 1e-6:
        x2 = min(xb, x + step); y = y0
        while y < y1 - 1e-6:
            y2 = min(y1, y + 2.0)
            if nz > 0: q('static', mat, (x, y, z), (x2, y, z), (x2, y2, z), (x, y2, z), n=(0.0, 0.0, 1.0))
            else: q('static', mat, (x2, y, z), (x, y, z), (x, y2, z), (x2, y2, z), n=(0.0, 0.0, -1.0))
            y = y2
        x = x2
def wall_z(mat, x, za, zb, y0, y1, nx, step=2.0):
    z = za
    while z < zb - 1e-6:
        z2 = min(zb, z + step); y = y0
        while y < y1 - 1e-6:
            y2 = min(y1, y + 2.0)
            if nx > 0: q('static', mat, (x, y, z2), (x, y, z), (x, y2, z), (x, y2, z2), n=(1.0, 0.0, 0.0))
            else: q('static', mat, (x, y, z), (x, y, z2), (x, y2, z2), (x, y2, z), n=(-1.0, 0.0, 0.0))
            y = y2
        z = z2
wall_z('x_panel', HX, ZF + 0.6, BACK, 0.0, TOP, 1); wall_z('x_panel', -HX, ZF + 0.6, BACK, 0.0, TOP, -1)
wall_x('x_panel', BACK, -HX, HX, 0.0, TOP, 1)
for z in range(0, int(BACK), 4):
    q('static', 'x_dark', (-HX, TOP, z), (HX, TOP, z), (HX, TOP, min(BACK, z + 4)), (-HX, TOP, min(BACK, z + 4)), n=(0.0, 1.0, 0.0))
q('static', 'x_dark', (-HX, TOP, ZF), (HX, TOP, ZF), (HX, TOP, 0), (-HX, TOP, 0), n=(0.0, 1.0, 0.0))
for y, mat in ((3.0, 'x_red'), (8.6, 'x_blue')):                     # light lines wrapping the body
    for s in (-1, 1):
        rounded_box(m, 'glow', mat, min(s * HX, s * (HX + 0.05)), y, ZF + 0.6, max(s * HX, s * (HX + 0.05)), y + 0.1, BACK, r=0.005)
    rounded_box(m, 'glow', mat, -HX, y, BACK, HX, y + 0.1, BACK + 0.05, r=0.005)
for s in (-1, 1): rounded_box(m, 'glow', 'x_blue', min(s * HX, s * (HX + 0.06)), TOP - 0.12, ZF, max(s * HX, s * (HX + 0.06)), TOP, BACK, r=0.005)
# ---- front: glass curtain wall in a white portal frame ----
for x0, x1 in ((-9.5, -1.6), (1.6, 9.0)):
    q('static', 'x_glass', (x1, 1.0, ZF), (x0, 1.0, ZF), (x0, 8.6, ZF), (x1, 8.6, ZF), uv=((0, 1), ((x1 - x0) / 8, 1), ((x1 - x0) / 8, 0), (0, 0)), n=(0.0, 0.0, -1.0))
q('static', 'x_glass', (1.6, 4.6, ZF), (-1.6, 4.6, ZF), (-1.6, 8.6, ZF), (1.6, 8.6, ZF), uv=((0, 1), (0.4, 1), (0.4, 0), (0, 0)), n=(0.0, 0.0, -1.0))
q('static', 'x_door', (1.5, 1.0, ZF + 1.0), (-1.5, 1.0, ZF + 1.0), (-1.5, 4.5, ZF + 1.0), (1.5, 4.5, ZF + 1.0), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, -1.0))
for s in (-1, 1): q('static', 'x_white', (s * 1.6, 1.0, ZF), (s * 1.6, 1.0, ZF + 1.0), (s * 1.6, 4.6, ZF + 1.0), (s * 1.6, 4.6, ZF), n=(-s * 1.0, 0.0, 0.0))
q('static', 'x_white', (-1.6, 4.6, ZF), (1.6, 4.6, ZF), (1.6, 4.6, ZF + 1.0), (-1.6, 4.6, ZF + 1.0), n=(0.0, -1.0, 0.0))
rounded_box(m, 'static', 'x_white', -HX - 0.3, 8.6, ZF - 0.6, HX + 0.3, TOP + 0.4, ZF + 0.2, r=0.25)             # header
rounded_box(m, 'static', 'x_white', -HX - 0.3, 0.0, ZF - 0.6, -9.5, TOP + 0.4, ZF + 0.2, r=0.25)                 # left pier (your right)
rounded_box(m, 'static', 'x_white', 9.0, 0.0, ZF - 0.6, HX + 0.3, 8.6, ZF + 0.2, r=0.25)                        # pier by the tower
rounded_box(m, 'static', 'x_dark', -9.5, 0.6, ZF - 0.3, 9.0, 1.0, ZF + 0.1, r=0.05)                             # plinth
q('glow', 'x_sign', (8.6, 8.85, ZF - 0.62), (-10.4, 8.85, ZF - 0.62), (-10.4, 10.15, ZF - 0.62), (8.6, 10.15, ZF - 0.62), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, -1.0))
rounded_box(m, 'glow', 'x_blue', -HX - 0.3, TOP + 0.4, ZF - 0.6, HX + 0.3, TOP + 0.5, ZF + 0.2, r=0.02)
for i, x in enumerate((-7.5, -4.5, 4.5, 7.5)):                                                                   # LED fins on the glass
    rounded_box(m, 'static', 'x_white', x - 0.08, 1.0, ZF - 0.5, x + 0.08, 8.6, ZF, r=0.03)
    rounded_box(m, 'glow', 'x_red' if i % 2 else 'x_blue', x - 0.03, 1.2, ZF - 0.53, x + 0.03, 8.4, ZF - 0.49, r=0.01)
# ---- the entrance: floating canopy and a glowing portal arch ----
rounded_box(m, 'static', 'x_white', -5.5, 5.3, -6.0, 5.5, 5.55, ZF, r=0.1)
rounded_box(m, 'glow', 'x_wglow', -5.3, 5.25, -5.8, 5.3, 5.3, ZF - 0.2, r=0.01)
rounded_box(m, 'glow', 'x_red', -5.55, 5.32, -6.05, 5.55, 5.38, -5.95, r=0.01)
torus_z(m, 'glow', 'x_blue', 0, 1.0, ZF - 0.15, 2.4, 0.09, seg=48, tseg=8, a_from=-math.pi / 2 - 0.02, a_to=math.pi / 2 + 0.02)
torus_z(m, 'glow', 'x_red', 0, 1.0, ZF - 0.2, 2.75, 0.07, seg=48, tseg=8, a_from=-math.pi / 2 - 0.02, a_to=math.pi / 2 + 0.02)
# ---- the glass beacon tower ----
cylinder_y_wrap(m, 'static', 'x_tower', TX, TZ, 0.0, 14.0, TR, seg=40)
cylinder_y(m, 'static', 'x_white', TX, TZ, 14.0, 14.4, TR + 0.2, seg=40)
for y in range(2, 14, 2): torus_y(m, 'glow', 'x_red' if (y // 2) % 2 else 'x_blue', TX, y, TZ, TR + 0.03, 0.05, seg=48, tseg=6)
cylinder_y(m, 'static', 'x_white', TX, TZ, 14.4, 17.5, 0.12, seg=12, r_top=0.03)
torus_y(m, 'glow', 'x_wglow', TX, 15.4, TZ, 1.4, 0.08, seg=48, tseg=6)
sphere(m, 'glow', 'x_red', TX, 17.6, TZ, 0.15, la=6, lo=10)
# ---- the plaza out front ----
for x in range(-14, 14, 4):
    for z0 in (-5.5,):
        q('static', 'x_paving', (x, 1.0, z0), (x, 1.0, ZF), (x + 4, 1.0, ZF), (x + 4, 1.0, z0), n=(0.0, 1.0, 0.0))
for s in (-1, 1): q('glow', 'x_blue', (s * 1.4 - 0.05, 1.01, -5.5), (s * 1.4 - 0.05, 1.01, ZF), (s * 1.4 + 0.05, 1.01, ZF), (s * 1.4 + 0.05, 1.01, -5.5), n=(0.0, 1.0, 0.0))
m.write(f'{OUT}/models/dustin_exterior.obj')
print('ok')
