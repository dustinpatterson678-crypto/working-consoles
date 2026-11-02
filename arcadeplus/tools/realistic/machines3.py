import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw, ImageFilter
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
os.makedirs(OUT + '/models', exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
P = 1 / 16.0

def pedestal(m, front, y1=0.80, z0=-0.38, z1=0.30, x=0.45):
    rounded_box(m, 'static', 'black', -x - 0.02, 0.0, z0 - 0.02, x + 0.02, 0.05, z1 + 0.02, r=0.06)
    rounded_box(m, 'static', 'black', -x, 0.05, z0, x, y1, z1, r=0.05, front_mat=front, side_mat='side_art')
    rounded_box(m, 'static', 'chrome', -x - 0.01, y1, z0 - 0.01, x + 0.01, y1 + 0.03, z1 + 0.01, r=0.05)
    rounded_box(m, 'static', 'badge', -0.2, y1 - 0.18, z0 - 0.012, 0.2, y1 - 0.10, z0 - 0.001, r=0.005, front_mat='badge')
    rounded_box(m, 'static', 'chrome', -0.30, 0.10, z0 - 0.005, -0.18, 0.16, z0 + 0.02, r=0.01)            # ticket mouth

def gradient(w, h, top, bot):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=tuple(int(a + (b - a) * t) for a, b in zip(top, bot)) + (255,))
    return img

save('blue_plastic', tex.solid((40, 90, 230), noise=3)); save('puck_red', tex.solid((220, 30, 30), noise=2))
save('steel', tex.solid((150, 156, 166), noise=6)); save('gold_door', tex.solid((230, 180, 50), noise=4))

# ---------------- Plinko Drop ----------------
SL = [1, 0, 0, 1, 4, 1, 0, 0, 1]
W, H = 1024, 1840                                       # board 0.80 x 1.44 blocks
b = gradient(W, H, (8, 18, 70), (20, 50, 150))
d = ImageDraw.Draw(b)
for i in range(40):
    x, y = random.random() * W, random.random() * H * 0.8; r = random.random() * 3 + 1
    d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 140))
bx0, bx1, by0, by1 = 0.40, -0.40, 0.86, 2.30              # board face: image left = +x
def bu(xpx): return (xpx * P - bx0) / (bx1 - bx0) * W
def bv(ypx): return (1 - (ypx * P - by0) / (by1 - by0)) * H
for i, v in enumerate(SL):                                # slot colours + values at the bottom
    xa, xb = 6 - i * 12 / 9, 6 - (i + 1) * 12 / 9
    col = (235, 40, 40) if v >= 4 else (60, 140, 255) if v >= 2 else (245, 245, 255) if v == 1 else (40, 40, 60)
    d.rectangle([bu(xa), bv(16.6), bu(xb), bv(13.8)], fill=col + (255,))
    f = tex.font(46)
    tex.glow_text(b, ((bu(xa) + bu(xb)) / 2, bv(15.2)), str(v), f, (255, 255, 255, 255) if v != 1 else (20, 20, 40, 255), (0, 0, 0, 255), blur=3)
tex.glow_text(b, (W / 2, bv(36.3)), "PLINKO DROP", tex.font(110), (255, 255, 255, 255), (230, 30, 30, 255), blur=10, stroke=3, stroke_fill=(150, 10, 10, 255))
tex.gloss(b, 30); save('pk_board', b)
save('pk_front', tex.neon_art(1024, 768, 'PLINKO DROP', 'DROP 3 PUCKS'))
save('pk_marquee', tex.marquee('PLINKO DROP'))
m = Mesh()
pedestal(m, 'pk_front')
rounded_box(m, 'static', 'black', -0.45, 0.83, 0.02, 0.45, 2.42, 0.30, r=0.04, front_mat='pk_board')
for s in (-1, 1):
    rounded_box(m, 'static', 'chrome', s * 0.42 - 0.025, 0.83, -0.06, s * 0.42 + 0.025, 2.42, 0.03, r=0.012)   # side rails
    rounded_box(m, 'leds', 'cyan', s * 0.42 - 0.008, 0.88, -0.065, s * 0.42 + 0.008, 2.38, -0.055, r=0.004)
for r in range(9):                                         # pegs
    y = (34.0 - r * 2.0 - 1.0) * P
    off = 0 if r % 2 == 0 else 1
    for k in range(-5, 6):
        x = (k * 1.0 + (0.5 if r % 2 else 0)) * P * 1.0
        if abs(x) > 0.36 or (k + off) % 2: continue
        cylinder_z(m, 'static', 'chrome', x, y, -0.05, 0.02, 0.009, seg=10)
