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
# cần: jadx/apktool không bắt buộc; bắt buộc APKEditor, JDK 17+, Android build-tools 36 + android-36 platform
.\build-patch.ps1 -StockApk ..\stock\base.apk -Out work\ig-hx-signed.apk
```

Chain: `build-stub.py` (cắt 1 dex) → APKEditor decode → `inject.py` → javac+d8 → APKEditor
build → `repack.py` → zipalign → apksigner. Ký bằng keystore riêng, **phải giữ nguyên
keystore** để update đè lên bản đã cài (không mất đăng nhập).

## Cài

```powershell
adb install -r work\ig-hx-signed.apk   # cùng key => giữ data
adb logcat -s HX-VO:V                  # xem hook chạy
```

Nếu máy chặn (`INSTALL_FAILED_USER_RESTRICTED`): `adb shell settings put global adb_install_need_confirm 0`.

## Trạng thái

- [x] Hook view-once photo → PNG
- [x] Hook view-once video → MP4 (URL CDN, tải trực tiếp)
- [x] Xác định điểm chặn quay màn hình: `LX/0SRR.<init>` gọi `SurfaceView.setSecure(true)`
- [ ] Patch `setSecure(true)` → `false` (quay màn hình video không còn đen)
- [ ] Tắt `registerScreenCaptureCallback` (bỏ thông báo "đã chụp màn hình")
- [ ] Port sang 457.x

Chi tiết kỹ thuật, anchor, gotcha: xem [`COMPACT.md`](COMPACT.md).
