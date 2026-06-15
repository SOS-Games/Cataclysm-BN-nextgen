# 06a — Atlas grid

Load a sheet PNG and derive the sprite grid: dimensions, cell size, sprite count (`size`), and
how pixel positions map to sprite indices. GPU upload and texture splitting are unit
[06b](./06b-texture-upload.md); color-filtered duplicate tables are unit [06c](./06c-filtered-variants.md).

---

## Purpose

For each `tiles-new` sheet (or legacy manifest image), the loader must:

1. Open the image file
2. Apply optional transparency handling
3. Compute how many sprites fit in the grid
4. Record atlas width for index→grid math during upload
5. Set `size` so the pipeline can advance `offset` after the sheet

---

## When this runs

Called from `load_internal` once per sheet, **before** parsing that sheet’s `tiles[]`
(unit 05). Parameters come from the current sheet entry:

| Parameter | Source |
| --- | --- |
| `img_path` | `tileset_root + "/" + file` (tiles-new) or manifest `img_path` (legacy) |
| `sprite_width`, `sprite_height` | Sheet fields, default `ts.tile_width` / `ts.tile_height` |
| `R`, `G`, `B` | Sheet `transparency` object, or −1 if absent |

---

## Image load

```text
surface = load_image(img_path)    // throws if missing or unreadable
tile_atlas_width = surface.width
```

**Sprites-only port:** missing image → **throw**. Do not implement BN’s empty-ASCII-fallback
skip (missing PNG + empty `tiles` + `ascii` present).

BN uses SDL_image (`IMG_Load`) and converts to a standard 32-bit RGBA format.

---

## Transparency / color key

When sheet `transparency` provides `R`, `G`, `B` each in 0…255:

```text
enable color key on the surface
enable RLE on the surface
```

**BN quirk:** the color key is set to **black (0, 0, 0)** regardless of the `R`/`G`/`B`
values in JSON. Those fields only gate whether color key is enabled. Ports may instead use
the configured RGB as the key for correctness.

When transparency is absent (`R`, `G`, `B` = −1): no color key.

---

## Grid layout

Sprites are a uniform grid covering the image from the top-left origin:

```text
columns = floor(atlas_width  / sprite_width)
rows    = floor(atlas_height / sprite_height)

size = expected_tilecount = columns * rows
```

### Remainder pixels

If `atlas_width % sprite_width ≠ 0` or `atlas_height % sprite_height ≠ 0`, the partial
strip along the right/bottom edge is **ignored** (not counted in `size`).

### Authoring conventions (not enforced)

- Index **0** should be a blank tile (first cell top-left)
- Legacy docs describe **16 sprites per row**; the loader does **not** hard-code 16 — it
  uses `tile_atlas_width / sprite_width` as columns

### Non-square cells

Sheets may use `sprite_width` / `sprite_height` different from `tile_info` (e.g. 20×20
monsters on a 10×10 tileset). Grid math always uses the **current sheet’s** cell size.

---

## Sprite index within a sheet

Linear index for a cell at pixel top-left `(px, py)`:

```text
col = px / sprite_width
row = py / sprite_height

local_index = col + row * (tile_atlas_width / sprite_width)
```

During texture upload (06b), each cell is stored at **global index**:

```text
global_index = offset + local_index
```

Where `offset` is the loader’s running sprite count before this sheet.

`tile_atlas_width` is the **full source image width** (not sub-rectangle width after atlas
splitting). Sub-rect upload passes a pixel `offset` into the atlas when splitting large PNGs
(unit 06b).

---

## Loader state after sheet load

| Field | Value after successful load |
| --- | --- |
| `size` | `expected_tilecount` for this sheet |
| `tile_atlas_width` | Source image width in pixels |
| `offset` (unchanged until end of sheet) | Global index of sprite 0 in this sheet |

After tile defs and optional metadata for the sheet:

```text
offset += size
```

(unit 05).

---

## Relationship to JSON sprite numbers

Tile entries reference sprites by integer index (unit 04c):

- **Base composed tilesets:** indices in JSON are often already **global** across all sheets
- **Mod tilesets:** indices are **sheet-local**; `sprite_id_offset` adds the pre-mod `offset`
  when parsing (not in this unit)

Grid load does not rewrite JSON — it only ensures texture slots `0 … offset+size-1` exist
after upload.

---

## `DYNAMIC_ATLAS` note

When compiled with `DYNAMIC_ATLAS`, vector pre-extension for eight texture tables is skipped;
sprites batch into a dynamic atlas instead (appendix A1). Grid math and `size` are unchanged.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Image load | `src/sdl_wrappers.cpp` — `load_image` |
| Grid + `size` | `src/cata_tiles.cpp` — `tileset_loader::load_tileset` |
| Index from position | `src/cata_tiles.cpp` — `copy_surface_to_texture` |
| Sheet params | `src/cata_tiles.cpp` — `load_internal` |

---

## Inputs

- `img_path` — absolute or rooted path to PNG
- `sprite_width`, `sprite_height` — grid cell size in pixels
- Optional `transparency` RGB (enable color key in BN)
- Current `offset` — global base for this sheet’s local indices

## Outputs

- Decoded raster surface (in memory until uploaded in 06b)
- `size` — sprite count for this sheet
- `tile_atlas_width` — source image width
- Extended texture table slots (06b) or atlas batch entries (A1)

## Failure modes

| Condition | Behavior |
| --- | --- |
| File missing / corrupt | Throw |
| `sprite_width` or `sprite_height` is 0 | Undefined / assert (avoid in port) |
| Atlas smaller than one cell | `size` = 0 |
| Max texture smaller than cell (06b) | Throw during upload |
| Transparency block with out-of-range channel | Color key not applied |

## Verification

A correct port should demonstrate:

1. 320×240 atlas, 32×32 cells → `size` = 10 × 7 = 70
2. 100×100 atlas, 32×32 cells → `size` = 3 × 3 = 9 (remainder discarded)
3. 20×20 cells on 200×40 image → `size` = 10 × 2 = 20
4. After sheet load, `offset` increases by `size`
5. Local index 0 maps to global `offset` before `offset += size`
6. Missing PNG throws (sprites-only port)
