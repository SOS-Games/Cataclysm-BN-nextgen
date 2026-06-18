# 13 — Building bundle sources

Where BN groups multiple mapgen pieces into one building, what nextgen loads today, and what
remains for future PRs.

---

## Purpose

Mapgen preview **P5–P6** imports **building bundles** (multi-floor and/or multi-OMT) instead of
a single `om_terrain` row. BN encodes those relationships in several JSON types and conventions —
not only `city_building`.

This unit is the **inventory and gap analysis** for bundle discovery. Implementation lives in
[10](./10-city-building-loader.md), [11](./11-map-volume-and-floors.md), [12](./12-omt-stitch-composer.md),
and `OvermapSpecialBuildingLoader` / `CombinedFloorMapgenResolver`.

---

## BN bundle concepts (reference)

| Concept | Meaning |
| --- | --- |
| **OMT** | One 24×24 overmap terrain tile (`SEEX`×`SEEY` cells) |
| **Vertical stack** | Same `(x, y)` on a special, different `z` — floors |
| **Horizontal footprint** | Multiple `(x, y)` at one `z` — multitile wing layout |
| **Bundle** | Preview session: `MapVolume` with one `MapGrid` per distinct `z` (stitched per floor if multi-OMT) |

BN does **not** use one JSON type for all of this. The game stitches pieces at **worldgen** from
`city_building`, static `overmap_special`, nested `om_terrain` mapgen, and other rules below.

---

## Explicit metadata types

### `city_building`

**Path:** usually `data/json/overmap/multitile_city_buildings.json` (and mod `multitile*.json`).

```json
{
  "type": "city_building",
  "id": "house_09",
  "overmaps": [
    { "point": [ 0, 0, 0 ], "overmap": "house_09_north" },
    { "point": [ 0, 0, 1 ], "overmap": "house_09_roof_north" }
  ]
}
```

| `point` | Meaning |
| --- | --- |
| `[0]` | OMT offset X (east) |
| `[1]` | OMT offset Y (south) |
| `[2]` | Z-level |

**Nextgen:** `BuildingBundleScanner` (P7a) walks each mod's content tree for `city_building` and static
`overmap_special`. Previously only `overmap/multitile*.json` and `overmap_special/` subdirs were scanned.

**Gap (P7b+):** same-file / suffix heuristics for orphans without metadata.

---

### `overmap_special` (static)

**Paths:** `data/json/overmap/overmap_special/`, `overmap_mutable/`, mod copies.

Large layouts (farms, labs, malls) list many `{ point, overmap }` entries — horizontal spread
**and** vertical stacks.

Example — `Farm_2side` stack at one OMT column:

```text
[ 2, 1, 0 ] → 2farm_6
[ 2, 1, 1 ] → 2farm_6_1
[ 2, 1, 2 ] → 2farm_6_2
…
```

**Nextgen:** `BuildingBundleScanner` (P7a) indexes static specials from any mod JSON path. Vertical
stacks at one `(x, y)` still become synthetic bundles (id = ground-floor `om_terrain`).

**Does not import:** mutable / procedural specials. Multi-column static specials register **both** per-column stacks (e.g. `2farm_4`) **and** the whole special id (e.g. `Farm_2side`) when ≥2 OMT columns.

**Gap:** Innawoods-style `copy-from` stubs without `overmaps` arrays still produce no bundle.

---

### `overmap_special` (mutable / procedural)

**Subtype:** `"subtype": "mutable"` (anthills, procedural labs, etc.).

Uses an **`overmaps` object** (named nodes), `phases`, `joins`, `root` — not a `point` array.
Worldgen **assembles** a layout at runtime.

**Nextgen:** **not supported** — parser expects static `overmaps: [ … ]`. No bundle metadata.

**Future:** procedural preview, curated snapshots, or “import seed” — separate from P5–P6.

---

## Mapgen-level grouping (no bundle JSON)

### Nested `om_terrain` grid

One `type: mapgen` entry covers a full multitile floor:

```json
"om_terrain": [
  [ "apartments_con_tower_110", "apartments_con_tower_010" ],
  [ "apartments_con_tower_100", "apartments_con_tower_000" ]
]
```

**Nextgen:** `JsonMapgenParser` flattens ids for catalog index + keeps `OmTerrainGrid`.
`CombinedFloorMapgenResolver` matches grid to `city_building` pieces and runs **one** mapgen per
floor (not per-wing stitch).

---

### Same file, multiple mapgens

Example: `house_2story.json` — `house_2story_base`, `_second`, `_roof`, `_basement` as separate
entries in one array file.

**Nextgen:** bundled when listed in `city_building` / vertical `overmap_special`, or inferred (P7b)
from same-file shared-prefix mapgens with floor suffixes.

---

### Naming suffix conventions

Common floor suffixes (not a JSON type):

| Pattern | Example |
| --- | --- |
| `_roof` | `house_09_roof` |
| `_loft` | `2farm_loft_3` |
| `_basement` | `house_2story_basement` |
| `_1`, `_2`, … | `2farm_6_1`, `2farm_6_2` |

**Nextgen:** suffix grouping via P7b when mapgens share a file and prefix; otherwise only via
`city_building` / `overmap_special`.

---

## Composition features (not separate buildings)

These affect how a **single** mapgen looks, not picker bundle rows:

| Feature | BN role | Nextgen status |
| --- | --- | --- |
| `predecessor_mapgen` | Run outdoor/field mapgen under building | Deferred — [08](./08-v2-parity-roadmap.md) |
| `update_mapgen` / `map_extras` | Post-gen overlay on tile | Not run |
| Weighted mapgen / `weight` | Random **variant**, not another floor | First runnable json + warn |
| `method: builtin` / `lua` | C++ / Lua generator | Unsupported in picker |
| Rotation (`_north`, `rotation`) | Same layout, rotated | Suffix stripped for lookup; rows not rotated |
| Regional `t_region_*` | Region-specific ter/furn ids | Pass-through id (no resolve) |

