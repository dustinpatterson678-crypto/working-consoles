import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw, ImageFilter
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
U = 1 / 16.0                                            # old model pixels -> blocks

def front_cab(m, x0, x1, y1, z0, z1, art, r=0.04):
    rounded_box(m, 'static', 'black', x0 - 0.015, 0.0, z0 - 0.015, x1 + 0.015, 0.05, z1 + 0.015, r=r + 0.01)
    rounded_box(m, 'static', 'black', x0, 0.05, z0, x1, y1, z1, r=r, front_mat=art, side_mat='side_art')
    rounded_box(m, 'static', 'chrome', x0 - 0.01, y1 - 0.035, z0 - 0.01, x1 + 0.01, y1 - 0.008, z1 + 0.01, r=r)          # chrome band just under the top
    rounded_box(m, 'static', 'badge', -0.16, y1 - 0.13, z0 - 0.012, 0.16, y1 - 0.065, z0 - 0.001, r=0.004, front_mat='badge')
    rounded_box(m, 'static', 'chrome', -0.30, 0.09, z0 - 0.005, -0.18, 0.15, z0 + 0.02, r=0.01)

def wood(w=512, h=512, base=(196, 150, 96)):
    img = Image.new("RGBA", (w, h)); px = img.load()
    for y in range(h):
        for x in range(w):
            g = math.sin(x * 0.09 + math.sin(y * 0.013) * 3) * 10 + random.randint(-4, 4)
            px[x, y] = tuple(max(0, min(255, int(c + g))) for c in base) + (255,)
    d = ImageDraw.Draw(img)
    for x in range(0, w, w // 6): d.line([(x, 0), (x, h)], fill=(120, 84, 50, 255), width=2)
    tex.gloss(img, 30); return img

def net(w=256, h=256):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    for k in range(-h, w, 32):
        d.line([(k, 0), (k + h, h)], fill=(235, 235, 240, 255), width=2); d.line([(k + h, 0), (k, h)], fill=(235, 235, 240, 255), width=2)
    return img

def panel(title, w=1024, h=512, top=(10, 20, 70), bot=(20, 50, 140)):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bot)) + (255,))
    if title:
        sz = int(h * 0.3)
        while d.textlength(title, font=tex.font(sz)) > w * 0.88: sz -= 4
        tex.glow_text(img, (w / 2, h * 0.25), title, tex.font(sz), (255, 255, 255, 255), (230, 30, 30, 255), blur=10, stroke=3, stroke_fill=(150, 10, 10, 255))
    tex.gloss(img, 30); return img

for k, im in {'wood': wood(), 'lane': wood(base=(214, 176, 120)), 'net': net(), 'felt_green': tex.solid((40, 120, 60)), 'white': tex.solid((236, 236, 240)),
              'navy': tex.solid((14, 26, 70)), 'water': tex.solid((40, 110, 120), noise=8), 'redplastic': tex.solid((205, 30, 30)),
              'plush_pink': tex.solid((255, 130, 180), noise=10), 'plush_blue': tex.solid((110, 170, 255), noise=10), 'plush_yellow': tex.solid((255, 214, 90), noise=10),
              'plush_green': tex.solid((130, 220, 120), noise=10), 'plush_purple': tex.solid((180, 130, 255), noise=10)}.items():
    save(k, im)

# ---------------- Alley Roller (2 blocks long) ----------------
save('ar_front', tex.neon_art(1024, 400, 'ALLEY ROLLER', 'ROLL FOR THE 100'))
bb = panel(None, 512, 800); bd = ImageDraw.Draw(bb)
save('ar_back', bb); save('ar_marquee', tex.marquee('ALLEY ROLLER'))
m = Mesh()
front_cab(m, -0.44, 0.44, 0.36, -0.5, 1.0, 'ar_front')
rounded_box(m, 'static', 'lane', -0.38, 0.36, -0.44, 0.38, 0.38, 0.90, r=0.01, top_mat='lane')                  # maple lane
m.add('static', 'lane', quad((-0.38, 0.38, 0.88), (0.38, 0.38, 0.88), (0.38, 0.56, 1.0), (-0.38, 0.56, 1.0)))      # ramp up to the targets
for s in (-1, 1):
    rounded_box(m, 'static', 'black', s * 0.44 - 0.03, 0.36, -0.5, s * 0.44 + 0.03, 0.56, 1.0, r=0.02)          # side rails
    rounded_box(m, 'leds', 'cyan', s * 0.44 - 0.012, 0.50, -0.505, s * 0.44 + 0.012, 0.545, 1.0, r=0.006)
