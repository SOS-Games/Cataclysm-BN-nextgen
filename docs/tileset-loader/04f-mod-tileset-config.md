# 04f — Mod tileset (config)

Schema and lifecycle for `mod_tileset` JSON: extra sprite sheets and tile definitions merged
into a **base** tileset at load time. Reuses the same inner schema as `tile_config.json`
(units 04b–04c, 04e).

---

## Purpose

Mods ship additional PNGs and tile defs without editing the base pack’s `tile_config.json`.
At game-data load, mod files are **registered** (compatibility metadata only). During full
tileset load, compatible mods are **merged** into the active tileset’s sprite space and
`tile_ids` map.

---

## Two-phase lifecycle

```text
Phase 1 — Registration (game-data load)
    JSON type "mod_tileset" discovered
    → append to all_mod_tilesets (path + compatibility list)

Phase 2 — Merge (tileset load, after base config)
    for each registered mod in order:
        if compatible with active tileset_id:
            load_internal(mod_object, mod_base_path, ...)
            sprite_id_offset = current global sprite count
```

Registration must finish **before** finalize triggers full tileset load (unit 01).

On game-data unload, `all_mod_tilesets` is cleared.

---

## File formats

### Single object

```json
{
  "type": "mod_tileset",
  "compatibility": ["UltimateCataclysm", "UNDEAD_PEOPLE_BASE"],
  "tiles-new": [ ... ]
}
```

### Array of objects

```json
[
  { "type": "mod_tileset", "compatibility": ["..."], "tiles-new": [ ... ] },
  { "type": "mod_tileset", "compatibility": ["..."], "tiles-new": [ ... ] }
]
```

Each object with `"type": "mod_tileset"` produces one registration entry. Multiple objects
in the **same file** are distinguished by `num_in_file` (1st, 2nd, …) matching registration
order for that `full_path`.

---

## Required fields

| Field | Type | Purpose |
| --- | --- | --- |
| `type` | string | Must be `"mod_tileset"` |

## Required for merge (phase 2)

At least one of:

| Field | Purpose |
| --- | --- |
| `tiles-new` | Add sprite sheets + tile defs |
| `tiles` | Legacy single-sheet additions (rare in mods) |
| `tints` / `tint_pairs` / `overlay_ordering` | Metadata-only patches (no new images) |

`compatibility` is required for registration (read as string array). Empty array → never
compatible.

---

## `compatibility`

```json
"compatibility": ["UNDEAD_PEOPLE_BASE", "UltimateCataclysm"]
```

| Rule | Behavior |
| --- | --- |
| Match target | **Exact** string match against active tileset id (`NAME` from base `tileset.txt`) |
| Multiple entries | Mod applies if **any** entry matches |
| No match | Mod skipped (info log); no error |

Ids must match registry names, not `VIEW` display labels.

---

## Reused config sections

A mod object may contain any section from [04b](./04b-tile-config-structure.md):

| Section | Notes |
| --- | --- |
| `tiles-new` | Primary path; `file` paths relative to **mod JSON directory** (`base_path`) |
| `tiles` | Legacy; uses `img_path` from base manifest (usually irrelevant) |
| `tints`, `tint_pairs` | Merged into tileset |
| `overlay_ordering` | Merged into tileset overlay order table |
| `state-modifiers` | On sheet entries; can override base groups (unit 04e) |

Mod objects **do not** include `tile_info`. Sheet dimensions use per-sheet
`sprite_width` / `sprite_height` or inherited defaults from the **already-loaded** base
tileset (`ts.tile_width`, `ts.tile_height`).

Tile entry grammar is identical to [04c](./04c-tile-entries.md).

---

## Path resolution

| Path | Value |
| --- | --- |
| `base_path` | Directory containing the mod JSON file |
| `full_path` | Path to the mod JSON file |
| Sheet image | `base_path + "/" + tiles-new[].file` |

Example: `data/json/external_tileset/cats.json` with `"file": "external_tileset/cats.png"`
→ image at `data/json/external_tileset/external_tileset/cats.png` if `base_path` is
`data/json/external_tileset/`. (Authors usually place PNGs beside or under that folder.)

---

## Sprite indices in mod JSON

Mod tile entries use **sheet-local** indices (0, 1, 2, …) per mod PNG.

At merge time:

```text
sprite_id_offset = offset    // total sprites loaded so far (base + prior mods)
stored_index = json_index + sprite_id_offset
```

