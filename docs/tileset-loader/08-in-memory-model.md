# 08 — In-memory model

Data structures that exist after a successful full tileset load. Built by units 05–07 and
finalized by unit 09. Texture tables: [06b](./06b-texture-upload.md), [06c](./06c-filtered-variants.md).
Tile defs: [07b](./07b-tile-registration.md). State modifiers: [07d](./07d-state-modifiers-parsing.md).

This unit describes **what is stored** and **how to resolve ids → sprite indices → textures**.
It does not describe the full draw pipeline (`draw_from_id_string`, lighting, entity iteration).

---

## Purpose

A loaded tileset is a single `tileset` object owned by `cata_tiles`. Until the next reload:

- String tile ids map to `tile_type` definitions
- Integer sprite indices map to GPU `texture` slots (and parallel effect tables)
- Seasonal and state-modifier data sit beside those maps for runtime lookup

Ports can treat `tileset` as the complete loader output; `cata_tiles` adds screen scaling and
caching around it.

---

## Ownership and lifetime

```text
cata_tiles
  ├─ tileset_ptr: unique_ptr<tileset>     // replaced atomically on successful load
  ├─ tileset_mod_list_stamp              // mod list used for last load (cache key)
  ├─ tile_width, tile_height             // screen pixels per tile (scaled)
  └─ tile_iso                            // global bool from tile_info (extern)

tileset_loader (temporary)
  └─ builds tileset during load(); destroyed after load completes
```

| Event | Effect |
| --- | --- |
| Successful `cata_tiles::load_tileset` | New `tileset` allocated; old one destroyed |
| `FORCE_TILESET_RELOAD` or id/mod change | Reload |
| Same id + same mod list + no force | Load skipped; model unchanged |
| Precheck load | No `tileset_ptr` replacement (loader returns early) |

**Invariant:** All pointers returned from `find_tile_type` / `find_tile_type_by_season` point
into `tileset::tile_ids`. They are invalid after reload.

---

## Global sprite index space

All sheets and mods share one dense index range:

```text
0 … N-1     where N = tile_values.size() (non-dynamic path)
```

| Index source | Meaning |
| --- | --- |
| Sheet upload order | Sheet 1 → `[0, size₁)`, sheet 2 → `[size₁, size₁+size₂)`, … |
| `tile_type.sprite.fg/bg` | Each variant holds `vector<int>` of indices into this space |
| `state_modifier_tile.fg_sprite` | Optional single index |
| `TILESET_NO_MASK` (`-1`) | Sentinel in mask lists, not a texture index |

After load (unit 09), tile sprite lists contain only indices in `[0, N)`.

---

## `tileset` — top-level fields

### Metadata (from `tile_info` + load end)

| Field | Type | Notes |
| --- | --- | --- |
| `tileset_id` | string | Active pack id; set at end of load |
| `tile_width`, `tile_height` | int | Logical cell size from `tile_info` |
| `tile_pixelscale` | float | Pack-wide scale multiplier |
| `zlevel_height` | int | Parsed then **forced to 0** in BN loader |
| `prevent_occlusion_min_dist` | float | Parsed then **forced to -1** |
| `prevent_occlusion_max_dist` | float | Parsed then **forced to 0** |

### Texture tables (standard build)

Eight parallel `vector<texture>` — same length `N`, aligned by index ([06c](./06c-filtered-variants.md)):

```text
tile_values              // base / none
shadow_tile_values
night_tile_values
overexposed_tile_values
underwater_tile_values
underwater_dark_tile_values
memory_tile_values
z_overlay_values
```

`DYNAMIC_ATLAS` builds use `dynamic_atlas` + `tile_lookup` map instead; see appendix A1.

### Tile definitions

| Field | Type | Purpose |
| --- | --- | --- |
| `tile_ids` | `unordered_map<string, tile_type>` | Primary id → definition map |
| `tile_ids_by_season` | `unordered_map<string, season_tile_value>[4]` | Seasonal resolve cache |

### Optional / config-driven

| Field | Type | Purpose |
| --- | --- | --- |
| `state_modifiers` | `vector<state_modifier_group>` | UV warp groups ([07d](./07d-state-modifiers-parsing.md)) |
| `global_warp_whitelist` | `vector<string>` | Overlay prefix filter (last sheet wins per prefix list) |
| `global_warp_blacklist` | `vector<string>` | Same |
| `tints` | `unordered_map<string, color_tint_pair>` | Named fg/bg tint configs |
| `tint_pairs` | `unordered_map<string, pair<string,bool>>` | Tint controller wiring |

### Outside `tileset` but load-related

`tileset_mutation_overlay_ordering` — global `map<string,int>` (mutation draw order from
`overlay_ordering` in JSON). Cleared at start of each tileset load; not stored on `tileset`.

---

## `texture`

Each sprite slot ([06b](./06b-texture-upload.md)):

```text
texture {
    gpu_texture: shared handle
    source_rect: (x, y, w, h) sub-region within gpu_texture
}
```

