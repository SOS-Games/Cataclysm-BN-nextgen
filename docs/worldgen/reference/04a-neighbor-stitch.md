# 04a — Neighbor stitch and `connections_out`

First step of `overmap::generate`: import edge connection endpoints from adjacent overmap files
so rivers and roads can continue across 180×180 boundaries.

**Parent:** [04-generation-pipeline.md](./04-generation-pipeline.md)

---

## When neighbors are fetched

`overmap::open` (~6441) loads save or generates. On generate path:

```text
pointers[0] = get_existing(loc + (0, -1))   // north
pointers[1] = get_existing(loc + (0, +1))   // south
pointers[2] = get_existing(loc + (-1, 0))  // west
pointers[3] = get_existing(loc + (+1, 0))  // east

generate(pointers[0], pointers[3], pointers[1], pointers[2], enabled_specials)
//            north      east        south     west
```

**Important:** `get_existing` returns `nullptr` if the neighbor file was never generated or loaded.
First overmap in a world has no neighbors — stitch is a no-op.

`overmapbuffer::generate` may batch multiple coordinates on a thread pool; neighbors generated
earlier in the same batch become visible via `get_existing`.

---

## `populate_connections_out_from_neighbors`

**Anchor:** `src/overmap.cpp` **4013–4058**.

For each adjacent overmap (N/W/S/E), copies matching entries from `adjacent->connections_out`
into this overmap's `connections_out`, remapping coordinates to the shared edge:

| Side | Include neighbor point when | Map to local |
| --- | --- | --- |
| North | `p.y == OMAPY - 1` | `(p.x, 0, p.z)` |
| West | `p.x == OMAPX - 1` | `(0, p.y, p.z)` |
| South | `p.y == 0` | `(p.x, OMAPY - 1, p.z)` |
| East | `p.x == 0` | `(OMAPX - 1, p.y, p.z)` |

All `overmap_connection_id` keys are copied — not just `local_road`. Rivers, railroads, etc.
use the same map.

---

## `connections_out` type

```cpp
std::unordered_map<overmap_connection_id, std::vector<tripoint_om_omt>> connections_out;
```

Populated when:

1. Neighbor stitch (this unit)
2. `build_connection` reaches an out-of-bounds endpoint on a path (~5322–5334)
3. `place_roads` may add synthetic exits when `< 2` `local_road` points exist (~4622–4679)

Consumed by:

- `place_rivers` — edge continuity and new endpoint rolls (~4424+)
- `place_roads` — hub list includes `roads_out` (~4682–4693)
- Future overmaps on the shared edge

---

## `connection_cache`

**Type:** `overmap_connection_cache` (optional member on `overmap`).

Reset at generate start/end. `build_connection` calls `connection_cache->add(connection.id, z, start.pos)`
when a path is laid (~5354–5356). Used to avoid inconsistent partial state when multiple segments
share connection ids during one generate pass.

---

## River edge stitch (related)

Before `connections_out` logic, `place_rivers` also copies `river_center` onto the local edge when
the neighbor cell is river (~4424–4450). That is separate from `connections_out` but serves the
same cross-file continuity goal — see [04b-hydrology.md](./04b-hydrology.md).

---

## Nextgen gap

Nextgen generates one in-memory grid with no `overmapbuffer` tiling. **No `connections_out`
import/export.** River and road generators do not read adjacent files.

W16 / Tier C: [../22-world-persistence.md](../22-world-persistence.md).

---

## Inputs

- Neighbor overmap pointers (nullable)
- Neighbor `connections_out` maps

## Outputs

- This overmap's `connections_out` pre-filled for shared edges
- Empty `connection_cache` ready for carve phases

## Failure modes

- Null neighbor — edge skipped; local generate may synthesize road exits in `place_roads`
- Stale neighbor (generated with different seed/world) — visual discontinuity at boundary

## Verification

1. Generate overmap `(0,0)` then `(1,0)` — east edge of west file and west edge of east file share road OMT ids where `connections_out` aligned.
2. Log `connections_out[local_road].size()` before `place_roads` — non-zero when neighbor had road exit.

**BN anchors:** `src/overmap.cpp` (`populate_connections_out_from_neighbors`, `open`, `build_connection`).
