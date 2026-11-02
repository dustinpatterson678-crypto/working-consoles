"""Dustin's Arcade video cabinets (original designs). Model frame: front (player) is -z, -x is the player's right,
1 block = 1.0. The game picture is drawn live by the cabinet renderer on a vertical plane at z = -front/16,
centred at y = sy/16, so each bezel opening sits just behind that plane."""
import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw, ImageFilter
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
P = 1 / 16.0
random.seed(11)

def art(title, sub, w=1024, h=1024, top=(10, 18, 70), bot=(30, 60, 160), motif=None):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bot)) + (255,))
    if motif: motif(img, w, h)
    if not title:
        tex.gloss(img, 35); return img
    sz = int(h * 0.16)
    while sz > 10 and d.textlength(title, font=tex.font(sz)) > w * 0.88: sz -= 4
    tex.glow_text(img, (w / 2, h * 0.2), title, tex.font(sz), (255, 255, 255, 255), (230, 30, 30, 255), blur=10, stroke=3, stroke_fill=(150, 10, 10, 255))
    if sub: tex.glow_text(img, (w / 2, h * 0.88), sub, tex.font(int(h * 0.06), tex.COND), (255, 255, 255, 255), (60, 140, 255, 255), blur=6)
    tex.gloss(img, 35); return img

def stars(img, w, h):
    d = ImageDraw.Draw(img)
    for _ in range(140):
        x, y, r = random.random() * w, random.random() * h, random.random() * 2.5 + 0.5
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 200))
def grid(img, w, h):
    ov = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(ov)
    hz = h * 0.55
    for k in range(-20, 21): d.line([(w / 2 + k * 20, hz), (w / 2 + k * w / 6, h)], fill=(255, 60, 200, 160), width=3)
    for i in range(12):
        y = hz + (h - hz) * (i / 12) ** 1.8; d.line([(0, y), (w, y)], fill=(255, 60, 200, 160), width=3)
    d.ellipse([w / 2 - w * 0.22, hz - w * 0.22, w / 2 + w * 0.22, hz + w * 0.22], fill=(255, 120, 40, 200))
    img.alpha_composite(ov)
def spooky(img, w, h):
    d = ImageDraw.Draw(img)
    d.ellipse([w * 0.62, h * 0.3, w * 0.9, h * 0.58], fill=(235, 235, 210, 255))
    for i in range(7):
        x = w * (0.08 + 0.13 * i); hh = h * random.uniform(0.18, 0.3)
        d.polygon([(x - 40, h * 0.86), (x, h * 0.86 - hh), (x + 40, h * 0.86)], fill=(8, 6, 14, 255))
    for i in range(5):
        x, y = random.uniform(0.1, 0.9) * w, random.uniform(0.62, 0.8) * h
        d.ellipse([x - 9, y - 5, x - 1, y + 3], fill=(255, 30, 30, 255)); d.ellipse([x + 3, y - 5, x + 11, y + 3], fill=(255, 30, 30, 255))
    fog = Image.new("RGBA", (w, h), (0, 0, 0, 0)); fd = ImageDraw.Draw(fog)
    for i in range(8): a_ = random.uniform(-0.2, 0.4) * w; fd.ellipse([a_, h * 0.75, a_ + random.uniform(0.4, 0.8) * w, h * 1.05], fill=(140, 150, 190, 40))
    img.alpha_composite(fog.filter(ImageFilter.GaussianBlur(20)))
def speed(img, w, h):
    d = ImageDraw.Draw(img)
    for i in range(40):
        y = random.uniform(0.3, 0.95) * h; x = random.uniform(-0.2, 0.8) * w
        d.line([(x, y), (x + random.uniform(0.2, 0.6) * w, y)], fill=(255, 255, 255, random.randint(60, 200)), width=random.randint(2, 6))

for k, im in {'bezel': tex.black_gloss(), 'screen_off': tex.solid((6, 8, 10), noise=2), 'panel_white': tex.solid((236, 238, 244), noise=3),
              'red': tex.solid((210, 24, 24)), 'blue_plastic': tex.solid((40, 90, 230), noise=3), 'rubber': tex.solid((30, 30, 34)),
              'speaker': tex.solid((24, 24, 28), noise=12)}.items():
    save(k, im)

