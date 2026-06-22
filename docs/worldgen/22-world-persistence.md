# 22 — World persistence (W16, deferred)

Save and load a **world session** to disk — overmap layout, exploration state, and optional
submap cache.

**Status:** deferred. **Not in v3 phase 1.** See [18-world-map-v3-roadmap](./18-world-map-v3-roadmap.md).

Implement only after [21-exploration-and-world-coords](./21-exploration-and-world-coords.md) (W15)
defines the in-memory `WorldgenSession` model.

---

## Purpose

v3 phase 1 (W13–W15) keeps all state in RAM. W16 adds:

```text
File → Save World…  →  world.json (+ optional cache/)
File → Open World…  →  restore session → continue exploration
```

This is **not** BN `.sav2` interop in v1 of W16.

---

## Planned contents (sketch)

| Field | Source |
| --- | --- |
| `version` | Format version |
| `seed`, `regionId` | `WorldgenSession` |
| `overmap` | `OvermapGrid` serialisation |
| `placements` | `PlacedBuildingIndex` |
| `exploration` | seen / visited bitsets |
| `position` | `WorldCoord` |
| `submapCache` | optional; gzip JSON per cell |

---

## Why deferred

| Reason | Detail |
| --- | --- |
| Model churn | W15 `WorldgenSession` should stabilise first |
| Cache size | Full submap cache on disk can be hundreds of MB |
| Product priority | Visit + layout + exploration first |

---

## Success criterion (when started)

Save 64×64 world with 10 visited OMTs → quit editor → reload → same grid, visited flags, cache
hits on revisit.

---

## Out of scope (W16 v1)

| Topic | Notes |
| --- | --- |
| `.sav2` read/write | Separate project |
| Mod list validation on load | Warn only |
| Incremental / autosave | Editor polish |

---

## Related

- [v3-implementation-plan](./v3-implementation-plan.md) — W16 section
- [map-editor map file format](../map-editor/04-map-file-format.md) — single-grid JSON; may inform world format
