#!/usr/bin/env python3
"""Generates the Fly launcher icons (real PNG files) for every density bucket."""

import os
import struct
import zlib

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                    "app", "src", "main", "res")
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
SS = 4  # supersampling factor


def lerp(a, b, t):
    return a + (b - a) * t


def inside_polygon(px, py, points):
    odd = False
    j = len(points) - 1
    for i in range(len(points)):
        xi, yi = points[i]
        xj, yj = points[j]
        if (yi > py) != (yj > py):
            x_cross = (xj - xi) * (py - yi) / (yj - yi) + xi
            if px < x_cross:
                odd = not odd
        j = i
    return odd


def render(size, round_mask):
    width = size * SS
    height = size * SS
    pixels = [[(0, 0, 0, 0)] * width for _ in range(height)]
    radius = width / 2.0
    corner = width * 0.24

    # Stylised "F" / wing mark in the centre.
    mark_top = height * 0.30
    mark_bottom = height * 0.72
    bar_left = width * 0.32
    bar_right = width * 0.40
    arm_right = width * 0.68
    arm_height = height * 0.10
    mid_top = height * 0.46
    mid_right = width * 0.60
    wing = [
        (bar_right, mark_top),
        (arm_right, mark_top),
        (arm_right, mark_top + arm_height),
        (bar_right + (arm_right - bar_right) * 0.45, mark_top + arm_height),
        (mid_right, mid_top),
        (mid_right, mid_top + arm_height * 0.9),
        (bar_right + (mid_right - bar_right) * 0.35, mid_top + arm_height * 0.9),
        (bar_right, mark_bottom - height * 0.02),
    ]

    for y in range(height):
        fy = (y + 0.5) / height
        for x in range(width):
            fx = (x + 0.5) / width
            inside = True
            if round_mask:
                dx = fx * width - radius
                dy = fy * height - radius
                inside = (dx * dx + dy * dy) <= radius * radius
            else:
                # Rounded square mask.
                cx = min(max(fx * width, corner), width - corner)
                cy = min(max(fy * height, corner), height - corner)
                dx = fx * width - cx
                dy = fy * height - cy
                inside = (dx * dx + dy * dy) <= corner * corner
            if not inside:
                pixels[y][x] = (0, 0, 0, 0)
                continue

            # Diagonal violet -> indigo -> cyan gradient.
            t = min(1.0, max(0.0, (fx * 0.45 + fy * 0.75) / 1.2))
            if t < 0.55:
                u = t / 0.55
                r = lerp(96, 52, u)
                g = lerp(60, 38, u)
                b = lerp(235, 168, u)
            else:
                u = (t - 0.55) / 0.45
                r = lerp(52, 34, u)
                g = lerp(38, 196, u)
                b = lerp(168, 220, u)

            if inside_polygon(x + 0.5, y + 0.5, wing):
                pixels[y][x] = (255, 255, 255, 255)
            else:
                pixels[y][x] = (255, int(r), int(g), int(b))
    return pixels


def downsample(pixels, size):
    out = []
    for y in range(size):
        row = []
        for x in range(size):
            sr = sg = sb = sa = 0
            for dy in range(SS):
                for dx in range(SS):
                    r, g, b, a = pixels[y * SS + dy][x * SS + dx]
                    sr += r * a
                    sg += g * a
                    sb += b * a
                    sa += a
            total = SS * SS
            if sa > 0:
                row.append((sr // sa, sg // sa, sb // sa, sa // total))
            else:
                row.append((0, 0, 0, 0))
        out.append(row)
    return out


def write_png(path, rows):
    height = len(rows)
    width = len(rows[0])
    raw = b"".join(b"\x00" + bytes([c for px in row for c in px]) for row in rows)

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as handle:
        handle.write(png)


def main():
    for bucket, size in SIZES.items():
        folder = os.path.join(ROOT, "mipmap-" + bucket)
        os.makedirs(folder, exist_ok=True)
        square = downsample(render(size, False), size)
        rounded = downsample(render(size, True), size)
        write_png(os.path.join(folder, "ic_launcher.png"), square)
        write_png(os.path.join(folder, "ic_launcher_round.png"), rounded)
        print("wrote", folder, size)


if __name__ == "__main__":
    main()
