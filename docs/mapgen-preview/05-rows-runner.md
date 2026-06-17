# 05 — Rows runner

Apply mapgen `object` + merged palettes to a [`MapGrid`](../map-editor/01-grid-model.md).

BN analogues: `mapgen_function_json::generate` (fill) + `jmapgen_objects` / `formatted_set_simple`.

---

## Purpose

Deterministic translation:

```text
(fill_ter, palettes, inline terrain/furniture, rows) → MapGrid(w, h)
```

Each cell: `terrainId` (always) and optional `furnitureId`.

---

## BN generation order (reference)

```text
mapgen_function_json::generate(md):
    1. if fill_ter: m.draw_fill_background(fill_ter)
    2. if predecessor_mapgen: run + rotation undo/redo
    3. jmapgen_objects.apply — includes row-placed pieces + place_*
    4. resolve_regional_terrain_and_furniture(md)
    5. rotate submap per mapgen + OMT rotation
```

**Preview v1** implements steps **1** and the **rows** portion of **3** only (terrain +
furniture placings from char maps). No rotation, no regional resolve, no `place_*`.

---

## Preview algorithm

```text
run(def, paletteRegistry, options):
    obj ← def.objectRoot
    fillTer ← obj.string("fill_ter", options.defaultFillTer)

    merged ← paletteRegistry.merge(obj.stringArray("palettes"), warnings)
    applyCharMap(merged.terrain, obj.object("terrain"))
    applyCharMap(merged.furniture, obj.object("furniture"))

    rows ← obj.array("rows") as List<String>
    if rows.isEmpty(): throw

    width ← max row codePointCount
    height ← rows.size()
    grid ← new MapGrid(width, height, fillTer)

    for y in 0 .. height-1:
        row ← rows[y]
        x ← 0
        for each codePoint in row (UTF-32 iteration):
            ter ← merged.terrain[codePoint]
            furn ← merged.furniture[codePoint]
            if ter != null: grid.setTerrain(x, y, ter)
            if furn != null: grid.setFurniture(x, y, furn)
            x++

    return grid
```

### `applyCharMap`

Parse `object.terrain` / `object.furniture` the same way as palette sections
([03](./03-palette-loader.md) `PaletteCharResolver`) and **overwrite** entries in `merged`.

### Precedence per cell (single code point)

| Layer | Source |
| --- | --- |
| Furniture | inline `object.furniture` → merged palette furniture |
| Terrain | inline `object.terrain` → merged palette terrain → `fill_ter` (grid init) |

BN may apply multiple `jmapgen_piece` per char (item + terrain); v1 only sets ter/furn ids.

### Same char, both ter and furn

Common in BN: `.` = outdoor terrain, `H` = sofa furniture on floor terrain. Runner sets
**both** when mappings exist. Floor remains under furniture in render ([07](./07-furniture-render.md)).

---

## BN rows setup (reference)

In `setup_common`, BN builds `jmapgen_place` per `(x,y)` for every `format_placings` entry
matching that char — including items, monsters, etc. At apply time `jmapgen_terrain` calls
`m->ter_set`, `jmapgen_furniture` calls `m->furn_set` (with `f_toilet` special case).

v1 special cases:

| BN | v1 |
| --- | --- |
| `f_toilet` → `place_toilet` | Set `f_toilet` id directly |
| Liquids / fields | Skip |
| Item spawn on `.` | Skip |

---

## `JsonMapgenRunner` API

```java
public final class JsonMapgenRunner {

    public static MapGrid run(
        JsonMapgenDefinition definition,
        PaletteRegistry palettes,
        JsonMapgenRunOptions options
    );

    public static final class JsonMapgenRunOptions {
        private String defaultFillTer = "t_dirt";
        private boolean validateIds = false;
        private LoadedGameData gameDataForValidation;  // optional
        private final List<String> warnings = new ArrayList<>();
    }
}
```

`RowsInterpreter` may hold static helpers for UTF-32 walk + `MergedCharMap` lookup.

---

## Grid sizing

| Case | Behavior |
| --- | --- |
| Row shorter than max width | Cells `x >= row.length` keep `fill_ter` only |
| BN 24×24 expectation | Preview accepts any size; `house09` typically 24×24 |
| Empty char mapping | Terrain stays `fill_ter`; furniture null |
| Space character | Resolve via palette — often ground or void |

---

## Id validation

When `options.validateIds` and `LoadedGameData` provided:

```text
for each cell:
    if !terrainRegistry.contains(ter): warn
    if furn != null && !furnitureRegistry.contains(furn): warn
```

Non-blocking — preview still returns grid.

---

## Warnings collection

| Event | Message pattern |
| --- | --- |
| Unknown palette id | `[mapgen] unknown palette 'foo'` |
| Unmapped char (no ter/furn, not fill) | `[mapgen] row y x: unmapped char 'X'` |
| Missing `fill_ter` and unmapped char | Same — char has no ter in palette |

BN errors on unmapped chars without `fill_ter`; preview warns but continues if `fill_ter` set.

---

## Test fixtures

```text
core/src/test/resources/mapgen-fixtures/
  palettes/
    minimal_palette.json       # id: minimal — . → t_floor, # → t_wall, H → f_chair
  mapgen/
    simple_room.json           # 5×5, palettes: [minimal]
    inline_override.json       # terrain.# overrides wall id
    merge_order.json           # tests palette merge via two palette files
```

### `simple_room.json` sketch

```json
[
  {
    "type": "mapgen",
    "method": "json",
    "om_terrain": "test_room",
    "object": {
      "fill_ter": "t_floor",
      "palettes": [ "minimal" ],
      "rows": [
        "#####",
        "#...#",
        "#.H.#",
        "#...#",
        "#####"
      ]
    }
  }
]
```

Integration (optional): `../Cataclysm-BN/data/json/mapgen/house/house09.json` + full palettes.

---

## BN source reference

| Concern | Location |
| --- | --- |
| fill_ter | `mapgen_function_json::setup_internal` |
| rows + palette temp load | `mapgen_function_json_base::setup_common` |
| Row walk | `setup_common` — `utf8_display_split` |
| Apply terrain/furn | `jmapgen_terrain`, `jmapgen_furniture` |
| formatted_set | `src/mapgenformat.cpp` |

---

## Inputs

- `JsonMapgenDefinition` with `object` containing `rows`
- `PaletteRegistry`

## Outputs

- `MapGrid`
- Side-effect warnings list on options

## Failure modes

| Condition | Result |
| --- | --- |
| No `rows` | `IllegalArgumentException` |
| All palette ids missing | Grid = `fill_ter` only (+ inline overrides) |
| Invalid dimensions 0×0 | Reject |

## Verification

1. `simple_room.json` → 5×5, corners `t_wall`, center has `f_chair`
2. `inline_override.json` → `#` uses override ter id
3. Palette merge order changes mapped ter id
4. Integration: `house09` ground floor ≈ 24×24, interior `t_floor`
5. UTF-8 symbol char in fixture (if added) uses code point lookup
6. Warnings list populated for unknown palette id
