# 21 — Nested and update mapgen

**`nested`** sub-mapgen chunks, **`mapgensize`**, and **`update_mapgen`** overlay passes.

**Status:** P15 done. See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

### Nested

BN places smaller mapgen fragments inside a parent OMT — labs, apartments, cargo modules.
Example shape:

```json
"nested": [
  {
    "chunks": [ [ "lab_room_1", 100 ] ],
    "x": 0,
    "y": 0
  }
],
"mapgensize": [ 24, 24 ]
```

Nested entries become `jmapgen_piece` with phase `mapgen_phase::nested_mapgen` — applied during
`objects.apply` after terrain/furniture row placings.

v1 **catalog** may match nested `om_terrain` **grids** for [combined floor](./12-omt-stitch-composer.md)
layout — but does not **execute** nested placement inside one OMT cell.

### Update mapgen

Separate mapgen definition applied on an already-generated map (loot refresh, damage). Referenced
from overmap terrain or mission — less common in static building import.

---

## BN nested concepts

| Field | Role |
| --- | --- |
| `nested` | Array of placement specs |
| `chunks` | Weighted list of nested mapgen ids or inline objects |
| `x`, `y` | Offset in parent mapgen coordinates |
| `mapgensize` | `[w,h]` of nested chunk in cells (default often 24×24) |
| `rotation` | Nested chunk rotation |

Weighted chunk pick uses BN rng — preview uses `previewSeed`.

---

## Preview algorithm — nested

```text
applyNested(parentGrid, object, catalog, palettes, options):
    nestedArray ← object.get("nested")
    if nestedArray == null: return

    chunkW, chunkH ← readMapgensize(object, default 24, 24)

    for entry in nestedArray:
        chunkId ← pickWeighted(entry.chunks, options.rng())
        nestedDef ← resolveNestedDefinition(catalog, chunkId)
        if nestedDef == null: warn; continue

        subGrid ← JsonMapgenRunner.run(nestedDef, palettes, options)  // full pipeline
        destX ← entry.x; destY ← entry.y
        blit(parentGrid, subGrid, destX, destY, clip=true)
```

### Order relative to other passes

```text
fill → predecessor → setmap → rows → place_terrain/furn → nested → regional → rotate
```

Nested runs in objects phase — **after** explicit row chars, **with** other place pieces sorted
by phase.

### Inline nested object

BN allows inline mapgen object instead of id string — parser returns embedded `JsonValue`; run
`JsonMapgenRunner` on synthetic `JsonMapgenDefinition`.

---

## Preview algorithm — update mapgen

```text
applyUpdateMapgen(baseGrid, updateDef, palettes, options):
    delta ← JsonMapgenRunner.run(updateDef, palettes, options)
    mergeNonEmpty(baseGrid, delta)
```

`updateDef` loaded from:

- Separate catalog entry (`type: mapgen` flagged update — check BN schema)
- Or `overmap_terrain` link — defer until OMT loader exists

Merge rule: for each cell, if delta has non-default ter/furn, overwrite base.

---

## `mapgensize`

```json
"mapgensize": [ 12, 12 ]
```

Nested chunk spans 12×12 cells within parent (common in labs). Parent `rows` may still be 24×24.

```text
readMapgensize(object):
    arr ← object.get("mapgensize")
    if arr size >= 2: return (arr[0], arr[1])
    return (24, 24)
```

Use when blitting nested result and when validating `x`/`y` bounds.

---

## Planned Java types

```java
public final class NestedMapgenRunner {
    public static void apply(
        MapGrid parent,
        JsonValue objectRoot,
        MapgenCatalog catalog,
        PaletteRegistry palettes,
        JsonMapgenRunOptions options
    );
}

public final class UpdateMapgenApplier {
    public static void mergeUpdate(
        MapGrid base,
        JsonMapgenDefinition updateDef,
        PaletteRegistry palettes,
        JsonMapgenRunOptions options
    );
}
```

Hook `NestedMapgenRunner.apply` from `JsonMapgenRunner` after rows + P13 place, before regional.

---

## Relationship to combined OMT mapgen

| Feature | Combined floor resolver | Nested runner |
| --- | --- | --- |
| `om_terrain` 2×2 grid in one json | Matches building pieces at z=0 | N/A |
| `nested` inside one OMT json | Ignored v1 | Executes sub-chunks |
| Multi-OMT stitch | [12](./12-omt-stitch-composer.md) | Per-piece run |

A single OMT json can have both `rows` and `nested` — both apply to same 24×24 canvas.

---

## Test fixtures

### `test_nested_chunk.json`

Parent 24×24 grass + nested 12×12 `t_floor` room at (6,6):

```json
"object": {
  "fill_ter": "t_grass",
  "rows": [ "... 24x24 ..." ],
  "mapgensize": [ 12, 12 ],
  "nested": [
    {
      "chunks": [ "test_nested_room" ],
      "x": 6,
      "y": 6
    }
  ]
}
```

Second mapgen entry `test_nested_room` — 12×12 floor interior.

### `test_update_overlay.json` (optional)

Base room + update that replaces one cell ter id.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Nested setup | `mapgen_function_json_base::setup_nested` |
| `jmapgen_nested` | apply in `jmapgen_objects` |
| Update mapgen | `mapgen_function_json` update variants |
| Phase enum | `mapgen_phase::nested_mapgen` — `mapgen.h` |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Nested extends past parent | Clip blit + warning |
| Unknown chunk id | Skip entry |
| `method: builtin` chunk | Warn; skip |
| Empty `chunks` array | Skip |

---

## Verification

1. Nested 12×12 floor visible inside grass parent at offset
2. Weighted chunks: fixed seed picks stable chunk
3. `mapgensize` 12 vs 24 — blit bounds differ
4. Update merge overwrites only delta cells
5. Integration: small BN lab json with nested (optional `@EnabledIf`)

---

## Out of scope (permanent unless rescoped)

- `nested` spanning **multiple OMTs** (worldgen placement)
- Dynamic nested pick from live overmap state
- Builtin/lua nested generators
