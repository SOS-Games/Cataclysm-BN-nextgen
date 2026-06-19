# 14 — Mod scan paths (alt layouts)

Where mapgen and palette JSON live **outside** core `data/json/mapgen/`, and how nextgen discovers them.

**Status:** done (P8). Supplements [02](./02-scan-paths.md).

---

## Purpose

BN core packs use `mapgen/` and `mapgen_palettes/`. Some mods colocate overmap terrain,
specials, mapgen, and palettes under custom directories. Without scanning those trees,
buildings appear in the picker (via [BuildingBundleScanner](./13-building-bundle-sources.md)) but
**no runnable mapgen** exists in `MapgenCatalog` → import fails with “produced no runnable floors”.

**Real failure (2026-06):** `arcana_anomaly_resurgence` — `overmap_special` in
`overmap_and_mapgen/overmap_specials.json`, mapgen in `mapgen_structure_anomalous.json`, zero
entries under `mods/Arcana_BN/mapgen/`.

---

## BN layout patterns

### Core `bn` mod

```text
data/json/
  mapgen/                    # ~650 files, type: mapgen
    house/house09.json
    lab/...
  mapgen_palettes/           # type: palette
    house_general_palette.json
```

BN `init.cpp` walks the entire `data/json` tree; mapgen dispatch is type-based, not path-based.
Nextgen intentionally scans **named subtrees** only ([02](./02-scan-paths.md)) for speed.

### Arcana (`Arcana` mod id)

```text
data/mods/Arcana_BN/
  modinfo.json
  overmap_and_mapgen/
    mapgen_palettes.json              # [ { type: palette, id: arcana_palette }, ... ]
    mapgen_structure_anomalous.json   # buffer + entrance + underground
    mapgen_variants.json              # microlab_arcana_surface, etc.
    mapgen_cleansingflame.json
  overmap_terrain.json
  overmap_specials.json               # arcana_anomaly_resurgence, 4x4_microlab_arcana, ...
```

All mapgen objects share one directory with overmap defs — common for total-conversion mods.

### Other mods

| Pattern | Example | Scanned by P8? |
| --- | --- | --- |
| Standard | Most core-style mods | `mapgen/` only |
| Colocated pack | Arcana `overmap_and_mapgen/` | ✓ |
| Inline palette in mapgen file | `data/json/mapgen/bridges.json` | `includeInlinePalettes` on `mapgen/` |
| Random path | `quests/custom_mapgen.json` | ✗ — not scanned |

---

## Scan algorithm (implemented)

```text
JsonMapgenLoader.load(options):
    modRegistry ← ModDiscovery.discover(dataRoots)
    for modId in ModOrderResolver.resolve(options.modIds, modRegistry):
        mod ← modRegistry.find(modId)
        if mod == null: continue
        content ← mod.resolvedContentPath
        if options.includeMapgenTree:
            for dir in MapgenScanRoots.mapgenDirs():   // "mapgen", "overmap_and_mapgen"
                loadFromTree(content / dir, definitions, warnings)

PaletteLoader.load(options):
    for modId in ...:
        content ← mod.resolvedContentPath
        if options.includePaletteTree:
            loadPalettesFromTree(content / "mapgen_palettes", ...)
            loadPalettesFromTree(content / "overmap_and_mapgen", ...)
        if options.includeInlinePalettes:
            loadPalettesFromTree(content / "mapgen", ...)   // palette objects only
```

### Per-file dispatch

Same as [02](./02-scan-paths.md):

| `object.type` | Handler |
| --- | --- |
| `mapgen` | `JsonMapgenParser` → `MapgenCatalog` |
| `palette` | `PaletteParser` → `PaletteRegistry` |
| other | skip |

`JsonDataScanner.listJsonFiles` recurses; mixed arrays (Arcana `mapgen_palettes.json`) are fine.

---

## Alignment with bundle discovery

| Scanner | Scope | Types |
| --- | --- | --- |
| `BuildingBundleScanner` | **All** JSON under mod content root | `city_building`, `overmap_special` |
| `JsonMapgenLoader` | `mapgen/` + `overmap_and_mapgen/` | `mapgen` |
| `PaletteLoader` | palette dirs above | `palette` |

**Rule:** Any mod id in `ModConfiguration.activeModIds()` must be scanned by **both** bundle and
mapgen loaders. `MapgenScanOptions.defaults()` uses the same mod list as game data.

---

## Java types (implemented)

```java
// io.gdx.cdda.bn.nextgen.mapgen.MapgenScanRoots
public final class MapgenScanRoots {
    public static List<String> mapgenDirs() {
        return List.of("mapgen", "overmap_and_mapgen");
    }
}
```

```java
// JsonMapgenLoader — per mod, both dirs
for (final String dir : MapgenScanRoots.mapgenDirs()) {
    loadFromTree(contentRoot.resolve(dir), definitions, warnings);
}
```

---

## Mod override semantics

Unchanged from [02](./02-scan-paths.md):

- **Palette id:** later mod in load order overwrites same id in `PaletteRegistry.put`
- **Mapgen:** catalog keeps **all** entries; identity = `(om_terrain, sourceFile, indexInFile)`
- **Runnable:** `findFirstRunnableByOmTerrain` returns first json+rows match in catalog order

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| `overmap_and_mapgen/` missing | Skip silently (empty dir) |
| JSON parse error | `warnings.add("failed to parse mapgen file …")` |
| `type: mapgen` without `object.rows` | Indexed but `isJsonPreviewSupported() == false` |
| Mapgen only in `furniture_and_terrain/` | Not scanned — out of scope |
| User mod list excludes Arcana | Special visible only if registered elsewhere; mapgen missing |

---

## Test fixtures

```text
core/src/test/resources/mapgen-fixtures/
  mods/
    bn/modinfo.json                    # path: ../../json
    alt_mapgen_mod/
      modinfo.json                     # path: .
      overmap_and_mapgen/
        alt_layout.json                # palette + mapgen + overmap_special
```

`AltMapgenLayoutScanTest` loads `List.of("bn", "alt_mapgen_mod")` and asserts catalog + import.

---

## Future: full mod walk (optional)

`BuildingBundleScanner` pattern for mapgen:

```text
scanModContent(contentRoot):
    for file in allJsonFiles(contentRoot):
        dispatch type mapgen / palette
```

**Pros:** Finds mapgen in any path. **Cons:** Larger scan; duplicates if both walk and subtree
run. Defer until a second mod pack proves necessary.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Full data walk | `src/init.cpp` — `load_data_from_path` |
| Mapgen type dispatch | `src/mapgen.cpp` — `load_mapgen` |
| File discovery | `cata::find_file_paths` |

---

## Verification

1. Fixture `alt_mapgen_mod` → `findFirstRunnableByOmTerrain("alt_layout_ground")` present
2. `PaletteLoader` → `contains("alt_layout_palette")`
3. `generateBuilding(alt_layout_duplex)` → 2 floors, non-empty grids
4. Integration: `arcana_structure_anomalous_entrance` in catalog when Arcana enabled
5. Core-only scan: `house09` still from `data/json/mapgen/` — no regression
6. `MapgenPreviewService.ensureLoaded` must be called again after code change (catalog cached)
