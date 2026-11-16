# Graph Report - Instagram-Revanced  (2026-11-16)

## Corpus Check
- 11 files · ~6,732 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 1 file(s) not represented in the graph (top: (none) 1)

## Summary
- 119 nodes · 191 edges · 10 communities (4 shown, 6 thin omitted)
- Extraction: 97% EXTRACTED · 3% INFERRED · 0% AMBIGUOUS · INFERRED: 5 edges (avg confidence: 0.95)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `537858d1`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Saver
- sys
- COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)
- android.content.Context
- .clearSecure
- Instagram-Revanced — view-once media saver

## God Nodes (most connected - your core abstractions)
1. `Saver` - 30 edges
2. `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` - 11 edges
3. `Instagram-Revanced — view-once media saver` - 7 edges
4. `3. Anchor thật của 455 (verify từ smali, không đoán)` - 6 edges
5. `main()` - 6 edges
6. `main()` - 4 edges
7. `run()` - 4 edges
8. `Runnable` - 3 edges
9. `scan()` - 3 edges
10. `main()` - 3 edges

## Surprising Connections (you probably didn't know these)
- `4. Cấu trúc APK & dex` --references--> `Saver`  [INFERRED]
  COMPACT.md → ext/app/hx/viewonce/Saver.java
- `9. Runbook khi Instagram ra bản mới` --references--> `Saver`  [INFERRED]
  COMPACT.md → ext/app/hx/viewonce/Saver.java
- `Cách hoạt động` --references--> `Saver`  [INFERRED]
  README.md → ext/app/hx/viewonce/Saver.java

## Import Cycles
- None detected.

## Communities (10 total, 6 thin omitted)

### Community 1 - "Saver"
Cohesion: 0.15
Nodes (3): 5. Extension (`ext/app/hx/viewonce/Saver.java`), Saver, Runnable

### Community 2 - "sys"
Cohesion: 0.17
Nodes (7): main(), scan(), find_methods(), main(), main(), run(), main()

### Community 3 - "COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)"
Cohesion: 0.14
Nodes (14): 10. Còn lại (chưa làm), 1. Mục tiêu, 2. Quyết định đã chốt (không mở lại), 3.1 Callback view-once, 3.2 Controller, 3.3 Callback của player video, 3.4 Secure surface (nguyên nhân quay màn hình đen), 3.5 FLAG_SECURE — hai cơ chế song song (+6 more)

### Community 9 - "Instagram-Revanced — view-once media saver"
Cohesion: 0.22
Nodes (7): Build, Bản đồ code, Cài, Cách hoạt động, Instagram-Revanced — view-once media saver, Không cần chế độ nhà phát triển để dùng, Trạng thái

## Knowledge Gaps
- **6 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Saver` connect `Saver` to `Saver.java`, `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)`, `android.content.Context`, `.clearSecure`, `Instagram-Revanced — view-once media saver`?**
  _High betweenness centrality (0.386) - this node is a cross-community bridge._
- **Are the 3 inferred relationships involving `Saver` (e.g. with `4. Cấu trúc APK & dex` and `9. Runbook khi Instagram ra bản mới`) actually correct?**
  _`Saver` has 3 INFERRED edges - model-reasoned connections that need verification._
- **Should `Saver.java` be split into smaller, more focused modules?**
  _Cohesion score 0.07692307692307693 - nodes in this community are weakly interconnected._
- **Why does `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` connect `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` to `Instagram-Revanced — view-once media saver`, `android.content.Context`, `Saver`?**
  _High betweenness centrality (0.148) - this node is a cross-community bridge._
- **Should `COMPACT — trạng thái & tri thức kỹ thuật (Instagram 455.0.0.51.72)` be split into smaller, more focused modules?**
  _Cohesion score 0.14285714285714285 - nodes in this community are weakly interconnected._