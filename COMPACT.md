# COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)

Bản nén của toàn bộ quá trình RE. Đọc file này là đủ để tiếp tục mà không cần đọc lại APK.

---

## 1. Mục tiêu

Tự lưu media **view-once trong DM** (ảnh + video) ra bộ nhớ máy, trên APK Instagram do
mình build/ký lại. Mở rộng: chặn `FLAG_SECURE` / secure surface, hiểu cách IG phát hiện
chụp màn hình, và port sang bản mới.

Thiết bị: Xiaomi 2510EPC8BG, Android 16 (API 36), arm64-v8a, PAGE_SIZE 4096,
bootloader `verifiedbootstate=green` (**không root**), adb qua wireless debugging (mdns,
port đổi mỗi lần bật lại).

## 2. Quyết định đã chốt (không mở lại)

| Vấn đề | Chốt |
|---|---|
| Version dev/target | **455.0.0.51.72** — đã cài, đã pull; 416 (daboynb test) dễ bị forced-update; 455 sát bản HushGram verify (454) |
| Cơ chế | Hook callback "media loaded" và lưu bytes, **không** dùng đường bitmap-only của daboynb |
| `RAVEN_VIEW_MODEL` int-enum remap | Bỏ — byte-scan 455 = MISS, tính năng đã chết |
| Lưu file | `MediaStore` + `RELATIVE_PATH=Pictures/InstagramViewOnce` + `IS_PENDING` (Android 16 / targetSdk 37 chặn `FileOutputStream` vào Pictures) |
| Lookup field/class | BFS reflection theo **fragment tên class/type**, không hardcode tên obfuscated |
| Toolchain | APKEditor 1.4.9 (decode/build smali), jadx 1.5.6 (chỉ `--single-class`), build-tools 36.0.0 (`d8`, `zipalign`, `apksigner`), `platforms/android-36/android.jar` |
| Đóng gói | 1 APK base duy nhất; splits (`executorch`, `pytorch`) **không cần** — manifest không khai `splits.required` |
| Ký | keystore riêng, giữ nguyên để update đè (không mất login) |

## 3. Anchor thật của 455 (verify từ smali, không đoán)

### 3.1 Callback view-once

```
X/0lJh  .implements LX/0Elt;
  .field A00:I           (index)
  .field A01:LX/0b4b;    (wrapper nội dung: A04 ImageUrl, A09 Media, A0J/A0M/A0O String)
  .field A02:LX/0iUl;    (danh sách message)
  .field A03:LX/0lt9;    (A00 -> DirectVisualMessageViewerController)
  .field A04:Z
  Fdl(Ljava/lang/String;Ljava/lang/Integer;)V   -> nhánh LỖI, chứa "photo_cant_load"
  Fs7(LX/00n3;)V                                -> nhánh THÀNH CÔNG  ← chèn hook
```

Đăng ký listener: `DirectVisualMessageViewerController.smali:3679`
`invoke-virtual {v7, v8, v5}, Lcom/instagram/common/ui/widget/imageview/IgProgressImageView;->A0C(LX/0Elt;I)V`
với `v8 = new-instance LX/0lJh;`.

Hook (instruction đầu tiên của `Fs7`, ngay sau `.locals 4`):

```smali
invoke-static {p0, p1}, Lapp/hx/viewonce/Saver;->onMediaLoaded(Ljava/lang/Object;Ljava/lang/Object;)V
```

### 3.2 Controller

FQN 455: `instagram.features.direct.visual.internal.DirectVisualMessageViewerController`
(daboynb hardcode `com.instagram.direct.visual.DirectVisualMessageViewerController` rồi
`.equals()` ⇒ **luôn null** trên ≥451 ⇒ patch đó vô dụng).

Field không obfuscated còn tồn tại: `contentHolder:LX/0ix0;`, `videoPlayer:LX/0mUc;`,
`viewerContainer`, `backgroundDimmer`, `photoTimerController`, `screenshotDetector`
(`A0O:LX/0AA3;`, attach = `H5Z(LX/02jN;)`).

