import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
P = 1 / 16.0
random.seed(3)

def pedestal(m, front, y1=0.80, z0=-0.38, z1=0.30, x=0.45):
    rounded_box(m, 'static', 'black', -x - 0.02, 0.0, z0 - 0.02, x + 0.02, 0.05, z1 + 0.02, r=0.06)
    rounded_box(m, 'static', 'black', -x, 0.05, z0, x, y1, z1, r=0.05, front_mat=front, side_mat='side_art')
    rounded_box(m, 'static', 'chrome', -x - 0.01, y1, z0 - 0.01, x + 0.01, y1 + 0.03, z1 + 0.01, r=0.05)
    rounded_box(m, 'static', 'badge', -0.2, y1 - 0.18, z0 - 0.012, 0.2, y1 - 0.10, z0 - 0.001, r=0.005, front_mat='badge')
    rounded_box(m, 'static', 'chrome', -0.30, 0.10, z0 - 0.005, -0.18, 0.16, z0 + 0.02, r=0.01)

def space(w, h, stars=160):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=(int(4 + 14 * t), int(6 + 18 * t), int(30 + 60 * t), 255))
    for _ in range(stars):
        x, y, r = random.random() * w, random.random() * h, random.random() * 2.2 + 0.6
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, random.randint(120, 255)))
    return img

save('ufo_glow', tex.solid((255, 140, 40), noise=6)); save('white_paint', tex.solid((240, 240, 244), noise=3))
save('lane_wood', tex.solid((24, 26, 44), noise=5)); save('gutter_glow', tex.solid((60, 140, 255), noise=2))

# ---------------- Rocket Race ----------------
W, H = 1024, 1408                                         # board x 0.84 wide, y 0.86..2.04
bd = space(W, H); d = ImageDraw.Draw(bd)
lanes_y = [1.85, 1.6, 1.35, 1.1]                          # rocket rows (blocks)
def by(yb): return (1 - (yb - 0.86) / (2.04 - 0.86)) * H
cols = [(235, 40, 40), (60, 140, 255), (240, 240, 255), (255, 210, 60)]
for i, yb in enumerate(lanes_y):
    d.rectangle([20, by(yb) - 40, W - 20, by(yb) + 40], outline=cols[i] + (255,), width=6)
fx = W * (1 - (0.38 - -0.36) / 0.84) + 40                      # finish line near model x -0.36 (player's right)
for k in range(0, H, 40): d.rectangle([W - 90, k, W - 60, k + 20], fill=(255, 255, 255, 255)); d.rectangle([W - 60, k + 20, W - 30, k + 40], fill=(255, 255, 255, 255))
tex.glow_text(bd, (W / 2, by(2.0)), "ROCKET RACE", tex.font(90), (255, 255, 255, 255), (230, 30, 30, 255), blur=8)
save('rr_board', bd)
save('rr_front', tex.neon_art(1024, 768, 'ROCKET RACE', 'BOOST TO THE FINISH'))
save('rr_marquee', tex.marquee('ROCKET RACE'))
m = Mesh()
pedestal(m, 'rr_front')
rounded_box(m, 'static', 'black', -0.46, 0.83, 0.02, 0.46, 2.10, 0.32, r=0.04, front_mat='rr_board')
rounded_box(m, 'marquee', 'black', -0.47, 2.10, 0.0, 0.47, 2.40, 0.33, r=0.04, front_mat='rr_marquee')
for s in (-1, 1): rounded_box(m, 'leds', 'cyan', s * 0.45 - 0.008, 0.88, 0.005, s * 0.45 + 0.008, 2.06, 0.015, r=0.004)
for i, mat in enumerate(['redplastic', 'blue_plastic', 'white_paint', 'gold']):
    g = f'rocket_{i}'
    cylinder_x(m, g, mat, 0, -0.05, -0.05, 0.05, 0.022, seg=16)                              # body (travels toward -x)
    sphere(m, g, mat, -0.055, 0, -0.05, 0.022, la=6, lo=10)                                  # nose
    rounded_box(m, g, 'black', 0.03, -0.04, -0.055, 0.06, 0.04, -0.045, r=0.004)              # fins
    sphere(m, g, 'ufo_glow', 0.06, 0, -0.05, 0.016, la=5, lo=8)                              # exhaust
