# 10 — City building loader

Load BN **`city_building`** metadata and resolve each piece to a `JsonMapgenDefinition` in
`MapgenCatalog`.

---

## Purpose

Bridge `multitile_city_buildings.json` → list of **pieces** (z, OMT offset, mapgen def) for
`MapVolumeBuilder` ([11](./11-map-volume-and-floors.md)) and `OmtStitchComposer`
([12](./12-omt-stitch-composer.md)).

---

## On-disk input

```text
data/json/overmap/multitile_city_buildings.json   # array of city_building objects
data/json/overmap/overmap_terrain/*.json        # optional — mapgen / id lists
```

Only objects with `"type": "city_building"` are indexed. Other types in the same file (if any)
are skipped with optional log.

### Example entry

```json
{
  "type": "city_building",
  "id": "house_09",
  "locations": [ "land" ],
  "overmaps": [
    { "point": [ 0, 0, 0 ], "overmap": "house_09_north" },
    { "point": [ 0, 0, 1 ], "overmap": "house_09_roof_north" },
    { "point": [ 0, 0, -1 ], "overmap": "basement_bionic_north" }
  ]
}
```

---

## Scan algorithm

```text
loadCityBuildings(scanOptions):
    buildings ← empty map id → CityBuildingDefinition
    for file in sorted(overmap/multitile_city_buildings.json, other multitile*.json):
        for obj in parseArray(file):
            if obj.type != "city_building": continue
            def ← parseCityBuilding(obj, file)
            if buildings.contains(def.id):
                warn duplicate id; last wins OR first wins (document: first wins)
            buildings.put(def.id, def)
    return CityBuildingRegistry(buildings, warnings)
```

**Mod order:** same as [02-scan-paths](./02-scan-paths.md) — later mod overrides building id.

---

## `CityBuildingPiece`

```java
public final class CityBuildingPiece {
    private final int offsetX;      // point[0] — OMT tiles east
    private final int offsetY;      // point[1] — OMT tiles south
    private final int zLevel;       // point[2]
    private final String overmapId; // raw, e.g. house_09_north
    private final String resolvedOmTerrain; // after resolver, e.g. house_09
}
```

---

## `CityBuildingDefinition`

```java
public final class CityBuildingDefinition {
    private final String id;              // city_building id, e.g. house_09
    private final Path sourceFile;
    private final List<CityBuildingPiece> pieces;

    public List<Integer> distinctZLevels() { … }  // sorted
    public List<CityBuildingPiece> piecesAtZ(int z) { … }
    public boolean isMultiTileAtZ(int z) {
        return piecesAtZ(z).size() > 1
            || piecesAtZ(z).stream().anyMatch(p -> p.offsetX != 0 || p.offsetY != 0);
    }
}
```

---

## Overmap id → mapgen resolution

### Step 1 — Strip rotation suffix

Known suffixes (v1): `_north`, `_south`, `_east`, `_west`, `_north_east`, `_north_west`,
`_south_east`, `_south_west` (longest match first).

```text
stripRotation("house_09_north") → "house_09"
stripRotation("2StoryModern04_1_2_north") → "2StoryModern04_1_2"
```

Rotation of **rows** is not applied in v1.5; preview shows unrotated json mapgen (same as
[01](./01-overview-and-scope.md)).

### Step 2 — Catalog lookup

```text
resolveMapgen(catalog, overmapId):
    candidates ← [ overmapId, stripRotation(overmapId) ]
    for c in candidates:
        def ← catalog.findByOmTerrain(c)
        if def != null && def.isJsonPreviewSupported(): return def
    return null
```

`MapgenCatalog.findByOmTerrain` may already exist or needs index by first `om_terrain` string.

### Step 3 — Overmap terrain file (optional v1.5b)

If catalog miss, scan `overmap_terrain` for object whose `id` array contains candidate:

```json
{
  "type": "overmap_terrain",
  "id": [ "house_09", "house_09_roof", … ],
  "mapgen": [ { "method": "json", "om_terrain": "house_09" } ]
}
```

Extract `om_terrain` from first `method: json` entry; lookup catalog again. Builtin-only entries
→ warn, skip piece.

---

## Link picker entry → building

When user highlights a catalog row with `om_terrain` = `house_09`:

```text
building ← registry.findByOmTerrain("house_09")
// match if any piece resolves to same om_terrain OR building.id equals om_terrain stem
```

Picker shows extra action **Import building** when `building != null`.

Also match `house_09_roof` → same `house_09` building (any piece’s resolved om terrain).

---

## `CityBuildingLoader` API

```java
public final class CityBuildingLoader {

    public CityBuildingRegistry load(MapgenScanOptions options) throws IOException;

    public static final class CityBuildingRegistry {
        Optional<CityBuildingDefinition> findById(String cityBuildingId);
        Optional<CityBuildingDefinition> findByOmTerrain(String omTerrain);
        List<CityBuildingDefinition> all();
    }
}
```

Loaded alongside palettes/catalog in `MapgenPreviewService.ensureLoaded()` (extend scan or
lazy-load on first picker open).

---

## Fixtures

```text
core/src/test/resources/mapgen-fixtures/
  overmap/minimal_city_building.json
```

```json
[
  {
    "type": "city_building",
    "id": "test_duplex",
    "overmaps": [
      { "point": [ 0, 0, 0 ], "overmap": "simple_room_ground_north" },
      { "point": [ 0, 0, 1 ], "overmap": "simple_room_roof_north" }
    ]
  }
]
```

Pair with two mapgen fixture files whose `om_terrain` match stripped ids.

---

## Inputs

- `MapgenScanOptions` (data roots, mod order)
- `MapgenCatalog` (already loaded)

## Outputs

- `CityBuildingRegistry`
- `List<String>` warnings

## Failure modes

| Condition | Behavior |
| --- | --- |
| File missing | Empty registry; no error |
| Malformed `point` | Skip piece; warn |
| Unresolved overmap | Warn; piece omitted from bundle |
| Empty building after filter | Import building disabled for that id |

## Verification

1. Parse fixture; `findById("test_duplex")` has 2 pieces, z=0 and z=1
2. `stripRotation` unit tests for eight directions
3. `findByOmTerrain("house_09")` finds BN `house_09` when sibling data present
4. Duplicate building id policy tested

---

## BN source reference

| Concern | Location |
| --- | --- |
| Load multitile | `src/overmap.cpp` — city building registration |
| Rotation names | `src/omt_id.h`, `src/overmap.cpp` |

---

## Related

- [09-building-bundles-overview](./09-building-bundles-overview.md)
- [13-building-bundle-sources](./13-building-bundle-sources.md) — `overmap_special` stacks + scan gaps
- [11-map-volume-and-floors](./11-map-volume-and-floors.md)
