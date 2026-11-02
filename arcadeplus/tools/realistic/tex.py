import math, random
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageChops
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
COND = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
def font(sz, path=BOLD): return ImageFont.truetype(path, sz)

def glow_text(img, xy, text, f, fill, glow, blur=10, anchor="mm", stroke=0, stroke_fill=None):
    lay = Image.new("RGBA", img.size, (0,0,0,0)); d = ImageDraw.Draw(lay)
    d.text(xy, text, font=f, fill=glow, anchor=anchor, stroke_width=stroke+6, stroke_fill=glow)
    lay = lay.filter(ImageFilter.GaussianBlur(blur))
    img.alpha_composite(lay)
    d = ImageDraw.Draw(img)
    d.text(xy, text, font=f, fill=fill, anchor=anchor, stroke_width=stroke, stroke_fill=stroke_fill or fill)

def gloss(img, strength=60):
    """Soft diagonal highlight, like light on glossy plastic."""
    w, h = img.size; lay = Image.new("RGBA", (w, h), (0,0,0,0)); d = ImageDraw.Draw(lay)
    d.polygon([(0, h*0.05), (w*0.45, 0), (w*0.75, 0), (0, h*0.55)], fill=(255,255,255,strength))
    img.alpha_composite(lay.filter(ImageFilter.GaussianBlur(w/20)))

def black_gloss(w=256, h=256):
    img = Image.new("RGBA", (w, h)); px = img.load()
    for y in range(h):
        for x in range(w):
            v = 14 + int(10 * math.sin(x / w * math.pi)) + random.randint(-2, 2)
            px[x, y] = (v, v, v + 4, 255)
    gloss(img, 35); return img

def chrome(w=256, h=256):
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    stops = [(0, 235), (0.18, 150), (0.35, 245), (0.5, 120), (0.62, 200), (0.8, 95), (1, 220)]
    for y in range(h):
        t = y / (h - 1)
        for i in range(len(stops) - 1):
            if stops[i][0] <= t <= stops[i+1][0]:
                k = (t - stops[i][0]) / (stops[i+1][0] - stops[i][0]); v = int(stops[i][1] + (stops[i+1][1] - stops[i][1]) * k); break
        d.line([(0, y), (w, y)], fill=(v, v, min(255, v + 8), 255))
    return img

def solid(c, w=64, h=64, noise=4):
    img = Image.new("RGBA", (w, h)); px = img.load()
    for y in range(h):
        for x in range(w):
            n = random.randint(-noise, noise); px[x, y] = tuple(max(0, min(255, v + n)) for v in c) + (255,)
    return img