---

## World-scale (out of preview scope)

| Type | Role |
| --- | --- |
| `overmap_terrain` | OMT typing, spawn weights, `mapgen` list — not bundle layout |
| `overmap_connection` | Roads/paths between tiles |
| Overmap / worldgen | Places specials on the map |

See [01-overview-and-scope](./01-overview-and-scope.md) and [08-v2-parity-roadmap](./08-v2-parity-roadmap.md).

---

## What nextgen loads today

```text
BuildingBundleScanner.load()   # P7a
    walk each mod content tree (*.json)
        → type: city_building
        → type: overmap_special (static overmaps[] only)
    OvermapSpecialBuildingLoader groups vertical stacks per (x,y)

MapVolumeBuilder.build()
    per z:
        if multi-piece floor:
            CombinedFloorMapgenResolver (nested om_terrain grid) → one mapgen
            else OmtStitchComposer (per-wing mapgen + blit)
        else single JsonMapgenRunner.run
```

| Source | Multi-floor | Multi-OMT same z | Import whole layout |
| --- | --- | --- | --- |
| `city_building` | ✓ | ✓ (P6 stitch or combined mapgen) | ✓ (footprint only) |
| `overmap_special` vertical stack | ✓ | — (single column) | — |
| Nested `om_terrain` mapgen | — | ✓ (per floor) | — |
| Mutable `overmap_special` | — | — | — |
| Same-file / suffix heuristic | ✓ (P7b) | — | — |

---

## Known gaps (prioritized)

### P7a — Broaden scans (high value) — **done**

| Gap | Example | Fix |
| --- | --- | --- |
| `city_building` not in `multitile*.json` | Arcana `regional_overlay.json`, DinoMod zoo | `BuildingBundleScanner` walks mod content |
| `overmap_special` non-standard path | Mod-root `specials.json` with `overmaps[]` | Same full-mod scan |

### P7b — Implicit bundles (medium) — **done**

| Gap | Fix |
| --- | --- |
| Mapgens in one file, shared prefix | `MapgenFileBundleInferrer` groups runnable json mapgens per file |
| `base` + `base_roof` without metadata | Floor suffix rules (`_base`, `_roof`, `_basement`, `_second`, …) |

Explicit `city_building` / claimed `om_terrain` always wins over inference.

### P7c — Whole static special (large) — **done**

| Gap | Fix |
| --- | --- |
| Import full `Farm_2side`, malls, labs | `SpecialLayoutImporter` + `SpecialLayoutFloorComposer` — combined ground mapgen, upper floors anchored to ground `om_terrain` cells |

### v2 — Mapgen + procedural

| Gap | See |
| --- | --- |
| `predecessor_mapgen`, `update_mapgen` | [08](./08-v2-parity-roadmap.md) |
| Mutable specials | New spec / worldgen slice |
| Builtin / lua mapgen | Unlikely in preview |

---

## Picker behavior (recap)

Bundled rows use `CityBuildingDefinition.isBundledBuilding()`:

```text
floorCount > 1  OR  hasMultiTileLayout()
```

- **Import building** — `MapVolumeBuilder` → floor switch `[` / `]`
- **Generate** — single mapgen row only when not bundled

Row label: building id + `(N floors, multi-tile WxH)` from [10](./10-city-building-loader.md).

---

## Planned Java types (extensions)

```text
mapgen/building/
  BuildingBundleScanner.java        # P7a — full mod JSON scan
  CityBuildingLoader.java           # parse city_building
  OvermapSpecialBuildingLoader.java # vertical stacks from static specials
  MapgenFileBundleInferrer.java     # P7b — same-file / suffix inference
  SpecialLayoutImporter.java        # P7c — full static special bundles
  SpecialLayoutFloorComposer.java   # P7c — per-floor mega-grid compose
```

---

## BN source reference

| Concern | Location |
| --- | --- |
| City buildings | `data/json/overmap/multitile_city_buildings.json` |
| Specials | `data/json/overmap/overmap_special/`, `overmap_mutable/` |
| Farm stacks | `overmap_special/specials.json` — `Farm_2side` |
| Nested om_terrain | `data/json/mapgen/apartment_con_new.json` |
| Mutable specials | `overmap_mutable/anthill.json`, `lab.json` |
| Mapgen run order | `src/mapgen.cpp` — `mapgen_function_json::generate` |
| OMT placement | `src/mapbuffer.cpp`, `src/mapgen.cpp` |

---

## Verification

1. `house_09` — `city_building`, 3 floors, picker collapsed
2. `2farm_6` — `overmap_special` stack, ≥4 floors, not in `city_building`
3. `apartments_con_new` — nested `om_terrain`, multi-OMT + multi-floor
4. Arcana / mod-root specials — **pass** after P7a (`house_arcana`, `test_regional_house` fixtures)
5. `Anthill` mutable special — remains unsupported (no false bundle)

---

## Related

- [09-building-bundles-overview](./09-building-bundles-overview.md) — P5–P6 user flows
- [10-city-building-loader](./10-city-building-loader.md) — `city_building` scan
- [11-map-volume-and-floors](./11-map-volume-and-floors.md) — `MapVolume`
- [12-omt-stitch-composer](./12-omt-stitch-composer.md) — per-wing stitch + combined floor
- [08-v2-parity-roadmap](./08-v2-parity-roadmap.md) — mapgen feature deferrals
