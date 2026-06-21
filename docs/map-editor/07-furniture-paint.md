# 07 — Furniture paint brush

Edit **furniture** ids on the grid — not just view mapgen output.

**Status:** done (**M5**). Display-only furniture is done ([mapgen-preview 07](../mapgen-preview/07-furniture-render.md)).

---

## Purpose

Mapgen import fills `MapCell.furnitureId` ([01 grid model](./01-grid-model.md)). Users cannot
paint sofas, counters, or doors-as-furniture without re-importing JSON.

M5 adds a **layer mode** to the palette and paint tools so hand-edited maps and touch-ups match
BN's two-layer cell model.

---

## Scope

### In scope

- Toggle **Terrain** vs **Furniture** brush mode
- Furniture palette rows from `FurnitureRegistry` (paintable-only, same rule as terrain)
- Left click/drag paints `furnitureId`; empty brush clears furniture on cell (`null`, not `f_null`)
- Eyedropper picks furniture when in furniture mode (or secondary pick with modifier — pick one)
- Map JSON save/load includes `furniture` field ([04](./04-map-file-format.md) — verify schema)
- R2 `looks_like` for furniture draw + palette eligibility

### Out of scope

- Furniture multitile autoconnect (R1.1b)
- Stacking multiple furniture per cell
- `deconstruct` / damage state

---

## UI design

### Mode switch

| Control | Action |
| --- | --- |
| Toolbar **Layer: Terrain / Furniture** | Toggle brush target |
| **`L`** hotkey | Cycle layer mode (optional) |
| HUD | `Layer: furniture` next to tool name |

Default after boot: **Terrain** (v1 behavior).

### Palette panel

```text
When terrain mode:
  header "Terrain (n/m)"
  rows ← TerrainRegistry paintable

When furniture mode:
  header "Furniture (n/m)"
  rows ← FurnitureRegistry paintable
  include "Clear furniture" row (empty brush)
```

Filter `/` applies to active mode list (name + id substring).

### Paint semantics

```text
paint(cell):
    if terrain mode:
        setTerrain(cell, brushTerrainId)
    if furniture mode:
        if brush is clear: setFurniture(cell, null)
        else: setFurniture(cell, brushFurnitureId)
```

Drag fills same as terrain — no separate eraser tool required for furniture.

### Eyedropper

| Mode | Pick |
| --- | --- |
| Terrain | `cell.terrainId` (existing) |
| Furniture | `cell.furnitureId` or clear brush if empty |

---

## `MapGrid` / `MapFileIO`

Confirm [04 map file format](./04-map-file-format.md) v1 schema:

```json
{
  "cells": [
    { "terrain": "t_floor", "furniture": "f_chair" }
  ]
}
```

If furniture omitted on load → `null`. Saving always writes key when non-null.

Add `MapGrid.clearFurniture(x,y)` if not present.

---

## Validation

Optional soft validate on paint: warn in status bar if id not in `FurnitureRegistry` (allow for
mod prototyping).

---

## Integration with mapgen import

Import replaces grid / `MapVolume` as today. Furniture layer toggle **`F`** remains **view**
visibility; distinct from **brush layer** (M5).

Suggested HUD clarity:

```text
[F] hide/show furniture sprites
[L] terrain vs furniture brush
```

---

## Inputs

- `FurnitureRegistry`, `LoadedTileset`, pointer events, active brush

## Outputs

- Mutated `MapCell.furnitureId`

## Failure modes

| Condition | Behavior |
| --- | --- |
| Paint OOB | Ignored |
| Unknown furniture id | Allowed on grid; canvas skips if no art (today's rule) |
| Tileset loading | Block paint (existing) |

---

## Verification

1. Paint `f_chair` on floor cell → visible with tileset that defines chair art
2. Clear furniture → cell shows terrain only
3. Save/load round-trip preserves furniture
4. Eyedropper in furniture mode copies id
5. Mapgen import then furniture touch-up persists after save

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.map.MapFileIOTest"
```

---

## Related

- [02 palette and paint](./02-palette-and-paint.md)
- [06 looks_like](./06-looks-like-draw-fallback.md)
- [game-data G3](../game-data-loader/README.md) — `FurnitureRegistry`
