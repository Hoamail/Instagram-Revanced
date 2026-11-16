#!/usr/bin/env python3
"""
Build a stub APK: AndroidManifest.xml + resources.arsc + one dex.

APKEditor needs a real APK to decode; feeding it all 32 dex makes a single
pass take ~25 min, this keeps it at ~40 s.

Usage: build-stub.py <stock.apk> <out.apk> [dex-name]
"""
import sys
import zipfile

src, out = sys.argv[1], sys.argv[2]
dex = sys.argv[3] if len(sys.argv) > 3 else "classes21.dex"

with zipfile.ZipFile(src) as zin:
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as zout:
        for name in ("AndroidManifest.xml", "resources.arsc", dex):
            zout.writestr(name, zin.read(name))

print(f"{out}: manifest + arsc + {dex}")
