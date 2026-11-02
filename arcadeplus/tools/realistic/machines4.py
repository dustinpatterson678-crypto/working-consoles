import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
P = 1 / 16.0

def pedestal(m, front, y1=0.80, z0=-0.38, z1=0.30, x=0.45):
    rounded_box(m, 'static', 'black', -x - 0.02, 0.0, z0 - 0.02, x + 0.02, 0.05, z1 + 0.02, r=0.06)
    rounded_box(m, 'static', 'black', -x, 0.05, z0, x, y1, z1, r=0.05, front_mat=front, side_mat='side_art')
    rounded_box(m, 'static', 'chrome', -x - 0.01, y1, z0 - 0.01, x + 0.01, y1 + 0.03, z1 + 0.01, r=0.05)
    rounded_box(m, 'static', 'badge', -0.2, y1 - 0.18, z0 - 0.012, 0.2, y1 - 0.10, z0 - 0.001, r=0.005, front_mat='badge')
    rounded_box(m, 'static', 'chrome', -0.30, 0.10, z0 - 0.005, -0.18, 0.16, z0 + 0.02, r=0.01)

def gradient(w, h, top, bot):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bot)) + (255,))
    return img

# ---------------- Key Vault ----------------
save('kv_front', tex.neon_art(1024, 768, 'KEY VAULT', 'GET THE KEY IN THE LOCK'))
save('kv_marquee', tex.marquee('KEY VAULT'))
save('kv_back', gradient(256, 256, (10, 18, 60), (24, 50, 130)))
hole = tex.chrome(128, 128); hd = ImageDraw.Draw(hole)
hd.ellipse([44, 22, 84, 62], fill=(10, 10, 12, 255)); hd.polygon([(54, 52), (74, 52), (80, 108), (48, 108)], fill=(10, 10, 12, 255))
save('kv_hole', hole)
m = Mesh()
pedestal(m, 'kv_front')
for s_ in (-1, 1):
    rounded_box(m, 'static', 'black', s_ * 0.45 - 0.025, 0.83, -0.30, s_ * 0.45 + 0.025, 2.15, 0.32, r=0.02, side_mat='side_art')   # side panels
rounded_box(m, 'static', 'black', -0.47, 0.83, 0.24, 0.47, 2.15, 0.32, r=0.02)                                # back
rounded_box(m, 'static', 'black', -0.47, 2.10, -0.30, 0.47, 2.15, 0.32, r=0.02)                               # roof
rounded_box(m, 'static', 'kv_back', -0.43, 0.88, 0.20, 0.43, 2.10, 0.24, r=0.01, front_mat='kv_back')        # back wall
for s in (-1, 1):
    rounded_box(m, 'static', 'chrome', s * 0.45 - 0.02, 0.86, -0.32, s * 0.45 + 0.02, 2.13, -0.27, r=0.01)   # glass frame
    rounded_box(m, 'leds', 'cyan', s * 0.43 - 0.008, 0.90, -0.33, s * 0.43 + 0.008, 2.08, -0.322, r=0.004)
rounded_box(m, 'static', 'chrome', -0.47, 2.10, -0.32, 0.47, 2.15, -0.27, r=0.01)
COLS = [5.6, 2.8, 0.0, -2.8, -5.6]; ROWS = [29.0, 25.0, 21.0]
for r, y in enumerate(ROWS):
    rounded_box(m, 'static', 'chrome', -0.43, (y + 0.6) * P, -0.22, 0.43, (y + 0.8) * P, 0.20, r=0.004)      # prize shelf
    for c, x in enumerate(COLS):
        rounded_box(m, 'static', 'black', x * P - 0.06, y * P - 0.06, -0.255, x * P + 0.06, y * P + 0.06, -0.22, r=0.01, front_mat='kv_hole')
