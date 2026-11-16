# Graph Report - Instagram-Revanced  (2026-11-16)

## Corpus Check
- 12 files · ~7,156 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 1 file(s) not represented in the graph (top: (none) 1)

## Summary
- 126 nodes · 197 edges · 11 communities (4 shown, 7 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 5 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `dbaa98a5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Saver
- sys
- COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)
- android.content.Context
- .clearSecure
- Instagram-Revanced — view-once media saver
- Prompt mẫu cho lần cập nhật Instagram sau

## God Nodes (most connected - your core abstractions)
1. `Saver` - 30 edges
2. `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` - 11 edges
3. `Instagram-Revanced — view-once media saver` - 7 edges
4. `Prompt mẫu cho lần cập nhật Instagram sau` - 6 edges
5. `main()` - 6 edges
6. `3. Anchor thật của 455 (verify từ smali, không đoán)` - 6 edges
7. `main()` - 4 edges
8. `run()` - 4 edges
9. `Runnable` - 3 edges
10. `scan()` - 3 edges

## Surprising Connections (you probably didn't know these)
- `4. Cấu trúc APK & dex` --references--> `Saver`  [INFERRED]
  COMPACT.md → ext/app/hx/viewonce/Saver.java
- `9. Runbook khi Instagram ra bản mới` --references--> `Saver`  [INFERRED]
  COMPACT.md → ext/app/hx/viewonce/Saver.java
- `Cách hoạt động` --references--> `Saver`  [INFERRED]
  README.md → ext/app/hx/viewonce/Saver.java

## Import Cycles
- None detected.

## Communities (11 total, 7 thin omitted)

### Community 2 - "sys"
Cohesion: 0.17
Nodes (7): main(), scan(), find_methods(), main(), main(), run(), main()

### Community 3 - "COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)"
Cohesion: 0.12
Nodes (16): 10. Còn lại (chưa làm), 1. Mục tiêu, 2. Quyết định đã chốt (không mở lại), 3.1 Callback view-once, 3.2 Controller, 3.3 Callback của player video, 3.4 Secure surface (nguyên nhân quay màn hình đen), 3.5 FLAG_SECURE — hai cơ chế song song (+8 more)

### Community 9 - "Instagram-Revanced — view-once media saver"
Cohesion: 0.22
Nodes (7): Build, Bản đồ code, Cài, Cách hoạt động, Instagram-Revanced — view-once media saver, Không cần chế độ nhà phát triển để dùng, Trạng thái

### Community 10 - "Prompt mẫu cho lần cập nhật Instagram sau"
Cohesion: 0.29
Nodes (6): 4 ràng buộc cần thêm (lần trước thiếu), Bối cảnh, Cách tôi test, Prompt mẫu cho lần cập nhật Instagram sau, Ràng buộc không đổi, Việc cần làm lần này

## Knowledge Gaps
- **7 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Saver` connect `Saver` to `Saver.java`, `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)`, `android.content.Context`, `.clearSecure`, `Instagram-Revanced — view-once media saver`?**
  _High betweenness centrality (0.344) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `Saver` (e.g. with `4. Cấu trúc APK & dex` and `9. Runbook khi Instagram ra bản mới`) actually correct?**
  _`Saver` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Saver.java` be split into smaller, more focused modules?**
  _Cohesion score 0.06896551724137931 - nodes in this community are weakly interconnected._
- **Why does `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` connect `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` to `Instagram-Revanced — view-once media saver`?**
  _High betweenness centrality (0.132) - this node is a cross-community bridge._
- **Should `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` be split into smaller, more focused modules?**
  _Cohesion score 0.125 - nodes in this community are weakly interconnected._