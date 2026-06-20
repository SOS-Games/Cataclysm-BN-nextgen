# World generation — implementation guide

Procedural **overmap layout** and **on-demand submap generation** for Cataclysm-BN-nextgen.
This is a **separate track** from [mapgen preview](./MAPGEN_PREVIEW.md) (single-building import).

**Spec index:** [`docs/worldgen/`](./worldgen/README.md)  
**Plan:** [`worldgen/implementation-plan.md`](./worldgen/implementation-plan.md)

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
