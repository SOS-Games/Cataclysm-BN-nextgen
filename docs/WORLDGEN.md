# World generation — implementation guide

Procedural **overmap layout** and **on-demand submap generation** for Cataclysm-BN-nextgen.
This is a **separate track** from [mapgen preview](./MAPGEN_PREVIEW.md) (single-building import).

**Spec index:** [`docs/worldgen/`](./worldgen/README.md)  
**Plan:** [`worldgen/implementation-plan.md`](./worldgen/implementation-plan.md) (W1–W6) · [`worldgen/v2-implementation-plan.md`](./worldgen/v2-implementation-plan.md) (W7–W11) · [`worldgen/v3-implementation-plan.md`](./worldgen/v3-implementation-plan.md) (W13–W16) · [`worldgen/v4-implementation-plan.md`](./worldgen/v4-implementation-plan.md) (W17 Tier A)

---

## Why a new track

Mapgen preview v2 (P8–P15) runs **one** JSON mapgen into a `MapGrid`. BN worldgen adds:

```text
seed + region  →  overmap grid (OMT ids)  →  visit (x,y,z)  →  weighted mapgen pick  →  submap cache
```

That stack lives in BN `overmap.cpp`, `overmapbuffer.cpp`, and `mapbuffer` — not in
`JsonMapgenRunner`.

| Mapgen preview (done) | Worldgen (this track) |
| --- | --- |
| User picks definition / building | Generator places OMT types on a grid |
| One grid per import | Many 24×24 submaps per overmap tile |
| No neighbor context | Nested joins, roads, city edges |

Detail: [01-overview-and-scope](./worldgen/01-overview-and-scope.md).

---

## Prerequisites (already in repo)

| Upstream | Status |
| --- | --- |
| [Game data G1–G5](./GAME_DATA_LOADER.md) | done |
| [Mapgen preview P1–P15](./mapgen-preview/v2-implementation-plan.md) | done |
| [Map editor M1–M4](./MAP_EDITOR.md) | done |
| [Tileset loader](./TILESET_LOADER.md) | done |

---

## Milestones (W1–W6)

| PR | Focus | Unit doc |
| --- | --- | --- |
| **W1** | `overmap_terrain` loader + registry | [02](./worldgen/02-overmap-terrain-loader.md) |
| **W2** | Mini-overmap grid + editor view | [03](./worldgen/03-mini-overmap-grid.md) |
| **W3** | Visit-tile weighted mapgen → submap | [04](./worldgen/04-visit-tile-mapgen.md) |
| **W4** | City + static special placement | [05](./worldgen/05-city-and-special-placement.md) |
| **W5** | Rivers, highways, connections | [06](./worldgen/06-rivers-roads-connections.md) |
| **W6** | Mutable specials + joins | [07](./worldgen/07-mutable-specials-and-joins.md) |

**First playable milestone:** W2 + W3 — pan a small overmap, click an OMT, see generated terrain.

---

## Milestones v2 (W7–W11)

| PR | Focus | Unit doc |
| --- | --- | --- |
| **W7** | Building-aware visit (`MapVolumeBuilder`) | [13](./worldgen/13-building-aware-visit.md) |
| **W8** | Multi-z visit | [14](./worldgen/14-multi-z-visit.md) |
| **W9** | Region settings terrain | [15](./worldgen/15-region-settings-terrain.md) |
| **W10** | Overmap scale (64–180) | [16](./worldgen/16-overmap-scale.md) |
| **W11** | Procedural layout v2 | [17](./worldgen/17-procedural-layout-v2.md) |

**Roadmap:** [12-v2-parity-roadmap](./worldgen/12-v2-parity-roadmap.md) · **Plan:** [v2-implementation-plan](./worldgen/v2-implementation-plan.md)

**Start v2 with W7** — overmap visit should match mapgen picker for placed buildings.

---

## Milestones v3 (W13–W16)

| PR | Focus | Unit doc |
| --- | --- | --- |
| **W13** | Visit / mapbuffer fidelity | [19](./worldgen/19-visit-mapbuffer-fidelity.md) |
| **W14** | Layout parity phase 2 (region specials, cities) | [20](./worldgen/20-layout-parity-phase2.md) |
| **W15** | Exploration & world coordinates | [21](./worldgen/21-exploration-and-world-coords.md) |
| **W16** | World persistence (**deferred**) | [22](./worldgen/22-world-persistence.md) |

**Roadmap:** [18-world-map-v3-roadmap](./worldgen/18-world-map-v3-roadmap.md) · **Plan:** [v3-implementation-plan](./worldgen/v3-implementation-plan.md)

**Start v3 with W13** (stitch audit) or **W14** (region layout) in parallel; **W15** after visit trust is acceptable. **W16** waits until W15 session model exists.

**CDDA parity inventory (post–W14):** [23](./worldgen/23-cdda-parity-overview.md) · [24](./worldgen/24-cdda-layout-gaps.md) · [25](./worldgen/25-cdda-region-visit-world-gaps.md)

---

## Milestones v4 (W17 — Tier A layout)

| PR | Focus | Unit doc |
| --- | --- | --- |
| **W17a** | Urban OMT fill (shops, parks, houses, finales) | [26](./worldgen/26-tier-a-urban-layout.md) |
| **W17b** | In-city local road grid | [26](./worldgen/26-tier-a-urban-layout.md) |
| **W17c** | Inter-city highways + generate reorder | [26](./worldgen/26-tier-a-urban-layout.md) |
| **W17d–f** | Hydrology, trails, underground (optional) | [26](./worldgen/26-tier-a-urban-layout.md) |

**Roadmap:** [27-world-map-v4-roadmap](./worldgen/27-world-map-v4-roadmap.md) · **Plan:** [v4-implementation-plan](./worldgen/v4-implementation-plan.md)

**Start v4 with W17a** — parse `city.shops`/`parks`/`finales` and fill urban blobs. W15/W16 can run in parallel.

---

## Parallel tracks (not worldgen)

Improvements that unblock worldgen quality but are not overmap layout:

| Track | Doc |
| --- | --- |
| Mapgen runner polish after v2 | [08](./worldgen/08-mapgen-post-v2-polish.md) |
| Editor / tile rendering | [09](./worldgen/09-editor-rendering-polish.md) |
| Game data G6+ (items, monsters) | [10](./worldgen/10-game-data-g6-plus.md) |
| Building bundle gaps | [11](./worldgen/11-building-bundle-gaps.md) |

---

## Suggested first PR

**W1 — `OvermapTerrainRegistry`:** scan `data/json/overmap/overmap_terrain/`, index by id,
expose flags + `mapgen` list references. No procedural placement yet.

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/worldgen/
  overmap/
    OvermapTerrainDefinition.java
    OvermapTerrainLoader.java
    OvermapTerrainRegistry.java
```

---

## BN source map

| Concern | Location |
| --- | --- |
| Overmap generate | `src/overmap.cpp` — `overmap::generate` |
| OMT types | `data/json/overmap/overmap_terrain/` |
| Mapgen pick at visit | `src/mapgen.cpp` — `oter_mapgen` |
| Submap buffer | `src/mapbuffer.cpp`, `src/map.cpp` — `draw_map` |
| City buildings | `data/json/overmap/multitile_city_buildings.json` |
| Connections | `data/json/overmap/overmap_connection/` |

---

## Agent entry

1. Read [worldgen/README.md](./worldgen/README.md)
2. Implement **one W PR** from [implementation-plan.md](./worldgen/implementation-plan.md)
3. Do not expand mapgen preview scope mid-PR — call `JsonMapgenRunner` from worldgen layer