for i in range(10):                                        # slot dividers
    x = (6 - i * 12 / 9) * P
    rounded_box(m, 'static', 'chrome', x - 0.006, 0.86, -0.05, x + 0.006, 16.6 * P, 0.02, r=0.003)
rounded_box(m, 'static', 'chrome', -0.42, 2.20, -0.06, 0.42, 2.23, 0.03, r=0.01)                   # dropper rail
rounded_box(m, 'dropper', 'redplastic', -0.045, 2.15, -0.075, 0.045, 2.25, -0.035, r=0.012)       # dropper carriage (moves)
cylinder_z(m, 'puck', 'puck_red', 0, 0, -0.045, -0.025, 0.045, seg=28, side_mat='chrome')         # the puck (drawn at its spot)
rounded_box(m, 'marquee', 'black', -0.46, 2.42, -0.02, 0.46, 2.70, 0.31, r=0.04, front_mat='pk_marquee')
cylinder_y(m, 'static', 'chrome', 0, -0.25, 0.83, 0.85, 0.08)
sphere(m, 'button', 'red', 0, 0.85, -0.25, 0.07, squash=0.45)
m.write(f'{OUT}/models/plinko.obj')

# ---------------- High Striker ----------------
W, H = 512, 2048
sc = gradient(W, H, (12, 20, 80), (30, 60, 170)); d = ImageDraw.Draw(sc)
marks = [("BELL!", 1.0), ("SUPER", 0.94), ("STRONG", 0.82), ("GOOD", 0.66), ("OKAY", 0.48), ("WEAK", 0.28)]
y0b, y1b = 0.35, 2.72                                      # puck range in blocks; board spans 0.30..2.95
for name, h in marks:
    yb = y0b + (y1b - y0b) * h; v = (1 - (yb - 0.30) / (2.95 - 0.30)) * H
    d.line([(20, v), (W - 20, v)], fill=(255, 255, 255, 255), width=4)
    tex.glow_text(sc, (W / 2, v - 40), name, tex.font(64), (255, 255, 255, 255), (230, 30, 30, 255) if h > 0.9 else (60, 140, 255, 255), blur=6)
for k in range(40):
    v = H * k / 40; d.line([(W * 0.42, v), (W * 0.58, v)], fill=(255, 255, 255, 90), width=2)
tex.gloss(sc, 30); save('st_scale', sc)
save('st_front', tex.neon_art(1024, 512, 'HIGH STRIKER', 'RING THE BELL'))
save('st_marquee', tex.marquee('HIGH STRIKER', 512, 320))
m = Mesh()
rounded_box(m, 'static', 'black', -0.47, 0.0, -0.47, 0.47, 0.05, 0.47, r=0.06)
rounded_box(m, 'static', 'black', -0.45, 0.05, -0.45, 0.45, 0.24, 0.45, r=0.05, front_mat='st_front', side_mat='side_art')
rounded_box(m, 'static', 'chrome', -0.46, 0.24, -0.46, 0.46, 0.26, 0.46, r=0.05)
rounded_box(m, 'static', 'black', -0.17, 0.26, -0.33, 0.17, 0.30, -0.05, r=0.03)                  # strike pad base
rounded_box(m, 'pad', 'redplastic', -0.13, 0.30, -0.29, 0.13, 0.34, -0.09, r=0.03)                # rubber pad
rounded_box(m, 'static', 'black', -0.28, 0.26, 0.26, 0.28, 2.95, 0.40, r=0.04, front_mat='st_scale')
for s in (-1, 1):
    rounded_box(m, 'static', 'chrome', s * 0.26 - 0.02, 0.26, 0.22, s * 0.26 + 0.02, 2.95, 0.27, r=0.01)
cylinder_y(m, 'static', 'chrome', 0, 0.20, 0.30, 2.80, 0.018, seg=16)                              # guide wire
for i in range(14):
    yb = 0.40 + i * (2.70 - 0.40) / 13
    for s in (-1, 1):
        sphere(m, f'bulb_{i}', 'bulb', s * 0.26, yb, 0.205, 0.022, la=6, lo=8)
