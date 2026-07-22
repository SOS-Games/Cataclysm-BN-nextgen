# 08a — Road / trail builtin mapgen & extras

Visit-time generation for LINEAR transportation OMTs: pavement topology, sidewalks, vehicles,
item litter, and region `map_extras` keyed by OMT `"extras": "road"`.

**Parent:** [08-omt-to-submap.md](./08-omt-to-submap.md) · **LINEAR paint:** [06a-linear-oter-paint.md](./06a-linear-oter-paint.md)

**BN anchors:**

| Topic | Location |
| --- | --- |
| Builtin registry | `src/mapgen_functions.cpp` (~98: `road_straight` … → `&mapgen_road`) |
| Body | `mapgen_road` ~491–837 |
| NESW decode | `terrain_type_to_nesw_array` ~433 |
| Mapgen pick key | `oter_t::get_mapgen_id` (`overmap.cpp` ~876) |
| Extras after draw | `map::generate` extras block (`mapgen.cpp` ~192–207) |
| Edge polish | `map::draw_connections` (~5562) — mostly sewer/subway; roads rely on builtin + LINEAR |
| Region weights | `data/json/regional_map_settings.json` → `map_extras.road` |
| Extra defs | `data/json/overmap/map_extras.json` (`mx_roadworks`, `mx_roadblock`, …) |

---

## Pipeline position (visit)

```text
map::generate / draw_map
  → function_key = oter.get_mapgen_id()     // e.g. "road_tee"
  → oter_mapgen.pick / run_mapgen_func
       → builtin mapgen_road (all five keys share one C++ fn)
  → draw_connections(dat)
  → region_extras[oter.extras] chance roll → MapExtras::apply_function
  → static monster spawns from oter
```

All of `road_straight`, `road_curved`, `road_end`, `road_tee`, `road_four_way` call
**`mapgen_road`**. Shape is derived from the **LINEAR line bits** on `dat.terrain_type()`, not
from which registry name was used.

---

## NESW connectivity

```cpp
int terrain_type_to_nesw_array( oter_id terrain_type, bool array[4] );
// array[N,E,S,W] = oter.has_connection(dir); returns count
```

`has_connection` for LINEAR oters follows the line bitmask segments. This is the single source
of truth for which pavement arms exist.

---

## `mapgen_road` algorithm (summary)

**Fill:** `dat.fills_groundcover()` (region groundcover).

### 1. Neighbor sidewalk survey

For 8 neighbors (N E S W NE SE SW NW): set `sidewalks_neswx[dir]` if neighbor has
`oter_flags::has_sidewalk`. Count → `neighbor_sidewalks` (urban vs rural vehicle tables).

### 2. Own road arms + curve hints

- `roads_nesw[4]` / `num_dirs` from `terrain_type_to_nesw_array`
- Dead end (`num_dirs == 1`): `dead_end_extension = 8` (pavement past center)
- For each arm whose neighbor type id is `"road"` and neighbor is **2-way** facing us:
  - If neighbor turns left/right → `curvedir_nesw[dir] ±1` (corner fillets toward diagonal)

### 3. Canonicalize orientation

Rotate arrays so remaining shapes are: `|`  `'-`  `-'-`  `-|-` (plus diagonal flag).

| `num_dirs` | Shape | Notes |
| --- | --- | --- |
| 4 | Four-way / plaza | Detects plaza center/side/corner via neighbor `road_nesw` pattern |
| 3 | Tee | Rotate missing arm to south |
| 2 | Straight or diagonal corner | Diagonal if arms adjacent |
| 1 | Dead end | Optional cul-de-sac |

### 4. Paint (24×24 = `2*SEEX` × `2*SEEY`)

**Diagonal branch** (`diag`):

- Optional sidewalk wedge if S/SW/W neighbors have sidewalks
- Diagonal pavement band with yellow dashes (`t_pavement_y`)

**Orthogonal branch:**

- Cul-de-sac (~1/3): if dead end + any sidewalk neighbor → full sidewalk fill, later circle of pavement
- Sidewalk strips flanking each road arm when adjacent OMTs have `has_sidewalk`
- **16-wide** pavement from center toward each arm (`t_pavement`), plus triangular curve fillets when `curvedir_nesw != 0`
- Yellow center dashes (shortened at some intersections)
- Plaza overlays: sidewalk plazas, benches, young trees, shallow water circles

Finally: `m->rotate(rot)` to restore world orientation.

### 5. Content inside the builtin (not map_extras)

