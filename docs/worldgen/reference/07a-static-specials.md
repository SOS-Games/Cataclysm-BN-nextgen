# 07a — Static overmap specials

Fixed-footprint `overmap_special` placement: zones, density, sorting, and `place_special_attempt`.

**Parent:** [07-specials-and-mutable.md](./07-specials-and-mutable.md)

---

## `place_specials` overview

**Anchor:** `src/overmap.cpp` **6176–6346**.

### Options read

| Option | Default | Role |
| --- | --- | --- |
| `SPECIALS_SPACING` | 6 | Min spacing for land overlay (`RANGE`) |
| `SPECIALS_DENSITY` | 1.0 | Scales occurrence counts |

### Zones

```cpp
enum zone { land, land_under, lake, river, last };
```

Classification per special (~6226–6229):

| Zone | Rule |
| --- | --- |
| `lake` | Special flag `LAKE` |
| `river` | Flag `RIVER`, or required location includes `"water"` |
| `land` | Has z=0 location requirements matching surface |
| `land_under` | Underground-only locations |

Point pools built by scanning map (~6271–6290): lake/river/underground OMTs always collected;
surface `land` points must match union of land special location tags (filters out city buildings).

---

## Density and crowd ratio

For each zone, estimate area needed from special footprints + spacing (~6241–6246):

```text
total_area += (sqrt(special_area) + range)² × average_occurrences × DENSITY
crowd_ratio = min(1, OMAP_AREA / area_needed[zone])
zone_ratio[zone] = (zone_points.size / OMAP_AREA) × crowd_ratio × DENSITY
```

River zone reuses underground ratio as hack (~6304–6305).

---

## Sort order

Specials sorted descending by `special_weight` (~6254–6268):

- Base weight = footprint area (`special_area`)
- ×1000 if `ENDGAME` on abs-origin overmap `pos() == point_abs_om()`
- Weight 0 if `ENDGAME` + `GLOBALLY_UNIQUE` off origin

Large specials claim space first; central lab prioritized at world center.

---

## Occurrence count

Per special (~6328+):

```text
amount = roll between constraints.occurrences.min and max
if UNIQUE or GLOBALLY_UNIQUE: amount = min/max rules differ
amount *= zone_ratio[current]
if ENDGAME at origin: rate = 1 (ignore zone_ratio)
```

Then:

```cpp
place_special_attempt(special, amount_to_place, *zone_overlay[current], false);
```

---

## `place_special_attempt`

**Anchor:** `src/overmap.cpp` **6084–6075** (approx).

### City-required specials

If `special.requires_city()`:

- Count cities matching `constraints.city_size`
- `max_per_city = ceil(max / valid_cities)`
- Reject anchors outside city distance constraints

### Placement loop

Iterates shuffled candidates from `specials_overlay`:

1. Spacing check vs existing specials (`SPECIALS_SPACING`, overlay bitmap)
2. `special.can_place` / footprint fit on all `overmap_special_terrain` cells
3. City distance / size constraints
4. Paint terrains + record `overmap_special_placements`
5. Run embedded `overmap_special_connection` carves if defined

Returns count placed; failures are silent (candidate skipped).

---

## JSON structures (`overmap_special.h`)

| Struct | Role |
| --- | --- |
| `overmap_special_terrain` | Relative OMT offset + `oter_str_id` + location constraints |
| `overmap_special_locations` | Where a piece may sit (`overmap_location_id` set) |
| `overmap_special_connection` | Optional embedded connection carve |
| `overmap_special_placement_constraints` | `city_size`, `city_distance`, `occurrences` interval |
| `overmap_special_spawns` | Monster/item spawns (gameplay) |

Subtype: `overmap_special_subtype::fixed` vs `mutable_`.

---

## City buildings (related)

Urban lots use `pick_random_building_to_place` → `place_special` / `place_building` during
`build_city_street` — **not** the global `place_specials` loop. Same underlying special
machinery. See [05-cities-and-urban.md](./05-cities-and-urban.md).

---

## Nextgen

`StaticSpecialPlacer` implements simplified zone/density. Gaps: `GLOBALLY_UNIQUE` world tracking,
ENDGAME center weighting, full `specials_overlay` spacing.

---

## Inputs

- Enabled batch sorted by weight
- Zone point pools and overlays
- `cities` list for city-tied specials

## Outputs

- OMT ids for special footprints
- `overmap_special_placements`

## Failure modes

- Empty zone pool — zero placements
- `max < 1` after density scaling — skipped
- Footprint overlap — attempt rejected

## Verification

1. Reduce `SPECIALS_DENSITY` to 0.1 — fewer specials per overmap.
2. Set `SPECIALS_SPACING` to 20 — larger gaps between land specials.
3. City shop special only in cities matching `city_size` in JSON.

**BN anchors:** `src/overmap_special.h`, `src/overmap.cpp` (`place_specials`, `place_special_attempt`).
