# 07a — Sprite lists (parsing)

Parse `fg` and `bg` JSON fields into weighted rotation lists. JSON schema is in
[04c](./04c-tile-entries.md). Tile registration that calls this parser is
[07b](./07b-tile-registration.md). Post-load pruning is cross-referenced in unit 09.

---

## Purpose

`load_tile_spritelists` converts one field (`"fg"` or `"bg"`) on a tile entry object into a
`weighted_int_list<std::vector<int>>`: a list of **variants**, each variant holding **1, 2, or 4**
sprite indices (rotation frames) and an integer **weight** for random selection at draw time.

The same function is used for:

- Tile `fg` / `bg`
- Tint mask `fg` / `bg` inside `masks[]` entries

State-modifier `fg` uses a separate, simpler path ([07d](./07d-state-modifiers-parsing.md)).

---

## Output type

```text
weighted_int_list<std::vector<int>>
  └─ [ variant₀, variant₁, … ]
       each variant = { obj: [sprite_index, …], weight: int }
```

| Field | Meaning |
| --- | --- |
| `obj` | Sprite indices into global texture tables (after offset addition) |
| `weight` | Relative pick weight; `0` is allowed |

Stored on `tile_type` as `tile_type::sprite_list` inside `sprite_pair`:

```text
tile_type.sprite.fg   // weighted rotation lists
tile_type.sprite.bg
tile_type.masks.tint.fg / .bg   // parallel mask indices (see masks below)
```

`precalc()` on the list is **not** called here; it runs in `process_variations_after_loading`
at end of load (unit 09).

---

## When parsing runs

```text
load_tilejson_from_file
  └─ load_tile(entry, id)
       ├─ load_tile_spritelists(entry, sprite.fg, "fg")
       ├─ load_tile_spritelists(entry, sprite.bg, "bg")
       └─ for each masks[] tint entry:
            load_tile_spritelists(mask, masks.tint.fg, "fg")
            load_tile_spritelists(mask, masks.tint.bg, "bg")
```

Per `tiles-new` sheet (unit 05): image upload runs first, then tile JSON for that sheet.

---

## Index offset: `sprite_id_offset`

Every parsed integer is adjusted before storage:

```text
stored_index = json_index + sprite_id_offset   // if stored_index >= 0
```

| Load context | `sprite_id_offset` |
| --- | --- |
| Base tileset (`tiles-new` or legacy `tiles`) | `0` for all base sheets |
| Each compatible mod tileset | `offset` immediately **before** that mod's `load_internal` |

`offset` (loader field) is the running total of uploaded sprites; it grows after each sheet.
`sprite_id_offset` is **not** updated between sheets inside one `load_internal` call.

```text
Base sheet 1:  json 35  →  35 + 0 = 35
Base sheet 2:  json 5   →  5 + 0 = 5   (must be global index if compose did not renumber)
Mod sheet 1:   json 0   →  0 + 150 = 150   (example: offset was 150 before mod)
```

**Authoring rule:** Composed base packs use **global** indices. Mod packs use **local**
indices relative to that mod's first sprite slot (`sprite_id_offset`). Multi-sheet mods still
share one `sprite_id_offset`; local indices must account for prior mod sheets if more than one
PNG is referenced (compose tool handles this). See [05](./05-load-pipeline.md) timeline.

Negative values after addition are **not** appended (silent drop at parse time).

---

## Decision tree

For field `objname` (`"fg"` or `"bg"`) on JSON object `entry`:

```text
entry.has_array(objname)?
  yes → g_array = entry[objname]
        g_array.test_int()?          // true if first element is a number
          yes → ROTATION ARRAY branch
        else g_array.test_object()?  // true if first element is an object
          yes → WEIGHTED VARIANTS branch
        else → nothing added (empty or unrecognized array)
  else entry.has_int(objname) && entry.get_int(objname) >= 0?
          yes → SINGLE INT branch
        else → nothing added
```

`test_int` / `test_object` inspect **only the first array element** to choose the branch.

---

## Branch 1 — Single integer

```json
"fg": 35
```

```text
vs.add([json + sprite_id_offset], weight=1)
```

Omitted field, `null`, negative int, or non-int/non-array → list stays empty.

---

## Branch 2 — Integer array (rotations)

```json
"fg": [2918, 2919, 2918, 2919]
```

All elements read as integers. One variant, weight `1`:

```text
v = []
for each json_int in array:
    id = json_int + sprite_id_offset
    if id >= 0: v.push_back(id)
vs.add(v, 1)
```

| Property | Behavior |
| --- | --- |
| Length after parse | **No** 1/2/4 validation in this branch |
| Typical lengths | 1 (static), 2 (flip), 4 (directional) — draw code expects these with `rotates` |
| Mixed array | If first element is int, branch runs; non-numeric later elements → JSON read error |
| Empty array | Neither branch matches → nothing added |

---

## Branch 3 — Object array (weighted variants)

```json
"fg": [
  { "weight": 50, "sprite": 640 },
  { "weight": 1, "sprite": [100, 101, 100, 101] }
]
```

One variant per object:

```text
for each vo in array:
    weight = vo.get_int("weight")
    if weight < 0 → throw_error("Invalid weight for sprite variation (<0)")

    v = []
    if vo.has_int("sprite"):
        id = vo.sprite + sprite_id_offset
        if id >= 0: v.push_back(id)
    else if vo.has_array("sprite"):
        for each json_int in vo.sprite:
            id = json_int + sprite_id_offset
            if id >= 0: v.push_back(id)

    if v.size() not in {1, 2, 4}:
        throw_error("Invalid number of sprites (not 1, 2, or 4)")

    vs.add(v, weight)
```

