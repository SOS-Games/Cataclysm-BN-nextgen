# 04c — Terrain fill (forests and swamps)

Noise-driven conversion of `default_oter` cells into forest and swamp OMT types.

**Parent:** [04-generation-pipeline.md](./04-generation-pipeline.md) · **RNG:** [appendix-algorithms-rng.md](./appendix-algorithms-rng.md)

---

## Gate

Both `place_forests` and `place_swamps` run only when:

```cpp
settings->overmap_forest.noise_threshold_forest > 0.0
```

Set threshold to `0.0` in region JSON to skip both phases entirely.

---

## `place_forests`

**Anchor:** `src/overmap.cpp` **4234–4263**.

```text
for each OMT (x,y) at z=0:
  if ter != settings->default_oter: continue
  n = om_noise_layer_forest.noise_at(p)
  if n > noise_threshold_forest_thick: ter ← forest_thick
  else if n > noise_threshold_forest:     ter ← forest
```

Only **unmodified** default terrain converts — cities, rivers, lakes, and roads are untouched.

### Region fields (`overmap_forest_settings`)

| Field | Default | Role |
| --- | --- | --- |
| `noise_threshold_forest` | 0.0 (disabled) | Minimum noise for `forest` |
| `noise_threshold_forest_thick` | 0.0 | Higher bar for `forest_thick` |
| `noise_threshold_swamp_adjacent_water` | 0.3 | Used in `place_swamps` |
| `noise_threshold_swamp_isolated` | 0.6 | Isolated bog noise |
| `river_floodplain_buffer_distance_min/max` | 3 / 15 | River buffer for swamps |

Forest/thick **oter ids** are hardcoded `"forest"` / `"forest_thick"` in C++ (not region JSON).

---

## `place_swamps`

**Anchor:** `src/overmap.cpp` **4561–4613**.

Two mechanisms:

### 1. River floodplain buffer

For each river OMT, increment a counter on all OMTs within random radius
`[river_floodplain_buffer_distance_min, max]` using `closest_points_first`.

### 2. Noise intersection

`om_noise_layer_floodplain` sampled per cell. For OMTs that are forest (`is_ot_match("forest", CONTAINS)`):

```text
if floodplain_counter > 0 and floodplain_noise > noise_threshold_swamp_adjacent_water:
  ter ← forest_water
else if floodplain_noise > noise_threshold_swamp_isolated:
  ter ← forest_water   // isolated bog
```

`forest_water` is the swamp OMT id used in BN source.

---

## Noise implementation

**Header:** `src/overmap_noise.h` · **Implementation:** `src/overmap_noise.cpp`.

| Layer | Octaves / scale | Post-process |
| --- | --- | --- |
| Forest | 8 @ 0.03 + 12 @ 0.07 | `max(0, r² - d²*0.5)` |
| Floodplain | 8 @ 0.05 | `r²` |
| Lake | 16 @ 0.002 | `r⁴` |

All use `scaled_octave_noise_3d(..., global_omt_x, global_omt_y, world_seed)`.

`global_base_point()` converts local OMT to absolute coordinates for seamless noise across overmap files.

---

## Phase order interactions

| Earlier phase | Effect on forests/swamps |
| --- | --- |
| Rivers / lakes | Reduce eligible `default_oter` cells |
| (none after) | Forests run **before** cities — urban areas can overwrite forest later via `place_cities` |

---

## Nextgen

W9/W17 load region thresholds into `RegionProfile`. Java forest placement should match:

- Default-oter-only conversion
- Dual threshold for thick forest
- Swamp = river buffer + floodplain noise on forest prefix

| Feature | Status |
| --- | --- |
| `BaseTerrainFiller` default-oter-only base fill | Done |
| `ForestGenerator` default-oter-only `place_forests` + thick tier | Done (Phase C slice 2) |
| `SwampGenerator` river floodplain buffer (`closest_points_first` + counter) | Done (Phase C slice 1) |
| `SwampGenerator` floodplain noise + forest-only gate | Done (Phase C slice 1) |
| `RegionTerrainNoise.forestNormalized` | Surrogate hash (not simplex octave) |
| `RegionTerrainNoise.floodplainNormalized` | Surrogate hash (not simplex octave) |
| `river_floodplain_buffer_distance_min/max` region fields | Done |

See [../25-cdda-region-visit-world-gaps.md](../25-cdda-region-visit-world-gaps.md).

---

## Inputs

- Surface layer with `default_oter` fill where untouched
- World seed → noise layers
- `overmap_forest` thresholds from region

## Outputs

- `forest`, `forest_thick`, `forest_water` OMT patches

## Failure modes

- Threshold 0 — phases skipped
- All cells pre-painted — no forests (common near max lake/river coverage)

## Verification

1. Default region: substantial `forest` blobs on new overmap; `field` remains in low-noise areas.
2. Set `noise_threshold_forest` very high — almost no forest OMTs.
3. River-adjacent forest converts to `forest_water` more often than interior forest.

**BN anchors:** `src/overmap.cpp` (`place_forests`, `place_swamps`), `src/overmap_noise.cpp`.