for i in range(10):
    sphere(m, f'meter_{i}', 'lamp_green' if i < 6 else 'lamp_yellow' if i < 9 else 'lamp_red', 0.30 - i * 0.065, 0.835, -0.30, 0.02, la=6, lo=8)
cylinder_y(m, 'static', 'chrome', 0, -0.16, 0.83, 0.85, 0.08)
sphere(m, 'button', 'red', 0, 0.85, -0.16, 0.07, squash=0.45)
m.write(f'{OUT}/models/rocketrace.obj')

# ---------------- Galaxy Lanes (2 blocks long) ----------------
save('gl_front', tex.neon_art(1024, 400, 'GALAXY LANES', 'ROLL A STRIKE'))
save('gl_back', tex.neon_art(512, 512, 'STRIKE!', None))
save('gl_marquee', tex.marquee('GALAXY LANES'))
m = Mesh()
rounded_box(m, 'static', 'black', -0.46, 0.0, -0.5, 0.46, 0.05, 1.55, r=0.06)
rounded_box(m, 'static', 'black', -0.44, 0.05, -0.48, 0.44, 0.40, 1.5, r=0.04, front_mat='gl_front', side_mat='side_art')
rounded_box(m, 'static', 'lane_wood', -0.24, 0.40, -0.44, 0.24, 0.42, 1.45, r=0.01, top_mat='lane_wood')
for s in (-1, 1):
    rounded_box(m, 'leds', 'gutter_glow', s * 0.30 - 0.05, 0.38, -0.44, s * 0.30 + 0.05, 0.40, 1.45, r=0.01)   # glowing gutters
    rounded_box(m, 'static', 'black', s * 0.40 - 0.04, 0.40, -0.48, s * 0.40 + 0.04, 0.62, 1.5, r=0.02)
    rounded_box(m, 'leds', 'cyan', s * 0.40 - 0.012, 0.58, -0.485, s * 0.40 + 0.012, 0.61, 1.5, r=0.005)
rounded_box(m, 'static', 'black', -0.44, 0.42, 1.45, 0.44, 1.55, 1.55, r=0.03, front_mat='gl_back')
rounded_box(m, 'static', 'black', -0.44, 0.95, 0.95, 0.44, 1.02, 1.5, r=0.02)                                  # pin hood
rounded_box(m, 'marquee', 'black', -0.45, 1.55, 1.2, 0.45, 1.85, 1.56, r=0.03, front_mat='gl_marquee')
for i, (px, pz) in enumerate([(0, 19.0), (-0.7, 20.2), (0.7, 20.2), (-1.4, 21.4), (0, 21.4), (1.4, 21.4), (-2.1, 22.6), (-0.7, 22.6), (0.7, 22.6), (2.1, 22.6)]):
    x, z = px * P * 1.0, pz * P - 0.2
    cylinder_y(m, f'pin_{i}', 'white_paint', x, z, 0.42, 0.50, 0.016, seg=12, r_top=0.024)
    cylinder_y(m, f'pin_{i}', 'redplastic', x, z, 0.50, 0.515, 0.024, seg=12)
    cylinder_y(m, f'pin_{i}', 'white_paint', x, z, 0.515, 0.55, 0.024, seg=12, r_top=0.012)
    sphere(m, f'pin_{i}', 'white_paint', x, 0.56, z, 0.014, la=5, lo=8)
sphere(m, 'ball', 'blue_plastic', 0, 0.455, 0, 0.035, la=8, lo=12)
m.write(f'{OUT}/models/lanes.obj')

