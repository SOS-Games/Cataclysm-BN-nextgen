# 15 — Setmap applier (`object.set`)

Apply BN **`set`** array entries on a `MapGrid` — point, line, and square bulk operations for
terrain, furniture, traps, radiation, and bash.

**Status:** todo (P9). See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

Many mapgens use sparse `rows` (often all spaces) plus **`set`** to scatter content across a
rectangle with `repeat` and `chance`:

```json
"fill_ter": "t_region_groundcover",
"rows": [ "                        ", ... ],
"set": [
  {
    "point": "terrain",
    "id": "t_region_groundcover_barren",
    "x": [ 0, 23 ],
    "y": [ 0, 23 ],
    "repeat": [ 100, 300 ]
  },
  {
    "point": "terrain",
    "id": "t_platform_resin",
    "x": [ 3, 20 ],
    "y": [ 3, 20 ],
    "repeat": [ 5, 50 ]
  },
  {
    "point": "trap",
    "id": "tr_archon_hallu_trap",
    "x": [ 3, 20 ],
    "y": [ 3, 20 ],
    "repeat": [ 1, 2 ]
  }
]
```

Source: Arcana [`mapgen_structure_anomalous.json`](../../../Cataclysm-BN/data/mods/Arcana_BN/overmap_and_mapgen/mapgen_structure_anomalous.json) — `arcana_field_anomalous_buffer`.

Without setmap, preview shows uniform `fill_ter` for the entire 24×24 OMT.

---

## BN generation order (authoritative)

From `mapgen_function_json::generate` (`src/mapgen.cpp`):

```text
1. m.draw_fill_background(fill_ter)
2. if predecessor_mapgen: run predecessor + rotation undo  [17]
3. for setmap in setmap_points: setmap.apply(md)         ← THIS UNIT
4. objects.apply(md)   // rows, place_items, place_monsters, nested, …
5. resolve_regional_terrain_and_furniture(md)            [19]
6. m.rotate(mapgen.rotation + OMT rotation)              [20]
```

**Critical:** Setmap runs **before** rows (`jmapgen_objects`). Rows and explicit char placings
can overwrite setmap cells where both apply. For buffer mapgens, rows are spaces → setmap
terrain remains visible.

**Preview `JsonMapgenRunner` target:**

```text
grid ← new MapGrid(w, h, fillTer)     // or predecessor canvas
SetmapApplier.apply(grid, object.get("set"), options)   // NEW
applyRows(grid, merged, rows)         // existing v1 loop
// place_*, regional, rotate — later PRs
```

---

## Entry shapes

BN supports four geometry kinds (via `point` / `line` / `square` key, or deprecated `set` key):

| Key | Geometry | Extra fields |
| --- | --- | --- |
| `point` | Single cell per repeat | `x`, `y` |
| `line` | Bresenham line | `x`, `y`, `x2`, `y2` |
| `square` | Filled rectangle | `x`, `y`, `x2`, `y2` |

Deprecated: `{ "set": "terrain", ... }` — same as `point`; BN warns in debug.

### `point` value (operation)

| `point` / `set` value | Enum | Preview v2.0 |
| --- | --- | --- |
| `terrain` | `JMAPGEN_SETMAP_TER` | `setTerrain` |
| `furniture` | `JMAPGEN_SETMAP_FURN` | `setFurniture` |
| `trap` | `JMAPGEN_SETMAP_TRAP` | warn / `MapCell.trapId` |
| `radiation` | `JMAPGEN_SETMAP_RADIATION` | skip |
| `bash` | `JMAPGEN_SETMAP_BASH` | skip |

Line/square variants add `LINE_` / `SQUARE_` prefix to op enum (see `mapgen.h`).

### Common fields

| Field | Type | BN semantics |
| --- | --- | --- |
| `x`, `y` | int or `[min,max]` | Inclusive; **random roll per application** via `jmapgen_int::get()` |
| `x2`, `y2` | int or range | Required for line/square |
| `id` | string | Ter/furn/trap id (radiation uses `amount` instead) |
| `repeat` | int or `[min,max]` | Default `[1,1]`; rolled once per entry → loop count |
| `chance` | int | **`one_in(chance)`** — `1` = always; `100` ≈ 1% per attempt |
| `rotation` | int | Furn rotation — defer v2.0 |
| `fuel`, `status` | int | Vehicle fields — N/A preview |

**Not `chance: 80` = 80%.** BN uses `one_in(80)` for 1/80 probability. Match this in preview.

---

## Apply algorithm (point ter)

