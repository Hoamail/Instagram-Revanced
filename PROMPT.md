# Prompt mẫu cho lần cập nhật Instagram sau

Copy nguyên khối dưới, thay `<...>`, gửi cho agent. Mục đích: giữ những ràng buộc đã
đúng ở lần trước, và thêm đúng 4 ràng buộc đã thiếu (in đậm).

---

## Bối cảnh

- Repo: `C:\adb\instagram-re` (làm việc) và `C:\adb\Instagram-Revanced` (bản phát hành).
- Đọc `COMPACT.md` trước, đặc biệt §3 (anchor), §9 (runbook), §10 (việc còn lại).
- Target: Instagram `<version>` (`com.instagram.android`), arm64, Android `<api>`, không root.
- APK gốc: `<đường dẫn>` (đã tải sẵn; nếu chưa có thì nói tôi tải, đừng tự đi tìm mirror).

## Ràng buộc không đổi

1. **Giữ login**: luôn `adb install -r` cùng `hx.keystore`; không uninstall, không xoá data.
2. Không sửa file ngoài `instagram-re/` và `Instagram-Revanced/`.
3. Chỉ tin dữ liệu máy (`dumpsys`, `logcat`, file trên `/sdcard`), không tin log của script.
4. Trước khi cài: build xong phải tự verify artefact, không đẩy việc đó cho tôi.

## 4 ràng buộc cần thêm (lần trước thiếu)

5. **Kiểm chứng patch bằng bytes, không bằng thông báo.** Sau mỗi patch, decode lại
   dex vừa build (hoặc đọc lại đúng dòng trong tree) và in ra dòng trước/sau. Nếu chuỗi
   không đổi thì dừng và báo lỗi, không được build tiếp.
6. **Lập bảng inventory đầy đủ trước khi patch.** Liệt kê *tất cả* site setter kèm
   `file:line`, method, opcode và 3 dòng ngữ cảnh; đưa bảng cho tôi xem; chỉ patch khi
   bảng đó đã đủ. Không patch theo danh sách cũ rồi mới đi tìm site bị sót.
7. **Đo trước, đoán sau.** Nếu có ≥2 nghi phạm cho cùng một hành vi, build **một** bản
   instrumentation ghi log (kèm stack trace) tại tất cả nghi phạm, xem cái nào chạy thật,
   rồi mới patch. Không patch-thử-từng-cái rồi đẩy cho tôi test.
8. **Luật dừng**: sau 2 giả thuyết sai liên tiếp, không được thử giả thuyết thứ 3 bằng
   cách build lại — phải chuyển sang instrumentation hoặc chứng minh bằng đọc code.

## Việc cần làm lần này

- `<mô tả ngắn: ví dụ "port anchor sang bản mới, giữ nguyên 3 tính năng đang có">`
- Khi xong: cập nhật `COMPACT.md` (anchor mới + post-mortem nếu có chướng ngại mới),
  `README.md` (trạng thái), chạy `graphify update .`, commit + push.

## Cách tôi test

- Tôi tự mở app và test tay. Chỉ nhờ tôi test khi bản build đã qua bước 5 và 7.
- Nếu cần tôi test, nói rõ: mở màn hình nào, làm gì, giữ nguyên bao lâu.
