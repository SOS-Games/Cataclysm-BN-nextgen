# 04e — State modifiers (config)

Schema for the `state-modifiers` array on a `tiles-new` sheet entry (unit 04b). These define
UV warp images applied at **draw time** to character sprites based on game state. This unit
covers loader-facing JSON and merge rules; UV math at render is out of scope.

Parsing algorithm: [07d](./07d-state-modifiers-parsing.md).

---

## Purpose

State modifiers let tilesets adjust character appearance (crouch, downed, movement, etc.)
without separate artwork per state. Each modifier is a sprite whose red/green channels encode
UV displacement or remapping.

The loader reads config, resolves sprite indices, and stores `state_modifier_group` records
on the tileset. Whether modifiers run is a runtime option in BN (`STATE_MODIFIERS`); the
loader always parses them when present.

---

## Placement

`state-modifiers` lives **inside** a `tiles-new` object, alongside `file` and `tiles`:

```json
{
  "file": "uv-tiles.png",
  "tiles": [],
  "state-modifiers": [
    {
      "id": "movement_mode",
      "override": false,
      "use_offset": true,
      "tiles": [
        { "id": "walk", "fg": null },
        { "id": "crouch", "fg": 1 },
        { "id": "run", "fg": 2 }
      ]
    }
  ]
}
```

Loaded after the sheet image for that entry (so sprite indices resolve against the current
global sprite space). See load order in [04b](./04b-tile-config-structure.md).

---

## Group object

| Field | Type | Default | Purpose |
| --- | --- | --- | --- |
| `id` | string | — | Group identifier (must match game-supported group) |
| `override` | bool | `false` | When active state has non-null modifier, skip lower-priority groups |
| `use_offset` | bool | `true` | `true` = offset UV mode; `false` = normalized UV mode |
| `tiles` | array | **required** | Per-state modifier mappings |
| `whitelist` | string[] | `[]` | Only apply to overlays whose id matches prefix (after `overlay_`) |
| `blacklist` | string[] | `[]` | Never apply to matching overlay prefixes |

Missing `tiles` array → **error**.

### Supported group ids (game contract)

| `id` | State ids in `tiles` | Meaning |
| --- | --- | --- |
| `movement_mode` | `walk`, `run`, `crouch` | Movement stance |
| `downed` | `normal`, `downed` | Knocked down |
| `lying_down` | `normal`, `lying` | Prone / sleeping |
| `activity` | `none`, activity ids (e.g. `ACT_CRAFT`) | Current activity pose |
| `body_size` | `tiny`, `small`, `medium`, `large`, `huge` | Character size |

The loader does not validate state id strings against this table; invalid ids simply never
match at runtime.

---

## Per-state tile entry (`tiles[]`)

| Field | Type | Default | Purpose |
| --- | --- | --- | --- |
| `id` | string | — | State name within the group |
| `fg` | int or `null` | identity | Sprite index for UV modifier image |
| `offset_x` | int | sheet `sprite_offset_x` | X draw offset for oversized modifiers |
| `offset_y` | int | sheet `sprite_offset_y` | Y draw offset |

### `fg` values

| JSON | Stored modifier |
| --- | --- |
| `null` | Identity — no UV change for this state |
| non-negative int | Global sprite index after adding `sprite_id_offset` |
| negative int | Treated as identity (`nullopt`) |
| omitted | Identity |

Modifier sprites live on the same sheet(s) as other entries; indices follow the same local vs
global rules as tile entries (unit 04c).

---

## UV modes (authoring reference)

Draw-time behavior; included so config authors understand `use_offset`:

**Offset mode** (`use_offset: true`, default):

- Neutral pixel ≈ RGB (127, 127, …)
- R/G channels encode displacement from neutral
- Multiple modifiers stack additively

**Normalized mode** (`use_offset: false`):

- R/G encode absolute UV coordinates across tile bounds
- Modifiers chain by re-sampling

---

## Overlay filters

`whitelist` / `blacklist` entries are **prefix strings** matched against overlay tile ids
(e.g. `wielded_`, `worn_`, `mutation_`).

| Filter setup | Applies to base character sprite? |
| --- | --- |
| `whitelist` only | No (base has no overlay prefix) |
| `blacklist` only or none | Yes |

Multiple groups may share the same `id` if **whitelist/blacklist sets differ**. Loader treats
`(group_id, whitelist, blacklist)` as the unique key.

Filters are sorted and deduplicated on load.

---

## Priority and `override`

Groups are stored in **array order** (index 0 = highest priority). At draw time:

- Process groups in order
- If a group has `override: true` and the active state’s modifier is non-identity, skip all
  lower-priority groups

Loader only stores order and flags; priority logic runs in the renderer.

---

## Merge and override rules

### Within one load pass

When a new group matches an existing entry with the same `(id, whitelist, blacklist)`:

→ **Replace** the previous group in place.

When `(id, whitelist, blacklist)` is new:

→ **Append** to the group list.

### Base tileset vs mod tileset

Mod tilesets load after the base config (unit 04f). A mod group with the same `id` and
filters as the base **replaces** the base definition. This is how packs patch movement/crouch
UVs without editing the base `tile_config.json`.

Later sheets in the same file can also replace earlier groups if keys match.

---

## Relationship to global warp lists

On the same `tiles-new` entry, `global-warp-whitelist` and `global-warp-blacklist` (unit 04b)
set tileset-wide overlay prefix filters used when a group does not define its own filters.
State modifier groups are independent data; warp lists affect draw-time UV application.

---

## Optional feature

State modifiers are only needed if the port supports BN-style character UV warping. A minimal
sprite-only loader may:

- Ignore `state-modifiers` keys in JSON
- Omit `state_modifiers` from the in-memory tileset

BN tilesets for Undead People and similar rely on this heavily for crouch/run poses.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Parse + merge | `src/cata_tiles.cpp` — `load_state_modifiers` |
| Storage types | `src/cata_tiles.h` — `state_modifier_group`, `state_modifier_tile` |
| Author docs | `docs/en/mod/json/reference/graphics/tileset.md` (State Modifiers) |
| Mod override note | `docs/en/mod/json/reference/graphics/mod_tileset.md` |

---

## Inputs

- `state-modifiers` array on a `tiles-new` entry
- Current `sprite_id_offset` (for index resolution)
- Current sheet `sprite_offset_x` / `sprite_offset_y` (defaults for per-state offsets)
- Existing `state_modifiers` list on tileset (for replacement)

## Outputs

- Updated `tileset.state_modifiers` vector (ordered list of groups)
- Each group: `group_id`, flags, filter lists, map `state_id → { fg_sprite, offset }`

## Failure modes

| Condition | Behavior |
| --- | --- |
| Missing `tiles` on group | Throw |
| Unknown JSON fields | Ignored if loader allows omitted members on parent |
| Invalid `fg` int negative | Stored as identity |
| Duplicate `(id, whitelist, blacklist)` | Later replaces earlier |

## Verification

A correct port should demonstrate:

1. Group with three states stores three entries in `group.tiles`
2. `fg: null` → identity modifier for that state
3. `fg: 5` with `sprite_id_offset = 100` → stored sprite index 105
4. Second group with same `id` and filters replaces first
5. Same `id`, different `whitelist` → two distinct groups coexist
6. `override` and `use_offset` preserved on stored group