| Property | Behavior |
| --- | --- |
| `weight` | Required per object; negative → **fatal JSON error** |
| `weight == 0` | Variant still stored (contributes nothing to `precalc` pick table) |
| `sprite` omitted / all negative | `v` empty → size check fails → **fatal JSON error** |
| `sprite` int vs array | Same rotation semantics as branch 2 after offset |
| Mixed array | If first element is object, branch runs |

---

## Rotation count rules (summary)

| JSON shape | Length validation |
| --- | --- |
| Int array (`[a, b, …]`) | None at parse time |
| Weighted `sprite` int | Always 1 after parse |
| Weighted `sprite` array | Must be exactly **1, 2, or 4** after parse |

Draw code uses list length with `tile_type.rotates` to pick a frame by facing (unit 08).

---

## Mask lists (same parser)

Tint masks call `load_tile_spritelists` on the mask object. After both sprite and mask lists
load, `load_tile` runs `ensure_mask`:

```text
if mask list non-empty:
    for each (mask_variant, sprite_variant) pair:
        if weights differ OR rotation counts differ:
            debugmsg warning; clear mask list
if mask list empty:
    for each sprite variant with rotation list L:
        mask.add(vector of TILESET_NO_MASK × |L|, same weight)
```

Mask entries must mirror sprite **variant count**, **weights**, and **rotation lengths**.
Mismatch → warning, masks rebuilt as “no mask” placeholders.

---

## Post-load cleanup (`process_variations_after_loading`)

Runs once per `fg` / `bg` list after all sheets and mods finish (not inside
`load_tile_spritelists`):

```text
for each variant v:
    remove indices where id < 0 OR id >= offset   // offset = final sprite count

remove variants with empty obj

vs.precalc()
```

| Stage | What removes bad indices |
| --- | --- |
| Parse time | Negative **json** values before offset (single int branch); negative **stored** ids not pushed |
| Post-load | `id >= offset` (out of range for uploaded textures); empty variants |

Tiles with both `fg` and `bg` empty after cleanup are dropped (unit 09).

---

## Port guidance

| Goal | Approach |
| --- | --- |
| **BN parity** | Implement all three branches + offset rules + post-load prune + `precalc` |
| **Minimal** | Support single int + int rotation array only; skip weighted variants or treat as error |
| **No random variants** | Store one variant per list; ignore weights at draw |
| **Single-sheet port** | `sprite_id_offset = 0` always; skip mod merge |

Equivalent pseudocode:

```text
function load_tile_spritelists(entry, vs, field_name, sprite_id_offset):
    if not entry.has(field_name): return

    if entry[field_name] is array:
        arr = entry[field_name]
        if arr[0] is number:
            ids = [n + sprite_id_offset for n in arr if n + sprite_id_offset >= 0]
            vs.add(ids, 1)
        elif arr[0] is object:
            for vo in arr:
                w = vo.weight
                if w < 0: error
                ids = resolve_sprite(vo.sprite, sprite_id_offset)
                if length(ids) not in (1, 2, 4): error
                vs.add(ids, w)
    elif entry[field_name] is int and entry[field_name] >= 0:
        vs.add([entry[field_name] + sprite_id_offset], 1)
```

---

## BN source reference

| Concern | Location |
| --- | --- |
| Parser | `src/cata_tiles.cpp` — `load_tile_spritelists` |
| Caller | `src/cata_tiles.cpp` — `load_tile` |
| Post-prune + precalc | `src/cata_tiles.cpp` — `process_variations_after_loading` |
| Output type | `src/cata_tiles.h` — `tile_type::sprite_list` |
| Weighted list | `src/weighted_list.h` — `weighted_int_list` |
| Offset assignment | `src/cata_tiles.cpp` — `tileset_loader::load` (mod loop) |
| JSON schema | [04c](./04c-tile-entries.md) |

---

## Inputs

- JSON object (tile entry or mask entry)
- Field name `"fg"` or `"bg"`
- `sprite_id_offset` (0 for base, global count before current mod)
- Empty output list to append into

## Outputs

- `weighted_int_list<std::vector<int>>` appended with zero or more variants
- Indices are global sprite table positions (before post-load prune)

## Failure modes

| Condition | Behavior |
| --- | --- |
| Negative `weight` in variant object | JSON `throw_error` |
| Weighted `sprite` yields 0, 3, or >4 frames | JSON `throw_error` |
| Negative single `fg`/`bg` int | Field ignored (empty list) |
| Unrecognized array (empty, mixed type at [0]) | Silent — nothing added |
| Stored index ≥ final `offset` | Stripped at post-load |
| Mask weight/count mismatch | Warning; mask reset to `TILESET_NO_MASK` fill |

## Verification

A correct port should demonstrate:

1. `"fg": 10` with `sprite_id_offset=100` → one variant `[110]`, weight 1
2. `"fg": [-1]` → empty list (negative not stored)
3. `"fg": [1, 2, 3, 4]` → one variant, four indices, no length error
4. Weighted entry with `sprite: [1, 2]` → accepted; `[1, 2, 3]` → error
5. `weight: -1` → error
6. Mod entry `fg: 0` with `sprite_id_offset=150` → `[150]`; texture upload for that mod must include index 150
7. Post-load: variant `[9999]` when only 100 sprites exist → variant removed or emptied then dropped
8. `precalc()` after post-load: weights `[3,1]` → nine slots mapping to variant indices
9. Mask branch: sprite has two weighted variants; mask with one → warning and auto-fill masks
