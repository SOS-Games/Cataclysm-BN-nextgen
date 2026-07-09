# 04b — Hydrology (rivers and lakes)

River placement, lake flood-fill, lake–river merge, and post-pass `polish_rivers`.

**Parent:** [04-generation-pipeline.md](./04-generation-pipeline.md) · **Connections:** [06-connections.md](./06-connections.md)

---

## Gate: `river_scale`

```cpp
if( settings->river_scale == 0.0 ) {
    return;  // place_rivers — entire function skipped
}
```

Set `river_scale` to `0.0` in region JSON to disable rivers. Default in `default` region is typically `4.0`.

Derived values inside `place_rivers` / `place_river`:

```cpp
int river_chance = max(1, int(1.0 / settings->river_scale));
int river_scale  = max(1, int(settings->river_scale));
```

Higher `river_scale` → wider river brush, more `one_in(river_chance)` paint passes.

---

## `place_rivers`

**Anchor:** `src/overmap.cpp` **4407–4558**.

### Edge continuity

For each loaded neighbor (N/W/S/E), scan edge OMTs (excluding corners `2 … OMAPX-3`):

1. If neighbor is river → paint local edge `river_center`
2. If three adjacent neighbor cells are river → roll `one_in(river_chance)` to add **start** or **end** endpoint (spacing ≥ `(i-6)*river_scale` along edge)

### Internal rivers

Additional start/end pairs rolled on map edges when no neighbor exists, then `place_river(start, end)`
connects them. Pairing logic handles mismatched start/end counts (~4520–4556).

**Note:** Rivers do **not** use `overmap_connection` / `build_connection` in BN — they paint
`river_center` directly via drunkard walk.

---

## `place_river` (drunkard walk)

**Anchor:** `src/overmap.cpp` **4696–4779**.

Pseudocode:

```text
p ← start
loop until p == end:
  jitter p by rng(-1,1) in x and y, clamp to [0, OMAPX-1]
  for each cell in (2*river_scale+1)² around p:
    if not lake and one_in(river_chance):
      ter_set river_center
  bias step toward end (probabilistic x/y increments using OMAPX/OMAPY fractions)
  repeat jitter + paint
```

Edge avoidance: cells within 1 OMT of map edge are skipped unless within 4 tiles of endpoint
(`inbounds(p, 1)` guard ~4765).

Lakes block painting (`!ter(p)->is_lake()`).

---

## `place_lakes`

**Anchor:** `src/overmap.cpp` **4265–4380+**.

**Gate:** `overmap_lake.noise_threshold_lake > 0`.

### Noise

`om_noise::om_noise_layer_lake` — see [appendix-algorithms-rng.md](./appendix-algorithms-rng.md).

### Algorithm

1. Scan for unvisited seeds where `noise_at(p) > threshold`
2. 4-connected flood fill → `lake_points`
3. Skip if `lake_points.size() < overmap_lake.lake_size_min` (default **20**)
4. Build `lake_set` = lake points **∪ all existing river OMTs** (rivers absorbed into lake)
5. For each in-bounds lake point:
   - 8-neighbor test → `lake_shore` vs `lake_surface`
   - Subsurface: `lake_water_cube` down to `lake_depth` (default **-5**), `lake_bed` at bottom
   - Shores: `lake_underwater_shore` columns
6. Optional: connect lake shore points to nearest river via `place_river` (~4362+)

**Order dependency:** Comment in source (~4308–4310): `place_rivers` must run before `place_lakes`.

---

## `polish_rivers`

**Anchor:** `src/overmap.cpp` **5460+** (runs after specials and trailheads).

For each OMT matching `is_ot_match("river", …, PREFIX)`:

1. Test N/E/S/W for `is_river_or_lake` (local or neighbor overmap at edge)
2. Ungenerated neighbor overmap → treat off-map as **water** (~5471)
3. Rewrite `river_center` / generic ids to directional variants:
   - Straights: `river_n`, `river_e`, …
   - Corners: `river_ne`, `river_sw`, …
   - Tee / four-way with "bite" corners: `river_c_not_nw`, etc.

Uses prefix match on `"river"` — includes `river_center` before polish.

---

## OMT ids (core)

| Id | Role |
| --- | --- |
| `river_center` | Unpolished walk / stitch fill |
| `river`, `river_ne`, … | Polished topology |
| `lake_surface`, `lake_shore` | Surface lake |
| `lake_water_cube`, `lake_bed`, `lake_underwater_shore` | Subsurface |

Data: `data/json/overmap/overmap_terrain/overmap_terrain_river.json`.

---

## Nextgen

| BN | Nextgen |
| --- | --- |
| Drunkard `place_river` | `RiverDrunkardCarver` via `RiverGenerator` |
| `place_rivers` edge stitch + multi-segment | `RiverEdgeStitcher` + paired `RiverGenerator.carve` |
| Lake flood + river merge | `LakeGenerator` shore/surface + river absorb |
| Lake outlet `connect_lake_to_closest_river` | `LakeOutletConnector.connectAll` after river carve |
| `polish_rivers` directional topology | `RiverPolisher.polishDirectional` with `OvermapNeighborContext` edge reads |
| Neighbor overmaps | `OvermapNeighborGrid` + `OvermapNeighborContext` wired in `OvermapGenerator` |

### Done (hydrology v2)

- `river_scale` gate + region loader
- Drunkard walk + river bank paint
- Edge stitch + synthetic endpoints + multi-segment pairing
- Lake shore/surface, river absorb into `lake_set`
- Lake north/south outlet drunkard walks (after river carve; see order note below)
- `RiverPolisher.polishDirectional` after specials/trails
- **W16:** `OvermapNeighborGrid` batch/exploration buffer, neighbor-aware carve + repolish

### Remaining (defer to Tier C / multi-z unless noted)

| Item | BN | Nextgen gap | Milestone |
| --- | --- | --- | --- |
| Lake subsurface z | `lake_water_cube`, `lake_bed`, `lake_underwater_shore` | Surface layer only | Tier D / multi-z |
| Phase order | `place_rivers` → `place_lakes` | Lakes first, rivers + `LakeOutletConnector` after | Intentional for v2 single pass |

Swamp floodplain moved to [04c-terrain-fill.md](./04c-terrain-fill.md) — **done** (Phase C slice 1).

---

## Inputs

- Surface `map_layer` after stitch
- `river_scale`, lake/forest thresholds from region
- Neighbor overmaps for edge stitch and polish

## Outputs

- `river_*`, `lake_*` OMT ids on surface and negative z
- Possible new river segments from lake outlet connection

## Failure modes

- `river_scale == 0` — no rivers
- Lake threshold 0 — lakes skipped; rivers unchanged
- Lake overwrites river cells in `lake_set` — intentional

## Verification

1. New world: `river_center` visible mid-generate; after polish, mostly directional `river_*`.
2. Large lake OMT blob with `lake_surface` interior and `lake_shore` ring.
3. Region `river_scale: 0` — overmap has no new river paint (except neighbor copies on edge).

**BN anchors:** `src/overmap.cpp` (`place_rivers`, `place_river`, `place_lakes`, `polish_rivers`).
