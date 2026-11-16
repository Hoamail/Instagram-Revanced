#!/usr/bin/env python3
"""
Repack a stock APK with any number of replaced dex files plus extra dex files.

  repack.py <stock.apk> <out.apk> [--replace classes4=path.dex ...] [--add classes33=path.dex ...]

Every untouched entry keeps its original order, timestamp, compression and
attributes, so the APK still installs as an update over the stock package.
"""
import sys
import zipfile


def main():
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    src, out = sys.argv[1], sys.argv[2]

    replace, add = {}, {}
    args = sys.argv[3:]
    i = 0
    while i < len(args):
        flag, spec = args[i], args[i + 1]
        name, path = spec.split("=", 1)
        if flag == "--replace":
            replace[name] = path
        elif flag == "--add":
            add[name] = path
        else:
            sys.exit(f"unknown flag {flag}")
        i += 2

    with zipfile.ZipFile(src) as zin:
        names = zin.namelist()
        for name in replace:
            if name not in names:
                sys.exit(f"{name} not present in {src}")
        for name in add:
            if name in names:
                sys.exit(f"{name} already present in {src}")

        with zipfile.ZipFile(out, "w") as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename in replace:
                    data = open(replace[info.filename], "rb").read()
                    print(f"replaced {info.filename} -> {len(data)}B")
                zi = zipfile.ZipInfo(info.filename, date_time=info.date_time)
                zi.compress_type = info.compress_type
                zi.external_attr = info.external_attr
                zi.internal_attr = info.internal_attr
                zi.create_system = info.create_system
                zout.writestr(zi, data)

            for name, path in add.items():
                data = open(path, "rb").read()
                zi = zipfile.ZipInfo(name, date_time=(2026, 1, 1, 0, 0, 0))
                zi.compress_type = zipfile.ZIP_DEFLATED
                zout.writestr(zi, data)
                print(f"added {name} (deflated, {len(data)}B)")

    print(f"wrote {out}")


if __name__ == "__main__":
    main()
