# 26 — Tier A urban layout (W17)

**Tier A** overmap layout parity: BN-style **urban blobs**, **local roads**, and **inter-city
highways** — closing the largest visual gap vs `overmap::generate` after W14.

**Status:** W17a–f done. See [v4-implementation-plan](./v4-implementation-plan.md).

**Prerequisites:** W9 region loader, W14b `CitySitePicker`, W5/W11c `HighwayGenerator` +
`OvermapConnectionRegistry`.

**Parent:** [23-cdda-parity-overview](./23-cdda-parity-overview.md) · [24-cdda-layout-gaps](./24-cdda-layout-gaps.md)

---

## Purpose

Today `CityPlacer` drops a **small quota** of multitile `city_building` bundles near
`CitySitePicker` centers. BN `place_cities` **fills** each urban blob with:

- Weighted **house** OMT ids (`city.houses`)
- **Shops**, **parks**, **finales** from separate region tables
- **Local road** grid (`local_road` connection)
- Then `place_roads` links **city centers** with highways

W17 implements a **subset** of that pipeline in three P0 slices (W17a–c), plus optional P1/P2
follow-ups (W17d–f).

W17 intentionally approximated BN with **blob fill + lattice roads**. Full street-first port:
[29-city-street-parity.md](./29-city-street-parity.md) · BN detail:
[reference/05a-city-street-growth.md](./reference/05a-city-street-growth.md).

---

## Sub-PRs

| PR | Focus | Priority |
| --- | --- | --- |
| **W17a** | Region `shops`/`parks`/`finales` + 1×1 urban OMT fill | P0 |
| **W17b** | In-city `local_road` grid | P0 |
| **W17c** | Inter-city highways + `OvermapGenerator` reorder | P0 |
| **W17d** | Multi-river + `polish_rivers` lite | P1 |
| **W17e** | Forest trails + trailheads | P2 |
| **W17f** | Subways / rails / sewers (ex-W14d) | P2 |

---

## Target generate order (after W17c)

Match BN more closely than v3 post-W14 order:

```text
BaseTerrainFiller
→ LakeGenerator → RiverGenerator (+ W17d polish)
→ ThickForest / Swamp / Beach
→ CityGenerator.placeAll          # W17a — urban blobs (houses, shops, parks, finales)
→ LocalRoadGenerator.carve        # W17b — inside each blob
→ HighwayGenerator.connectCities  # W17c — city center to city center
→ RegionSpecialPlacer
→ StaticSpecialPlacer
→ MutableSpecialPlacer
→ (W17e forest trails — optional pass)
→ (W17f underground — optional pass)
```

**Change from today:** roads run **after cities**, **before** most specials; highways use **city
centers only**, not every special/mutable site.

Keep `options.isLegacyGenerationOrder()` for regression tests.

---

## W17a — Urban OMT fill

### BN reference

`overmap::place_cities` — `src/overmap.cpp` ~4781:

- Computes `NUM_CITIES` from `city_size`, `city_spacing`, overmap dimensions
- Per city: random size tier (tiny/small/large/huge), optional finale
- Fills blob with weighted picks from `city_spec.houses`, `shops`, `parks`, `finales`
- Uses `local_road` connection during fill (W17b may split this)

### Target

```text
CityGenerator.placeAll(grid, region, options, rng):
    sites = CitySitePicker.pickSites(...)   // existing W14b
    for each site in sites:
        tier = rollCityTier(rng)            // simplified BN size/finale flags
        radius = citySize from tier
        fillUrbanBlob(site, radius, region, rng):
            for each cell in square [-radius..+radius]²:
                if not clearable: skip
                roll category: house | shop | park | empty lot | (finale if tier large)
                pick weighted OMT id from region table
                grid.setOmtId(x, y, omtId)
        optionally place 0–1 multitile city_building (reduced weight vs today)
```

### Region JSON (extend loader)

Parse from `city` block in `regional_map_settings.json`:

| Field | Type | Usage |
| --- | --- | --- |
| `houses` | `Map<String, Integer>` | Already loaded — reuse |
| `shops` | `Map<String, Integer>` | **New** |
| `parks` | `Map<String, Integer>` | **New** |
| `finales` | `Map<String, Integer>` | **New** |

BN also has `city_size`, `city_spacing` — already `CitySizeSettings` (W14b).

### New / extended types

