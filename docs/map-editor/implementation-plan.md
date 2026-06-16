# Implementation plan — map editor

Agent-oriented guide for the paintable grid. Spec units: [README](./README.md).

---

## Goal

A **MapEditorScreen** that:

1. Loads `LoadedGameData` + `LoadedTileset`
2. Displays a resizable grid of terrain tiles
3. Lets the user pick a brush from terrain ids and paint cells
4. Saves/loads local map JSON (not BN saves)

**v1 milestones M1–M4 are implemented.**

---

## Dependencies

| Upstream | Provides |
| --- | --- |
| [Game data loader](../game-data-loader/08-in-memory-model.md) | `TerrainRegistry`, ids, names |
| [Tileset loader](../tileset-loader/08-in-memory-model.md) | `LoadedTileset`, `TilesetLoadSession` |
| [Sprite viewer](../SPRITE_VIEWER.md) | Animation tick, draw-layer patterns |

Game data **G2** (terrain) is required; **G3** (furniture) optional for terrain-only editor.

---

## Deliverables

| Class | Unit | Status |
| --- | --- | --- |
| `MapGrid` | [01](./01-grid-model.md) | done |
| `MapPalettePanel` / paint input | [02](./02-palette-and-paint.md) | done |
| `MapEditorScreen` + camera | [03](./03-render-bridge.md) | done |
| `MapFileIO` | [04](./04-map-file-format.md) | done |
| `MapEditorToolbar` | [02](./02-palette-and-paint.md) | done |
| `ScreenInput` | [03](./03-render-bridge.md) | done |
| `MainMenuScreen` | — | done |

---

## PR slices

| PR | Status | Notes |
| --- | --- | --- |
| **M1** | done | `MapGridTest`, `MapFileIOTest` |
| **M2** | done | `TileSpriteResolver`, render + camera |
| **M3** | done | Palette + paint + eyedropper |
| **M4** | done | Filter, resize, HUD, toolbar, mouse, `TilesetLoadSession` |

**Prerequisite:** game data **G2** (`TerrainRegistry`).

---

## Entry from `Main`

```text
lwjgl3:run → MainMenuScreen
  → Sprite Viewer (TileDisplayScreen)
  → Map Editor (MapEditorScreen)
```

Sprite viewer **`E`** opens map editor. **`Esc`** returns to menu.

Share `TilesetLoadSession` for tileset load in viewer and editor (render thread only).

---

## Related

- [MAP_EDITOR.md](../MAP_EDITOR.md) — controls and run instructions
- [game-data-loader/implementation-plan.md](../game-data-loader/implementation-plan.md)
