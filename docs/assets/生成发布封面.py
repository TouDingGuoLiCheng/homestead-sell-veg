# -*- coding: utf-8 -*-
"""多多卖菜 — 在 cover.png 底图上叠加发布文案（居中排版）"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
SRC = HERE / "cover.png"

TARGET_W, TARGET_H = 1536, 1024

TEXT_FILL = (58, 38, 24, 255)
TEXT_SHADOW = (255, 255, 255, 130)

# 优先 Noto Sans SC Bold；缺失时回退微软雅黑 Bold
FONT_CANDIDATES = [
    r"C:\Windows\Fonts\Noto Sans SC Bold (TrueType).otf",
    r"C:\Windows\Fonts\msyhbd.ttc",
]
FONT_NAME = "Noto Sans SC Bold（思源黑体 Bold）"


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for path in FONT_CANDIDATES:
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def load_base() -> Image.Image:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing base image: {SRC}")
    base = Image.open(SRC).convert("RGBA")
    if base.size != (TARGET_W, TARGET_H):
        base = base.resize((TARGET_W, TARGET_H), Image.Resampling.NEAREST)
    return base


def render_line(text: str, font, shear: float = 0.0) -> Image.Image:
    pad = max(80, font.size)
    canvas = Image.new("RGBA", (2400, 600), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    x = y = pad
    draw.text((x + 2, y + 2), text, font=font, fill=TEXT_SHADOW)
    for dx, dy in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        draw.text((x + dx, y + dy), text, font=font, fill=TEXT_FILL)
    draw.text((x, y), text, font=font, fill=TEXT_FILL)
    arr = np.array(canvas)
    ys, xs = np.where(arr[:, :, 3] > 0)
    glyph = canvas.crop((xs.min() - 4, ys.min() - 4, xs.max() + 5, ys.max() + 5))
    if abs(shear) < 1e-6:
        return glyph
    extra = int(abs(shear) * glyph.height) + 20
    wide = Image.new("RGBA", (glyph.width + extra, glyph.height + 14), (0, 0, 0, 0))
    wide.paste(glyph, (extra // 2, 7))
    cy = wide.height / 2.0
    sheared = wide.transform(
        wide.size,
        Image.AFFINE,
        (1, shear, -shear * cy, 0, 1, 0),
        resample=Image.BICUBIC,
    )
    sa = np.array(sheared)
    ys, xs = np.where(sa[:, :, 3] > 0)
    return sheared.crop((xs.min(), ys.min(), xs.max() + 1, ys.max() + 1))


def paste_center(base: Image.Image, layer: Image.Image, center_x: int, top: int):
    base.alpha_composite(layer, (center_x - layer.width // 2, top))


def overlay_centered(base: Image.Image, items: list[tuple[str, int, int]]):
    cx = base.width // 2
    for text, size, top in items:
        layer = render_line(text, load_font(size))
        paste_center(base, layer, cx, top)


def main():
    full = load_base()
    overlay_centered(full, [
        ("多多卖菜", 132, 175),
        ("MineCraft模组发布", 88, 772),
    ])
    full_out = HERE / "hsv-sellveg-cover-release.png"
    full.save(full_out)
    print(f"[full] Wrote {full_out} ({full.width}x{full.height})")

    title = load_base()
    overlay_centered(title, [
        ("多多卖菜", 132, 175),
    ])
    title_out = HERE / "hsv-sellveg-cover-title-only.png"
    title.save(title_out)
    print(f"[title] Wrote {title_out} ({title.width}x{title.height})")
    print(f"Font: {FONT_NAME}")


if __name__ == "__main__":
    main()
