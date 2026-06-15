# A1 — Dynamic atlas (appendix)

Compile-time alternate to the eight prebaked texture tables ([06c](./06c-filtered-variants.md)).
Enabled with CMake `DYNAMIC_ATLAS=ON` (default when `TILES=ON` in BN). Standard model:
[08](./08-in-memory-model.md).

---

## Purpose

**Standard path:** At load, bake eight filtered copies of every sprite into parallel
`vector<texture>` tables.

**Dynamic atlas path:**

1. Pack **base** sprites into a growable GPU atlas at load time
2. Store base lookups in `tile_lookup`
3. **Lazily** composite effects (night, tint, mask, UV warp) on first draw request
4. Cache composites back into `tile_lookup` and the atlas

Trade-off: lower load memory and faster load; higher first-draw cost and more runtime CPU/GPU
work. Required for BN state-modifier UV warping (needs CPU readback of sprite pixels).

---

## Build flag

| Setting | Effect |
| --- | --- |
| `TILES=OFF` | No tile build; `DYNAMIC_ATLAS` unavailable |
| `TILES=ON`, `DYNAMIC_ATLAS=ON` | Default BN tiles build |
| `TILES=ON`, `DYNAMIC_ATLAS=OFF` | Standard eight-table path ([06b](./06b-texture-upload.md), [06c](./06c-filtered-variants.md)) |

```text
target_compile_definitions(cataclysm-bn-tiles-common PUBLIC DYNAMIC_ATLAS)
```

Android JNI build forces `DYNAMIC_ATLAS=1`.

---

## Architecture overview

```text
tileset
  ├─ tileset_atlas: dynamic_atlas          // packed GPU sheets + CPU surfaces (batch mode)
  ├─ tile_lookup: map<tileset_lookup_key, tile_lookup_entry>
  │     └─ { texture, warp_offset }
  └─ warp_cache: map<hash, { surface, offset, offset_mode }>   // per-frame UV composites

tile_ids / sprite indices     // unchanged — still global int indices 0…N-1
```

There are **no** `tile_values` / `night_tile_values` / … vectors in this build.

---

## `dynamic_atlas`

Implementation: `src/dynamic_atlas.h`, `src/dynamic_atlas.cpp`.

### Construction at load start

```text
tileset_atlas = dynamic_atlas(4096, 4096, tile_width, tile_height)
tileset_atlas->start_batch()
```

`4096×4096` max sheet size; `tile_width` / `tile_height` hint stripe packing granularity.

### Sprite sheets

```text
sprite_sheet {
    texture: GPU atlas page
    surface: CPU copy (batch mode — written during load/draw composite)
    packer: stripe_texture_packer | null_texture_packer (software renderer)
    dirty: needs GPU upload
}
```

New GPU pages are allocated when the packer cannot fit `(w,h)`. Software renderer uses one
sprite per texture (`null_texture_packer`).

### Key APIs

| API | Role |
| --- | --- |
| `get_or_create_sprite(w, h, id?, callback)` | Pack rect; blit via callback; optional content `id` for dedup |
| `create_sprite` | Always allocates (or warns on duplicate `id`) |
| `find_sprite(id)` | Lookup prior placement by hash id |
| `get_staging_area(w, h)` | Reusable scratch surface for sheet slicing |
| `start_batch` / `end_batch` | Load uses batch: CPU surfaces during load, `SDL_UpdateTexture` at `end_batch` |
| `readback_load` | GPU → CPU readback of dirty sheets (for pixel access / UV warp) |
| `readback_find(texture)` | Map `texture` → `(surface, rect)` |

---

## Load-time upload (`copy_surface_to_dynamic_atlas`)

Replaces `create_textures_from_tile_atlas` + eight-table bake ([06b](./06b-texture-upload.md)).

Per grid cell in the sheet (same index math as standard path):

```text
index = offset + grid_position_to_linear_index(...)

blit cell → staging surface
content_hash = hash(pixels)

atlas_tex = atlas.get_or_create_sprite(cell_w, cell_h, content_hash, blit_callback)

tile_lookup[ { index, NO_MASK, none, NO_COLOR, NO_WARP, (0,0) } ] =
    { texture(atlas_tex), warp_offset: (0,0) }
```

