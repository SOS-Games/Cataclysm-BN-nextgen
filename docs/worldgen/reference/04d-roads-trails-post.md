# 04d — Roads, forest trails, and post-layout

`place_roads`, forest trail network, trailhead specials, and non-terrain gameplay placement.

**Parent:** [04-generation-pipeline.md](./04-generation-pipeline.md) · **Cities:** [05-cities-and-urban.md](./05-cities-and-urban.md) · **Connections API:** [06-connections.md](./06-connections.md)

---

## `place_roads`

**Anchor:** `src/overmap.cpp` **4615–4694**.

BN inter-city / edge connectivity uses **`local_road` only** — there is no separate `highway`
connection pass in C++ `overmap.cpp`. (Nextgen adds `HighwayGenerator` separately.)

### Steps

1. Reference `connections_out[local_road]`
2. If `roads_out.size() < 2`, pick up to one exit per **missing** neighbor side (N/E/S/W where
   neighbor pointer is null):
   - Shuffle candidate edge positions `10 … OMAPX-11`
   - Reject if cell or adjacent ±1 is river
   - Push into `roads_out` until ≥ 2 exits or candidates exhausted
3. Build `road_points` = all `roads_out` xy + all `cities[].pos`
4. `connect_closest_points(road_points, 0, local_road)` — greedy closest-pair MST-like wiring

`build_connection` during carve may call `elevate_bridges` for water crossings (~5359+).

LINEAR bitmask merge and directional `road_*` ids: [06a-linear-oter-paint.md](./06a-linear-oter-paint.md).
Visit-time pavement / extras: [08a-road-builtin-mapgen.md](./08a-road-builtin-mapgen.md).
Nextgen work: [../28-road-rendering-fidelity.md](../28-road-rendering-fidelity.md).

---

## `place_forest_trails`

**Anchor:** `src/overmap.cpp` **4060–4179**.

**Gate:** `settings->forest_trail.chance > 0`.

### Algorithm sketch

1. Flood-fill each contiguous forest blob (`forest`, `forest_thick`, `forest_water` prefix match)
2. Skip if size `< forest_trail.minimum_forest_size` (default **50**)
3. Roll `one_in(forest_trail.chance)` per blob — most blobs skipped
4. Pick random interior points (`random_point_min` … `max`, scaled by blob size)
5. Optionally add border-adjacent points (`border_point_chance`)
6. `connect_closest_points(chosen_points, 0, forest_trail connection id)`

Connection id from region: `forest_trail.trail_connection` → JSON `"forest_trail"`.

---

## `place_forest_trailheads`

**Anchor:** `src/overmap.cpp` (after `place_specials`, ~3437).

Places trailhead **specials** from `forest_trail.trailheads` building bin near trail/road
intersections. Uses `forest_trail.trailhead_chance`, `trailhead_road_distance`, etc.

Runs **after** roads and specials so road adjacency tests succeed.

---

## `place_mongroups` / `place_radios`

**Anchors:** `place_mongroups` ~6348+, `place_radios` elsewhere in `overmap.cpp`.

Gameplay spawns — not layout art. Run after terrain stable:

- City zombie hordes when `WANDER_SPAWNS`
- Dimensional surface groups near `central_lab_entrance` on `(0,0)` overmap
- Radio stations on suitable OMTs

Nextgen: out of scope for layout parity.

---

## Option keys (world defaults)

| Option | Default | Affects |
| --- | --- | --- |
| `CITY_SIZE` | 8 | City count/radius (unless region override) |
| `CITY_SPACING` | 4 | Map coverage fraction |
| `SPECIALS_DENSITY` | 1.0 | Special placement (phase 07) |
| `SPECIALS_SPACING` | 6 | Min distance between specials |

`CITY_SIZE == 0` disables cities **and** `place_cities` returns early — also blocks city-start scenarios per option text.

---

## Nextgen divergence

| Feature | BN | Nextgen |
| --- | --- | --- |
| Inter-city roads | `place_roads` + `local_road` | `LocalRoadGenerator` + **`HighwayGenerator`** |
| Edge road synthesis | Yes when `< 2` exits | N/A (no edges) |
| Forest trails | Full algorithm | Partial W17 |
| Mongroups | Yes | No |

---

## Inputs

- Post-city surface layer
- `connections_out` from stitch + city carve
- `forest_trail` region settings
- `local_road` / `forest_trail` connection definitions

## Outputs

- Painted `road_*` / `forest_trail` OMT chains
- Updated `connections_out`
- Bridge OMT stacks at water
- Trailhead special footprints (if rolled)

## Failure modes

- Missing `local_road` JSON — roads skipped entirely
- `< 2` viable edge exits on island overmap — partial connectivity
- Forest blob too small — no trail

## Verification

1. Two cities on same overmap: continuous `road_*` path after generate (may require several closest-point segments).
2. Large forest mass: occasional `forest_trail` OMT chains.
3. Compare BN vs nextgen: nextgen may show extra highway-like links not present in BN `place_roads`.

**BN anchors:** `src/overmap.cpp` (`place_roads`, `place_forest_trails`, `place_forest_trailheads`, `place_mongroups`).
