# AGENTS.md

Instructions for AI coding agents working in **Cataclysm-BN-nextgen** (LibGDX Java).

## Communication

- Summaries must be concise: what changed, why, and how to run it.
- Do not commit unless the user asks.

## Project role

This repo implements the **Cataclysm-BN tileset loader**, an **in-game sprite viewer**, a
**map editor** (v1), and (eventually) the full game client on LibGDX. It loads
**existing BN packs** from disk (`gfx/<pack>/`, `data/json/`); it does not ship those assets.

## Related repositories

| Repository | GitHub | Typical local clone |
| --- | --- | --- |
| Cataclysm: Bright Nights | [cataclysmbn/Cataclysm-BN](https://github.com/cataclysmbn/Cataclysm-BN) | `../Cataclysm-BN` |
| CDDA-Tilesets | [I-am-Erk/CDDA-Tilesets](https://github.com/I-am-Erk/CDDA-Tilesets) | `../CDDA-Tilesets` |
| cygnus-engine (LibGDX patterns) | [SOS-Games/cygnus-engine](https://github.com/SOS-Games/cygnus-engine) | `../../Documents/cygnus-engine` |

See [README.md](README.md#related-repositories) for roles and suggested directory layout.

## Specification (read first)

### Gfx (tileset loader — done)

Canonical specs: **`docs/tileset-loader/`**. BN C++ in `src/cata_tiles.cpp`.

| Doc | Path |
| --- | --- |
| Loader guide | `docs/TILESET_LOADER.md` |
| Sprite viewer | `docs/SPRITE_VIEWER.md` |
| Incremental loading | `docs/INCREMENTAL_LOADING.md` |
| Spec index | `docs/tileset-loader/README.md` |
| Implementation plan | `docs/tileset-loader/implementation-plan.md` |

### Game data loader (in progress)

Specs: **`docs/game-data-loader/`**. BN C++ in `src/init.cpp`, `src/mapdata.cpp`.

**G1–G5 done** (paths, terrain, furniture, validation, mod order).

| Doc | Path |
| --- | --- |
| Guide | `docs/GAME_DATA_LOADER.md` |
| Spec index | `docs/game-data-loader/README.md` |
| Implementation plan | `docs/game-data-loader/implementation-plan.md` |

### Map editor (v1 done)

Specs: **`docs/map-editor/`**. Consumes game data + tileset loaders.

| Doc | Path |
| --- | --- |
| Guide | `docs/MAP_EDITOR.md` |
| Spec index | `docs/map-editor/README.md` |

### Mapgen preview (done)

Specs: **`docs/mapgen-preview/`**. BN JSON mapgen → `MapGrid` (not full worldgen).

| Doc | Path |
| --- | --- |
| Guide | `docs/MAPGEN_PREVIEW.md` |
| Spec index | `docs/mapgen-preview/README.md` |
| Implementation plan | `docs/mapgen-preview/implementation-plan.md` |

**PR slices:** P1–P7c done (… → OMT stitch → bundle discovery → implicit bundles → whole special). **v2:** [v2-implementation-plan](docs/mapgen-preview/v2-implementation-plan.md) (P8 done → P9 setmap next). Index: [08-v2-parity-roadmap](docs/mapgen-preview/08-v2-parity-roadmap.md), units [14–21](docs/mapgen-preview/14-mod-scan-paths.md).

| Docs index | `docs/README.md` |

Follow each plan’s **v1 milestones** unless the user requests full BN parity.

**Sprites-only scope:** skip ASCII / `fallback.png` (units 04d, 07c). State modifiers (07d)
and dynamic atlas (A1) are implemented; draw-time UV warps are optional.

## Reference implementation (patterns, not porting game logic)

Use **[cygnus-engine](https://github.com/SOS-Games/cygnus-engine)** as a LibGDX/Java style reference:

| Path | Use for |
| --- | --- |
| `../../Documents/cygnus-engine/AGENTS.md` | Agent workflow, concise summaries |
| `../../Documents/cygnus-engine/MODDING.md` | Gradle layout (`core` / `lwjgl3`) |
| `../../Documents/cygnus-engine/core/.../ModPaths.java` | Resolving data dirs from varying CWD |
| `../../Documents/cygnus-engine/core/.../ModJson.java` | LibGDX `Json` helper |
| `../../Documents/cygnus-engine/core/.../*DataIO.java` | Load JSON → typed records |

Do **not** copy space-game logic into nextgen. Reuse **structure** (paths, JSON I/O, module
split), not domain code.

## This repo layout

```text
core/     Shared logic (package io.gdx.cdda.bn.nextgen)
lwjgl3/   Desktop launcher
assets/   LibGDX assets (UI later); tileset PNGs are not bundled by default
docs/     Loader specs, implementation guide, sprite viewer docs
```

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/
  tileset/                    # gfx loader — done
  gamedata/                   # game JSON — docs/game-data-loader/
  map/                        # MapGrid, MapFileIO — docs/map-editor/
  mapgen/                     # P1–P4 done — docs/mapgen-preview/
  view/
    MainMenuScreen.java
    TileDisplayScreen.java    # sprite viewer
    MapEditorScreen.java
    MapPalettePanel.java
    MapEditorToolbar.java
    TileSpriteResolver.java
    ScreenInput.java
  Main.java
```

## Gfx / BN data paths

Clone [Cataclysm-BN](https://github.com/cataclysmbn/Cataclysm-BN) and optionally
[CDDA-Tilesets](https://github.com/I-am-Erk/CDDA-Tilesets) as siblings:

```text
../Cataclysm-BN/gfx/              # tilesets (GfxPaths / cdda.gfx.roots)
../Cataclysm-BN/data/             # game JSON (DataPaths / cdda.data.roots)
../CDDA-Tilesets/gfx/             # optional external tilesets repo
```

`GfxPaths` resolves gfx roots from `cdda.gfx.roots` and common relative paths.
`DataPaths` resolves data roots from `cdda.data.roots` and common relative paths.

## Run & build

```bash
gradlew.bat lwjgl3:run
gradlew.bat compileJava
gradlew.bat test
```

## Implementation order

**Tileset loader:** complete — see `docs/tileset-loader/implementation-plan.md`.

**Game data loader:** G1–G5 done — see `docs/game-data-loader/implementation-plan.md`.

**Map editor:** M1–M4 done — `docs/map-editor/implementation-plan.md`.

**Mapgen preview:** P1–P7c done; see `docs/mapgen-preview/implementation-plan.md` for v2 items.

Sprite viewer follow-ups: search, detail pane. Gfx: draw-time seasonal/tint/warp.

**Loading:** use `TilesetLoadSession` for UI; never `TilesetLoader.load` on a worker thread
(see `docs/INCREMENTAL_LOADING.md`).

Each unit doc ends with **Verification** — turn those into JUnit tests where practical.

## Code style

- Match existing Java in this repo and cygnus-engine nearby files.
- Prefer small focused classes over large god-objects.
- LibGDX: `Texture`, `TextureRegion` for GPU; `Pixmap` for PNG decode during load.
- Sprite viewer: pixel-aligned rendering (`HdpiMode.Pixels`), fixed cell size on window resize.
- Incremental load: `TilesetLoadSession.step()` once per frame on the render thread.
