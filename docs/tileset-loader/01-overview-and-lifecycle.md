# 01 — Overview and lifecycle

High-level map of when and how tilesets are loaded in Cataclysm-BN. This document
describes orchestration only. File formats, parsing, and GPU upload are covered in
later units (see [README](./README.md)).

---

## Purpose

The tileset loader bridges three concerns:

1. **Discovery** — find installed tilesets on disk and expose them as selectable ids
   (unit 02).
2. **Registration** — record `mod_tileset` JSON during game-data load; sprites load later
   (unit 04f).
3. **Load** — read manifest + config + images, build an in-memory tileset used for
   rendering (units 05–08).

The loader is only active when the build includes tile graphics support (`TILES`). Without
it, all paths described here are absent.

---

## Core concepts

### Tile contexts

Two independent **tile contexts** may exist at runtime:

| Context | Option key | Used for |
| --- | --- | --- |
| Main | `TILES` | Game map, UI previews, most drawing |
| Overmap | `OVERMAP_TILES` | Overmap window when overmap tiles are enabled |

Each context holds its own loaded tileset instance. If `TILES` and `OVERMAP_TILES` resolve to
the **same id string**, both views share one context object (pointer alias). If they differ,
a second context is created and loaded separately.

Overmap drawing additionally requires cached flags `use_tiles` and `use_tiles_overmap`
(from options `USE_TILES` and `USE_TILES_OVERMAP`).

### Registry vs load

| Phase | When | Result |
| --- | --- | --- |
| Registry build | Options initialization | Map: `tileset_id → directory_path` (global `TILESETS`) |
| Mod tileset registration | Game-data load (per mod) | List: `all_mod_tilesets` with compatibility metadata |
| Full load | See triggers below | Populated `tileset` inside each active context |

Registry scan does **not** load images or parse `tile_config.json` tile definitions.

### Load entry point

All full loads funnel through a single operation on the tile context (BN:
`cata_tiles::load_tileset`), which delegates to an internal loader (`tileset_loader::load`).

**Parameters:**

| Parameter | Meaning |
| --- | --- |
| `tileset_id` | Internal id from the registry (matches `NAME` in `tileset.txt`) |
| `mod_list` | Ordered list of active mods for the current world; used for cache invalidation and mod tileset merge |
| `precheck` | If true, parse only `tile_info` from config then return — no images, no tile defs, no mod merge |
| `force` | If true, skip the “already loaded” cache and reload |
| `pump_events` | If true, process UI/window events during long loads (implementation detail for responsiveness; not required for correctness) |

**Atomic swap:** The loader builds a new in-memory tileset in a temporary object. The
context replaces its current tileset only after the load succeeds. A failed load leaves the
previous tileset intact (except when there was no prior tileset).

**Side effects on success:** Reset mutation overlay ordering; set draw scale to default;
configure minimap mode from iso flag in `tile_info`.

---

## Lifecycle timeline

```text
Application start
    │
    ├─► Options init
    │       └─► Scan gfx directories → TILESETS registry + option choices
    │
    ├─► Graphics subsystem init (SDL)
    │       ├─► Create main tile context
    │       ├─► Precheck load (TILES)          mod_list = empty
    │       └─► Precheck load (OVERMAP_TILES)  if different id; may alias context
    │
    ├─► Main menu / world setup
    │
    ├─► Game-data load (mods + core JSON)
    │       └─► Register mod_tileset entries → all_mod_tilesets
    │
    ├─► Finalize game data
    │       └─► Full load (TILES + OVERMAP_TILES)   mod_list = active world mods
    │               └─► Tile loading report
    │
    └─► Runtime
            ├─► Option change (tile-related) → optional reload
            └─► Manual “reload tileset” action → forced reload
```

### 1. Options initialization

When graphics options are registered, the registry scan runs (`build_tilesets_list`):

- Clears and rebuilds `TILESETS`
- Scans game `gfx/` then user gfx directory
- Populates `TILES` and `OVERMAP_TILES` option choice lists

This can happen before any tile context exists. It does not load sprites.

