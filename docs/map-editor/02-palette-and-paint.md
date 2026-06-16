# 02 — Palette and paint

Brush selection and editing. Lists from `TerrainRegistry`; sprites from `LoadedTileset`.
Patterns reused from [SPRITE_VIEWER.md](../SPRITE_VIEWER.md).

**Status:** implemented (M3 + M4).

---

## Purpose

Side-panel **palette** (not full-screen catalog) + map canvas **paint** gestures + bottom
**toolbar** for mouse-first editing.

---

## Layout

```text
┌─────────────────────────────────────┬──────────┐
│  HUD (brush, cursor, status)        │ Tileset  │
│  Map canvas                         │ Filter   │
│  pan/zoom grid                      │ count    │
│                                     │ scroll   │
│                                     │ previews │
├─────────────────────────────────────┴──────────┤
│  Toolbar: Paint Pan Pick  - +  Center Save …   │
└────────────────────────────────────────────────┘
```

Palette width: `MapPalettePanel.WIDTH` (220px).

---

## Palette data source

```text
ids ← gameData.getTerrain().allIds()   // sorted
for id in ids:
    if !TileSpriteResolver.hasDrawableArt(tileset, id): continue
    def ← registry.find(id)
    preview ← tileset.findTile(id)     // exact match only
    label ← def.name + " (" + id + ")"
```

### Filters (implemented)

| Filter | Behavior |
| --- | --- |
| Text search (`/` or click filter row) | Substring on id or terrain `name` |
| Has gfx only | **Always on** — only ids with resolved fg/bg in active tileset |
| Animated subset | Not in palette (viewer `M` filter only) |

Count line: `visible/filtered` + `paintable/total in tileset`.

### Tileset header

```text
Tileset: <id>  n/m
```

While loading: `Tileset: <id>  …` + spinner centered on palette (`TilesetLoadSession`).

---

## Brush state

```text
selectedTerrainId: string
defaultTerrainId: string   // new grid + resize fill
```

Eraser / `t_null` deferred.

---

## Input mapping

| Input | Action |
| --- | --- |
| Left click / drag | Paint cell (Paint tool) |
| Right click | Eyedropper |
| Right / middle / Space+drag | Pan |
| Toolbar buttons | Tool, zoom, center, save/load, grid, tileset |
| `[` / `]` | Cycle tileset (`TilesetLoadSession` + spinner) |
| `+` / `-` | Zoom canvas |
| `/` | Palette filter edit |
| Palette click | Select brush |
| Palette wheel | Scroll list |
| `Ctrl+S` / `Ctrl+O` | Save / load map JSON |

No WASD player movement in v1.

### Drag paint

Track `lastPaintCellX/Y` to avoid duplicate writes per stroke.

### Pointer coordinates

All screen hits use `ScreenInput.fromInputY` — see [03](./03-render-bridge.md#pointer-coordinates).

---

## Shared code

| Helper | Role |
| --- | --- |
| `TileSpriteResolver` | `animationPickIndex`, layer resolve, `hasDrawableArt` |
| `LoadingSpinner` | Tileset load overlay on palette |
| `ScreenInput` | GLFW y-down → LibGDX y-up |
| `MapEditorToolbar` | Bottom mouse controls |

Palette preview = single 16px thumb (bg + fg), no `unknown` fallback in list.

---

## Furniture (v2)

Second brush mode or layer toggle painting `furnitureId`. v1 terrain only.

---

## Inputs

- Pointer events (screen → cell coords via `MapEditorScreen.screenToCell`)
- `TerrainRegistry`, `LoadedTileset`, `MapGrid`

## Outputs

- Mutated `MapGrid`
- Updated brush selection

## Failure modes

| Condition | Behavior |
| --- | --- |
| Paint OOB | Ignored |
| Selected id missing gfx | Not listed in palette; canvas may still show `unknown` for painted cells |
| Tileset loading | Paint disabled; spinner on palette |

## Verification

1. Select grass, click → cell updated
2. Drag diagonal line → cells filled
3. Eyedropper copies terrain from cell
4. Palette scroll shows paintable entries only; no placeholder thumbs
5. Tileset switch shows spinner; list refreshes to new pack's paintable set
6. Mouse click selects palette row; wheel scrolls list
