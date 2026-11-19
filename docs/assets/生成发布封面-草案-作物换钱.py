# -*- coding: utf-8 -*-
"""多多卖菜 — 发布封面草案：GUI 羊皮纸底 + 作物 → ¥ + 右侧标题"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
REFS = HERE / "cover_refs"
OUT = HERE / "hsv-sellveg-cover-draft-crops.png"

W, H = 1536, 1024
SHEAR = 0.14
WOOD = (74, 48, 32, 255)

LINES = [
    ("生存轻经营", 52),
    ("多多", 156),
    ("卖菜", 164),
    ("Minecraft模组发布", 68),
]

TITLE_LEFT = 720
AFFILIATE_TOP = 150
TITLE1_TOP = 300
TITLE2_TOP = 510
BOTTOM_TOP = 790
TEXT_FILL = (58, 38, 24, 255)
TEXT_SHADOW = (255, 255, 255, 120)

CROPS = [
    ("wheat.png", 0, 0),
    ("carrot.png", 28, 52),
    ("potato.png", -18, 104),
    ("beetroot.png", 34, 150),
]

FONT_CANDIDATES = [
    r"C:\Windows\Fonts\Noto Sans SC Bold (TrueType).otf",
    r"C:\Windows\Fonts\msyhbd.ttc",
    r"C:\Windows\Fonts\msyhbd.ttc",
]


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for path in FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def scale_icon(path: Path, scale: int) -> Image.Image:
    img = Image.open(path).convert("RGBA")
    return img.resize((img.width * scale, img.height * scale), Image.Resampling.NEAREST)


def draw_bg(canvas: Image.Image):
    shop = Image.open(REFS / "shop_bg.png").convert("RGBA")
    # 铺满画布：先铺木色，再最近邻放大 GUI（保留像素感）
    canvas.paste(WOOD, (0, 0, W, H))
    scale = max(W / shop.width, H / shop.height)
    sw, sh = int(shop.width * scale), int(shop.height * scale)
    big = shop.resize((sw, sh), Image.Resampling.NEAREST)
    ox, oy = (W - sw) // 2, (H - sh) // 2
    canvas.alpha_composite(big, (ox, oy))


def draw_arrow(draw: ImageDraw.ImageDraw, x: int, y: int, length: int = 180):
    cy = y + 20
    draw.polygon(
        [(x, cy - 18), (x + length - 50, cy - 18), (x + length - 50, cy - 38), (x + length, cy), (x + length - 50, cy + 38), (x + length - 50, cy + 18)],
        fill=(58, 38, 24, 255),
    )
    draw.rectangle((x, cy - 18, x + length - 55, cy + 18), fill=(58, 38, 24, 255))


def draw_yen(draw: ImageDraw.ImageDraw, x: int, y: int):
    font = load_font(200)
    for dx, dy in ((-2, 0), (2, 0), (0, -2), (0, 2), (-2, -2), (2, 2)):
        draw.text((x + dx, y + dy), "￥", font=font, fill=(120, 78, 20, 255))
    draw.text((x, y), "￥", font=font, fill=(255, 196, 48, 255))


def render_line(text: str, font, fill=TEXT_FILL, shadow=TEXT_SHADOW) -> Image.Image:
    pad = max(80, font.size)
    canvas = Image.new("RGBA", (2400, 600), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    x = y = pad
    draw.text((x + 2, y + 2), text, font=font, fill=shadow)
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        draw.text((x + dx, y + dy), text, font=font, fill=fill)
    draw.text((x, y), text, font=font, fill=fill)
    arr = np.array(canvas)
    ys, xs = np.where(arr[:, :, 3] > 0)
    glyph = canvas.crop((xs.min() - 4, ys.min() - 4, xs.max() + 5, ys.max() + 5))
    extra = int(abs(SHEAR) * glyph.height) + 24
    wide = Image.new("RGBA", (glyph.width + extra, glyph.height + 16), (0, 0, 0, 0))
    wide.paste(glyph, (extra // 2, 8))
    cy = wide.height / 2.0
    sheared = wide.transform(
        wide.size,
        Image.AFFINE,
        (1, SHEAR, -SHEAR * cy, 0, 1, 0),
        resample=Image.BICUBIC,
    )
    sa = np.array(sheared)
    ys, xs = np.where(sa[:, :, 3] > 0)
    return sheared.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def paste_line(base: Image.Image, layer: Image.Image, left: int, top: int, center: bool = False):
    x = left - layer.width // 2 if center else left
    base.alpha_composite(layer, (x, top))


def main():
    base = Image.new("RGBA", (W, H), WOOD)
    draw_bg(base)
    draw = ImageDraw.Draw(base)

    # 左侧：作物簇 → 箭头 → ￥（避开 GUI 左下角装饰，放在偏上区域）
    crop_scale = 9
    origin_x, origin_y = 120, 330
    for name, ox, oy in CROPS:
        icon = scale_icon(REFS / "vanilla" / name, crop_scale)
        base.alpha_composite(icon, (origin_x + ox, origin_y + oy))

    arrow_x = origin_x + 210
    arrow_y = origin_y + 70
    draw_arrow(draw, arrow_x, arrow_y)
    draw_yen(draw, arrow_x + 210, origin_y + 10)

    fonts = [load_font(size) for _, size in LINES]
    layers = [render_line(text, font) for (text, _), font in zip(LINES, fonts)]
    title_w = max(layers[1].width, layers[2].width)
    aff_center_x = TITLE_LEFT + title_w // 2
    paste_line(base, layers[0], aff_center_x, AFFILIATE_TOP, center=True)
    paste_line(base, layers[1], TITLE_LEFT, TITLE1_TOP)
    paste_line(base, layers[2], TITLE_LEFT, TITLE2_TOP)
    paste_line(base, layers[3], TITLE_LEFT, BOTTOM_TOP)

    base.save(OUT)
    print(f"Wrote {OUT} ({W}x{H})")


if __name__ == "__main__":
    main()
