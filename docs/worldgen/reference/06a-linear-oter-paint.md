# 06a — LINEAR overmap paint (roads & trails)

How BN turns a path into directional `road_*` / `forest_trail_*` OMT ids. This is the
**overmap-layer** half of “road variations.” Visit-time curves live in
[08a-road-builtin-mapgen.md](./08a-road-builtin-mapgen.md).

**Parent:** [06-connections.md](./06-connections.md) · **Placement:** [04d-roads-trails-post.md](./04d-roads-trails-post.md)

**BN anchors:** `src/overmap.cpp` (`om_lines`, `oter_type_t::finalize`, `overmap::build_connection`)

---

## Why this exists

A single JSON type `"road"` with flag `LINEAR` expands into **16** concrete `oter_id`s at load
time (`road_ns`, `road_ew`, `road_ne`, `road_nes`, `road_nesw`, …). Carving does not pick
`_ew` / `_ns` heuristically — it **merges a 4-bit line mask** as the path walks, and rewrites
neighbors so junctions become tees / four-ways.

Without this pass, the overmap shows generic `road` and visit mapgen cannot know which arms
exist (unless it re-derives neighbors ad hoc).

---

## Flag & JSON

```json
{
  "type": "overmap_terrain",
  "id": "road",
  "flags": [ "LINEAR", "IGNORE_ROTATION_FOR_ADJACENCY" ],
  "extras": "road",
  "mapgen_straight": [ { "method": "builtin", "name": "road_straight" } ],
  "mapgen_curved":   [ { "method": "builtin", "name": "road_curved" } ],
  "mapgen_end":      [ { "method": "builtin", "name": "road_end" } ],
  "mapgen_tee":      [ { "method": "builtin", "name": "road_tee" } ],
  "mapgen_four_way": [ { "method": "builtin", "name": "road_four_way" } ]
}
```

| Rule | Detail |
| --- | --- |
| `LINEAR` ↔ `oter_flags::line_drawing` | Mutually exclusive with `NO_ROTATE` |
| Finalize | Registers 16 peers via `om_lines::all[i].suffix` |
| Mapgen slots | Loads five lists keyed by `mapgen_suffixes` |

`hiway_ns` / `hiway_ew` are **not** LINEAR — they are `NO_ROTATE` builtins named `highway`.

Same LINEAR machinery applies to `forest_trail`, `sewer`, `subway`, `railroad` where flagged.

---

## `om_lines` bitmask (N=1, E=2, S=4, W=8)

Namespace in `overmap.cpp` (~174–259). Bit order matches `om_direction` (N E S W).

| Line | Bits (WSEN) | Suffix | Mapgen bucket (`mapgen` index) |
| --- | ---: | --- | --- |
| 0 | 0000 | `_isolated` | four_way (4) |
| 1 | ---N | `_end_south` | end (2) |
| 2 | --E- | `_end_west` | end |
| 3 | --EN | `_ne` | curved (1) |
| 4 | -S-- | `_end_north` | end |
| 5 | -S-N | `_ns` | straight (0) |
| 6 | -SE- | `_es` | curved |
| 7 | -SEN | `_nes` | tee (3) |
| 8 | W--- | `_end_east` | end |
| 9 | W--N | `_wn` | curved |
| 10 | W-E- | `_ew` | straight |
| 11 | W-EN | `_new` | tee |
| 12 | WS-- | `_sw` | curved |
| 13 | WS-N | `_nsw` | tee |
| 14 | WSE- | `_esw` | tee |
| 15 | WSEN | `_nesw` | four_way |

Helpers:

| Function | Role |
| --- | --- |
| `set_segment(line, dir)` | OR bit for direction |
| `has_segment(line, dir)` | Test bit |
| `is_straight(line)` | `{1,2,4,5,8,10}` — ends + NS/EW |
| `rotate(line, dir)` | Bitwise rotate for OMT rotation |
| `from_dir(dir)` | NS→5, EW→10 (rotatable peer seed) |

