# Map editor — implementation guide

Paintable **terrain grid** for sketching layouts. Consumes [game data](./GAME_DATA_LOADER.md)
for ids/names and [tileset loader](./TILESET_LOADER.md) for sprites.

Specs: [`docs/map-editor/`](./map-editor/README.md)

---

## Where to implement

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/
  map/
    MapGrid.java
    MapCell.java
    MapFileIO.java
  view/
    MainMenuScreen.java       # boot: sprite viewer or map editor
    MapEditorScreen.java
    MapEditorToolbar.java     # bottom toolbar (paint / pan / pick, tileset, save)
    MapPalettePanel.java
    TileSpriteResolver.java   # shared with TileDisplayScreen
    ScreenInput.java            # pointer y-up conversion (GLFW y-down on some desktops)
    LoadingSpinner.java
  gamedata/                     # prerequisite — see GAME_DATA_LOADER.md
  tileset/                      # LoadedTileset, TilesetLoadSession
```

Package roots: `io.gdx.cdda.bn.nextgen.map`, `io.gdx.cdda.bn.nextgen.view`

---

## v1 milestone

| Item | Status |
| --- | --- |
| Spec index (`docs/map-editor/`) | done |
| `MapGrid` + map JSON load/save (M1) | done |
| Render grid with `LoadedTileset` (M2) | done |
| Palette + paint gestures (M3) | done |
| Polish: filter, resize, HUD, toolbar, mouse (M4) | done |
| `MainMenuScreen` + sprite viewer `[E]` shortcut | done |
| Walkable player | out of scope |
| Multitile autoconnect | out of scope |

---

## Run

```bash
gradlew.bat lwjgl3:run
```

Opens **Main menu** → choose **Map Editor** (or **Sprite Viewer**, then press **`E`**).

When `../Cataclysm-BN/gfx` and `../Cataclysm-BN/data` exist, Gradle sets `-Dcdda.gfx.roots` /
`-Dcdda.data.roots` automatically.

---

## Controls (map editor)

| Input | Action |
| --- | --- |
| **Toolbar** (bottom) | Paint / Pan / Pick, zoom, center, save/load, grid size, tileset `<` `>` |
| Left click / drag | Paint (Paint tool) |
| Right click | Eyedropper (Pick tool or RMB without drag) |
| Right drag / middle drag / Space+drag | Pan |
| `[` / `]` | Previous / next tileset |
| `+` / `-` | Zoom |
| `G` | Cycle grid preset (10×10 … 64×64) |
| `Ctrl+S` / `Ctrl+O` | Save / load `maps/autosave.json` |
| `/` | Focus palette filter |
| `F3` | Toggle pointer debug logging (console) |
| `Esc` | Clear filter / back to main menu |
| **Palette** (right) | Click terrain row to select brush; wheel to scroll; click **Filter:** to type |
| Mouse wheel on canvas | Zoom (wheel over palette scrolls list) |

Pointer coordinates use [ScreenInput](./map-editor/03-render-bridge.md#pointer-coordinates) so the
OS cursor aligns with the grid on Windows (GLFW y-down vs LibGDX y-up).

---

## Implementation modules

| Class | Responsibility | Spec unit |
| --- | --- | --- |
| `MapCell` | Per-cell terrain (+ furniture later) | [01](./map-editor/01-grid-model.md) |
| `MapGrid` | 2D grid CRUD, resize | [01](./map-editor/01-grid-model.md) |
| `MapFileIO` | Save/load map JSON v1 | [04](./map-editor/04-map-file-format.md) |
| `TileSpriteResolver` | fg/bg resolve, animation pick, `hasDrawableArt` | [03](./map-editor/03-render-bridge.md) |
| `MapEditorScreen` | Camera, render loop, input, tileset session | [03](./map-editor/03-render-bridge.md) |
| `MapPalettePanel` | Terrain list, filter, tileset header, paintable-only rows | [02](./map-editor/02-palette-and-paint.md) |
| `MapEditorToolbar` | Mouse-first tool and file controls | [02](./map-editor/02-palette-and-paint.md) |

**Upstream:**

| Class | From |
| --- | --- |
| `LoadedGameData`, `TerrainRegistry` | [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md) G2+ |
| `LoadedTileset`, `TilesetLoadSession` | [TILESET_LOADER.md](./TILESET_LOADER.md) |

---

## Suggested PR slices (map editor)

| PR | Status | Done when |
| --- | --- | --- |
| **M1** `MapCell`, `MapGrid`, `MapFileIO` | done | `MapFileIOTest`, fixture `maps/test_10x10.json` |
| **M2** `TileSpriteResolver`, `MapEditorScreen` render | done | Checkerboard + pan/zoom |
| **M3** `MapPalettePanel`, paint input | done | Click/drag paint, eyedropper |
| **M4** filter, resize, HUD, toolbar, menu entry | done | Mouse palette; `[`/`]` tileset + spinner |

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.map.*"
```

---

## Palette: paintable-only rows

The sidebar lists terrain ids from `TerrainRegistry` that have **drawable art** in the active
tileset (`TileSpriteResolver.hasDrawableArt`). Entries with no matching tile (or empty sprite
lists) are hidden so the list does not show `unknown` / placeholder glyphs.

The map canvas still uses `unknown` fallback when **displaying** cells whose terrain id is not
in the tileset.

Header shows **`Tileset: <id>  n/m`** and **`shown/total in tileset`**. While swapping packs,
`TilesetLoadSession` runs on the render thread with a **spinner** over the palette panel.

---

## End-to-end order

```text
G1 → G2 → G3 → M1 → M2 → M3 → M4 → (G4, G5 as needed)
```

Minimum paintable editor today: **G2 + M1–M4** (furniture G3 optional for terrain-only painting).

---

## Related

- [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) — shared draw/animation patterns
- [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md) — game data PR slices (G1–G5)
- [INCREMENTAL_LOADING.md](./INCREMENTAL_LOADING.md) — `TilesetLoadSession`
- [map-editor/implementation-plan.md](./map-editor/implementation-plan.md)