### 2. Graphics subsystem init (precheck)

After the renderer is created, the main tile context is instantiated and a **precheck**
load runs for the current `TILES` value, then for `OVERMAP_TILES` if it differs.

Precheck purpose: validate that the selected config exists and read tile dimensions (`tile_info`)
early enough to disable tile mode if the selection is broken.

**Precheck behavior** (`precheck = true`):

- Resolve tileset directory from registry (or fall back to default paths if id invalid)
- Open `tile_config.json` (or path from manifest)
- Require `tile_info` array; read width, height, iso, pixelscale, etc.
- Call `allow_omitted_members` on config and **return** — no `load_internal`, no images, no mod tilesets

**Mod list:** Empty during precheck. Mod tilesets are not merged.

**Tileset id on context:** Precheck returns before assigning the loaded tileset's id field.
In BN this ensures the subsequent full load at finalize does not cache-skip off the precheck
pass (the id comparison fails until the first full load completes).

**On failure:** Log error; set `use_tiles = false`. No user popup at this stage. Overmap
precheck failure also sets `use_tiles = false` (same flag, not a separate overmap-only disable).

### 3. Game-data load

While loading core data and mods, JSON objects with `type: mod_tileset` are registered into
`all_mod_tilesets` (compatibility list only — no images yet). This must complete **before**
the full tileset load at finalize.

### 4. Finalize (full load)

After all game data is finalized (terrain, items, monsters, etc.), the last finalize step is
**full tileset load** (`load_tileset()` in BN, called only from finalize).

**Full load behavior** (`precheck = false`):

- Everything in precheck, plus: load images, parse tile definitions, merge compatible mod
  tilesets, post-process (unit 09)
- `mod_list` = active world's mod load order (so cache invalidates when mods change)
- `force = false` (cache may skip reload — see below)

After main context loads, run **tile loading report** (cross-check game entity ids vs loaded
tiles; requires game data to be loaded).

Then load overmap context if id differs; same report for overmap context.

If `tilecontext` is null or `use_tiles` is false, full load returns immediately without work.

### 5. Option changes

When the user saves graphics options, changed keys are compared to previous values. These
changes set `used_tiles_changed = true`:

- `TILES`, `USE_TILES`, `OVERMAP_TILES`
- `STATICZEFFECT`, `MEMORY_MAP_MODE`
- Night-vision color options (affect derived sprite tables)

If `used_tiles_changed`, `refresh_tiles` runs:

1. `reinit` on main context (clear renderer, reset draw scale)
2. Full load main tileset
3. Loading report → debug log
4. Load or repoint overmap context
5. Reset UI zoom / layout

**Mod list:** Active world mods if in-game; empty if at main menu.

**`force` flag:** `true` only when certain options change that affect derived texture
variants without changing tileset id (e.g. `STATICZEFFECT`, `MEMORY_MAP_MODE`, night-vision
colors). Changing `TILES` itself uses `force = false` but a different id bypasses cache anyway.

**On failure:** User popup with error message; set `use_tiles = false` and
`use_tiles_overmap = false`.

### 6. Manual reload

In-game action `reload_tileset` (bindable key):

- `reinit` + full load with `force = true` (always reloads)
- Loading report output routed to caller (popup/log callback)
- Preserves zoom level after reload
- On failure: popup only; does **not** disable `use_tiles`

Useful for tileset authors editing files on disk.

---

## Caching

Skip reload when **all** of the following hold:

1. `force` is false
2. Context already has a loaded tileset
3. Option `FORCE_TILESET_RELOAD` is false
4. Requested `tileset_id` equals currently loaded id
5. `mod_list` equals the stamp stored at last successful load

If any condition fails, perform a full load.

`FORCE_TILESET_RELOAD` (debug option, default false): when true, every load request reloads
even if id and mod list are unchanged. Intended for tileset development.

---

## Configuration keys read by the loader