| Topic | Behavior |
| --- | --- |
| Dedup | Identical pixel content shares atlas placement via `content_hash` as atlas sprite id |
| Global index | `index` in lookup key matches tile JSON sprite indices |
| Hash collision | `emplace` fails → error log; possible visual glitch |
| Filtered tables | **Not** created — effects deferred to `get_or_default` |

`load_tileset` does **not** extend eight `vector<texture>` tables (`#if !defined(DYNAMIC_ATLAS)`).

### Load finalize ([09](./09-post-load-validation.md))

```text
end_batch()        // upload dirty atlas sheets to GPU
readback_load()    // CPU surfaces available for warp pipeline
```

---

## `tileset_lookup_key`

Cache key for composed textures:

```text
{
    sprite_index: int
    mask_index: int              // TILESET_NO_MASK (-1) if none
    effect: tileset_fx_type
    tint: tint_config             // color + blend + contrast/saturation/brightness
    warp_hash: size_t             // TILESET_NO_WARP (0) if none
    sprite_offset: point          // UV warp alignment from tile_type
}
```

Same sprite with different night/tint/warp → distinct cache entries.

---

## `get_or_default` (lazy composite)

**Cache hit:** return cached `texture` + `warp_offset`.

**Cache miss:** pipeline (simplified):

```text
1. Load base texture from tile_lookup[base_key(sprite_index)]
2. Optional mask texture from tile_lookup[base_key(mask_index)]
3. Read back pixels (atlas readback) into staging surfaces
4. Apply entity tint (or identity)
5. Apply visual effect:
     night / overexposed → option-driven brightness tint on top of filtered pass
     enhanced_night / enhanced_overexposed → enhanced NV option tints
     default → per-pixel filter from get_pixel_function(type)
6. Optional UV warp from warp_cache[warp_hash] → may expand output bounds
7. Pack result into atlas; store tile_lookup[mod_tex_key]
8. Return new texture + warp_output_offset
```

`ACTION_DISPLAY_TILES_NO_VFX` debug flag forces base unfiltered texture only.

### Effect types only on this path

`enhanced_night` and `enhanced_overexposed` use option-based tints here; the standard path
has no prebaked table for them ([06c](./06c-filtered-variants.md)).

---

## Warp cache (state modifiers)

Parsed groups ([07d](./07d-state-modifiers-parsing.md)) are used at **draw** time when
`STATE_MODIFIERS` is enabled:

```text
build_composite_uv_modifier(character, …)  // needs get_sprite_surface
register_warp_surface(composite, offset, offset_mode) → warp_hash
get_or_default(…, warp_hash, sprite_offset)
```

| API | Role |
| --- | --- |
| `get_sprite_surface(sprite_index)` | `tile_lookup` → `readback_find` base texture |
| `ensure_readback_loaded()` | GPU→CPU before reading pixels (once per character draw) |
| `register_warp_surface` | Content-hash key; stores composite UV surface |
| `get_warp_surface(hash)` | Lookup for `get_or_default` warp pass |
| `clear_warp_cache()` | Cleared after each character overlay draw |

Without `DYNAMIC_ATLAS`, `build_composite_uv_modifier` returns null (UV warps disabled).

---

## `ensure_default_item_highlight` (dynamic variant)

```text
index = offset   // next index after all sheets (may equal N)
create atlas sprite (solid blue placeholder)
tile_ids["highlight_item"].sprite.fg = [index]
tile_lookup[base_key(index)] = texture entry
```

Does not append to `tile_values` (vector absent). See [09](./09-post-load-validation.md).

---

## Batch vs non-batch

| Phase | `is_batching` | Pixel write target |
| --- | --- | --- |
| Load (`start_batch` … `end_batch`) | true | CPU `sprite_sheet.surface` |
| Lazy `get_or_default` composite | false | GPU lock or staging blit to atlas |

`start_batch` calls `readback_load()` first so prior GPU content is on CPU before new batch writes.

---

## Standard vs dynamic comparison