rounded_box(m, 'static', 'black', -0.43, 0.88, -0.25, 0.43, 1.10, 0.20, r=0.01)                              # key track base
cylinder_z(m, 'key', 'gold', 0, 0, -0.34, -0.27, 0.012, seg=12)                                               # the key: shaft
torus_z(m, 'key', 'gold', 0, 0, -0.355, 0.03, 0.010, seg=24, tseg=8)                                          # bow
rounded_box(m, 'key', 'gold', -0.004, -0.02, -0.29, 0.004, 0.0, -0.275, r=0.002)                             # bit
rounded_box(m, 'key', 'black', -0.04, -0.05, -0.40, 0.04, -0.03, -0.30, r=0.01)                               # carriage
rounded_box(m, 'marquee', 'black', -0.48, 2.15, -0.31, 0.48, 2.45, 0.33, r=0.04, front_mat='kv_marquee')
for bx, g in ((0.12, 'button'), (-0.12, 'button2')):
    cylinder_y(m, 'static', 'chrome', bx, -0.20, 0.83, 0.85, 0.07)
    sphere(m, g, 'red' if g == 'button' else 'blue_plastic', bx, 0.85, -0.20, 0.06, squash=0.45)
m.write(f'{OUT}/models/keyvault.obj')

# ---------------- Speed Lights ----------------
save('sl_front', tex.neon_art(1024, 768, 'SPEED LIGHTS', 'HIT THE LIGHT'))
save('sl_marquee', tex.marquee('SPEED LIGHTS'))
save('sl_deck', tex.black_gloss())
def board(w=1024, h=1024):
    img = gradient(w, h, (8, 16, 60), (20, 46, 130)); d = ImageDraw.Draw(img)
    for k in range(18):
        a = 2 * math.pi * k / 18
        d.line([(w / 2, h * 0.62), (w / 2 + math.cos(a) * w, h * 0.62 + math.sin(a) * w)], fill=(60, 120, 255, 40), width=18)
    d.rounded_rectangle([w * 0.2, h * 0.08, w * 0.8, h * 0.36], 30, fill=(4, 4, 8, 255), outline=(230, 30, 30, 255), width=8)
    tex.glow_text(img, (w / 2, h * 0.5), "SPEED LIGHTS", tex.font(110), (255, 255, 255, 255), (230, 30, 30, 255), blur=10, stroke=3, stroke_fill=(150, 10, 10, 255))
    tex.glow_text(img, (w / 2, h * 0.66), "30 SECONDS", tex.font(70), (255, 255, 255, 255), (60, 140, 255, 255), blur=6)
    tex.gloss(img, 30); return img
