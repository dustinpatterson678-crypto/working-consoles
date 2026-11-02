"""Dustin's Arcade Grand Hall, futuristic edition: polished graphite floor with glowing inlaid lanes, gunmetal wall
panels split by light ribs, a high ceiling with floating halo rings, a giant video wall over the lounge arch
and a glowing portal around the doors.
Model coords in blocks, world-aligned: x = world x (so -x is your right as you walk in; layout lx = -x),
y up (floor top = 1), z into the hall (front wall = -0.5)."""
import sys, math, os, random
sys.path.insert(0, '.')
from mesh import *
import tex
from PIL import Image, ImageDraw, ImageFilter
OUT = sys.argv[1] if len(sys.argv) > 1 else '/home/claude/real/out'
os.makedirs(OUT + '/models', exist_ok=True); os.makedirs(OUT + '/textures', exist_ok=True)
def save(name, im): im.save(f'{OUT}/textures/{name}.png')
random.seed(7)

HX, DEPTH, CEIL, LOUNGE = 25, 64, 13, 51
X0, X1, Z0, Z1 = -HX - 0.5, HX + 0.5, -0.5, DEPTH - 0.5
ARCH, ARCH_H = 3.5, 5.0
L = lambda lx: -lx

# ---------------- textures ----------------
def floor_tex(n=512, base=(16, 18, 26)):
    img = Image.new("RGBA", (n, n), base + (255,)); d = ImageDraw.Draw(img)
    for k in range(0, n + 1, n // 4):
        d.line([(k, 0), (k, n)], fill=(34, 38, 52, 255), width=3); d.line([(0, k), (n, k)], fill=(34, 38, 52, 255), width=3)
    ov = Image.new("RGBA", (n, n), (0, 0, 0, 0)); od = ImageDraw.Draw(ov)
    for k in range(-n, n, 90): od.line([(k, n), (k + n, 0)], fill=(120, 150, 255, 10), width=40)
    img.alpha_composite(ov)
    noise = Image.effect_noise((n, n), 8).convert("L")
    return Image.composite(img, Image.new("RGBA", (n, n), tuple(max(0, c - 4) for c in base) + (255,)), noise.point(lambda v: 255 if v > 20 else 230))

def wall_tex(w=512, h=1536, base=(22, 26, 40)):
    img = Image.new("RGBA", (w, h), base + (255,)); d = ImageDraw.Draw(img)
    ph = h / 6
    for r in range(6):
        y0 = r * ph
        for y in range(int(y0), int(y0 + ph)):
            t = (y - y0) / ph; c = tuple(int(v + 18 * (1 - t)) for v in base)
            d.line([(6, y), (w - 6, y)], fill=c + (255,))
        d.line([(0, y0), (w, y0)], fill=(8, 10, 16, 255), width=6)
        d.polygon([(w * 0.08, y0 + ph * 0.25), (w * 0.3, y0 + ph * 0.25), (w * 0.24, y0 + ph * 0.75), (w * 0.08, y0 + ph * 0.75)], fill=(30, 36, 56, 255))
    d.rectangle([0, 0, 6, h], fill=(8, 10, 16, 255)); d.rectangle([w - 6, 0, w, h], fill=(8, 10, 16, 255))
    tex.gloss(img, 25); return img

def ceiling_tex(n=512):
    img = Image.new("RGBA", (n, n), (10, 11, 16, 255)); d = ImageDraw.Draw(img)
    r = n / 8
    for row in range(-1, 10):
        for col in range(-1, 8):
            cx = col * r * 1.5; cy = row * r * math.sqrt(3) + (r * math.sqrt(3) / 2 if col % 2 else 0)
            pts = [(cx + r * 0.95 * math.cos(math.radians(60 * k)), cy + r * 0.95 * math.sin(math.radians(60 * k))) for k in range(6)]
            d.polygon(pts, fill=(16, 18, 26, 255), outline=(28, 32, 46, 255))
    return img

def lounge_floor(n=512):
    img = Image.new("RGBA", (n, n), (18, 14, 30, 255)); d = ImageDraw.Draw(img)
    for k in range(0, n, 64): d.line([(k, 0), (k, n)], fill=(28, 22, 46, 255), width=2)
    noise = Image.effect_noise((n, n), 18).convert("L")
    return Image.composite(img, Image.new("RGBA", (n, n), (12, 10, 22, 255)), noise.point(lambda v: 255 if v > 40 else 180))

def door(w=512, h=1024):
    img = Image.new("RGBA", (w, h), (20, 40, 80, 255)); d = ImageDraw.Draw(img)
    for k in range(-h, w, 120): d.line([(k, h), (k + h * 0.5, 0)], fill=(110, 160, 240, 255), width=8)
    d.rectangle([0, 0, w - 1, h - 1], outline=(190, 200, 220, 255), width=24)
    d.line([(w / 2, 0), (w / 2, h)], fill=(190, 200, 220, 255), width=18)
    return img

def sign(text, w=1024, h=256, fg=(255, 255, 255), glow=(230, 30, 30), bg=(4, 6, 16)):
    img = Image.new("RGBA", (w, h), bg + (255,)); d = ImageDraw.Draw(img)
    d.rounded_rectangle([6, 6, w - 7, h - 7], 30, outline=(60, 140, 255, 255), width=8)
    sz = int(h * 0.55)
    while d.textlength(text, font=tex.font(sz)) > w * 0.86: sz -= 4
    tex.glow_text(img, (w / 2, h / 2), text, tex.font(sz), fg + (255,), glow + (255,), blur=10, stroke=3, stroke_fill=tuple(int(c * 0.6) for c in glow) + (255,))
    return img

def video_wall(w=2048, h=768):
    img = Image.new("RGBA", (w, h), (2, 4, 14, 255)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=(int(10 + 30 * t), int(20 + 40 * t), int(70 + 90 * t), 255))
    rays = Image.new("RGBA", (w, h), (0, 0, 0, 0)); rd = ImageDraw.Draw(rays)
    for i in range(36):
        a0 = 2 * math.pi * i / 36; a1 = a0 + math.pi / 36
        rd.polygon([(w / 2, h * 0.55), (w / 2 + math.cos(a0) * w, h * 0.55 + math.sin(a0) * w), (w / 2 + math.cos(a1) * w, h * 0.55 + math.sin(a1) * w)], fill=(255, 60, 60, 26) if i % 2 else (60, 140, 255, 26))
    img.alpha_composite(rays)
    tex.glow_text(img, (w / 2, h * 0.42), "DUSTIN'S ARCADE", tex.font(220), (255, 255, 255, 255), (230, 30, 30, 255), blur=18, stroke=5, stroke_fill=(150, 10, 10, 255))
    tex.glow_text(img, (w / 2, h * 0.72), "PLAY  *  WIN  *  REPEAT", tex.font(90, tex.COND), (255, 255, 255, 255), (60, 140, 255, 255), blur=10)
    d = ImageDraw.Draw(img)
    for x in range(0, w, 6): d.line([(x, 0), (x, h)], fill=(0, 0, 0, 255), width=1)
    for y in range(0, h, 6): d.line([(0, y), (w, y)], fill=(0, 0, 0, 255), width=1)
    return img

T = {'h_floor': floor_tex(), 'h_lfloor': lounge_floor(), 'h_wall': wall_tex(), 'h_lwall': wall_tex(base=(34, 18, 30)),
     'h_ceiling': ceiling_tex(), 'h_door': door(), 'h_black': tex.solid((8, 8, 12)), 'h_chrome': tex.chrome(),
     'h_neon_red': tex.solid((255, 50, 60), noise=2), 'h_neon_blue': tex.solid((60, 150, 255), noise=2), 'h_neon_white': tex.solid((235, 240, 255), noise=2),
     'h_video': video_wall(), 'h_sign_lounge': sign("PRIZE ROOM", glow=(60, 140, 255)), 'h_sign_office': sign("OFFICE", 512, 160), 'h_sign_power': sign("POWER ROOM", 768, 160, glow=(255, 200, 40)), 'h_office_wall': wall_tex(base=(40, 36, 32)),
     'h_sign_pinball': sign("PINBALL", 1024, 256, glow=(60, 140, 255)), 'h_sign_redeem': sign("REDEMPTION"),
     'h_sign_video': sign("VIDEO GAMES", glow=(60, 140, 255)), 'h_sign_exit': sign("EXIT", 512, 192, fg=(255, 230, 230), glow=(255, 40, 40), bg=(20, 4, 4)),
     'h_sign_welcome': sign("WELCOME TO DUSTIN'S ARCADE", 2048, 256)}
for k, im in T.items(): save(k, im)

m = Mesh()
def q(g, mat, a, b, c, d_, uv=((0, 0), (1, 0), (1, 1), (0, 1)), n=None): m.add(g, mat, quad(a, b, c, d_, uv=uv, n=n))

def plane_tiles(mat, x0, x1, z0, z1, y, up, step=4.0, g='static'):
    z = z0
    while z < z1 - 1e-6:
        z2 = min(z1, z + step); x = x0
        while x < x1 - 1e-6:
            x2 = min(x1, x + step); u1, v1 = (x2 - x) / step, (z2 - z) / step
            if up: q(g, mat, (x, y, z), (x, y, z2), (x2, y, z2), (x2, y, z), uv=((0, 0), (0, v1), (u1, v1), (u1, 0)), n=(0.0, 1.0, 0.0))
            else: q(g, mat, (x, y, z), (x2, y, z), (x2, y, z2), (x, y, z2), uv=((0, 0), (u1, 0), (u1, v1), (0, v1)), n=(0.0, -1.0, 0.0))
            x = x2
        z = z2

# ---------------- floor with glowing inlaid lanes ----------------
plane_tiles('h_floor', X0, X1, Z0, LOUNGE - 0.5, 1.0, True)
plane_tiles('h_lfloor', X0, X1, LOUNGE - 0.5, Z1, 1.0, True)
def lane(mat, x0, x1, z0, z1, y=1.006): q('glow', mat, (x0, y, z0), (x0, y, z1), (x1, y, z1), (x1, y, z0), n=(0.0, 1.0, 0.0))
for s in (-1, 1):
    lane('h_neon_blue', s * 3.4 - 0.06, s * 3.4 + 0.06, 0.5, LOUNGE - 0.6)
for z in (5.5, 12.5, 18.5, 30.5, 44.5):
    lane('h_neon_red' if z in (12.5, 30.5) else 'h_neon_blue', X0 + 1.2, X1 - 1.2, z - 0.05, z + 0.05)
def ring_floor(mat, cx, cz, r, w=0.08, seg=72, y=1.006):
    for i in range(seg):
        a0, a1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
        p = lambda a, rr: (cx + math.cos(a) * rr, y, cz + math.sin(a) * rr)
        q('glow', mat, p(a0, r - w), p(a0, r + w), p(a1, r + w), p(a1, r - w), n=(0.0, 1.0, 0.0))
ring_floor('h_neon_red', 0, 27, 4.2)
ring_floor('h_neon_blue', 0, 27, 4.6)

# ---------------- walls: gunmetal panels with light ribs ----------------
def wall_z(mat, x, za, zb, y0, y1, nx, step=4.0):
    z = za
    while z < zb - 1e-6:
        z2 = min(zb, z + step); u1 = (z2 - z) / step; v0 = 1 - (y0 - 1) / 12.0; v1 = 1 - (y1 - 1) / 12.0
        if nx > 0: q('static', mat, (x, y0, z2), (x, y0, z), (x, y1, z), (x, y1, z2), uv=((0, v0), (u1, v0), (u1, v1), (0, v1)), n=(1.0, 0.0, 0.0))
        else: q('static', mat, (x, y0, z), (x, y0, z2), (x, y1, z2), (x, y1, z), uv=((0, v0), (u1, v0), (u1, v1), (0, v1)), n=(-1.0, 0.0, 0.0))
        z = z2
def wall_x(mat, z, xa, xb, y0, y1, nz, step=4.0):
    x = xa
    while x < xb - 1e-6:
        x2 = min(xb, x + step); u1 = (x2 - x) / step; v0 = 1 - (y0 - 1) / 12.0; v1 = 1 - (y1 - 1) / 12.0
        if nz > 0: q('static', mat, (x, y0, z), (x2, y0, z), (x2, y1, z), (x, y1, z), uv=((0, v0), (u1, v0), (u1, v1), (0, v1)), n=(0.0, 0.0, 1.0))
        else: q('static', mat, (x2, y0, z), (x, y0, z), (x, y1, z), (x2, y1, z), uv=((0, v0), (u1, v0), (u1, v1), (0, v1)), n=(0.0, 0.0, -1.0))
        x = x2
wall_z('h_wall', X0, Z0, LOUNGE - 0.5, 1, CEIL, 1); wall_z('h_wall', X1, Z0, LOUNGE - 0.5, 1, CEIL, -1)
wall_z('h_lwall', X0, LOUNGE + 0.5, Z1, 1, CEIL, 1); wall_z('h_lwall', X1, LOUNGE + 0.5, Z1, 1, CEIL, -1)
wall_x('h_wall', Z0, X0, -2.5, 1, CEIL, 1); wall_x('h_wall', Z0, 2.5, X1, 1, CEIL, 1); wall_x('h_wall', Z0, -2.5, 2.5, 5, CEIL, 1)
wall_x('h_lwall', Z1, X0, X1, 1, CEIL, -1)
LZ0, LZ1 = LOUNGE - 0.5, LOUNGE + 0.5
for face_z, mat, nz in ((LZ0, 'h_wall', -1), (LZ1, 'h_lwall', 1)):
    wall_x(mat, face_z, X0, -ARCH, 1, CEIL, nz); wall_x(mat, face_z, ARCH, X1, 1, CEIL, nz); wall_x(mat, face_z, -ARCH, ARCH, 1 + ARCH_H, CEIL, nz)
for xs in (-ARCH, ARCH):
    q('static', 'h_chrome', (xs, 1, LZ0), (xs, 1, LZ1), (xs, 1 + ARCH_H, LZ1), (xs, 1 + ARCH_H, LZ0), n=(-1.0 if xs > 0 else 1.0, 0.0, 0.0))
q('static', 'h_chrome', (-ARCH, 1 + ARCH_H, LZ0), (-ARCH, 1 + ARCH_H, LZ1), (ARCH, 1 + ARCH_H, LZ1), (ARCH, 1 + ARCH_H, LZ0), n=(0.0, -1.0, 0.0))
for i, z in enumerate([zz + 0.5 for zz in range(2, LOUNGE - 2, 4)]):
    mat = 'h_neon_red' if i % 2 else 'h_neon_blue'
    for x, s in ((X0, 1), (X1, -1)):
        rounded_box(m, 'glow', mat, min(x, x + s * 0.06), 1.3, z - 0.05, max(x, x + s * 0.06), CEIL - 0.6, z + 0.05, r=0.01)
for x, s in ((X0, 1), (X1, -1)):
    rounded_box(m, 'glow', 'h_neon_blue', min(x, x + s * 0.05), 1.0, Z0, max(x, x + s * 0.05), 1.12, LOUNGE - 0.5, r=0.005)
    rounded_box(m, 'glow', 'h_neon_white', min(x, x + s * 0.05), 5.0, Z0, max(x, x + s * 0.05), 5.06, LOUNGE - 0.5, r=0.005)
for (xa, xb) in ((X0, -3.6), (3.6, X1)):
    rounded_box(m, 'glow', 'h_neon_blue', xa, 1.0, Z0, xb, 1.12, Z0 + 0.05, r=0.005)
# ---------------- ceiling: hex panels, cove light, floating halo rings ----------------
plane_tiles('h_ceiling', X0, X1, Z0, Z1, CEIL, False)
for x, s in ((X0, 1), (X1, -1)):
    rounded_box(m, 'glow', 'h_neon_blue', min(x, x + s * 0.4), CEIL - 0.25, Z0, max(x, x + s * 0.4), CEIL - 0.2, Z1, r=0.01)
rounded_box(m, 'glow', 'h_neon_blue', X0, CEIL - 0.25, Z0, X1, CEIL - 0.2, Z0 + 0.4, r=0.01)
HALOS = ((27, 9.5, 'h_neon_white', CEIL - 1.6), (27, 6.0, 'h_neon_red', CEIL - 2.2), (12, 5.0, 'h_neon_blue', CEIL - 1.8), (42, 5.0, 'h_neon_blue', CEIL - 1.8))
for (cz, R, mat, y) in HALOS:
    torus_y(m, 'glow', mat, 0, y, cz, R, 0.12, seg=96, tseg=8)
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        cylinder_y(m, 'static', 'h_chrome', math.cos(a) * R, cz + math.sin(a) * R, y + 0.1, CEIL, 0.02, seg=6)
for lz in range(4, DEPTH, 6):
    for lx in range(-HX + 4, HX - 2, 6):
        if any(math.hypot(lx, lz - cz) < R + 1.5 for cz, R, _, _ in HALOS): continue
        cylinder_y(m, 'glow', 'h_neon_white', lx, lz, CEIL - 0.06, CEIL - 0.02, 0.32, seg=20)
# ---------------- the doors: glowing portal arches ----------------
q('static', 'h_door', (2.5, 1, Z0 + 0.02), (-2.5, 1, Z0 + 0.02), (-2.5, 5, Z0 + 0.02), (2.5, 5, Z0 + 0.02), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, 1.0))
torus_z(m, 'glow', 'h_neon_blue', 0, 1.6, Z0 + 0.12, 3.9, 0.1, seg=64, tseg=8, a_from=-math.pi / 2 - 0.05, a_to=math.pi / 2 + 0.05)
torus_z(m, 'glow', 'h_neon_red', 0, 1.6, Z0 + 0.12, 4.3, 0.08, seg=64, tseg=8, a_from=-math.pi / 2 - 0.05, a_to=math.pi / 2 + 0.05)
q('glow', 'h_sign_exit', (-1.0, 5.3, Z0 + 0.03), (1.0, 5.3, Z0 + 0.03), (1.0, 6.0, Z0 + 0.03), (-1.0, 6.0, Z0 + 0.03), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, 1.0))
# ---------------- signs and the video wall ----------------
def hanging(mat, cx, cz, w, h, y=8.5):
    rounded_box(m, 'glow', 'h_black', cx - w / 2, y, cz - 0.08, cx + w / 2, y + h, cz + 0.08, r=0.03, front_mat=mat)
    q('glow', mat, (cx - w / 2 + 0.02, y + 0.02, cz + 0.085), (cx + w / 2 - 0.02, y + 0.02, cz + 0.085), (cx + w / 2 - 0.02, y + h - 0.02, cz + 0.085), (cx - w / 2 + 0.02, y + h - 0.02, cz + 0.085),
      uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, 1.0))                     # readable from behind too
    for sx in (-w / 2 + 0.3, w / 2 - 0.3):
        cylinder_y(m, 'static', 'h_chrome', cx + sx, cz, y + h, CEIL, 0.025, seg=8)
