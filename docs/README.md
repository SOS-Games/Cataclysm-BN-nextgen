# Cataclysm-BN-nextgen documentation

| Document | Purpose |
| --- | --- |
| [TILESET_LOADER.md](./TILESET_LOADER.md) | Loader implementation guide, milestone checklist, Java package map |
| [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) | In-game sprite browser (`TileDisplayScreen`) |
| [tileset-loader/README.md](./tileset-loader/README.md) | Loader specification index (ported from Cataclysm-BN) |
| [tileset-loader/implementation-plan.md](./tileset-loader/implementation-plan.md) | Module map, API contract, acceptance tests |

## Specification source

Behavioral specs for the tileset loader live in **`docs/tileset-loader/`** in this repository.
They were extracted from [Cataclysm-BN](../Cataclysm-BN) (`src/cata_tiles.cpp` and related files).
Each unit doc lists BN source anchors for cross-checking ambiguous behavior.

## Quick start

```bash
# Windows — opens the sprite viewer when ../Cataclysm-BN/gfx exists
gradlew.bat lwjgl3:run
```

See [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) for controls and [TILESET_LOADER.md](./TILESET_LOADER.md) for
`GfxPaths` / system properties.