`get_or_default` returns `texture_result { const texture *tex, point warp_offset }`.
`warp_offset` is non-zero only on the `DYNAMIC_ATLAS` path when UV warp expands bounds.

---

## `tile_type` — per-id definition

Populated by [07b](./07b-tile-registration.md).

```text
tile_type {
    sprite: { fg, bg }              // sprite_pair — weighted_int_list<vector<int>> each
    masks:  { tint: { fg, bg } }   // parallel mask indices; -1 = no mask

    multitile: bool
    rotates: bool
    animated: bool
    has_om_transparency: bool
    is_multitile_subtile: bool

    height_3d: int
    offset, offset_retracted: point   // draw / UV warp alignment
    pixelscale: float

    available_subtiles: string[]      // parent multitile only — sub-id names in load order
    flags: set<flag_id>
    default_tint: optional<SDL_Color>
}
```

### Sprite pair semantics

```text
sprite.fg / sprite.bg:
  variant₀: { weight, obj: [idx] }           // static
  variant₁: { weight, obj: [i₀,i₁,i₂,i₃] }  // rotations
  …
```

- **Variant** chosen at draw time via `weighted_int_list::pick` (uses `precalc()` from unit 09)
- **Rotation index** `0…|obj|-1` chosen from facing / connection logic at draw time
- Either fg or bg may be empty; tiles with **both** empty are removed before load finishes

### Multitile layout

```text
tile_ids["t_wall"]              // parent — multitile=true, available_subtiles=["center","corner",…]
tile_ids["t_wall_center"]       // subtile — is_multitile_subtile may be true
tile_ids["t_wall_corner"]
```

Subtiles are independent map entries; the parent does not embed their sprites.

---

## Seasonal cache

```text
season_tile_value {
    default_tile: tile_type*              // pointer into tile_ids
    season_tile: optional<tile_lookup_res> // { id ref, tile ref } for suffixed variant
}
```

Keyed by **base id** (no `_season_*` suffix) in `tile_ids_by_season[season]`.

Populated in `create_tile_type` ([07b](./07b-tile-registration.md)):

| Registered id | Cache update |
| --- | --- |
| `t_tree` | `default_tile` set in all four season maps |
| `t_tree_season_winter` | `tile_ids_by_season[winter]["t_tree"].season_tile` set |

### Lookup API

```text
find_tile_type(id) → const tile_type*
    Direct map lookup; id must be exact key (including seasonal suffix if used).

find_tile_type_by_season(base_id, season) → optional<tile_lookup_res>
    1. If season_tile set for (base_id, season) → return seasonal variant
    2. Else if default_tile set → return { base_id, *default_tile }
    3. Else → nullopt
```

`cata_tiles::find_tile_with_season(id)` wraps this with `season_of_year(calendar::turn)`.

**Note:** Header comments describe suffix concatenation conceptually; the implementation uses
the pre-built cache only (no runtime string append).

---

## Sprite index → texture

### `tileset_fx_type`

```text
none, shadow, night, enhanced_night, overexposed, enhanced_overexposed,
underwater, underwater_dark, memory, z_overlay
```

Prebaked tables (standard build) map a subset at load time ([06c](./06c-filtered-variants.md)).
`enhanced_*` variants are handled on the `DYNAMIC_ATLAS` path only.

### `get_or_default(sprite_index, mask_index, type, tint, warp_hash, sprite_offset)`

**Standard build** (non-`DYNAMIC_ATLAS`):

```text
if sprite_index >= tile_values.size():
    return { nullptr, (0,0) }

switch type:
    shadow          → shadow_tile_values[sprite_index]
    night           → night_tile_values[sprite_index]
    overexposed     → overexposed_tile_values[sprite_index]
    underwater      → underwater_tile_values[sprite_index]
    underwater_dark → underwater_dark_tile_values[sprite_index]
    memory          → memory_tile_values[sprite_index]
    z_overlay       → z_overlay_values[sprite_index]
    default         → tile_values[sprite_index]

warp_offset = (0, 0)
```

`mask_index`, `tint`, `warp_hash`, and `sprite_offset` are ignored in this path (masks and
warps are not composited here).

**`DYNAMIC_ATLAS`:** lazy composite into `tile_lookup` keyed by
`tileset_lookup_key { sprite_index, mask_index, effect, tint, warp_hash, sprite_offset }`.
See appendix A1.

### Constants

| Name | Value | Use |
| --- | --- | --- |
| `TILESET_NO_MASK` | `-1` | No mask sprite |
| `TILESET_NO_WARP` | `0` | No UV warp hash |
| `TILESET_NO_COLOR` | `{0,0,0,0}` | Default tint_config |

---

## State modifiers (stored form)

```text
state_modifier_group {
    group_id: string
    override_lower: bool
    use_offset_mode: bool
    whitelist, blacklist: string[]
    tiles: map<state_id, state_modifier_tile>
}

state_modifier_tile {
    state_id: string
    fg_sprite: optional<int>    // nullopt = identity
    offset: point
}
```

