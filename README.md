# Cataclysm-BN-nextgen

LibGDX reimplementation of **Cataclysm: Bright Nights** — focused today on **worldgen preview**
(overmap layout + visit-tile walkaround), plus tileset loader, sprite viewer, and map editor.
Eventually a full game client. Reads existing BN `gfx/` / `data/` packs from disk.

Generated from [gdx-liftoff](https://github.com/libgdx/gdx-liftoff); desktop target is LWJGL3.

## Related repositories

This project reads assets and mirrors behavior from BN; it does not vendor those trees.

| Repository | GitHub | Typical local clone | Role |
| --- | --- | --- | --- |
| **Cataclysm: Bright Nights** | [cataclysmbn/Cataclysm-BN](https://github.com/cataclysmbn/Cataclysm-BN) | `../Cataclysm-BN` | C++ source of truth (`src/cata_tiles.cpp`, `src/mapdata.cpp`); ships `data/` and `gfx/` |
| **CDDA-Tilesets** | [I-am-Erk/CDDA-Tilesets](https://github.com/I-am-Erk/CDDA-Tilesets) | `../CDDA-Tilesets` | Community tileset packs under `gfx/` ([master tileset repo](https://github.com/I-am-Erk/CDDA-Tilesets)) |
| **cygnus-engine** | [SOS-Games/cygnus-engine](https://github.com/SOS-Games/cygnus-engine) | `../../Documents/cygnus-engine` (or sibling) | LibGDX reference — Gradle layout, path resolution, JSON I/O (not game logic) |

Recommended layout when developing all three:

```text
games/
  Cataclysm-BN/          # git clone cataclysmbn/Cataclysm-BN
  CDDA-Tilesets/         # git clone I-am-Erk/CDDA-Tilesets (optional)
  Cataclysm-BN-nextgen/  # this repo
```

## Documentation

| Doc | Purpose |
| --- | --- |
| [docs/README.md](docs/README.md) | Documentation index |
| [AGENTS.md](AGENTS.md) | Instructions for AI coding agents |
| [docs/TILESET_LOADER.md](docs/TILESET_LOADER.md) | Loader implementation guide and milestone |
| [docs/GAME_DATA_LOADER.md](docs/GAME_DATA_LOADER.md) | Game JSON loader (terrain, furniture) |
| [docs/MAPGEN_PREVIEW.md](docs/MAPGEN_PREVIEW.md) | JSON mapgen preview (buildings → grid) |
| [docs/WORLDGEN.md](docs/WORLDGEN.md) | Overmap generation + visit-tile submaps |
| [docs/MAP_EDITOR.md](docs/MAP_EDITOR.md) | Map editor |
| [docs/SPRITE_VIEWER.md](docs/SPRITE_VIEWER.md) | In-game sprite browser |
| [docs/INCREMENTAL_LOADING.md](docs/INCREMENTAL_LOADING.md) | Frame-sliced tileset load (`TilesetLoadSession`) |
| [docs/tileset-loader/](docs/tileset-loader/README.md) | Full loader specification (unit docs) |

## Platforms

- **`core`** — shared application logic (tileset loader, sprite viewer, game)
- **`lwjgl3`** — desktop launcher

## Gfx data (not bundled)

Tileset PNGs and JSON are **not** committed here. At dev time, point the loader at:

```text
../Cataclysm-BN/gfx/<tileset_id>/
```

See [docs/TILESET_LOADER.md](docs/TILESET_LOADER.md) for `GfxPaths` and system properties.

## Run

```bash
# Windows — main menu; auto-sets gfx/data paths when ../Cataclysm-BN exists
gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

Main menu: **Sprite Viewer**, **Map Editor**, **Worldgen** (overmap preview), **Configure Mods**.
Controls: [docs/SPRITE_VIEWER.md](docs/SPRITE_VIEWER.md), [docs/MAP_EDITOR.md](docs/MAP_EDITOR.md),
[docs/WORLDGEN.md](docs/WORLDGEN.md).

## Gradle

| Task | Description |
| --- | --- |
| `compileJava` | Compile all modules |
| `lwjgl3:run` | Start the application |
| `lwjgl3:jar` | Runnable JAR in `lwjgl3/build/libs/` |
| `test` | Unit tests |
| `clean` | Remove build output |

Use `./gradlew` or `gradlew.bat` from the project root.

## Status

- **Tileset loader** — v1 complete ([docs/TILESET_LOADER.md](docs/TILESET_LOADER.md))
- **Sprite viewer** — `TileDisplayScreen` ([docs/SPRITE_VIEWER.md](docs/SPRITE_VIEWER.md))
- **Map editor** — v1 + v2 R1–R3 / M5–M7 ([docs/MAP_EDITOR.md](docs/MAP_EDITOR.md))
- **Game data loader** — G1–G5 done ([docs/GAME_DATA_LOADER.md](docs/GAME_DATA_LOADER.md))
- **Mapgen preview** — P1–P7c + v2 P8–P15 done ([docs/MAPGEN_PREVIEW.md](docs/MAPGEN_PREVIEW.md))
- **Worldgen** — W1–W14 + W17 Tier A + city streets C1–C5 ([docs/WORLDGEN.md](docs/WORLDGEN.md)); W15 next
- **Full game client** — not started
