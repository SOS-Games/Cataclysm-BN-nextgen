# 04 — Visit-tile mapgen (W3)

**Weighted mapgen selection** when visiting an OMT cell, producing a cached **24×24 submap**.

**Status:** done (W3). See [implementation-plan](./implementation-plan.md).

---

## Purpose

BN calls `map::draw_map` when a submap loads. For json mapgen:

```text
oter_mapgen.pick(function_key)  →  mapgen_function
mapgen_function_json::generate(mapgendata)
```

Nextgen equivalent: given OMT id at `(x,y)`, pick a `JsonMapgenDefinition` and run
[JsonMapgenRunner](../mapgen-preview/05-rows-runner.md).

---

## Algorithm

```text
visit(overmap, omtX, omtY, z, worldOptions):
    key ← (seed, omtX, omtY, z)
    if cache.contains(key): return cache.get(key)

    omtId ← overmap.getOmtId(omtX, omtY)
    ter ← oterRegistry.find(omtId).orElse(warn fallback field)

    def ← mapgenPicker.pick(ter, z, rngFor(key))
    if def == null: return emptyGrid(fillTer)

    options ← worldOptions
        .withPreviewSeed(mixSeed(worldSeed, key))
        .withOmtRotation(rotationFromOmSuffix(omtId))
        .withMapgenCatalog(catalog)
        .withRegionContext(regionContext)

    grid ← JsonMapgenRunner.run(def, catalog, palettes, options)
    cache.put(key, grid)
    return grid
```

---

## `MapgenPicker`

```java
public final class MapgenPicker {
    public Optional<JsonMapgenDefinition> pick(
        OvermapTerrainDefinition terrain,
        int z,
        Random rng,
        MapgenCatalog catalog,
        List<String> warnings
    );
}
```

Rules (v1):

| Rule | Behavior |
| --- | --- |
| Single json candidate | Always that def |
| Multiple weights | Sum weights; `rng.nextInt(total)` |
| `disabled: true` on mapgen | Skip |
| No candidates | Warn; return `Optional.empty()` |
| Builtin / lua ref | Warn once per visit |

Match BN: weights live on **mapgen JSON entries**, not on OMT `mapgen` array (BN links by om_terrain key).

---

## Submap cache

```java
public final class SubmapCache {
    public Optional<MapGrid> get(SubmapKey key);
    public void put(SubmapKey key, MapGrid grid);
    public void clear();
}
```

LRU cap (v1): 64 entries. Key includes world seed.

---

## `WorldgenPreviewService`

Facade similar to [MapgenPreviewService](../MAPGEN_PREVIEW.md):

```java
public final class WorldgenPreviewService {
    public void ensureLoaded(WorldgenScanOptions options);
    public OvermapGrid createTestOvermap(int w, int h, long seed);
    public VisitResult visit(OvermapGrid overmap, int x, int y, int z);
}
```

---

## Z-levels (W3.1)

BN picks mapgen per `(om_terrain, z)`. v1: **z=0 only**.

Extension: filter catalog defs whose `om_terrain` matches `_roof` / basement suffix patterns or explicit z in building bundles.

---

## Neighbor context (stub)

BN nested mapgen checks neighbors ([21-nested](../mapgen-preview/21-nested-update-mapgen.md)). W3 v1:

- Pass neighbor OMT ids in `JsonMapgenRunOptions` stub for future join checks
- Do not block visit if neighbors missing

---

## Test plan

| Test | Assert |
| --- | --- |
| `MapgenPickerTest` | Two defs weight 1:9 → ~90% second id over many rolls |
| `SubmapGeneratorTest` | Same key → same grid bytes |
| Integration | OMT `test_field` → matches direct runner on catalog def |

Fixture: [03](./03-mini-overmap-grid.md) 8×8 with one `house_09_north` cell.

---

## BN source reference

| Concern | Location |
| --- | --- |
| `draw_map` | `src/map.cpp` |
| `oter_mapgen` | `src/mapgen.cpp` |
| Weighted pick | `weighted_mapgen_function_list` |

---

## Verification

1. Click OMT in W2 UI loads 24×24 grid
2. Rotation suffix on OMT id affects grid (P14 rotator)
3. Second visit same cell returns cached grid (identity)
4. Changing world seed changes grid
