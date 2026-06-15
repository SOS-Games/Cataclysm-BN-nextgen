# Sprite viewer

Grid browser for loaded tileset sprites. Intended as a **long-term in-game reference tool** for
modders and developers — not a throwaway debug harness.

## Code

| Piece | Path |
| --- | --- |
| Screen | `core/src/main/java/io/gdx/cdda/bn/nextgen/view/TileDisplayScreen.java` |
| Entry (current) | `core/src/main/java/io/gdx/cdda/bn/nextgen/Main.java` |
| Desktop launcher | `lwjgl3/src/main/java/io/gdx/cdda/bn/nextgen/lwjgl3/Lwjgl3Launcher.java` |

The viewer consumes **`LoadedTileset`** only — it does not parse JSON or PNGs itself. Any fix to
loader output is validated here before it reaches gameplay rendering.

## Run

```bash
# Windows
gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

When `../Cataclysm-BN/gfx` exists next to this repo, the `lwjgl3:run` Gradle task sets
`-Dcdda.gfx.roots` automatically. Otherwise set the property manually:

```bash
gradlew.bat lwjgl3:run -Dcdda.gfx.roots=C:/path/to/Cataclysm-BN/gfx
```

Default window size is **960×720**. Rendering uses **pixel coordinates** (`HdpiMode.Pixels`) so
sprites and labels stay the same size when the window is resized; extra space shows more tiles.

## Controls

| Input | Action |
| --- | --- |
| `←` / `→` or `A` / `D` | Previous / next page |
| Mouse wheel up | Next page |
| Mouse wheel down | Previous page |
| `F` | Cycle FX table (NONE → SHADOW → NIGHT → …) |
| `1`–`8` | Jump to FX table directly |
| `+` / `-` | Zoom (integer scale 1–6, multiplied by tileset `pixelscale`) |
| `R` | Reload tileset from disk |

## Tileset selection

On load, the viewer picks the first available pack from this preference list:

1. `hoder`
2. `retrodays`
3. `UltimateCataclysm`
4. First id returned by discovery

**Planned:** explicit tileset picker in the HUD.

## Grid layout

- Preferred tile ids (`t_dirt`, `t_grass`, `player`, …) appear first; remaining ids are alphabetical.
- Each cell draws **background** then **foreground** layers with tile offsets applied.
- Cell size is fixed for a given zoom level; resize only changes column/row count.
- Labels truncate with `…` when the cell is too narrow.

## FX preview

Draws from the eight baked sprite tables ([06c](./tileset-loader/06c-filtered-variants.md)) via
`LoadedTileset.getTexture(index, TilesetFxType)`. Dynamic-atlas loads use the same API.

## Planned features

| Feature | Notes |
| --- | --- |
| Tileset picker | Cycle or list all discovered packs |
| Search / filter | Jump to tile id, prefix filter |
| Detail pane | Indices, offsets, seasonal variants, state modifiers |
| Game entry point | Debug menu or keybind instead of booting straight into the viewer |

## Related

- [TILESET_LOADER.md](./TILESET_LOADER.md) — loader milestone and package layout
- [tileset-loader/08-in-memory-model.md](./tileset-loader/08-in-memory-model.md) — `LoadedTileset` contract
