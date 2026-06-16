# 02 — Palette and paint

Brush selection and editing. Lists from `TerrainRegistry`; sprites from `LoadedTileset`.
Patterns reused from [SPRITE_VIEWER.md](../SPRITE_VIEWER.md).

---

## Purpose

Side-panel **palette** (not full-screen catalog) + map canvas **paint** gestures.

---

## Layout

```text
┌─────────────────────────────────────┬──────────┐
│  Map canvas (unit 03)               │ Palette  │
│  pan/zoom grid                      │ search   │
│                                     │ scroll   │
│                                     │ previews │
└─────────────────────────────────────┴──────────┘
```

Minimum width palette ~200px; resizable.

---

## Palette data source

```text
ids ← gameData.getTerrain().allIds()   // sorted
for id in ids:
    def ← registry.find(id)
    preview ← tileset.findTile(id)     // optional thumb
    label ← def.name + " (" + id + ")"
```

### Filters (later)

| Filter | Key idea |
| --- | --- |
| Text search | Substring on id/name |
| Has gfx only | `tileset.findTile(id).present` |
| Flags contains | e.g. `INDOORS` |

Reuse sprite viewer `M` filter concept only if animated terrain subset needed.

---

## Brush state

```text
selectedTerrainId: string
eraser: bool              // paints defaultTerrainId or t_null
defaultTerrainId: string   // new grid + eraser target
```

---

## Input mapping

| Input | Action |
| --- | --- |
| Left click | Paint cell |
| Left drag | Paint stroke (cells under cursor) |
| Right click | Eyedropper → set brush to cell terrain |
| `[` / `]` | Cycle tileset (reuse TilesetLoadSession) |
| `+` / `-` | Zoom canvas |
| `F` | Global FX preview (optional) |

No WASD player movement in v1.

### Drag paint

Track `lastPaintedCell` to avoid duplicate work per frame; still paint if re-entering same
cell after leave optional.

---

## Shared code extraction

From `TileDisplayScreen` → shared helpers:

| Helper | Role |
| --- | --- |
| `TileSpriteResolver` | `animationPickIndex`, `resolveSpriteIndex`, `drawLayer` |
| `LoadingSpinner` | Tileset load in progress |
| Tileset picker HUD | Pack name line |

Palette cell draw = single tile preview (no label truncation logic from viewer grid).

---

## Furniture (v2)

Second brush mode or layer toggle painting `furnitureId`. v1 terrain only.

---

## Inputs

- Pointer events (screen → cell coords)
- `TerrainRegistry`, `LoadedTileset`, `MapGrid`

## Outputs

- Mutated `MapGrid`
- Updated brush selection

## Failure modes

| Condition | Behavior |
| --- | --- |
| Paint OOB | Clip |
| Selected id missing gfx | Canvas shows `unknown`; palette still allows |

## Verification

1. Select grass, click → cell updated
2. Drag diagonal line → cells filled
3. Eyedropper copies terrain from cell
4. Palette scroll shows >100 entries without frame drop (lazy render)
5. Tileset switch reloads previews
