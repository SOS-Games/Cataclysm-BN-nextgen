# 06c — Filtered variants

At load time, BN precomputes **eight parallel texture tables** from each sheet: the same
sprite indices, different per-pixel color transforms. Draw code picks a table via
`tileset_fx_type` (unit 08). Upload mechanics are in [06b](./06b-texture-upload.md).

---

## Purpose

Avoid applying expensive per-pixel filters every frame. Each effect is baked into GPU
textures during load:

```text
same sprite_index → eight texture slots (one per effect family)
```

---

## The eight tables

| # | Tileset field | Filter key | `tileset_fx_type` at draw |
| --- | --- | --- | --- |
| 1 | `tile_values` | `color_pixel_none` (no filter) | `none` (default) |
| 2 | `shadow_tile_values` | `color_pixel_grayscale` | `shadow` |
| 3 | `night_tile_values` | `color_pixel_nightvision` | `night` |
| 4 | `overexposed_tile_values` | `color_pixel_overexposed` | `overexposed` |
| 5 | `underwater_tile_values` | `color_pixel_underwater` | `underwater` |
| 6 | `underwater_dark_tile_values` | `color_pixel_underwater_dark` | `underwater_dark` |
| 7 | `z_overlay_values` | `color_pixel_zoverlay` | `z_overlay` |
| 8 | `memory_tile_values` | `MEMORY_MAP_MODE` option | `memory` |

All eight are extended by `expected_tilecount` and filled in the same upload chunk loop
(06b).

### Memory table filter (runtime option)

`memory_tile_values` uses the filter named by option `MEMORY_MAP_MODE`:

| Option value | Filter |
| --- | --- |
| `color_pixel_sepia` | Sepia gradient (default) |
| `color_pixel_darken` | ~33% brightness |

Resolved at **load time** from current options. Changing `MEMORY_MAP_MODE` forces a
tileset reload (unit 01).

---

## Load-time bake algorithm

For each atlas upload chunk (06b):

```text
for (table, filter_name) in eight_entries:
    fn = lookup_filter(filter_name)    // nullptr for color_pixel_none

    if fn is null:
        upload unfiltered surface → table
    else:
        filtered = copy_surface(surface)
        for each pixel with alpha > 0:
            pixel = fn(pixel)
        upload filtered → table
```

BN: `apply_color_filter_blit_copy` copies the surface then maps each opaque pixel through
`fn`. Transparent pixels (`alpha == 0`) are left unchanged.

All eight tables receive **identical** `source_rect` layout per `global_index` — only pixel
colors differ.

---

## Filter functions (per-pixel semantics)

Each filter: `RGBA in → RGBA out`. Registered by string name in `builtin_color_pixel_functions`.

| Name | Summary |
| --- | --- |
| `color_pixel_none` | No filter (`nullptr` → raw upload) |
| `color_pixel_copy` | Identity |
| `color_pixel_grayscale` | Luminance → gray RGB; preserves alpha; black unchanged |
| `color_pixel_nightvision` | Green-tinted night vision curve |
| `color_pixel_overexposed` | Brightened / washed-out curve |
| `color_pixel_underwater` | Blue-green shift (bright) |
| `color_pixel_underwater_dark` | Dimmer underwater (~half levels) |
| `color_pixel_darken` | ~33% of each channel |
| `color_pixel_sepia` | Sepia gradient from grayscale |
| `color_pixel_zoverlay` | Cyan overlay tint for lower z-levels |

### `color_pixel_zoverlay` and `STATICZEFFECT`

```text
if STATICZEFFECT option:
    mix source color with cyan (128,255,255) weighted by alpha/8
else:
    replace with fixed cyan (128,255,255) keeping alpha
```

`STATICZEFFECT` is read when the filter runs at load time. Toggling it forces tileset
reload.

---

## Draw-time lookup (reference)

Non-`DYNAMIC_ATLAS` path — `get_or_default(sprite_index, type)`:

```text
switch type:
    shadow          → shadow_tile_values[sprite_index]
    night           → night_tile_values[sprite_index]
    overexposed     → overexposed_tile_values[sprite_index]
    underwater      → underwater_tile_values[sprite_index]
    underwater_dark → underwater_dark_tile_values[sprite_index]
    memory          → memory_tile_values[sprite_index]
    z_overlay       → z_overlay_values[sprite_index]
    default         → tile_values[sprite_index]
```

`enhanced_night` / `enhanced_overexposed` blend extra tints at draw time in the
`DYNAMIC_ATLAS` path only; the prebaked tables use `night` and `overexposed` entries.

---

## Options that invalidate baked tables

Reload required when these change (filters or table choice change):

| Option | Affects |
| --- | --- |
| `MEMORY_MAP_MODE` | Memory table filter |
| `STATICZEFFECT` | Z-overlay filter output |
| `NIGHT_VISION_COLOR` / `NIGHT_VISION_DEFAULT_COLOR` | Draw-time enhanced night (atlas path) |
| `ENHANCED_NIGHT_VISION_*` | Draw-time enhanced night |

First two affect **load-time** bake in the standard eight-table path.

---

## Port guidance

| Approach | When |
| --- | --- |
| **Full BN parity** | Eight tables + filters at load |
| **Minimal** | Single `tile_values`; apply grayscale/night/etc. in shader or at draw |
| **Hybrid** | Bake only `shadow` + `night`; rest at draw |

If using one table, map all `tileset_fx_type` to the same table or implement filters in the
renderer.

Filter math can be ported from `src/sdl_utils.cpp` or approximated in GPU shaders.

---

## `DYNAMIC_ATLAS` note

Prebaked eight-table path is **not** used. Effects apply at draw time via
`get_pixel_function(type)` + `apply_color_filter` on staging surfaces. Load still uploads
base sprites into the dynamic atlas. See appendix A1.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Eight-table loop | `src/cata_tiles.cpp` — `create_textures_from_tile_atlas` |
| Filter registry | `src/sdl_utils.cpp` — `builtin_color_pixel_functions` |
| Filter implementations | `src/sdl_utils.cpp` — `color_pixel_*` |
| Draw lookup | `src/cata_tiles.cpp` — `tileset::get_or_default` |
| FX enum | `src/cata_tiles.h` — `tileset_fx_type` |
| Memory option | `src/options.cpp` — `MEMORY_MAP_MODE` |

---

## Inputs

- Decoded sheet surface (same chunk as 06b)
- `expected_tilecount`, `offset`, pixel offsets
- Current options: `MEMORY_MAP_MODE`, `STATICZEFFECT` (for z-overlay bake)

## Outputs

- Eight parallel `vector<texture>` tables with aligned indices
- Each index: same rect layout across tables, different pixel content

## Failure modes

| Condition | Behavior |
| --- | --- |
| Unknown filter name | `debugmsg`; nullptr → unfiltered upload for that table |
| Filter pass fails | Same as 06b upload failure |
| Option changes without reload | Stale memory/z-overlay bake until reload |

## Verification

A correct port should demonstrate:

1. Eight tables same length after each sheet
2. `tile_values[i]` and `night_tile_values[i]` same dimensions, different pixels
3. Transparent pixels unchanged in filtered copies
4. `get_or_default(i, night)` reads `night_tile_values[i]`
5. Changing `MEMORY_MAP_MODE` without reload leaves old memory table (BN); with reload, rebakes
6. Minimal port: single table + `fx_type` ignored still renders base sprites
