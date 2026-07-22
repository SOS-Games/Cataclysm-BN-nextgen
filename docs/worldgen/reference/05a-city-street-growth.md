# 05a — City street growth (`build_city_street`)

Deep dive into BN **street-first** city construction. Parent overview:
[05-cities-and-urban.md](./05-cities-and-urban.md).

**BN anchors:** `src/overmap.cpp` — `place_cities` (~4781), `build_city_street` (~4961),
`place_building` (~4936), `pick_random_building_to_place` (~4897), `lay_out_street` (~5158).

**Region data:** `city_settings` in `src/regional_settings.h` · JSON `city` block in
`data/json/regional_map_settings.json`.

**Nextgen gap / plan:** [../29-city-street-parity.md](../29-city-street-parity.md)

---

## Mental model

BN cities are **not** “fill a square with houses, then stamp a road grid.”

```text
1. Pick NUM_CITIES centers (coverage formula)
2. At each center: plant road_nesw_manhole (+ sewer_isolated @ z-1)
3. Recursively grow streets (build_city_street × 4 cardinals)
4. Along each street cell: place_building LEFT and RIGHT
5. Connect sewer manholes back to city center
```

Shape emerges from the **street tree**. Buildings exist because a street walked past them.

---

## City count (`place_cities`)

```cpp
op_city_size    = city_spec.city_size    >= 0 ? … : option CITY_SIZE
op_city_spacing = city_spec.city_spacing >= 0 ? … : option CITY_SPACING
if (op_city_size <= 0) return;

omts_per_overmap        = OMAPX * OMAPY          // 180²
city_map_coverage_ratio = 1.0 / pow(2.0, op_city_spacing)
omts_per_city           = (2*op_city_size + 1)² * 3/4

NUM_CITIES = roll_remainder(omts_per_overmap * coverage / omts_per_city)
```

Comment table in source (~4791) maps spacing → % map covered → expected city counts.

Placement loop: up to `OMAPX*OMAPY` attempts; reject if cell ≠ `default_oter`; keep cities
≥ `size` from overmap edge.

### Size tiers (per city seed)

Base: `size = rng(op_city_size - 1, op_city_size + 1)`, then:

| Chance | Tier | Size transform | Finale |
| --- | --- | --- | --- |
| ~33% | tiny | `size = 1` | no |
| ~33% | small | `size * 2/3` | no |
| ~17% | large | `size * 3/2` | yes (`finale_distance = 5`) |
| ~17% | huge | `size * 2` | yes (`finale_distance = 15`) |

If `city_spec.finales` empty → force `attempt_finale = false`.

Finale retry: backup entire `map_layer` before carve; if finale required but not placed,
restore and retry (up to 1500 attempts).

---

## Center seed

Before any street:

```cpp
ter_set(p, "road_nesw_manhole");
ter_set(p + below, "sewer_isolated");
cities.push_back(tmp);  // pos, size, finale_counter = rng(0, finale_distance)
```

Then four arms:

```cpp
start_dir = random cardinal
cur_dir = start_dir
do {
  build_city_street(local_road, tmp.pos, size, cur_dir, tmp, sewers);
} while ((cur_dir = turn_right(cur_dir)) != start_dir);
```

Connection id: **`local_road` only** (not a separate highway id).

---

## `lay_out_street`

**Anchor:** ~5158.

Straight walk from `source` along `dir` for up to `len` OMTs:

| Stop condition | Reason |
| --- | --- |
| Near overmap bound (`inbounds(pos, 1)` fails) | Avoid edge clip |
| Cell is river | Don’t pave water |
| No connection subtype for cell | Can’t carve here |
| ≥3 neighboring road cells in 3×3 (excluding forward/back) | Anti-parallel-road collision |
| After first step, current cell already has this connection | Stop at existing street |

Optional +1 length if cell one past planned end already has connection.

Returns `straight_path(source, dir, actual_len)` — **not** A* (contrast `lay_out_connection`).

---

## `build_city_street`

**Signature:** `(connection, start_point, cs /*radius*/, dir, town&, sewers&, block_width=2)`

```text
path = lay_out_street(connection, p, dir, cs+1)
if path.length <= 1: return
build_connection(connection, path, z=0)   // paints LINEAR road_* bits

c = cs; croad = cs
new_width = (block_width == 2) ? rng(3,5) : 2   // alternate thick/thin blocks

for each node after start:
  c--
  if c >= 2 && c < croad - block_width:
    croad = c
    left  = cs - rng(1,3);  if left==1: left++
    right = cs - rng(1,3);  if right==1: right++
    recurse: build_city_street(..., turn_left(dir),  left,  new_width)
    recurse: build_city_street(..., turn_right(dir), right, new_width)
    if one_in(8) && road line==15 (nesw):
      road_nesw_manhole + sewer_isolated below → sewers[]

  // Buildings on both flanks (BUILDINGCHANCE = 4 → ~75% each side)
  if !one_in(4): place_building(node, turn_left(dir),  town, finale?)
  if !one_in(4): place_building(node, turn_right(dir), town, finale?)

# Edge of town: optional right-angle neighborhood spur
cs -= rng(1,3)
if cs >= 2 && c == 0:
  build_city_street(..., turn_random(dir), cs)
  if one_in(5): also opposite turn
```

