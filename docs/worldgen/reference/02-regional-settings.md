# 02 — Regional settings

BN region profiles control default terrain, hydrology thresholds, city parameters, forest
trails, feature-flag special filters, and submap groundcover substitution.

---

## Data sources

| Source | Path / symbol |
| --- | --- |
| JSON definitions | `data/json/regional_map_settings.json` (`type: region_settings`) |
| Mod overlays | Mod `region_settings` folders; merged at load |
| Runtime map | Global `region_settings_map` (`src/regional_settings.cpp`) |
| Per-overmap pointer | `overmap::settings` |

Load pipeline: `regional_settings::load` → per-entry `finalize` → `check_regional_settings` →
`overmap_terrains::finalize` (requires `"default"` region).

---

## Top-level `regional_settings` fields

**Header:** `src/regional_settings.h` **244–274**.

| Field | Type / default | Role |
| --- | --- | --- |
| `id` | string | Region key (`"default"`, `"isolated"`, mod ids) |
| `default_oter` | `oter_str_id` | Surface fill before generate phases |
| `display_oter` | optional | Overmap UI symbol override; **stored tile stays** `default_oter` |
| `river_scale` | `1.0` (`0` = no rivers) | River width/density |
| `default_groundcover` | weighted ter list | Submap base terrain substitution |
| `city_spec` | `city_settings` | City size override, building bins |
| `field_coverage` | `groundcover_extra` | Field mapgen extras |
| `forest_composition` | `forest_mapgen_settings` | Forest submap composition |
| `forest_trail` | `forest_trail_settings` | Trail chance, connection id, trailheads |
| `overmap_feature_flag` | blacklist/whitelist | Filters `enabled_specials` in `populate` |
| `overmap_forest` | thresholds | Forest/swamp noise gates |
| `overmap_lake` | thresholds + depth | Lake noise, min size, z depth |
| `region_terrain_and_furniture` | alias maps | Resolve `t_region_*` placeholders at visit |
| `region_extras` | map extras | Submap spawn extras |
| `weather` | generator | Climate (not overmap layout) |

---

## Nested: `city_settings`

| Field | Default | Role |
| --- | --- | --- |
| `city_size` | `-1` | Override `CITY_SIZE` option if ≥ 0 |
| `city_spacing` | `-1` | Override `CITY_SPACING` if ≥ 0 |
| `shop_radius`, `shop_sigma` | 30, 20 | Normal distribution for shop placement |
| `park_radius`, `park_sigma` | 30, 70 | Park placement |
| `houses`, `urban_houses`, `shops`, `urban_shops`, `parks`, `finales` | `building_bin` | Weighted `overmap_special_id` lists |

Pick methods: `pick_house()`, `pick_shop()`, etc.

---

## Nested: `overmap_forest_settings`

| Field | Default | Consumer |
| --- | --- | --- |
| `noise_threshold_forest` | 0.0 | `place_forests` gate |
| `noise_threshold_forest_thick` | 0.0 | Thick forest cutoff |
| `noise_threshold_swamp_adjacent_water` | 0.3 | `place_swamps` near rivers |
| `noise_threshold_swamp_isolated` | 0.6 | Isolated bog |
| `river_floodplain_buffer_distance_min/max` | 3, 15 | Swamp buffer radius |

---

## Nested: `overmap_lake_settings`

| Field | Default | Consumer |
| --- | --- | --- |
| `noise_threshold_lake` | 0.0 | `place_lakes` gate |
| `lake_size_min` | 20 | Minimum flood-fill size |
| `lake_depth` | -5 | Deepest z for lake bed |
| `shore_extendable_overmap_terrain` | list | Terrains treatable as shore extensions |

---

## Nested: `forest_trail_settings`

| Field | Default | Consumer |
| --- | --- | --- |
| `chance` | 0 | Gate trails + trailheads |
| `minimum_forest_size` | 50 | Min blob for trail |
| `border_point_chance` | 2 | Edge anchor rolls |
| `random_point_min/max` | 4, 50 | Interior trail nodes |
| `trailhead_chance` | 1 | Trailhead special rolls |
| `trailhead_road_distance` | 6 | Distance to road for trailhead |
| `trailheads` | `building_bin` | Special ids |

Connection id in JSON: `"trail_connection": "forest_trail"`.

---

## Disable cheatsheet (region JSON)

Comments in `regional_settings.h` (~251–254):

| Set to | Disables |
| --- | --- |
| `river_scale: 0.0` | Rivers |
| `overmap_forest.noise_threshold_forest: 0.0` | Forests + swamps |
| `overmap_lake.noise_threshold_lake: 0.0` | Lakes |
| `forest_trail.chance: 0` | Trails + trailheads |

---

## JSON example (minimal)

```json
{
  "type": "region_settings",
  "id": "default",
  "default_oter": "field",
  "river_scale": 4.0,
  "overmap_forest": {
    "noise_threshold_forest": 0.5,
    "noise_threshold_forest_thick": 0.62
  },
  "overmap_lake": {
    "noise_threshold_lake": 0.25,
    "lake_size_min": 20,
    "lake_depth": -5
  },
  "forest_trail": {
    "chance": 2,
    "trail_connection": "forest_trail"
  }
}
```

Full stock file: `data/json/regional_map_settings.json` (~1295 lines).

---

## Fields consumed by generate phases

| Phase | Region fields |
| --- | --- |
| Initial fill | `default_oter` |
| Rivers | `river_scale` |
| Lakes | `overmap_lake.*` |
| Forests/swamps | `overmap_forest.*` |
| Cities | `city_spec`, optional size/spacing override |
| Trails | `forest_trail.*` |
| Specials filter | `overmap_feature_flag` in `populate` |
| Submap visit | `default_groundcover`, `region_terrain_and_furniture` |

---

## Relationship to nextgen

W9 `RegionSettingsLoader` / `RegionProfile`: partial — `default_oter`, some thresholds.
`RegionPickerDialog` should bind to full profiles.

Gaps: [../25-cdda-region-visit-world-gaps.md](../25-cdda-region-visit-world-gaps.md).

---

## Inputs

- Merged JSON at init
- Active region id at overmap construction

## Outputs

- `region_settings_map`
- `overmap::settings` pointer

## Failure modes

- No `default` region after finalize — hard error
- Unknown region id — fallback + debug message
- Invalid oter in JSON — `check_regional_settings` / terrain finalize error

## Verification

1. `--check-mods` passes regional validation.
2. Test region with `river_scale: 0` — no rivers on new overmap.
3. `display_oter` set — overmap UI differs but mapgen still uses `default_oter`.

**BN anchors:** `src/regional_settings.h`, `src/regional_settings.cpp`,
`data/json/regional_map_settings.json`, `src/overmap.cpp` (constructor ~2882).
