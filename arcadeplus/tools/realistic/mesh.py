"""Tiny mesh kit: smooth primitives with UVs and normals, written to OBJ with named groups + materials.
Units are blocks. Front of a machine is -z; the player's right is -x (so front-facing UVs run with -x)."""
import math, numpy as np

class Mesh:
    def __init__(s): s.groups = {}      # name -> {mat: [tri,...]}  tri = 3 x (pos, uv, nrm)
    def add(s, group, mat, tris):
        s.groups.setdefault(group, {}).setdefault(mat, []).extend(tris)
    def write(s, path):
        V, T, N, out = [], [], [], []
        vi = {}
        def idx(lst, key, val):
            k = tuple(round(x, 5) for x in val)
            d = vi.setdefault(key, {})
            if k not in d:
                lst.append(k); d[k] = len(lst)
            return d[k]
        body = []
        for g, mats in s.groups.items():
            body.append(f"o {g}")
            for m, tris in mats.items():
                body.append(f"usemtl {m}")
                for tri in tris:
                    f = []
                    for p, uv, n in tri:
                        a = idx(V, 'v', p); b = idx(T, 't', uv); c = idx(N, 'n', n)
                        f.append(f"{a}/{b}/{c}")
                    body.append("f " + " ".join(f))
        with open(path, "w") as fh:
            for v in V: fh.write("v %.5f %.5f %.5f\n" % v)
            for t in T: fh.write("vt %.5f %.5f\n" % (t[0], 1 - t[1]))   # OBJ: v runs up
            for n in N: fh.write("vn %.4f %.4f %.4f\n" % n)
            fh.write("\n".join(body) + "\n")

def nrm(v):
    v = np.asarray(v, float); l = np.linalg.norm(v)
    return tuple(v / l) if l > 1e-9 else (0.0, 1.0, 0.0)

def quad(p0, p1, p2, p3, uv=((0,0),(1,0),(1,1),(0,1)), n=None):
    if n is None:
        n = nrm(np.cross(np.subtract(p1, p0), np.subtract(p3, p0)))
    ns = [n]*4 if isinstance(n[0], float) else n
    return [((p0,uv[0],ns[0]),(p1,uv[1],ns[1]),(p2,uv[2],ns[2])), ((p0,uv[0],ns[0]),(p2,uv[2],ns[2]),(p3,uv[3],ns[3]))]

def front_uv(x, y, x0, x1, y0, y1):
    """UV for a face seen from the front (-z): image left = +x side (player's left)."""
    return ((x1 - x) / (x1 - x0) if False else (x - x1) / (x0 - x1), 1 - (y - y0) / (y1 - y0))

