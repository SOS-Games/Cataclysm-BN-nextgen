# 03 — Palette loader

Load BN `type: palette` JSON into char → terrain/furniture maps for the rows runner.

BN analogue: `mapgen_palette::load_internal` + value resolution in `mapgen_value`.

---

## Purpose

Mapgen `rows` strings use symbols (usually one UTF-8 code point per cell). Palettes map each
symbol to `t_*` / `f_*` ids. Shared palettes like `standard_domestic_palette` avoid repeating
hundreds of mappings in every building file.

---

## JSON shape

```json
{
  "type": "palette",
  "id": "standard_domestic_palette",
  "palettes": [ "other_palette" ],
  "parameters": { "interior_wall_type": { "type": "ter_str_id", "default": { … } } },
  "terrain": {
    ".": "t_region_groundcover_urban",
    "#": "t_brick_wall",
    "+": [ [ "t_door_c", 5 ], [ "t_door_o", 5 ], [ "t_door_locked_interior", 1 ] ],
    "|": { "param": "interior_wall_type", "fallback": "t_wall_w" },
    "o": [ [ "t_window_domestic", 10 ], "t_window_open" ]
  },
  "furniture": {
    "H": "f_sofa",
    "h": "f_chair",
    "y": [ "f_indoor_plant", "f_indoor_plant_y" ]
  },
  "items": { … },
  "liquids": { … },
  "toilets": { "t": { } }
}
```

Source: `data/json/mapgen_palettes/house_general_palette.json`

---

## Identity fields

| Field | Required | Notes |
| --- | --- | --- |
| `type` | yes | `"palette"` |
| `id` | yes | Global registry key |
| `palettes` | no | Parent palette ids — **v1 skip** (see [08](./08-v2-parity-roadmap.md)) |
| `parameters` | no | **v1 skip** — use `fallback` on param objects only |

---

## Mapping sections

BN `load_internal` reads many placing sections. v1 loads only:

| JSON key | Maps to | v1 |
| --- | --- | --- |
| `terrain` | `ter_str_id` | **Load** |
| `furniture` | `furn_str_id` | **Load** |
| `items`, `item` | item groups | Skip |
| `liquids` | liquid amounts | Skip |
| `toilets` | toilet fields | Skip |
| `monsters`, `monster`, `vehicles`, `traps`, … | spawn specs | Skip |
| `nested`, `translate`, `zones`, … | advanced | Skip |

---

## Value resolution (`PaletteCharResolver`)

BN accepts multiple JSON forms per char key. v1 deterministic rules:

| Form | Example | v1 result |
| --- | --- | --- |
| String | `"#": "t_brick_wall"` | `"t_brick_wall"` |
| Array of strings | `"y": [ "f_a", "f_b" ]` | `"f_a"` (first) |
| Weighted pairs | `[ [ "t_door_c", 5 ], "t_door_o" ]` | `"t_door_c"` (first entry’s id) |
| Param object | `{ "param": "interior_wall_type", "fallback": "t_wall_w" }` | `"t_wall_w"` |
| Bare string in weighted | `"t_window_open"` | `"t_window_open"` |

```java
public final class PaletteCharResolver {
    public static Optional<String> resolveTerOrFurn(JsonValue value);
}
```

Log at `FINE` when collapsing weighted/param forms.

### Char keys

- JSON object keys are strings; BN uses `map_key` (UTF-8 aware)
- v1: use **first code point** of key string (`key.codePointAt(0)`)
- Keys longer than one code point: use first code point + warn (rare; e.g. multi-byte symbols)

---

## `MapgenPalette` model

```java
public final class MapgenPalette {
    private final String id;
    private final Map<Integer, String> terrainByCodePoint;   // Unicode code point → t_*
    private final Map<Integer, String> furnitureByCodePoint; // → f_*

    public Optional<String> terrainForCodePoint(int codePoint);
    public Optional<String> furnitureForCodePoint(int codePoint);
}
```

Use `Integer` code point keys to avoid surrogate-pair bugs for symbols like `₸` in domestic palette.

---

## `PaletteRegistry`

```java
public final class PaletteRegistry {
    public void put(MapgenPalette palette);  // replaces same id
    public Optional<MapgenPalette> find(String id);
    public int size();
    public List<String> allIds();

    /** Merge palettes for mapgen run — does not include mapgen inline overrides. */
    public MergedCharMap merge(List<String> paletteIds, List<String> warnings);
}
```

### Merge algorithm

```text
merge(paletteIds):
    mergedTer ← empty map
    mergedFurn ← empty map
    for id in paletteIds:
        pal ← find(id)
        if pal empty:
            warnings += "unknown palette: " + id
            continue
        for (cp, ter) in pal.terrain: mergedTer.put(cp, ter)   // later overwrites
        for (cp, furn) in pal.furniture: mergedFurn.put(cp, furn)
    return MergedCharMap(mergedTer, mergedFurn)
```

Matches BN order: mapgen `object.palettes` array merged in sequence via `mapgen_palette::add`.

---

## Loader algorithm

```text
PaletteLoader.load(MapgenScanOptions options):
    registry ← new PaletteRegistry()
    warnings ← []

    for mod in orderedMods(options):
        root ← mod.contentPath / "mapgen_palettes"
        for file in sortedJsonFiles(root):
            for obj in parseFile(file):
                if obj.type != "palette": continue
                palette ← parsePalette(obj.root)
                if palette.id empty: warnings++; continue
                registry.put(palette)

    return new MapgenLoadResult(registry, warnings)
```

`parsePalette` walks `terrain` and `furniture` objects via `PaletteCharResolver`.

---

## Regional terrain tokens

Palettes reference ids like `t_region_groundcover_urban`. BN resolves these at mapgen time
using world region. v1:

- Store literal string in `MergedCharMap`
- Gfx may lack tile — validation [V5](../game-data-loader/10-post-load-validation.md) warns
- v2: optional `RegionTerrainResolver` ([08](./08-v2-parity-roadmap.md))

---

## Validation (optional, after load)

With `LoadedGameData` available:

```text
for each ter/furn id in all palettes:
    if !registry.contains(id): warn "[palette] unknown ter/furn id"
```

Duplicate char in same section during parse: **last JSON key wins** (LibGDX `JsonValue` iteration order).

---

## BN source reference

| Concern | Location |
| --- | --- |
| Load | `src/mapgen.cpp` — `mapgen_palette::load`, `load_internal` |
| Merge | `src/mapgen.cpp` — `mapgen_palette::add` |
| Terrain placing | `load_place_mapings<jmapgen_terrain>` |
| Furniture placing | `load_place_mapings<jmapgen_furniture>` |
| Apply char | `src/mapgenformat.cpp` — `format_effect::translate` |

---

## Inputs

- JSON under `mapgen_palettes/` per mod content path

## Outputs

- `PaletteRegistry`
- Parse warnings

## Failure modes

| Condition | Behavior |
| --- | --- |
| Missing `id` | Skip object + warn |
| Empty `terrain` and `furniture` | Store empty palette (valid but useless) |
| Unresolvable value | Skip that char + warn |
| Duplicate palette id across mods | Later mod wins (no BN-style duplicate rejection) |

## Verification

1. Load `standard_domestic_palette` from BN checkout when `cdda.data.roots` set
2. `"#"` → `t_brick_wall` (verify against current JSON — ids may change between BN versions)
3. `|` param entry → `t_wall_w` fallback
4. Weighted `+` door array → first variant deterministically
5. Unit: merge `palette_a` + `palette_b` → `b` wins on shared char
6. Unit: unknown palette id in merge → warning, partial merge
