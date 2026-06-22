# Cataclysm-BN-nextgen documentation

| Document | Purpose |
| --- | --- |
| [TILESET_LOADER.md](./TILESET_LOADER.md) | Gfx loader — implementation guide, milestone checklist |
| [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md) | Game JSON loader — **PR slices G1–G5** + end-to-end timeline |
| [MAP_EDITOR.md](./MAP_EDITOR.md) | Paintable grid — **M1–M4 done**; **v2 R1–R3, M5–M7** ([plan](./map-editor/v2-implementation-plan.md)) |
| [MAPGEN_PREVIEW.md](./MAPGEN_PREVIEW.md) | JSON mapgen → grid preview — **P1–P7c + v2 P8–P15 done** |
| [WORLDGEN.md](./WORLDGEN.md) | Overmap + submap world generation — **W1–W11 done**; **W13–W15** ([v3 plan](./worldgen/v3-implementation-plan.md)); W16 persistence deferred |
| [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) | In-game sprite browser (`TileDisplayScreen`) |
| [INCREMENTAL_LOADING.md](./INCREMENTAL_LOADING.md) | Frame-sliced tileset load (`TilesetLoadSession`) |
| [tileset-loader/README.md](./tileset-loader/README.md) | Gfx loader specification index |
| [game-data-loader/README.md](./game-data-loader/README.md) | Game data specification index |
| [map-editor/README.md](./map-editor/README.md) | Map editor specification index |
| [mapgen-preview/README.md](./mapgen-preview/README.md) | Mapgen preview specification index (units 01–21, v2 plan) |
| [worldgen/README.md](./worldgen/README.md) | World generation specification index (units 01–22, W1–W16 plans) |

## Specification sources

| Tree | BN sources |
| --- | --- |
| `docs/tileset-loader/` | `src/cata_tiles.cpp`, gfx JSON |
| `docs/game-data-loader/` | `src/init.cpp`, `src/mapdata.cpp`, `data/json/` |
| `docs/map-editor/` | Nextgen-local; consumes loaders above |
| `docs/mapgen-preview/` | `src/mapgen.cpp`, `src/mapgenformat.cpp`, `data/json/mapgen/` |
| `docs/worldgen/` | `src/overmap.cpp`, `src/overmapbuffer.cpp`, `data/json/overmap_terrain/` |

## Quick start

```bash
# Windows — main menu when ../Cataclysm-BN/gfx exists
gradlew.bat lwjgl3:run
```

- **Sprite Viewer** — browse tileset sprites (`[`/`]` switch packs, spinner while loading)
- **Map Editor** — paint terrain grid; palette shows only tiles with art in the active pack

See [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) and [MAP_EDITOR.md](./MAP_EDITOR.md) for controls.
`GfxPaths` / `DataPaths` system properties: [TILESET_LOADER.md](./TILESET_LOADER.md),
[GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md).