`LX/0ix0` (contentHolder): `A0H:IgProgressImageView`, `A0R:CircularImageView`,
`A02..A05:View`, `A0G:UserSession`.

### 3.3 Callback của player video

`LX/0mUc` (`videoPlayer`) implements listener của video player: `GgV`, `GgX`, `GRn`,
`Ghe`, `GhH`, `GhY`, `Gg7(LX/07lE;Ljava/lang/String;)V` (=`return-void`).
`LX/07lE.A03` chứa `LX/0lWl` (`.A00:LX/0b4b`). `A0i/A0j(String)` chỉ là analytics
(`"start"`, `"tapped"`, `"bottom_sheet"`), **không** phải URL.

### 3.4 Secure surface (nguyên nhân quay màn hình đen)

```
X/0SRR.<init>(Landroid/view/SurfaceView;)V
    invoke-virtual {p1, v2}, Landroid/view/SurfaceView;->setSecure(Z)V     // v2 = 1
    invoke-virtual {p1, v2}, Landroid/view/SurfaceView;->setZOrderOnTop(Z)V
    ... setFormat(-2), setFixedSize(1,1) ...
```

`0SRR` implements `SurfaceHolder$Callback2` + `OnAttachStateChangeListener`, field
`A03:SurfaceView`, `A04:OnPreDrawListener` (lambda `LX/0RG4` với `$t=6`).
Patch dự kiến: chèn `const/4 v0, 0x0` ngay trước `setSecure` và đổi thanh ghi thành `v0`
(v0 chưa dùng tại điểm đó, bị ghi lại ngay sau bằng `const/4 v0, -0x2`).