def rounded_box(m, g, mat, x0, y0, z0, x1, y1, z1, r=0.03, seg=4, front_mat=None, side_mat=None, top_mat=None):
    """Box with rounded vertical edges (rounded-rect footprint) and a bevelled top edge. Smooth normals."""
    # footprint outline (counter-clockwise seen from above)
    pts = []
    corners = [(x1 - r, z1 - r, 0), (x0 + r, z1 - r, 90), (x0 + r, z0 + r, 180), (x1 - r, z0 + r, 270)]
    for cx, cz, a0 in corners:
        for k in range(seg + 1):
            a = math.radians(a0 + 90 * k / seg)
            pts.append((cx + r * math.cos(a), cz + r * math.sin(a), (math.cos(a), math.sin(a))))
    n = len(pts)
    # perimeter length for side UVs
    per = [0.0]
    for i in range(1, n + 1):
        a, b = pts[i - 1], pts[i % n]
        per.append(per[-1] + math.hypot(b[0] - a[0], b[1] - a[1]))
    L = per[-1]
    yb = y1 - r
    for i in range(n):
        a, b = pts[i], pts[(i + 1) % n]
        na, nb = (a[2][0], 0.0, a[2][1]), (b[2][0], 0.0, b[2][1])
        mid = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2)
        face_front = abs(a[1] - z0) < 1e-6 and abs(b[1] - z0) < 1e-6
        mm = front_mat if (front_mat and face_front) else (side_mat or mat)
        if face_front and front_mat:
            ua, ub = front_uv(a[0], 0, x0, x1, 0, 1)[0], front_uv(b[0], 0, x0, x1, 0, 1)[0]
            m.add(g, mm, quad((a[0], y0, a[1]), (b[0], y0, b[1]), (b[0], yb, b[1]), (a[0], yb, a[1]),
                              uv=((ua, 1), (ub, 1), (ub, (y1 - yb) / (y1 - y0)), (ua, (y1 - yb) / (y1 - y0))), n=[na, nb, nb, na]))
        else:
            u0, u1 = per[i] / L * 4, per[i + 1] / L * 4
            m.add(g, mm, quad((a[0], y0, a[1]), (b[0], y0, b[1]), (b[0], yb, b[1]), (a[0], yb, a[1]),
                              uv=((u0, 1), (u1, 1), (u1, 0), (u0, 0)), n=[na, nb, nb, na]))
        # bevel ring up to the top
        for k in range(seg):
            t0, t1 = math.pi / 2 * k / seg, math.pi / 2 * (k + 1) / seg
            def ring(pp, t):
                s = r * (1 - math.cos(t))
                cx = mid
                # move inward along the outward normal
                return (pp[0] - pp[2][0] * s, yb + r * math.sin(t), pp[1] - pp[2][1] * s)
            def rn(pp, t):
                return nrm((pp[2][0] * math.cos(t), math.sin(t), pp[2][1] * math.cos(t)))
            m.add(g, top_mat or mat, quad(ring(a, t0), ring(b, t0), ring(b, t1), ring(a, t1), n=[rn(a, t0), rn(b, t0), rn(b, t1), rn(a, t1)]))
    # top cap (fan) and bottom
    cxm, czm = (x0 + x1) / 2, (z0 + z1) / 2
    for i in range(n):
        a, b = pts[i], pts[(i + 1) % n]
        ia = (a[0] - a[2][0] * r, y1, a[1] - a[2][1] * r); ib = (b[0] - b[2][0] * r, y1, b[1] - b[2][1] * r)
        uv = lambda p: ((p[0] - x0) / (x1 - x0), (p[2] - z0) / (z1 - z0))
        c = (cxm, y1, czm)
        m.add(g, top_mat or mat, [((c, uv(c), (0, 1, 0)), (ib, uv(ib), (0, 1, 0)), (ia, uv(ia), (0, 1, 0)))])
        m.add(g, mat, [(((cxm, y0, czm), (0.5, 0.5), (0, -1, 0)), ((a[0], y0, a[1]), (0, 0), (0, -1, 0)), ((b[0], y0, b[1]), (1, 0), (0, -1, 0)))])

def cylinder_z(m, g, mat, cx, cy, z0, z1, r, seg=48, cap_mat=None, uv_face=None, side_mat=None, cap_back=True):
    """Disc/cylinder with its axis along z (faces the player). Front cap at z0 gets planar UVs (mirrored for the player)."""
    for i in range(seg):
        a0, a1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
        p = lambda a, z: (cx - math.sin(a) * r, cy + math.cos(a) * r, z)
        nn = lambda a: (-math.sin(a), math.cos(a), 0.0)
        m.add(g, side_mat or mat, quad(p(a0, z0), p(a1, z0), p(a1, z1), p(a0, z1), uv=((i / seg, 0), ((i + 1) / seg, 0), ((i + 1) / seg, 1), (i / seg, 1)), n=[nn(a0), nn(a1), nn(a1), nn(a0)]))
        fuv = uv_face or (lambda x, y: (0.5 - (x - cx) / (2 * r), 0.5 - (y - cy) / (2 * r)))
        c0, pa, pb = (cx, cy, z0), p(a0, z0), p(a1, z0)
        m.add(g, cap_mat or mat, [((c0, fuv(cx, cy), (0, 0, -1)), (pa, fuv(pa[0], pa[1]), (0, 0, -1)), (pb, fuv(pb[0], pb[1]), (0, 0, -1)))])
        if cap_back:
            c1, qa, qb = (cx, cy, z1), p(a0, z1), p(a1, z1)
            m.add(g, mat, [((c1, (0.5, 0.5), (0, 0, 1)), (qb, (0, 0), (0, 0, 1)), (qa, (1, 0), (0, 0, 1)))])