| Key | Role |
| --- | --- |
| `USE_TILES` | Master enable; cached as `use_tiles` |
| `TILES` | Main tileset id |
| `USE_TILES_OVERMAP` | Enable tiled overmap; cached as `use_tiles_overmap` |
| `OVERMAP_TILES` | Overmap tileset id |
| `FORCE_TILESET_RELOAD` | Disable load cache |

Other graphics options (`STATICZEFFECT`, `MEMORY_MAP_MODE`, night-vision colors) do not
change discovery or parsing but can **force** a reload to rebuild derived sprite tables
(unit 06c).

---

## Error handling summary

| Stage | On load failure |
| --- | --- |
| Precheck (startup) | Log; `use_tiles = false` |
| Finalize (game start) | Exception propagates (not caught in `load_tileset`) |
| Option refresh | Popup; `use_tiles` and `use_tiles_overmap` = false |
| Manual reload | Popup; tiles stay enabled |

Invalid tileset id in options: loader falls back to default config paths (`tile_config.json`
under default data paths), not the named tileset directory.

---

## Relationship to other units

| Topic | Unit |
| --- | --- |
| Scanning gfx dirs, `tileset.txt` NAME/VIEW | [02](./02-discovery-and-registry.md), [04a](./04a-tileset-manifest.md) |
| `tile_config.json` structure | [04b](./04b-tile-config-structure.md) |
| Mod tileset registration and merge | [04f](./04f-mod-tileset-config.md), [05](./05-load-pipeline.md) |
| Image upload, sprite index space | [06a](./06a-atlas-grid.md)–[06c](./06c-filtered-variants.md) |
| Tile def parsing | [07a](./07a-sprite-lists.md)–[07d](./07d-state-modifiers-parsing.md) |
| Loaded data structures | [08](./08-in-memory-model.md) |
| Loading report, prune pass | [09](./09-post-load-validation.md) |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Finalize hook | `src/init.cpp` — `DynamicDataLoader::finalize_loaded_data` |
| Startup precheck + finalize caller | `src/sdltiles.cpp` — `catacurses::init`, `load_tileset` |
| Cache gate + context API | `src/cata_tiles.cpp` — `cata_tiles::load_tileset` |
| Precheck early exit | `src/cata_tiles.cpp` — `tileset_loader::load` |
| Option-driven reload | `src/options.cpp` — `refresh_tiles` |
| Manual reload | `src/game.cpp` — `game::reload_tileset` |
| Context globals | `src/sdltiles.h` — `tilecontext`, `overmap_tilecontext` |

---

## Inputs

- User options: `USE_TILES`, `TILES`, `USE_TILES_OVERMAP`, `OVERMAP_TILES`, `FORCE_TILESET_RELOAD`
- `TILESETS` registry (from discovery)
- `all_mod_tilesets` (from game-data load)
- Active world mod order (for full loads in-game)
- Renderer / graphics backend (for texture upload in full load)

## Outputs

- Main and/or overmap tile contexts with loaded `tileset` (or unchanged on cache hit / early return)
- Updated `tileset_mod_list_stamp` per context on successful load
- Cached flags `use_tiles`, `use_tiles_overmap` may be cleared on certain failures
- Tile loading report lines (full load only, when game data is available)

## Failure modes

| Condition | Behavior |
| --- | --- |
| Tileset id not in registry | Fall back to default json/png paths; may still throw if files missing |
| Missing or invalid `tile_info` | Throw; handled per stage table above |
| `precheck` with valid config | Success with minimal tileset metadata only |
| Load skipped (cache) | No file I/O; previous tileset retained |
| `use_tiles` false at finalize | `load_tileset()` no-op |

## Verification

A correct port should demonstrate:

1. Registry populated before first precheck without loading PNGs
2. Precheck reads dimensions only; no tile ids or textures afterward
3. Full load after mod registration includes mod tileset sprites
4. Same `TILES` and `OVERMAP_TILES` id → one context, two references
5. Different ids → two independent loads
6. Second full load with same id + mod list + `FORCE_TILESET_RELOAD` false → no reload
7. `force = true` or mod list change → reload occurs
8. Option change to `TILES` triggers full reload path; night-vision option triggers reload with `force` semantics
