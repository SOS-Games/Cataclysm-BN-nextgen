# 24 — CDDA layout gaps (overmap generation)

Remaining differences between **BN `overmap::generate` layout** and **nextgen
`OvermapGenerator`** after W11 + W14.

**Status:** draft

**Parent:** [23-cdda-parity-overview](./23-cdda-parity-overview.md)

---

## Purpose

Layout gaps explain symptoms like:

- One river across an otherwise dry map
- `test_road_*` / highway segments with no town around them
- Few or no shop/park OMTs despite a “city” region
- No subways, rails, or ocean tiles

Visit-time stitching (W13) does **not** fix these — they are OMT **placement** gaps.

---

## Generate order (detailed)

| Step | BN function | Nextgen equivalent | Status |
| --- | --- | --- | --- |
| 1 | `populate_connections_out_from_neighbors` | — | **Missing** |
| 2 | `place_rivers` (+ neighbor stitch) | `RiverGenerator.carve` | **Partial** |
| 3 | `place_lakes` | `LakeGenerator.fill` | **Done** (W11b) |
| 4 | `place_forests` | `BaseTerrainFiller` | **Done** (W9) |
| 5 | `place_swamps` | `SwampGenerator.fill` | **Partial** (W14c — region pass, not full BN swamp graph) |
| 6 | `place_cities` | `CityPlacer.placeAll` | **Partial** — see § Cities |
| 7 | `place_forest_trails` | — | **Missing** |
| 8 | `place_roads` | `HighwayGenerator.connectSites` | **Partial** — see § Roads |
| 9 | `place_specials` | `RegionSpecialPlacer` + `StaticSpecialPlacer` + `MutableSpecialPlacer` | **Partial** — see § Specials |
| 10 | `place_forest_trailheads` | — | **Missing** |
| 11 | `polish_rivers` | — | **Missing** |
| 12 | `place_mongroups` / `place_radios` | — | **Out of scope** (gameplay) |

**Nextgen call order** (`OvermapGenerator.java`):

```text
BaseTerrainFiller
→ LakeGenerator → RiverGenerator
→ ThickForestGenerator → SwampGenerator → BeachGenerator
→ RegionSpecialPlacer
→ CityPlacer
→ StaticSpecialPlacer
→ MutableSpecialPlacer
→ HighwayGenerator.connectSites(placedSites)
```

Legacy order (`options.isLegacyGenerationOrder()`): river before lakes; no W14 terrain passes.

---

## Cities (`place_cities`)

### What BN does

`overmap::place_cities` builds **urban areas** from the region `city` block:

- Picks city centers with spacing / size from `city_size`, `city_spacing`
- Fills blobs with **houses** (weighted `city_building` or OMT ids)
- Places **shops**, **parks**, **finales** from separate weight tables
- Lays local **road** grids inside the city (not the same as inter-city highways)
- Respects terrain, existing rivers, and placement failures with retries

### What nextgen does (W4 + W14b)

| Feature | Class | Behavior |
| --- | --- | --- |
| Urban site picking | `CitySitePicker` | Region `city_size` / `city_spacing` (W14b) |
| Building placement | `CityPlacer` | Weighted `city.houses` → multitile `city_building` bundles |
| Quota | `OvermapGenerateOptions` | `cityBuildingQuota` caps total drops |

### Gaps

| Gap | BN | Nextgen | Tier |
| --- | --- | --- | --- |
| Shop OMTs | `city.shops` weights | Not consumed | A / P0 |
| Park OMTs | `city.parks` | Not consumed | A / P0 |
| Finale structures | `city.finales` | Not consumed | A / P0 |
| In-city road grid | Part of `place_cities` | Only `HighwayGenerator` between **centers** | A / P0 |
| House density inside blob | Many small OMTs per city | Few large multitile buildings | A / P0 |
| Sigma / radius placement | BN city shape params | Site picker only | A / P1 |
| Faction camps / unique city features | BN specials batch | Not in city pass | A / P2 |

**User-visible symptom:** Overmap shows **isolated** `2storyModern01`-scale footprints, not a
BN-style town with gas stations, sidewalks, and house rows.

---

## Roads (`place_roads`)

### What BN does

`overmap::place_roads`:

- Connects **cities** and important sites with a **highway network**
- Uses directional `overmap_connection` templates (straight, tee, four_way, bridges)
- Continues roads from **neighbor overmaps** (north/east/south/west)
- Elevates bridges over rivers (`elevate_bridges` / polish passes)

### What nextgen does (W5 + W11c)

| Feature | Class | Behavior |
| --- | --- | --- |
| Site linking | `HighwayGenerator.connectSites` | Minimum spanning tree over `placedSites` |
| Connection templates | `OvermapConnectionRegistry` | Directional carve when registry loaded |
| Trigger | After all placements | Roads only where a site was already placed |

### Gaps

| Gap | BN | Nextgen | Tier |
| --- | --- | --- | --- |
| World-spanning highway graph | Yes | MST between sparse sites only | A / P0 |
| Roads **before** specials | BN order | Roads **after** everything | A / P1 (ordering) |
| Neighbor overmap continuity | `place_roads(n,e,s,w)` | Single isolated grid | C |
| Interchanges / highway junctions | BN road pass | Not implemented | A / P1 |
| Bridges over rivers | `elevate_bridges` | Not implemented | A / P1 |
| Rural roads / field paths | BN connection phases | Not implemented | A / P2 |

