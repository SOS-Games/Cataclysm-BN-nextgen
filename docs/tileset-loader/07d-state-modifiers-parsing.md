# 07d — State modifiers (parsing)

Parse `state-modifiers` on a `tiles-new` sheet entry into `tileset.state_modifiers`. JSON
schema and authoring rules are [04e](./04e-state-modifiers-config.md). Draw-time UV math is
out of scope.

---

## Purpose

`load_state_modifiers` reads each group in the `state-modifiers` array, resolves per-state
modifier sprite indices, normalizes overlay filter lists, and **merges** into the tileset's
ordered `state_modifiers` vector.

Unlike tile entries ([07b](./07b-tile-registration.md)), state modifiers:

- Do not create `tile_ids` entries
- Use a single optional `fg` int per state (not weighted rotation lists — [07a](./07a-sprite-lists.md))
- Merge by `(group_id, whitelist, blacklist)` rather than by string id alone

---

## When parsing runs

```text
load_internal (per tiles-new sheet)
  ├─ load_tileset(image)              // sprites uploaded; offset = this sheet's base index
  ├─ load_tilejson_from_file(...)
  └─ if sheet.has_array("state-modifiers"):
       load_state_modifiers(sheet)
```

| Context | Notes |
| --- | --- |
| Legacy `tiles` root (no `tiles-new`) | `load_state_modifiers` never called |
| Missing `state-modifiers` key | No-op return |
| Empty `"state-modifiers": []` | No-op (loop runs zero times) |
| Mod tileset sheets | Same path inside mod `load_internal`; `sprite_id_offset` = global count before mod |

Load order within a sheet: **image → tile defs → state modifiers**. Modifier `fg` indices
must reference sprites already present in the global index space (same rules as [07a](./07a-sprite-lists.md)).

---

## Output structures

```text
tileset.state_modifiers: vector<state_modifier_group>   // ordered; index 0 = highest priority

state_modifier_group {
    group_id: string
    override_lower: bool      // JSON "override", default false
    use_offset_mode: bool     // JSON "use_offset", default true
    whitelist: string[]
    blacklist: string[]
    tiles: map<state_id, state_modifier_tile>
}

state_modifier_tile {
    state_id: string
    fg_sprite: optional<int>   // nullopt = identity (no UV warp)
    offset: point              // draw offset for oversized modifier sprites
}
```

Storage types: `src/cata_tiles.h`.

---

## Algorithm

```text
function load_state_modifiers(config):
    if not config.has_array("state-modifiers"):
        return

    for mod_group in config["state-modifiers"]:
        group = new state_modifier_group
        group.group_id      = mod_group.get_string("id")
        group.override_lower = mod_group.get_bool("override", false)
        group.use_offset_mode = mod_group.get_bool("use_offset", true)

        if mod_group.has_array("whitelist"):
            group.whitelist = copy all strings from array
        if mod_group.has_array("blacklist"):
            group.blacklist = copy all strings from array

        if not mod_group.has_array("tiles"):
            throw_error("state-modifier group must have a 'tiles' array")

        for tile_entry in mod_group["tiles"]:
            tile = parse_state_modifier_tile(tile_entry)
            group.tiles[tile.state_id] = tile    // duplicate state_id → last wins

        sort + dedupe group.whitelist
        sort + dedupe group.blacklist

        existing = find in ts.state_modifiers where
            g.group_id == group.group_id
            && g.whitelist == group.whitelist
            && g.blacklist == group.blacklist

        if existing:
            *existing = move(group)     // replace in place; keeps vector index
        else:
            ts.state_modifiers.push_back(move(group))
```

---

## Per-state tile parsing

`parse_state_modifier_tile(tile_entry)`:

| JSON field | Parsing |
| --- | --- |
| `id` | Required string → `state_id` |
| `fg` | See [fg resolution](#fg-resolution) |
| `offset_x` | `get_int("offset_x", sprite_offset.x)` |
| `offset_y` | `get_int("offset_y", sprite_offset.y)` |

`sprite_offset` is the loader's current sheet `sprite_offset_x` / `sprite_offset_y` (from the
parent `tiles-new` entry).

### `fg` resolution

```text
if tile_entry.has_null("fg"):
    fg_sprite = nullopt                    // identity
else if tile_entry.has_int("fg"):
    v = tile_entry.get_int("fg")
    if v >= 0:
        fg_sprite = v + sprite_id_offset
    else:
        fg_sprite = nullopt                // negative → identity
else:
    fg_sprite = nullopt                    // omitted or non-int/non-null → identity
```

| JSON | Stored `fg_sprite` |
| --- | --- |
| `"fg": null` | `nullopt` (identity) |
| `"fg": 5` | `5 + sprite_id_offset` |
| `"fg": -1` | `nullopt` |
| `fg` omitted | `nullopt` |
| `"fg": [1,2]` (array) | `nullopt` (not int; treated as omitted) |

No post-load prune removes out-of-range modifier indices (unlike tile sprite lists). Invalid
indices surface at draw time when the modifier texture is looked up.