| Aspect | Standard (`DYNAMIC_ATLAS` off) | Dynamic (`DYNAMIC_ATLAS` on) |
| --- | --- | --- |
| Load memory | ~8× sprite pixels on GPU | ~1× base sprites |
| Load CPU | Filter all pixels × 8 | Slice + hash + pack once |
| Effect tables | Eight parallel vectors | None |
| Texture lookup | `table[sprite_index]` by fx type | `tile_lookup` composite key |
| Tint / mask at draw | Pre-baked or ignored | Composited in `get_or_default` |
| State modifiers / UV | Not supported | Supported |
| `enhanced_night` | N/A | Runtime tint in `get_or_default` |
| Sprite index space | `0…N-1` into eight tables | `0…N-1` into `tile_lookup` base keys |
| Post-load prune ([09](./09-post-load-validation.md)) | Same `offset` bound | Same — invalid indices in `tile_ids` |

---

## Port guidance

| Goal | Recommendation |
| --- | --- |
| **Minimal port** | Ignore `DYNAMIC_ATLAS`; implement standard eight-table or single-table path |
| **BN desktop parity** | Full dynamic atlas + lazy composite + warp cache |
| **No character UV warp** | Standard path sufficient; skip readback/warp |
| **Simpler runtime** | Prebake filters at load (06c) even if using one atlas for storage |

A port cannot mix paths without `#ifdef`-style branching: data structures differ (`vector` vs
`tile_lookup`).

---

## BN source reference

| Concern | Location |
| --- | --- |
| Atlas class | `src/dynamic_atlas.cpp`, `src/dynamic_atlas.h` |
| Sheet upload | `src/cata_tiles.cpp` — `copy_surface_to_dynamic_atlas` |
| Load bracket | `src/cata_tiles.cpp` — `tileset_loader::load` (`start_batch` / `end_batch`) |
| Lazy lookup | `src/cata_tiles.cpp` — `tileset::get_or_default` |
| Readback / warp | `src/cata_tiles.cpp` — `get_sprite_surface`, `register_warp_surface`, `clear_warp_cache` |
| UV composite | `src/cata_tiles.cpp` — `build_composite_uv_modifier` |
| CMake option | `CMakeLists.txt` — `DYNAMIC_ATLAS` |
| Types / keys | `src/cata_tiles.h` — `tileset_lookup_key`, `tile_lookup_entry` |

---

## Inputs

- Same loader inputs as standard path through sheet decode ([06a](./06a-atlas-grid.md))
- SDL renderer with texture readback support (for warps)
- `DYNAMIC_ATLAS` compile flag

## Outputs

- `tileset_atlas` with packed base sprites
- `tile_lookup` base entries for indices `0…N-1` (plus lazy entries at draw)
- No eight-table vectors
- `warp_cache` empty until draw

## Failure modes

| Condition | Behavior |
| --- | --- |
| Atlas pack full / alloc fail | `debugmsg`; load or composite may fail |
| Duplicate content hash `emplace` | Error log; possible wrong texture |
| `get_or_default` without base key | Returns nullptr |
| Warp without readback | `get_sprite_surface` fails; modifier skipped |
| Hash `0` for warp surface | Stored as hash `1` (avoids `TILESET_NO_WARP` sentinel) |
| Software renderer | One sprite per GPU texture; more atlas pages |

## Verification

A correct dynamic-atlas port should demonstrate:

1. Load completes without eight `*_tile_values` vectors
2. Every valid sprite index has `tile_lookup` base key with non-null texture
3. `get_or_default(i, -1, night, …)` on first call creates second cache entry; second call hits cache
4. Identical sheet cells share atlas placement (hash dedup) but distinct global indices
5. `end_batch` + `readback_load` after load → `get_sprite_surface(i)` returns pixels
6. With `STATE_MODIFIERS` off, `warp_hash=0` → draw matches base + fx composite only
7. `clear_warp_cache` after character draw → warp entries do not leak across characters
8. `highlight_item` synthesized with `tile_lookup` entry when missing from JSON
9. Standard-build binary: `copy_surface_to_dynamic_atlas` not compiled; eight tables present instead
