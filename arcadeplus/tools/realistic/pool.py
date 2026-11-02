"""Dustin's Arcade pool table (2 wide x 3 long). Model frame: front -z, x from -0.45 to 1.45 (the second cell is +x),
z from -0.45 to 2.45. Playfield (inside the cushions) is u -0.82..0.82, v -1.32..1.32 around the centre (0.5, 1.0)."""
import sys, math
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw
OUT = '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
CX, CZ, TOP = 0.5, 1.0, 0.80
U, V, R = 0.82, 1.32, 0.045

COLORS = {1: (240, 200, 20), 2: (30, 60, 200), 3: (220, 30, 30), 4: (110, 40, 160), 5: (240, 120, 20), 6: (20, 130, 60), 7: (130, 30, 30), 8: (16, 16, 18)}
def ball_tex(n, w=256, h=128):
    if n == 0:
        img = Image.new("RGBA", (w, h), (246, 244, 236, 255)); d = ImageDraw.Draw(img)
        d.ellipse([w * 0.22, h * 0.42, w * 0.28, h * 0.58], fill=(220, 30, 30, 255)); return img
    base = COLORS[n if n <= 8 else n - 8]
    img = Image.new("RGBA", (w, h), (246, 244, 236, 255) if n > 8 else base + (255,)); d = ImageDraw.Draw(img)
    if n > 8: d.rectangle([0, h * 0.28, w, h * 0.72], fill=base + (255,))
    for cx in (w * 0.25, w * 0.75):
        d.ellipse([cx - 18, h / 2 - 18, cx + 18, h / 2 + 18], fill=(250, 250, 245, 255))
        d.text((cx, h / 2), str(n), font=tex.font(24), fill=(10, 10, 10, 255), anchor="mm")
    return img
for n in range(16): save(f'pool_{n}', ball_tex(n))
felt = tex.solid((20, 70, 170), noise=6); d = ImageDraw.Draw(felt)
save('pool_felt', felt)
def felt_logo(w=512, h=820):
    img = tex.solid((20, 70, 170), w, h, noise=6)
    tex.glow_text(img, (w / 2, h / 2), "DUSTIN'S", tex.font(64), (60, 110, 210, 255), (20, 70, 170, 255), blur=2)
    d = ImageDraw.Draw(img); d.line([(0, h * 0.25), (w, h * 0.25)], fill=(60, 110, 210, 255), width=3)
    d.ellipse([w / 2 - 5, h * 0.75 - 5, w / 2 + 5, h * 0.75 + 5], fill=(200, 210, 230, 255))
    return img
save('pool_bed', felt_logo())
save('pool_wood', tex.solid((70, 30, 24), noise=10)); save('pool_rail', tex.solid((180, 20, 24), noise=6))
save('pool_cue', tex.solid((200, 160, 100), noise=8))
m = Mesh()
X0, X1, Z0, Z1 = CX - U - 0.13, CX + U + 0.13, CZ - V - 0.13, CZ + V + 0.13
for x in (X0 + 0.12, X1 - 0.12):                                                  # legs
    for z in (Z0 + 0.15, CZ, Z1 - 0.15):
        rounded_box(m, 'static', 'pool_wood', x - 0.07, 0.0, z - 0.07, x + 0.07, 0.6, z + 0.07, r=0.03)
rounded_box(m, 'static', 'pool_wood', X0, 0.55, Z0, X1, TOP - 0.02, Z1, r=0.06, side_mat='pool_wood')       # body
q = lambda a, b, c, d_, uv=((0, 0), (1, 0), (1, 1), (0, 1)): m.add('static', 'pool_bed', quad(a, b, c, d_, uv=uv, n=(0.0, 1.0, 0.0)))
q((CX - U, TOP, CZ - V), (CX - U, TOP, CZ + V), (CX + U, TOP, CZ + V), (CX + U, TOP, CZ - V), uv=((1, 1), (1, 0), (0, 0), (0, 1)))
H = 0.05                                                                           # cushions and rails
for s in (-1, 1):
    rounded_box(m, 'static', 'pool_felt', CX + s * U - (0.035 if s > 0 else 0) + (0 if s > 0 else 0), TOP - 0.02, CZ - V + 0.09, CX + s * U + (0.035 if s < 0 else 0) - (0 if s < 0 else 0) + (0.0 if s > 0 else 0), TOP + H, CZ - 0.09, r=0.01) if False else None
def cushion(x0, x1, z0, z1): rounded_box(m, 'static', 'pool_felt', x0, TOP - 0.02, z0, x1, TOP + H, z1, r=0.012)
cushion(CX - U - 0.04, CX - U, CZ - V + 0.1, CZ - 0.09); cushion(CX - U - 0.04, CX - U, CZ + 0.09, CZ + V - 0.1)
cushion(CX + U, CX + U + 0.04, CZ - V + 0.1, CZ - 0.09); cushion(CX + U, CX + U + 0.04, CZ + 0.09, CZ + V - 0.1)
cushion(CX - U + 0.1, CX + U - 0.1, CZ - V - 0.04, CZ - V); cushion(CX - U + 0.1, CX + U - 0.1, CZ + V, CZ + V + 0.04)
for (x0, x1, z0, z1) in ((X0, CX - U - 0.04, Z0, Z1), (CX + U + 0.04, X1, Z0, Z1), (X0, X1, Z0, CZ - V - 0.04), (X0, X1, CZ + V + 0.04, Z1)):
    rounded_box(m, 'static', 'pool_rail', x0, TOP - 0.02, z0, x1, TOP + 0.075, z1, r=0.02)
for (pu, pv) in ((-1, -1), (1, -1), (-1, 1), (1, 1), (-1.02, 0), (1.02, 0)):        # pockets
    x, z = CX + pu * U, CZ + pv * V
    cylinder_y(m, 'static', 'black', x, z, TOP + 0.075, TOP + 0.08, 0.085, seg=20)
    torus_y(m, 'static', 'chrome', x, TOP + 0.08, z, 0.085, 0.012, seg=20, tseg=6)
for k in range(1, 4):                                                              # sight diamonds
    for s in (-1, 1):
        sphere(m, 'static', 'chrome', CX + s * (U + 0.09), TOP + 0.078, CZ - V + k * V / 2, 0.01, la=4, lo=6, squash=0.4)
for n in range(16): sphere(m, f'ball_{n}', f'pool_{n}', 0, 0, 0, R, la=8, lo=14)
cylinder_z(m, 'cue', 'pool_cue', 0, 0, -1.45, -0.07, 0.012, seg=10, cap_back=True)
cylinder_z(m, 'cue', 'black', 0, 0, -1.45, -1.0, 0.016, seg=10)
cylinder_z(m, 'cue', 'cyan', 0, 0, -0.09, -0.07, 0.011, seg=10)
m.write(f'{OUT}/models/pool.obj')
print('ok')
