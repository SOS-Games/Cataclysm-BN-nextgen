# Cataclysm-BN-nextgen documentation

| Document | Purpose |
| --- | --- |
| [TILESET_LOADER.md](./TILESET_LOADER.md) | Gfx loader — implementation guide, milestone checklist |
| [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md) | Game JSON loader — **PR slices G1–G5** + end-to-end timeline |
| [MAP_EDITOR.md](./MAP_EDITOR.md) | Paintable grid — **M1–M4 done**; controls and run guide |
| [MAPGEN_PREVIEW.md](./MAPGEN_PREVIEW.md) | JSON mapgen → grid preview — **P1–P6 done** (building bundles + OMT stitch) |
| [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) | In-game sprite browser (`TileDisplayScreen`) |
| [INCREMENTAL_LOADING.md](./INCREMENTAL_LOADING.md) | Frame-sliced tileset load (`TilesetLoadSession`) |
| [tileset-loader/README.md](./tileset-loader/README.md) | Gfx loader specification index |
| [game-data-loader/README.md](./game-data-loader/README.md) | Game data specification index |
| [map-editor/README.md](./map-editor/README.md) | Map editor specification index |
| [mapgen-preview/README.md](./mapgen-preview/README.md) | Mapgen preview specification index (units 01–08) |

## Specification sources

| Tree | BN sources |
| --- | --- |
| `docs/tileset-loader/` | `src/cata_tiles.cpp`, gfx JSON |
| `docs/game-data-loader/` | `src/init.cpp`, `src/mapdata.cpp`, `data/json/` |
| `docs/map-editor/` | Nextgen-local; consumes loaders above |
| `docs/mapgen-preview/` | `src/mapgen.cpp`, `src/mapgenformat.cpp`, `data/json/mapgen/` |

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