rounded_box(m, 'static', 'black', -0.44, 0.56, 1.0, 0.44, 1.62, 1.5, r=0.04, front_mat='ar_back')              # target cabinet
for (x, y, c) in ((0, 11, 'white'), (0, 14, 'cyan'), (0, 17, 'gold'), (0, 20, 'red'), (-4.5, 23, 'pink'), (4.5, 23, 'pink')):
    torus_z(m, 'static', c, -x * U * 0 + x * U, y * U, 0.995, 0.085, 0.018, seg=24, tseg=6)
    cylinder_z(m, 'static', 'black', x * U, y * U, 0.998, 1.03, 0.07, seg=20)
rounded_box(m, 'marquee', 'black', -0.45, 1.62, 0.94, 0.45, 1.80, 1.52, r=0.03, front_mat='ar_marquee')
m.write(f'{OUT}/models/skeeball.obj')

# ---------------- Hoop Frenzy (basketball, 1.75 long) ----------------
save('hf_front', tex.neon_art(1024, 420, 'HOOP FRENZY', '30 SECONDS - SINK IT'))
save('hf_marquee', tex.marquee('HOOP FRENZY')); save('hf_back', panel(None, 512, 512, (14, 26, 70), (30, 60, 150)))
m = Mesh()
front_cab(m, -0.5, 0.5, 0.44, -0.5, 1.25, 'hf_front')
rounded_box(m, 'static', 'wood', -0.44, 0.44, -0.44, 0.44, 0.465, 0.88, r=0.01, top_mat='wood')                  # ramp
for x in (-0.47, 0.47):
    for z in (-0.47, 1.22):
        cylinder_y(m, 'static', 'chrome', x, z, 0.44, 1.94, 0.022, seg=12)
    rounded_box(m, 'static', 'chrome', x - 0.02, 1.92, -0.49, x + 0.02, 1.96, 1.24, r=0.01)
    m.add('static', 'net', quad((x, 0.46, -0.45), (x, 0.46, 1.2), (x, 1.92, 1.2), (x, 1.92, -0.45), uv=((0, 2), (3, 2), (3, 0), (0, 0))))
rounded_box(m, 'static', 'chrome', -0.49, 1.92, 1.20, 0.49, 1.96, 1.24, r=0.01)
rounded_box(m, 'static', 'black', -0.44, 0.88, 1.13, 0.44, 1.88, 1.19, r=0.02, front_mat='hf_back')             # backboard (hoop + score drawn live)
rounded_box(m, 'marquee', 'black', -0.47, 1.76, -0.52, 0.47, 1.94, -0.44, r=0.02, front_mat='hf_marquee')
m.write(f'{OUT}/models/basketball.obj')

# ---------------- Mole Bonk ----------------
top = Image.new("RGBA", (512, 512), (40, 120, 60, 255)); td = ImageDraw.Draw(top)
for _ in range(900):
    x, y = random.randint(0, 511), random.randint(0, 511); td.point((x, y), fill=(60, 150, 80, 255))
for hx, hz in ((-4.5, -3.5), (4.5, -3.5), (0, 0), (-4.5, 3.5), (4.5, 3.5)):
    cx, cy = (hx * U + 0.5) * 512, (hz * U + 0.5) * 512
    td.ellipse([cx - 62, cy - 62, cx + 62, cy + 62], fill=(90, 60, 30, 255)); td.ellipse([cx - 50, cy - 50, cx + 50, cy + 50], fill=(10, 8, 6, 255))
save('mb_top', top); save('mb_front', tex.neon_art(1024, 768, 'MOLE BONK', 'BONK EM ALL - 30 SECONDS')); save('mb_marquee', tex.marquee('MOLE BONK'))
m = Mesh()
front_cab(m, -0.5, 0.5, 0.74, -0.5, 0.5, 'mb_front')
rounded_box(m, 'static', 'black', -0.48, 0.74, -0.48, 0.48, 0.765, 0.48, r=0.02, top_mat='mb_top')
rounded_box(m, 'static', 'black', -0.5, 0.765, 0.40, 0.5, 1.10, 0.5, r=0.03)
rounded_box(m, 'marquee', 'black', -0.48, 0.80, 0.39, 0.48, 1.08, 0.40, r=0.005, front_mat='mb_marquee')
rounded_box(m, 'leds', 'cyan', -0.48, 0.74, -0.51, 0.48, 0.755, -0.495, r=0.004)
m.write(f'{OUT}/models/whackamole.obj')

