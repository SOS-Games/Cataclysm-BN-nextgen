# 03 — Render bridge

Draw [MapGrid](./01-grid-model.md) via [LoadedTileset](../tileset-loader/08-in-memory-model.md).
Reuses sprite viewer draw logic — not BN `cata_tiles` map draw.

**Status:** implemented (M2 + M4).

---

## Purpose

`MapEditorScreen.render()` transforms grid → screen pixels with camera.

---

## Coordinate pipelines

Grid uses **top-down row indices** (`(0,0)` = top-left per [01](./01-grid-model.md)) in LibGDX
**y-up** screen space:

```text
gridBaseY = cameraY + hudHeight()
gridTopY  = gridBaseY + grid.height * tilePx
cellBottomY(row) = gridTopY - (row + 1) * tilePx

cellX = floor((screenX - cameraX) / tilePx)
cellY = floor((gridTopY - screenY) / tilePx)
```

### Cell pixel size

```text
tilePx = tileset.tileInfo.width * tileset.pixelScale * zoom
```

Use `HdpiMode.Pixels` (same as sprite viewer) — integer zoom, no fractional stretch.

### Pointer coordinates

`ScreenInput` converts raw GLFW pointer y (often **y-down** on Windows) to match the ortho
projection (**y-up**):

```text
screenY = viewportHeight - inputY
```

Used for hover, paint, palette hits, toolbar, and main menu. Debug with **`F3`** in the editor
(console logs `inputRaw` vs `screen` and highlight bounds).

---

## Draw order per cell

```text
1. terrain bg layer
2. terrain fg layer
3. hover highlight (white overlay, Paint/Pick tools)
4. (v2) furniture bg/fg
```

Per layer: `TileSpriteResolver` with animation tick from `System.currentTimeMillis()/17`.

### Missing tile (canvas only)

```text
tile ← tileset.findTile(id).orElse(tileset.findTile("unknown"))
```

Palette list does **not** use this fallback — see [02](./02-palette-and-paint.md).

---

## Camera

| State | Type |
| --- | --- |
| `cameraX`, `cameraY` | float pixel offset |
| `zoom` | int 1..6 |

| Input | Effect |
| --- | --- |
| Arrow keys / drag | Pan |
| `+` / `-` or toolbar | Zoom |
| `C` or toolbar | Center on grid |
| Resize | More cells visible; tile px unchanged |

---

## Visible region culling

Row/column bounds computed from camera and viewport; full grid draw OK for ≤64×64 v1.

---

## Multitile limitation (v1)

BN selects grass/wall subtiles from neighbor mask. v1 draws **base id** sprite only —
edges may look wrong.

---

## FX preview

Not in map editor v1 (viewer `F` / `1-8` only).

---

## Load states

| State | UI |
| --- | --- |
| Tileset loading | `TilesetLoadSession` + `LoadingSpinner` on palette; paint blocked |
| Game data | Blocking at editor startup |
| Both ready | Grid + palette list |

---

## Inputs

- `MapGrid`, `LoadedTileset`, camera, animation tick

## Outputs

- Rendered frame

## Failure modes

| Condition | Behavior |
| --- | --- |
| tileset null | Grid without sprites; palette spinner or empty |
| empty grid | Clear background |

## Verification

1. Checkerboard `t_dirt`/`t_grass` renders with row 0 at top
2. Water tile animates when playback on (`P`)
3. Pan/zoom keeps pixel-aligned tiles; hover tracks OS cursor
4. `unknown` fallback for terrain id not in tileset on canvas
5. Tileset swap shows spinner until session completes
