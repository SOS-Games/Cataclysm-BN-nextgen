# 10 — Overmap debug view

**OMT-level** view inside the map editor — select a cell, inspect id, trigger submap generation.

**Status:** done (**R3**). **Depends on worldgen W1 + W2** (implemented for editor smoke).

Canonical overmap model: [worldgen 03](../worldgen/03-mini-overmap-grid.md). This unit covers
**editor UX only**; generation rules stay in worldgen.

---

## Purpose

Worldgen W2 produces an `OvermapGrid` of OMT id strings. R3 exposes it in the same app as the
24×24 submap editor so you can:

- Visualize layout without a separate tool
- Click `house_09_north` → W3 `SubmapGenerator.visit` → swap to tile view
- Debug city footprints before W4 placement exists

---

## Editor modes

```text
enum EditorMode {
    SUBMAP,    // today — MapGrid / MapVolume tile view
    OVERMAP    // W2 — one cell = one OMT
}
```

| Control | Action |
| --- | --- |
| **`M`** or toolbar | Cycle Submap ↔ Overmap |
| Overmap click | Select OMT cell; HUD shows id + mapgen ref count |
| **Enter** / double-click | `SubmapGenerator.visit(omtX, omtY)` → switch to SUBMAP with result |
| **Esc** in overmap | Back to menu or clear selection |

When W3 is not implemented, Enter shows status "Visit not implemented" and optional direct
`JsonMapgenRunner` pick for smoke tests.

---

## Render (overmap mode)

Per OMT cell `(omtX, omtY)`:

```text
id ← overmapGrid.getOmtId(omtX, omtY)
if tileset loaded and OvermapOmtSpriteResolver finds art for id:
    draw tileset sprite (strip _north/_east/… suffix + rotation when needed)
else:
    def ← overmapTerrainRegistry.find(id)
    if def.hasSymbol():
        draw char + color from OvermapTerrainDefinition
    else:
        draw colored rect from type hash
if selected:
    highlight border
```

Cell screen size: configurable **OMT px** (e.g. 16–32), independent of tile px.

Optional: thumbnail from cached last visit ([worldgen 03](../worldgen/03-mini-overmap-grid.md)).

---

## Data wiring

```text
MapEditorScreen
  overmapGrid: OvermapGrid?
  overmapRegistry: OvermapTerrainRegistry   // W1
  submapCache: SubmapCache                  // W3
  editorMode: EditorMode
```

Load overmap from:

- Fixture JSON (tests)
- `OvermapGridFactory.empty(16, 16)` for smoke
- W4+ generator when available

---

## HUD

```text
Mode: OVERMAP  16×16  seed=12345
Selected: (4,2) house_09_north  mapgens=3  rotatable
Enter → generate submap
```

---

## Relationship to R1 / R2

Submap view after visit uses normal tile rendering with multitile + looks_like. Overmap mode
uses the **same loaded tileset** when an OMT id has drawable art (BN `use_tiles_overmap`
parity); otherwise falls back to overmap sym/color from W1.

---

## Inputs

- `OvermapGrid`, `OvermapTerrainRegistry`, pointer

## Outputs

- Selection state; optional `MapGrid` after visit

## Failure modes

| Condition | Behavior |
| --- | --- |
| W1 not loaded | Overmap mode disabled; status message |
| Unknown OMT id | Gray cell + raw id string |
| Visit fails | Keep overmap selection; log warnings |

---

## Verification

1. Load 8×8 fixture → distinct colors/syms per OMT type
2. Click cell → HUD updates
3. W3 wired: Enter generates same grid as mapgen picker for that OMT (seed fixed)
4. **`M`** returns to prior submap grid unchanged

---

## Related

- [worldgen 02 overmap_terrain](../worldgen/02-overmap-terrain-loader.md) — W1
- [worldgen 03 mini-overmap](../worldgen/03-mini-overmap-grid.md) — W2
- [worldgen 04 visit-tile](../worldgen/04-visit-tile-mapgen.md) — W3
- [v2-implementation-plan](./v2-implementation-plan.md) — R3 slice