- Vector index `0` = highest priority
- Read-only access: `tileset::get_state_modifiers()`
- Draw code matches game state → `fg_sprite` → warp surface → `get_or_default` (out of scope here)

---

## Tints (stored form)

```text
color_tint_pair = pair<tint_config, tint_config>   // { bg, fg }

tint_config {
    color: SDL_Color
    blend_mode: tint_blend_mode
    contrast, saturation, brightness: float   // 1.0 = neutral
}
```

Loaded from root/mod `tints` arrays in `load_internal`. `tint_pairs` maps controller ids to
`(target_tint_id, bool)`.

---

## Resolution graph (id → pixels)

Conceptual chain a port needs; draw code adds lighting, overlays, and entity context:

```text
string tile_id
    │
    ├─ find_tile_type(id)                    exact key
    └─ find_tile_type_by_season(base, season) cached default / seasonal
            │
            ▼
        tile_type
            │
            ├─ sprite.fg.pick() → variant
            ├─ variant.obj[rotation] → sprite_index
            ├─ optional masks.tint.fg same variant/rotation → mask_index
            │
            ▼
        get_or_default(sprite_index, mask_index, fx_type, …)
            │
            ▼
        texture + source_rect  (+ warp_offset if dynamic atlas)
```

`looks_like` fallback chains live on `cata_tiles`, not `tileset` — omitted here per scope.

---

## Invariants after successful load

1. **Index alignment:** All eight `vector<texture>` tables have equal length `N`.
2. **Valid references:** Every sprite index in every `tile_type` satisfies `0 ≤ idx < N` (after unit 09).
3. **Non-empty tiles:** Every remaining `tile_ids` entry has `fg` or `bg` non-empty.
4. **Season pointers:** `default_tile` / `season_tile` point at live `tile_ids` values.
5. **Stability:** No loader code mutates `tile_ids` or texture tables after load returns (except `DYNAMIC_ATLAS` lazy `tile_lookup` growth).
6. **Id uniqueness:** One `tile_type` per key; subtiles use `{parent}_{subid}` keys.
7. **`precalc`:** Every non-empty `sprite_list` on remaining tiles has `precalc()` called.
8. **`highlight_item`:** Present (tileset-defined or synthesized in unit 09).
9. **`unknown`:** May be absent (warning only).

---

## Minimal port subset

| Full BN | Minimal sprites-only |
| --- | --- |
| Eight texture tables | Single `textures[]` |
| `tile_ids` + seasonal cache | `tile_ids` only |
| `state_modifiers`, warp lists | Omit |
| `tints`, `tint_pairs` | Omit |
| `get_or_default` fx switch | Always `textures[i]` |
| Multitile subtiles | Optional if no connection rendering |

---

## BN source reference

| Concern | Location |
| --- | --- |
| `tile_type`, `tileset` | `src/cata_tiles.h` |
| `texture`, `texture_result` | `src/cata_tiles.h` |
| Season cache write | `src/cata_tiles.cpp` — `tileset::create_tile_type` |
| Season lookup | `src/cata_tiles.cpp` — `tileset::find_tile_type_by_season` |
| Texture lookup | `src/cata_tiles.cpp` — `tileset::get_or_default` |
| Load → `tileset_ptr` | `src/cata_tiles.cpp` — `cata_tiles::load_tileset` |
| Screen tile size | `src/cata_tiles.cpp` — `set_draw_scale` |
| Dynamic atlas model | [appendix-dynamic-atlas.md](./appendix-dynamic-atlas.md) |

---

## Inputs

- Completed `tileset_loader::load()` (full, not precheck)
- Post-validation state from unit 09

## Outputs

- Populated `tileset` ready for read-only queries until reload
- `cata_tiles::tileset_ptr` pointing at it; `tile_iso` and scaled `tile_width`/`tile_height` updated

## Failure modes

| Condition | Model state |
| --- | --- |
| Load throws mid-way | Old `tileset_ptr` retained (load uses temp `tileset` until success) |
| Precheck | Previous model unchanged |
| Missing `unknown` | Model valid; warning logged |
| Out-of-range index if 09 skipped | `get_or_default` returns nullptr or wrong slot |

## Verification

A correct port should demonstrate:

1. After load, `tile_values.size()` equals sum of sheet sprite counts
2. `find_tile_type("t_dirt")` returns same pointer as `tile_ids["t_dirt"]`
3. Register `t_tree` + `t_tree_season_winter` → winter lookup returns winter id string; spring returns base
4. `get_or_default(i, -1, night)` reads `night_tile_values[i]` with same rect layout as `tile_values[i]`
5. Multitile parent has `available_subtiles.size()` == number of `additional_tiles` loaded
6. `get_state_modifiers().size()` matches parsed groups
7. Reload replaces `tileset_ptr` address; old pointers invalid
8. `TILESET_NO_MASK` entries in mask lists do not index textures
9. Eight table lengths equal for all indices `0…N-1`