Concrete id: `type.id + suffix` → e.g. `road` + `_nes` = `road_nes`.

Visit connectivity:

```cpp
bool oter_t::has_connection( om_direction::type dir ) const {
  if( id == "road_nesw_manhole" ) return true;  // special-case four-way
  return om_lines::has_segment( line, dir );
}
```

Mapgen id (visit pick key):

```text
oter_t::get_mapgen_id():
  if line_drawing:
    return type.id + mapgen_suffixes[om_lines::all[line].mapgen]
    // e.g. road_nes → "road_tee"
  else:
    return type.id
```

`mapgen_suffixes` = `_straight`, `_curved`, `_end`, `_tee`, `_four_way`.

JSON `mapgen_*` entries all resolve to builtin `road_straight` / … historically; the C++
function is unified as **`mapgen_road`** (see 08a) registered under those names.

---

## `build_connection` linear paint

**Anchor:** `overmap::build_connection` ~5261–5350.

For each node on a directed path:

```text
subtype = connection.pick_subtype_for(current_oter)   // field→road, water→bridge, …
prev_dir / new_dir from path edges

if subtype.terrain is LINEAR:
  new_line = existing line bits if cell already this connection, else 0
  OR segment toward new_dir
  OR segment toward opposite(prev_dir)

  for each cardinal neighbor:
    if neighbor is same connection and LINEAR:
      if neighbor is straight OR already has segment toward us:
        mutual connect: rewrite neighbor line + OR our bit toward neighbor
    else if neighbor rotatable and parallel:
      OR our bit toward neighbor
    else if out of bounds AND node is path start/end:
      OR bit toward edge; append to connections_out[connection.id]

  ter_set(pos, subtype.terrain.get_linear(new_line))
else:
  paint rotatable subtype.get_rotated(new_dir or prev_dir)
```

Also:

- `connection.clear_subtype_cache()` at path start — weighted bridge/dam variant per road
- After paint, `local_road` at z=0 may `elevate_bridges` for `IS_BRIDGE` cells (~5359+)

### City street special case

City centers often start with `road_nesw_manhole` (line 15 + manhole OMT). Occasional
`road_nesw` → manhole rewrite while laying streets (~5013).

---

## What nextgen does today

| BN | Nextgen |
| --- | --- |
| 16 LINEAR peers | Often plain `road` / `test_road` |
| Bitmask merge + neighbor rewrite | `pickTerrainForStep` → `_ew` / `_ns` / default only |
| `connections_out` edge exits | Partial / unused for roads |
| `elevate_bridges` stack | Weak river-center → bridge id pick |

Java: `OvermapConnectionDefinition.pickTerrainForStep`, `HighwayGenerator`,
`LocalRoadGenerator`, `OrthogonalPathCarver`.

---

## Inputs

- `overmap_connection` subtype terrain (`road`, `bridge`, …)
- Directed path nodes with cardinal directions
- Existing OMT layer (for mutual junction merge)

## Outputs

- LINEAR `oter_id`s with correct line bits
- Updated neighbor LINEAR cells at junctions
- Optional `connections_out` edge points
- Optional bridge elevation stacks

## Failure modes

- No subtype for current ground oter → `debugmsg`, path abort
- `new_line == invalid` → abort
- Missing LINEAR finalize peers → id resolution fails at load

## Verification

1. Carve NS then EW across same cell → ends as `road_nesw` (or tee), not two overwrites to `_ew`.
2. Neighbor previously `road_ns` gains east arm → becomes tee / four-way.
3. `get_mapgen_id()` for `road_ne` is `road_curved`; for `road_ns` is `road_straight`.

**Nextgen work:** [28-road-rendering-fidelity.md](../28-road-rendering-fidelity.md) · Visit: [08a](./08a-road-builtin-mapgen.md)
