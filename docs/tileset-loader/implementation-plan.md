# Implementation plan — external tileset loader

Agent-oriented guide for implementing a **tileset loader in a new repository** (any language)
that reads **existing Cataclysm-BN tileset packs** from disk. Behavioral specs live in the
unit docs indexed by [README](./README.md); this file is the **project plan and module map**.

---

## Target repository

**Implementation code lives in this repo** (Cataclysm-BN-nextgen).

| Item | Path |
| --- | --- |
| Java package | `core/src/main/java/io/gdx/cdda/bn/nextgen/tileset/` |
| Agent guide | [`AGENTS.md`](../../AGENTS.md) |
| Implementation checklist | [`docs/TILESET_LOADER.md`](../TILESET_LOADER.md) |
| Sprite viewer | [`docs/SPRITE_VIEWER.md`](../SPRITE_VIEWER.md) |
| Unit specs | This directory (`docs/tileset-loader/`) |

Java package root: `io.gdx.cdda.bn.nextgen.tileset`

## Reference implementation (LibGDX patterns)

**Not** tileset code — use for Gradle layout, path resolution, JSON I/O:

| Repo | Path |
| --- | --- |
| **cygnus-engine** | `../../../../Documents/cygnus-engine` (from this file; under user `Documents/`) |

Key files: `ModPaths.java`, `ModJson.java`, `*DataIO.java`, [`AGENTS.md`](../../../../Documents/cygnus-engine/AGENTS.md).

BN C++ sources cited in unit docs remain in the sibling [Cataclysm-BN](../../../Cataclysm-BN) repo.

---

## Goal

Build a library that:

1. Discovers tilesets under `gfx/`
2. Loads a selected pack (`tileset.txt` → `tile_config.json` → PNG sheets)
3. Optionally merges compatible `mod_tileset` JSON from mods
4. Produces an in-memory **`Tileset`** your renderer queries (id → sprites → textures)

You are **not** reimplementing BN’s draw loop. You are reimplementing the loader described in
units 01–09 (and optionally A1).

---

## On-disk inputs (point your repo at BN installs)

```text
gfx/
  <tileset_id>/
    tileset.txt          # NAME, VIEW, JSON, TILESET
    tile_config.json     # tile_info, tiles-new[], tints, overlay_ordering, …
    *.png                # images referenced by tiles-new[].file

data/mods/.../           # optional — mod_tileset JSON + companion PNGs
```

| File | Spec unit |
| --- | --- |
| `tileset.txt` | [04a](./04a-tileset-manifest.md) |
| `tile_config.json` | [04b](./04b-tile-config-structure.md) |
| `tiles[]` entry schema | [04c](./04c-tile-entries.md) |
| `mod_tileset` | [04f](./04f-mod-tileset-config.md) |

**Not required at runtime:** `scripts/tileset.ts` (compose tool). Published packs already use
composed global sprite indices in `tile_config.json`.

### Configurable roots

Expose paths in your loader config:

| Root | BN equivalent | Purpose |
| --- | --- | --- |
| Game gfx | `PATH_INFO::gfxdir()` → `gfx/` | Primary tileset packs |
| User gfx | `PATH_INFO::user_gfx()` | User-installed packs |
| Mod data dirs | mod load paths | `mod_tileset` registration + merge |

---

## Deliverable: in-memory model

After `load_tileset(id)` succeeds, hold a **`Tileset`** object ([08](./08-in-memory-model.md)):

```text
Tileset {
  id: string
  tile_width, tile_height: int
  tile_pixelscale: float

  // Global sprite index space: 0 … N-1
  textures: TextureSlot[N]              // minimal v1
  // OR eight parallel tables           // BN parity (06c)

  tile_ids: Map<string, TileType>
  tile_ids_by_season: [4] Map<string, SeasonEntry>   // recommended

  state_modifiers: StateModifierGroup[]   // optional (07d)
  tints: Map<string, TintPair>            // optional
  global_warp_whitelist, global_warp_blacklist: string[]
}

TileType {
  sprite: { fg, bg: WeightedList<SpriteFrame[]> }
  masks:  { tint: { fg, bg } }            // -1 = no mask
  multitile, rotates, animated, flags, height_3d
  offset, offset_retracted, pixelscale
  available_subtiles: string[]            // multitile parents only
  default_tint: optional<Color>
}

SpriteFrame[] length ∈ {1, 2, 4}         // rotation frames (07a)
WeightedList: variants with weight + precalc table (09)
```

