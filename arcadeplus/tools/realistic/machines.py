import sys, math, os
sys.path.insert(0, '.')
from mesh import *
import tex
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
os.makedirs(OUT + '/models', exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
for k, im in {'black': tex.black_gloss(), 'chrome': tex.chrome(), 'red': tex.solid((210, 24, 24)), 'rubber': tex.solid((30, 30, 34)),
              'bulb': tex.solid((255, 244, 200), noise=2), 'gold': tex.solid((216, 168, 48)), 'cyan': tex.solid((60, 140, 255), noise=2),
              'pink': tex.solid((235, 40, 40), noise=2), 'badge': tex.badge(), 'side_art': tex.side_art(),
              'lamp_red': tex.solid((255, 40, 40), noise=3), 'lamp_blue': tex.solid((60, 120, 255), noise=3),
              'lamp_green': tex.solid((60, 255, 110), noise=3), 'lamp_yellow': tex.solid((255, 210, 60), noise=3)}.items():
    save(k, im)

def pedestal(m, front, y1=0.80, z0=-0.38, z1=0.30):
    rounded_box(m, 'static', 'black', -0.47, 0.0, z0 - 0.02, 0.47, 0.05, z1 + 0.02, r=0.06)
    rounded_box(m, 'static', 'black', -0.45, 0.05, z0, 0.45, y1, z1, r=0.05, front_mat=front, side_mat='side_art')
    rounded_box(m, 'static', 'chrome', -0.46, y1, z0 - 0.01, 0.46, y1 + 0.03, z1 + 0.01, r=0.05)
    rounded_box(m, 'static', 'badge', -0.2, y1 - 0.18, z0 - 0.012, 0.2, y1 - 0.10, z0 - 0.001, r=0.005, front_mat='badge')
    rounded_box(m, 'static', 'chrome', -0.30, 0.10, z0 - 0.005, -0.18, 0.16, z0 + 0.02, r=0.01)            # ticket mouth

# ---------------- Bonus Drum ----------------
DV = [1, 2, 1, 25, 1, 2, 1, 10, 0, 2, 1, 15, 1, 2, 0, 250, 1, 2, 1, 10]
base = [(32, 96, 216, 255), (232, 232, 236, 255), (212, 32, 32, 255), (232, 232, 236, 255)]
dcol = lambda i, v: (64, 224, 96, 255) if v >= 250 else (138, 42, 200, 255) if v >= 25 else base[i % 4]
save('bd_strip', tex.drum_strip(DV, dcol)); save('bd_front', tex.neon_art(1024, 768, 'BONUS DRUM', 'PULL THE LEVER  -  WIN THE 250'))
save('bd_marquee', tex.marquee('BONUS DRUM')); save('bd_bonus', tex.marquee('250 BONUS', 512, 160))
m = Mesh(); CY, CZ, R, X = 1.20, 0.0, 0.30, 0.33
pedestal(m, 'bd_front')
for s in (-1, 1):
    x0, x1 = (0.40, 0.47) if s > 0 else (-0.47, -0.40)
    rounded_box(m, 'static', 'black', x0, 0.83, -0.30, x1, 1.62, 0.30, r=0.03)                              # cheek panels
    rounded_box(m, 'leds', 'pink', x0 + 0.01 if s < 0 else x0 - 0.002, 0.88, -0.31, x1 - 0.01 if s > 0 else x1 + 0.002, 1.58, -0.295, r=0.004)
    cylinder_x(m, 'static', 'chrome', CY, CZ, s * X if s > 0 else -X - 0.03, s * X + 0.03 if s > 0 else -X, R + 0.03, seg=40)   # end caps
cylinder_x(m, 'drum', 'black', CY, CZ, -X, X, R, seg=80, side_mat='bd_strip')
for s in (-1, 1):
    arch_x(m, 'static', 'chrome', s * 0.37, CY, CZ, R + 0.10, 0.018, -0.05, math.pi + 0.05)
for i in range(18):
    s = -1 if i < 9 else 1; k = i % 9
    t = 0.15 + (math.pi - 0.3) * k / 8
    sphere(m, f'bulb_{i}', 'bulb', s * 0.37, CY + math.sin(t) * (R + 0.10), CZ - math.cos(t) * (R + 0.10), 0.02, la=6, lo=8)
rounded_box(m, 'static', 'black', -0.46, CY + R + 0.12, -0.20, 0.46, CY + R + 0.44, 0.24, r=0.04, front_mat='bd_marquee')
rounded_box(m, 'static', 'chrome', -0.47, CY + R + 0.11, -0.21, 0.47, CY + R + 0.135, 0.25, r=0.02)
rounded_box(m, 'bonus', 'black', -0.24, 0.84, -0.33, 0.24, 0.94, -0.26, r=0.02, front_mat='bd_bonus')          # lit bonus readout on the deck
FY, FZ = CY + 0.14, CZ - R - 0.09                                                                              # flapper bracket above the front
rounded_box(m, 'static', 'black', -0.26, FY - 0.01, FZ - 0.04, -0.14, FY + 0.05, CZ - 0.05, r=0.012)
m.add('flapper', 'rubber', quad((-0.22, FY, FZ), (-0.18, FY, FZ), (-0.195, FY - 0.13, FZ + 0.035), (-0.205, FY - 0.13, FZ + 0.035)))
LX, LY, LZ = -0.50, 0.95, 0.0                                                                                  # side lever (player's right)
cylinder_x(m, 'static', 'chrome', LY, LZ, -0.50, -0.45, 0.06, seg=20)
m.add('lever', 'chrome', quad((LX - 0.012, LY, LZ - 0.012), (LX + 0.012, LY, LZ - 0.012), (LX + 0.012, LY + 0.48, LZ - 0.012), (LX - 0.012, LY + 0.48, LZ - 0.012)))
m.add('lever', 'chrome', quad((LX - 0.012, LY, LZ + 0.012), (LX - 0.012, LY, LZ - 0.012), (LX - 0.012, LY + 0.48, LZ - 0.012), (LX - 0.012, LY + 0.48, LZ + 0.012)))
m.add('lever', 'chrome', quad((LX + 0.012, LY, LZ - 0.012), (LX + 0.012, LY, LZ + 0.012), (LX + 0.012, LY + 0.48, LZ + 0.012), (LX + 0.012, LY + 0.48, LZ - 0.012)))
m.add('lever', 'chrome', quad((LX + 0.012, LY, LZ + 0.012), (LX - 0.012, LY, LZ + 0.012), (LX - 0.012, LY + 0.48, LZ + 0.012), (LX + 0.012, LY + 0.48, LZ + 0.012)))
sphere(m, 'lever', 'red', LX, LY + 0.52, LZ, 0.06)
m.write(f'{OUT}/models/bonusdrum.obj')

# ---------------- Light Chaser ----------------
_p = tex.neon_art(1024, 512, 'LIGHT CHASER', 'STOP ON RED = JACKPOT')
from PIL import Image as _I
_w = _I.new('RGBA', (2048, 512)); _w.paste(_p, (512, 0)); _w.paste(_p.crop((512, 0, 1024, 512)), (0, 0)); _w.paste(_p.crop((0, 0, 512, 512)), (1536, 0))
save('lc_wrap', _w)
save('lc_top', tex.lamp_ring_top()); save('lc_marquee', tex.marquee('LIGHT CHASER  *  LIGHT CHASER', 2048, 320))
from mesh import cylinder_y_wrap, torus_y
m = Mesh()
cylinder_y(m, 'static', 'black', 0, 0, 0.0, 0.06, 0.50, seg=48)
cylinder_y_wrap(m, 'static', 'lc_wrap', 0, 0, 0.06, 0.80, 0.47, seg=64)
torus_y(m, 'static', 'chrome', 0, 0.80, 0, 0.47, 0.02)
cylinder_y(m, 'static', 'black', 0, 0, 0.80, 0.84, 0.47, seg=64, top_mat='lc_top')
torus_y(m, 'static', 'chrome', 0, 0.84, 0, 0.46, 0.012)
from mesh import sphere
CYCV = [0, 0, 1, 2, 1, 0, 5, 1, 2, 3, 1, 0, 10, 0, 1, 2, 1, 0, 5, 1, 2, 3, 1, 0]
for i in range(24):
    a = 2 * math.pi * i / 24; x, z = math.sin(a) * 0.37, math.cos(a) * 0.37
    mat = 'lamp_red' if i == 0 else 'lamp_blue' if i == 12 else 'lamp_green' if CYCV[i] >= 5 else 'lamp_yellow'
    cylinder_y(m, 'static', 'chrome', x, z, 0.84, 0.855, 0.058, seg=16)
    sphere(m, f'lamp_{i}', mat, x, 0.855, z, 0.05, la=6, lo=10, squash=0.6)
cylinder_y(m, 'static', 'black', 0, 0, 0.84, 1.55, 0.12, seg=40)
rounded_box(m, 'static', 'black', -0.11, 1.12, -0.14, 0.11, 1.33, -0.09, r=0.01)                            # jackpot display plate
cylinder_y_wrap(m, 'marquee', 'lc_marquee', 0, 0, 1.55, 1.85, 0.22, seg=48)
cylinder_y(m, 'static', 'chrome', 0, 0, 1.53, 1.56, 0.235, seg=48)
cylinder_y(m, 'static', 'chrome', 0, 0, 1.85, 1.88, 0.235, seg=48)
rounded_box(m, 'static', 'black', -0.11, 0.60, -0.53, 0.11, 0.70, -0.40, r=0.03)                             # button pod
cylinder_y(m, 'static', 'chrome', 0, -0.47, 0.70, 0.72, 0.07)
sphere(m, 'button', 'red', 0, 0.72, -0.47, 0.058, squash=0.45)
rounded_box(m, 'static', 'badge', -0.1, 0.62, -0.536, 0.1, 0.68, -0.53, r=0.003, front_mat='badge')
m.write(f'{OUT}/models/lightchaser.obj')

# ---------------- Tower Stack ----------------
save('ts_front', tex.neon_art(1024, 1024, 'TOWER STACK', 'STACK IT TO THE TOP'))
save('ts_marquee', tex.marquee('TOWER STACK')); save('ts_grid', tex.solid((8, 8, 12), 64, 64, noise=2))
def prize_window(w=1024, h=420):
    from PIL import Image, ImageDraw, ImageFilter
    img = Image.new("RGBA", (w, h), (10, 20, 60, 255)); d = ImageDraw.Draw(img)
    for x in range(0, w, 8): d.line([(x, 0), (x, h)], fill=(16, 30, 80, 255))
    cols = [(255, 120, 180), (110, 180, 255), (255, 210, 80), (120, 220, 120), (200, 120, 255)]
    for i in range(5):
        cx, cy = w * (0.12 + 0.19 * i), h * 0.58; c = cols[i]
        d.ellipse([cx - 70, cy - 60, cx + 70, cy + 90], fill=c + (255,)); d.ellipse([cx - 52, cy - 140, cx + 52, cy - 30], fill=c + (255,))
        d.ellipse([cx - 60, cy - 160, cx - 25, cy - 120], fill=c + (255,)); d.ellipse([cx + 25, cy - 160, cx + 60, cy - 120], fill=c + (255,))
        d.ellipse([cx - 22, cy - 100, cx - 10, cy - 88], fill=(20, 20, 20, 255)); d.ellipse([cx + 10, cy - 100, cx + 22, cy - 88], fill=(20, 20, 20, 255))
    d.rectangle([0, h * 0.86, w, h], fill=(90, 50, 20, 255))
    tex.glow_text(img, (w / 2, h * 0.12), "MAJOR PRIZE", tex.font(int(h * 0.15)), (255, 255, 255, 255), (230, 30, 30, 255), blur=8)
    tex.gloss(img, 50); return img
save('ts_prize', prize_window())
m = Mesh()
rounded_box(m, 'static', 'black', -0.47, 0.0, -0.40, 0.47, 0.05, 0.38, r=0.06)
rounded_box(m, 'static', 'black', -0.45, 0.05, -0.38, 0.45, 0.95, 0.36, r=0.05, front_mat='ts_front', side_mat='side_art')
rounded_box(m, 'static', 'chrome', -0.46, 0.95, -0.42, 0.46, 0.98, 0.37, r=0.04)
rounded_box(m, 'static', 'black', -0.44, 0.98, -0.41, 0.44, 1.02, -0.06, r=0.03)
cylinder_y(m, 'static', 'chrome', 0, -0.24, 1.02, 1.05, 0.10)
sphere(m, 'button', 'red', 0, 1.05, -0.24, 0.085, squash=0.45)
rounded_box(m, 'static', 'badge', -0.2, 0.75, -0.392, 0.2, 0.83, -0.381, r=0.005, front_mat='badge')
rounded_box(m, 'static', 'black', -0.45, 1.02, -0.06, 0.45, 2.50, 0.36, r=0.04, front_mat='ts_grid')        # lamp tower (grid drawn live)
for s in (-1, 1):
    rounded_box(m, 'leds', 'cyan', s * 0.43 - 0.012, 1.06, -0.075, s * 0.43 + 0.012, 2.46, -0.055, r=0.005)
rounded_box(m, 'static', 'chrome', -0.46, 2.49, -0.07, 0.46, 2.52, 0.37, r=0.02)
rounded_box(m, 'static', 'black', -0.45, 2.52, -0.06, 0.45, 2.95, 0.36, r=0.03, front_mat='ts_prize')
rounded_box(m, 'marquee', 'black', -0.46, 2.95, -0.07, 0.46, 3.18, 0.37, r=0.04, front_mat='ts_marquee')
m.write(f'{OUT}/models/towerstack.obj')
print('ok')
