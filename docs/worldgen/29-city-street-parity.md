# 29 — City street parity (BN `place_cities` vs nextgen)

Port BN **street-first** city growth into nextgen. W17a–b delivered an intentional
approximation (blob fill + lattice roads); this unit replaces that core loop.

**Status:** done (C1–C5).

**BN truth:** [reference/05a-city-street-growth.md](./reference/05a-city-street-growth.md) ·
[reference/05-cities-and-urban.md](./reference/05-cities-and-urban.md)

**Related:** [26-tier-a-urban-layout.md](./26-tier-a-urban-layout.md) (W17 done) ·
[24-cdda-layout-gaps.md](./24-cdda-layout-gaps.md) § Cities · [28-road-rendering-fidelity.md](./28-road-rendering-fidelity.md)

---

## User-visible symptom

Cities look like a **dense carpet of house OMTs** with a **regular road lattice** cut through,
not BN towns with:

- A real downtown crossroads
- Irregular blocks and side streets
- Buildings facing roads
- Shops near center, houses at the edge

---

## Algorithm comparison (CDDA / BN vs nextgen)

### Growth order

| Step | BN (`overmap.cpp`) | Nextgen (`CityGenerator`) |
| --- | --- | --- |
| 1 | Count cities from size×spacing coverage formula | `CitySitePicker` BN coverage + quota cap |
| 2 | Seed `road_nesw_manhole` at center | `CityStreetGenerator` manhole seed |
| 3 | `build_city_street` × 4 cardinals (recursive) | `CityStreetGenerator.growCity` |
| 4 | `place_building` left/right of each street node | `CityLotPlacer` + distance zoning |
| 5 | Manholes + sewer connections | Manholes + surface sewer approx (C4) |
| 6 | Later `place_roads` links city centers | `HighwayGenerator.connectCities` |

Legacy W17 blob+lattice remains behind `OvermapGenerateOptions.withLegacyUrbanFill(true)`.

### Structural diagram

```text
BN                              Nextgen (default / C1–C4)
─────────────────────────       ─────────────────────────
        ★ manhole                     ★ manhole
         │                             │
    ─────┼─────                   ─────┼─────
         │                             │
      ┌──┴──┐                       ┌──┴──┐
      │lots │                       │lots │
      │face │                       │face │
      │road │                       │road │
```

Legacy (`legacyUrbanFill`): square blob fill, then `GRID_SPACING=3` lattice.

### Zoning

| Rule | BN | Nextgen |
| --- | --- | --- |
| Shop vs house | `town_dist` vs `normal_roll(shop_radius, shop_sigma)` | `SHOP_ROLL_PERCENT = 20` flat |
| Park | Separate park normal roll | `PARK_ROLL_PERCENT = 12` flat |
| Urban bins | `town_size > 10` → `urban_shops` / `urban_houses` | Single tables only |
| Finale | Counter + retry with layer backup | Occasional RNG in outer half of blob |

### Buildings

| Rule | BN | Nextgen |
| --- | --- | --- |
| What gets placed | `overmap_special` via `place_special` | Mostly 1×1 OMT ids from region weights |
| Orientation | Face street (`opposite(street_dir)`) | None |
| Adjacency | Only beside carved roads | Every clearable cell in radius |
| Multitile | Native special footprints | Optional late `CityPlacer` quota |

### Local roads

| Rule | BN | Nextgen |
| --- | --- | --- |
| Path | `lay_out_street` straight walk + collision stop | Orthogonal lines every 3 OMTs |
| Branching | Recursive left/right with `block_width` 2↔3–5 | Fixed lattice |
| Connection | `local_road` + LINEAR bitmask merge | Same connection id, then R1 polish |
| Anti-parallel | Stop if ≥3 nearby road cells | None |

### City count

