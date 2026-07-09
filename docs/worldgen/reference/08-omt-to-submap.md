# 08 — OMT to submap

How BN turns one OMT's `oter_id` into playable tile data when the player approaches.

---

## Scale correction

| Unit | Size |
| --- | --- |
| One submap | `SEEX × SEEY` = **12×12** tiles |
| One OMT | **2×2** submaps = **24×24** tiles |
| Loaded window | Typically `MAPSIZE` submaps (see `game_constants.h`) |

Nextgen often uses **one 24×24 `MapGrid` per OMT** directly — equivalent tile coverage, different
cache granularity.

---

## Trigger

Player movement → `mapbuffer` loads/creates submaps for neighborhood of OMTs.

BN loads a **2×2** OMT submap window; nextgen may use **3×3** patch stitch (`OmtNeighborhoodStitcher`).

---

## Pipeline

```text
mapbuffer → submap load
  resolve oter_id at world (omt, z)
  map::draw_map(mapgendata, options)
    → oter_mapgen.pick(function_key) OR run_mapgen_func
    → mapgen_function::generate(dat)
    → (fallback hardcoded office/temple/mine)
    → draw_connections(dat)
  cache in mapbuffer
```

**Primary source:** `src/mapgen.cpp` — `map::draw_map` **4347–4411**.

---

## `draw_map` steps

1. `function_key = terrain_type->get_mapgen_id()` from weighted oter mapgen list
2. If `options.use_selected_mapgen` — force specific function (editor/debug)
3. Else `oter_mapgen.pick(function_key)` or `run_mapgen_func`
4. If still not generated — prefix fallbacks `office`/`temple`/`mine`
5. Else `debugmsg` + `fill_background(t_floor)`
6. Always: `draw_connections(dat)`

Worker threads: may return `needs_main_thread` for Lua/non-thread-safe mapgen.

---

## Mapgen pick

`oter_mapgen` registry populated at load from:

- JSON mapgen ids → `mapgen_function_json`
- Builtin keys → C++ functions in `mapgen_functions.cpp`
- Lua mapgen (when enabled)

Rotation: OMT suffix `_north` etc. rotates json/builtin where supported.

Weights: JSON order in `overmap_terrain` entry.

---

## Builtin mapgen (common keys)

| Key | Output |
| --- | --- |
| `field` | Grass, region groundcover |
| `forest` | Trees, underbrush |
| `river` / `lake_shore` | Water/shore tiles |
| `road` | Pavement bands |

Region substitution: `regional_settings::default_groundcover`, `region_terrain_and_furniture.resolve`.

Nextgen: `BackgroundOmtSubmapBuilder` + `JsonMapgenRunner` for JSON ids.

---

## JSON mapgen

`mapgen_function_json::generate` — full grammar in [mapgen-preview](../../mapgen-preview/README.md).

Building bundles / multitile city JSON may use separate compose paths before load.

---

## `draw_connections`

Runs after primary mapgen. Uses overmap neighbor `oter_id`s and `overmap_connection` metadata
to paint road/river/subway transitions on **submap edges** (`EAST_EDGE`, `SOUTH_EDGE`).

Critical for continuous highways and rivers across OMT boundaries.

Nextgen: partial via `JoinContext` / neighbor options (W13).

---

## Seeds

Submap generation mixes world seed with OMT/submap coordinates in `mapgendata` setup — exact
formula in mapgen init code. Must match for deterministic golden tests.

Nextgen: `SubmapSeed.mix(worldSeed, SubmapKey)`.

---

## Z levels

`mapgendata` z selects floor. Multi-z buildings: different `oter_id` per floor or `mapgen_offset`.

Nextgen: `ZLevelResolver` (W8).

---

## Caching

Generated submaps stay in `mapbuffer` until evicted. Changing overmap `oter_id` does not
auto-invalidate existing submaps.

Nextgen: `SubmapCache` keyed by `(worldSeed, omtX, omtY, z)`.

---

## Relationship to nextgen

| BN | Nextgen |
| --- | --- |
| 2×2 submap window | 3×3 OMT patch optional |
| `oter_mapgen.pick` | `MapgenPicker` |
| Builtin mapgen | `BackgroundOmtSubmapBuilder` |
| `draw_connections` | Partial W13 |
| Lua mapgen | Not ported |

---

## Inputs

- `oter_id` + z at world OMT
- Neighbor submaps if already generated
- World seed, `LoadedGameData`
- Region groundcover / terrain resolve tables

## Outputs

- Filled `submap` ter/furn arrays (+ entities in full game)

## Failure modes

- No mapgen — fallback floor or debugmsg
- Invalid palette — skip object + debugmsg
- Missing ter id — load error or fallback

## Verification

1. Visit `house_09` — json interior matches fixture.
2. Adjacent `road_ns` OMTs — continuous pavement at shared submap edge.
3. Same seed + OMT → identical submap on second visit (cache hit).

**BN anchors:** `src/mapgen.cpp`, `src/mapgen_functions.cpp`, `src/mapgenformat.cpp`.
