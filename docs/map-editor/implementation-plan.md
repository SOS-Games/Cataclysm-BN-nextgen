# Implementation plan — map editor

Agent-oriented guide for the paintable grid. Spec units: [README](./README.md).

---

## Goal

A **MapEditorScreen** that:

1. Loads `LoadedGameData` + `LoadedTileset`
2. Displays a resizable grid of terrain tiles
3. Lets the user pick a brush from terrain ids and paint cells
4. Saves/loads local map JSON (not BN saves)

---

## Dependencies

| Upstream | Provides |
| --- | --- |
| [Game data loader](../game-data-loader/08-in-memory-model.md) | `TerrainRegistry`, ids, names |
| [Tileset loader](../tileset-loader/08-in-memory-model.md) | `LoadedTileset`, sprites |
| [Sprite viewer](../SPRITE_VIEWER.md) | Animation tick, draw-layer patterns |

Implement game-data **v1** before map editor render/palette, or use a hardcoded id list for
UI prototyping only.

---

## Deliverables

| Class | Unit |
| --- | --- |
| `MapGrid` | [01](./01-grid-model.md) |
| `MapPalettePanel` / paint input | [02](./02-palette-and-paint.md) |
| `MapEditorScreen` + camera | [03](./03-render-bridge.md) |
| `MapFileIO` | [04](./04-map-file-format.md) |

---

## Suggested PR slices

| PR | Content | Done when |
| --- | --- | --- |
| 1 | `MapGrid` + file IO | Load/save 10×10 `t_dirt` map JSON |
| 2 | Render bridge | Grid visible with tileset |
| 3 | Palette + paint | Click/drag places terrain |
| 4 | Polish | Search, grid resize, HUD |

---

## Entry from `Main`

Boot options (future):

- Sprite viewer (current default)
- Map editor (`--editor` or key from viewer)

Share `TilesetLoadSession` for tileset load; add blocking or incremental `GameDataLoader`.

---

## Related

- [game-data-loader/implementation-plan.md](../game-data-loader/implementation-plan.md)