def stick(m, x, y, z, ball='red'):
    cylinder_y(m, 'static', 'black', x, z, y, y + 0.012, 0.03, seg=12)
    cylinder_y(m, 'static', 'chrome', x, z, y + 0.012, y + 0.075, 0.006, seg=8)
    sphere(m, 'static', ball, x, y + 0.09, z, 0.022, la=6, lo=10)
def buttons(m, x0, y, z, n=3, step=0.045, cols=('red', 'blue_plastic', 'red')):
    for i in range(n):
        cylinder_y(m, 'static', cols[i % len(cols)], x0 - i * step, z, y, y + 0.012, 0.016, seg=12)

def screen_hole(m, sy, front, sw, sh, depth=0.04):
    """Black recessed screen behind the live picture plane."""
    y, z, w, h = sy * P, -front * P, sw * P, sh * P
    rounded_box(m, 'static', 'screen_off', -w / 2, y - h / 2, z + 0.004, w / 2, y + h / 2, z + depth, r=0.004)

# ---------------- 1. Retro Upright ----------------
save('cab_up_side', art('', None, 512, 1024, motif=stars))
save('cab_up_marquee', tex.marquee('RETRO CLASSICS'))
save('cab_up_front', art("DUSTIN'S ARCADE", 'INSERT COIN', 512, 512, motif=stars))
m = Mesh(); SY, SF, SW, SH = 23.0, 1.2, 11.5, 8.6
rounded_box(m, 'static', 'black', -0.42, 0.0, -0.30, 0.42, 1.10, 0.42, r=0.03, side_mat='cab_up_side')
rounded_box(m, 'static', 'black', -0.42, 1.10, -0.12, 0.42, 2.00, 0.42, r=0.03, side_mat='cab_up_side')
m.add('static', 'black', quad((-0.42, 1.10, -0.30), (0.42, 1.10, -0.30), (0.42, 1.15, -0.12), (-0.42, 1.15, -0.12)))
rounded_box(m, 'static', 'black', -0.40, 0.98, -0.42, 0.40, 1.06, -0.10, r=0.02)                    # control panel
stick(m, 0.18, 1.06, -0.28); buttons(m, 0.02, 1.06, -0.28)
rounded_box(m, 'static', 'cab_up_front', -0.40, 0.08, -0.31, 0.40, 0.96, -0.29, r=0.01, front_mat='cab_up_front')
rounded_box(m, 'static', 'bezel', -0.40, 1.08, -0.10, 0.40, 1.80, -0.05, r=0.02)
screen_hole(m, SY, SF, SW, SH)
rounded_box(m, 'marquee', 'black', -0.42, 1.80, -0.14, 0.42, 2.00, 0.0, r=0.02, front_mat='cab_up_marquee')
for s in (-1, 1): rounded_box(m, 'leds', 'cyan', s * 0.42 - 0.006, 0.05, -0.31, s * 0.42 + 0.006, 1.98, -0.30, r=0.003)
m.write(f'{OUT}/models/retro_upright.obj'); print('upright', (SY, SF, SW, SH))

# ---------------- 2. Night Shooter Deluxe (light gun; same screen as the gun cabinet) ----------------
save('cab_ns_side', art('', None, 768, 1024, top=(4, 4, 12), bot=(40, 10, 20), motif=spooky))
save('cab_ns_marquee', tex.marquee('NIGHT SHOOTER DELUXE'))
save('cab_ns_front', art('NIGHT SHOOTER', 'GRAB A GUN', 1024, 512, top=(4, 4, 12), bot=(40, 10, 20), motif=spooky))
m = Mesh(); SY, SF, SW, SH = 21.0, 3.48, 15.4, 12.0
rounded_box(m, 'static', 'black', -0.62, 0.0, -0.32, 0.62, 2.35, 0.45, r=0.04, side_mat='cab_ns_side')
rounded_box(m, 'static', 'cab_ns_front', -0.60, 0.05, -0.36, 0.60, 0.90, -0.30, r=0.02, front_mat='cab_ns_front')
rounded_box(m, 'static', 'bezel', -0.58, 0.92, -0.30, 0.58, 1.86, -0.20, r=0.03)
screen_hole(m, SY, SF, SW, SH)
for s in (-1, 1):
    rounded_box(m, 'static', 'speaker', s * 0.56 - 0.05, 0.95, -0.33, s * 0.56 + 0.05, 1.80, -0.25, r=0.02)
    rounded_box(m, 'static', 'red', s * 0.40 - 0.07, 0.78, -0.42, s * 0.40 + 0.07, 0.88, -0.32, r=0.02)    # gun holsters
    rounded_box(m, 'leds', 'pink', s * 0.62 - 0.008, 0.05, -0.33, s * 0.62 + 0.008, 2.30, -0.31, r=0.004)
