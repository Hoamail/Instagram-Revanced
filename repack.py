#!/usr/bin/env python3
"""
Repack a signed-ready APK: replace one dex, append the extension dex.

Keeps every original entry's order, timestamp, compression and attributes so
the result still installs as an update of the stock package.

Usage: repack.py <stock.apk> <patched.dex> <extra.dex> <out.apk>
"""
import sys
import zipfile

src, patched_dex, extra_dex, out = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
TARGET = "classes21.dex"
EXTRA = "classes33.dex"

with zipfile.ZipFile(src) as zin, zipfile.ZipFile(out, "w") as zout:
    names = zin.namelist()
    if TARGET not in names:
        raise SystemExit(f"{TARGET} not in {src}")

    for info in zin.infolist():
        data = zin.read(info.filename)
        if info.filename == TARGET:
            data = open(patched_dex, "rb").read()
            print(f"replaced {TARGET} -> {len(data)}B")
        zi = zipfile.ZipInfo(info.filename, date_time=info.date_time)
        zi.compress_type = info.compress_type
        zi.external_attr = info.external_attr
        zi.internal_attr = info.internal_attr
        zi.create_system = info.create_system
        zout.writestr(zi, data)

    if EXTRA in names:
        raise SystemExit(f"{EXTRA} already present in {src}")
    zi = zipfile.ZipInfo(EXTRA, date_time=(2026, 1, 1, 0, 0, 0))
    zi.compress_type = zipfile.ZIP_DEFLATED
    zout.writestr(zi, open(extra_dex, "rb").read())
    print(f"added {EXTRA} (deflated, {len(open(extra_dex, 'rb').read())}B)")

print(f"wrote {out}")
