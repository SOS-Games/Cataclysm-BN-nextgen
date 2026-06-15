# 04c — Tile entries

Schema for objects in a `tiles` array (either inside a `tiles-new` sheet entry or at the
root in legacy mode). This is the **contract** for what authors put in JSON; index resolution
algorithms are in [07a](./07a-sprite-lists.md) and [07b](./07b-tile-registration.md).

Parent context: [04b](./04b-tile-config-structure.md).

---

## Purpose

Each tile entry maps one or more **string ids** (game entity names) to foreground/background
sprites, optional masks, and metadata. Multiple entries may share one `tiles` array; later
entries **override** earlier ones for the same id.

---

## Entry object — fields

| Field | Type | Default | Purpose |
| --- | --- | --- | --- |
| `id` | string **or** string[] | — | Lookup key(s); see [id conventions](#id-conventions) |
| `fg` | int, int[], or array | — | Foreground sprite(s); see [fg / bg shapes](#fg--bg-shapes) |
| `bg` | int, int[], or array | — | Background sprite(s); same shapes as `fg` |
| `rotates` | bool | `multitile` value, else `false` | Use directional rotation when drawing |
| `multitile` | bool | `false` | Enable `additional_tiles` subtiles |
| `additional_tiles` | object[] | — | Sub-definitions; requires `multitile: true` |
| `animated` | bool | `false` | Sprite animation hint |
| `height_3d` | int | `0` | 3D height hint for iso rendering |
| `flags` | string[] | `[]` | Game flags on this tile def |
| `default_tint` | color | — | Default tint color for this tile |
| `has_om_transparency` | bool | `false` | Overmap transparency hint |
| `masks` | object[] | — | Parallel mask sprites; see [masks](#masks) |

Entries with no `id` field (or empty id list) are skipped.

`additional_tiles` without `multitile: true` → **error**.

---

## `id` field

### Single or multiple ids

```json
{ "id": "t_dirt", "fg": 35 }
```

```json
{ "id": ["t_dirt", "t_dirtmound"], "fg": 35 }
```

Each id in an array gets an independent copy of the same tile definition (same sprites and
metadata).

### Override semantics

`tile_ids[id]` is overwritten on each load. Within one file, later entries win. Mod tilesets
and later sheets can override base defs.

---

## Id conventions

String ids are opaque to the loader; the game maps them to entities at draw time. Common
patterns:

| Pattern | Example | Typical use |
| --- | --- | --- |
| (none) | `rock`, `jacket` | Items |
| `t_` | `t_dirt` | Terrain |
| `f_` | `f_chair` | Furniture |
| `vp_` | `vp_door` | Vehicle parts |
| `mon_` | `mon_zombie` | Monsters |
| `overlay_mutation_` | `overlay_mutation_GOURMAND` | Mutation overlay |
| `overlay_male_mutation_` / `overlay_female_mutation_` | gender-specific overlays | |
| `overlay_worn_` / `overlay_male_worn_` / `overlay_female_worn_` | worn item overlays | |
| `overlay_wielded_` / … | wielded item overlays | |
| `_season_*` suffix | `t_tree_season_winter` | Seasonal variant |
| `player_female`, `player_male`, `npc_female`, `npc_male` | avatar sprites | |
| `unknown` | fallback when no sprite exists | **recommended** |
| `animation_bullet_{id}` | `animation_bullet_9mm` | Projectile animation |
| `highlight_item` | UI highlight | special |

Seasonal ids: suffix `_season_spring`, `_season_summer`, `_season_autumn`, `_season_winter`
on any base id. Loader registers them in the seasonal index (unit 07b / 08).

---

## `fg` / `bg` shapes

Sprite values are **integer indices** into the tileset’s global sprite space (after load,
each index maps to a region in a sheet texture — unit 06).

### 1. Single integer

```json
"fg": 35
```

One sprite, weight 1. Negative integers are **ignored** (not added).

### 2. Integer array — rotations

All elements must be integers. One weighted variant containing the full list.

```json
"fg": [2918, 2919, 2918, 2919]
```

| Length | Meaning |
| --- | --- |
| 1 | No rotation |
| 2 | Two directions (e.g. left/right) |
| 4 | Four directions (N/E/S/W or pre-rotated quadrants) |

Used with `"rotates": true` for entities that change facing.

### 3. Object array — weighted random variants

Each object:

| Field | Type | Required | Purpose |
| --- | --- | --- | --- |
| `weight` | int ≥ 0 | yes | Relative pick weight; negative → **error** |
| `sprite` | int **or** int[] | yes | One sprite or rotation list |

```json
"fg": [
  { "weight": 50, "sprite": 640 },
  { "weight": 1, "sprite": 3620 },
  { "weight": 1, "sprite": [100, 101, 100, 101] }
]
```

Each variant’s `sprite` array must have length **1, 2, or 4** after parsing — otherwise
**error**.

### Omitted `fg` / `bg`

Omitted or empty lists are allowed. After load, tiles with **both** fg and bg empty are
**removed** (unit 09).

### Sprite index semantics

| Source | Index meaning |
| --- | --- |
| Composed base tileset JSON | Usually **global** indices (compose tool pre-adds sheet base offset) |
| Mod tileset JSON | **Sheet-local** indices; loader adds current sprite count (`sprite_id_offset`) |
| First sheet in uncomposed data | Often 0-based local (coincides with global when `offset` starts at 0) |

Authors using the compose pipeline (`scripts/tileset.ts`) get global indices in output.
Hand-edited configs must keep indices consistent with load order of sheets.

---

## Multitile and `additional_tiles`

When `"multitile": true`:

```json
{
  "id": "t_wall",
  "fg": 2918,
  "bg": 633,
  "rotates": true,
  "multitile": true,
  "additional_tiles": [
    { "id": "center", "fg": 2919 },
    { "id": "corner", "fg": [2924, 2922, 2922, 2923] },
    { "id": "unconnected", "fg": 2235 }
  ]
}
```

### Subtile ids

Each `additional_tiles[]` entry has its own `id` (short name). Stored tile id:

```text
{parent_id}_{sub_id}     e.g.  t_wall_corner
```

Subtile entries use the same `fg`/`bg` shapes as parent entries. Parent inherits
`height_3d`, `default_tint`, `flags` unless subentry overrides `animated`.

### Recognized multitile sub-id names

These sub-ids set `is_multitile_subtile` for connection-style rendering:

`center`, `corner`, `edge`, `t_connection`, `end_piece`, `unconnected`, `open`, `broken`

Other sub-id strings (e.g. `broken` on items) still load but may not get connection behavior.

### `rotates` default

If `rotates` omitted and `multitile` is true → `rotates` defaults to **true**.

Subtiles always load with `rotates: true` in BN.

### `available_subtiles`

Parent tile records the list of sub-id strings from `additional_tiles` for runtime lookup.

---

## Masks

Optional array of mask layers parallel to `fg`/`bg`:

```json
"masks": [
  { "type": "tint", "fg": 100, "bg": 101 }
]
```

| Field | Purpose |
| --- | --- |
| `type` | Only `"tint"` supported; others log warning |
| `fg`, `bg` | Same shapes as tile `fg`/`bg` |

If mask lists are present, they must match sprite list **weights and rotation counts**. On
mismatch → warning and mask cleared, then auto-filled with “no mask” placeholders matching
each sprite variant.

---

## Other metadata

### `flags`

Array of game flag ids attached to the tile definition.

### `default_tint`

Color value (hex or named) applied as default tint for this tile.

### `height_3d`

Integer vertical extent hint for iso / 3D draw ordering.

### `animated`

Boolean; marks tile for animation handling at draw time.

### `has_om_transparency`

Boolean; overmap-specific transparency behavior.

---

## Projectile tile ids

Convention (draw-time `looks_like` is separate):

| Id pattern | Use |
| --- | --- |
| `animation_bullet_{ammo_id}` | Fired ammunition |
| `animation_bullet_{item_id}` | Thrown items |

Typically `"rotates": true`. Fallback ids like `animation_bullet_normal` exist in many packs.

---

## Expansion tileset marker (compose pipeline)

When building from compositing directories, an expansion sheet may use:

```json
{ "id": "some_mod_tile_id", "fg": 0, "rotates": false }
```

`fg: 0` marks the sheet as an expansion placeholder at compose time. Composed `tile_config.json`
in the repo uses renumbered integer indices, not this literal in final output.

---

## Post-load validation (cross-reference)

After all entries load (unit 09):

- Sprite indices ≥ current total sprite count are stripped from variants
- Empty variants removed
- Tiles with no fg and no bg removed
- Missing `unknown` id logs warning

---

## Relationship to other units

| Topic | Unit |
| --- | --- |
| `tiles` array location | [04b](./04b-tile-config-structure.md) |
| Parsing `fg`/`bg` | [07a](./07a-sprite-lists.md) |
| Registration + seasonal index | [07b](./07b-tile-registration.md) |
| In-memory `tile_type` | [08](./08-in-memory-model.md) |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Iterate `tiles` | `src/cata_tiles.cpp` — `load_tilejson_from_file` |
| Per-entry load | `src/cata_tiles.cpp` — `load_tile` |
| `fg`/`bg` shapes | `src/cata_tiles.cpp` — `load_tile_spritelists` |
| Multitile keys | `src/cata_tiles.cpp` — `multitile_keys` |
| Author guide | `docs/en/mod/json/reference/graphics/tileset.md` |

---

## Inputs

- JSON object per tile entry (from `tiles[]`)
- Parent sheet context: `sprite_offset`, `pixelscale`, current `offset` / `sprite_id_offset`
- Prior tile defs in same load (for override behavior)

## Outputs

- `tile_ids` map entries (`tile_type` per string id)
- Multitile subtiles at `{id}_{subid}`
- Seasonal index updates via `create_tile_type`

## Failure modes

| Condition | Behavior |
| --- | --- |
| `additional_tiles` without `multitile` | Throw |
| Negative `weight` in variant | Throw |
| Variant `sprite` length not 1, 2, or 4 | Throw |
| Invalid mask shape | Warning; masks replaced with placeholders |
| Unknown mask `type` | Warning |
| Both fg and bg empty after parse | Tile dropped at end of load |
| Duplicate id in same load | Later definition wins |

## Verification

A correct port should demonstrate:

1. `{ "id": "foo", "fg": 1 }` registers one tile with one sprite
2. `{ "id": ["a","b"], "fg": 1 }` creates two ids with identical sprites
3. Rotation array length 4 preserved as one weighted variant
4. Weighted random entries keep separate weights
5. Multitile creates `parent_sub` ids and `available_subtiles` on parent
6. `multitile` false + `additional_tiles` → error
7. Second entry with same `id` overrides first
8. Mod-local index 0 maps to global `offset` at merge time when `sprite_id_offset` is set