rounded_box(m, 'marquee', 'black', -0.62, 1.95, -0.36, 0.62, 2.35, -0.10, r=0.03, front_mat='cab_ns_marquee')
m.write(f'{OUT}/models/night_shooter.obj'); print('night', (SY, SF, SW, SH))

# ---------------- 3. Turbo Moto (ride-on bike racer) ----------------
save('cab_tm_side', art('', None, 768, 768, motif=speed))
save('cab_tm_marquee', tex.marquee('TURBO MOTO'))
m = Mesh(); SY, SF, SW, SH = 25.0, 1.5, 13.0, 9.5
rounded_box(m, 'static', 'black', -0.44, 0.0, -0.12, 0.44, 2.10, 0.42, r=0.04, side_mat='cab_tm_side')
rounded_box(m, 'static', 'bezel', -0.44, 1.20, -0.14, 0.44, 1.95, -0.06, r=0.03)
screen_hole(m, SY, SF, SW, SH)
rounded_box(m, 'marquee', 'black', -0.45, 1.95, -0.16, 0.45, 2.20, 0.10, r=0.03, front_mat='cab_tm_marquee')
# the bike: body, tank, seat, handlebars, wheels
rounded_box(m, 'static', 'red', -0.10, 0.35, -0.95, 0.10, 0.62, -0.20, r=0.06)
rounded_box(m, 'static', 'panel_white', -0.11, 0.62, -0.55, 0.11, 0.72, -0.30, r=0.05)
rounded_box(m, 'static', 'black', -0.10, 0.62, -0.90, 0.10, 0.70, -0.58, r=0.04)
cylinder_x(m, 'static', 'chrome', 0.95, -0.22, -0.26, 0.26, 0.012, seg=10)
for s in (-1, 1): sphere(m, 'static', 'rubber', s * 0.26, 0.95, -0.22, 0.025, la=5, lo=8)
rounded_box(m, 'static', 'chrome', -0.02, 0.62, -0.26, 0.02, 0.95, -0.18, r=0.01)
for z in (-1.0, -0.15):
    cylinder_x(m, 'static', 'rubber', 0.22, z, -0.06, 0.06, 0.20, seg=24)
m.write(f'{OUT}/models/turbo_moto.obj'); print('moto', (SY, SF, SW, SH))

# ---------------- 4. Star Pilot (sit-in cockpit) ----------------
save('cab_sp_side', art('', None, 1024, 768, top=(2, 4, 20), bot=(20, 30, 90), motif=stars))
save('cab_sp_marquee', tex.marquee('STAR PILOT'))
m = Mesh(); SY, SF, SW, SH = 19.0, -1.0, 13.0, 9.5
rounded_box(m, 'static', 'black', -0.50, 0.0, -0.20, 0.50, 1.90, 0.48, r=0.06, side_mat='cab_sp_side')
rounded_box(m, 'static', 'bezel', -0.46, 0.80, 0.07, 0.46, 1.60, 0.14, r=0.03)
screen_hole(m, SY, SF, SW, SH)
for s in (-1, 1): rounded_box(m, 'static', 'black', s * 0.48 - 0.04, 0.0, -1.2, s * 0.48 + 0.04, 1.75, -0.2, r=0.04, side_mat='cab_sp_side')   # cockpit walls
rounded_box(m, 'static', 'black', -0.52, 1.75, -1.2, 0.52, 1.90, -0.2, r=0.04)                                   # roof
rounded_box(m, 'marquee', 'black', -0.52, 1.90, -1.22, 0.52, 2.15, -0.9, r=0.03, front_mat='cab_sp_marquee')
rounded_box(m, 'static', 'black', -0.30, 0.0, -1.05, 0.30, 0.40, -0.70, r=0.05)                                  # seat base
rounded_box(m, 'static', 'blue_plastic', -0.28, 0.40, -1.05, 0.28, 0.48, -0.72, r=0.05)
rounded_box(m, 'static', 'blue_plastic', -0.28, 0.40, -1.12, 0.28, 0.95, -1.04, r=0.05)
rounded_box(m, 'static', 'black', -0.36, 0.62, -0.30, 0.36, 0.72, -0.10, r=0.03)                                 # yoke console
stick(m, 0.12, 0.72, -0.22, 'red'); stick(m, -0.12, 0.72, -0.22, 'blue_plastic')
for s in (-1, 1): rounded_box(m, 'leds', 'cyan', s * 0.52 - 0.008, 1.70, -1.21, s * 0.52 + 0.008, 1.74, -0.2, r=0.003)
m.write(f'{OUT}/models/star_pilot.obj'); print('pilot', (SY, SF, SW, SH))

