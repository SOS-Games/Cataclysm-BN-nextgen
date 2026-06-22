# 15 — Region settings and base terrain (W9)

Load **`region_settings`** JSON and drive **base overmap fill**, **city building weights**, and
**placement quotas** from region data instead of hardcoded forest/field noise.

**Status:** done. See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

W4 `BaseTerrainFiller` (`core/.../generate/BaseTerrainFiller.java`) uses ~35% random forest vs
field on every cell:

```java
final boolean forest = rng.nextInt(100) < 35;
grid.setOmtId(x, y, forest ? forestId : fieldId);
```

City count uses `OvermapGenerateOptions.forSize`:

```java
final int cityQuota = Math.max(1, area / 32);
```

BN reads `type: region_settings` from `data/json/regional_map_settings.json` (and mods) for
noise thresholds, default OMT, city house weights, lakes, forests.

W9 makes `OvermapGenerateOptions.getRegionId()` affect generation.

---

## BN data paths

| Path | Role |
| --- | --- |
| `data/json/regional_map_settings.json` | Array of `region_settings` objects |
| `data/json/world_types.json` | `region_settings: "default"` per world type |
| `data/json/test_regions.json` | Test / mod regions |

**Not** a `region_settings/` directory — scan like game data loader (array roots).

### Example fields (default region)

| JSON path | Use in W9 |
| --- | --- |
| `id` | Registry key (`default`) |
| `default_oter` | Fallback OMT (`field`) |
| `overmap_forest_settings` | `noise_threshold_forest`, thick/swamp thresholds |
| `overmap_lake_settings` | Lake noise (W11b may consume fully) |
| `city.houses` | Weighted `city_building` id → int weight |
| `city.shop_radius` / `park_radius` | Defer — needs city grid structure |
| `region_terrain_and_furniture` | Submap only — already [19-regional](../mapgen-preview/19-regional-terrain.md) |

---

## Load phase

```java
public final class RegionSettingsDefinition {
    private final String id;
    private final String defaultOter;
    private final OvermapForestSettings forestSettings;
    private final OvermapLakeSettings lakeSettings;   // optional v9.1
    private final Map<String, Integer> cityHouseWeights;
}

public final class RegionSettingsLoader {
    public static RegionSettingsLoadResult load(RegionSettingsScanOptions options);
}

public final class RegionSettingsRegistry {
    public Optional<RegionSettingsDefinition> find(String id);
}
```

**Scan options:** reuse `DataPaths` + mod merge order from [game data loader](../game-data-loader/README.md).

**Parser:** LibGDX `JsonReader` — same pattern as `OvermapTerrainLoader`.

---

## `BaseTerrainFiller` v2

Replace uniform 35% forest with **per-cell noise** aligned to BN subset:

```text
fill(grid, options, region, oterRegistry, rng):
    forest ← region.getForestSettings()
    for y, x:
        n ← normalizedNoise(x, y, options.seed)   // 0..1

        if n < forest.noiseThresholdForest:
            id ← "forest"   // resolve via registry
        else if n < forest.noiseThresholdForestThick:
            id ← "forest_thick"
        else:
            id ← region.defaultOter

        grid.setOmtId(x, y, resolveId(id, oterRegistry))
```

| v1 | v9 |
| --- | --- |
| XOR hash bit | Seeded `noise(x,y)` — e.g. Perlin or BN-like table |
| 2 OMT types | `forest`, `forest_thick`, `field`, `swamp` when thresholds met |

Keep v1 path when `region == null` or `options.regionId` unknown.

---

## `CityPlacer` v2

Today: uniform random from all `city_building` candidates that fit on grid.

W9: weighted pick from `region.city.houses`:

```text
pickCityBuilding(rng, region, registry):
    weights ← region.cityHouseWeights
    id ← weightedPick(weights, rng)
  return registry.find(id)  // map BN token e.g. "2storyModern01" → bundle id
```

**Id mapping:** BN `city.houses` keys may differ from `CityBuildingDefinition.id` — add
`CityBuildingRegistry.findByAlias(token)` or explicit map in bundle JSON.

| Token | Bundle id (example) |
| --- | --- |
| `2storyModern01` | `2storyModern01` or `2story_modern_01` |
| `house_w_1` | match `OvermapSpecialBuildingLoader` ids |

Log `unknown city house token` once per token.

---

## `OvermapGenerator` integration

```java
public static OvermapGenerateResult generate(..., RegionSettingsRegistry regions) {
    final RegionSettingsDefinition region = regions
        .find(options.getRegionId())
        .orElse(null);

    BaseTerrainFiller.fill(grid, options, region, oterRegistry, rng);
    // rivers, cities, ...
}
```

`WorldgenPreviewService.resolveGenerateOptions` already has `regionId` on options — thread
into loader after `ensureLoaded`.

---

## Relationship to submap regional resolve

| Layer | System | Doc |
| --- | --- | --- |
| Overmap OMT ids | W9 `BaseTerrainFiller` | this doc |
| Submap `t_region_*` ter/furn | `RegionalTerrainResolver` | [19](../mapgen-preview/19-regional-terrain.md) |

Same `region_settings` JSON file; different sections parsed by different loaders.

---

## W9 scope slices

| Slice | Deliverable |
| --- | --- |
| W9.0 | Loader + registry + `default_oter` |
| W9.1 | `overmap_forest_settings` noise fill |
| W9.2 | `city.houses` weighted `CityPlacer` |
| W9.3 | `overmap_lake_settings` noise → lake OMT (overlap W11b) |

Land W9.0–W9.1 before W11b full hydrology.

---

## Test plan

| Test | Assert |
| --- | --- |
| `RegionSettingsLoaderTest` | loads `default` from fixture |
| `RegionSettingsLoaderTest` | mod overrides base region field |
| `BaseTerrainFillerRegionTest` | same seed + region → stable grid |
| `BaseTerrainFillerRegionTest` | `desert_test` ≠ `default` forest ratio |
| `CityPlacerRegionTest` | weights favor `2storyModern01` over rare id |

### Fixture

```json
// worldgen-fixtures/region/minimal_region.json
[
  {
    "type": "region_settings",
    "id": "test_region",
    "default_oter": "field",
    "overmap_forest_settings": {
      "noise_threshold_forest": 0.5,
      "noise_threshold_forest_thick": 0.9
    },
    "city": { "houses": { "test_house": 100 } }
  }
]
```

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Unknown `regionId` | Warn; use v1 filler + `default` region if present |
| Weight references missing OMT | Skip weight + warn |
| `city.houses` token not in registry | Skip token |
| Empty forest thresholds | All `default_oter` |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Region load | `src/regional_settings.cpp` |
| Forest noise on overmap | `src/overmap.cpp` — forest placement passes |
| City house pick | `src/overmap.cpp` — `pick_building` |

---

## Verification

1. `generateOvermap` with `regionId=default` produces more forest than `desert_test` (if present)
2. City buildings on generated map skew toward high-weight `city.houses` entries
3. Loader respects mod order (patch region overrides)
4. Missing region does not crash — v1 fallback

---

## Dependencies

| Requires | PR |
| --- | --- |
| W1 OMT registry | done |
| W4 `OvermapGenerator` | done |
| `CityBuildingRegistry` aliases | W9.2 may need scanner tweak |
