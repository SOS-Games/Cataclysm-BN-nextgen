# 05 — City and static special placement (W4)

Place **`city_building`** footprints and **static `overmap_special`** layouts onto the overmap grid.

**Status:** done (W4). See [implementation-plan](./implementation-plan.md).

---

## Purpose

BN `overmap::generate` scatters towns, farms, and specials after base terrain. W4 adds the
**first procedural layout** pass on top of W2's grid — reusing existing bundle loaders from
[mapgen preview](../mapgen-preview/09-building-bundles-overview.md).

---

## Inputs

| Source | Loader (existing) |
| --- | --- |
| `multitile_city_buildings.json` | `CityBuildingLoader` |
| `overmap_special/` (static) | `OvermapSpecialBuildingLoader` / P7c |
| `OvermapTerrainRegistry` | W1 — validate `overmap` ids exist |

---

## Placement algorithm (v1 subset)

```text
OvermapGenerator.generate(seed, region, options):
    grid ← fillBaseTerrain(seed, region)     // forest/field/water noise — minimal

    for building in pickCityBuildings(seed, citySize):
        footprint ← building.horizontalFootprintAtZ(0)
        (x, y) ← findClearRect(grid, footprint.w, footprint.h)
        if found:
            blitOmtIds(grid, building, x, y)   // each piece → oter id at offset

    for special in pickStaticSpecials(seed, quota):
        similar blit from overmap_special overmaps[]

    return grid
```

**v1 simplifications:**

- Fixed city count (e.g. 1 town per 16×16)
- No road connection requirements (W5)
- No collision with rivers (W5)
- Rotation: pick `_north` only or random cardinal

---

## Blitting OMT ids

For each [CityBuildingPiece](../mapgen-preview/10-city-building-loader.md):

```text
grid.setOmtId(baseX + piece.offsetX, baseY + piece.offsetY, piece.overmapId)
```

Multi-tile same floor shares ids per BN (e.g. `house_09_north` on 2×1 OMT).

---

## Planned Java types

```java
public final class OvermapGenerator {
    public OvermapGrid generate(OvermapGenerateOptions options);
}

public final class CityPlacer {
    public int place(CityBuildingDefinition building, OvermapGrid grid, Random rng);
}

public final class StaticSpecialPlacer {
    public int place(CityBuildingDefinition special, OvermapGrid grid, Random rng);
}
```

`CityBuildingDefinition` already models many specials after P7c.

---

## Region

Use [RegionContext](../mapgen-preview/19-regional-terrain.md) id from world options to filter
which buildings/specials appear (BN `region_settings` weights — v1: uniform random).

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Footprint overlap | Skip building + warning |
| Unknown overmap id on piece | Skip piece |
| Grid too small | Warn; partial place |

---

## Test fixtures

`worldgen-fixtures/overmaps/after_city_place.json` — expected ids after seed `42` on 16×16.

Integration: sibling BN data — `house_09` appears in generated town.

---

## BN source reference

| Concern | Location |
| --- | --- |
| City generation | `src/overmap.cpp` — city related passes |
| Building defs | `data/json/overmap/multitile_city_buildings.json` |
| Specials | `data/json/overmap/overmap_special/` |

---

## Verification

1. Generated 16×16 contains contiguous house footprint
2. Visit house OMT (W3) produces same grid as building import
3. Seed reproducibility