Each compatible mod runs `load_internal` with `sprite_id_offset` set to the current
`offset` before that mod’s sheets load. Base tileset sheets must load first.

---

## Override semantics

| Data | Rule |
| --- | --- |
| Tile ids | Later load wins (`create_tile_type` overwrites) |
| Mod order | Order of entries in `all_mod_tilesets` (= registration order) |
| State modifier groups | Same `(id, whitelist, blacklist)` → later replaces earlier |
| `tints` | Same tint id → later overwrites |

Later mods in the active mod list can patch earlier mods and the base pack.

---

## Registration algorithm (phase 1)

```text
on load_mod_tileset(jsobj, base_path, full_path):
    allow_omitted_members on jsobj

    num_in_file = 1 + count(existing registrations with same full_path)

    append mod_tileset(base_path, full_path, num_in_file)
    for each string in jsobj.compatibility:
        append to entry.compatibility
```

Only `compatibility` is read at registration; other fields are deferred to phase 2.

---

## Merge algorithm (phase 2)

```text
after load_internal(base_tile_config):

for mts in all_mod_tilesets:
    if not mts.is_compatible(active_tileset_id):
        continue

    sprite_id_offset = offset
    tileset_root = mts.base_path
    open mts.full_path as JSON

    if root is array:
        n = 1
        for obj in array:
            if obj.type == "mod_tileset" and n == mts.num_in_file:
                load_internal(obj, tileset_root, base_img_path, ...)
                break
            if obj.type == "mod_tileset":
                n++
    else:
        load_internal(root_object, tileset_root, base_img_path, ...)

// then global post-process (prune empty tiles, etc.)
```

Non-`mod_tileset` objects in arrays are skipped with `allow_omitted_members`.

---

## Examples

### Simple mod sheet

```json
{
  "type": "mod_tileset",
  "compatibility": ["UltimateCataclysm"],
  "tiles-new": [
    {
      "file": "MMA_normal.png",
      "sprite_width": 32,
      "sprite_height": 32,
      "tiles": [
        { "id": "manual_mma_panzer", "fg": 1, "rotates": true }
      ]
    }
  ]
}
```

### Tints-only patch

```json
{
  "type": "mod_tileset",
  "compatibility": ["UndeadPeopleTileset"],
  "tints": [
    { "id": "hair_blond", "fg": "#91631f", "blend_mode": "multiply" }
  ]
}
```

No `tiles-new` → no images; tints merged via `load_internal`.

---

## Relationship to other units

| Topic | Unit |
| --- | --- |
| Inner `tiles-new` / `tiles` schema | [04b](./04b-tile-config-structure.md) |
| Tile entries | [04c](./04c-tile-entries.md) |
| State modifiers | [04e](./04e-state-modifiers-config.md) |
| Full pipeline | [05](./05-load-pipeline.md) |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Registration | `src/mod_tileset.cpp` — `load_mod_tileset` |
| Registry | `src/mod_tileset.h` — `all_mod_tilesets`, `mod_tileset` |
| Merge loop | `src/cata_tiles.cpp` — `tileset_loader::load` |
| JSON type registration | `src/init.cpp` — `DynamicDataLoader` |
| Clear on unload | `src/init.cpp` — `reset_mod_tileset` |
| Author docs | `docs/en/mod/json/reference/graphics/mod_tileset.md` |

---

## Inputs

- `all_mod_tilesets` from phase 1
- Active `tileset_id` from options
- Base tileset already loaded (`offset` = sprite count so far)
- Mod JSON at `full_path`

## Outputs

- Additional sprites appended to global index space
- New or overridden `tile_ids` entries
- Merged tints, overlay order, state modifiers

## Failure modes

| Condition | Behavior |
| --- | --- |
| Incompatible id | Skip mod (no error) |
| Mod JSON not readable | Throw |
| Missing image for required sheet | Throw (unless empty-tiles ASCII skip — N/A for sprites-only port) |
| `type` not `mod_tileset` in array | Object skipped |
| `num_in_file` mismatch | Mod content not loaded (silent skip of that entry) |
| Empty `compatibility` | Never merged |

## Verification

A correct port should demonstrate:

1. Register mod with `compatibility: ["retrodays"]` → merged only when `TILES=retrodays`
2. Mod `fg: 0` maps to global index `offset` at merge time
3. Mod tile id matching base id replaces base definition
4. Two `mod_tileset` objects in one file → two registrations with `num_in_file` 1 and 2
5. Registration cleared on data unload
6. Image path resolves under mod `base_path`, not base tileset root
