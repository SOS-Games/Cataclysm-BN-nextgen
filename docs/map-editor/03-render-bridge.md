# 03 — Render bridge

Draw [MapGrid](./01-grid-model.md) via [LoadedTileset](../tileset-loader/08-in-memory-model.md).
Reuses sprite viewer draw logic — not BN `cata_tiles` map draw.

---

## Purpose

`MapEditorScreen.render()` transforms grid → screen pixels with camera.

---

## Coordinate pipelines

```text
screen (px) ← camera ← world cell (x, y)
worldX = (screenX - camX) / (tileW * zoom)
cellX = floor(worldX)
```

### Cell pixel size

```text
tilePx = tileset.tileInfo.width * tileset.pixelScale * zoom
```

Use `HdpiMode.Pixels` (same as sprite viewer) — integer zoom, no fractional stretch.

---

## Draw order per cell

```text
1. terrain bg layer
2. terrain fg layer
3. (v2) furniture bg/fg
```

Per layer: `TileSpriteResolver` with animation tick from `System.currentTimeMillis()/17`.

### Missing tile

```text
tile ← tileset.findTile(id).orElse(tileset.findTile("unknown"))
if still missing: skip draw or flat magenta debug
```

---

## Camera

| State | Type |
| --- | --- |
| `camX`, `camY` | float pixel offset |
| `zoom` | int 1..6 |

| Input | Effect |
| --- | --- |
| Arrow keys / drag | Pan |
| `+` / `-` | Zoom |
| Resize | More cells visible; tile px unchanged |

Center camera on grid optional on new map.

---

## Visible region culling

```text
firstCol = max(0, floor(-camX / tilePx))
lastCol  = min(width, ceil((viewportW - camX) / tilePx))
```

Full grid draw OK for ≤256×256 v1.

---

## Multitile limitation (v1)

BN selects grass/wall subtiles from neighbor mask. v1 draws **base id** sprite only —
edges may look wrong. HUD note optional.

---

## FX preview

Optional global `TilesetFxType` from sprite viewer (`F` / `1-8`).

---

## Load states

| State | UI |
| --- | --- |
| Tileset loading | `TilesetLoadSession` + spinner |
| Game data loading | Blocking or small spinner |
| Both ready | Grid + palette |

---

## Inputs

- `MapGrid`, `LoadedTileset`, camera, animation tick

## Outputs

- Rendered frame

## Failure modes

| Condition | Behavior |
| --- | --- |
| tileset null | Spinner only |
| empty grid | Clear background |

## Verification

1. Checkerboard `t_dirt`/`t_grass` renders correctly
2. Water tile animates when playback on
3. Pan/zoom keeps pixel-aligned tiles
4. `unknown` fallback for fake id in grid cell
5. 64×64 grid ≥ 30 fps on dev machine
