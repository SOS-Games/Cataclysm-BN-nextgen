# 04b — Tile config structure

Top-level schema of `tile_config.json` (and equivalent mod tileset JSON objects): required
metadata, sheet layout (`tiles-new` vs legacy `tiles`), and optional global sections. Per-tile
grammar is in [04c](./04c-tile-entries.md); nested `ascii` and `state-modifiers` in [04d](./04d-ascii-config.md)
and [04e](./04e-state-modifiers-config.md).

---

## Purpose

After [04a](./04a-tileset-manifest.md) resolves `json_path`, the loader opens a single JSON
**object** and:

1. Reads **`tile_info`** (required) — global tile dimensions and flags
2. Loads sprite sheets and tile definitions via **`tiles-new`** or legacy **`tiles`**
3. Optionally applies top-level **`overlay_ordering`**, **`tints`**, **`tint_pairs`**

---

## Document shape

```text
{
  "tile_info": [ { ... } ],           // required

  "tiles-new": [ { ... }, ... ],      // modern (preferred if present)
  // OR
  "tiles": [ { ... } ],               // legacy (single manifest image)

  "overlay_ordering": [ ... ],        // optional
  "tints": [ ... ],                   // optional
  "tint_pairs": [ ... ]               // optional
}
```

The file is one JSON object, not an array (mod JSON files may wrap multiple objects in an
array at the file level — see [04f](./04f-mod-tileset-config.md)).

---

## `tile_info` (required)

Array of one or more objects. BN reads **every** entry in order; later entries overwrite
earlier fields on the in-memory tileset.

| Field | Type | Default | Stored | Notes |
| --- | --- | --- | --- | --- |
| `width` | int | — | yes | Tile width in pixels |
| `height` | int | — | yes | Tile height in pixels |
| `iso` | bool | `false` | yes (global) | Isometric layout flag |
| `pixelscale` | float | `1.0` | yes | Display scale multiplier |
| `zlevel_height` | int | `0` | **no** | Read then forced to `0` in BN |
| `retract_dist_min` | float | `-1` | **no** | Read then forced to `-1` |
| `retract_dist_max` | float | `0` | **no** | Read then forced to `0` |

Missing `tile_info` → **error** (`"tile_info" missing`).

During **precheck** (unit 01), parsing stops after `tile_info`; remaining keys are ignored.

---

## Sheet loading modes

The loader chooses **one** branch in `load_internal`:

```text
if config has array "tiles-new":
    use modern multi-sheet path
else if config has array "tiles":
    use legacy single-sheet path
else:
    skip sheet load (tints-only / metadata-only config)
```

If both arrays exist, **`tiles-new` wins**; `tiles` is ignored.

### Global sprite index space

Across all sheets in one load pass, sprite indices in tile definitions are **local** to the
current sheet (0 … N−1). The loader maintains a running **`offset`**: each sheet’s local
indices are shifted by `offset` when stored. After each sheet:

```text
offset += size   // size = sprite count from the sheet just loaded
```

Legacy mode loads one image then sets `offset += size` once.

---

## `tiles-new` — per-sheet entry

Each array element describes one PNG (or other image) plus tile definitions tied to that
sheet.

### Required / common fields

| Field | Type | Default | Purpose |
| --- | --- | --- | --- |
| `file` | string | — | Image path relative to **tileset root** |
| `tiles` | array | — | Tile definitions (unit 04c) |

### Optional sheet parameters

| Field | Type | Default | Purpose |
| --- | --- | --- | --- |
| `sprite_width` | int | `tile_info.width` | Grid cell width for this sheet |
| `sprite_height` | int | `tile_info.height` | Grid cell height |
| `sprite_offset_x` | int | `0` | Draw offset applied to tiles from this sheet |
| `sprite_offset_y` | int | `0` | |
| `sprite_offset_x_retracted` | int | `sprite_offset_x` | Offset when retracted/occlusion |
| `sprite_offset_y_retracted` | int | `sprite_offset_y` | |
| `pixelscale` | float | `1.0` | Per-sheet scale (overrides default for defs on this sheet) |
| `transparency` | object | none | Color key `{ "R", "G", "B" }` for this sheet’s image |
| `ascii` | array | — | *(optional — skipped in sprites-only port)* |
| `state-modifiers` | array | — | UV state groups (unit 04e) |
| `global-warp-whitelist` | string[] | — | Replaces tileset global warp whitelist |
| `global-warp-blacklist` | string[] | — | Replaces tileset global warp blacklist |

### Per-sheet load sequence

For each `tiles-new` entry, in order:

```text
1. Resolve image path = tileset_root + "/" + file
2. Read transparency, sprite dimensions, offsets, pixelscale
3. If image missing AND tiles is empty AND ascii present:
       log warning, skip sheet (ASCII-only fallback placeholder)
4. Load image → compute size (unit 06a)
5. Parse tiles[] from this entry object (unit 07b)
6. If `ascii` present → parse (unit 07c) — **optional; skip for sprites-only ports**
7. If state-modifiers present → parse (unit 07d)
8. If global-warp-* present → replace tileset lists
9. offset += size
```

`transparency` sets RGB color key for the upcoming image load. If absent, `R/G/B = -1`
(disabled).