def cylinder_y(m, g, mat, cx, cz, y0, y1, r, seg=32, top_mat=None, r_top=None):
    rt = r if r_top is None else r_top
    for i in range(seg):
        a0, a1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
        pb = lambda a, rr, y: (cx + math.cos(a) * rr, y, cz + math.sin(a) * rr)
        nn = lambda a: nrm((math.cos(a), (r - rt) / max(1e-6, y1 - y0), math.sin(a)))
        m.add(g, mat, quad(pb(a0, r, y0), pb(a1, r, y0), pb(a1, rt, y1), pb(a0, rt, y1), uv=((i / seg, 1), ((i + 1) / seg, 1), ((i + 1) / seg, 0), (i / seg, 0)), n=[nn(a0), nn(a1), nn(a1), nn(a0)]))
        c = (cx, y1, cz)
        m.add(g, top_mat or mat, [((c, (0.5, 0.5), (0, 1, 0)), (pb(a1, rt, y1), (0.5 + 0.5 * math.cos(a1), 0.5 + 0.5 * math.sin(a1)), (0, 1, 0)), (pb(a0, rt, y1), (0.5 + 0.5 * math.cos(a0), 0.5 + 0.5 * math.sin(a0)), (0, 1, 0)))])

def torus_z(m, g, mat, cx, cy, cz, R, r, seg=64, tseg=10, a_from=0.0, a_to=2 * math.pi):
    """Ring tube around the z axis (a chrome rim). Angle convention matches the wheel: 0 = top, mirrored x."""
    for i in range(seg):
        a0 = a_from + (a_to - a_from) * i / seg; a1 = a_from + (a_to - a_from) * (i + 1) / seg
        for j in range(tseg):
            b0, b1 = 2 * math.pi * j / tseg, 2 * math.pi * (j + 1) / tseg
            def P(a, b):
                d = (-math.sin(a), math.cos(a))
                rr = R + r * math.cos(b)
                return (cx + d[0] * rr, cy + d[1] * rr, cz + r * math.sin(b))
            def Nn(a, b):
                d = (-math.sin(a), math.cos(a))
                return nrm((d[0] * math.cos(b), d[1] * math.cos(b), math.sin(b)))
            m.add(g, mat, quad(P(a0, b0), P(a1, b0), P(a1, b1), P(a0, b1), uv=((i / seg, j / tseg), ((i + 1) / seg, j / tseg), ((i + 1) / seg, (j + 1) / tseg), (i / seg, (j + 1) / tseg)), n=[Nn(a0, b0), Nn(a1, b0), Nn(a1, b1), Nn(a0, b1)]))

def sphere(m, g, mat, cx, cy, cz, r, la=10, lo=16, squash=1.0):
    for i in range(la):
        f0, f1 = -math.pi / 2 + math.pi * i / la, -math.pi / 2 + math.pi * (i + 1) / la
        for k in range(lo):
            a0, a1 = 2 * math.pi * k / lo, 2 * math.pi * (k + 1) / lo
            P = lambda f, a: (cx + math.cos(f) * math.cos(a) * r, cy + math.sin(f) * r * squash, cz + math.cos(f) * math.sin(a) * r)
            Nn = lambda f, a: nrm((math.cos(f) * math.cos(a), math.sin(f) / max(squash, 1e-3), math.cos(f) * math.sin(a)))
            m.add(g, mat, quad(P(f0, a0), P(f0, a1), P(f1, a1), P(f1, a0), uv=((k / lo, i / la), ((k + 1) / lo, i / la), ((k + 1) / lo, (i + 1) / la), (k / lo, (i + 1) / la)), n=[Nn(f0, a0), Nn(f0, a1), Nn(f1, a1), Nn(f1, a0)]))

