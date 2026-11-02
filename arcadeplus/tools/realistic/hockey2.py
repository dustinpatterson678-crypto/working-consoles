"""Full-size air hockey: 1 block wide, 2 blocks long (the main block plus one behind)."""
import sys, math
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw
OUT = '/home/claude/real/out'
P = 1 / 16.0
W, H = 512, 1024
t = Image.new("RGBA", (W, H), (236, 238, 244, 255)); d = ImageDraw.Draw(t)
for y in range(12, H, 24):
    for x in range(12, W, 24): d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=(190, 194, 204, 255))
d.line([(0, H / 2), (W, H / 2)], fill=(235, 40, 40, 255), width=8)
d.ellipse([W / 2 - 90, H / 2 - 90, W / 2 + 90, H / 2 + 90], outline=(235, 40, 40, 255), width=8)
for yy, col in ((0, (60, 140, 255)), (H, (235, 40, 40))):
    d.arc([W / 2 - 150, yy - 150, W / 2 + 150, yy + 150], 0, 360, fill=col + (255,), width=8)
    d.line([(0, abs(yy - H * 0.28)), (W, abs(yy - H * 0.28))], fill=col + (255,), width=6)
tex.glow_text(t, (W / 2, H / 2), "DUSTIN'S", tex.font(44), (255, 255, 255, 255), (230, 30, 30, 255), blur=4)
t.save(f'{OUT}/textures/hk_top.png')
m = Mesh()
X, Z0, Z1, TOP = 0.47, -0.47, 1.47, 0.78
rounded_box(m, 'static', 'black', -X + 0.04, 0.0, Z0 + 0.06, X - 0.04, 0.05, Z1 - 0.06, r=0.06)
for z in (Z0 + 0.15, Z1 - 0.15):
    for s in (-1, 1): rounded_box(m, 'static', 'black', s * (X - 0.12) - 0.06, 0.05, z - 0.06, s * (X - 0.12) + 0.06, 0.58, z + 0.06, r=0.03)   # legs
rounded_box(m, 'static', 'black', -X, 0.55, Z0, X, TOP, Z1, r=0.05, front_mat='hk_front', side_mat='side_art')
rounded_box(m, 'static', 'black', -X + 0.06, TOP, Z0 + 0.06, X - 0.06, TOP + 0.01, Z1 - 0.06, r=0.01, top_mat='hk_top')
for s in (-1, 1):
    rounded_box(m, 'static', 'chrome', s * (X - 0.03) - 0.03, TOP, Z0, s * (X - 0.03) + 0.03, TOP + 0.06, Z1, r=0.02)
    for x0, x1 in ((-X, -0.18), (0.18, X)):
        zc = Z0 + 0.03 if s < 0 else Z1 - 0.03
        rounded_box(m, 'static', 'chrome', x0, TOP, zc - 0.03, x1, TOP + 0.06, zc + 0.03, r=0.02)
    zc = Z0 + 0.03 if s < 0 else Z1 - 0.03
    rounded_box(m, 'static', 'rubber', -0.18, TOP - 0.08, zc - 0.03, 0.18, TOP, zc + 0.03, r=0.01)
rounded_box(m, 'leds', 'cyan', -X, 0.70, Z0 - 0.005, X, 0.72, Z0 + 0.005, r=0.004)
rounded_box(m, 'leds', 'cyan', -X, 0.70, Z1 - 0.005, X, 0.72, Z1 + 0.005, r=0.004)
rounded_box(m, 'static', 'chrome', -0.04, TOP + 0.06, Z1 - 0.06, 0.04, 1.20, Z1 - 0.02, r=0.01)
rounded_box(m, 'marquee', 'black', -0.46, 1.20, Z1 - 0.12, 0.46, 1.52, Z1, r=0.03, front_mat='hk_score')
rounded_box(m, 'marquee', 'black', -0.46, 1.52, Z1 - 0.12, 0.46, 1.70, Z1, r=0.03, front_mat='hk_marquee')
cylinder_y(m, 'puck', 'puck_red', 0, 0, TOP + 0.01, TOP + 0.03, 0.9 * P, seg=24)
for g, mat in (('mallet_p', 'blue_plastic'), ('mallet_a', 'redplastic')):
    cylinder_y(m, g, mat, 0, 0, TOP + 0.01, TOP + 0.035, 1.3 * P, seg=28)
    cylinder_y(m, g, mat, 0, 0, TOP + 0.035, TOP + 0.08, 0.45 * P, seg=16)
    sphere(m, g, mat, 0, TOP + 0.085, 0, 0.55 * P, la=6, lo=12)
m.write(f'{OUT}/models/hockey.obj')
print('ok')