```text
applySetmapEntry(grid, entry, rng, warnings):
    if entry.chance > 1 && !oneIn(rng, entry.chance):
        return

    op ← parseOp(entry)                    // terrain | furniture | ...
    repeatCount ← rollInt(rng, entry.repeat, default 1)

    for i in 0 .. repeatCount-1:
        x ← rollInt(rng, entry.x)
        y ← rollInt(rng, entry.y)
        if !inBounds(grid, x, y): warn; continue
        switch op:
            case TERRAIN: grid.setTerrain(x, y, entry.id)
            case FURNITURE: grid.setFurniture(x, y, entry.id)
            ...
```

### Line ter (`draw_line_ter`)

```text
for (x,y) in bresenham(x1,y1,x2,y2):
    grid.setTerrain(x, y, id)
```

### Square ter (`draw_square_ter`)

```text
for x in min(x1,x2) .. max(x1,x2):
    for y in min(y1,y2) .. max(y1,y2):
        grid.setTerrain(x, y, id)
```

Square trap/radiation loops cells similarly in BN (`jmapgen_setmap::apply`).

---

## `JmapgenIntRange` helper

Shared with [18](./18-place-spawners.md):

```java
public final class JmapgenIntRange {
  public static int roll(JsonValue field, Random rng);
  // int → fixed value; array [a,b] → uniform random in [a,b] inclusive
}
```

Parse once per `set` entry for `repeat`; re-roll `x`/`y` each inner iteration.

---

## Planned Java types

```java
package io.gdx.cdda.bn.nextgen.mapgen.json;

public final class SetmapApplier {
    public static void apply(
        MapGrid grid,
        JsonValue setArray,
        JsonMapgenRunOptions options
    );
}

public final class SetmapEntry {
    enum Geometry { POINT, LINE, SQUARE }
    enum Op { TERRAIN, FURNITURE, TRAP, RADIATION, BASH }
    // parsed fields...
}
```

`JsonMapgenRunOptions` additions:

```java
private long previewSeed = 0xC0DA;
public Random createRng() { return new Random(previewSeed ^ definitionHash); }
```

Use one RNG per mapgen run so repeat/import is reproducible.

---

## P9 delivery slices

| Slice | Scope |
| --- | --- |
| **P9.0** | `point` + `terrain` + `furniture` only |
| **P9.1** | `line` / `square` ter + furn |
| **P9.2** | trap id storage on `MapCell` (optional) |

Ship P9.0 first; Arcana buffer only needs `point`/`terrain`.

---

## Test fixtures

### `test_setmap_buffer.json`

```json
[
  {
    "type": "mapgen",
    "method": "json",
    "om_terrain": "test_setmap_buffer",
    "object": {
      "fill_ter": "t_grass",
      "rows": [ "........................", "... (24 lines)" ],
      "set": [
        {
          "point": "terrain",
          "id": "t_dirt",
          "x": [ 0, 23 ],
          "y": [ 0, 23 ],
          "repeat": [ 50, 50 ],
          "chance": 1
        },
        {
          "square": "terrain",
          "id": "t_wall",
          "x": 10,
          "y": 10,
          "x2": 12,
          "y2": 12
        }
      ]
    }
  }
]
```

**Asserts (seed=42):** exact dirt cell count stable; square wall block at (10–12, 10–12).

---

## BN source reference

| Concern | Location |
| --- | --- |
| Parse `set` array | `mapgen_function_json_base::setup_setmap` — ~L906 |
| `jmapgen_setmap::apply` | ~L3989 |
| Op enums | `mapgen.h` — `jmapgen_setmap_op` |
| `jmapgen_int` ranges | `mapgen.h` / parser in setup |
| Generate order | `mapgen_function_json::generate` — setmap before `objects.apply` |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Missing `point`/`line`/`square` | Warning; skip entry |
| Invalid `point` value | Warning; skip |
| `x`/`y` out of grid | Skip cell + warning |
| Unknown ter/furn id | Set id anyway; warn if `validateIds` |
| `set` omitted | No-op |
| `chance` fails `one_in` | Skip entire entry (BN returns early) |

---

## Verification

1. P9.0 fixture: dirt scatter count stable across runs with same `previewSeed`
2. Square overwrites fill_ter in 3×3 block
3. `chance: 100` → visibly sparser placement than `chance: 1` (same repeat)
4. Order: setmap before rows — row `#` wall wins over set dirt on same cell
5. Integration: Arcana buffer — `t_platform_resin` / barren patches visible in editor
6. `SetmapApplierTest` + optional `JsonMapgenRunnerTest` extension