```text
region/CityContentWeights.java       # houses + shops + parks + finales
region/RegionSettingsDefinition.java # expose getCityContentWeights()
region/RegionSettingsLoader.java   # parse shops/parks/finales

generate/CityGenerator.java          # orchestrates urban blob fill (may wrap/refactor CityPlacer)
generate/UrbanOmtPlacer.java       # 1×1 weighted OMT placement in blob
generate/CityTier.java               # tiny/small/large/huge + attemptFinale
```

### Placement rules (v1)

| Rule | Behavior |
| --- | --- |
| Clearable terrain | Reuse `OmtBuildingBlitter.defaultClearableIds` + field/forest/lake |
| Registry check | Skip unknown OMT ids with warning (fixture + BN integration) |
| Shop/park rate | ~15–25% shops, ~10–15% parks inside blob (tune; document constants) |
| Multitile buildings | ≤1 per blob; prefer small house bundles over `2storyModern01` scale |
| Finale | At most 1 per large/huge tier; min distance from center (BN `finale_distance`) |
| Water / river | Do not overwrite `river` / `lake` OMT ids |

### Extend

| File | Change |
| --- | --- |
| `OvermapGenerator` | Call `CityGenerator` instead of raw `CityPlacer.placeAll` for non-legacy |
| `CityPlacer` | Refactor: multitile path kept; urban fill delegated to `UrbanOmtPlacer` |
| `PlacedBuildingRecord` | Optional: `PlacementSource.URBAN_OMT` for 1×1 picks (visit uses mapgen pick) |

### Tests

| Test | Assert |
| --- | --- |
| `RegionSettingsLoaderTest` | Parses `shops`/`parks`/`finales` from fixture |
| `UrbanOmtPlacerTest` | Fixed seed → known count of shop/park OMT ids in blob |
| `CityGeneratorTest` | 64×64 + urban region → `stats` road-adjacent building ids > quota-only baseline |

### Fixture

```text
core/src/test/resources/worldgen-fixtures/urban-heavy-region.json
```

Minimal region with `city_size: 4`, `city_spacing: 2`, and small `shops`/`parks` tables pointing
at test OMT ids present in gfx/game fixtures.

---

## W17b — In-city local road grid

### BN reference

`place_cities` uses `overmap_connection_id local_road` to carve streets while filling the blob.

### Target

```text
LocalRoadGenerator.carve(grid, urbanSites, connectionRegistry, options, rng):
    for each UrbanSite site:
        carve orthogonal grid or cross through site.center
        use connection id "local_road" (fallback: generic road OMT)
        only inside site.radius
        record junction cells for highway endpoints (W17c)
```

### New types

```text
generate/LocalRoadGenerator.java
generate/UrbanSite.java            # centerX, centerY, radius, tier, attemptFinale
```

### Extend

| File | Change |
| --- | --- |
| `OvermapConnectionRegistry` | Resolve `local_road` in tests (BN data or fixture) |
| `OvermapGenerator` | W17b immediately after W17a |

### Tests

| Test | Assert |
| --- | --- |
| `LocalRoadGeneratorTest` | Blob contains connected `road_*` cells; center reachable from edge |
| Integration | `OvermapGenerator` non-legacy → road cells inside city radius > 0 |

---

## W17c — Inter-city highways + reorder

### BN reference

`overmap::place_roads` (~4615) — connects cities after `place_cities`, before `place_specials`.

### Target

```text
HighwayGenerator.connectCities(grid, urbanSites, connections, options, registry, rng):
    centers = urbanSites.map(site -> site.center)
    if centers.size() < 2: return 0
    // v1: connect in deterministic order (sorted by x,y) or MST on centers only
    carve paths with highway connection id (existing directional carve)
    do NOT add mutable/static special centers to highway graph
```

### Extend

| File | Change |
| --- | --- |
| `HighwayGenerator` | Add `connectCities`; deprecate connecting all `placedSites` for non-legacy |
| `OvermapGenerator` | Reorder passes per § Target generate order |
| `placedCenters` | Highways use `UrbanSite` centers; specials may still append for other features |

### Tests

| Test | Assert |
| --- | --- |
| `HighwayGeneratorCityTest` | Two urban sites → path of road OMTs between centers |
| `OvermapGeneratorOrderTest` | Roads appear before static specials in generation log / snapshot hook |

### Manual smoke

64×64, `default` region, fixed seed:

