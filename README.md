# Instagram-Revanced — view-once media saver

Tự lưu ảnh/video **xem một lần** trong DM Instagram ra `Pictures/InstagramViewOnce`,
trên bản APK do chính bạn build và ký lại.

Target: Instagram **455.0.0.51.72** (arm64-v8a, Android 16 / API 36).

| Loại | Định dạng | Nguồn |
|---|---|---|
| Ảnh view-once | PNG 1280×2275 (~2.4 MB) | bitmap lớn nhất trong `contentHolder` |
| Video view-once | MP4 (~350 KB) | URL CDN dò từ media model |

## Cách hoạt động

App gọi `IgProgressImageView.A0C(listener, index)` với listener `X/0lJh`. Khi ảnh load
xong, Instagram gọi `X/0lJh->Fs7(LX/00n3;)V`. Hook nằm ở **instruction đầu tiên** của
method đó:

```smali
.method public final Fs7(LX/00n3;)V
    .locals 4
    invoke-static {p0, p1}, Lapp/hx/viewonce/Saver;->onMediaLoaded(Ljava/lang/Object;Ljava/lang/Object;)V
```

`Saver` được nhồi vào `classes33.dex` và gắn vào APK; nó tìm controller bằng **fragment
tên class** (`DirectVisualMessageViewerController`), không hardcode field obfuscated.

## Build

```powershell
# cần: APKEditor, JDK 17+, Android build-tools 36 + android-36 platform
# 1) extension (Saver) -> work\ext-dex\classes.dex
.\build-patch.ps1 -SkipPatched                # javac + d8 only
# 2) patch từng dex (decode -> sửa smali -> build) -> work\<dex>.dex.patched
python patch_dex.py <stock.apk> classes4.dex  X/09Os.smali:178  X/09Os.smali:258
python patch_dex.py <stock.apk> classes21.dex X/0lJh.smali:1 ...
# 3) ghép + ký + cài
.\assemble.ps1 -Stock <stock.apk>             # -NoInstall nếu chỉ muốn build
```

Chain trong `assemble.ps1`: `repack.py` (thay 8 dex + thêm `classes33.dex`) → zipalign →
apksigner → `adb install -r`. Ký bằng keystore riêng, **phải giữ nguyên keystore** để
update đè lên bản đã cài (không mất đăng nhập).

## Cài

```powershell
adb install -r work\ig-hx-signed.apk   # cùng key => giữ data
adb logcat -s HX-VO:V                  # xem hook chạy
```

Nếu máy chặn (`INSTALL_FAILED_USER_RESTRICTED`): `adb shell settings put global adb_install_need_confirm 0`.

## Trạng thái

- [x] Hook view-once photo → PNG
- [x] Hook view-once video → MP4 (URL CDN, tải trực tiếp), dedupe theo nội dung
- [x] Xác định **hai** cơ chế chặn chụp màn hình: registry `X/09Os` (theo window) +
      9 điểm `setFlags(0x2000)` rải trong `classes5/13/16/27/28/29`
- [x] Xác định điểm làm đen bản ghi màn hình: `LX/0SRR.<init>` → `SurfaceView.setSecure(true)`
- [x] Patch cả loạt 8 dex (`classes4,5,13,16,21,27,28,29`) → APK v5, cài đè giữ đăng nhập
- [ ] Xác nhận Instants chụp được (đang test trên máy)
- [ ] Xác nhận quay màn hình view-once video không còn đen
- [ ] Tắt `registerScreenCaptureCallback` (bỏ thông báo "đã chụp màn hình" gửi đi)
- [ ] Port sang 457.x

Chi tiết kỹ thuật, anchor, gotcha: xem [`COMPACT.md`](COMPACT.md).
