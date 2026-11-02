"""Arcade Attendant entity (GeckoLib) + Attendant Badge item. Run: python tools/modelgen/attendant.py"""
import json, os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")
A = os.path.join(ROOT, "arcadeplus", "src", "main", "resources", "assets", "arcadeplus")


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1)


colours = {"skin": (226, 178, 140), "cap": (200, 30, 40), "shirt": (40, 80, 190), "vest": (190, 30, 40),
           "pants": (35, 35, 55), "shoe": (20, 20, 22), "eye": (20, 30, 60), "gold": (240, 200, 60), "hair": (90, 55, 30)}
pos, img = {}, Image.new("RGBA", (64, 64), (0, 0, 0, 0))
for i, (k, c) in enumerate(colours.items()):
    x, y = (i % 8) * 8, (i // 8) * 8
    pos[k] = (x, y)
    for px in range(x, x + 8):
        for py in range(y, y + 8):
            img.putpixel((px, py), c + (255,))


def box(x0, y0, z0, x1, y1, z1, col):
    u, v = pos[col]
    face = {"uv": [u + 1, v + 1], "uv_size": [4, 4]}
    return {"origin": [x0, y0, z0], "size": [x1 - x0, y1 - y0, z1 - z0],
            "uv": {f: dict(face) for f in ("north", "south", "east", "west", "up", "down")}}


def bone(name, parent, pivot, cubes):
    b = {"name": name, "pivot": pivot, "cubes": cubes}
    if parent:
        b["parent"] = parent
    return b


bones = [
    bone("root", None, [0, 0, 0], []),
    bone("body", "root", [0, 24, 0], [box(-4, 12, -2, 4, 24, 2, "shirt"), box(-4.2, 14, -2.2, 4.2, 23, -1.9, "vest"),
                                      box(1, 20, -2.4, 3, 21.5, -2.2, "gold")]),
    bone("head", "body", [0, 24, 0], [box(-4, 24, -4, 4, 32, 4, "skin"), box(-4.2, 30, -4.2, 4.2, 32.6, 4.2, "cap"),
                                      box(-4.2, 30, -7, 4.2, 30.6, -4.2, "cap"), box(-4.1, 27, 3.9, 4.1, 30, 4.1, "hair"),
                                      box(-2.5, 27, -4.1, -1.5, 28, -4.0, "eye"), box(1.5, 27, -4.1, 2.5, 28, -4.0, "eye")]),
    bone("arm_l", "body", [6, 22, 0], [box(4, 12, -2, 8, 24, 2, "shirt"), box(4, 12, -2, 8, 14, 2, "skin")]),
    bone("arm_r", "body", [-6, 22, 0], [box(-8, 12, -2, -4, 24, 2, "shirt"), box(-8, 12, -2, -4, 14, 2, "skin")]),
    bone("leg_l", "root", [2, 12, 0], [box(0, 0, -2, 4, 12, 2, "pants"), box(0, 0, -2.4, 4, 2, 2, "shoe")]),
    bone("leg_r", "root", [-2, 12, 0], [box(-4, 0, -2, 0, 12, 2, "pants"), box(-4, 0, -2.4, 0, 2, 2, "shoe")]),
]
w(f"{A}/geckolib/models/entity/attendant.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.attendant", "texture_width": 64, "texture_height": 64,
                    "visible_bounds_width": 2, "visible_bounds_height": 3, "visible_bounds_offset": [0, 1.5, 0]},
    "bones": bones}]})
os.makedirs(f"{A}/textures/entity", exist_ok=True)
img.save(f"{A}/textures/entity/attendant.png")


def rot(frames):
    return {"rotation": {str(t): {"vector": v} for t, v in frames}}


w(f"{A}/geckolib/animations/entity/attendant.animation.json", {"format_version": "1.8.0", "animations": {
    "idle": {"loop": True, "animation_length": 3.0, "bones": {
        "arm_l": rot([(0.0, [0, 0, -2]), (1.5, [0, 0, -5]), (3.0, [0, 0, -2])]),
        "arm_r": rot([(0.0, [0, 0, 2]), (1.5, [0, 0, 5]), (3.0, [0, 0, 2])]),
        "head": rot([(0.0, [0, 0, 0]), (1.5, [3, 0, 0]), (3.0, [0, 0, 0])])}},
    "walk": {"loop": True, "animation_length": 1.0, "bones": {
        "leg_l": rot([(0.0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1.0, [30, 0, 0])]),
        "leg_r": rot([(0.0, [-30, 0, 0]), (0.5, [30, 0, 0]), (1.0, [-30, 0, 0])]),
        "arm_l": rot([(0.0, [-25, 0, 0]), (0.5, [25, 0, 0]), (1.0, [-25, 0, 0])]),
        "arm_r": rot([(0.0, [25, 0, 0]), (0.5, [-25, 0, 0]), (1.0, [25, 0, 0])])}},
    "cheer": {"loop": False, "animation_length": 1.5, "bones": {
        "arm_l": rot([(0.0, [0, 0, 0]), (0.2, [180, 0, -20]), (0.5, [160, 0, -35]), (0.8, [180, 0, -20]), (1.1, [160, 0, -35]), (1.5, [0, 0, 0])]),
        "arm_r": rot([(0.0, [0, 0, 0]), (0.2, [180, 0, 20]), (0.5, [160, 0, 35]), (0.8, [180, 0, 20]), (1.1, [160, 0, 35]), (1.5, [0, 0, 0])]),
        "head": rot([(0.0, [0, 0, 0]), (0.3, [-15, 0, 0]), (1.2, [-15, 0, 0]), (1.5, [0, 0, 0])])}},
}})

# badge: gold star on a red shield, 16x16 item
badge = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(badge)
d.polygon([(2, 2), (13, 2), (13, 9), (8, 14), (7, 14), (2, 9)], fill=(190, 30, 40), outline=(90, 10, 16))
d.polygon([(7, 4), (8, 4), (9, 7), (12, 7), (10, 9), (11, 12), (8, 10), (7, 10), (4, 12), (5, 9), (3, 7), (6, 7)], fill=(250, 210, 60))
os.makedirs(f"{A}/textures/item", exist_ok=True)
badge.save(f"{A}/textures/item/attendant_badge.png")
w(f"{A}/models/item/attendant_badge.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "arcadeplus:item/attendant_badge"}})
w(f"{A}/items/attendant_badge.json", {"model": {"type": "minecraft:model", "model": "arcadeplus:item/attendant_badge"}})
lp = f"{A}/lang/en_us.json"
lang = json.load(open(lp, encoding="utf-8")) if os.path.exists(lp) else {}
lang["item.arcadeplus.attendant_badge"] = "Attendant Badge"
lang["entity.arcadeplus.attendant"] = "Arcade Attendant"
w(lp, lang)
print("attendant assets written")
