# 07 — Furniture render bridge

Draw furniture ids on the map editor canvas above terrain.

---

## Purpose

JSON mapgen places many symbols as **furniture** (`H` sofa, `h` chair, `r` desk). Doors and
walls are usually **terrain** (`#`, `+`, `|`). Without furniture drawing, previews show hollow
rooms — floors and walls only.

This unit extends [map editor 03](../map-editor/03-render-bridge.md). The mapgen runner
([05](./05-rows-runner.md)) already sets `MapCell.furnitureId`.

---

## BN draw order (reference)

In tiled mode BN roughly draws:

```text
1. Terrain (including door terrain tiles)
2. Furniture
3. Items / fields / overlays
4. Creature / vehicle
5. UI / lighting
```

Preview v1: terrain then furniture only.

---

## Draw order (v1)

```text
drawGrid():
    for each visible cell (x, y):
        cell ← grid.get(x, y)
        drawCellTerrain(cell.terrainId, worldX, worldY, tilePx)
        if showFurnitureLayer:
            drawCellFurniture(cell.furnitureId, worldX, worldY, tilePx)
```

Furniture draws at same cell origin as terrain ([map editor coordinates](../map-editor/03-render-bridge.md)).

---

## `drawCellFurniture` sketch

```java
private void drawCellFurniture(
    final String furnitureId,
    final float drawX,
    final float drawY,
    final int tilePx
) {
    if (furnitureId == null || furnitureId.isEmpty()) {
        return;
    }
    if (!spriteResolver.hasDrawableArt(furnitureId)) {
        return;   // no purple placeholder per cell
    }
    final TileSpriteLayers layers = spriteResolver.resolve(furnitureId);
    // Same fg/bg/animation path as terrain; furniture ids use f_* tile entries in tileset
    drawSpriteLayers(layers, drawX, drawY, tilePx, animationTick);
}
```

Refactor shared `drawSpriteLayers` from `drawCellTerrain` if duplication grows.

---

## `TileSpriteResolver` rules

Same as terrain ([`TileSpriteResolver`](../view/TileSpriteResolver.java)):

| Rule | Behavior |
| --- | --- |
| Exact tile id in `LoadedTileset` | Use fg/bg regions |
| Missing tile | `hasDrawableArt` false → skip |
| `unknown` fallback | Do not use for per-cell furniture |
| Animation | Use `animationTick` / `animationPlayback` from editor |

Furniture gfx uses the same `tile_config.json` id namespace (`f_chair`, etc.).

---

## Tile offsets

Some BN tiles define `offset_x` / `offset_y` in tileset JSON. v1:

- Use `TileDefinition` offsets when present (same as sprite viewer)
- If absent, align furniture to cell bottom-left like terrain

Check `LoadedTileset` / `TileDefinition` offset fields when implementing.

---

## `showFurnitureLayer` flag

```java
// MapEditorScreen
private boolean showFurnitureLayer = true;
```

| Input | Action |
| --- | --- |
| **`F`** (optional) | Toggle furniture layer |
| Toolbar toggle (v2) | Checkbox “Furniture” |

Default **on** after P4 so mapgen previews look furnished.

---

## Palette vs gfx gaps

| Situation | Policy |
| --- | --- |
| Furniture id in JSON, no gfx | Skip draw (silent) |
| Partial tileset (RetroDays) | Fewer furniture sprites — expected |
| Terrain `t_region_*` missing gfx | Terrain skip; furniture may still draw |

Do not spam `unknown` tile per cell.

---

## Interaction with palette panel

Terrain palette ([M3](../map-editor/02-palette-and-paint.md)) lists terrain ids only.
Furniture paint brush remains **out of scope** — generated furniture is view-only unless
user adds furniture paint in map editor v2.

Eyedropper: v2 could pick `furnitureId` from cell; v1 eyedropper terrain only.

---

## Out of scope (v1)

- Furniture paint brush in palette
- Z-order / transparency between furniture on same tile
- Multitile furniture — [map-editor 05](../map-editor/05-multitile-autoconnect.md) § R1.1b
- `looks_like` at draw time — [map-editor 06](../map-editor/06-looks-like-draw-fallback.md) **R2**
- Items on ground (mapgen `place_items`)

---

## BN source reference

| Concern | Location |
| --- | --- |
| Layer draw | `src/cata_tiles.cpp` — map draw paths |
| Furniture tile ids | `gfx/.../tile_config.json` |

---

## Inputs

- `MapCell.furnitureId`
- `LoadedTileset`, `TileSpriteResolver`

## Outputs

- Furniture sprites composited on grid

## Failure modes

| Condition | Behavior |
| --- | --- |
| Null furniture | Skip |
| No gfx | Skip |
| `showFurnitureLayer=false` | Skip all furniture |
| Tileset loading | Spinner; no furniture until complete |

## Verification

1. Generated `house09` shows sofas/chairs with tileset that defines `f_sofa`, `f_chair`
2. Terrain-only cells unchanged vs pre-P4
3. Toggle `F` hides furniture, walls remain
4. No measurable frame time regression on 64×64 grid (manual)
5. Furniture with tile offset renders aligned (if offset in tileset)