| Content | Rule |
| --- | --- |
| Vehicles | `vspawn_id( neighbor_sidewalks ? "default_city" : "default_country" )` with shape key `road_four_way` / `road_tee` / `road_end` / `road_curved` / `road_straight` (skipped for plaza center) |
| Zombies | If any sidewalk neighbor: `place_spawns(GROUP_ZOMBIE, …)`; rare Jackson |
| Items | `place_items("road" or "trash", chance 5, …)` |
| Manhole | If oter is `road_nesw_manhole` → random `t_manhole_cover` |

---

## Map extras (`"extras": "road"`)

After successful `draw_map`, BN rolls region extras:

```text
ex = region_settings.region_extras[ terrain_type->get_extras() ]  // "road"
if ex.chance > 0 && one_in(ex.chance):
  pick weighted mx_* from ex.values
  MapExtras::apply_function(id, map, abs_sub)
```

Default region (`regional_map_settings.json`) **`map_extras.road`**:

| Field | Default |
| --- | --- |
| `chance` | **75** → roughly 1/75 OMTs get an extra |
| Weighted pool | `mx_roadworks` 100, `mx_roadblock` 100, `mx_casings` 100, `mx_bandits_block` 80, `mx_mayhem` 50, `mx_collegekids` 50, `mx_science` 40, `mx_surrounded_vehicle` 30, `mx_corpses` 30, `mx_military` 25, `mx_drugdeal` 30, `mx_prison_bus` 15, `mx_supplydrop` 10, `mx_crater` 10, `mx_helicopter`/`mx_aircraft` 1, portals, etc. |

Methods (`map_extras.json`):

| Method | Examples |
| --- | --- |
| `map_extra_function` | `mx_roadworks`, `mx_roadblock`, `mx_mayhem`, `mx_helicopter` |
| `update_mapgen` | `mx_military`, `mx_survivor`, `mx_science`, `mx_collegekids` |

These are the “things spawning alongside / on the road” players notice (barricades, wrecks,
heli crash, roadworks). They are **independent** of pavement topology.

Related: `bridgehead_ground` extras (`mx_minefield`), urban `build` extras — different keys.

---

## Related builtins

| Builtin name | Function | Notes |
| --- | --- | --- |
| `highway` | `mapgen_highway` | `hiway_ns` / `hiway_ew` only |
| `railroad_*` | `mapgen_railroad` | Same LINEAR + NESW pattern family |
| `subway` / sewer | Separate mapgens | `draw_connections` heavy for subway↔sewer |

Forest trails use trail connection LINEAR ids + trail mapgen / background builders in nextgen.

---

## What nextgen does today

| BN | Nextgen |
| --- | --- |
| `mapgen_road` full topology | `BackgroundOmtSubmapBuilder.buildRoad` — thin `t_pavement` cross from neighbor OMT flags |
| Sidewalks / cul-de-sac / plaza | Missing |
| Curve fillets from 2-way neighbors | Missing |
| Vehicle spawn tables | Missing |
| Item group `road` / `trash` | Missing |
| Region `map_extras.road` | Not implemented ([25](../25-cdda-region-visit-world-gaps.md)) |
| Builtin pick via `get_mapgen_id` | Roads never reach JSON/builtin picker if background builder claims them first |

---

## Inputs

- LINEAR `oter_id` (or rotatable highway) + 8 neighbor oters in `mapgendata`
- Region groundcover + `region_extras["road"]`
- World/abs submap coords for extra seed

## Outputs

- 24×24 terrain/furniture (pavement, sidewalks, manhole, plaza props)
- Optional vehicles, items, spawns
- Optional map extra overlay

## Failure modes

- Wrong / missing line bits → wrong `num_dirs` → straight stub instead of tee
- No sidewalk flags on city buildings → rural vehicle table + no zombie strip
- `extras` key missing from region → no roadside events
- Builtin not registered → `draw_map` fallback debugmsg + floor fill

## Verification

1. Visit `road_nes` → tee pavement; yellow dashes stop short of stem.
2. Visit `road_ne` → diagonal or curved fillet (not axis-aligned cross only).
3. Urban road next to `has_sidewalk` building → sidewalk strips + city vehicle spawn pool.
4. Generate many road visits with fixed seed: occasional `mx_roadworks` / roadblock when chance rolls.
5. `road_nesw_manhole` → manhole cover present.

**Nextgen work:** [28-road-rendering-fidelity.md](../28-road-rendering-fidelity.md)