# ---------------- Coin Cascade ----------------
cb = panel('COIN CASCADE', 512, 512); cbd = ImageDraw.Draw(cb)
for _ in range(60):
    x, y, r = random.randint(20, 490), random.randint(200, 500), random.randint(14, 26)
    cbd.ellipse([x - r, y - r, x + r, y + r], fill=(230, 190, 60, 255), outline=(150, 110, 20, 255), width=3)
save('cc_back', cb); save('cc_front', tex.neon_art(1024, 640, 'COIN CASCADE', 'PUSH THE PILE OVER')); save('cc_marquee', tex.marquee('COIN CASCADE'))
m = Mesh()
front_cab(m, -0.5, 0.5, 0.62, -0.375, 0.5, 'cc_front')
rounded_box(m, 'static', 'black', -0.5, 0.62, 0.25, 0.5, 1.75, 0.5, r=0.03, front_mat=None)
rounded_box(m, 'static', 'black', -0.46, 0.64, 0.245, 0.46, 1.72, 0.25, r=0.005, front_mat='cc_back')
for s in (-1, 1):
    rounded_box(m, 'static', 'black', s * 0.47 - 0.03, 0.62, -0.375, s * 0.47 + 0.03, 1.75, 0.5, r=0.02)
    rounded_box(m, 'leds', 'pink', s * 0.47 - 0.01, 0.66, -0.385, s * 0.47 + 0.01, 1.72, -0.37, r=0.004)
rounded_box(m, 'static', 'chrome', -0.44, 0.81, -0.31, 0.44, 0.835, 0.25, r=0.005)                             # fixed shelf
rounded_box(m, 'static', 'chrome', -0.47, 1.70, -0.38, 0.47, 1.74, -0.34, r=0.01)                              # window header
rounded_box(m, 'marquee', 'black', -0.5, 1.75, -0.39, 0.5, 1.95, 0.5, r=0.04, front_mat='cc_marquee')
m.write(f'{OUT}/models/coinpusher.obj')

# ---------------- Prize Claw ----------------
save('pc_front', tex.neon_art(1024, 640, 'PRIZE CLAW', 'GRAB A PRIZE')); save('pc_marquee', tex.marquee('PRIZE CLAW'))
m = Mesh()
front_cab(m, -0.5, 0.5, 0.62, -0.5, 0.5, 'pc_front')
for x in (-0.47, 0.47):
    for z in (-0.47, 0.47):
        rounded_box(m, 'static', 'chrome', x - 0.03, 0.62, z - 0.03, x + 0.03, 1.88, z + 0.03, r=0.012)
rounded_box(m, 'static', 'white', -0.44, 0.62, -0.44, 0.44, 0.64, 0.44, r=0.005)
plush = ['plush_pink', 'plush_blue', 'plush_yellow', 'plush_green', 'plush_purple']
for i, (x, z) in enumerate(((-4, 2), (-1, 4), (2, 1), (4, 4), (-3, -1), (1, -2), (5, -1), (3, -4), (-1, 1))):
    c = plush[i % 5]
    sphere(m, 'static', c, x * U, 0.70, z * U, 0.075, la=8, lo=12)
    sphere(m, 'static', c, x * U, 0.80, z * U, 0.055, la=8, lo=12)
rounded_box(m, 'static', 'white', -0.44, 0.64, -0.44, -0.19, 0.86, -0.19, r=0.01)                              # prize chute
rounded_box(m, 'static', 'black', -0.42, 0.06, -0.508, -0.21, 0.19, -0.49, r=0.01)                             # prize door
rounded_box(m, 'static', 'black', -0.5, 1.86, -0.5, 0.5, 2.02, 0.5, r=0.03)
rounded_box(m, 'marquee', 'black', -0.49, 1.88, -0.505, 0.49, 2.0, -0.495, r=0.004, front_mat='pc_marquee')
rounded_box(m, 'static', 'black', -0.13, 0.55, -0.56, 0.13, 0.62, -0.48, r=0.02)                               # joystick deck
m.write(f'{OUT}/models/clawmachine.obj')