Quan sát thực tế trên máy: **chụp màn hình vẫn lưu được** (IG chỉ hiện toast "Không thể
chụp màn hình" + bắn analytics) ⇒ màn hình này **không** set `FLAG_SECURE`; chỉ **video**
dùng secure surface nên quay màn hình ra đen.

### 3.5 FLAG_SECURE — hai cơ chế song song

**(a) Registry `X/09Os` — cơ chế chính, theo window** (dex `classes4`)

```java
final class 09Os {                      // singleton qua LX/09Ot.A00()
    WeakHashMap<Window,Integer> A01;    // refcount theo window
    WeakHashMap<Window,List<String>> A00; // tên các "surface" đã đăng ký
    void A00(Window w);                 // reconcile: count>0 -> setFlags(0x2000,0x2000), count==0 -> clearFlags
    void A01(Window w, String name);    // register: nếu window chưa có cờ -> setFlags(0x2000,0x2000); count++
    void A02(Window w, String name);    // unregister: count--, count<=1 -> clearFlags
}
```

Log đặc trưng: `"Inconsistency in window FLAG_SECURE state detected!"`.

Caller đăng ký tên (đều nằm trong luồng DM/quicksnap):
`DirectVisualMessageViewerController` (classes21, 2 chỗ), `DirectVisualReplyFragment`
(classes14, `0N6T`), `ScreenshotNotificationManager` (classes4, `02Fy`), `0A2g`, `0bdC`.
⇒ Patch **2 site `setFlags`** trong `09Os` (`A00` và `A01`) là đủ cho phần lớn surface.

**(b) Set cờ trực tiếp** ở nhiều module (không qua registry):

| dex | file | method | kiểu |
|---|---|---|---|
| classes5 | `X/06GW` | `A04` | `setFlags(0x2000,0x2000)` trên window của FragmentActivity |
| classes13 | `X/0HXa` | `onResume` | `setFlags(0x2000,0x2000)` |
| classes13 | `X/0HYW` | `onResume` | `setFlags(0x2000,0x2000)` qua `requireWindow()` |
| classes13 | `X/0Hx7` | `A0F(Z)` | `addFlags(0x2000)` — **`QuickSnapUnifiedFragment` = Instants**, thủ phạm thật |
| classes16 | `X/0O6n` | `A03` | set nếu window chưa secure |
| classes16 | `X/0OKO` | `A04` | `setFlags(flags,0x2000)` (bật/tắt theo tham số) |
| classes16 | `X/0Vyh` | `A01` | `addFlags(0x2000)` |
| classes27 | `X/0bKa` | `A04` | như `0OKO` |
| classes28 | `HonoluluBaseActivity` | `onCreate` | `setFlags(0x2000,0x2000)` — base của `CameraActivity`/`CameraShortcutActivity` |
| classes29 | `X/0prN` | `A00` | refcount/window qua `0qlI`, `addFlags(0x2000)` |
| classes21 | `X/0R8Q` | `A00`/`A01` | quicksnap manager (dùng bởi `X/0R84` ← `X/05g6`, quicksnap consumption VM) |
| classes21 | `X/0RG4` | `onPreDraw` `$t=6` | bản sao logic của `0R8Q` |
| classes21 | `X/0mkM` | `run` | `setFlags(0x2000,0x2000)` |
| classes21 | `X/0SRR` | `<init>` | **`SurfaceView.setSecure(true)`** ← nguyên nhân quay màn hình video ra đen |

Patch chuẩn: `const/16 vX, 0x2000` → `const/4 vX, 0x0` tại đúng dòng set
(không đụng các dòng `clearFlags`/`and-int` kiểm tra). Riêng `0OKO`/`0bKa` thì
`setFlags(0x2000 → 0, mask 0x2000)` = clear, vẫn đúng ý.

`flagscan.py` quét 32 dex: **16 setter site** (`const`/`or-int` + `setFlags`/`addFlags`/
`iput flags`), gồm 2 site `androidx.compose.ui.window.PopupLayout` chỉ *kế thừa*
FLAG_SECURE từ window cha (bỏ qua). Các dòng `clearFlags`/`and-int` không patch.

Cạm bẫy đã dính: lần quét đầu chỉ lọc `const` + window-API trong ±14 dòng nên bỏ sót
`0Hx7:1787` (`addFlags` cách `const` 8 dòng); và `patch_dex.py` bản đầu hard-code
`const/16 v0` nên **âm thầm không patch** dòng dùng `v1`. Đã đổi sang regex
`const/16 (v\d+), 0x2000`. Sau mỗi lần patch phải kiểm tra log in ra `const/4 ...`.

**(c) Guard per-frame** (lưới an toàn, `classes28`): `TransparentModalActivity.onCreate`
gọi `Lapp/hx/viewonce/Saver;->guardWindow(Landroid/app/Activity;)V`; hàm này clear
`FLAG_SECURE` và đăng ký `OnPreDrawListener` clear lại trước mỗi frame. Log
`HX-VO: cleared FLAG_SECURE` chỉ xuất hiện khi thực sự phải can thiệp (im lặng = không
setter nào chạy).

Sự thật đo được trên máy (455.0.0.51.72, Android 16):
- DM view-once: chụp màn hình vốn đã lưu được (chỉ có toast giả).
- Instants: `fl=... SECURE ...` trên window `com.instagram.modal.TransparentModalActivity`
  — do `0Hx7.A0F(Z)`. Sau khi patch `0Hx7:1787` + guard, `dumpsys` chỉ còn
  `fl=LAYOUT_IN_SCREEN LAYOUT_INSET_DECOR SPLIT_TOUCH HARDWARE_ACCELERATED ...`
  (không còn `SECURE`) và chụp màn hình Instants chạy đúng.

## 4. Cấu trúc APK & dex

- `classes.dex` … `classes32.dex` (32 dex), `classes21.dex` là dex chứa `X/0lJh`.
- Thêm mới: **`classes33.dex`** (dex của extension `app.hx.viewonce.Saver`), nén deflate.
- `repack.py` giữ nguyên thứ tự entry / timestamp / compression / attr của APK gốc.
- APK sau khi chèn: 160 MB, 18 573 entry, `zipalign -p -f 4`, ký v1+v2+v3 bằng apksigner.

## 5. Extension (`ext/app/hx/viewonce/Saver.java`)

Luồng `onMediaLoaded(callback, model)`:

1. debounce 1.5 s (tránh lưu trùng khi callback bắn nhiều lần).
2. `postDelayed(900ms)` — đợi ảnh render xong.
3. `findController`: duyệt field của callback, match class name chứa
   `DirectVisualMessageViewerController` (2 tầng).
4. Lấy `Context` từ controller (field instanceof `Context`).
5. **Video trước**: BFS ≤7 tầng, tìm object có class chứa `VideoUrl`/`VideoVersion`,
   lấy String bắt đầu bằng `http` (hoặc method `getUrl`). Tải bằng `HttpURLConnection`
   (UA `Instagram 455.0.0.51.72 Android`), ghi vào MediaStore Video.
6. **Ảnh sau**: BFS ≤4 tầng từ `contentHolder` (rồi từ controller) thu **mọi**
   `ImageView`, bỏ `CircularImageView`/`IgSimpleAvatarView`, bỏ bitmap < 200 px,
   chọn **diện tích lớn nhất** → PNG 100 %.
7. Ghi qua `ContentResolver.insert` + `openOutputStream` + `IS_PENDING=0`.

Bài học: BFS "first BitmapDrawable" bắt nhầm avatar 72×72; phải chọn max area.
Video view-once vẫn đi qua `Fs7` (poster load) ⇒ một hook phục vụ cả hai.

## 6. Gotcha toolchain / môi trường

- `sdkmanager` (cmdline-tools 2026) từ chối chạy ("deprecated") → tải `android.jar` +
  build-tools thủ công.
- jadx full-dex treo > 20 min; chỉ dùng `--single-class 'Lcom/foo/Bar;'`.
- APKEditor decode **tất cả** dex: ~25 min; cắt stub 1 dex → ~40 s (build 8–37 s).
- APKEditor `b` từ chối ghi nếu file output đã tồn tại → `Remove-Item` trước.
- APKEditor decode cần APK hợp lệ, **không** nhận dex rời (`-i x.dex` → crash
  `ApkModule.loadApkFile`).
- PowerShell 5.1: không có if-as-expression; `Set-Content -Encoding utf8` sinh BOM làm
  javac báo `illegal character '\ufeff'` → dùng `[IO.File]::WriteAllText(..., UTF8Encoding($false))`;
  đường dẫn .NET phải tuyệt đối; `finalize` không dùng được làm tên static method
  (đổi thành `publish`); biến trong string phải `${s}` trước dấu `:`.
- MIUI/HyperOS: `adb install` thường → `INSTALL_FAILED_USER_RESTRICTED`; fix:
  `adb shell settings put global adb_install_need_confirm 0`, cài **1 APK** (không
  `install-multiple`).
- Không có `grep`/`rg` trên PATH → dùng Grep tool hoặc python.
- Logcat tag của extension: `HX-VO`.

## 7. Kết quả đã verify trên máy

```
I HX-VO: hit cb=X.0lJh model=X.00n3
I HX-VO: controller=instagram.features.direct.visual.internal.DirectVisualMessageViewerController
I HX-VO: photo 1280x2275 from com.instagram.common.ui.widget.imageview.IgImageView view=1280x2275
I HX-VO: saved photo 2401447B 1280x2275 -> content://media/external/images/media/1895
I HX-VO: video https://scontent.cdninstagram.com/o1/v/t2/f2/m549/...mp4?...&oe=6AFC9B92
I HX-VO: saved video 354540B -> content://media/external/video/media/1896
```

File: `/sdcard/Pictures/InstagramViewOnce/vo_<epoch>.png|mp4`. MP4 verify: `ftyp/isom`,
có `moov` + `mdat`.

## 8. Post-mortem: từng chướng ngại và cách vượt

| # | Chướng ngại | Cách phát hiện | Cách vượt |
|---|---|---|---|
| 1 | Cách của daboynb chết từ 451+ | `.equals()` với FQN cũ luôn null | Bỏ hardcode FQN; tìm controller bằng **fragment tên class** qua reflection BFS |
| 2 | Không có "ảnh đã load" event | đọc `DirectVisualMessageViewerController` | Hook `X/0lJh->Fs7(LX/00n3;)V` — callback của `IgProgressImageView.A0C` |
| 3 | `FileOutputStream` vào `Pictures` bị chặn (targetSdk 37) | SecurityException | Ghi qua **MediaStore** + `RELATIVE_PATH` + `IS_PENDING` |
| 4 | Lưu trùng 4 lần cùng 1 video | thấy 4 file `43548 B` giống nhau | Dedupe theo nội dung: URL cho video, fingerprint 8×8 cho ảnh |
| 5 | Chụp màn hình vẫn bị chặn sau khi patch 14 site | `dumpsys` còn `SECURE` | Quét lại **theo ngữ cảnh ±8 dòng** (không phải ±14) → ra `0Hx7.A0F(Z)` = `QuickSnapUnifiedFragment` |
| 6 | Patch "thành công" nhưng không có tác dụng | log in ra `const/16 v1, 0x2000` (không đổi) | `patch_dex.py` hard-code `v0`; đổi sang regex `const/16 (v\d+), 0x2000` |
| 7 | Quay màn hình ra đen | `0SRR.<init>` | `SurfaceView.setSecure(true)` → `(false)`; cộng thêm việc bỏ `FLAG_SECURE` |
| 8 | Sợ patch hỏng app | — | 8 dex build lại + cài đè cùng keystore → giữ login, không crash |

**Bài học chung:** mỗi lần "đã patch mà không có tác dụng" thì nguyên nhân luôn là một
trong ba: (a) site chưa được liệt kê, (b) patch không thực sự ghi vào file, (c) code
đó không được nạp. Phải kiểm chứng bằng dữ liệu máy (`dumpsys`, `logcat`), không tin log
của script.

## 9. Runbook khi Instagram ra bản mới

1. **Chuẩn bị**: lưu `hx.keystore` (mất là phải uninstall → mất login). Chụp restore point
   mới: APK gốc + `dumpsys` + sha256. (`restore-point\<ngày>\restore.ps1`.)
2. **Quét**: `python flagscan.py <apk>` để lấy mọi literal `0x2000`; rồi chạy bản quét
   ngữ cảnh (±8 dòng, có `setFlags`/`addFlags`/`iput flags`) để ra danh sách setter.
   Kiểm tra bằng mắt từng dòng trước khi patch.
3. **Hook view-once**: tìm class có tên chứa `DirectVisualMessageViewerController`, tìm
   `IgProgressImageView.A0C(LX/0Elt;I)` trong class đó → listener là `X/0lJh` (tên có thể
   đổi) → hook method `void (LX/00n3;)V` đầu tiên của nó. `Saver` không phụ thuộc gì khác.
4. **Instants**: tìm class có `__redex_internal_original_name = "QuickSnapUnifiedFragment"`
   → method có `addFlags(0x2000)` → patch `const/16 vX, 0x2000` thành `const/4 vX, 0x0`.
5. **Guard**: chèn `invoke-static {p0}, Lapp/hx/viewonce/Saver;->guardWindow(Landroid/app/Activity;)V`
   sau `invoke-super ... onCreate` trong `TransparentModalActivity`.
6. **Build**: `python patch_dex.py <stock.apk> <dex> <file:line> ...` cho từng dex đã sửa,
   compile lại `Saver.java` bằng javac+d8 (`build-patch.ps1`), rồi `.\assemble.ps1`.
7. **Verify**: `dumpsys window` không còn `SECURE`; `adb logcat -s HX-VO:V` có `hit cb=`;
   file mới trong `/sdcard/Pictures/InstagramViewOnce`; chụp + quay thử Instants.
8. Nếu anchor đổi: `inject.py`/`patch_dex.py` phải fail rõ ràng — không được im lặng bỏ qua
   (xem mục #6 ở trên).

## 10. Còn lại (chưa làm)

1. Tắt `registerScreenCaptureCallback` (`classes2/X/05uZ.1`) để bỏ thông báo
   "đã chụp màn hình" gửi cho người gửi.
2. Lưu **file gốc** của Instants (Compose `QuickSnapMediaViewerScreen`, model trong
   `QuickSnapConsumptionViewModel`) — hiện chỉ quay được màn hình, chưa lưu bitrate gốc.
3. Port sang 457.x.
