# 05a — Terrain config schema

JSON shape for `type: terrain` objects. Parsing: [07a](./07a-parse-terrain.md). Shared fields
with furniture: `map_data_common_t` (loaded first in `ter_t::load`).

---

## Purpose

Document every terrain JSON field BN reads, marking **v1** (map editor palette) vs **deferred**
(simulation). Authoritative reference: `docs/en/mod/json/reference/json_info.md`.

---

## File location and envelope

```text
data/json/furniture_and_terrain/terrain-*.json
```

Almost always a **JSON array** of objects. Each object:

```json
{
  "type": "terrain",
  "id": "t_dirt",
  ...
}
```

---

## Identity and factory

| Field | Type | Required | v1 | Notes |
| --- | --- | --- | --- | --- |
| `type` | string | yes | yes | Must be `"terrain"` |
| `id` | string or array | yes | string only v1 | `generic_factory` supports id array — same data, multiple ids |
| `abstract` | string | no | defer | Template id; not inserted into registry |
| `copy-from` | string | no | defer | Inherit from abstract; may defer load |

### Id conventions

- Prefix `t_` — convention (`t_dirt`, `t_floor`, `t_null`)
- `t_null` — BN inserts null terrain on first load if factory empty
- Mods may define new `t_*` or override core ids (later mod wins)

---

## `map_data_common_t` fields (terrain + furniture)

Loaded via `map_data_common_t::load` before terrain-specific fields.

### Display / ASCII

| Field | Type | Required | v1 | Notes |
| --- | --- | --- | --- | --- |
| `name` | string | yes (terrain) | yes | Short name; translated in BN |
| `description` | string | yes | optional | Long description |
| `symbol` | string or seasonal array | yes* | yes | 1 char, or `LINE_XOXO` / `LINE_OXOX` |
| `color` | string or seasonal array | one of color/bgcolor | yes | Named color → `nc_color` |
| `bgcolor` | string or seasonal array | one of color/bgcolor | defer | Mutually exclusive with `color` |
| `looks_like` | string | no | yes | Gfx/game fallback id |
| `copy-from` | string | no | defer | If `looks_like` empty, sets looks_like from copy-from in `load_symbol` |

\* `load_symbol` errors if neither `color` nor `bgcolor` present.

### Flags and behavior

| Field | Type | v1 | Notes |
| --- | --- | --- | --- |
| `flags` | string[] | yes (store raw) | Each flag → `set_flag` → bitflags (`TRANSPARENT`, `FLAT`, `WALL`, …) |
| `default_vars` | object | defer | Map memory variables |
| `message` | string | defer | Examine message |
| `prompt` | string | defer | Construction prompt |
| `examine_action` | string | defer | `"none"`, action id, or `lua:...` |
| `light_color` | color | defer | |
| `curtain_transform` | string | defer | |

### Harvest

| Field | Type | v1 | Notes |
| --- | --- | --- | --- |
| `harvest_by_season` | array | defer | Inline harvest lists per season |

---

## Terrain-specific (`ter_t::load`)

| Field | Type | Required | v1 | Notes |
| --- | --- | --- | --- | --- |
| `move_cost` | int | **yes** | yes | 0 = impassable; BN check: terrain should be 0 or ≥2 |
| `coverage` | int | no | defer | Cover value |
| `max_volume` | volume | no | defer | |
| `trap` | string | no | defer | Trap id string → resolved after load |
| `light_emitted` | int | no | defer | |
| `heat_radiation` | int | no | defer | Fire field intensity units |
| `connects_to` | string | no | defer | Connection group name (`WALL`, …) |
| `open` | ter id | no | defer | Open action result |
| `close` | ter id | no | defer | Close action result |
| `transforms_into` | ter id | no | defer | |
| `roof` | ter id | no | defer | Floor on z+1 |
| `lockpick_result` | ter id | no | defer | |
| `lockpick_message` | string | no | defer | |
| `nail_pull_result` | ter id | no | defer | |
| `nail_pull_items` | [nails, planks] | no | defer | |
| `fill_result` | ter id | no | defer | |
| `fill_minutes` | int | no | defer | Default 15 |
| `oxytorch` | object | no | defer | Activity data |
| `boltcut` | object | no | defer | |
| `hacksaw` | object | no | defer | |
| `bash` | object | no | defer | `map_bash_info` — sound, str_min/max, ter_set, items |
| `digging_results` | object | no | defer | Dig action |
| `deconstruct` | object | no | defer | |
| `pry` | object | no | defer | Pry tool results |

### `connects_to` vs flags

```text
connect_group = TERCONN_NONE
apply flags (may set WALL connect via TFLAG_WALL)
if JSON has "connects_to":
    set_connects(string)    // overrides flag-derived group
```

---

## Example (core data)

```json
{
  "type": "terrain",
  "id": "t_dirt",
  "name": "dirt",
  "description": "It's dirt...",
  "symbol": ".",
  "color": "brown",
  "move_cost": 2,
  "flags": [ "TRANSPARENT", "FLAT", "PLOWABLE", "VEH_TREAT_AS_BASH_BELOW" ],
  "digging_results": { "digging_min": 1, "result_ter": "t_pit_shallow", "num_minutes": 60 },
  "bash": { "sound": "thump", "ter_set": "t_pit_shallow", "str_min": 50, "str_max": 100 }
}
```

v1 palette needs: `id`, `name`, `symbol`, `color`, `move_cost`, `flags`, `looks_like` (if present).

---

## Common flag strings (reference)

Stored as strings in v1; BN maps to `ter_bitflags` via `ter_bitflags_map`:

| Flag | Typical meaning |
| --- | --- |
| `TRANSPARENT` | Does not block light |
| `FLAT` | Flat surface |
| `WALL` | Wall; may imply connect group |
| `DIGGABLE` | Can dig |
| `ROUGH` | Rough terrain |
| `NO_SCENT` | Scent blocking |
| `MOUNTABLE` | Vehicle mountable |
| `INDOORS` | Indoor tile |

Full list: BN `json_flag` / terrain flag docs. v1 does not need bitflag translation for editor.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Example files | `data/json/furniture_and_terrain/terrain-*.json` |
| Common load | `src/mapdata.cpp` — `map_data_common_t::load`, `load_symbol` |
| Terrain load | `src/mapdata.cpp` — `ter_t::load` |
| Struct | `src/mapdata.h` — `ter_t`, `map_data_common_t` |
| Factory | `src/mapdata.cpp` — `terrain_data`, `load_terrain` |
| Author docs | `docs/en/mod/json/reference/json_info.md` |

---

## Inputs

- `JsonObject` with `type: terrain`

## Outputs

- Schema validation result; field set for parser

## Failure modes

| Condition | BN |
| --- | --- |
| Both `color` and `bgcolor` | JSON error |
| Missing `color`/`bgcolor` | JSON error in `load_symbol` |
| Symbol not 1 char (and not LINE_*) | JSON error |
| `move_cost` == 1 | `check()` debugmsg |
| Missing `name` or `move_cost` | Load error (mandatory) |

## Verification

1. Catalog all v1 fields present on `t_dirt` in core JSON
2. Entry with only v1 fields parses without deferred-field errors
3. Document count of terrain entries with `copy-from` in core (inheritance scope for v2)
4. `looks_like` preserved when present (`t_mud` → `t_dirt`)
