#!/usr/bin/env python3
"""
Locate every place Instagram sets WindowManager FLAG_SECURE (0x2000).

Looks for three shapes in decoded smali:
  1. const/16 vX, 0x2000   followed within 6 lines by Window->addFlags / setFlags
  2. or-int/lit16 vX, vY, 0x2000   (LayoutParams.flags |= FLAG_SECURE)
  3. const/16 vX, 0x2000   followed within 6 lines by iput / iput-* into a flags field

Usage: flagscan.py <smali-root> [<smali-root> ...]
"""
import os
import re
import sys

CALL = re.compile(r"Landroid/view/(?:Window|WindowManager\$LayoutParams|WindowManager)->"
                  r"(?:addFlags|setFlags)\(I+I?\)")
CONST = re.compile(r"const(?:/16|/high16)?\s+(v\d+), 0x0*2000\b")
ORLIT = re.compile(r"or-int/lit16\s+v\d+, v\d+, 0x0*2000\b")
IPUTFLAGS = re.compile(r"iput(?:-boolean|-wide)?\s+v\d+, v\d+, .*->flags:I")


def scan(root):
    hits = []
    for dirpath, _, files in os.walk(root):
        for fn in files:
            if not fn.endswith(".smali"):
                continue
            path = os.path.join(dirpath, fn)
            try:
                lines = open(path, encoding="utf-8", errors="replace").read().splitlines()
            except OSError:
                continue
            for i, line in enumerate(lines):
                if ORLIT.search(line):
                    hits.append((path, i + 1, "or-int-lit16", line.strip()))
                    continue
                m = CONST.search(line)
                if not m:
                    continue
                window = lines[i + 1:i + 7]
                for off, nxt in enumerate(window):
                    if CALL.search(nxt):
                        hits.append((path, i + 1, "const+call", nxt.strip()))
                        break
                    if IPUTFLAGS.search(nxt):
                        hits.append((path, i + 1, "const+iput-flags", nxt.strip()))
                        break
    return hits


def main():
    roots = sys.argv[1:] or ["."]
    total = 0
    for root in roots:
        hits = scan(root)
        total += len(hits)
        for path, ln, kind, ctx in hits:
            short = path.replace("\\", "/")
            for marker in ("/smali_classes", "/smali/"):
                if marker in short:
                    short = short.split(marker, 1)[1]
                    break
            print(f"{short}:{ln}  [{kind}]  {ctx}")
    print(f"# {total} site(s)")


if __name__ == "__main__":
    main()