save('sl_board', board())
m = Mesh()
pedestal(m, 'sl_front', y1=0.84, z0=-0.45, z1=0.30)
rounded_box(m, 'static', 'black', -0.44, 0.87, -0.43, 0.44, 0.89, 0.10, r=0.03, top_mat='sl_deck')
LAMPS = ['lamp_red', 'lamp_blue', 'lamp_green', 'lamp_yellow', 'lamp_red', 'lamp_blue', 'lamp_green', 'lamp_yellow', 'lamp_red']
for i in range(9):
    x = [4.5, 0.0, -4.5][i % 3] * P; z = [-5.0, -1.6, 1.8][i // 3] * P
    cylinder_y(m, 'static', 'chrome', x, z, 0.89, 0.905, 0.075, seg=24)
    sphere(m, f'btn_{i}', LAMPS[i], x, 0.905, z, 0.062, la=6, lo=14, squash=0.5)
rounded_box(m, 'static', 'black', -0.46, 0.89, 0.12, 0.46, 2.05, 0.32, r=0.04, front_mat='sl_board')
for s in (-1, 1):
    rounded_box(m, 'leds', 'cyan', s * 0.44 - 0.008, 0.93, 0.11, s * 0.44 + 0.008, 2.01, 0.12, r=0.004)
rounded_box(m, 'marquee', 'black', -0.47, 2.05, 0.10, 0.47, 2.30, 0.33, r=0.04, front_mat='sl_marquee')
m.write(f'{OUT}/models/speedlights.obj')

# ---------------- Drop Zone ----------------
DZ = [1, 0, 2, 0, 1, 0, 5, 0, 1, 0, 2, 0, 1, 0, 25, 0]
S = 1024; disc = Image.new("RGBA", (S, S), (0, 0, 0, 255)); d = ImageDraw.Draw(disc)
c = S / 2; R = S / 2 - 2; w = 360 / len(DZ)
pal = [(212, 32, 32), (32, 96, 216), (236, 236, 242), (32, 96, 216)]
for k, v in enumerate(DZ):                         # sector k covers local angles k*w..(k+1)*w; u = cos, v = sin
    col = (255, 214, 64) if v >= 25 else (40, 40, 48) if v == 0 else pal[k % 4]
    d.pieslice([c - R, c - R, c + R, c + R], k * w, (k + 1) * w, fill=col + (255,))
for k, v in enumerate(DZ):
    a = math.radians((k + 0.5) * w)
    hx, hy = c + math.cos(a) * R * 0.75, c + math.sin(a) * R * 0.75
    if v > 0:
        d.ellipse([hx - 44, hy - 44, hx + 44, hy + 44], fill=(8, 8, 10, 255), outline=(200, 200, 210, 255), width=6)
        tx, ty = c + math.cos(a) * R * 0.45, c + math.sin(a) * R * 0.45
        lay = Image.new("RGBA", (200, 120), (0, 0, 0, 0)); ld = ImageDraw.Draw(lay)
        ld.text((100, 60), str(v), font=tex.font(70), fill=(255, 255, 255, 255) if v != 25 else (30, 20, 10, 255), anchor="mm", stroke_width=3, stroke_fill=(0, 0, 0, 160))
        lay = lay.rotate(-math.degrees(a) - 90, expand=True)
        disc.alpha_composite(lay, (int(tx - lay.width / 2), int(ty - lay.height / 2)))
d.ellipse([c - R * 0.15, c - R * 0.15, c + R * 0.15, c + R * 0.15], fill=(20, 18, 28, 255), outline=(216, 168, 48, 255), width=10)
tex.gloss(disc, 40); save('dz_disc', disc)
save('dz_front', tex.neon_art(1024, 768, 'DROP ZONE', 'DROP IT IN THE 25'))
save('dz_marquee', tex.marquee('DROP ZONE'))
m = Mesh()
pedestal(m, 'dz_front')
cylinder_y(m, 'static', 'black', 0, -0.04, 0.83, 0.86, 0.44, seg=48)
cylinder_y(m, 'disc', 'black', 0, 0, 0.86, 0.885, 0.40, seg=64, top_mat='dz_disc')                        # turntable (spins about its centre)
torus_y(m, 'static', 'chrome', 0, 0.90, 0, 0.42, 0.02)                                                       # rim
cylinder_y(m, 'static', 'chrome', 0, -0.30, 1.12, 1.50, 0.045, seg=20)                                      # drop tube over the front
cylinder_y(m, 'static', 'chrome', 0, -0.30, 1.50, 1.56, 0.07, seg=20)
rounded_box(m, 'static', 'chrome', -0.02, 1.40, -0.30, 0.02, 1.44, 0.30, r=0.008)                            # tube arm
rounded_box(m, 'static', 'black', -0.08, 0.86, 0.30, 0.08, 1.80, 0.40, r=0.02)                               # back post
rounded_box(m, 'marquee', 'black', -0.46, 1.80, 0.22, 0.46, 2.08, 0.42, r=0.04, front_mat='dz_marquee')
sphere(m, 'ball', 'chrome', 0, 0, 0, 0.035, la=8, lo=12)
cylinder_y(m, 'static', 'chrome', 0.32, -0.40, 0.83, 0.85, 0.06)
sphere(m, 'button', 'red', 0.32, 0.85, -0.40, 0.05, squash=0.45)
m.write(f'{OUT}/models/dropzone.obj')
print('ok')
