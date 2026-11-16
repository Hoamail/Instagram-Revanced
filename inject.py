#!/usr/bin/env python3
"""
Find the view-once "media loaded" callback in a decoded smali tree and inject
the Saver hook as its first instruction.

Anchors (same idea as ReVanced/Morphe fingerprints, done with plain text):
  1. the class whose body contains the string "photo_cant_load"  (error branch)
  2. inside it, the void method with >=1 parameter that references
     DirectVisualMessageViewerController and is NOT the error method
     -> the "media loaded" callback
  3. insert
       invoke-static {p0, p1}, Lapp/hx/viewonce/Saver;->onMediaLoaded(Ljava/lang/Object;Ljava/lang/Object;)V
     right after its .locals/.registers directive

Idempotent: re-running on a patched tree reports "already patched".

Usage: inject.py <smali-root> [--dex classes21.dex] [--dry-run]
Exit 0 = patched or already patched, 2 = anchor not found.
"""
import os
import sys

HOOK = ("    invoke-static {p0, p1}, Lapp/hx/viewonce/Saver;"
        "->onMediaLoaded(Ljava/lang/Object;Ljava/lang/Object;)V")

ANCHOR_STRING = "photo_cant_load"
CONTROLLER = "DirectVisualMessageViewerController"


def find_methods(lines):
    methods, cur = [], None
    for i, line in enumerate(lines):
        if line.startswith(".method "):
            cur = {"start": i, "header": line.strip(), "body": []}
        elif line.startswith(".end method") and cur is not None:
            cur["end"] = i
            methods.append(cur)
            cur = None
        elif cur is not None:
            cur["body"].append(line)
    return methods


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    root = sys.argv[1]
    dry = "--dry-run" in sys.argv

    targets = []
    for dirpath, _, files in os.walk(root):
        for fn in files:
            if not fn.endswith(".smali"):
                continue
            path = os.path.join(dirpath, fn)
            with open(path, encoding="utf-8", errors="replace") as fh:
                body = fh.read()
            if ANCHOR_STRING in body:
                targets.append((path, body))

    if not targets:
        sys.exit(f"FAIL: no smali file contains {ANCHOR_STRING!r}")

    failures = 0
    for path, body in targets:
        lines = body.splitlines(keepends=True)
        methods = find_methods(lines)

        error_header = None
        for m in methods:
            if ANCHOR_STRING in "".join(m["body"]):
                error_header = m["header"]

        picked = None
        for m in methods:
            header = m["header"]
            text = "".join(m["body"])
            if header == error_header:
                continue
            if CONTROLLER not in text:
                continue
            if not header.split(")")[-1].strip().startswith("V"):
                continue
            params = header[header.find("(") + 1:header.find(")")]
            if not params:
                continue
            picked = m
            break

        if picked is None:
            print(f"SKIP {path}: class found, callback not found "
                  f"({[m['header'] for m in methods]})")
            failures += 1
            continue

        if HOOK.strip() in body:
            print(f"OK   {path}: already patched")
            continue

        insert_at = None
        for i in range(picked["start"], picked["end"]):
            s = lines[i].strip()
            if s.startswith(".locals") or s.startswith(".registers"):
                insert_at = i + 1
                break
        if insert_at is None:
            print(f"SKIP {path}: no .locals in {picked['header']}")
            failures += 1
            continue

        print(f"PATCH {path}")
        print(f"      method: {picked['header']}")
        if dry:
            continue
        lines.insert(insert_at, "\n" + HOOK + "\n")
        with open(path, "w", encoding="utf-8", newline="") as fh:
            fh.write("".join(lines))

    if failures:
        sys.exit(2)


if __name__ == "__main__":
    main()
