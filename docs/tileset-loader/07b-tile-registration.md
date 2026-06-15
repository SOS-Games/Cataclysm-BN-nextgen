# 07b — Tile registration (parsing)

Register tile JSON entries into `tileset::tile_ids` and the seasonal lookup cache. Sprite
list parsing is [07a](./07a-sprite-lists.md). Entry schema is [04c](./04c-tile-entries.md).
Post-load pruning is [09](./09-post-load-validation.md).

---

## Purpose

After a sheet image is uploaded (unit 06), `load_tilejson_from_file` walks the sheet's
`tiles[]` array and, for each entry:

1. Resolves one or more string **tile ids**
2. Builds a `tile_type` (sprites, masks, flags, …) via `load_tile`
3. Inserts or **overwrites** the id in `tile_ids` via `create_tile_type`
4. Registers **multitile subtiles** as separate ids
5. Updates the **seasonal index** for draw-time lookup

---

## Call chain

```text
load_internal (per sheet)
  └─ load_tilejson_from_file(config)     // config = tiles-new part or legacy root
       for each entry in config["tiles"]:
         resolve id(s)
         for each t_id:
           load_tile(entry, t_id)         → create_tile_type (sprites + masks)
           apply sheet + entry metadata on returned tile_type &
           if multitile: register subtiles
```

**Requires** a `"tiles"` member on the config object. Missing → JSON `throw_error`. An
empty `"tiles": []` array is valid (no registrations).

---

## Entry iteration: `id` field

```text
if entry has string "id":
    ids = [ that string ]
else if entry has array "id":
    ids = string array
else:
    ids = []    // entry skipped entirely
```

| Case | Behavior |
| --- | --- |
| `"id": "t_dirt"` | One registration |
| `"id": ["t_dirt", "t_dirtmound"]` | Same entry data registered twice, independent map keys |
| No `id` / empty `id` array | Entry ignored (no error) |

Each `t_id` in `ids` runs the full registration path independently.

---

## `load_tile` — per-id sprite bundle

Creates a fresh `tile_type`, fills sprite data, then commits via `create_tile_type`:

```text
load_tile(entry, id):
    curr = default tile_type

    load_tile_spritelists(entry, curr.sprite.fg, "fg")    // 07a
    load_tile_spritelists(entry, curr.sprite.bg, "bg")

    for each mask in entry["masks"] (if present):
        if mask.type == "tint":
            load_tile_spritelists(mask, curr.masks.tint.fg, "fg")
            load_tile_spritelists(mask, curr.masks.tint.bg, "bg")
        else:
            debugmsg warning (invalid mask type)

    ensure_mask(curr.masks.tint.fg, curr.sprite.fg)
    ensure_mask(curr.masks.tint.bg, curr.sprite.bg)

    curr.has_om_transparency = entry.get_bool("has_om_transparency", false)

    return create_tile_type(id, move(curr))
```

### Mask alignment (`ensure_mask`)

| State | Action |
| --- | --- |
| Mask list non-empty and matches sprite (same variant count, weights, rotation lengths) | Keep mask indices |
| Mask list non-empty but mismatch | `debugmsg`; clear masks |
| Mask list empty after above | Auto-fill: one mask variant per sprite variant, each rotation slot = `TILESET_NO_MASK` (`-1`) |

Only `masks[].type == "tint"` is supported.

**Note:** `load_tile` sets only `has_om_transparency` and sprite/mask fields. Multitile
flags, `rotates`, tints, and sheet offsets are applied afterward in
`load_tilejson_from_file` (see below).

---

## `load_tilejson_from_file` — metadata after `load_tile`

For each `t_id`, after `load_tile` returns `curr_tile` (a reference into `tile_ids`):

### Sheet context (from current `tiles-new` part)

| Field on `tile_type` | Source |
| --- | --- |
| `offset` | `sprite_offset` (loader field for this sheet) |
| `offset_retracted` | `sprite_offset_retracted` |
| `pixelscale` | `sprite_pixelscale` |

### Entry-level fields (parent tile)

| Field | JSON | Default |
| --- | --- | --- |
| `multitile` | `multitile` | `false` |
| `rotates` | `rotates` | `multitile` value if omitted |
| `height_3d` | `height_3d` | `0` |
| `flags` | `flags` | `[]` |
| `default_tint` | `default_tint` | unset (`std::nullopt`) |
| `animated` | `animated` | `false` |
| `is_multitile_subtile` | — | `false` |

`default_tint` uses the same color parsing as elsewhere (`entry.read("default_tint", …)` —
named colors or hex).

