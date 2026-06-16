# 05b — Furniture config schema

JSON shape for `type: furniture` objects. Parsing: [07b](./07b-parse-furniture.md). Shares
`map_data_common_t` with terrain ([05a](./05a-terrain-config.md)).

---

## Purpose

Document furniture JSON fields with **v1** vs **deferred** tiers. Furniture uses
**`move_cost_mod`** (not `move_cost`) and **`required_str`** as mandatory terrain differs.

---

## File location

```text
data/json/furniture_and_terrain/furniture-*.json
```

JSON **array** of objects with `"type": "furniture"`.

---

## Identity

Same `id` / `abstract` / `copy-from` rules as terrain ([05a](./05a-terrain-config.md)#identity-and-factory).

- Id convention: `f_*` (`f_chair`, `f_null`)
- `f_null` inserted on first furniture load if factory empty

---

## `map_data_common_t` fields

Same as terrain: `name`, `description`, `symbol`, `color`/`bgcolor`, `flags`, `looks_like`,
`examine_action`, `harvest_by_season`, etc.

**Note:** `name` is mandatory for furniture in `furn_t::load` (via `map_data_common_t` +
furniture-specific fields).

---

## Furniture-specific (`furn_t::load`)

| Field | Type | Required | v1 | Notes |
| --- | --- | --- | --- | --- |
| `move_cost_mod` | int | **yes** | yes | Movement modifier on terrain (not absolute cost) |
| `required_str` | int | **yes** | defer* | Strength to move; negative = not movable |
| `coverage` | int | no | defer | |
| `comfort` | int | no | defer | Sleep comfort |
| `floor_bedding_warmth` | int | no | defer | |
| `max_volume` | volume | no | defer | Storage |
| `deployed_item` | item id | no | defer | Item when picked up |
| `light_emitted` | int | no | defer | |
| `connects_to` | string | no | defer | Same as terrain |
| `open` / `close` | furn id | no | defer | |
| `lockpick_result` | furn id | no | defer | |
| `transforms_into` | furn id | no | defer | |
| `oxytorch` / `boltcut` / `hacksaw` | object | no | defer | |
| `bash` | object | no | defer | |
| `deconstruct` | object | no | defer | Default `required` true for furniture |
| `pry` | object | no | defer | |
| `workbench` | object | no | defer | Crafting |
| `plant_data` | object | no | defer | |
| `emissions` | emit id[] | no | defer | Requires `EMITTER` flag |
| `provides_liquids` | ... | no | defer | |
| `keg_capacity` | volume | no | defer | |
| `fluid_grid` | object | no | defer | Complex fluid system |
| `active` | object | no | defer | Active furniture state |
| `crafting_pseudo_item` | ... | no | defer | |
| `surgery_skill_multiplier` | float | no | defer | |

\* v1 palette may omit `required_str` defaulting to `-1` (immovable) if missing in JSON —
BN treats as mandatory; verify parser strictness.

---

## Layer semantics

```text
map cell:
  terrain_id   → always (e.g. t_floor)
  furniture_id → optional overlay (e.g. f_chair)
```

Gfx: separate `tile_config.json` entries for `f_*` ids. Editor v1 may paint terrain only;
furniture layer in [map file format](../map-editor/04-map-file-format.md).

---

## Example

```json
{
  "type": "furniture",
  "id": "f_rubble",
  "name": "rubble",
  "symbol": "#",
  "color": "brown",
  "move_cost_mod": 2,
  "required_str": 10,
  "flags": [ "TRANSPARENT", "NOITEM", "REDUCE_SCENT", "MOUNTABLE", "SHORT" ]
}
```

---

## BN source reference

| Concern | Location |
| --- | --- |
| Example files | `data/json/furniture_and_terrain/furniture-*.json` |
| Load | `src/mapdata.cpp` — `furn_t::load`, `load_furniture` |
| Struct | `src/mapdata.h` — `furn_t` |
| Check | `src/mapdata.cpp` — `furn_t::check` |

---

## Inputs

- `JsonObject` with `type: furniture`

## Outputs

- Validated field set

## Failure modes

Same common failures as [05a](./05a-terrain-config.md). Furniture-specific:

| Condition | BN |
| --- | --- |
| `EMITTER` flag without `emissions` | `check()` debugmsg |
| Invalid fluid_grid config | `check()` / load debugmsg |

## Verification

1. Common `f_*` id in core data matches v1 field expectations
2. `move_cost_mod` parsed separately from terrain `move_cost` (no field confusion)
3. `f_null` handling documented for empty furniture layer