- Highways link town centers
- Wilderness between cities not fully paved

---

## W17d — Hydrology v2 (P1)

### BN reference

`place_rivers`, `polish_rivers` — multiple paths, junction cleanup, bridge hooks.

### Target (lite)

- Optional second `RiverGenerator` pass with different seed xor
- `RiverPolisher.smooth` — remove dangling single-tile river spurs
- Bridge terrain when highway crosses river (reuse connection bridge field from W11c)

### Tests

`RiverGeneratorMultiTest`, `RiverPolisherTest` on lake-heavy fixture.

---

## W17e — Forest trails (P2)

### BN reference

`place_forest_trails`, `place_forest_trailheads` when `forest_trail.chance > 0`.

### Target

- Parse `forest_trail_settings` from region settings
- Carve low-weight trail OMTs through forest cells only (`ForestTrailGenerator`)
- Optional trailhead OMT at trail end near road (`trailheads` weights)

### Implementation

| File | Role |
| --- | --- |
| `ForestTrailSettings` | Parsed `forest_trail_settings` block |
| `ForestTrailGenerator` | Flood-fill forest blobs → MST trail carve + trailheads |
| `OvermapGenerator` | Optional pass after mutable specials (non-legacy) |

### Tests

| Test | Assert |
| --- | --- |
| `ForestTrailGeneratorTest` | Trails in forest blob; disabled when `chance=0`; trailhead near road |
| `RegionSettingsLoaderTest` | `forest_trails` fixture parses trail settings |

---

## W17f — Subways / rails / sewers (P2)

Reopens [20-layout-parity-phase2](./20-layout-parity-phase2.md) W14d.

Separate connection-graph carve passes; do not block W17a–c.

### Target (lite)

- Parse `underground_network_settings` from region (`subways`, `rails`, `sewers` booleans)
- `SubwayGenerator` — MST between urban centers via `subway_tunnel`
- `RailGenerator` — extremal city pairs via `local_railroad`
- `SewerGenerator` — in-city sewer grid via `sewer_tunnel` (surface OMT preview)

### Implementation

| File | Role |
| --- | --- |
| `UndergroundNetworkSettings` | Parsed region flags |
| `ConnectionPathGenerator` | Shared MST / extremal orthogonal carve |
| `SubwayGenerator` / `RailGenerator` / `SewerGenerator` | Individual networks |
| `UndergroundNetworkGenerator` | Orchestrator after forest trails |

### Tests

| Test | Assert |
| --- | --- |
| `UndergroundNetworkGeneratorTest` | Subway + rail + sewer cells when enabled |
| `RegionSettingsLoaderTest` | `underground_networks` fixture parses settings |

---

## `UrbanSite` model

```java
public final class UrbanSite {
    private final int centerX;
    private final int centerY;
    private final int radius;
    private final CityTier tier;
    private final boolean attemptFinale;
}
```

`CitySitePicker` returns `List<UrbanSite>` (breaking change behind new method or adapter from
`int[]` centers + `citySize`).

---

## Editor / options

| Topic | v1 behavior |
| --- | --- |
| `regionId` | Still defaults to `"default"`; document in tests |
| Quotas | `cityBuildingQuota` becomes **max multitile** per overmap; urban OMT fill uses blob area |
| Export | `OvermapGridExporter` stats — add `urbanOmtsPlaced`, `localRoadCells`, `highwayCells` (optional) |

---

## v1 out of scope (W17)

| Topic | Notes |
| --- | --- |
| Neighbor overmap road/river stitch | Tier C |
| Exact BN `NUM_CITIES` formula | Approximate via `CitySitePicker` until proven off |
| Sewer graph inside cities | W17f |
| Special `city_sizes` constraints | Separate P1 PR after W17c |

---

## BN source map

| Concern | Location |
| --- | --- |
| `place_cities` | `src/overmap.cpp` ~4781 |
| `place_roads` | ~4615 |
| `city_settings` | `src/regional_settings.h` / JSON `city` block |
| `local_road` | `data/json/overmap/overmap_connection/` |

---

## Verification

1. W17a–c each have tests + fixture region
2. Target generate order documented and enforced in `OvermapGenerator`
3. [27-world-map-v4-roadmap](./27-world-map-v4-roadmap.md) success criteria met
4. [24-cdda-layout-gaps](./24-cdda-layout-gaps.md) city/road rows updated when W17 lands
