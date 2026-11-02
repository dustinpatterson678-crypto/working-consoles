import sys, math, os
sys.path.insert(0, '.')
from mesh import *
import tex
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
os.makedirs(OUT + '/models', exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)
V = [50, 1, 0, 1, 3, 0, 1, 5, 0, 2, 0, 1, 0, 4, 0, 2, 1, 3, 0, 1]
P = [(212, 32, 32, 255), (240, 192, 32, 255), (32, 96, 216, 255), (32, 168, 72, 255), (236, 236, 242, 255)]
col = lambda i, v: (255, 214, 64, 255) if v >= 50 else (40, 40, 48, 255) if v == 0 else P[i % 5]
T = {
 'bw_front': tex.neon_art(1024, 768, 'BIG WHEEL', 'SPIN TO WIN TICKETS'),
 'bw_side': tex.side_art(), 'black': tex.black_gloss(), 'chrome': tex.chrome(),
 'bw_face': tex.wheel_face(V, col, 1024), 'bw_back': tex.backboard(), 'bw_marquee': tex.marquee('BIG WHEEL'),
 'red': tex.solid((210, 24, 24)), 'rubber': tex.solid((30, 30, 34)), 'bulb': tex.solid((255, 244, 200), noise=2),
 'gold': tex.solid((216, 168, 48)), 'cyan': tex.solid((60, 140, 255), noise=2), 'badge': tex.badge()}
for k, im in T.items(): im.save(f'{OUT}/textures/{k}.png')

m = Mesh()
HY = 1.52                      # hub height
# ---- pedestal ----
rounded_box(m, 'static', 'black', -0.47, 0.0, -0.40, 0.47, 0.05, 0.34, r=0.06)                         # plinth
rounded_box(m, 'static', 'black', -0.45, 0.05, -0.38, 0.45, 0.80, 0.32, r=0.05, front_mat='bw_front', side_mat='bw_side')
rounded_box(m, 'static', 'chrome', -0.46, 0.80, -0.39, 0.46, 0.83, 0.33, r=0.05)                         # chrome lip
rounded_box(m, 'static', 'black', -0.44, 0.83, -0.37, 0.44, 0.86, -0.02, r=0.03)                         # control deck
cylinder_y(m, 'static', 'chrome', 0.0, -0.22, 0.86, 0.90, 0.095)                                           # button bezel
sphere(m, 'button', 'red', 0.0, 0.90, -0.22, 0.078, squash=0.45)                                           # STOP button
rounded_box(m, 'static', 'chrome', 0.24, 0.86, -0.30, 0.36, 0.875, -0.20, r=0.01)                         # token slot plate
rounded_box(m, 'static', 'black', 0.27, 0.875, -0.27, 0.33, 0.88, -0.23, r=0.005)
rounded_box(m, 'static', 'chrome', -0.30, 0.10, -0.385, -0.18, 0.16, -0.36, r=0.01)                       # ticket mouth
# ---- tower: side columns with LED strips ----
for s in (-1, 1):
    x0, x1 = (0.40, 0.47) if s > 0 else (-0.47, -0.40)
    rounded_box(m, 'static', 'black', x0, 0.83, 0.10, x1, 2.06, 0.22, r=0.025)
    rounded_box(m, 'leds', 'cyan', x0 + 0.015, 0.95, 0.075, x1 - 0.015, 2.0, 0.10, r=0.006)
# ---- backboard, gold rim, bulbs ----
cylinder_z(m, 'static', 'black', 0.0, HY, 0.02, 0.10, 0.455, seg=64, cap_mat='bw_back')
torus_z(m, 'static', 'gold', 0.0, HY, 0.01, 0.45, 0.02)
for i in range(28):
    a = 2 * math.pi * i / 28
    sphere(m, f'bulb_{i}', 'bulb', -math.sin(a) * 0.425, HY + math.cos(a) * 0.425, -0.005, 0.016, la=6, lo=8)
# ---- the wheel (rotates about the hub, z axis) ----
cylinder_z(m, 'wheel', 'black', 0.0, HY, -0.07, -0.02, 0.385, seg=72, cap_mat='bw_face')
torus_z(m, 'wheel', 'chrome', 0.0, HY, -0.045, 0.385, 0.012, seg=72, tseg=8)
for i in range(20):
    a = 2 * math.pi * i / 20
    cylinder_z(m, 'wheel', 'chrome', -math.sin(a) * 0.372, HY + math.cos(a) * 0.372, -0.11, -0.07, 0.008, seg=8)
cylinder_z(m, 'wheel', 'chrome', 0.0, HY, -0.09, -0.07, 0.075, seg=32)
sphere(m, 'wheel', 'red', 0.0, HY, -0.09, 0.045, squash=1.0)
# ---- flapper (pivots at the top) ----
FY = HY + 0.46
rounded_box(m, 'static', 'black', -0.05, FY - 0.02, -0.16, 0.05, FY + 0.06, 0.02, r=0.015)                # mount
m.add('flapper', 'rubber', quad((-0.025, FY, -0.12), (0.025, FY, -0.12), (0.005, FY - 0.12, -0.12), (-0.005, FY - 0.12, -0.12)))
m.add('flapper', 'rubber', quad((0.025, FY, -0.10), (-0.025, FY, -0.10), (-0.005, FY - 0.12, -0.10), (0.005, FY - 0.12, -0.10)))
# ---- marquee crown ----
rounded_box(m, 'marquee', 'black', -0.47, 2.06, -0.06, 0.47, 2.36, 0.20, r=0.04, front_mat='bw_marquee')
rounded_box(m, 'static', 'chrome', -0.48, 2.04, -0.07, 0.48, 2.07, 0.21, r=0.02)
rounded_box(m, 'static', 'chrome', -0.48, 2.35, -0.07, 0.48, 2.38, 0.21, r=0.02)
rounded_box(m, 'static', 'badge', -0.2, 0.62, -0.392, 0.2, 0.70, -0.381, r=0.005, front_mat='badge')       # Dustin's Arcade badge
m.write(f'{OUT}/models/bigwheel.obj')
print('tris', sum(len(t) for g in m.groups.values() for t in g.values()))
