# Cataclysm-BN-nextgen

LibGDX reimplementation of **Cataclysm: Bright Nights** — tileset loader, sprite viewer, and
(eventually) full game client. Reads existing BN `gfx/` packs from disk.

Generated from [gdx-liftoff](https://github.com/libgdx/gdx-liftoff); desktop target is LWJGL3.

## Related repositories

| Repo | Role |
| --- | --- |
| [Cataclysm-BN](../Cataclysm-BN) | C++ source of truth for loader behavior (`src/cata_tiles.cpp`) |
| [CDDA-Tilesets](../CDDA-Tilesets) | Optional external `gfx/` packs |
| [cygnus-engine](../../Documents/cygnus-engine) | LibGDX **reference project** (paths, JSON I/O, Gradle layout) |

## Documentation

| Doc | Purpose |
| --- | --- |
| [docs/README.md](docs/README.md) | Documentation index |
| [AGENTS.md](AGENTS.md) | Instructions for AI coding agents |
| [docs/TILESET_LOADER.md](docs/TILESET_LOADER.md) | Loader implementation guide and milestone |
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
# Windows — sprite viewer; auto-sets gfx path when ../Cataclysm-BN/gfx exists
gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

Controls: [docs/SPRITE_VIEWER.md](docs/SPRITE_VIEWER.md).

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

- **Tileset loader** — v1 milestone complete (see [docs/TILESET_LOADER.md](docs/TILESET_LOADER.md))
- **Sprite viewer** — `TileDisplayScreen` grid browser; long-term in-game reference tool
- **Game client** — not started