**User-visible symptom:** Short road chains between two building centers in empty forest — no
grid, no approach to a “city core”.

---

## Hydrology

| Feature | BN | Nextgen | Status |
| --- | --- | --- | --- |
| Lake noise | `place_lakes` | `LakeGenerator` | **Done** |
| River carving | Multiple paths, neighbor-aware | Single `RiverGenerator.carve` | **Partial** |
| `polish_rivers` | Smoothing, junction cleanup | — | **Missing** |
| Ocean / coastal OMTs | BN world edge | — | **Missing** |
| River ↔ road bridges | Polish + elevation | — | **Missing** |

---

## Terrain passes (forest, swamp, beach)

| Feature | BN | Nextgen | Status |
| --- | --- | --- | --- |
| Base forest/field noise | `place_forests` | `BaseTerrainFiller` | **Done** |
| Thick forest | Region thresholds | `ThickForestGenerator` (W14c) | **Partial** |
| Swamp | `place_swamps` | `SwampGenerator` (W14c) | **Partial** |
| Beach / sand | Coastal logic | `BeachGenerator` (W14c) | **Partial** — no ocean anchor |

W14c adds **region-driven upgrades** on top of base fill; BN swamp/beach placement is tied to
hydrology and world position more deeply.

---

## Specials (`place_specials`)

### What BN does

- Builds `overmap_special_batch` from region `overmap_special_settings`
- Enforces **city_sizes**, distance between specials, overmap bounds
- Multiple **phases** (before/after cities, faction batches)
- Hundreds of weighted ids per region profile

### What nextgen does

| Pass | Class | Notes |
| --- | --- | --- |
| Region-weighted | `RegionSpecialPlacer` (W14a) | Min/max + weights from region |
| Quota fallback | `StaticSpecialPlacer` | `OvermapGenerateOptions.staticSpecialQuota` |
| Procedural | `MutableSpecialPlacer` (W6/W11a) | Multi-phase labs etc. |

### Gaps

| Gap | BN | Nextgen | Tier |
| --- | --- | --- | --- |
| `city_sizes` constraint | Per-special | Not enforced | A / P1 |
| Min distance between specials | BN batch rules | Simple collision only | A / P1 |
| Phase ordering vs cities/roads | Strict BN order | Region specials before cities; roads last | A / P1 |
| Faction / railroad specials | Separate batches | Partial via region table | A / P2 |
| Density at 180×180 | Full tables | Quota caps for performance | A / P1 |

---

## Subways, rails, sewers (W14d deferred)

BN runs separate **connection graph** phases for underground networks, often after surface layout.

| Network | BN | Nextgen |
| --- | --- | --- |
| Subways | `overmap_connection` + carve passes | **Missing** (W14d) |
| Rails | Region railroad specials + tracks | **Missing** |
| Sewers | Linked to cities | **Missing** |

See [20-layout-parity-phase2](./20-layout-parity-phase2.md) — W14d explicitly deferred to v4 or
later.

---

## Neighbor overmaps

| Feature | BN | Nextgen |
| --- | --- | --- |
| Multi-overmap world | `overmapbuffer` — many 180×180 | One grid per editor session |
| Stitch rivers/roads at boundary | `place_rivers/roads(n,e,s,w)` | **Missing** |
| Connection cache | `overmap_connection_cache` | **Missing** |

Single-overmap preview is **v3 in scope**; cross-overmap continuity is Tier C.

---

## Suggested implementation slices (post-W15)

| PR slice | Delivers | Depends on |
| --- | --- | --- |
| **W17a** | `city.shops` / `city.parks` inside urban blobs | W14b site picker |
| **W17b** | In-city road grid (subset of `place_cities`) | W17a |
| **W17c** | `place_roads` v2 — city-to-city highways, not MST-only | W17b |
| **W17d** | Multi-river + `polish_rivers` lite | W11b hydrology |
| **W17e** | Forest trails + trailheads | Region `forest_trail` |
| **W17f** | W14d subways/rails | Connection registry + carve |

These are **proposed** ids — not yet in [v3-implementation-plan](./v3-implementation-plan.md).
Add a v4 plan when Tier A is approved.

---

## Java reference (current)

| Concern | Class |
| --- | --- |
| Orchestration | `worldgen/generate/OvermapGenerator.java` |
| Cities | `CityPlacer`, `CitySitePicker` |
| Roads | `HighwayGenerator` |
| Rivers / lakes | `RiverGenerator`, `LakeGenerator` |
| Region specials | `RegionSpecialPlacer` |
| Static / mutable | `StaticSpecialPlacer`, `MutableSpecialPlacer` |
| Terrain passes | `BaseTerrainFiller`, `SwampGenerator`, `BeachGenerator`, `ThickForestGenerator` |
| Options | `OvermapGenerateOptions` |
| Export for diff | `worldgen/overmap/OvermapGridExporter.java` |

---

## Verification

1. Each BN `place_*` function maps to done / partial / missing
2. City and road sections explain the “roads in wilderness” symptom
3. W14d deferral cross-links [20](./20-layout-parity-phase2.md)
4. Proposed W17 slices are optional — document only until plan approved
