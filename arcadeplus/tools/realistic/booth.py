"""The prize booth: a walk-up redemption counter with glass cases, side returns, a back wall of shelves and a PRIZES sign.
Model frame like the machines: front (customers) is -z, -x is the customer's right. Core cell at (0, 0, 0); booth spans x -3.5..3.5, z -0.5..3.5."""
import sys, math, os
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
def save(name, im): im.save(f'{OUT}/textures/{name}.png')

save('pb_front', tex.neon_art(2048, 512, "DUSTIN'S ARCADE PRIZES", 'REDEEM YOUR TICKETS HERE'))
save('pb_sign', tex.marquee('PRIZES', 1024, 256))
def pegboard(w=512, h=512):
    img = tex.solid((24, 50, 140), w, h, noise=4); d = ImageDraw.Draw(img)
    for y in range(16, h, 32):
        for x in range(16, w, 32): d.ellipse([x - 4, y - 4, x + 4, y + 4], fill=(10, 20, 60, 255))
    return img
save('pb_peg', pegboard())
save('pb_glass', Image.new("RGBA", (16, 16), (170, 210, 255, 70)))
save('pb_felt', tex.solid((140, 20, 26), noise=6))
save('pb_top', tex.solid((235, 235, 240), noise=3))
save('pb_muncher', tex.neon_art(512, 512, 'TICKETS', 'FEED ME'))
m = Mesh()
W = 3.5
# front counter: cabinet with art, glass display case on top
rounded_box(m, 'static', 'black', -W, 0.0, -0.45, W, 0.08, 0.45, r=0.03)
rounded_box(m, 'static', 'black', -W + 0.02, 0.08, -0.42, W - 0.02, 0.62, 0.42, r=0.03, front_mat='pb_front', side_mat='side_art')
rounded_box(m, 'static', 'chrome', -W, 0.62, -0.46, W, 0.66, 0.46, r=0.02)
rounded_box(m, 'static', 'pb_felt', -W + 0.06, 0.66, -0.40, W - 0.06, 0.68, 0.40, r=0.01)
rounded_box(m, 'static', 'pb_top', -W, 1.05, -0.46, W, 1.09, 0.46, r=0.02)                   # counter top over the case
for x in [-W + 0.03 + k * (2 * W - 0.06) / 7 for k in range(8)]:
    rounded_box(m, 'static', 'chrome', x - 0.015, 0.66, -0.45, x + 0.015, 1.05, -0.42, r=0.005)   # case posts
rounded_box(m, 'glass', 'pb_glass', -W + 0.03, 0.66, -0.45, W - 0.03, 1.05, -0.44, r=0.002)
rounded_box(m, 'leds', 'cyan', -W, 0.60, -0.47, W, 0.62, -0.46, r=0.004)
# side returns (customers can't walk in)
for s in (-1, 1):
    x0, x1 = (W - 0.9, W) if s > 0 else (-W, -W + 0.9)
    rounded_box(m, 'static', 'black', x0, 0.0, 0.45, x1, 1.05, 2.55, r=0.03, side_mat='side_art')
    rounded_box(m, 'static', 'pb_top', x0 - 0.02, 1.05, 0.45, x1 + 0.02, 1.09, 2.57, r=0.02)
# back wall of shelves
rounded_box(m, 'static', 'black', -W, 0.0, 3.0, W, 3.7, 3.5, r=0.03)
rounded_box(m, 'static', 'pb_peg', -W + 0.1, 0.3, 2.98, W - 0.1, 3.5, 3.0, r=0.005, front_mat='pb_peg')
for y in (0.35, 1.15, 1.95, 2.75):
    rounded_box(m, 'static', 'chrome', -W + 0.12, y - 0.04, 2.55, W - 0.12, y, 2.98, r=0.01)
    rounded_box(m, 'leds', 'cyan', -W + 0.12, y - 0.06, 2.54, W - 0.12, y - 0.04, 2.56, r=0.004)
# PRIZES sign on top, lit
rounded_box(m, 'marquee', 'black', -2.6, 3.7, 2.9, 2.6, 4.6, 3.4, r=0.05, front_mat='pb_sign')
rounded_box(m, 'static', 'chrome', -2.65, 3.68, 2.85, 2.65, 3.72, 3.45, r=0.02)
# the ticket muncher on the counter
rounded_box(m, 'static', 'black', 2.4, 1.09, -0.30, 3.2, 1.75, 0.25, r=0.05, front_mat='pb_muncher')
rounded_box(m, 'static', 'chrome', 2.55, 1.40, -0.32, 3.05, 1.44, -0.29, r=0.01)
m.write(f'{OUT}/models/prize_stand.obj')
print('ok')