---

## Multitile registration

### Valid configuration

```json
{
  "id": "t_wall",
  "fg": 2918,
  "multitile": true,
  "additional_tiles": [
    { "id": "center", "fg": 2919 },
    { "id": "corner", "fg": [2924, 2922, 2922, 2923] }
  ]
}
```

### Invalid configuration

```text
additional_tiles present AND multitile != true
  → throw_error("Additional tiles defined, but 'multitile' is not true.")
```

### Subtile algorithm

When `multitile` is true:

```text
for each subentry in entry["additional_tiles"]:   // missing key → empty array, no subtiles
    s_id = subentry["id"]
    m_id = t_id + "_" + s_id

    curr_subtile = load_tile(subentry, m_id)

    curr_subtile.offset           = sprite_offset
    curr_subtile.offset_retracted = sprite_offset_retracted
    curr_subtile.pixelscale       = sprite_pixelscale
    curr_subtile.rotates            = true          // always, not from JSON
    curr_subtile.is_multitile_subtile = (s_id in multitile_keys)
    curr_subtile.height_3d        = parent height_3d
    curr_subtile.animated         = subentry.get_bool("animated", false)
    curr_subtile.default_tint     = parent default_tint
    curr_subtile.flags            = parent flags

    parent.available_subtiles.push_back(s_id)
```

### Stored ids

```text
parent:  t_wall
subtile: t_wall_center, t_wall_corner, …
```

Subtiles are **full** `tile_ids` entries (own fg/bg, masks, etc.). The parent keeps its own
fg/bg from the parent entry; subtiles do not inherit parent sprites.

### `multitile_keys` (connection subtiles)

These `s_id` values set `is_multitile_subtile = true`:

`center`, `corner`, `edge`, `t_connection`, `end_piece`, `unconnected`, `open`, `broken`

Any other sub-id still loads; draw code may not treat it as a connection subtile.

### Parent `available_subtiles`

Ordered list of `s_id` strings from `additional_tiles` — used at draw time to resolve
`{parent}_{subid}` without scanning the map.

---

## `create_tile_type` — map insert + seasonal index

```text
create_tile_type(id, new_tile_type):
    tile_ids[id] = move(new_tile_type)     // overwrites existing id
    inserted_tile = reference to tile_ids[id]

    if id ends with one of:
        "_season_spring", "_season_summer", "_season_autumn", "_season_winter":
        base_id = id without suffix
        tile_ids_by_season[that_season][base_id].season_tile =
            { id, inserted_tile }
    else:
        for each season map (all four):
            tile_ids_by_season[*][id].default_tile = &inserted_tile
```

### Season suffix table

| Suffix | Season index |
| --- | --- |
| `_season_spring` | spring (0) |
| `_season_summer` | summer (1) |
| `_season_autumn` | autumn (2) |
| `_season_winter` | winter (3) |

Suffix length is fixed at 15 characters in BN (`_season_spring`, etc.).

### Lookup semantics (`find_tile_type_by_season`)

At draw time (unit 08):

```text
lookup(base_id, season):
    entry = tile_ids_by_season[season][base_id]
    if entry.season_tile:  return seasonal variant
    if entry.default_tile: return default variant (base id's tile_type)
    return not found
```

| Registration order | Result for winter |
| --- | --- |
| `t_tree` then `t_tree_season_winter` | Winter uses seasonal sprite; other seasons use default |
| `t_tree_season_winter` then `t_tree` | Winter still prefers seasonal (`season_tile` wins) |

Seasonal ids are stored in `tile_ids` under the **full** suffixed string. The seasonal cache
keys by **base** id (without suffix).

### Override behavior

Every `create_tile_type` call **replaces** `tile_ids[id]` and refreshes seasonal pointers.
Later entries in the same `tiles[]` array, later sheets, and later mod merges all win on id
collision.

---

## Override timeline

```text
Same load order as JSON / sheets / mods (unit 05):

Sheet 1 entry A  id: rock   fg: 10
Sheet 2 entry B  id: rock   fg: 99     → tile_ids["rock"].fg uses 99

Mod entry        id: rock   fg: 5      → overwrites again (if mod loads last)
```

Multitile parent and subtiles override independently (`t_wall` vs `t_wall_corner`).

---

## What registration does **not** do

