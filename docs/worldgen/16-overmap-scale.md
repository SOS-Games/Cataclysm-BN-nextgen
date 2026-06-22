# 16 — Overmap scale and performance (W10)

Grow overmap from **16×16** toward **BN 180×180** with acceptable editor and generation
performance.

**Status:** done. See [v2-implementation-plan](./v2-implementation-plan.md).

**Prerequisite:** W7 visit quality on 16×16 — do not scale before building visit works.

---

## Purpose

v1 editor cycles sizes in `MapEditorScreen.cycleOvermapSize` (typically 8 → 12 → 16). BN default
overmap is **180×180** OMT cells (`OVERMAP_SIZE` in C++).

W10 makes large overmaps **generatable**, **navigable**, and **visitable** without UI stalls.

---

## Current costs (v1)

| Operation | Complexity | Notes |
| --- | --- | --- |
| `BaseTerrainFiller.fill` | O(w×h) | cheap |
| `CityPlacer` / specials | O(quota × candidates × footprint) | grows with area |
| `RiverGenerator.carve` | O(w) path length | one river |
| `HighwayGenerator` | O(sites²) MST | few sites on small maps |
| `drawOvermapGrid` | O(w×h) per frame | **bottleneck** at 180×180 |
| `SubmapCache` | 64 entries LRU | independent of overmap size |

At 180×180 = 32,400 cells: ~32k string ids in `OvermapGrid.cells[]` — ~1–3 MB — acceptable on
desktop. **Rendering** all cells each frame is not.

---

## Targets

| Metric | v1 | W10 goal |
| --- | --- | --- |
| Default generate size | 8–16 | 64 for dev; 180 optional |
| Generate 64×64 | n/a | < 3s typical (Java, BN data) |
| Generate 180×180 | n/a | < 15s or progress indicator |
| Overmap pan/zoom | OK at 16 | 60fps at 64 with culling |
| Submap cache | 64 fixed | 128–256 configurable |
| Volume cache (W7) | n/a | 16–32 buildings |

---

## `OvermapGrid` storage

v1 (`OvermapGrid.java`):

```java
private final String[] cells;  // width * height, row-major
```

| Size | Cells | Storage |
| --- | --- | --- |
| 16×16 | 256 | trivial |
| 64×64 | 4,096 | ~few hundred KB |
| 180×180 | 32,400 | OK dense |

**Decision:** keep dense array through 180×180 unless profiling shows GC pressure on `String`
churn during regen.

Optional later: `String[]` intern pool for common ids (`field`, `forest`, `river_center`).

---

## Editor: viewport culling (required)

`MapEditorScreen.drawOvermapGrid` currently iterates all cells.

**Target:**

```text
computeVisibleOmtRange(camera, tilePx):
    firstCol ← max(0, floor(cameraX / tilePx))
    lastCol  ← min(width, ceil((cameraX + viewportW) / tilePx))
    firstRow ← max(0, floor(cameraY / tilePx))
    lastRow  ← min(height, ceil((cameraY + viewportH) / tilePx))

for y in firstRow .. lastRow-1:
    for x in firstCol .. lastCol-1:
        draw cell tint
```

Submap mode already computes `firstCol`/`lastCol` for terrain — **reuse same helpers** for
overmap mode.

### OMT label / hover

Only format hover cell (`formatOvermapCursorCell`) — do not draw text per cell at scale.

---

## Generation optimizations

| Technique | Where | Priority |
| --- | --- | --- |
| Viewport culling | `drawOvermapGrid` | **P0** |
| Cap mutable assembly attempts | `MutableSpecialPlacer` | P1 |
| Skip rivers if w×h < 32 | `RiverGenerator` | already informal |
| Parallel fill | `BaseTerrainFiller` | defer — measure first |
| Spatial index for placement | city/special search | defer until 180×180 placement slow |

### `OvermapGenerateOptions` presets

```java
public static OvermapGenerateOptions forSize(final int width, final int height) { … }

public static OvermapGenerateOptions preview64() {
    return forSize(64, 64);
}

public static OvermapGenerateOptions bnScale() {
    return forSize(180, 180)
        .withQuotas(reducedCityQuota, reducedSpecialQuota, 1);
}
```

Reduce quotas at 180×180 — BN does not place thousands of cities on one screen.

Suggested 180×180 quotas:

| Setting | Value |
| --- | --- |
| `cityBuildingQuota` | 8–12 |
| `staticSpecialQuota` | 2–4 |
| `mutableSpecialQuota` | 1 |

---

## Caching

### Submap cache

```java
// WorldgenPreviewService.java
private static final int DEFAULT_CACHE_SIZE = 64;

public void setSubmapCacheCapacity(final int capacity);
```

LRU eviction — visiting many OMTs on large map should not grow unbounded.

### Volume cache (W7)

Cap at ~16 entries — each volume is heavy (multi-floor stitch).

### Regenerate invalidation

`generateOvermap` already calls `submapCache.clear()`. Also clear volume cache + placement index.

---

## Editor UX at scale

| Feature | W10 |
| --- | --- |
| `cycleOvermapSize` | Add 32, 64; optional 128/180 with confirm dialog |
| Regenerate `R` | Show `Generating…` for >32×32 |
| Min zoom | Existing 0.125× — enough to see 180×180 overview |
| Visit | unchanged — one cell at a time |

---

## Test plan

| Test | Assert |
| --- | --- |
| `OvermapGeneratorScaleTest` | 64×64 generate returns grid 64×64 |
| `OvermapGeneratorScaleTest` | all cells non-null id |
| `OvermapGridTest` | 180×180 alloc + random access |
| Manual | 64×64 pan/zoom smooth |
| Manual | visit corner cell after generate |

Optional benchmark (not CI): log ms for 64/180 generate.

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| 180×180 generate slow | Status spinner; no freeze (background thread — already used for load) |
| Placement quota unmet | Warnings (existing) |
| Out of memory | Catch `OutOfMemoryError`; suggest smaller size in dialog |

---

## Files to touch

| File | Change |
| --- | --- |
| `view/MapEditorScreen.java` | overmap culling; size cycle |
| `worldgen/generate/OvermapGenerateOptions.java` | `preview64`, `bnScale` |
| `worldgen/WorldgenPreviewService.java` | cache capacity API |
| `worldgen/submap/SubmapCache.java` | configurable max |

---

## Verification

1. 64×64 overmap mode pan/zoom feels responsive
2. Visit + cache work on cell (0,0) and (63,63)
3. Regenerate 64×64 completes without manual GC
4. 180×180 optional — generates or warns clearly

---

## Dependencies

| Requires | PR |
| --- | --- |
| W7 building visit | strongly recommended |
| W2 overmap render | done |
| W9 region fill | optional — noise fill cost similar |

---

## Out of scope

- Chunked/streaming overmap for infinite worlds
- Disk-backed overmap
- GPU instanced OMT rendering
