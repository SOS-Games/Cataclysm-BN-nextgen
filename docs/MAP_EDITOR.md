# Map editor — implementation guide

Paintable **terrain grid** for sketching layouts. Consumes [game data](./GAME_DATA_LOADER.md)
for ids/names and [tileset loader](./TILESET_LOADER.md) for sprites.

Specs: [`docs/map-editor/`](./map-editor/README.md)

---

## Where to implement

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/
  map/MapGrid.java, MapFileIO.java
  view/MapEditorScreen.java, MapPalettePanel.java
```

---

## v1 milestone

| Item | Status |
| --- | --- |
| Spec index (`docs/map-editor/`) | done |
| `MapGrid` + map JSON load/save | todo |
| Render grid with `LoadedTileset` | todo |
| Palette + paint gestures | todo |
| Boot from `Main` / viewer shortcut | todo |
| Walkable player | out of scope |
| Multitile autoconnect | out of scope |

---

## Implementation order

1. [Game data loader](./game-data-loader/implementation-plan.md) v1 (`TerrainRegistry`)
2. [01 grid model](./map-editor/01-grid-model.md) + [04 map file](./map-editor/04-map-file-format.md)
3. [03 render bridge](./map-editor/03-render-bridge.md)
4. [02 palette and paint](./map-editor/02-palette-and-paint.md)

---

## Related

- [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) — reuse draw/animation patterns
- [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md)
