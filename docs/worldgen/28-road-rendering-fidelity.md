# 28 — Road rendering fidelity

Close the gap between **BN roads** (LINEAR OMTs + `mapgen_road` + region extras) and nextgen’s
path-carve + thin pavement stub.

**Status:** R1–R5 implemented (v1 scope). See verification below.

**BN truth:** [reference/06a-linear-oter-paint.md](./reference/06a-linear-oter-paint.md) ·
[reference/08a-road-builtin-mapgen.md](./reference/08a-road-builtin-mapgen.md)

**Related:** [06-rivers-roads-connections.md](./06-rivers-roads-connections.md) (W5 done) ·
[24-cdda-layout-gaps.md](./24-cdda-layout-gaps.md) § Roads · [26-tier-a-urban-layout.md](./26-tier-a-urban-layout.md) (W17b–c layout)

---

## User-visible symptom

Overmap roads look like generic segments; visiting a road tile shows a flat pavement cross
without bends, tees that match LINEAR topology, sidewalks, parked cars, litter, or roadside
map extras (roadworks, blocks, wrecks).

Layout connectivity (city grid / highways) may already exist from W17 — **look and roadside
content** do not.

---

## Gap inventory

| Layer | BN | Nextgen | Priority |
| --- | --- | --- | --- |
| A. Overmap LINEAR ids | `build_connection` bitmask + neighbor rewrite | `_ew`/`_ns`/default only | P0 |
| B. Visit builtin | `mapgen_road` (curves, sidewalks, plaza, vehicles, items) | `BackgroundOmtSubmapBuilder.buildRoad` | P0 |
| C. Map extras | `region_extras["road"]` after draw | Missing | P1 |
| D. Bridges | `elevate_bridges` + bridgehead extras | Weak subtype pick | P1 |
| E. Neighbor road stitch | `place_roads` + `connections_out` | Rivers wired (W16); roads not | P2 |
| F. Highway OMTs | `hiway_*` + `mapgen_highway` | May use same stub as road | P2 |

---

## Suggested PR slices

### R1 — LINEAR polish after carve (overmap) — **done**

**Goal:** After `LocalRoadGenerator` / `HighwayGenerator` / connection carvers, rewrite road
cells to proper `road_*` (or fixture `test_road_*`) line ids — same idea as
`RiverPolisher.polishDirectional`.

| Deliverable | Notes |
| --- | --- |
| `OmLines` | `connection/OmLines.java` |
| `RoadConnectionPolisher` | NESW neighbor → LINEAR id; hooked in `OvermapGenerator` + neighbor repolish |
| Fixtures | Full `test_road_*` peer set in `test_omts.json` |
| Tests | `RoadLinearPolishTest` |

---

### R2 — Visit `mapgen_road` subset (submap) — **done**

**Goal:** Replace or gate `BackgroundOmtSubmapBuilder.buildRoad` with a topology-aware painter.

| Deliverable | Notes |
| --- | --- |
| `BuiltinRoadMapgen` | NESW from LINEAR id / neighbors; arms; yellow dashes; curve fillets; sidewalks |
| Wired via | `BackgroundOmtSubmapBuilder` → `BuiltinRoadMapgen.generate` |
| Deferred | Full plaza / cul-de-sac / BN vehicle tables |

---

### R3 — Road content lite — **done (stub)**

| Deliverable | Notes |
| --- | --- |
| Litter / bench | Sparse `f_rubble` / `f_bench` on road visits |
| Vehicles / monsters | Deferred |

---

### R4 — Region map extras (`road`) — **done (stub apply)**

| Deliverable | Notes |
| --- | --- |
| `RegionMapExtrasSettings.roadDefaults` | BN-like chance 75 + weights |
| `RoadMapExtras.roll` | After visit paint |
| Apply | Terrain/furniture stubs (`mx_roadworks` barricades, wrecks, crater dirt) — not full BN `MapExtras` functions |

---

### R5 — Bridges & neighbor roads — **done (v1)**

| Deliverable | Notes |
| --- | --- |
| `BridgeElevator.elevateCrossings` | Road with water on opposite sides → bridge OMT |
| `RoadEdgeStitcher` | Copy road edge OMTs from N/E/S/W neighbors before city/highway carve |
| Neighbor buffer | `OvermapNeighborGrid.repolishAt` re-stitches + elevates + LINEAR polish |

---

## Implementation notes

### Do not double-paint

Today roads are claimed by `BackgroundOmtSubmapBuilder` **before** JSON/builtin pick. R2 should
either:

1. Implement fidelity inside the background builder, or  
2. Stop claiming LINEAR roads and route through a dedicated `BuiltinRoadMapgen` registered like
   BN’s `get_mapgen_cfunction`.

Prefer (2) long-term so `get_mapgen_id()` / weighted JSON overrides can work.

### Fixtures

Extend `test_omts.json` with LINEAR-style peers if not already complete:
`test_road_ns`, `_ew`, `_ne`, `_nes`, `_nesw`, `_end_*`, etc., matching polish expectations.

### Rivers analogy

Hydrology already did neighbor-aware polish (`RiverPolisher` + W16 buffer). Roads need the same
**pattern** at overmap layer; visit layer is heavier (sidewalks/extras).

---

## Non-goals (this unit)

- Full Lua / every `mx_*` function body
- Exact BN vehicle spawn density parity
- Draw-time overmap glyph art (tileset)
- Replacing W17 city/highway **placement** algorithms

---

## Follow-up (layout connectivity, post–R5)

Not visit paint — overmap graph cleanup after streets/highways/specials:

| Class | Notes |
| --- | --- |
| `ParallelRoadLaneDissolver` | Solid 2×2 only |
| `RoadGapFiller` | ≥3-neighbor natural holes |
| `RoadTipBridger` | Tip gaps + diagonal L-joins; after specials |

See [worldgen/README.md](./README.md#road-connectivity-polish-postc5).

---

## Verification

1. Fixed seed overmap: majority of road cells are directional `road_*`, not bare `road`.
2. Visit tee / curve / four-way fixtures → distinct pavement masks (golden hash or sampled cells).
3. Urban adjacency with sidewalk flag → sidewalk strips present.
4. With extras enabled: at least one `mx_*` applies across N visits (seed search OK).
5. Existing worldgen tests still pass; add `RoadLinearPolishTest`, `BuiltinRoadMapgenTest`.

---

## Doc map

| Doc | Role |
| --- | --- |
| [06a](./reference/06a-linear-oter-paint.md) | BN LINEAR / `build_connection` |
| [08a](./reference/08a-road-builtin-mapgen.md) | BN `mapgen_road` + extras |
| This file | Nextgen PR contract |
| [06](./reference/06-connections.md) | Connection templates |
| [04d](./reference/04d-roads-trails-post.md) | `place_roads` / trails |