hanging('h_sign_welcome', 0, 4, 12, 1.4, y=7.0)
hanging('h_sign_video', L(-18), 9, 6, 1.5)
hanging('h_sign_pinball', L(-18), 32, 6, 1.5)
hanging('h_sign_redeem', L(18), 30, 6, 1.5)
q('glow', 'h_video', (11, 6.4, LZ0 - 0.03), (-11, 6.4, LZ0 - 0.03), (-11, 12.4, LZ0 - 0.03), (11, 12.4, LZ0 - 0.03), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, -1.0))
rounded_box(m, 'static', 'h_black', -11.2, 6.2, LZ0 - 0.12, 11.2, 6.4, LZ0, r=0.02)
rounded_box(m, 'glow', 'h_neon_red', -11.2, 12.4, LZ0 - 0.1, 11.2, 12.5, LZ0, r=0.02)
q('glow', 'h_sign_lounge', (-3.3, 6.3, LZ1 + 0.02), (3.3, 6.3, LZ1 + 0.02), (3.3, 7.8, LZ1 + 0.02), (-3.3, 7.8, LZ1 + 0.02), uv=((0, 1), (1, 1), (1, 0), (0, 0)), n=(0.0, 0.0, 1.0))
# ---------------- the back rooms: prize room (left), office and power room (right) ----------------
PX = -13.0                                                          # partition at layout lx 13 (model x -13)
def part_wall_z(x, z0, z1, gaps, nx, mat):
    z = z0
    for g0, g1 in gaps + [(z1, z1)]:
        if g0 > z: wall_z(mat, x, z, g0, 1, CEIL, nx)
        if g1 > g0: wall_z(mat, x, g0, g1, 4, CEIL, nx)
        z = max(z, g1)