# ---------------- 5. Versus Fighter (2-player sit-down) ----------------
save('cab_vf_side', art('', None, 768, 768, top=(60, 6, 10), bot=(10, 20, 80), motif=grid))
save('cab_vf_marquee', tex.marquee('VERSUS FIGHTER'))
m = Mesh(); SY, SF, SW, SH = 20.0, 4.3, 14.2, 11.0
rounded_box(m, 'static', 'panel_white', -0.46, 0.0, -0.10, 0.46, 1.85, 0.46, r=0.06, side_mat='cab_vf_side')
rounded_box(m, 'static', 'panel_white', -0.46, 0.0, -0.45, 0.46, 0.72, -0.08, r=0.05, side_mat='cab_vf_side')
rounded_box(m, 'static', 'black', -0.44, 0.72, -0.48, 0.44, 0.80, -0.14, r=0.03)
stick(m, 0.32, 0.80, -0.36); buttons(m, 0.22, 0.80, -0.36, 3, 0.04)
stick(m, -0.06, 0.80, -0.36, 'blue_plastic'); buttons(m, -0.16, 0.80, -0.36, 3, 0.04, ('blue_plastic', 'red'))
rounded_box(m, 'static', 'bezel', -0.44, 0.84, -0.30, 0.44, 1.62, -0.22, r=0.03)
screen_hole(m, SY, SF, SW, SH)
rounded_box(m, 'marquee', 'black', -0.46, 1.62, -0.30, 0.46, 1.85, -0.08, r=0.03, front_mat='cab_vf_marquee')
for s in (-1, 1): rounded_box(m, 'leds', 'pink', s * 0.46 - 0.006, 0.05, -0.46, s * 0.46 + 0.006, 1.82, -0.44, r=0.003)
m.write(f'{OUT}/models/versus_fighter.obj'); print('versus', (SY, SF, SW, SH))

# ---------------- 6. Dustin Multi (slim modern multi-game) ----------------
save('cab_dm_side', art('', None, 512, 1024, top=(4, 6, 20), bot=(10, 30, 110), motif=grid))
save('cab_dm_marquee', tex.marquee("DUSTIN'S MULTI"))
m = Mesh(); SY, SF, SW, SH = 25.0, 2.6, 12.6, 10.4
rounded_box(m, 'static', 'black', -0.40, 0.0, -0.16, 0.40, 2.30, 0.40, r=0.08, side_mat='cab_dm_side')
rounded_box(m, 'static', 'black', -0.40, 0.0, -0.40, 0.40, 1.00, -0.14, r=0.06, side_mat='cab_dm_side')
rounded_box(m, 'static', 'black', -0.38, 1.00, -0.44, 0.38, 1.07, -0.14, r=0.03)
stick(m, 0.18, 1.07, -0.32); buttons(m, 0.04, 1.07, -0.32, 4, 0.04)
rounded_box(m, 'static', 'bezel', -0.38, 1.20, -0.20, 0.38, 2.05, -0.15, r=0.03)
screen_hole(m, SY, SF, SW, SH)
for s in (-1, 1): rounded_box(m, 'leds', 'cyan', s * 0.40 - 0.008, 0.05, -0.41, s * 0.40 + 0.008, 2.26, -0.17, r=0.004)
rounded_box(m, 'marquee', 'black', -0.40, 2.08, -0.20, 0.40, 2.30, -0.10, r=0.03, front_mat='cab_dm_marquee')
m.write(f'{OUT}/models/dustin_multi.obj'); print('multi', (SY, SF, SW, SH))
