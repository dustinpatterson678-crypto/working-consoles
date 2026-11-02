"""Game room furniture (wcore decor). Model front faces north (-z); backrests sit on +z."""
import json, os
from padgen import make_pad, ROOT, _w

RES = os.path.join(ROOT, "wcore", "src", "main", "resources")


def decor(name, display, colours, cubes, recipe):
    make_pad("wcore", name, display, colours, {"body": cubes}, "minecraft:air")
    a = os.path.join(RES, "assets", "wcore")
    _w(f"{a}/blockstates/{name}.json", {"variants": {"": {"model": f"wcore:block/{name}"}}})
    _w(f"{RES}/data/wcore/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "entries": [{"type": "minecraft:item", "name": f"wcore:{name}"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    _w(f"{RES}/data/wcore/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "building",
        "pattern": recipe[0], "key": recipe[1], "result": {"id": f"wcore:{name}", "count": recipe[2] if len(recipe) > 2 else 1}})


decor("gaming_chair", "Gaming Chair", {"black": (28, 28, 32), "red": (190, 30, 40), "metal": (120, 120, 128)}, [
    (-0.6, 0, -0.6, 0.6, 5, 0.6, "metal"), (-5, 0, -0.5, 5, 0.6, 0.5, "metal"), (-0.5, 0, -5, 0.5, 0.6, 5, "metal"),
    (-5, 5, -5, 5, 7, 5, "black"), (-4, 7, -4.5, 4, 7.3, 4, "red"),
    (-5, 7, 4, 5, 21, 6, "black"), (-3, 8, 3.7, 3, 19, 4, "red"), (-3, 21, 4.2, 3, 23, 5.8, "black"),
    (-6, 9, -4, -5, 10, 3, "black"), (5, 9, -4, 6, 10, 3, "black"), (-6, 7, 1, -5, 9, 2, "metal"), (5, 7, 1, 6, 9, 2, "metal"),
], (["W W", "WRW", "I I"], {"W": "#minecraft:wool", "R": "minecraft:red_dye", "I": "minecraft:iron_ingot"}))

decor("couch", "Couch", {"blue": (40, 70, 170), "dark": (25, 45, 120), "wood": (90, 60, 35)}, [
    (-8, 0, -6, 8, 1, 7, "wood"), (-8, 1, -6, 8, 6, 7, "dark"), (-8, 6, -6, 8, 8, 4, "blue"),
    (-8, 6, 4, 8, 16, 8, "blue"), (-8, 14, 3, 8, 16, 4, "dark"),
], (["  W", "WWW", "P P"], {"W": "minecraft:blue_wool", "P": "#minecraft:planks"}, 2))

decor("bean_bag", "Bean Bag", {"red": (200, 35, 45), "dark": (140, 20, 30)}, [
    (-6, 0, -6, 6, 4, 6, "red"), (-5, 4, -5, 5, 6, 5, "red"), (-5, 6, 1, 5, 11, 6, "dark"), (-4, 11, 2, 4, 12, 5, "dark"),
], (["WWW", "W W", "WWW"], {"W": "minecraft:red_wool"}))

decor("coffee_table", "Coffee Table", {"wood": (110, 72, 40), "glass": (170, 210, 230), "dark": (70, 45, 25)}, [
    (-7, 0, -5, -6, 6, -4, "dark"), (6, 0, -5, 7, 6, -4, "dark"), (-7, 0, 4, -6, 6, 5, "dark"), (6, 0, 4, 7, 6, 5, "dark"),
    (-7, 2, -5, 7, 2.6, 5, "wood"), (-7.5, 6, -5.5, 7.5, 7, 5.5, "wood"), (-6.5, 7, -4.5, 6.5, 7.2, 4.5, "glass"),
], (["GGG", "P P"], {"G": "minecraft:glass_pane", "P": "#minecraft:planks"}))