### Required lookup API (consumer contract)

```text
list_tilesets(roots) → [tileset_id]

load_tileset(id, options) → Tileset
  options: { gfx_roots, mod_tilesets[], force_reload? }

find_tile(id: string) → TileType?
find_tile_by_season(base_id: string, season: Season) → { id, tile }?

get_texture(sprite_index: int, fx: FxType) → TextureView
  TextureView: { handle, src_rect, optional warp_offset }
```

Draw-time lighting, entities, and `looks_like` chains are **out of scope** ([README](./README.md)).

---

## Implementation modules (suggested repo layout)

Map each module to spec units. Names are illustrative; use your language’s conventions.

| Module | Responsibility | Spec units |
| --- | --- | --- |
| `discover` | Scan `gfx/*/tileset.txt`, build id → path registry | [01](./01-overview-and-lifecycle.md), [02](./02-discovery-and-registry.md) |
| `manifest` | Resolve JSON/TILESET paths from manifest | [04a](./04a-tileset-manifest.md) |
| `config` | Parse `tile_info`, `tiles-new`, legacy `tiles`, tints | [04b](./04b-tile-config-structure.md) |
| `mod_tileset` | Register + merge mod packs | [04f](./04f-mod-tileset-config.md) |
| `load` | Orchestrate full pipeline | [05](./05-load-pipeline.md) |
| `atlas` | Grid math, PNG decode, texture upload | [06a](./06a-atlas-grid.md), [06b](./06b-texture-upload.md) |
| `filters` | Eight-table color bake OR stub | [06c](./06c-filtered-variants.md) |
| `parse_sprites` | `fg`/`bg` JSON → weighted lists | [07a](./07a-sprite-lists.md) |
| `parse_tiles` | `tiles[]` → `tile_ids`, multitile, seasonal index | [07b](./07b-tile-registration.md) |
| `parse_state_mods` | `state-modifiers` (optional) | [04e](./04e-state-modifiers-config.md), [07d](./07d-state-modifiers-parsing.md) |
| `validate` | Post-load prune + fallbacks | [09](./09-post-load-validation.md) |
| `model` | Public types + lookup methods | [08](./08-in-memory-model.md) |

**Skipped for sprites-only v1:** ASCII / `fallback.png` (units **04d**, **07c** — cancelled; see [README](./README.md)).

---

## Loader mutable state (carry across sheets)

From [05](./05-load-pipeline.md) — implement as a struct on the loader, not on `Tileset`:

| Field | Meaning |
| --- | --- |
| `offset` | Sprites loaded so far; final value = `N` |
| `size` | Sprites in current sheet |
| `sprite_id_offset` | Added to JSON indices (`0` base; global count before each mod) |
| `sprite_width`, `sprite_height` | Grid cell size for current sheet |
| `sprite_offset`, `sprite_offset_retracted` | Per-sheet draw offsets |
| `sprite_pixelscale` | Per-sheet scale |
| `R`, `G`, `B` | Transparency color key (−1 = off) |

---

## Per-sheet load order (must match)

```text
for each tiles-new entry (or legacy single sheet):
  1. load PNG
  2. upload grid cells → global indices [offset, offset+size)
  3. load tiles[] from same entry (07a, 07b)
  4. optional: state-modifiers (07d)
  5. optional: global-warp-whitelist/blacklist
  6. offset += size

after all base sheets:
  for each compatible mod_tileset:
    sprite_id_offset = offset
    repeat load_internal on mod JSON

post-load (09):
  clean sprite indices, prune empty tiles, warn unknown, synthesize highlight_item
```

---

## Version tiers

### v1 — Minimal sprites-only (recommended first milestone)

| Include | Skip |
| --- | --- |
| Discover + manifest + `tiles-new` | ASCII (units 04d, 07c) |
| PNG grid + **one** texture table | Eight-table bake (06c) — use single table + draw-time FX later |
| 07a, 07b, seasonal cache | State modifiers (07d) |
| Mod merge (04f) if mods needed | `DYNAMIC_ATLAS` (A1) |
| Post-load validate (09) | `do_tile_loading_report` (optional QA tool) |

Apply visual effects in your renderer (shader or CPU) instead of prebaking six extra tables.

### v2 — BN visual parity (standard build)

- Eight parallel texture tables ([06c](./06c-filtered-variants.md))
- `MEMORY_MAP_MODE` / `STATICZEFFECT` option-driven filter selection at load
- Full tint / `overlay_ordering` if character rendering needs them