cylinder_y(m, 'bell', 'gold', 0, 0.20, 2.80, 2.96, 0.13, seg=36, r_top=0.06)                       # the bell
sphere(m, 'bell', 'gold', 0, 2.96, 0.20, 0.06, la=6, lo=12, squash=0.6)
cylinder_y(m, 'puck', 'puck_red', 0, 0.20, 0.0, 0.07, 0.07, seg=24)                               # the puck (moved up the wire)
rounded_box(m, 'marquee', 'black', -0.34, 2.98, 0.18, 0.34, 3.30, 0.42, r=0.03, front_mat='st_marquee')
for i in range(10):                                        # power meter lamps along the front edge
    x = 0.36 - i * 0.08
    sphere(m, f'meter_{i}', 'lamp_green' if i < 6 else 'lamp_yellow' if i < 9 else 'lamp_red', x, 0.20, -0.455, 0.022, la=6, lo=8)
HZ, HY = -0.46, 0.34                                       # hammer pivots at the front edge, head lands on the pad
cylinder_z(m, 'hammer', 'wood', 0, HY + 0.02, HZ, -0.19, 0.016, seg=10)
rounded_box(m, 'hammer', 'black', -0.11, HY - 0.03, -0.26, 0.11, HY + 0.09, -0.13, r=0.03)
for s in (-1, 1):
    cylinder_x(m, 'hammer', 'rubber', HY + 0.03, -0.195, s * 0.11, s * 0.13, 0.055, seg=16)
m.write(f'{OUT}/models/striker.obj')

# ---------------- Air Hockey ----------------
W, H = 1024, 1210
t = Image.new("RGBA", (W, H), (236, 238, 244, 255)); d = ImageDraw.Draw(t)
for y in range(16, H, 28):
    for x in range(16, W, 28): d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(190, 194, 204, 255))
d.line([(0, H / 2), (W, H / 2)], fill=(235, 40, 40, 255), width=8)
d.ellipse([W / 2 - 150, H / 2 - 150, W / 2 + 150, H / 2 + 150], outline=(235, 40, 40, 255), width=8)
for yy, col in ((0, (60, 140, 255)), (H, (235, 40, 40))):
    d.arc([W / 2 - 260, yy - 260, W / 2 + 260, yy + 260], 0, 360, fill=col + (255,), width=8)
    d.line([(0, abs(yy - H * 0.3)), (W, abs(yy - H * 0.3))], fill=col + (255,), width=6)
tex.glow_text(t, (W / 2, H / 2), "DUSTIN'S ARCADE", tex.font(54), (255, 255, 255, 255), (230, 30, 30, 255), blur=4)
save('hk_top', t)
save('hk_front', tex.neon_art(1024, 260, 'AIR HOCKEY'))
save('hk_marquee', tex.marquee('AIR HOCKEY', 1024, 240))
def score_panel(w=1024, h=360):
    img = gradient(w, h, (8, 16, 60), (16, 40, 120)); dd = ImageDraw.Draw(img)
    for x0, lab, col in ((0.06, 'YOU', (60, 140, 255)), (0.56, 'MACHINE', (235, 40, 40))):
        dd.rounded_rectangle([w * x0, h * 0.30, w * (x0 + 0.38), h * 0.92], 20, fill=(4, 4, 8, 255), outline=col + (255,), width=6)
        tex.glow_text(img, (w * (x0 + 0.19), h * 0.15), lab, tex.font(54), (255, 255, 255, 255), col + (255,), blur=5)
    tex.gloss(img, 30); return img
save('hk_score', score_panel())
m = Mesh()
TW, TL = 6.4 * P, 7.4 * P                                   # table half size incl. rails
rounded_box(m, 'static', 'black', -0.42, 0.0, -0.48, 0.42, 0.05, 0.48, r=0.06)
for s in (-1, 1):
    rounded_box(m, 'static', 'black', s * 0.34 - 0.05, 0.05, -0.40, s * 0.34 + 0.05, 0.62, 0.40, r=0.03)   # legs / side skirts
