# 04 — JSON mapgen format

BN `type: mapgen` objects with `method: json` — schema subset, load path, and catalog fields.

Authoritative BN docs: `docs/en/mod/json/reference/mapgen.md` (Cataclysm-BN repo).

---

## Purpose

Define what nextgen parses from mapgen JSON, what it stores in `JsonMapgenDefinition`, and what
the runner consumes from `object`.

---

## Top-level mapgen object

```json
{
  "type": "mapgen",
  "method": "json",
  "om_terrain": [ "house_09" ],
  "weight": 300,
  "disabled": false,
  "object": {
    "fill_ter": "t_floor",
    "rows": [ "...", "..." ],
    "palettes": [ "standard_domestic_palette" ],
    "terrain": { "#": "t_adobe_brick_wall" },
    "place_monsters": [ … ]
  }
}
```

### Top-level fields

| Field | Type | v1 | Notes |
| --- | --- | --- | --- |
| `type` | string | required | `"mapgen"` |
| `method` | string | required | `"json"` for preview |
| `object` | object | required | Layout spec |
| `om_terrain` | string or string[] | index | Catalog key; BN uses for OMT matching |
| `weight` | int | store | Default 1000; BN weighted pick |
| `disabled` | bool | filter | If true, exclude from catalog |
| `name` | string | store | Optional debug label |

### Unsupported `method` (v1)

| `method` | BN | Preview |
| --- | --- | --- |
| `builtin` | C++ function pointer by `name` | `supported=false` |
| `lua` | `luamethod` string | `supported=false` |

---

## BN load path (reference)

```text
load_mapgen(jo):
    if jo.method == "json":
        f ← load_mapgen_function(jo)   // reads object subtree
        for id in jo.om_terrain:
            oter_mapgen.add(id, f)

load_mapgen_function:
    if method == "json":
        return mapgen_function_json(object_source_location, weight, …)
        // defers parsing object until setup_common at generation time
```

Nextgen parses `object` eagerly at scan or first run — acceptable for preview.

---

## `object` — layout spec

### Core fields (v1 runner)

| Field | Type | Notes |
| --- | --- | --- |
| `fill_ter` | `ter_str_id` | Fills entire grid before rows (BN `draw_fill_background`) |
| `rows` | string[] | One row per line; see [05](./05-rows-runner.md) |
| `palettes` | string[] | Merged in order via `PaletteRegistry` |
| `terrain` | object | Char → ter id; merged **after** palettes |
| `furniture` | object | Char → furn id; merged **after** palettes |

Example (`house09.json`):

```json
"object": {
  "fill_ter": "t_floor",
  "rows": [
    "............%%%...-p%%%.",
    "...........####oo#*####.",
    "...........#H  E     r#."
  ],
  "palettes": [
    "standard_domestic_palette",
    "standard_domestic_landscaping_palette"
  ],
  "terrain": { "#": "t_adobe_brick_wall" }
}
```

### BN `setup_common` validation (reference)

When `rows` present, BN (`mapgen_function_json_base::setup_common`):

1. Builds temporary palette from `object` via `mapgen_palette::load_temp(jo)` — includes
   `palettes`, `terrain`, `furniture` on the **same** object
2. Requires every non-space char in `rows` to have terrain **or** placing **or** `fill_ter`
3. For standard OMT mapgen, expects row count = 24 and each row length = 24
4. Registers `jmapgen_place` per cell for all `format_placings` at that char

**Preview v1** relaxes fixed 24×24 — grid size = max row width × row count.

### Deferred `object` fields

| Field | BN role |
| --- | --- |
| `place_monsters`, `place_npc`, `place_vehicles` | Spawn entities |
| `place_items`, `place_item`, `add` | Item spawn |
| `place_terrain`, `place_furniture` | Random rectangles |
| `place_traps`, `place_fields`, `place_toilets`, … | Special handlers |
| `set` | Post-rows bulk mutations |
| `nested` | Nested mapgen chunks |
| `rotation` | Submap rotation 0–3 |
| `predecessor_mapgen` | Run another OMT mapgen first |
| `mapgensize` | Nested only — `[w, h]` |

---

## Row encoding

| Rule | Detail |
| --- | --- |
| Indexing | `rows[y]` is row `y`; `x` = column index in string |
| Width | `max(rows[i].length)` over all rows |
| Short rows | Trailing cells on that row keep `fill_ter` |
| Space ` ` | Valid symbol — often outdoor ground via palette `.` or ` ` mapping |
| Unicode | BN splits with `utf8_display_split` — v1 first code point per cell |
| Newlines | Not embedded in JSON strings — one array element per row |

BN submap origin = `(0,0)`; preview aligns with `MapGrid` top-left.

---

## Multiple mapgens per file

Common pattern — ground + roof:

```json
[
  { "type": "mapgen", "om_terrain": "house_09", "object": { "fill_ter": "t_floor", … } },
  { "type": "mapgen", "om_terrain": "house_09_roof", "object": { "fill_ter": "t_shingle_flat_roof", … } }
]
```

Catalog stores **separate** `JsonMapgenDefinition` per array index. UI labels:

```text
house_09  (house09.json #0)
house_09_roof  (house09.json #1)
```

---

## `JsonMapgenDefinition` (planned)

```java
public final class JsonMapgenDefinition {
    private final List<String> omTerrain;
    private final String method;
    private final int weight;
    private final boolean disabled;
    private final Path sourceFile;
    private final int indexInFile;
    private final JsonValue objectRoot;   // object subtree only

    public boolean isJsonPreviewSupported() {
        return "json".equals(method) && !disabled && objectRoot.has("rows");
    }

    public String displayName() {
        if (!omTerrain.isEmpty()) return omTerrain.get(0);
        return sourceFile.getFileName() + "#" + indexInFile;
    }
}
```

Keep `JsonValue` for runner to avoid re-parse; or freeze parsed `RowsSpec` in P2.

---

## `JsonMapgenLoader` (planned)

```text
scanMapgen(options):
    catalog ← empty
    for mod in orderedMods:
        for file in sortedJson(mapgen/**):
            for (i, obj) in enumerate(parseFile(file)):
                if obj.type != "mapgen": continue
                catalog.add(parseMapgen(obj, file, i))
    return catalog
```

---

## BN source reference

| Concern | Location |
| --- | --- |
| Load | `src/mapgen.cpp` — `load_mapgen`, `load_mapgen_function` |
| setup_common | `src/mapgen.cpp` — `mapgen_function_json_base::setup_common` |
| generate | `src/mapgen.cpp` — `mapgen_function_json::generate` |
| Examples | `data/json/mapgen/house/house09.json` |

---

## Inputs

- Parsed `type: mapgen` objects from `mapgen/` tree

## Outputs

- `JsonMapgenDefinition` entries in `MapgenCatalog`

## Failure modes

| Condition | Behavior |
| --- | --- |
| Missing `object` | Skip + warn |
| `method` not `json` | Index with `supported=false` |
| Missing `rows` | Index but not runnable |
| `disabled: true` | Hidden from default catalog filter |

## Verification

1. Parse `house09.json` — ≥1 runnable json mapgen
2. `om_terrain` contains `house_09` on ground entry
3. Roof entry has separate `om_terrain` and different `fill_ter`
4. `builtin` mapgen in mixed file does not break scan
5. `weight` and `sourceFile` preserved for UI sort