# ---------------- Swamp Smash ----------------
sb = panel('SWAMP SMASH', 1024, 512, (10, 30, 30), (20, 70, 50)); sbd = ImageDraw.Draw(sb)
for x in (6, 3, 0, -3, -6):
    cx = (0.5 - x * U) * 1024; sbd.ellipse([cx - 80, 300, cx + 80, 470], fill=(6, 8, 6, 255)); sbd.rectangle([cx - 80, 385, cx + 80, 512], fill=(6, 8, 6, 255))
save('ss_back', sb); save('ss_front', tex.neon_art(1024, 768, 'SWAMP SMASH', 'BONK THE GATORS')); save('ss_marquee', tex.marquee('SWAMP SMASH'))
m = Mesh()
front_cab(m, -0.5, 0.5, 0.75, -0.5, 0.5, 'ss_front')
rounded_box(m, 'static', 'black', -0.48, 0.75, -0.48, 0.48, 0.765, 0.25, r=0.02, top_mat='water')
rounded_box(m, 'static', 'black', -0.5, 0.75, 0.25, 0.5, 1.32, 0.5, r=0.03)
m.add('static', 'ss_back', quad((0.48, 0.76, 0.245), (-0.48, 0.76, 0.245), (-0.48, 1.30, 0.245), (0.48, 1.30, 0.245), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, -1.0)))
for x in (-7, -3.5, 0, 3.5, 7):
    cylinder_y(m, 'static', 'felt_green', x * U, (-5.4 + abs(x) * 0.3) * U, 0.765, 0.775, 0.045, seg=14)
m.write(f'{OUT}/models/gators.obj')

# ---------------- Color Echo ----------------
save('ce_front', tex.neon_art(1024, 768, 'COLOR ECHO', 'WATCH - THEN REPEAT')); save('ce_marquee', tex.marquee('COLOR ECHO'))
m = Mesh()
front_cab(m, -0.47, 0.47, 0.69, -0.47, 0.47, 'ce_front')
rounded_box(m, 'static', 'black', -0.47, 0.69, 0.40, 0.47, 1.07, 0.47, r=0.02)
rounded_box(m, 'marquee', 'black', -0.46, 0.96, 0.395, 0.46, 1.06, 0.40, r=0.003, front_mat='ce_marquee')
m.write(f'{OUT}/models/echo.obj')

# ---------------- Clown Knockdown ----------------
cs = Image.new("RGBA", (512, 512), (240, 240, 245, 255)); csd = ImageDraw.Draw(cs)
for x in range(0, 512, 64): csd.rectangle([x, 0, x + 31, 511], fill=(210, 30, 30, 255))
tex.gloss(cs, 25); save('ck_back', cs); save('ck_front', tex.neon_art(1024, 768, 'CLOWN KNOCKDOWN', 'KNOCK EM FLAT')); save('ck_marquee', tex.marquee('CLOWN KNOCKDOWN'))
m = Mesh()
front_cab(m, -0.5, 0.5, 0.75, -0.5, 0.5, 'ck_front')
rounded_box(m, 'static', 'redplastic', -0.48, 0.75, -0.48, 0.48, 0.79, 0.25, r=0.02)
rounded_box(m, 'static', 'black', -0.5, 0.75, 0.25, 0.5, 1.75, 0.5, r=0.03)
m.add('static', 'ck_back', quad((0.48, 0.79, 0.245), (-0.48, 0.79, 0.245), (-0.48, 1.74, 0.245), (0.48, 1.74, 0.245), uv=((0, 1), (2, 1), (2, 0), (0, 0)), n=(0.0, 0.0, -1.0)))
rounded_box(m, 'static', 'redplastic', -0.53, 1.75, -0.06, 0.53, 1.88, 0.53, r=0.04)
rounded_box(m, 'marquee', 'black', -0.52, 1.77, -0.075, 0.52, 1.86, -0.06, r=0.004, front_mat='ck_marquee')
m.write(f'{OUT}/models/clowns.obj')
print('ok')
