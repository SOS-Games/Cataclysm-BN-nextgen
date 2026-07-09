# 03 — Overmap terrain (oter)

OMT type definitions — the vocabulary of overmap cells. Every `map_layer` cell stores an
`oter_id` referencing this registry.

---

## JSON location

```
data/json/overmap/overmap_terrain/
  overmap_terrain.json
  overmap_terrain_river.json
  … (split files by theme)
```

`"type": "overmap_terrain"`. Mods add/override; later mod wins on duplicate ids.

---

## Core types

| C++ type | Role |
| --- | --- |
| `oter_type_t` | Static metadata: flags, mapgen weights, vision, line drawing |
| `oter_t` | Instance with rotation suffix (`_north`, `_east`, …) |
| `oter_id` | Runtime handle into global map |

Registry: `overmap_terrains` namespace in `src/overmap.cpp` (~964+). Types in `src/omdata.h`.

---

## JSON fields

| Field | Meaning |
| --- | --- |
| `id` | Canonical type id (`road`, `house_09`) |
| `name` | Display name |
| `sym` / `color` | ASCII overmap glyph |
| `see_cost` | Overmap vision / path cost |
| `flags` | Array of strings → `oter_flags` (see table below) |
| `mapgen` | Weighted list: json id, builtin key, or array of alternatives |
| `mapgen_offset` | `{x,y,z}` submap offset for multi-tile buildings |
| `mapgen_params` | Key-value passed to mapgen |
| `looks_like` | Tile report similarity |
| `locations` | `overmap_location` tags for specials / connections |
| `extend` / `copy-from` | Mod inheritance |

Rotatable terrains: base id + direction suffix. `oter_t::get_mapgen_id()` resolves weighted pick.

---

## `oter_flags` (complete enum)

**Source:** `src/omdata.h` **119–156** · JSON names in `src/overmap.h` `oter_flags_map`.

| JSON flag | C++ enum | Layout / gameplay |
| --- | --- | --- |
| `KNOWN_DOWN` | `known_down` | Z-down hint |
| `KNOWN_UP` | `known_up` | Z-up hint |
| `NO_ROTATE` | `no_rotate` | Single orientation |
| `IGNORE_ROTATION_FOR_ADJACENCY` | `ignore_rotation_for_adjacency` | Adjacency tests |
| `RIVER` | `river_tile` | Hydrology helpers |
| `SIDEWALK` | `has_sidewalk` | Urban |
| `LINEAR` | `line_drawing` | 8-way road/rail topology |
| `SUBWAY` | `subway_connection` | Underground network |
| `LAKE` | `lake` | Lake flood / polish |
| `LAKE_SHORE` | `lake_shore` | Shore detection |
| `GENERIC_LOOT` | `generic_loot` | Loot maps |
| `RISK_HIGH` / `RISK_LOW` | risk_* | Mission scoring |
| `SOURCE_*` | `source_*` | Item source categories (many) |
| `IS_BRIDGE` | `is_bridge` | Bridge detection |

Linear terrains use **line bitmask** (`om_lines`) for N/E/S/W segments — connection carving
sets segments via `get_linear(line)`.

---

## Builtin vs JSON mapgen

| `mapgen` entry | Engine |
| --- | --- |
| Builtin key (`forest`, `field`, …) | `mapgen_functions.cpp` |
| Json id | `mapgen_function_json` → `data/json/mapgen/` |
| Lua id | Lua mapgen when enabled |

`map::draw_map` fallback: hardcoded prefixes `office`, `temple`, `mine` (~4387).

---

## Connection compatibility

Linear OMT ids map to `overmap_connection` via:

- Explicit placement (`build_connection`)
- `overmap_connections::guess_for(oter_id)` — reverse lookup

Subtype selection uses `overmap_location` tags on current terrain — see [06-connections.md](./06-connections.md).

---

## `overmap_location` tags (connection-relevant)

Defined in `data/json/overmap/special_locations.json`. Examples used in
`overmap_connections.json`:

`field`, `road`, `forest_without_trail`, `forest_trail`, `swamp`, `water`, `bridge`, `dam`,
`subterranean`, `subterranean_subway`, `wilderness`, `railroad`.

---

## Relationship to nextgen

| BN | Nextgen |
| --- | --- |
| `overmap_terrains::finalize` | `OvermapTerrainLoader` (W1 ✓) |
| Weighted mapgen | `MapgenPicker` (W3) |
| LINEAR flags | `OvermapConnectionResolver` |

---

## Inputs

- `overmap_terrain` JSON from core + mods

## Outputs

- Global `oter_id` table + `oter_type_t` metadata
- Mapgen weight lists per type

## Failure modes

- Duplicate ids — last mod wins
- Missing mapgen target — `check_consistency` error
- Unknown id at runtime — `debugmsg`, fallback terrain

## Verification

1. `--check-mods` / `overmap_terrains::check_consistency` clean.
2. `road` type has `LINEAR` flag; instances include `road_ns`, `road_ne`, etc.
3. `house_09` mapgen weights sum > 0.

**BN anchors:** `src/omdata.h`, `src/overmap.cpp`, `data/json/overmap/overmap_terrain/`.