def neon_art(w, h, title, sub=None):
    """Front cabinet art: deep purple to magenta, neon sweeps, starbursts, big glowing title."""
    img = Image.new("RGBA", (w, h)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h
        d.line([(0, y), (w, y)], fill=(int(8 + 14 * t), int(16 + 30 * t), int(60 + 70 * t), 255))
    rays = Image.new("RGBA", (w, h), (0,0,0,0)); rd = ImageDraw.Draw(rays)
    cx, cy = w * 0.5, h * 0.62
    for i in range(24):
        a0 = 2 * math.pi * i / 24; a1 = a0 + math.pi / 24
        rd.polygon([(cx, cy), (cx + math.cos(a0) * w, cy + math.sin(a0) * w), (cx + math.cos(a1) * w, cy + math.sin(a1) * w)], fill=(120, 170, 255, 30))
    img.alpha_composite(rays)
    for k, col in enumerate([(70, 150, 255), (235, 40, 40), (245, 245, 255)]):
        sw = Image.new("RGBA", (w, h), (0,0,0,0)); sd = ImageDraw.Draw(sw)
        y0 = h * (0.78 + 0.07 * k)
        sd.arc([-w * 0.3, y0 - h * 0.5, w * 1.3, y0 + h * 0.5], 200, 340, fill=col + (255,), width=max(3, h // 70))
        img.alpha_composite(sw.filter(ImageFilter.GaussianBlur(3))); img.alpha_composite(sw)
    for _ in range(40):
        x, y, r = random.random() * w, random.random() * h * 0.6, random.random() * 3 + 1
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255, 200))
    sz = int(h * 0.26)
    while sz > 10 and ImageDraw.Draw(img).textlength(title, font=font(sz)) > w * 0.86: sz -= 4
    glow_text(img, (w / 2, h * 0.36), title, font(sz), (255, 255, 255, 255), (230, 30, 30, 255), blur=h // 40, stroke=4, stroke_fill=(150, 10, 10, 255))
    if sub:
        glow_text(img, (w / 2, h * 0.6), sub, font(int(h * 0.09), COND), (255, 255, 255, 255), (60, 140, 255, 255), blur=6)
    gloss(img, 40)
    return img

def side_art(w=512, h=512):
    img = Image.new("RGBA", (w, h), (8, 14, 40, 255)); d = ImageDraw.Draw(img)
    for k, col in enumerate([(235, 40, 40), (245, 245, 255), (60, 140, 255)]):
        sw = Image.new("RGBA", (w, h), (0,0,0,0)); sd = ImageDraw.Draw(sw)
        sd.line([(0, h * (0.3 + 0.18 * k)), (w, h * (0.1 + 0.18 * k))], fill=col + (255,), width=10)
        img.alpha_composite(sw.filter(ImageFilter.GaussianBlur(8))); img.alpha_composite(sw)
    gloss(img, 30); return img

def badge(w=512, h=128):
    img = Image.new("RGBA", (w, h), (10, 20, 60, 255))
    glow_text(img, (w / 2, h / 2), "DUSTIN'S ARCADE", font(int(h * 0.42), COND), (255, 255, 255, 255), (230, 30, 30, 255), blur=6)
    ImageDraw.Draw(img).rectangle([0, 0, w - 1, h - 1], outline=(230, 30, 30, 255), width=6)
    return img

def wheel_face(values, colors, n=1024):
    """The printed face of the wheel. Segment i runs clockwise from the top (as the player sees it)."""
    S = n * 2; img = Image.new("RGBA", (S, S), (0,0,0,0)); d = ImageDraw.Draw(img)
    c = S / 2; R = S / 2 - 2; seg = 360 / len(values)
    for i, v in enumerate(values):
        a0 = -90 + i * seg; col = colors(i, v)
        d.pieslice([c - R, c - R, c + R, c + R], a0, a0 + seg, fill=col)
        # inner shading toward the hub
    shade = Image.new("RGBA", (S, S), (0,0,0,0)); sd = ImageDraw.Draw(shade)
    for k in range(60):
        r = R * (0.25 + 0.75 * k / 60); a = int(70 * (1 - k / 60))
        sd.ellipse([c - r, c - r, c + r, c + r], outline=(0, 0, 0, a), width=int(R / 60) + 2)
    img.alpha_composite(shade)
    for i in range(len(values)):
        a = math.radians(-90 + i * seg)
        d.line([(c, c), (c + math.cos(a) * R, c + math.sin(a) * R)], fill=(245, 245, 245, 255), width=S // 160)
    for i, v in enumerate(values):
        am = math.radians(i * seg + seg / 2)                       # clockwise from top
        txt = str(v); big = v >= 100
        f = font(int(S * (0.068 if len(txt) < 2 else 0.056 if len(txt) < 3 else 0.042)))
        lay = Image.new("RGBA", (int(S * 0.36), int(S * 0.12)), (0,0,0,0)); ld = ImageDraw.Draw(lay)
        fill = (30, 20, 10, 255) if colors(i, v)[0] > 200 and colors(i, v)[1] > 150 else (255, 255, 255, 255)
        ld.text((lay.width / 2, lay.height / 2), txt, font=f, fill=fill, anchor="mm", stroke_width=S // 300, stroke_fill=(0, 0, 0, 160))
        lay = lay.rotate(-math.degrees(am), expand=True, resample=Image.BICUBIC)
        rr = R * 0.8
        px, py = c + math.sin(am) * rr, c - math.cos(am) * rr
        img.alpha_composite(lay, (int(px - lay.width / 2), int(py - lay.height / 2)))
    d.ellipse([c - R * 0.22, c - R * 0.22, c + R * 0.22, c + R * 0.22], fill=(20, 18, 28, 255), outline=(216, 168, 48, 255), width=S // 120)
    glow_text(img, (c, c), "D", font(int(S * 0.14)), (255, 255, 255, 255), (230, 30, 30, 255), blur=S // 200)
    gloss(img, 45)
    return img.resize((n, n), Image.LANCZOS)

def backboard(n=1024):
    img = Image.new("RGBA", (n, n), (8, 16, 52, 255)); d = ImageDraw.Draw(img); c = n / 2
    for i in range(36):
        a0 = 2 * math.pi * i / 36; a1 = a0 + math.pi / 36
        d.polygon([(c, c), (c + math.cos(a0) * n, c + math.sin(a0) * n), (c + math.cos(a1) * n, c + math.sin(a1) * n)], fill=(20, 40, 110, 255))
    gloss(img, 30); return img

def marquee(text, w=1024, h=320):
    img = Image.new("RGBA", (w, h), (8, 16, 60, 255)); d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / h; d.line([(0, y), (w, y)], fill=(10, int(20 + 20 * (1 - abs(t - .5) * 2)), int(70 + 60 * (1 - abs(t - .5) * 2)), 255))
    sz = int(h * 0.62)
    while ImageDraw.Draw(img).textlength(text, font=font(sz)) > w * 0.9: sz -= 4
    glow_text(img, (w / 2, h * 0.5), text, font(sz), (255, 255, 255, 255), (230, 30, 30, 255), blur=12, stroke=3, stroke_fill=(160, 10, 10, 255))
    d.rectangle([0, 0, w - 1, h - 1], outline=(230, 30, 30, 255), width=10)
    return img

def drum_strip(values, colors, w=512, segh=128):
    """Printed surface of a number drum: one band per segment, top of the image = segment 0 at the front."""
    n = len(values); img = Image.new("RGBA", (w, segh * n)); d = ImageDraw.Draw(img)
    for i, v in enumerate(values):
        # segment i sits at th in [i*seg,(i+1)*seg]; v = 1 - th/2pi so segment i is band (n-1-i) from the top
        y0 = (n - 1 - i) * segh; col = colors(i, v)
        for y in range(segh):
            k = 0.8 + 0.2 * math.sin(y / segh * math.pi)
            d.line([(0, y0 + y), (w, y0 + y)], fill=tuple(int(c * k) for c in col[:3]) + (255,))
        d.rectangle([0, y0, w, y0 + 5], fill=(20, 20, 24, 255))
        txt = str(v); dark = col[0] > 200 and col[1] > 150
        sz = int(segh * 0.72)
        # the band is seen upside-down relative to image rows (th grows upward), so flip the text
        lay = Image.new("RGBA", (w, segh), (0, 0, 0, 0)); ld = ImageDraw.Draw(lay)
        ld.text((w / 2, segh / 2), txt, font=font(sz), fill=(30, 20, 10, 255) if dark else (255, 255, 255, 255), anchor="mm", stroke_width=3, stroke_fill=(0, 0, 0, 140))
        img.alpha_composite(lay, (0, y0))
    gloss(img, 25)
    return img

def lamp_ring_top(n=1024):
    img = Image.new("RGBA", (n, n), (8, 14, 40, 255)); d = ImageDraw.Draw(img); c = n / 2
    for k in range(10):
        r = c * (0.95 - k * 0.07); d.ellipse([c - r, c - r, c + r, c + r], outline=(40, 70, 150, 255), width=3)
    gloss(img, 25); return img

def wrap_art(w, h, title, sub):
    img = neon_art(w, h, title, sub); return img
