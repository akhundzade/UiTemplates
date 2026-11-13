#!/usr/bin/env python3
"""Generates the app icon for every platform from one geometry.

Run from anywhere: python3 tools/app_icon.py
Writes the Android adaptive icon (vector background, foreground and monochrome layers), the legacy
Android launcher PNGs, and the iOS 1024px icon. Needs Pillow only.

The artwork is a motion trail: fading violet balls arc up into a glowing violet-to-cyan ball, on the
showcase's dark navy. Coordinates are in the 108-unit space of an Android adaptive icon, whose central
66-unit circle is the safe zone every launcher mask keeps.
"""
from pathlib import Path

from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "androidApp" / "src" / "main" / "res"
IOS_ICON = ROOT / "iosApp" / "iosApp" / "Assets.xcassets" / "AppIcon.appiconset" / "app-icon-1024.png"

UNITS = 108
BG_TOP = (0x2B, 0x24, 0x6B)
BG_BOTTOM = (0x0B, 0x0A, 0x19)
VIOLET = (0x8B, 0x7C, 0xFF)
CYAN = (0x22, 0xD3, 0xEE)

# Quadratic arc the trail follows, ending at the main ball.
ARC = ((34.0, 76.0), (32.0, 46.0), (63.0, 47.0))
BALL_RADIUS = 12.0
TRAIL = [(0.0, 3.2, 0.28), (0.201, 4.6, 0.42), (0.547, 6.6, 0.62)]  # (t on arc, radius, opacity)
GLOW_RADIUS = 24.0


def arc_point(t):
    (x0, y0), (x1, y1), (x2, y2) = ARC
    u = 1 - t
    return (u * u * x0 + 2 * u * t * x1 + t * t * x2, u * u * y0 + 2 * u * t * y1 + t * t * y2)


BALL = arc_point(1.0)
HIGHLIGHT = (BALL[0] - 4.2, BALL[1] - 4.6, 4.0)  # (x, y, radius) soft white catch-light


# ---------------------------------------------------------------- raster (Pillow)

def linear_gradient(size, start, end, angle):
    """`start` at the top-left corner fading to `end` at the bottom-right when angle is 45."""
    big = int(size * 1.5)
    ramp = Image.linear_gradient("L").resize((big, big)).rotate(angle, resample=Image.BICUBIC)
    offset = (big - size) // 2
    mask = ramp.crop((offset, offset, offset + size, offset + size))
    return Image.composite(Image.new("RGB", (size, size), end), Image.new("RGB", (size, size), start), mask)


def circle_mask(size, cx, cy, r, scale):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([(cx - r) * scale, (cy - r) * scale, (cx + r) * scale, (cy + r) * scale], fill=255)
    return mask


def paint(canvas, color_image, mask, opacity=1.0):
    if opacity < 1.0:
        mask = mask.point(lambda v: int(v * opacity))
    canvas.paste(color_image, (0, 0), mask)


def render_layers(size):
    """Full-bleed 108-unit composition at `size` px: background, then foreground."""
    scale = size / UNITS
    canvas = linear_gradient(size, BG_TOP, BG_BOTTOM, 45).convert("RGBA")

    glow = circle_mask(size, BALL[0], BALL[1], GLOW_RADIUS * 0.62, scale).filter(ImageFilter.GaussianBlur(9 * scale))
    paint(canvas, Image.new("RGB", (size, size), VIOLET), glow, 0.55)

    for t, r, alpha in TRAIL:
        x, y = arc_point(t)
        paint(canvas, Image.new("RGB", (size, size), VIOLET), circle_mask(size, x, y, r, scale), alpha)

    # The gradient spans the ball itself, so both colours show on it.
    side = int(2 * BALL_RADIUS * scale)
    ball_fill = Image.new("RGB", (size, size), CYAN)
    ball_fill.paste(linear_gradient(side, VIOLET, CYAN, 45), (int((BALL[0] - BALL_RADIUS) * scale), int((BALL[1] - BALL_RADIUS) * scale)))
    paint(canvas, ball_fill, circle_mask(size, BALL[0], BALL[1], BALL_RADIUS, scale))
    hx, hy, hr = HIGHLIGHT
    highlight = circle_mask(size, hx, hy, hr, scale).filter(ImageFilter.GaussianBlur(1.6 * scale))
    highlight = ImageChops.multiply(highlight, circle_mask(size, BALL[0], BALL[1], BALL_RADIUS, scale))
    paint(canvas, Image.new("RGB", (size, size), (255, 255, 255)), highlight, 0.45)
    return canvas


def render(size_px, crop_units, shape=None, supersample=4):
    """Crop the central `crop_units` of the composition, scale to `size_px`, and mask to `shape`."""
    full = int(size_px * UNITS / crop_units) * supersample
    art = render_layers(full)
    inset = (full - size_px * supersample) // 2
    art = art.crop((inset, inset, inset + size_px * supersample, inset + size_px * supersample))
    art = art.resize((size_px, size_px), Image.LANCZOS)
    if shape is None:
        return art.convert("RGB")
    mask = Image.new("L", (size_px * supersample,) * 2, 0)
    draw = ImageDraw.Draw(mask)
    box = [0, 0, size_px * supersample - 1, size_px * supersample - 1]
    if shape == "circle":
        draw.ellipse(box, fill=255)
    else:
        draw.rounded_rectangle(box, radius=int(size_px * supersample * 0.22), fill=255)
    art.putalpha(mask.resize((size_px, size_px), Image.LANCZOS))
    return art


