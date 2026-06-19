# 17 — Predecessor mapgen

Run **`predecessor_mapgen`** before the main mapgen content on the **same** submap canvas.

**Status:** todo (P12). See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

BN fills the submap with an outdoor or generic template before placing building structure.
Without predecessor, rural houses and shops show interior rows on flat `fill_ter` — missing
surrounding field, pavement, or forest.

Typical JSON:

```json
"object": {
  "predecessor_mapgen": "field",
  "fill_ter": "t_floor",
  "rows": [ "#####", "#...#", "#####" ]
}
```

Or predecessor only (no `fill_ter` on main) — BN requires `fill_ter` **or** predecessor **or**
rows+terrain.

---

## BN generate sequence

From `mapgen_function_json::generate` (`src/mapgen.cpp`):

```text
1. if fill_ter: draw_fill_background(fill_ter)     // main object fill — may run AFTER pred setup
2. if predecessor_mapgen id set:
       mapgendata pred_dat(md, predecessor_id)
       run_mapgen_func(predecessor_id, pred_dat)   // full json/builtin/lua generate
       // rotation undo so predecessor stays aligned when main rotates:
       m.rotate((-rotation + 4) % 4)
       if OMT rotatable: m.rotate((-ter.rotation + 4) % 4)
3. setmap apply
4. objects.apply (rows, place_*, nested)
5. resolve_regional
6. rotate forward (main rotation + OMT)
```

Predecessor runs **another complete mapgen** on the same `map` — including its own fill, set,
rows, regional pass, and rotation (then undone).

---

## Preview algorithm (v2.0)

### Catalog lookup

`predecessor_mapgen` value is an **`oter_id` / mapgen key string** (e.g. `"field"`, `"forest"`,
`"empty_grass"`). Resolve via:

```text
predDef ← catalog.findFirstRunnableByOmTerrain(predId)
// or findByOmTerrain + pick json method
```

Not the same as `om_terrain` on the **current** definition — it's a **different** OMT type’s
default mapgen.

### Recursive run

```text
run(def, catalog, palettes, options, depth):
    if depth > MAX_DEPTH: warn; return empty grid

    obj ← def.objectRoot
    predId ← obj.getString("predecessor_mapgen", null)

    MapGrid grid
    if predId != null:
        predDef ← resolvePredecessor(catalog, predId)
        if predDef present:
            grid ← run(predDef, catalog, palettes, options, depth+1)  // full pipeline
        else:
            warn; grid ← new MapGrid(w,h, fillTer)
    else:
        grid ← new MapGrid(w,h, fillTer)

  // Main object on same canvas (no second fill if predecessor filled):
    if predId == null && fillTer: already filled on grid init
    if predId != null: do NOT clear grid — apply main set/rows on top

    SetmapApplier.apply(grid, obj.set, options)
    applyRows(grid, obj, palettes, options)
    ...
    return grid
```

### `fill_ter` interaction

| Case | Behavior |
| --- | --- |
| Predecessor only | Predecessor’s fill + content; main rows overlay |
| `fill_ter` + predecessor | BN draws main fill **before** predecessor in C++; verify when porting — may need to match C++ order exactly |
| Main fill after pred | If C++ fills first then predecessor overwrites, mirror that |

**Action item for implementer:** Read `generate()` fill_ter block vs predecessor block order in
current BN; snapshot test one BN mapgen with both fields.

### Rotation (P14 coordination)

Full BN predecessor rotation undo is non-trivial. **v2.0 acceptable gap:** run predecessor
without unrotate; document visual mismatch for rotated OMTs until [20](./20-mapgen-rotation.md).

---

## Planned Java types

```java
public final class PredecessorMapgenRunner {
    private static final int MAX_DEPTH = 4;

    public static Optional<JsonMapgenDefinition> resolve(
        MapgenCatalog catalog,
        String predecessorOmTerrain
    );

    public static void runPredecessorIfPresent(
        MapGrid canvas,
        JsonMapgenDefinition definition,
        MapgenCatalog catalog,
        PaletteRegistry palettes,
        JsonMapgenRunOptions options,
        int depth
    );
}
```

Integrate at top of `JsonMapgenRunner.run` after allocating canvas dimensions (from main rows or
24×24 default).

---

## Common predecessor ids (BN core)

| Id | Typical use |
| --- | --- |
| `field` | Rural building underlay |
| `forest` | Woodland clearing |
| `empty_grass` | Cleared grass OMT |
| `pavement` | Urban lot |

Exact strings must exist as runnable json mapgen in catalog — core `data/json/mapgen/` usually
provides them.

---

## Test fixtures

### `test_predecessor_field.json`

```json
[
  {
    "type": "mapgen",
    "method": "json",
    "om_terrain": "test_field_bg",
    "object": {
      "fill_ter": "t_grass",
      "rows": [ "........................", "... 24x24 grass ..." ]
    }
  },
  {
    "type": "mapgen",
    "method": "json",
    "om_terrain": "test_house_on_field",
    "object": {
      "predecessor_mapgen": "test_field_bg",
      "rows": [
        "#####",
        "#...#",
        "#####"
      ],
      "palettes": [ "minimal" ]
    }
  }
]
```

**Assert:** Cells outside 5×5 room still `t_grass` from predecessor.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Read field | `mapgen_function_json_base::setup_internal` ~L3685 |
| Generate | `mapgen_function_json::generate` ~L4170–4192 |
| `run_mapgen_func` | dispatches builtin/json/lua |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Unknown predecessor id | Warning; main content only on `fill_ter` |
| Depth > 4 | Truncate chain |
| Predecessor `method: builtin` | Warning; skip (no builtin port) |
| Circular pred chain | Depth limit catches |

---

## Verification

1. Fixture: grass predecessor + centered room walls
2. Recursive: A pred B pred C — depth limit warning
3. Integration: BN `house09` or known rural mapgen with `predecessor_mapgen` — edges not `t_dirt` default
4. Import building unchanged — predecessor only affects `JsonMapgenRunner` per piece
