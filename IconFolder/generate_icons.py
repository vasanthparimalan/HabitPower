"""
HabitPower icon generator.

Design:
  - Dark forest-green rounded-square background (#1A3A2C)
  - Bold white "H" centred on the canvas
  - Bright-green diagonal arrow (bottom-left → top-right) cutting through the H
  - Tapered dotted arc (light green) wrapping the top-right quadrant

Run:
  python generate_icons.py

Outputs:
  - All mipmap density PNGs placed directly into app/src/main/res/mipmap-*/
  - mipmap-anydpi-v26/ic_launcher.xml and ic_launcher_round.xml (adaptive icon XML)
  - IconFolder/play_store_512.png (Play Store upload icon)
"""

import math
import os
from PIL import Image, ImageDraw

# ── Colour palette ────────────────────────────────────────────────────────────
BG         = (26, 58, 44, 255)   # dark forest green
WHITE      = (255, 255, 255, 255)
ARROW_G    = (72, 199, 90, 255)  # bright lime-green arrow
DOT_G      = (106, 212, 100, 210)  # lighter green dots / arc


# ── Core drawing function ─────────────────────────────────────────────────────
def make_icon(size: int, with_bg: bool = True) -> Image.Image:
    """Draw one HabitPower icon at *size* × *size* pixels."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = size

    # Background – rounded square
    if with_bg:
        r = int(s * 0.19)
        draw.rounded_rectangle([0, 0, s - 1, s - 1], radius=r, fill=BG)

    # ── H letter ──────────────────────────────────────────────────────────────
    bar = int(s * 0.115)          # stroke width of each H bar
    lx  = int(s * 0.215)         # left bar left edge
    rx  = int(s * 0.785) - bar   # right bar left edge
    ty  = int(s * 0.215)         # top of H
    by  = int(s * 0.785)         # bottom of H
    cy1 = int(s * 0.465)         # crossbar top
    cy2 = int(s * 0.535)         # crossbar bottom

    draw.rectangle([lx,       ty,       lx + bar, by      ], fill=WHITE)  # left bar
    draw.rectangle([rx,       ty,       rx + bar, by      ], fill=WHITE)  # right bar
    draw.rectangle([lx,       cy1,      rx + bar, cy2     ], fill=WHITE)  # crossbar

    # ── Arrow: diagonal shaft + arrowhead ─────────────────────────────────────
    thick = int(s * 0.082)
    x1, y1 = s * 0.255, s * 0.760   # tail (bottom-left)
    x2, y2 = s * 0.790, s * 0.225   # tip  (top-right)

    dx = x2 - x1
    dy = y2 - y1
    L  = math.hypot(dx, dy)
    nx =  (-dy / L) * thick / 2     # perpendicular half-width (x)
    ny =  ( dx / L) * thick / 2     # perpendicular half-width (y)

    head_len  = thick * 2.1
    ratio     = (L - head_len) / L
    sx2 = x1 + dx * ratio           # shaft-end before arrowhead
    sy2 = y1 + dy * ratio

    shaft = [
        (x1 + nx, y1 + ny),
        (sx2 + nx, sy2 + ny),
        (sx2 - nx, sy2 - ny),
        (x1 - nx, y1 - ny),
    ]
    draw.polygon(shaft, fill=ARROW_G)

    head = [
        (x2,  y2),
        (sx2 + nx * 1.65, sy2 + ny * 1.65),
        (sx2 - nx * 1.65, sy2 - ny * 1.65),
    ]
    draw.polygon(head, fill=ARROW_G)

    # ── Dotted arc – top-right quadrant ───────────────────────────────────────
    cx, cy   = s / 2, s / 2
    arc_r    = s * 0.435
    base_dot = s * 0.030
    n_dots   = 12
    a_start  = 205    # degrees (clockwise from 3-o'clock, PIL convention)
    a_end    = 340

    for i in range(n_dots):
        t     = i / (n_dots - 1)
        angle = math.radians(a_start + (a_end - a_start) * t)
        x     = cx + arc_r * math.cos(angle)
        y     = cy + arc_r * math.sin(angle)
        taper = math.sin(t * math.pi)              # 0→1→0 fade at ends
        dr    = base_dot * (0.35 + 0.65 * taper)
        draw.ellipse([x - dr, y - dr, x + dr, y + dr], fill=DOT_G)

    return img


# ── Save helper ───────────────────────────────────────────────────────────────
def save(img: Image.Image, path: str, size: int) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    out = img.resize((size, size), Image.LANCZOS)
    out.save(path, "PNG")
    print(f"  OK {path}  ({size}x{size})")


# ── Paths ─────────────────────────────────────────────────────────────────────
HERE    = os.path.dirname(os.path.abspath(__file__))
RES_DIR = os.path.join(HERE, "..", "app", "src", "main", "res")

MIPMAP_SIZES = {
    "mipmap-ldpi":     36,
    "mipmap-mdpi":     48,
    "mipmap-hdpi":     72,
    "mipmap-xhdpi":    96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

ADAPTIVE_DIR = os.path.join(RES_DIR, "mipmap-anydpi-v26")
DRAWABLE_DIR = os.path.join(RES_DIR, "drawable")


# ── Generate master at 1024 px ────────────────────────────────────────────────
print("Generating HabitPower icons …\n")
master = make_icon(1024, with_bg=True)

# Legacy mipmap PNGs (launcher + round)
for folder, px in MIPMAP_SIZES.items():
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        save(master, os.path.join(RES_DIR, folder, name), px)

# Adaptive foreground (transparent bg, 512 px → stored in drawable/)
fg_master = make_icon(512, with_bg=False)
fg_path   = os.path.join(DRAWABLE_DIR, "ic_launcher_foreground.png")
os.makedirs(DRAWABLE_DIR, exist_ok=True)
fg_master.save(fg_path, "PNG")
print(f"  OK {fg_path}  (512x512, no bg)")

# Adaptive background colour drawable
bg_xml = """<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#1A3A2C"/>
</shape>
"""
bg_xml_path = os.path.join(DRAWABLE_DIR, "ic_launcher_background.xml")
with open(bg_xml_path, "w") as f:
    f.write(bg_xml)
print(f"  OK {bg_xml_path}")

# Adaptive icon XMLs
adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>
"""
os.makedirs(ADAPTIVE_DIR, exist_ok=True)
for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
    path = os.path.join(ADAPTIVE_DIR, name)
    with open(path, "w") as f:
        f.write(adaptive_xml)
    print(f"  OK {path}")

# Play Store icon (512 px, squared – no rounded corners needed)
ps_icon = make_icon(512, with_bg=True)
ps_path = os.path.join(HERE, "play_store_512.png")
ps_icon.save(ps_path, "PNG")
print(f"  OK {ps_path}  (Play Store 512x512)")

print("\nAll icons generated successfully.")
