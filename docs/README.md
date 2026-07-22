# Cataclysm-BN-nextgen documentation

| Document | Purpose |
| --- | --- |
| [TILESET_LOADER.md](./TILESET_LOADER.md) | Gfx loader — implementation guide, milestone checklist |
| [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md) | Game JSON loader — **PR slices G1–G5** + end-to-end timeline |
| [MAP_EDITOR.md](./MAP_EDITOR.md) | Paintable grid — **M1–M4 done**; **v2 R1–R3, M5–M7** ([plan](./map-editor/v2-implementation-plan.md)) |
| [MAPGEN_PREVIEW.md](./MAPGEN_PREVIEW.md) | JSON mapgen → grid preview — **P1–P7c + v2 P8–P15 done** |
| [WORLDGEN.md](./WORLDGEN.md) | Overmap + submap world generation — **W1–W14 + W17 + C1–C5 done**; **W15** next ([v3](./worldgen/v3-implementation-plan.md) / [v4](./worldgen/v4-implementation-plan.md)); W16 persistence deferred |
| [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) | In-game sprite browser (`TileDisplayScreen`) |
| [INCREMENTAL_LOADING.md](./INCREMENTAL_LOADING.md) | Frame-sliced tileset load (`TilesetLoadSession`) |
| [tileset-loader/README.md](./tileset-loader/README.md) | Gfx loader specification index |
| [game-data-loader/README.md](./game-data-loader/README.md) | Game data specification index |
| [map-editor/README.md](./map-editor/README.md) | Map editor specification index |
| [mapgen-preview/README.md](./mapgen-preview/README.md) | Mapgen preview specification index (units 01–21, v2 plan) |
| [worldgen/README.md](./worldgen/README.md) | World generation specification index (units 01–29, W1–W17 / C1–C5 / R1–R5) |
| [worldgen/reference/README.md](./worldgen/reference/README.md) | BN overmap/worldgen C++ specification (language-agnostic) |

## Specification sources

| Tree | BN sources |
| --- | --- |
| `docs/tileset-loader/` | `src/cata_tiles.cpp`, gfx JSON |
| `docs/game-data-loader/` | `src/init.cpp`, `src/mapdata.cpp`, `data/json/` |
| `docs/map-editor/` | Nextgen-local; consumes loaders above |
| `docs/mapgen-preview/` | `src/mapgen.cpp`, `src/mapgenformat.cpp`, `data/json/mapgen/` |
| `docs/worldgen/` | Nextgen contracts; BN layout in `worldgen/reference/` |
| `docs/worldgen/reference/` | `src/overmap.cpp`, `src/overmapbuffer.cpp`, `src/regional_settings.cpp`, `data/json/overmap_terrain/` |

## Quick start

```bash
# Windows — main menu when ../Cataclysm-BN/gfx exists
gradlew.bat lwjgl3:run
```

- **Sprite Viewer** — browse tileset sprites (`[`/`]` switch packs, spinner while loading)
- **Map Editor** — paint terrain grid; palette shows only tiles with art in the active pack
- **Worldgen** — opens map editor in overmap mode (generate layout, click OMT → walkaround)
- **Configure Mods** — enable BN mods for game data / mapgen scan

Cold start from **Worldgen** prefers the **tileset** loading overlay while gfx and catalogs load
in parallel (tiles dominate wall time).

See [SPRITE_VIEWER.md](./SPRITE_VIEWER.md), [MAP_EDITOR.md](./MAP_EDITOR.md), and
[WORLDGEN.md](./WORLDGEN.md) for controls. `GfxPaths` / `DataPaths` system properties:
[TILESET_LOADER.md](./TILESET_LOADER.md), [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md).
