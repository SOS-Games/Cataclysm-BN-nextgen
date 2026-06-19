# 02 — Scan paths and JSON discovery

Where mapgen and palette JSON live in BN `data/`, how nextgen scans them, and how results
feed the mapgen preview loaders.

**Status:** implemented (P1–P2, P8 alt paths). Alt-layout detail: [14-mod-scan-paths.md](./14-mod-scan-paths.md).

---

## Purpose

Discover `type: palette` and `type: mapgen` objects without loading the entire `data/json/`
tree into unrelated registries. Reuse [game data mod order](../game-data-loader/09-mod-load-order.md)
so modded palettes override core.

Building bundles (`city_building`, `overmap_special`) use a **wider** scan — see
[13-building-bundle-sources.md](./13-building-bundle-sources.md) — but mapgen **catalog** and
**palette** registries use the paths below.

---

## BN layout

### Core `bn` mod

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

BN itself walks all of `data/json/` by type (`init.cpp`); nextgen scans **named subtrees** for
speed and clear boundaries.

### Mod colocated packs (P8)

Some mods place mapgen beside overmap defs instead of under `mapgen/`:

```text
data/mods/Arcana_BN/
  overmap_and_mapgen/
    mapgen_palettes.json           # type: palette (e.g. arcana_palette)
    mapgen_structure_anomalous.json
    mapgen_variants.json
    overmap_terrain.json
    overmap_specials.json          # bundles only — not mapgen catalog
```

**Important:** `overmap_specials.json` is indexed by `BuildingBundleScanner`, not
`JsonMapgenLoader`. Mapgen scan only ingests `type: mapgen` and `type: palette` objects from
the same directory.

Approximate scale (core `bn` mod, 2026):

| Tree | Files (order of magnitude) | Primary `type` |
| --- | --- | --- |
| `mapgen/` | 650+ | `mapgen` |
| `mapgen_palettes/` | 50+ | `palette` |
| `overmap_and_mapgen/` (mods) | varies | `mapgen`, `palette` mixed in same files |

Mapgen entries reference palettes by **id string** (`"palettes": [ "standard_domestic_palette" ]`),
not file path. The same id may only be defined once per effective mod load order.

---

## Scan algorithm (implemented)

```text
PaletteLoader.load(options):
    registry ← empty PaletteRegistry
    modIds ← ModOrderResolver.resolve(options.modIds, ModDiscovery.discover(roots))
    for modId in modIds:
        content ← mod.resolvedContentPath
        if includePaletteTree:
            scanDir(content / "mapgen_palettes", registry)
            scanDir(content / "overmap_and_mapgen", registry)    // P8
        if includeInlinePalettes:
            scanDir(content / "mapgen", registry)   // palette objects only

JsonMapgenLoader.load(options):
    definitions ← []
    for modId in modIds:
        content ← mod.resolvedContentPath
        if includeMapgenTree:
            for dir in [ "mapgen", "overmap_and_mapgen" ]:      // MapgenScanRoots
                scanDir(content / dir, definitions)

    return MapgenCatalog(definitions)
```

`MapgenPreviewService.ensureLoaded` runs both loaders once and caches results.

### Per-directory scan

```text
scanDir(dir, …):
    if !isDirectory(dir): return silently
    for file in JsonDataScanner.listJsonFiles(dir, recursive):
        for object in JsonDataScanner.parseFile(file):
            dispatch by object.type
```

Mixed JSON arrays (e.g. Arcana `mapgen_palettes.json` with palette + mapgen in one file) are
supported — each array element dispatches independently.

### Dispatch

| `object.type` | Handler | Storage |
| --- | --- | --- |
| `palette` | `PaletteParser` | `PaletteRegistry.put(id, …)` |
| `mapgen` | `JsonMapgenParser` | `MapgenCatalog` list + `byOmTerrain` index |
| other | skip | — |

Reuse [JSON envelope rules](../game-data-loader/04-json-dispatch.md) from `JsonDataScanner`.

---

## `MapgenScanOptions`

```java
public final class MapgenScanOptions {
    private final List<Path> dataRoots;
    private final List<String> modIds;           // empty → ModOrderResolver adds core "bn"
    private final boolean includeMapgenTree;     // default true
    private final boolean includePaletteTree;    // default true
    private final boolean includeInlinePalettes; // mapgen/ palettes only — default false

    public static MapgenScanOptions defaults() {
        return new MapgenScanOptions(
            DataPaths.gameDataRoots(),
            ModConfiguration.activeModIds(),
            true,
            true,
            false
        );
    }
}
```

`defaults()` uses the same mod list as the map editor (saved prefs or `mods/default.json`).

### Scan roots per mod

| Flag | Path under `mod.resolvedContentPath` |
| --- | --- |
| `includePaletteTree` | `mapgen_palettes/`, `overmap_and_mapgen/` |
| `includeMapgenTree` | `mapgen/`, `overmap_and_mapgen/` |
| `includeInlinePalettes` | `mapgen/` (palette objects only) |

Java helper:

```java
// io.gdx.cdda.bn.nextgen.mapgen.MapgenScanRoots
MapgenScanRoots.mapgenDirs()  // → ["mapgen", "overmap_and_mapgen"]
```

**Not scanned for mapgen/palette:** `overmap_terrain/` as a dedicated tree name, `region_settings/`,
`furniture_and_terrain/` — those belong to the game data loader. Files under
`overmap_and_mapgen/` **are** scanned when the flag is on.

**Not scanned anywhere:** ad-hoc paths like `quests/foo.json` unless the mod uses standard dirs or
[BuildingBundleScanner](./13-building-bundle-sources.md) finds bundle metadata there.

Further examples and failure cases: [14-mod-scan-paths.md](./14-mod-scan-paths.md).

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
| Missing `mapgen_palettes/` | Empty palette registry for that path; inline `object.terrain` still works |
| Missing `mapgen/` | No core mapgen from that path; mod may still supply `overmap_and_mapgen/` |
| Missing `overmap_and_mapgen/` | Skip silently (same as empty dir) — **P8** |
| Mod enabled but mapgen only in non-standard path | Bundle may appear in picker; import fails until mapgen found or P8 path added |
| JSON parse error | Skip file; add to `warnings` list |
| `mapgen` with `method: builtin` | Indexed; `isJsonPreviewSupported() == false` |
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
2. Fixture finds json mapgen in `mapgen/house/house09.json` (or fixture `simple_room.json`)
3. **P8:** `alt_mapgen_mod/overmap_and_mapgen/` → catalog + palette (`AltMapgenLayoutScanTest`)
4. Integration: Arcana enabled → `arcana_structure_anomalous_entrance` in catalog
5. Scan does not require `TerrainRegistry` loaded first
6. Mod override fixture: patch mod replaces palette char mapping
7. Scan warnings include path on parse failure
8. `defaults()` uses same `dataRoots` and mod list as map editor (`ModConfiguration`)