def cylinder_x(m, g, mat, cy, cz, x0, x1, r, seg=60, side_mat=None, cap_mat=None):
    """Drum with its axis along x. Side UV: u across (player's left = 0), v down the face (th grows upward on the front)."""
    for i in range(seg):
        t0, t1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
        P = lambda x, t: (x, cy + math.sin(t) * r, cz - math.cos(t) * r)
        Nn = lambda t: (0.0, math.sin(t), -math.cos(t))
        v0, v1 = 1 - t0 / (2 * math.pi), 1 - t1 / (2 * math.pi)
        m.add(g, side_mat or mat, quad(P(x1, t0), P(x0, t0), P(x0, t1), P(x1, t1), uv=((0, v0), (1, v0), (1, v1), (0, v1)), n=[Nn(t0), Nn(t0), Nn(t1), Nn(t1)]))
        for x, sgn in ((x0, -1), (x1, 1)):
            c = (x, cy, cz)
            uvf = lambda t: (0.5 + 0.5 * math.cos(t), 0.5 + 0.5 * math.sin(t))
            m.add(g, cap_mat or mat, [((c, (0.5, 0.5), (sgn, 0, 0)), (P(x, t0), uvf(t0), (sgn, 0, 0)), (P(x, t1), uvf(t1), (sgn, 0, 0)))])

def torus_y(m, g, mat, cx, cy, cz, R, r, seg=64, tseg=10):
    """Horizontal ring (around the y axis)."""
    for i in range(seg):
        a0, a1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
        for j in range(tseg):
            b0, b1 = 2 * math.pi * j / tseg, 2 * math.pi * (j + 1) / tseg
            P = lambda a, b: (cx + math.cos(a) * (R + r * math.cos(b)), cy + r * math.sin(b), cz + math.sin(a) * (R + r * math.cos(b)))
            Nn = lambda a, b: nrm((math.cos(a) * math.cos(b), math.sin(b), math.sin(a) * math.cos(b)))
            m.add(g, mat, quad(P(a0, b0), P(a1, b0), P(a1, b1), P(a0, b1), n=[Nn(a0, b0), Nn(a1, b0), Nn(a1, b1), Nn(a0, b1)]))

def arch_x(m, g, mat, x, cy, cz, R, r, t_from, t_to, seg=32, tseg=8):
    """Tube bent in the y/z plane (a hood rib over a drum). t = 0 is toward the player, growing upward."""
    for i in range(seg):
        a0 = t_from + (t_to - t_from) * i / seg; a1 = t_from + (t_to - t_from) * (i + 1) / seg
        for j in range(tseg):
            b0, b1 = 2 * math.pi * j / tseg, 2 * math.pi * (j + 1) / tseg
            def P(a, b):
                d = (math.sin(a), -math.cos(a)); rr = R + r * math.cos(b)
                return (x + r * math.sin(b), cy + d[0] * rr, cz + d[1] * rr)
            def Nn(a, b):
                d = (math.sin(a), -math.cos(a))
                return nrm((math.sin(b), d[0] * math.cos(b), d[1] * math.cos(b)))
            m.add(g, mat, quad(P(a0, b0), P(a1, b0), P(a1, b1), P(a0, b1), n=[Nn(a0, b0), Nn(a1, b0), Nn(a1, b1), Nn(a0, b1)]))

def cylinder_y_wrap(m, g, mat, cx, cz, y0, y1, r, seg=48):
    """Upright cylinder whose side texture wraps once around; the front (toward -z) is the middle of the image."""
    for i in range(seg):
        a0, a1 = 2 * math.pi * i / seg, 2 * math.pi * (i + 1) / seg
        # angle 0 = front (-z); u grows toward the player's right (-x)
        P = lambda a, y: (cx - math.sin(a) * r, y, cz - math.cos(a) * r)
        Nn = lambda a: (-math.sin(a), 0.0, -math.cos(a))
        u0, u1 = 0.5 + a0 / (2 * math.pi), 0.5 + a1 / (2 * math.pi)
        m.add(g, mat, quad(P(a0, y0), P(a1, y0), P(a1, y1), P(a0, y1), uv=((u0, 1), (u1, 1), (u1, 0), (u0, 0)), n=[Nn(a0), Nn(a1), Nn(a1), Nn(a0)]))