# ---------------------------------------------------------------- vector (Android)

def hex_color(rgb, alpha=1.0):
    return "#{:02X}{:02X}{:02X}{:02X}".format(round(alpha * 255), *rgb)


def circle_path(cx, cy, r):
    return f"M{cx - r:.2f},{cy:.2f}a{r:.2f},{r:.2f} 0,1 1,{2 * r:.2f},0a{r:.2f},{r:.2f} 0,1 1,{-2 * r:.2f},0z"


def vector(body):
    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        "<!-- Generated by tools/app_icon.py. Edit the script, not this file. -->\n"
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:aapt="http://schemas.android.com/aapt"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        f"{body}</vector>\n"
    )


def background_xml():
    return vector(
        '    <path android:pathData="M0,0h108v108h-108z">\n'
        '        <aapt:attr name="android:fillColor">\n'
        '            <gradient android:type="linear" android:startX="0" android:startY="0"\n'
        '                android:endX="108" android:endY="108"\n'
        f'                android:startColor="{hex_color(BG_TOP)}" android:endColor="{hex_color(BG_BOTTOM)}" />\n'
        "        </aapt:attr>\n    </path>\n"
    )


def foreground_xml(monochrome=False):
    body = ""
    if not monochrome:
        body += (
            f'    <path android:pathData="{circle_path(BALL[0], BALL[1], GLOW_RADIUS)}">\n'
            '        <aapt:attr name="android:fillColor">\n'
            f'            <gradient android:type="radial" android:centerX="{BALL[0]:.2f}" android:centerY="{BALL[1]:.2f}"\n'
            f'                android:gradientRadius="{GLOW_RADIUS:.2f}"\n'
            f'                android:startColor="{hex_color(VIOLET, 0.5)}" android:endColor="{hex_color(VIOLET, 0)}" />\n'
            "        </aapt:attr>\n    </path>\n"
        )
    for t, r, alpha in TRAIL:
        x, y = arc_point(t)
        color = hex_color((255, 255, 255) if monochrome else VIOLET, alpha)
        body += f'    <path android:pathData="{circle_path(x, y, r)}" android:fillColor="{color}" />\n'
    if monochrome:
        body += f'    <path android:pathData="{circle_path(BALL[0], BALL[1], BALL_RADIUS)}" android:fillColor="#FFFFFFFF" />\n'
        return vector(body)
    left, top = BALL[0] - BALL_RADIUS, BALL[1] - BALL_RADIUS
    body += (
        f'    <path android:pathData="{circle_path(BALL[0], BALL[1], BALL_RADIUS)}">\n'
        '        <aapt:attr name="android:fillColor">\n'
        f'            <gradient android:type="linear" android:startX="{left:.2f}" android:startY="{top:.2f}"\n'
        f'                android:endX="{left + 2 * BALL_RADIUS:.2f}" android:endY="{top + 2 * BALL_RADIUS:.2f}"\n'
        f'                android:startColor="{hex_color(VIOLET)}" android:endColor="{hex_color(CYAN)}" />\n'
        "        </aapt:attr>\n    </path>\n"
    )
    hx, hy, hr = HIGHLIGHT
    body += (
        f'    <path android:pathData="{circle_path(hx, hy, hr * 1.5)}">\n'
        '        <aapt:attr name="android:fillColor">\n'
        f'            <gradient android:type="radial" android:centerX="{hx:.2f}" android:centerY="{hy:.2f}"\n'
        f'                android:gradientRadius="{hr * 1.5:.2f}"\n'
        f'                android:startColor="{hex_color((255, 255, 255), 0.45)}" android:endColor="#00FFFFFF" />\n'
        "        </aapt:attr>\n    </path>\n"
    )
    return vector(body)


ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}


def main():
    (RES / "drawable" / "ic_launcher_background.xml").write_text(background_xml())
    (RES / "drawable-v24" / "ic_launcher_foreground.xml").write_text(foreground_xml())
    (RES / "drawable-v24" / "ic_launcher_monochrome.xml").write_text(foreground_xml(monochrome=True))
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        (RES / "mipmap-anydpi-v26" / name).write_text(ADAPTIVE)
    # Legacy launchers (API 24-25) show the central 72 units, the same area adaptive masks reveal.
    for density, px in LEGACY_SIZES.items():
        render(px, 72, "rounded").save(RES / f"mipmap-{density}" / "ic_launcher.png")
        render(px, 72, "circle").save(RES / f"mipmap-{density}" / "ic_launcher_round.png")
    # iOS masks the icon itself and rejects alpha, so it gets an opaque full-bleed square.
    render(1024, 80).save(IOS_ICON)
    print("Wrote Android adaptive + legacy icons and the iOS icon")


if __name__ == "__main__":
    main()
