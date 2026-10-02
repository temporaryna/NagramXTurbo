#!/usr/bin/env python3
"""Crop ic_notification_* glyphs to Telegram-like canvas fill (~91% width).

Per file: trim transparent margins (alpha bbox), scale glyph to TARGET_WIDTH_RATIO
of the canvas, cap height at TARGET_HEIGHT_RATIO, center, save webp lossless
on the original canvas size. Relative to each file's own canvas: density buckets
(mdpi..xxxhdpi) are handled uniformly.
"""

import glob
import os
import sys

from PIL import Image

RES_ROOT = os.path.join(os.path.dirname(__file__), "../../TMessagesProj/src/main/res")
TARGET_WIDTH_RATIO = 0.91
TARGET_HEIGHT_RATIO = 0.83


def glyph_bbox(im):
    alpha = im.getchannel("A")
    return alpha.getbbox()


def process(path, apply):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    bbox = glyph_bbox(im)
    if bbox is None:
        return (path, w, h, "ПУСТОЙ (пропуск)", (0, 0), (0, 0))
    glyph = im.crop(bbox)
    gw, gh = glyph.size
    max_w = round(w * TARGET_WIDTH_RATIO)
    max_h = round(h * TARGET_HEIGHT_RATIO)
    scale = min(max_w / gw, max_h / gh)
    new_w, new_h = round(gw * scale), round(gh * scale)
    if new_w > max_w:
        new_w = max_w
    if new_h > max_h:
        new_h = max_h
    resized = glyph.resize((new_w, new_h), Image.LANCZOS)
    canvas = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    canvas.alpha_composite(resized, ((w - new_w) // 2, (h - new_h) // 2))
    fill_before = (100 * gw // w, 100 * gh // h)
    fill_after = (100 * new_w // w, 100 * new_h // h)
    note = f"{w}x{h}: {fill_before[0]}%x{fill_before[1]}% -> {fill_after[0]}%x{fill_after[1]}%"
    if apply and fill_before != fill_after:
        canvas.save(path, "WEBP", lossless=True)
    return (path, w, h, note, fill_before, fill_after)


def main():
    apply = "--apply" in sys.argv
    files = sorted(glob.glob(os.path.join(RES_ROOT, "drawable-*", "ic_notification_*.webp")))
    if not files:
        print("файлы не найдены")
        return 1
    changed = 0
    for path in files:
        _, _, _, note, before, after = process(path, apply)
        if before != after:
            changed += 1
        print(f"{os.path.relpath(path, RES_ROOT)}: {note}")
    print(f"\n{'APPLIED' if apply else 'DRY-RUN'}: {len(files)} файлов, изменится {changed}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