| Rule | BN | Nextgen |
| --- | --- | --- |
| Formula | `roll_remainder(area * 1/2^spacing / omts_per_city)` | `min(area/spacing², quota/2)` |
| Edge margin | `rng(size-1, OMAP-size)` | `margin = max(2, citySize)` |
| Isolation | — | `city_isolated` → 1 site |

---

## Java map (today)

| Class | Role vs BN |
| --- | --- |
| `CityGenerator.placeAll` | Street-first by default; blob path via `legacyUrbanFill` |
| `CityStreetGenerator` | `lay_out_street` + recursive `build_city_street` + manholes/sewers |
| `CityLotPlacer` | Flank lots + shop/park distance zoning |
| `CitySitePicker` | Coverage formula + spacing |
| `CityTier` | Simplified tiny/small/large/huge rolls |
| `UrbanOmtPlacer` / `LocalRoadGenerator` | Legacy blob + lattice only |
| `CityPlacer` | Multitile leftovers / non-urban regions |
| `HighwayGenerator` | Inter-city hubs (city centers) |

---

## Suggested PR slices

### C1 — Center seed + street grow (no buildings yet)

| Deliverable | Notes |
| --- | --- |
| `CityStreetGenerator` | Port `lay_out_street` + recursive `build_city_street` road-only |
| Seed `road_nesw_manhole` / fixture equivalent at center | |
| Use `local_road` + existing LINEAR polish (R1) | |
| Gate | Feature flag or replace `LocalRoadGenerator` when enabled |
| Tests | Cross from center; side branches; stops at river |

**Success:** City shows irregular road tree, empty lots still field / default oter.

### C2 — Lot placement beside streets

| Deliverable | Notes |
| --- | --- |
| `CityLotPlacer` | `place_building` equivalent: adjacent cell, face street |
| Pick from region special / OMT bins | Prefer specials when placeable; OMT fallback |
| Distance zoning | Port `pick_random_building_to_place` math |
| `BUILDINGCHANCE = 4` | ~75% attempt each flank |
| Tests | No building without adjacent road; shops bias to center |

**Success:** Buildings only on street flanks; downtown denser with shops.

### C3 — Replace blob fill as default

| Deliverable | Notes |
| --- | --- |
| `CityGenerator` calls C1→C2 instead of `UrbanOmtPlacer` + lattice | |
| Keep blob+lattice behind `legacyUrbanFill` or legacy generate order | |
| Wire `CitySitePicker` count toward BN coverage formula | |
| Finale retry (optional v1: single attempt) | |

### C4 — Sewers during street carve

| Deliverable | Notes |
| --- | --- |
| Manhole 1/8 on four-ways | |
| Collect sewer points; connect to center with `sewer_tunnel` | Reuse underground connection carver |

### C5 — Polish / docs

| Deliverable | Notes |
| --- | --- |
| Update [05](./reference/05-cities-and-urban.md) nextgen table | |
| Golden overmap screenshot / hash fixtures | |
| Retire or shrink W17 blob docs as “legacy fill” | |

---

## Non-goals (this unit)

- Full `can_place_special` parity for every special constraint
- Exact BN finale layer-backup performance
- Changing inter-city `HighwayGenerator` topology (already city-center based)
- Visit-time building mapgen (already W7)

---

## Verification

1. Same seed: city center is four-way / manhole road, not a house.
2. Road graph is recursive / irregular — not a uniform every-3 lattice.
3. Sample 50 building OMTs: all have a cardinal road neighbor.
4. Mean shop distance to center < mean house distance (statistical check).
5. Existing worldgen tests pass; add `CityStreetGeneratorTest`, `CityLotPlacerTest`.

---

## Doc map

| Doc | Role |
| --- | --- |
| [05a](./reference/05a-city-street-growth.md) | BN algorithm detail |
| [05](./reference/05-cities-and-urban.md) | BN city overview |
| This file | Nextgen PR contract + comparison |
| [26](./26-tier-a-urban-layout.md) | W17 blob/lattice (current) |
| [06a](./reference/06a-linear-oter-paint.md) | LINEAR roads used by streets |
