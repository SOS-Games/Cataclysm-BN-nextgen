# 20 — Layout parity phase 2 (W14)

Extend procedural overmap generation with **remaining BN `overmap::generate` passes** driven by
`region_settings` — beyond W9 forest/lake and W11 layout v2.

**Status:** done. See [v3-implementation-plan](./v3-implementation-plan.md).

**Prerequisite:** W9 region loader, W11 generate ordering.

---

## Purpose

v2 layout consumes:

| Region JSON | W9/W11 usage |
| --- | --- |
| `overmap_forest_settings` | `BaseTerrainFiller` noise |
| `overmap_lake_settings` | `LakeGenerator` |
| `city.houses` | `CityPlacer` weights |
| Heuristic quotas | `OvermapGenerateOptions.forSize` |

BN additionally uses:

| Region JSON | BN effect |
| --- | --- |
| `overmap_special_settings` | Weighted static / faction specials |
| `city` size / spacing | Urban blob size, distance between cities |
| `overmap_terrain_settings` | Swamp, sand, forest thickness thresholds |
| Connection phases | Subways, rails (separate graph) |

W14 closes the **documented v3 milestone** for region-driven specials and urban spacing — not full
BN `place_cities` / `place_roads` parity. Remaining layout gaps:
[24-cdda-layout-gaps](./24-cdda-layout-gaps.md).

---

## Sub-PRs

| PR | Focus | Priority |
| --- | --- | --- |
| **W14a** | `overmap_special_settings` | P0 |
| **W14b** | `city_size` / urban spacing | P0 |
| **W14c** | Swamp / beach / thick forest | P1 |
| **W14d** | Subways / rails / sewers | P2 or defer v4 |

---

## W14a — Overmap special settings

### BN reference

`regional_map_settings.json` → `overmap_special_settings`: weighted lists of `overmap_special`
ids, min/max counts, phase flags.

### Target

```text
RegionSpecialPlacer.place(grid, region, options, rng):
    for each special entry in region.getSpecialSettings():
        roll count in [min, max]
        pick weighted special id
        StaticSpecialPlacer.tryPlace(specialId, ...)  // reuse W4
```

### New types

```text
region/OvermapSpecialSettings.java
region/SpecialPlacementEntry.java
generate/RegionSpecialPlacer.java
```

### Extend

| File | Change |
| --- | --- |
| `RegionSettingsLoader` | Parse `overmap_special_settings` |
| `RegionSettingsDefinition` | Expose settings |
| `OvermapGenerator` | Call after base fill, before or after cities (match BN order — document choice) |

### Tests

| Test | Assert |
| --- | --- |
| `RegionSpecialPlacerTest` | Heavy special region places more specials than light region |
| `RegionSettingsLoaderTest` | Parses fixture `overmap_special_settings` |

### Fixture

```json
"overmap_special_settings": {
  "specials": { "test_farm_stack": 100, "test_special_wide": 10 },
  "min": 1,
  "max": 3
}
```

---

## W14b — City size and urban spacing

### BN reference

`city` block: `city_size`, `city_spacing`, `city_travel_time`, house weights (W9 has weights).

### v2 gap

`CityPlacer` uses `placedCenters` for roads and random clear rects — no concept of **city blob**
or minimum distance between urban sites.

### Target

```text
CityPlacer.placeAll(..., CitySizeSettings citySize):
    sites ← pickCitySites(grid, citySize, rng)   // Poisson or grid with min spacing
    for site in sites:
        placeBuildingAt(site, ...)
```

| Field | Use |
| --- | --- |
| `city_size` | Radius or building count cap per city |
| `city_spacing` | Min OMT distance between city centers |
| `city_isolated` | Optional — single-city maps |

### Tests

| Test | Assert |
| --- | --- |
| `CityPlacerUrbanSpacingTest` | Two city centers ≥ spacing on 64×64 |
| `CityPlacerRegionTest` | Extend existing — city count scales with `city_size` |

---

## W14c — Additional terrain passes

### BN reference

`overmap_terrain_settings`: swamp noise, sand beaches along water, forest_thick threshold.

### Target

```text
1. BaseTerrainFiller (W9) — keep
2. SwampGenerator.fill — noise above swamp threshold → swamp OMT
3. BeachGenerator.paint — lake/river shores → sand OMT (if region defines)
4. ThickForestGenerator — upgrade forest → forest_thick by second threshold
```

Reuse `RegionTerrainNoise` from W9.

### Region loader

Extend `OvermapForestSettings` or add `OvermapTerrainSettings` with swamp/beach ids.

### Tests

- `SwampGeneratorTest` — cluster on low noise fixture region
- Integration: region with high swamp threshold → more swamp OMTs than default

---

## W14d — Subways / rails / sewers (optional)

W11c implemented **highways** via `overmap_connection`. BN subways use separate placement passes
and connection types.

| v3 stance | Defer to v4 unless contributor owns BN `overmap.cpp` subway section |
| --- | --- |

If included:

```text
SubwayGenerator.carve(grid, connectionRegistry, options, rng)
RailGenerator.carve(...)
```

Requires new connection ids in data and directional painting (extend W11c).

---

## Generate order (W14 target)

Documented order (may differ from v2 W11 — note in PR):

```text
1. BaseTerrainFiller (+ region)
2. LakeGenerator
3. RiverGenerator
4. Swamp / beach / thick forest (W14c)
5. RegionSpecialPlacer (W14a) — or after cities per BN
6. CityPlacer (W14b spacing)
7. StaticSpecialPlacer (quota fallback)
8. MutableSpecialPlacer
9. HighwayGenerator
```

Add `OvermapGenerateOptions.legacyGenerationOrder` behavior note when order changes.

---

## Files to touch (summary)

| File | W14a | W14b | W14c |
| --- | --- | --- | --- |
| `RegionSettingsLoader.java` | ✓ | ✓ | ✓ |
| `RegionSettingsDefinition.java` | ✓ | ✓ | ✓ |
| `OvermapGenerator.java` | ✓ | ✓ | ✓ |
| `CityPlacer.java` | | ✓ | |
| `BaseTerrainFiller.java` | | | ✓ |
| New generators | | | ✓ |

---

## Verification

1. W14a: switch region special weights → special OMT count changes on fixed seed
2. W14b: city centers respect spacing on 64×64
3. W14c: swamp region → measurable swamp OMT cluster
4. Manual: 64×64 screenshot compare default vs `forest_heavy` vs new special-heavy region

---

## Dependencies

| Requires | PR |
| --- | --- |
| W4 placement | done |
| W9 region | done |
| W11 ordering | done |

---

## Out of scope (W14)

| Topic | Notes |
| --- | --- |
| Faction camp simulation | Placement only |
| Ocean tiles | Large-scale; defer |
| World persistence | W16 |
| Editing placed specials after gen | W15 editor |
