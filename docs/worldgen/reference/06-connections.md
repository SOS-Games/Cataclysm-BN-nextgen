# 06 — Connections

`overmap_connection` templates define how linear features carve paths across OMT cells and how
submaps stitch at edges.

**Headers:** `src/overmap_connection.h`, `src/overmap_connection.cpp` · **Carve:** `src/overmap.cpp`
(`build_connection`, `connect_closest_points`, `lay_out_connection`, `lay_out_street`).

---

## JSON location

Primary file: `data/json/overmap/overmap_connections.json`

Mod examples: `data/mods/desert_region/desert_overmap_connections.json`,
`data/mods/innawoods/overmap_connections.json`.

```json
{
  "type": "overmap_connection",
  "id": "local_road",
  "default_terrain": "road",
  "subtypes": [
    {
      "terrain": "road",
      "locations": [ "field", "road" ],
      "basic_cost": 0
    },
    {
      "terrain": "bridge",
      "locations": [ "water" ],
      "basic_cost": 120,
      "weight": 4
    }
  ]
}
```

---

## Stock connection inventory (BN core)

| Id | `default_terrain` | Layout | Used by |
| --- | --- | --- | --- |
| `local_road` | `road` | default (linear) | Cities, `place_roads` |
| `sewer_tunnel` | `sewer` | default | City sewers (z < 0) |
| `subway_tunnel` | `subway` | **`p2p`** | Subway specials / missions |
| `forest_trail` | `forest_trail` | default | `place_forest_trails` |
| `local_railroad` | `railroad` | default | Rail specials (not `place_roads`) |

**Note:** BN `place_roads` uses **`local_road` only**. There is no `highway` connection id in
core JSON — inter-city links are plain `local_road` segments. Nextgen adds separate highway logic.

Rivers **do not** use `overmap_connection` — direct `river_center` paint ([04b](./04b-hydrology.md)).

---

## Connection struct

| Field | Role |
| --- | --- |
| `id` | Template name |
| `default_terrain` | Fallback linear `oter_type` |
| `layout` | `city` or `p2p` (default linear if omitted) |
| `subtypes[]` | Per-terrain overrides |

### Subtype fields

| Field | Role |
| --- | --- |
| `terrain` | `oter_type` id to paint (linear or rotatable) |
| `locations` | Allowed **ground** `overmap_location` tags |
| `basic_cost` | Pathfinding cost penalty |
| `weight` | Random choice among matching subtypes (bridges vs dams) |
| `flags` | `"ORTHOGONAL"` → no turns unless existing connection |

Methods: `pick_subtype_for(oter_id)`, `can_start_at`, `has(oter_id)`.

---

## Layout modes

| `overmap_connection_layout` | Behavior |
| --- | --- |
| `city` (default) | Grid/street friendly; used with `lay_out_street` |
| `p2p` | Point-to-point; orthogonal flag common (`subway_tunnel`) |

---

## Carving API

| Function | Anchor | Role |
| --- | --- | --- |
| `build_connection(conn, path, z)` | ~5261 | Paint linear/rotatable along `directed_path` |
| `build_connection(src, dest, z, conn, …)` | ~5391 | Pathfind + build |
| `connect_closest_points(points, z, conn)` | ~5400 | Greedy closest-pair chaining |
| `lay_out_connection` | ~5070 | A* with subtype costs |
| `lay_out_street` | (city) | Street grid path from center |
| `populate_connections_out_from_neighbors` | 4013 | Edge stitch ([04a](./04a-neighbor-stitch.md)) |

### Linear paint algorithm (sketch)

For each node in path:

1. `subtype = connection.pick_subtype_for(current_oter)`
2. If linear: merge line bitmask with prev/next direction + neighbor mutual connect
3. If path endpoint at map edge → append to `connections_out[connection.id]`
4. If `local_road` at z=0 → collect bridge points → `elevate_bridges` (~5359)

`connection.clear_subtype_cache()` at path start — random bridge/dam variant per road.

---

## `connect_closest_points`

```text
for i in 0..n-1:
  find j>i minimizing trig_dist(points[i], points[j])
  if closest > 0: build_connection(points[i], points[j], z, connection)
```

Not a global MST — greedy per index. Used by `place_roads` and forest trails.

Pathfinding: `lay_out_connection` rejects cells with no subtype, existing explored submaps
(when `must_be_unexplored`), wrong rotatable direction.

---

## `place_roads` (summary)

See [04d-roads-trails-post.md](./04d-roads-trails-post.md). Hubs: `connections_out[local_road]` +
city centers → `connect_closest_points`.

---

## Submap stitching

`map::draw_connections(dat)` after mapgen (~4407 in `mapgen.cpp`) — aligns tile edges using
same connection/subtype rules. Edge constants: `SOUTH_EDGE = 2*SEEY-1`, `EAST_EDGE = 2*SEEX-1`.

See [08-omt-to-submap.md](./08-omt-to-submap.md).

---

## Validation

`overmap_connection::check` — each subtype `terrain` exists; locations resolve; layout valid.

`overmap_connections::check_consistency` at load finalize.

---

## Relationship to nextgen

| BN | Nextgen |
| --- | --- |
| `OvermapConnectionLoader` | `connection.*` package |
| `place_roads` | `LocalRoadGenerator` |
| Rivers | `RiverGenerator` (not connection-based) |
| `connections_out` | Not implemented |
| Highways | **Nextgen-only** `HighwayGenerator` |

`BackgroundOmtSubmapBuilder` uses connection registry for visit-time road paint.

---

## Inputs

- Connection JSON
- Endpoint lists, terrain registry
- Optional neighbor submaps (visit)

## Outputs

- Painted linear `oter_id` chains
- Updated `connections_out`
- Bridge/dam OMT stacks

## Failure modes

- No subtype for terrain — `debugmsg`, segment abort
- Invalid line bitmask — path rejected
- Pathfinding failure — segment dropped

## Verification

1. `--check-mods` connection checks pass.
2. City A to city B: continuous `road_*` on same overmap.
3. Road crosses river: `bridge` or `dam` OMT from weighted subtype.
4. Adjacent submaps: pavement continuous at shared edge after visit.

**BN anchors:** `src/overmap_connection.cpp`, `src/overmap.cpp`, `data/json/overmap/overmap_connections.json`.
