# AGENTS.md

Instructions for AI coding agents working in **Cataclysm-BN-nextgen** (LibGDX Java).

## Communication

- Summaries must be concise: what changed, why, and how to run it.
- Do not commit unless the user asks.

## Project role

This repo implements the **Cataclysm-BN tileset loader**, an **in-game sprite viewer**, and
(eventually) the game client on LibGDX. It loads **existing BN tileset packs** from disk
(`gfx/<pack>/`); it does not ship those assets.

## Specification (read first)

Canonical loader specs live in **`docs/tileset-loader/`** in this repository (ported from
Cataclysm-BN). BN C++ files cited in each unit doc are the authority for ambiguous behavior.

| Doc | Path |
| --- | --- |
| Docs index | `docs/README.md` |
| Loader guide | `docs/TILESET_LOADER.md` |
| Sprite viewer | `docs/SPRITE_VIEWER.md` |
| Spec index | `docs/tileset-loader/README.md` |
| **Implementation plan** | `docs/tileset-loader/implementation-plan.md` |
| Unit specs | `docs/tileset-loader/01-…` through `09-…`, `appendix-dynamic-atlas.md` |

Follow the plan’s **v1 milestones** unless the user requests full BN parity.

**Sprites-only scope:** skip ASCII / `fallback.png` (units 04d, 07c). State modifiers (07d)
and dynamic atlas (A1) are implemented; draw-time UV warps are optional.

## Reference implementation (patterns, not porting game logic)

Use **cygnus-engine** as a LibGDX/Java style reference:

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
  tileset/
    GfxPaths.java
    TilesetDiscovery.java
    TilesetLoader.java
    load/ parse/ atlas/ mod/ model/ validate/
  view/
    TileDisplayScreen.java    # sprite viewer — see docs/SPRITE_VIEWER.md
  Main.java                   # currently boots into sprite viewer
```

## Gfx / BN data paths

```text
../Cataclysm-BN/gfx/              # game gfx root (auto-detected by lwjgl3:run)
../CDDA-Tilesets/gfx/             # optional external tilesets repo
```

`GfxPaths` resolves roots from `cdda.gfx.roots` and common relative paths.

## Run & build

```bash
gradlew.bat lwjgl3:run
gradlew.bat compileJava
gradlew.bat test
```

## Implementation order

See **Suggested first PR slices** in `docs/tileset-loader/implementation-plan.md`.

v1 loader slices are complete. Future work: sprite viewer features (search, tileset picker),
game rendering, draw-time seasonal/tint/warp support.

Each unit doc ends with **Verification** — turn those into JUnit tests where practical.

## Code style

- Match existing Java in this repo and cygnus-engine nearby files.
- Prefer small focused classes over large god-objects.
- LibGDX: `Texture`, `TextureRegion` for GPU; `Pixmap` for PNG decode during load.
- Sprite viewer: pixel-aligned rendering (`HdpiMode.Pixels`), fixed cell size on window resize.