### Block structure

- `block_width` controls how far along a street before side streets sprout
- Alternating 2 vs 3–5 produces irregular block sizes (little neighborhoods)
- Length-1 nubs bumped to 2

### Manholes / sewers

During sprout: 1/8 chance at true four-way (`get_line() == 15`) → manhole + `sewer_isolated`
at z−1, collect for later `build_connection(city_center, sewer_point, z, sewer_tunnel)`.

---

## `place_building`

**Anchor:** ~4936.

```cpp
building_pos = street_node + displace(dir)     // lot beside road
building_dir = opposite(dir)                   // face the street
town_dist = trig_dist(building_pos, town.pos) * 100 / max(town.size, 1)

for retries in 10..0:
  tid = pick_random_building_to_place(town_dist, town.size, attempt_finale)
  if can_place_special(tid, building_pos, building_dir, …):
    place_special(…)
    return true
return false
```

Buildings are **`overmap_special` ids** from region bins (houses, shops, parks, finales) —
multitile footprints with rotation facing the street — **not** bare 1×1 `oter` paint.

---

## Zoning: `pick_random_building_to_place`

**Anchor:** ~4897 · defaults in `city_settings`:

| Field | Default | Role |
| --- | --- | --- |
| `shop_radius` | 30 | Mean of shop normal roll |
| `shop_sigma` | 20 (JSON often 50) | Shop spread |
| `park_radius` | = shop_radius | Park mean |
| `park_sigma` | 100 − park_radius | Parks bleed outward |

```cpp
shop_normal = max(normal_roll(shop_radius, shop_sigma), shop_radius)
park_normal = max(normal_roll(park_radius, park_sigma), park_radius)

if attempt_finale: return pick_finale()
else if shop_normal > town_dist:
  return town_size > 10 ? pick_urban_shop() : pick_shop()
else if park_normal > town_dist:
  return pick_park()
else:
  return town_size > 10 ? pick_urban_house() : pick_house()
```

`town_dist` is **percent of city radius** (0 near center → 100+ at edge). Shops concentrate
downtown; houses dominate outskirts; parks can appear anywhere via large sigma.

JSON bins: `houses`, `urban_houses`, `shops`, `urban_shops`, `parks`, `finales` — weighted
`overmap_special_id` lists (`building_bin`).

---

## Pipeline position

Inside `overmap::generate` (~3431):

```text
rivers → lakes → forests/swamps
→ place_cities()          ← THIS UNIT
→ forest_trails
→ place_roads(n,e,s,w)    ← inter-city; hubs = cities + connections_out
→ specials → trailheads
→ polish_rivers
```

Local streets exist **before** world `place_roads`. Inter-city highways connect city centers
(and edge exits), they do not invent the downtown grid.

---

## Inputs / outputs / failures

### Inputs

- Surface after hydrology / forest (mostly `default_oter`)
- `city_spec` + world `CITY_SIZE` / `CITY_SPACING`
- `local_road`, `sewer_tunnel` connections
- Overmap special registry for building bins

### Outputs

- `cities[]` with `pos`, `size`, finale flags
- LINEAR `road_*` / manhole OMTs
- Building special footprints oriented to streets
- Sewer OMTs at z−1

### Failure modes

| Case | Behavior |
| --- | --- |
| `CITY_SIZE == 0` | No cities; `place_roads` has fewer hubs |
| Center not `default_oter` | Skip seed |
| `lay_out_street` length ≤ 1 | Arm aborted |
| `place_building` 10 retries fail | Empty lot beside street |
| Finale never places | Restore layer backup / give up after max tries |
| Missing `local_road` | Streets cannot carve |

### Verification

1. City center is `road_nesw_manhole` (or LINEAR four-way).
2. Street graph is irregular recursive branches — not a uniform lattice.
3. Buildings only appear adjacent to roads, facing them.
4. Near center: higher shop density; edge: mostly houses.
5. Some manholes have sewer below; sewer tunnels link toward center.

---

## Nextgen divergence (summary)

| BN | Nextgen (W17) |
| --- | --- |
| Street-first recursive growth | Fill square blob, then lattice roads (`GRID_SPACING=3`) |
| `place_special` beside street | 1×1 OMT paint in radius |
| Distance zoning | Flat % shop/park/skip |
| Center manhole seed | Center often skipped / empty |
| Sewers during street carve | Separate underground pass |

Full comparison + PR slices: [29-city-street-parity.md](../29-city-street-parity.md)