# ---------------- Meteor Strike ----------------
save('ms_screen', space(1024, 1408, 220))
save('ms_front', tex.neon_art(1024, 768, 'METEOR STRIKE', 'BLAST THE METEORS'))
save('ms_marquee', tex.marquee('METEOR STRIKE'))
save('ms_ground', tex.solid((80, 40, 30), noise=10))
m = Mesh()
pedestal(m, 'ms_front')
rounded_box(m, 'static', 'black', -0.47, 0.83, 0.02, 0.47, 2.30, 0.34, r=0.04)
rounded_box(m, 'static', 'black', -0.42, 0.95, 0.0, 0.42, 2.22, 0.03, r=0.01, front_mat='ms_screen')
rounded_box(m, 'static', 'ms_ground', -0.42, 0.86, -0.12, 0.42, 0.95, 0.03, r=0.01)
for s in (-1, 1): rounded_box(m, 'leds', 'pink', s * 0.445 - 0.01, 0.9, -0.01, s * 0.445 + 0.01, 2.26, 0.01, r=0.004)
rounded_box(m, 'marquee', 'black', -0.48, 2.30, 0.0, 0.48, 2.60, 0.35, r=0.04, front_mat='ms_marquee')
rounded_box(m, 'cannon', 'chrome', -0.05, 0.95, -0.08, 0.05, 1.01, -0.02, r=0.015)
cylinder_y(m, 'cannon', 'black', 0, -0.05, 1.01, 1.12, 0.018, seg=12)
sphere(m, 'meteor', 'ufo_glow', 0, 0, -0.04, 0.035, la=8, lo=12)
cylinder_y(m, 'shot', 'cyan', 0, -0.05, 0, 0.08, 0.008, seg=8)
cylinder_y(m, 'static', 'chrome', 0, -0.25, 0.83, 0.85, 0.08)
sphere(m, 'button', 'red', 0, 0.85, -0.25, 0.07, squash=0.45)
m.write(f'{OUT}/models/meteor.obj')

# ---------------- Snip It ----------------
save('si_front', tex.neon_art(1024, 768, 'SNIP IT', 'CUT TO WIN'))
save('si_marquee', tex.marquee('SNIP IT'))
save('si_back', space(512, 768, 60))
save('si_glass', Image.new("RGBA", (16, 16), (170, 210, 255, 60)))
m = Mesh()
pedestal(m, 'si_front')
for s in (-1, 1): rounded_box(m, 'static', 'chrome', s * 0.43 - 0.03, 0.83, -0.38, s * 0.43 + 0.03, 2.2, 0.30, r=0.015)
rounded_box(m, 'static', 'si_back', -0.43, 0.83, 0.26, 0.43, 2.2, 0.30, r=0.01, front_mat='si_back')
rounded_box(m, 'static', 'black', -0.46, 2.2, -0.40, 0.46, 2.26, 0.32, r=0.03)
rounded_box(m, 'marquee', 'black', -0.47, 2.26, -0.40, 0.47, 2.55, 0.32, r=0.04, front_mat='si_marquee')
rounded_box(m, 'glass', 'si_glass', -0.43, 0.85, -0.38, 0.43, 2.2, -0.37, r=0.002)
rounded_box(m, 'static', 'chrome', -0.40, 1.82, -0.02, 0.40, 1.84, 0.02, r=0.005)                                # scissor rail
rounded_box(m, 'string', 'white_paint', -0.002, -0.62, -0.002, 0.002, 0.0, 0.002, r=0.001)                      # string hangs down from y 0
for a in (0.35, -0.35):                                                                                             # scissors blades
    rounded_box(m, 'scissors', 'chrome', -0.004, -0.01, -0.12, 0.004, 0.01, 0.0, r=0.002)
torus_z(m, 'scissors', 'redplastic', 0, 0, -0.15, 0.03, 0.008, seg=16, tseg=6)
cylinder_y(m, 'static', 'chrome', 0, -0.28, 0.83, 0.85, 0.08)
sphere(m, 'button', 'red', 0, 0.85, -0.28, 0.07, squash=0.45)
m.write(f'{OUT}/models/snipit.obj')
print('ok')
