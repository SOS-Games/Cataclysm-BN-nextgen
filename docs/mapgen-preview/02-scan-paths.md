# 02 — Scan paths and JSON discovery

Where mapgen and palette JSON live in BN `data/`, how nextgen scans them, and how results
feed P1–P3.

---

## Purpose

Discover `type: palette` and `type: mapgen` objects without loading the entire `data/json/`
tree into unrelated registries. Reuse [game data mod order](../game-data-loader/09-mod-load-order.md)
so modded palettes override core.

---

## BN layout

```text
data/json/
  mapgen/                    # ~650 files, ~many thousand mapgen objects
    house/
    lab/
    city_blocks/
    …
  mapgen_palettes/           # shared palette definitions
    house_general_palette.json   # standard_domestic_palette, …
    military/
    …
  mapgen/bridges.json        # occasional inline type: palette
```

Approximate scale (core `bn` mod, 2026):

| Tree | Files (order of magnitude) | Primary `type` |
| --- | --- | --- |
| `mapgen/` | 650+ | `mapgen` |
| `mapgen_palettes/` | 50+ | `palette` |

Mapgen entries reference palettes by **id string** (`"palettes": [ "standard_domestic_palette" ]`),
not file path. The same id may only be defined once per effective mod load order.

---

## Scan algorithm (planned)

```text
PaletteLoader.load(options):
    registry ← empty PaletteRegistry
    warnings ← []

    modIds ← ModOrderResolver.resolve(options.modIds, ModDiscovery.discover(roots))
    for modId in modIds:
        mod ← registry.find(modId)
        if mod missing: continue
        content ← mod.resolvedContentPath

        if options.includePaletteTree:
            scanDir(content / "mapgen_palettes", registry, warnings)

        if options.includeMapgenTree:
            // deferred to JsonMapgenLoader — separate catalog
            pass

    return MapgenLoadResult(registry, warnings)
```

### Per-directory scan

```text
scanDir(dir, registry, warnings):
    if !isDirectory(dir): return
    for file in sorted recursive *.json under dir:
        try:
            for object in JsonDataScanner.parseFile(file):
                dispatch(object, file, registry, catalog, warnings)
        catch IOException | JsonError:
            warnings.add(file + ": " + message)
```

### Dispatch

| `object.type` | Handler | Storage |
| --- | --- | --- |
| `palette` | `PaletteLoader.parsePalette` | `PaletteRegistry.put(id, …)` |
| `mapgen` | `JsonMapgenLoader.parseMapgen` | `MapgenCatalog.add(…)` |
| other | skip | — |

Reuse [JSON envelope rules](../game-data-loader/04-json-dispatch.md) from `JsonDataScanner`.

---

## `MapgenScanOptions` (planned)

```java
public final class MapgenScanOptions {
    private final List<Path> dataRoots;
    private final List<String> modIds;           // empty → core-only via ModOrderResolver
    private final boolean includeMapgenTree;     // default true
    private final boolean includePaletteTree;    // default true
    private final boolean includeInlinePalettes; // scan mapgen/ for type:palette — default false v1

    public static MapgenScanOptions defaults() {
        return new MapgenScanOptions(
            DataPaths.gameDataRoots(),
            Collections.emptyList(),
            true,
            true,
            false
        );
    }
}
```

### Scan roots per mod

| Flag | Path under `mod.resolvedContentPath` |
| --- | --- |
| `includePaletteTree` | `mapgen_palettes/` |
| `includeMapgenTree` | `mapgen/` |
| `includeInlinePalettes` | `mapgen/` (palette objects only) |

**Not scanned:** `overmap_terrain/`, `region_settings/`, `furniture_and_terrain/` (game data loader).

---

## Mod override semantics

Same as terrain: later mod in `ModOrderResolver.resolve` list wins on duplicate **palette id**
or duplicate **mapgen identity**.

Mapgen identity for catalog:

```text
key = (om_terrain_primary, sourceFile, indexInFile)
```

Multiple mapgens may share an `om_terrain` (different weights) — catalog keeps **all** entries;
preview UI lists each with weight.

---

## `MapgenCatalog` index

```java
public final class MapgenCatalog {
    private final List<JsonMapgenDefinition> definitions;
    private final Map<String, List<JsonMapgenDefinition>> byOmTerrain;

    public List<JsonMapgenDefinition> all();
    public List<JsonMapgenDefinition> filter(String query);
    public List<JsonMapgenDefinition> findByOmTerrain(String omTerrainId);
    public List<JsonMapgenDefinition> fromFile(Path jsonFile);
}
```

### `om_terrain` indexing

JSON forms:

```json
"om_terrain": "house_09"
"om_terrain": [ "house_09", "house_09_ab" ]
```

Index each string id. Search query matches id, filename, or path substring.

### Performance (v1)

| Operation | Expected cost |
| --- | --- |
| Full palette scan | < 200 ms on SSD, core mod |
| Full mapgen index | < 2 s (parse metadata only — defer full `object` parse if needed) |
| Re-scan each editor open | Acceptable v1; cache in `MapgenPreviewService` singleton |

v2: lazy catalog + background thread (no OpenGL in loader).

---

## Integration with `GameDataLoader`

| Approach | Pros | Cons |
| --- | --- | --- |
| **Separate** `MapgenPreviewService` | Clear boundaries | Second scan pass |
| Extend `loadMods` dispatch | Single walk | Mixes concerns |

**Recommendation:** separate `PaletteLoader` / `JsonMapgenLoader` invoked from preview UI or
`MapgenPreviewService.loadAll(options)` — do not add mapgen to `GameDataLoader.loadCore` hot path.

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Missing `mapgen_palettes/` | Empty palette registry; inline `object.terrain` still works |
| Missing `mapgen/` | Empty catalog |
| JSON parse error | Skip file; add to `warnings` list |
| `mapgen` with `method: builtin` | Index with `supported=false` |
| Unknown palette id at run time | Runner warns; see [05](./05-rows-runner.md) |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Recursive JSON scan | `src/init.cpp` — `load_data_from_path` |
| Mapgen dispatch | `src/mapgen.cpp` — `load_mapgen` |
| Palette dispatch | `src/mapgen.cpp` — `mapgen_palette::load` |
| File sort order | `cata::find_file_paths` — lexical |

---

## Inputs

- `data/` roots from `DataPaths`
- Mod list from `ModOrderResolver`

## Outputs

- `PaletteRegistry`
- `MapgenCatalog` (or lazy loader)
- `List<String> scanWarnings`

## Verification

1. Fixture scan finds `standard_domestic_palette` under `mapgen_palettes/`
2. Fixture finds json mapgen in `mapgen/house/house09.json`
3. Scan does not require `TerrainRegistry` loaded first
4. Mod override fixture: patch mod replaces palette char mapping
5. Scan warnings include path on parse failure
6. `defaults()` uses same `dataRoots` as `GameDataLoadOptions.defaults()`
