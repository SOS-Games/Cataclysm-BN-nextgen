# Cataclysm-BN-nextgen documentation

| Document | Purpose |
| --- | --- |
| [TILESET_LOADER.md](./TILESET_LOADER.md) | Gfx loader — implementation guide, milestone checklist |
| [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md) | Game JSON loader — terrain/furniture (in progress) |
| [MAP_EDITOR.md](./MAP_EDITOR.md) | Paintable grid editor — implementation guide |
| [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) | In-game sprite browser (`TileDisplayScreen`) |
| [INCREMENTAL_LOADING.md](./INCREMENTAL_LOADING.md) | Frame-sliced tileset load (`TilesetLoadSession`) |
| [tileset-loader/README.md](./tileset-loader/README.md) | Gfx loader specification index |
| [game-data-loader/README.md](./game-data-loader/README.md) | Game data specification index |
| [map-editor/README.md](./map-editor/README.md) | Map editor specification index |

## Specification sources

| Tree | BN sources |
| --- | --- |
| `docs/tileset-loader/` | `src/cata_tiles.cpp`, gfx JSON |
| `docs/game-data-loader/` | `src/init.cpp`, `src/mapdata.cpp`, `data/json/` |
| `docs/map-editor/` | Nextgen-local; consumes loaders above |

## Quick start

```bash
# Windows — opens the sprite viewer when ../Cataclysm-BN/gfx exists
gradlew.bat lwjgl3:run
```

See [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) for controls and [TILESET_LOADER.md](./TILESET_LOADER.md) for
`GfxPaths` / system properties.