doors = [(53.5, 54.5), (59.5, 60.5)]
part_wall_z(PX + 0.5, LZ1, Z1, doors, 1, 'h_lwall')                  # prize room side
part_wall_z(PX - 0.5, LZ1, Z1, doors, -1, 'h_office_wall')           # office / power side
for g0, g1 in doors:
    for zz in (g0, g1): q('static', 'h_chrome', (PX - 0.5, 1, zz), (PX + 0.5, 1, zz), (PX + 0.5, 4, zz), (PX - 0.5, 4, zz), n=(0.0, 0.0, 1.0 if zz == g0 else -1.0))
    q('static', 'h_chrome', (PX - 0.5, 4, g0), (PX + 0.5, 4, g0), (PX + 0.5, 4, g1), (PX - 0.5, 4, g1), n=(0.0, -1.0, 0.0))
wall_x('h_office_wall', 57.5, X0, PX - 0.5, 1, CEIL, -1)                # between office and power room
wall_x('h_office_wall', 58.5, X0, PX - 0.5, 1, CEIL, 1)
q('glow', 'h_sign_office', (PX + 0.52, 4.3, 53.0), (PX + 0.52, 4.3, 55.0), (PX + 0.52, 4.9, 55.0), (PX + 0.52, 4.9, 53.0), uv=((1, 1), (0, 1), (0, 0), (1, 0)), n=(1.0, 0.0, 0.0))
q('glow', 'h_sign_power', (PX + 0.52, 4.3, 58.8), (PX + 0.52, 4.3, 61.2), (PX + 0.52, 4.9, 61.2), (PX + 0.52, 4.9, 58.8), uv=((1, 1), (0, 1), (0, 0), (1, 0)), n=(1.0, 0.0, 0.0))
for x in (PX + 0.52,): rounded_box(m, 'glow', 'h_neon_red', x, 9.0, LZ1, x + 0.04, 9.1, Z1, r=0.005)
m.write(f'{OUT}/models/grand_hall.obj')
print('ok')