rounded_box(m, 'static', 'black', -0.48, 0.55, -0.50, 0.48, 0.78, 0.50, r=0.05, front_mat='hk_front', side_mat='side_art')
rounded_box(m, 'static', 'black', -0.37, 0.78, -0.43, 0.37, 0.79, 0.43, r=0.01, top_mat='hk_top')     # air surface
for s in (-1, 1):
    rounded_box(m, 'static', 'chrome', s * 0.40 - 0.03, 0.78, -0.47, s * 0.40 + 0.03, 0.84, 0.47, r=0.02)   # side rails
    for x0, x1 in ((-0.40, -0.14), (0.14, 0.40)):
        rounded_box(m, 'static', 'chrome', x0, 0.78, s * 0.445 - 0.025, x1, 0.84, s * 0.445 + 0.025, r=0.02)   # end rails around the goals
    rounded_box(m, 'static', 'rubber', -0.14, 0.70, s * 0.45 - 0.03, 0.14, 0.78, s * 0.45 + 0.03, r=0.01)        # goal mouth
rounded_box(m, 'leds', 'cyan', -0.47, 0.70, -0.505, 0.47, 0.72, -0.495, r=0.005)
rounded_box(m, 'static', 'chrome', -0.05, 0.84, 0.44, 0.05, 1.20, 0.48, r=0.01)                    # scoreboard post
rounded_box(m, 'marquee', 'black', -0.46, 1.20, 0.38, 0.46, 1.52, 0.50, r=0.03, front_mat='hk_score')
rounded_box(m, 'marquee', 'black', -0.46, 1.52, 0.38, 0.46, 1.70, 0.50, r=0.03, front_mat='hk_marquee')
cylinder_y(m, 'puck', 'puck_red', 0, 0, 0.79, 0.81, 0.7 * P, seg=24)
for g, mat in (('mallet_p', 'blue_plastic'), ('mallet_a', 'redplastic')):
    cylinder_y(m, g, mat, 0, 0, 0.79, 0.815, 1.0 * P, seg=28)
    cylinder_y(m, g, mat, 0, 0, 0.815, 0.86, 0.35 * P, seg=16)
    sphere(m, g, mat, 0, 0.865, 0, 0.45 * P, la=6, lo=12)
m.write(f'{OUT}/models/hockey.obj')

# ---------------- Lucky Locker ----------------
def door(w=256, h=512):
    img = tex.solid((140, 148, 160), w, h, noise=6); dd = ImageDraw.Draw(img)
    for k in range(6):
        y = h * 0.1 + k * 18; dd.rounded_rectangle([w * 0.2, y, w * 0.8, y + 8], 4, fill=(40, 44, 52, 255))
    dd.rectangle([w * 0.72, h * 0.48, w * 0.84, h * 0.62], fill=(30, 30, 36, 255))
    dd.rectangle([2, 2, w - 3, h - 3], outline=(90, 96, 108, 255), width=6)
    tex.gloss(img, 40); return img
save('lk_door', door())
save('lk_front', tex.neon_art(1024, 768, 'LUCKY LOCKER', 'STOP ON GOLD'))
save('lk_marquee', tex.marquee('LUCKY LOCKER'))
save('lk_inside', tex.solid((20, 20, 26), noise=3))
m = Mesh()
pedestal(m, 'lk_front')
rounded_box(m, 'static', 'black', -0.47, 0.83, -0.12, 0.47, 2.38, 0.30, r=0.04)
for c in range(4):
    for r in range(3):
        i = r * 4 + c
        x1 = 0.40 - c * 0.20; x0 = x1 - 0.18
        y1 = 2.18 - r * 0.44; y0 = y1 - 0.36
        rounded_box(m, 'static', 'lk_inside', x0, y0, -0.125, x1, y1, -0.10, r=0.004)
        # door: hinged at x1 (the player's left edge of the door); drawn in its own group so it can swing
        rounded_box(m, f'door_{i}', 'steel', x0 + 0.004, y0 + 0.004, -0.15, x1 - 0.004, y1 - 0.004, -0.13, r=0.006, front_mat='lk_door')
        sphere(m, f'lamp_{i}', 'bulb', (x0 + x1) / 2, y1 + 0.035, -0.135, 0.018, la=6, lo=8)
for s in (-1, 1):
    rounded_box(m, 'leds', 'cyan', s * 0.45 - 0.008, 0.88, -0.135, s * 0.45 + 0.008, 2.34, -0.125, r=0.004)
rounded_box(m, 'marquee', 'black', -0.48, 2.38, -0.14, 0.48, 2.68, 0.31, r=0.04, front_mat='lk_marquee')
cylinder_y(m, 'static', 'chrome', 0, -0.25, 0.83, 0.85, 0.08)
sphere(m, 'button', 'red', 0, 0.85, -0.25, 0.07, squash=0.45)
m.write(f'{OUT}/models/locker.obj')
print('ok')
