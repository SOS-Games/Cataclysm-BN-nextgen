# 05 — Cities and urban layout

How BN places cities, carves street grids, fills lots, and tunnels sewers.

**Pipeline position:** after hydrology/forests, **before** forest trails and `place_roads`.

**Anchor:** `overmap::place_cities` **4781–4895**, `build_city_street` **4961–5068**.

---

## City count formula

```cpp
op_city_size    = city_spec.city_size    >= 0 ? city_spec.city_size    : option CITY_SIZE
op_city_spacing = city_spec.city_spacing >= 0 ? city_spec.city_spacing : option CITY_SPACING
if (op_city_size <= 0) return;

omts_per_overmap       = OMAPX * OMAPY   // 180² = 32400
city_map_coverage_ratio = 1.0 / pow(2.0, op_city_spacing)
omts_per_city          = (2*op_city_size + 1)² * 3/4

NUM_CITIES = roll_remainder(omts_per_overmap * city_map_coverage_ratio / omts_per_city)
```

Comment table in source (~4791–4802) lists expected city counts vs spacing/size.

---

## City seed placement loop

Up to `OMAPX * OMAPY` attempts:

1. Random `tmp.pos` on map (not too close to existing cities)
2. Size tier roll (~4832–4841):
   - 33% tiny (`size = 1`, no finale)
   - 33% small (`size * 2/3`)
   - 17% large (`size * 3/2`, attempt finale)
   - remainder normal
3. Backup `map_layer` before carving (finale retry)
4. Carve four `build_city_street` arms from center (N/E/S/W rotation loop)
5. Connect sewer manholes via `sewer_tunnel` connection
6. If finale required but failed — restore backup and retry (up to `finale_max_tries`)

---

## `build_city_street`

Parameters: `connection`, start `p`, radius `cs`, direction `dir`, `city &town`, sewer list.

```text
street_path = lay_out_street(connection, p, dir, cs+1)
build_connection(connection, street_path, z=0)
for each node along path (except start):
  every few tiles: recursive side streets (turn_left / turn_right, reduced radius)
  every ~BUILDINGCHANCE: place_building on both sides (pick_random_building_to_place)
  1/8 four-way road: road_nesw_manhole + sewer_isolated below → sewers list
end
optional dead-end spur at last node (1/5 opposite direction)
```

`lay_out_street` uses connection pathfinding — same cost model as [06-connections.md](./06-connections.md).

---

## Building picks — `pick_random_building_to_place`

**Anchor:** ~4897+.

Uses normal distribution from city center:

| Distance from center | Bin |
| --- | --- |
| Within `shop_normal` | `pick_shop()` / `pick_urban_shop()` |
| Within `park_normal` | `pick_park()` |
| Else | `pick_house()` / `pick_urban_house()` |
| Finale attempt | `pick_finale()` |

Bins are weighted `overmap_special_id` lists in region `city_spec`.

---

## Sewers

During street carve:

- Manhole at 4-way `road` intersections (`road_nesw_manhole`, `sewer_isolated` at z−1)
- After four arms: `build_connection(city_center, sewer_point, z, sewer_tunnel)` for each collected sewer OMT

Connection id: `"sewer_tunnel"` → paints `sewer` on z < 0.

---

## `city` struct

**Source:** `src/overmap.h` **56–71**.

| Field | Role |
| --- | --- |
| `pos` | Center OMT |
| `size` | Radius parameter used in carve |
| `finale_counter`, `finale_placed`, `attempt_finale` | Finale building state |
| `name` | Optional generated name |

---

## Data files

| Path | Role |
| --- | --- |
| `data/json/overmap/multitile_city_buildings.json` | Large urban footprints |
| `data/json/overmap/overmap_special/` | Shop/house/finale specials referenced from region |
| Region `city_spec` in `regional_map_settings.json` | Weight tables |

---

## Z levels

- Surface: building/road OMT ids
- z − 1: sewers under manholes / connections
- Some specials span multiple z via terrain list

---

## Relationship to nextgen

| BN | Nextgen |
| --- | --- |
| `place_cities` + `build_city_street` | `CityGenerator`, `LocalRoadGenerator` (W4, W17a) |
| Normal-distributed shop/park/house | Simplified picks |
| Sewer carve | `UndergroundNetworkGenerator` (W17f partial) |

Houses surrounded by `field` on overmap usually mean local roads did not reach — visit still
uses OMT id for mapgen.

---

## Inputs

- Surface after forests/rivers/lakes
- `city_spec`, `CITY_SIZE`, `CITY_SPACING`
- `local_road`, `sewer_tunnel` connections

## Outputs

- `cities` vector
- Road OMT ids within urban radii
- Building footprints via `place_special`/`place_building`
- Sewer OMTs at z − 1

## Failure modes

- `CITY_SIZE == 0` — no cities, no city-driven roads in `place_roads` hubs
- Missing `local_road` — streets skipped
- Finale never places — city restored from backup or accepted without finale

## Verification

1. Default options: handful of cities on 180×180 overmap.
2. City center: `road_*` cross pattern.
3. `house_*` / shop ids within ~`size` OMTs of center.
4. Sewer id under manhole OMT at z − 1.

**BN anchors:** `src/overmap.cpp` (`place_cities`, `build_city_street`, `pick_random_building_to_place`).