| Concern | Handled in |
| --- | --- |
| `fg`/`bg` JSON shapes | [07a](./07a-sprite-lists.md) |
| Sprite index offset (`sprite_id_offset`) | [07a](./07a-sprite-lists.md) |
| Strip out-of-range sprite indices | [09](./09-post-load-validation.md) — `process_variations_after_loading` |
| Remove tiles with empty fg and bg | [09](./09-post-load-validation.md) |
| `state-modifiers` | [07d](./07d-state-modifiers-parsing.md) |
| `tints`, `overlay_ordering` | Other branches in `load_internal` |
| ASCII tile defs | Skipped (sprites-only port) |

---

## Post-load hooks (cross-reference)

After all sheets and mods, `tileset_loader::load` runs:

1. `process_variations_after_loading` on every tile's fg/bg
2. Erase tiles with both fg and bg empty (also tries to erase seasonal cache by **full** tile id string)
3. Warn if `unknown` id missing
4. `ensure_default_item_highlight()` — synthesizes `highlight_item` if absent (bypasses `create_tile_type`; does not update seasonal cache)

---

## Port guidance

| Goal | Minimum |
| --- | --- |
| **BN parity** | `tile_ids` map + seasonal cache + multitile subtiles + mask fill |
| **Minimal** | `map<string, tile_type>` only; skip seasonal cache (always use base id); skip multitile subtiles if no connection rendering |
| **Multitile without seasons** | Parent + `{id}_{subid}` entries + `available_subtiles` |

Equivalent registration pseudocode:

```text
function load_tilejson_from_file(config):
    require config.tiles
    for entry in config.tiles:
        ids = resolve_ids(entry)
        for t_id in ids:
            tile = load_tile(entry, t_id)
            apply_sheet_offsets(tile)
            multi = entry.multitile ?? false
            if multi:
                for sub in entry.additional_tiles ?? []:
                    m_id = t_id + "_" + sub.id
                    subtile = load_tile(sub, m_id)
                    decorate_subtile(subtile, parent_fields)
                    parent.available_subtiles.push(sub.id)
            else if entry.additional_tiles:
                error
            apply_parent_metadata(tile, entry, multi)
```

---

## BN source reference

| Concern | Location |
| --- | --- |
| Iterate `tiles[]` | `src/cata_tiles.cpp` — `load_tilejson_from_file` |
| Per-entry sprites | `src/cata_tiles.cpp` — `load_tile` |
| Map + seasonal index | `src/cata_tiles.cpp` — `tileset::create_tile_type` |
| Season lookup | `src/cata_tiles.cpp` — `tileset::find_tile_type_by_season` |
| Multitile key list | `src/cata_tiles.cpp` — `multitile_keys` |
| `tile_type` layout | `src/cata_tiles.h` — `struct tile_type` |
| Highlight fallback | `src/cata_tiles.cpp` — `ensure_default_item_highlight` |

---

## Inputs

- JSON object with `"tiles"` array (sheet part or legacy root)
- Loader sheet context: `sprite_offset`, `sprite_offset_retracted`, `sprite_pixelscale`
- `sprite_id_offset` for sprite parsing (07a)
- Existing `tile_ids` (for overwrite)

## Outputs

- New or updated `tile_ids` entries (`tile_type` per string id)
- Multitile subtiles at `{parent_id}_{sub_id}`
- `available_subtiles` on multitile parents
- Updated `tile_ids_by_season` entries (`default_tile` and/or `season_tile`)

## Failure modes

| Condition | Behavior |
| --- | --- |
| Missing `"tiles"` member | JSON `throw_error` |
| `additional_tiles` without `multitile: true` | JSON `throw_error` |
| Invalid mask `type` | Warning; mask ignored |
| Mask weight/count mismatch | Warning; masks auto-filled with `TILESET_NO_MASK` |
| Entry without `id` | Skipped |
| Empty fg+bg after post-load | Tile removed from `tile_ids` (unit 09) |

## Verification

A correct port should demonstrate:

1. Two entries with same `id` in one file → second wins
2. `"id": ["a","b"]` → two map keys with identical `tile_type` sprite data
3. `multitile: true` + sub `corner` → `t_wall_corner` exists; parent `available_subtiles` contains `"corner"`
4. `additional_tiles` without `multitile` → parse error
5. `t_tree` + `t_tree_season_winter` → winter lookup returns winter tile; spring returns default
6. Re-registering `t_tree` after seasonal variant → winter still uses seasonal entry
7. Mask omitted → `masks.tint.fg` parallel to `sprite.fg` with all `-1`
8. `has_om_transparency: true` on entry → set on `tile_type` after `load_tile`
9. Sheet `sprite_offset_x/y` → copied to `tile_type.offset` for every id on that sheet