---

## Legacy `tiles` mode

When only top-level `tiles` exists:

| Source | Path |
| --- | --- |
| Image | `img_path` from manifest (`tileset_root` + manifest `TILESET`) |
| Definitions | `config.tiles` array on the **root** object |

Sheet parameters use globals: `sprite_width/height = tile_info`, offsets zero, no
per-sheet transparency object (R/G/B = −1).

```text
1. Load img_path
2. Parse config.tiles (not nested under tiles-new)
3. offset += size
```

This matches older tilesets with one `tiles.png` and one flat `tiles` list.

---

## Tints-only configs

If neither `tiles-new` nor `tiles` is present, no images load in this pass. Used by some
`mod_tileset` files that only add `tints` / `tint_pairs` / `overlay_ordering`. Top-level
optional sections still run.

---

## `overlay_ordering` (optional)

Array of objects controlling draw order for mutation/bionic overlays (0–9999, higher = on top).

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | string or string[] | Mutation or bionic id(s) |
| `order` | int | Layer priority |

Each id in `id` (tag expansion supported) maps to `order` in the tileset-specific ordering
table. Tileset entries **override** base game ordering from `mutation_ordering.json` when
both define the same id.

---

## `tints` (optional)

Array of color recipes keyed by id (mutation id, tint-pair source, etc.).

| Field | Type | Purpose |
| --- | --- | --- |
| `id` | string | Lookup key; empty id skipped |
| `fg` | string or object | Foreground tint |
| `bg` | string or object | Background tint |
| `blend_mode` | string | Applied when `fg`/`bg` are simple strings |
| `contrast` | float | Top-level modifier for simple string mode |
| `saturation` | float | |
| `brightness` | float | |

**Simple string** (`"fg": "#RRGGBB"` or `"c_white"`): optional top-level `contrast` /
`saturation` / `brightness` / `blend_mode` apply.

**Object form** (`"fg": { "color", "blend_mode", "contrast", ... }`): only when no top-level
`contrast` or `saturation` on the same entry.

Hex colors: `#RRGGBB` or `#RRGGBBMM` where the 4th byte is **brightness** (not alpha):
`brightness = MM / 128`.

Stored in `tileset.tints` when `fg` or `bg` resolves to a value.

---

## `tint_pairs` (optional)

Array mapping overlay “source” tint types to “target” types (e.g. hair color → hair style).

| Field | Type | Default | Purpose |
| --- | --- | --- | --- |
| `source_type` | string | — | Controller type |
| `target_type` | string | — | Overlay type to tint |
| `override` | bool | `false` | Bypass legacy tile tint spec |

Empty `source_type` or `target_type` → entry skipped. Stored as
`target_type → (source_type, override)`.

---

## Relationship to other units

| Topic | Unit |
| --- | --- |
| Manifest paths | [04a](./04a-tileset-manifest.md) |
| `tiles[]` entry objects | [04c](./04c-tile-entries.md) |
| `ascii` array | [04d](./04d-ascii-config.md) *(skipped — sprites-only)* |
| `state-modifiers` | [04e](./04e-state-modifiers-config.md) |
| Orchestration | [05](./05-load-pipeline.md) |
| Image grid / `size` | [06a](./06a-atlas-grid.md) |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Open config + `tile_info` | `src/cata_tiles.cpp` — `tileset_loader::load` |
| `load_internal` branches | `src/cata_tiles.cpp` — `tileset_loader::load_internal` |
| `overlay_ordering` | `src/overlay_ordering.cpp` |
| `tints` / `tint_pairs` | `src/cata_tiles.cpp` — end of `load_internal` |
| Author examples | `docs/en/mod/json/reference/graphics/tileset.md` |

---

## Inputs

- Parsed JSON root object from `json_path`
- `tileset_root` directory for relative `file` paths
- `img_path` from manifest (legacy mode only)
- `precheck` flag (stops after `tile_info` in `load`, not in `load_internal`)

## Outputs

- Populated tileset dimensions and flags from `tile_info`
- Loaded sprite sheets and incremented global `offset` (full load only)
- Optional: mutation overlay order map, tints, tint pairs, global warp lists

## Failure modes

| Condition | Behavior |
| --- | --- |
| JSON not an object | Parse error |
| Missing `tile_info` | Throw |
| `tiles-new` entry, missing image | Throw on load (unless empty ASCII fallback skip) |
| `tiles-new` entry, missing `file` | Throw when reading `file` |
| Legacy mode, bad `img_path` | Throw on image load |
| Invalid color in `tints` | Entry skipped or partial (no tint stored) |
| Both `tiles-new` and `tiles` | Only `tiles-new` used |

## Verification

A correct port should demonstrate:

1. Reject config with no `tile_info`
2. `tiles-new` with two `file` entries produces contiguous global sprite indices (second
   sheet indices offset by first sheet’s `size`)
3. Legacy config uses manifest image, not `tiles-new[].file`
4. Config with only `tints` loads no images but stores tint data
5. `tile_info.iso` and `pixelscale` available before sheet loop
6. BN ignores `zlevel_height` and `retract_dist_*` after read (store 0 / -1 / 0 or document port choice)
7. `global-warp-whitelist` on a sheet replaces the previous list, not merges
