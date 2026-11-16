#!/usr/bin/env python3
"""
Decode one dex from the stock APK, patch `const/16 vX, 0x2000` literals to
`const/4 vX, 0x0`, rebuild the dex and drop it in work/<dex>.patched.

Usage:
  patch_dex.py <stock.apk> <dex-name> <relpath:line> [...]
  patch_dex.py base.apk classes5.dex X/06GW.smali:1241
"""
import os
import subprocess
import sys
import zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
APKEDITOR = os.path.join(ROOT, "tools", "dl", "APKEditor.jar")
WORK = os.path.join(ROOT, "work")


def run(args, **kw):
    print("+", " ".join(str(a) for a in args), flush=True)
    res = subprocess.run(args, cwd=ROOT, capture_output=True, text=True, errors="replace", **kw)
    if res.returncode != 0:
        sys.exit(f"FAILED ({res.returncode}): {res.stdout[-800:]}\n{res.stderr[-800:]}")
    return res.stdout


def main():
    if len(sys.argv) < 4:
        sys.exit(__doc__)
    stock, dex = sys.argv[1], sys.argv[2]
    targets = []
    for spec in sys.argv[3:]:
        rel, line = spec.rsplit(":", 1)
        targets.append((rel, int(line)))

    base = dex.replace(".dex", "")
    stub = os.path.join(WORK, f"stub_{base}.apk")
    tree = os.path.join(WORK, f"d_{base}")

    run([sys.executable, "build-stub.py", stock, stub, dex])
    if os.path.isdir(tree):
        subprocess.run(["cmd", "/c", "rmdir", "/s", "/q", tree], check=False)
    run(["java", "-Xmx5g", "-jar", APKEDITOR, "d", "-f", "-no-cache", "-comment-level", "off",
         "-i", stub, "-o", tree])

    for rel, line in targets:
        path = os.path.join(tree, "smali", dex.replace(".dex", ""), rel.replace("/", os.sep))
        if not os.path.exists(path):
            sys.exit(f"missing {path}")
        with open(path, encoding="utf-8", errors="replace") as fh:
            lines = fh.read().splitlines(keepends=True)
        old = lines[line - 1]
        if "0x2000" not in old:
            sys.exit(f"{rel}:{line} does not hold 0x2000: {old!r}")
        lines[line - 1] = old.replace("const/16 v0, 0x2000", "const/4 v0, 0x0")
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write("".join(lines))
        print(f"patched {dex}/{rel}:{line} -> {lines[line - 1].strip()}", flush=True)

    out_apk = os.path.join(WORK, f"{base}-patched.apk")
    if os.path.exists(out_apk):
        os.remove(out_apk)
    run(["java", "-Xmx5g", "-jar", APKEDITOR, "b", "-i", tree, "-o", out_apk])

    dest = os.path.join(WORK, f"{dex}.patched")
    with zipfile.ZipFile(out_apk) as z:
        data = z.read(dex)
    with open(dest, "wb") as fh:
        fh.write(data)
    print(f"{dest} {len(data)}B", flush=True)


if __name__ == "__main__":
    main()
