# 03 — Mini-overmap grid (W2)

A small **overmap grid** of OMT ids and an **editor view** to select cells.

**Status:** done (W2). See [implementation-plan](./implementation-plan.md).

---

## Purpose

Before procedural `overmap::generate`, prove:

- OMT coordinates map to the right **id string**
- User can **select** a cell and trigger submap generation (W3)
- Debug UI shows layout without full 180×180 canvas

---

## `OvermapGrid` model

```java
public final class OvermapGrid {
    private final int width;
    private final int height;
    private final String[] cells;  // oter_id per OMT, row-major

    public String getOmtId(int omtX, int omtY);
    public void setOmtId(int omtX, int omtY, String id);
}
```

| Constant | v1 value | BN reference |
| --- | --- | --- |
| Default size | 8×8 or 16×16 | ~180×180 |
| Cell content | OMT id string | `oter_id` |

---

## Factory sources (v1)

| Source | Use |
| --- | --- |
| `OvermapGrid.empty(w, h, fillId)` | All `open_air` or `field` |
| `OvermapGrid.fromJson(fixture)` | Test layouts |
| `OvermapGridFactory.noise(seed, registry)` | Simple biome noise — optional W2.1 |
| `OvermapGenerator.generate()` | W4+ replaces hand factories |

---

## Editor integration

Extend [MapEditorScreen](../map-editor/README.md) or add **Overmap mode**:

```text
Render:
  each OMT cell → colored rect or sym char (from OvermapTerrainDefinition)
  optional: thumbnail from last visited submap

Input:
  click (omtX, omtY) → select cell
  Enter / double-click → SubmapGenerator.visit(selected)  (W3)

HUD:
  OMT id, mapgen ref count, seed
```

**Camera:** one OMT cell might be 32×32 screen px; submap view switches to 24×24 tile view.

---

## JSON fixture format

`core/src/test/resources/worldgen-fixtures/overmaps/test_8x8.json`:

```json
{
  "width": 8,
  "height": 8,
  "fill": "test_field",
  "cells": [
    [ "test_field", "test_field", "house_09_north", "house_09_north" ]
  ]
}
```

Or sparse `{ "x": 2, "y": 1, "id": "house_09_north" }` patches over fill.

---

## Planned Java types

```java
public final class OvermapGridFactory { … }

public final class OvermapSelection {
    public final int omtX;
    public final int omtY;
    public final String omtId;
}
```

UI (optional W2):

```java
// view/OvermapView.java or mode flag on MapEditorScreen
```

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Unknown OMT id in cell | Render magenta + warning |
| Out of bounds click | Ignore |
| Empty grid | Disable visit |

---

## Dependencies

| Requires | Unit |
| --- | --- |
| OMT ids valid in registry | [02](./02-overmap-terrain-loader.md) W1 |

---

## Verification

1. Load 8×8 fixture; cell (2,1) id matches JSON
2. Editor overmap mode shows grid; click updates selection
3. W3 hooks `visit` without refactoring grid model
