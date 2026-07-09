# 09 — In-memory model

Key C++ structures holding overmap and submap state after generation or load.

---

## `overmap`

**Header:** `src/overmap.h` · **Implementation:** `src/overmap.cpp`

| Member | Type / role |
| --- | --- |
| `loc` | `point_abs_om` file coordinates |
| `layer` | `std::array<map_layer, OVERMAP_LAYERS>` — index `z + OVERMAP_DEPTH` |
| `cities` | `std::vector<city>` |
| `connections_out` | `unordered_map<overmap_connection_id, vector<tripoint_om_omt>>` |
| `settings` | `const regional_settings *` |
| `overmap_special_placements` | `unordered_map<tripoint_om_omt, overmap_special_id>` |
| `mongroups`, radios, notes | Gameplay / UI |

Optional during generate: `connection_cache` (`overmap_connection_cache`).

### `map_layer`

| Field | Role |
| --- | --- |
| `terrain[i][j]` | `oter_id` — `OMAPX × OMAPY` |
| `visible[i][j]` | Player revealed on overmap UI |
| `explored[i][j]` | Seen at least once |
| `path[i][j]` | Travel plan overlay |

Access: `overmap::ter(tripoint_om_omt)`, `ter_set`.

---

## `oter_id` / `oter_type_t`

- `oter_id` — string handle; `operator->` → `oter_t` instance
- `oter_type_t` — shared static: mapgen, flags, line drawing, names
- Global tables: `overmap_terrains` namespace

Rotation: `oter_t::get_dir()`, `get_linear(size_t line)`.

---

## `submap` / `mapbuffer`

| Type | Role |
| --- | --- |
| `submap` | 12×12 `ter_id` / `furn_id` + entities |
| `mapbuffer` | Cache keyed by absolute submap tripoint |
| Coordinates | `coords` hierarchy: `omt`, `sm` (submap), `ms` (tile) |

One OMT = 2×2 submaps in each horizontal dimension.

Nextgen `MapGrid`: 24×24 directly; terrain/furniture only in preview scope.

---

## `city`

**Source:** `src/overmap.h` **56–71**.

| Field | Role |
| --- | --- |
| `pos` | Center `point_om_omt` |
| `size` | Layout radius |
| `finale_*` | Finale special placement state |
| `name` | Procedural name string |

`get_nearest_city(tripoint)` used in special placement.

---

## Special placement map

`overmap_special_placements`: anchor OMT → `overmap_special_id`.

Queried by `overmap_special_at`, missions, mapgen extras.

Mutable specials may add runtime state outside this map.

---

## Coordinates (summary)

| Type | Meaning |
| --- | --- |
| `point_om_omt` | Local OMT in one overmap file |
| `point_abs_omt` | World OMT |
| `point_abs_om` | Overmap file index |
| `tripoint_om_omt` | OMT + z (-10…+10 typical) |

Helpers: `project_combine`, `project_to`, `overmapbuffer::get_om_global`.

---

## Serialization

`overmap::serialize` / `unserialize` — binary in world save:

- All `map_layer` terrain (+ visibility in separate stream)
- Cities, connections_out, special placements, mongroups

Submaps: separate save entries when dirty.

Nextgen: W16 not implemented — [../22-world-persistence.md](../22-world-persistence.md).

---

## Relationship to nextgen

| BN | Nextgen |
| --- | --- |
| `overmap` / `map_layer` | `OvermapGrid` |
| `oter_id` | String cell id |
| `submap` / `mapbuffer` | `MapGrid`, `SubmapCache`, `VolumeCache` |
| `visible`/`explored` | W15 todo |
| `connections_out` | Missing |

---

## Inputs

- Generate or deserialize

## Outputs

- Queryable terrain at any OMT/z
- Save blobs

## Failure modes

- OOB `ter_set` — silently ignored (~2996)
- Invalid `oter_id` in save — migration / debugmsg

## Verification

1. Post-generate: all in-bounds `ter()` return valid ids.
2. Save/reload: city count + sample terrain unchanged.
3. Road exits east edge: `connections_out[local_road]` contains edge tripoints.

**BN anchors:** `src/overmap.h`, `src/mapbuffer.h`, `src/omdata.h`.