### v3 — Character UV warps

- [07d](./07d-state-modifiers-parsing.md) + [A1](./appendix-dynamic-atlas.md)
- Requires GPU readback or dynamic compositing; significantly more complex

---

## External dependencies

| Capability | Used for |
| --- | --- |
| JSON parser | All config |
| PNG decoder | Sheet images |
| GPU texture API (or CPU bitmaps) | 06b upload |
| Filter functions | 06c — port from `src/sdl_utils.cpp` or approximate in shaders |

---

## Explicitly out of scope

Do **not** implement in the loader library:

- BN `cata_tiles` draw path, field lighting, animation layers
- Runtime `looks_like` resolution (except optional coverage report in 09)
- `external_tileset` game-data JSON
- Option UI / BN `options.cpp` wiring (only read equivalent config values if needed)
- Compose pipeline (`scripts/tileset.ts`)

---

## Acceptance tests

Run against a real pack (e.g. `gfx/UndeadPeopleTileset` or `gfx/MshockXottoplus`).

| # | Check | Spec reference |
| --- | --- | --- |
| 1 | `list_tilesets` returns id matching `NAME` in `tileset.txt` | 02 |
| 2 | `load_tileset(id)` succeeds without error | 05 |
| 3 | `N = sum(sheet tile counts)` equals texture table length | 06a, 08 |
| 4 | `find_tile("t_dirt")` has fg indices all in `[0, N)` | 09 |
| 5 | `find_tile("unknown")` exists or warning logged | 09 |
| 6 | `find_tile("highlight_item")` exists after load | 09 |
| 7 | Multitile: `find_tile("t_wall_corner")` exists when parent is multitile | 07b |
| 8 | Seasonal: `t_tree` + `t_tree_season_winter` resolve per season | 07b, 08 |
| 9 | Mod: local `fg: 0` maps to `sprite_id_offset` at merge time | 04f, 07a |
| 10 | Reload same id → stable `N` and tile count | 01 |

Each unit doc ends with additional **Verification** bullets — use them as unit tests during
implementation.

---

## Agent workflow

When implementing in a new repo:

1. **Read** [README](./README.md) scope and this plan.
2. **Implement in unit dependency order** (01 → 02 → 04a → … → 09). Do not skip 05
   orchestration — it defines state threading.
3. **For each unit:** implement → run that unit’s Verification section → proceed.
4. **Point** `gfx_roots` at a BN install or copy of `gfx/<pack>/`.
5. **Defer** A1 unless character UV warps are required.
6. **Cross-check** ambiguous behavior against BN source files listed in each unit’s
   “BN source reference” table.

### Suggested first PR slices

| PR | Units | Done when |
| --- | --- | --- |
| 1 | 02, 04a | Can list tilesets and open `tile_config.json` |
| 2 | 06a, 06b, 05 (sheet loop only) | PNG → texture table + global indices |
| 3 | 07a, 07b, 09 | `tile_ids` populated and validated |
| 4 | 04f | Mod tileset merge |
| 5 | 06c or renderer FX | Night/shadow/etc. visually correct |
| 6 | 07d, A1 | Optional — crouch/run UV warps |

---

## Public API sketch (language-agnostic)

```text
// Discovery
function list_tilesets(gfx_roots: GfxRoots): string[]

// Load
type LoadOptions = {
  gfx_roots: GfxRoots
  mod_tilesets: ModTilesetRef[]   // from prior registration pass
  force_reload?: bool
}

function load_tileset(tileset_id: string, opts: LoadOptions): Tileset

// Query (on Tileset)
method find_tile(id: string): TileType | null
method find_tile_by_season(base_id: string, season: Season): { id: string, tile: TileType } | null
method get_texture(sprite_index: int, fx: FxType = FxType.None): TextureView | null
method sprite_count(): int   // N
```

---

## Related documents

| Document | Role |
| --- | --- |
| [README](./README.md) | Unit index, scope, progress |
| [01](./01-overview-and-lifecycle.md) | When to load, cache, dual contexts |
| [08](./08-in-memory-model.md) | Final data shapes |
| [09](./09-post-load-validation.md) | Mandatory cleanup before publish |
| [appendix-dynamic-atlas.md](./appendix-dynamic-atlas.md) | Only if matching BN desktop atlas path |

BN author docs (secondary): `docs/en/mod/json/reference/graphics/tileset.md`,
`mod_tileset.md`.