---

## Filter list normalization

Before merge lookup, both lists are:

```text
sort ascending
unique (adjacent duplicates removed)
```

Merge key equality uses the **normalized** vectors. `["worn_", "wielded_"]` and
`["wielded_", "worn_"]` are the same group key.

Empty whitelist and empty blacklist are valid and participate in the key.

---

## Merge semantics

### Unique key

```text
(group_id, whitelist, blacklist)   // after sort + dedupe
```

Same `group_id` with different filters → **separate** vector entries (e.g. movement UV for
`wielded_` only vs base character).

### Replace vs append

| Situation | Action |
| --- | --- |
| Key matches existing group | **Replace** group content at same vector index |
| Key is new | **Append** to end of `state_modifiers` |

Replace updates all fields (`override_lower`, `use_offset_mode`, `tiles` map, filters) but
does **not** move the group's priority position in the vector.

### Override timeline

```text
Base sheet 1:  movement_mode (no filters)     → appended at index 0
Base sheet 2:  movement_mode (no filters)     → replaces index 0
Mod sheet:     movement_mode (no filters)     → replaces index 0 again
Mod sheet:     movement_mode whitelist ["worn_"] → appended at index 1 (new key)
```

Later **array position** in a single `load_state_modifiers` call only matters for first-time
appends; replacements keep the slot of the first registration with that key.

Mod load order follows `all_mod_tilesets` registration order ([04f](./04f-mod-tileset-config.md)).

---

## Duplicate state ids within one group

```json
"tiles": [
  { "id": "crouch", "fg": 1 },
  { "id": "crouch", "fg": 2 }
]
```

The second entry overwrites the first in `group.tiles` (`unordered_map` assignment). No error.

---

## Relationship to global warp lists

On the same `tiles-new` entry, `global-warp-whitelist` / `global-warp-blacklist` are loaded
**after** state modifiers in `load_internal`. They are separate tileset fields used at draw
time when a group does not define its own filters ([04e](./04e-state-modifiers-config.md)).

State modifier parsing does not read or modify global warp lists.

---

## Port guidance

| Goal | Approach |
| --- | --- |
| **BN parity** | Full algorithm + merge key + ordered vector |
| **Minimal / no UV warp** | Skip `state-modifiers` key entirely; leave `state_modifiers` empty |
| **Read without apply** | Parse and store; ignore at draw unless `STATE_MODIFIERS`-equivalent enabled |

Simpler than tile registration: no seasonal index, no masks, no multitile. Main pitfalls:

- Forgetting `sprite_id_offset` on mod sheets
- Using replace key without normalizing filter lists
- Expecting later append to change priority when key matches (it replaces in place)

---

## BN source reference

| Concern | Location |
| --- | --- |
| Parser | `src/cata_tiles.cpp` — `load_state_modifiers` |
| Call site | `src/cata_tiles.cpp` — `load_internal` (after `load_tilejson_from_file`) |
| Types | `src/cata_tiles.h` — `state_modifier_group`, `state_modifier_tile` |
| Accessor | `src/cata_tiles.h` — `tileset::get_state_modifiers()` |
| Config schema | [04e](./04e-state-modifiers-config.md) |

---

## Inputs

- `state-modifiers` array on a `tiles-new` sheet object
- `sprite_id_offset` (0 for base, global count before current mod)
- `sprite_offset` / `sprite_offset_retracted` (defaults for per-state `offset_x` / `offset_y`)
- Existing `ts.state_modifiers` (for replace)

## Outputs

- Updated `tileset.state_modifiers` vector
- Per group: flags, normalized filters, `tiles` map with resolved `fg_sprite` and offsets

## Failure modes

| Condition | Behavior |
| --- | --- |
| Group missing `tiles` array | JSON `throw_error` |
| Missing top-level `state-modifiers` | No-op |
| Negative `fg` int | Stored as identity (`nullopt`) |
| Invalid / out-of-range `fg` after offset | Stored as-is; no load-time validation |
| Duplicate `(group_id, whitelist, blacklist)` in later load | Replace earlier |
| Duplicate `id` within group `tiles` | Last entry wins |
| Non-int `fg` (array, string) | Identity |

## Verification

A correct port should demonstrate:

1. Sheet without `state-modifiers` → vector unchanged
2. One group with three `tiles` entries → map size 3
3. `"fg": null` → `fg_sprite` unset / nullopt
4. `"fg": 5` with `sprite_id_offset = 100` → stored `105`
5. `"fg": -3` → identity
6. Omitted `fg` → identity
7. Second load with same `id` + same filters → same vector length, content replaced, index unchanged
8. Same `id`, `whitelist: ["worn_"]` vs no whitelist → two vector entries
9. `whitelist: ["b", "a", "a"]` → stored `["a", "b"]` for merge key
10. `override: true` and `use_offset: false` preserved on stored group
11. `offset_x` omitted → uses sheet `sprite_offset_x`
12. Mod sheet: local `fg: 0` with `sprite_id_offset = 150` → stored `150`
