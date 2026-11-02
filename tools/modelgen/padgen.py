"""Generates a console controller's GeckoLib assets (model, texture, animation, item/block json, recipe, lang).

make_pad(module, name, display, colours, bones, recipe_dye)
  colours: {"swatch": (r, g, b)}; every cube is painted one solid swatch (per-face UV into a 64x64 texture)
  bones:   {"body": [box, ...], "button_a": (pivot, [box, ...]), ...}; box = (x0, y0, z0, x1, y1, z1, swatch)
Model units: 1 = 1/16 block, y up, the grips/front face +z. Run with: python tools/modelgen/padgen.py <pads module>
"""
import json, os, sys, importlib
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..")


def _w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1)


def make_pad(module, name, display, colours, bones, recipe_dye):
    res = os.path.join(ROOT, module, "src", "main", "resources")
    a = os.path.join(res, "assets", module)
    pos = {}
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for i, (k, c) in enumerate(colours.items()):
        x, y = (i % 8) * 8, (i // 8) * 8
        pos[k] = (x, y)
        for px in range(x, x + 8):
            for py in range(y, y + 8):
                img.putpixel((px, py), tuple(c) + (255,))

    def cube(b):
        x0, y0, z0, x1, y1, z1, col = b
        u, v = pos[col]
        face = {"uv": [u + 1, v + 1], "uv_size": [4, 4]}
        return {"origin": [x0, y0, z0], "size": [round(x1 - x0, 3), round(y1 - y0, 3), round(z1 - z0, 3)],
                "uv": {f: dict(face) for f in ("north", "south", "east", "west", "up", "down")}}

    out = [{"name": "root", "pivot": [0, 0, 0]},
           {"name": "body", "parent": "root", "pivot": [0, 0, 0], "cubes": [cube(b) for b in bones["body"]]}]
    for bone, spec in bones.items():
        if bone != "body":
            pivot, boxes = spec
            out.append({"name": bone, "parent": "body", "pivot": list(pivot), "cubes": [cube(b) for b in boxes]})

    _w(f"{a}/geckolib/models/item/{name}.geo.json", {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + name, "texture_width": 64, "texture_height": 64,
                        "visible_bounds_width": 2, "visible_bounds_height": 1, "visible_bounds_offset": [0, 0.25, 0]},
        "bones": out}]})
    _w(f"{a}/geckolib/animations/item/{name}.animation.json", {"format_version": "1.8.0", "animations": {"idle": {
        "loop": True, "animation_length": 1.0, "bones": {"root": {"rotation": {"0.0": {"vector": [0, 0, 0]}}}}}}})
    os.makedirs(f"{a}/textures/item", exist_ok=True)
    img.save(f"{a}/textures/item/{name}.png")
    _w(f"{a}/items/{name}.json", {"model": {"type": "minecraft:special", "base": f"{module}:item/{name}",
                                            "model": {"type": "geckolib:geckolib"}}})
    base = json.load(open(os.path.join(ROOT, "wcore/src/main/resources/assets/wcore/models/item/wireless_controller.json"), encoding="utf-8"))
    base["textures"]["particle"] = f"{module}:item/{name}"
    _w(f"{a}/models/item/{name}.json", base)
    _w(f"{a}/models/block/{name}.json", {"textures": {"particle": f"{module}:item/{name}"}})
    _w(f"{a}/blockstates/{name}.json", {"variants": {f"facing={d}": {"model": f"{module}:block/{name}"}
                                                     for d in ("north", "south", "east", "west")}})
    _w(f"{res}/data/{module}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc",
        "pattern": [" R ", "IPI", "I I"],
        "key": {"R": "minecraft:redstone", "I": "minecraft:iron_ingot", "P": recipe_dye},
        "result": {"id": f"{module}:{name}", "count": 1}})
    lp = f"{a}/lang/en_us.json"
    lang = json.load(open(lp, encoding="utf-8")) if os.path.exists(lp) else {}
    lang[f"item.{module}.{name}"] = display
    lang[f"block.{module}.{name}"] = display
    _w(lp, lang)
    print("made", module + ":" + name)


if __name__ == "__main__":
    sys.path.insert(0, os.path.dirname(__file__))
    importlib.import_module(sys.argv[1])
