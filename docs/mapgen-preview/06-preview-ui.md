# 06 — Preview UI

User flow: discover mapgens → pick one → generate `MapGrid` → view in map editor.

---

## Purpose

Expose mapgen preview without hand-editing JSON paths. Reuse [map editor render bridge](../map-editor/03-render-bridge.md)
and existing tileset load session.

---

## Entry points (v1)

| Entry | Priority | Behavior |
| --- | --- | --- |
| **Map editor toolbar** | P3 primary | “Mapgen…” opens picker; replaces current grid |
| **Main menu** | P3 optional | “Mapgen Preview” → editor with picker open |
| **JUnit / API** | P2 | `JsonMapgenRunner.run` without UI |
| **Sprite viewer** | defer | Low value |

**Recommendation:** implement toolbar + dialog first ([MAP_EDITOR.md](../MAP_EDITOR.md) bottom toolbar).

---

## `MapgenPreviewService` (planned orchestrator)

```java
public final class MapgenPreviewService {

    private PaletteRegistry palettes;
    private MapgenCatalog catalog;
    private List<String> loadWarnings;

    public void ensureLoaded(MapgenScanOptions options) throws IOException;

    public MapgenPreviewResult generate(
        JsonMapgenDefinition definition,
        LoadedGameData gameData,
        JsonMapgenRunOptions runOptions
    );

    public static final class MapgenPreviewResult {
        MapGrid grid;
        List<String> runWarnings;
    }
}
```

Called from `MapEditorScreen` on render thread (no OpenGL in service).

---

## Load sequence

```text
MapEditorScreen.importMapgen():
    1. gameData ← already loaded (or GameDataLoader.loadMods)
    2. previewService.ensureLoaded(MapgenScanOptions.defaults())
         → PaletteRegistry + MapgenCatalog + loadWarnings
    3. userPick ← MapgenPickerDialog.show(catalog)
    4. result ← previewService.generate(userPick, gameData, options)
    5. grid ← result.grid
    6. setGrid(grid); centerCameraOnGrid(); fitZoom()
    7. statusMessage ← "Mapgen: " + userPick.displayName() + " " + w + "×" + h
    8. runGameDataValidation(tileset)  // existing gfx cross-check
    9. log loadWarnings + result.runWarnings to Gdx.app
```

Tileset: unchanged — `TilesetLoadSession` if not loaded.

---

## `MapgenPickerDialog` (v1 minimal)

Text UI acceptable for P3 — no Scene2D skin required initially.

```text
┌─ Import mapgen ─────────────────────────────┐
│ Filter: [ house_09___________ ]             │
│                                             │
│  house_09        house09.json #0   w:300   │
│  house_09_roof   house09.json #1           │
│  shelter         shelter.json #0           │
│  …                                          │
│                                             │
│  [Generate]  [Cancel]                       │
└─────────────────────────────────────────────┘
```

### List row fields

| Column | Source |
| --- | --- |
| Label | `om_terrain[0]` or `displayName()` |
| Source | relative path + `#index` |
| Weight | `weight` if ≠ 1000 |
| Runnable | gray out if `!isJsonPreviewSupported()` |

Filter: case-insensitive substring on `om_terrain`, filename, path.

### Keyboard

| Key | Action |
| --- | --- |
| Up/Down | Move selection |
| Enter | Generate |
| Esc | Cancel |
| Typing | Filter box when focused |

Mouse: click row to select; double-click generate.

---

## `MapgenCatalog` API

```java
public final class MapgenCatalog {
    public List<JsonMapgenDefinition> all();
    public List<JsonMapgenDefinition> runnableOnly();
    public List<JsonMapgenDefinition> filter(String query);
    public List<JsonMapgenDefinition> findByOmTerrain(String id);
    public Optional<JsonMapgenDefinition> findExact(String omTerrain, Path file, int index);
}
```

Sort default: `om_terrain` asc, then `sourceFile`, then `indexInFile`.

---

## Preview mode behavior

| Editor feature | v1 policy |
| --- | --- |
| Paint / erase | **Allowed** — user may fix gaps after preview |
| Save | `MapFileIO` — saves nextgen format, not mapgen JSON |
| Palette brush | Unchanged |
| Status HUD | `Mapgen: house_09 (24×24) | 12 validation issue(s)` |
| Grid resize | Allowed — may desync from source mapgen |

Optional v2: `readOnlyPreview` flag disables paint.

---

## HUD / errors

| State | `statusMessage` / log |
| --- | --- |
| Scanning | `Loading mapgen catalog…` |
| Generating | `Generating house_09…` |
| Unknown palette | `game-data` log: `[mapgen] unknown palette 'foo'` |
| Unsupported | Dialog: `builtin mapgen not supported` |
| No data roots | `Set -Dcdda.data.roots=…` on menu |
| Success | `Mapgen: house_09 (24×24)` |
| Partial gfx | Existing validation summary |

---

## Controls (incremental)

Reuse [MAP_EDITOR.md](../MAP_EDITOR.md) bindings. Add:

| Input | Action |
| --- | --- |
| Toolbar **Mapgen** button | Open picker |
| **`Ctrl+G`** | Open picker (when editor focused) |
| Picker **Generate** | Run and close |
| Picker **Esc** | Cancel |

---

## Gradle / data paths

When sibling BN exists, `lwjgl3/build.gradle` may set:

```text
-Dcdda.data.roots=../Cataclysm-BN/data
-Dcdda.gfx.roots=../Cataclysm-BN/gfx
```

Preview requires **data** for mapgen JSON + palettes; **gfx** for sprites.

---

## Performance notes

| Step | v1 expectation |
| --- | --- |
| First catalog scan | 1–3 s blocking on core mod (acceptable with spinner) |
| Subsequent generate | < 50 ms for single house |
| Re-open picker | Reuse cached `MapgenPreviewService` |

Show `LoadingSpinner` overlay during first `ensureLoaded` (same as tileset swap).

---

## BN source reference

N/A — nextgen-local UI.

---

## Inputs

- User selection from catalog
- `MapgenPreviewService` loaded state

## Outputs

- Updated `MapGrid` in editor
- Console + HUD messages

## Failure modes

| Condition | UX |
| --- | --- |
| No data roots | Disable mapgen button; tooltip with property |
| Scan exception | Dialog with first warning |
| Runner exception | Keep previous grid; show error |
| Empty runnable list | Dialog: no json mapgens found |

## Verification

1. `Ctrl+G` → filter `house_09` → generate → 24×24 grid with walls visible
2. Fixture-only test without BN checkout via `mapgen-fixtures`
3. Tileset swap after preview still works
4. Save → reload `MapFileIO` preserves terrain
5. `house_09_roof` generates different `fill_ter` than ground floor
6. Spinner shown during initial catalog load
