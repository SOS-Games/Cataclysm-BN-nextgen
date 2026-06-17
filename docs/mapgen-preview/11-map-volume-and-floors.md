# 11 — Map volume and floor UI

Hold **one `MapGrid` per z-level** and switch floors in the map editor without re-importing.

---

## Purpose

Replace single-grid-only editor state with a **`MapVolume`** when user imports a
`city_building` bundle ([09](./09-building-bundles-overview.md)). P5 delivers floor switching;
P6 may replace per-floor grid with a **stitched** larger grid ([12](./12-omt-stitch-composer.md)).

---

## `MapVolume` model

```java
public final class MapVolume {

    private final String buildingId;           // city_building id, or ""
    private final List<Integer> zLevels;       // sorted, e.g. [-1, 0, 1]
    private final Map<Integer, MapGrid> gridsByZ;
    private int activeZ;

    public MapGrid getActiveGrid();
    public void setActiveZ(int z);
    public int getActiveZ();
    public Optional<Integer> nextZ();   // higher floor, for PageUp
    public Optional<Integer> previousZ(); // lower floor, for PageDown
}
```

### Invariants

| Rule | Notes |
| --- | --- |
| Every z in `zLevels` has a non-null `MapGrid` | Missing piece → omit that z from list |
| `activeZ` ∈ `zLevels` | Default: lowest z with a grid, prefer z=0 if present |
| Single-floor import | `MapVolume` optional — editor may keep flat `MapGrid` |

**Persistence:** v1.5 does not save `MapVolume` to map JSON ([04-map-file-format](../map-editor/04-map-file-format.md) v2 may add `floors[]`). Export active floor only via existing save.

---

## `MapVolumeBuilder`

```java
public final class MapVolumeBuilder {

    public MapVolume build(
        CityBuildingDefinition building,
        MapgenCatalog catalog,
        PaletteRegistry palettes,
        LoadedGameData gameData,
        JsonMapgenRunOptions runOptions
    ) {
        warnings ← []
        gridsByZ ← empty
        for z in building.distinctZLevels():
            pieces ← building.piecesAtZ(z)
            if pieces.size() == 1 && pieces[0].offsetX == 0 && pieces[0].offsetY == 0:
                def ← resolveMapgen(catalog, pieces[0])
                grid ← JsonMapgenRunner.run(def, …)
                gridsByZ.put(z, grid)
            else:
                // P6: OmtStitchComposer.stitch(pieces, …)
                defer or call stitch when P6 landed
        return new MapVolume(building.getId(), sortedKeys(gridsByZ), gridsByZ, pickDefaultZ(…))
    }
}
```

P5 implementation: **only single-OMT floors** (typical `house_09`). Multi-tile z-levels log
“stitch required” until P6.

---

## Editor integration

### State

```java
// MapEditorScreen
private MapGrid grid;           // existing — active working grid
private MapVolume mapVolume;    // null when single-floor / hand-painted

private void setVolume(MapVolume volume) {
    this.mapVolume = volume;
    this.grid = volume.getActiveGrid();
    centerCameraOnGrid();
}
```

Rendering and paint continue to use `grid` reference; floor switch updates `grid` pointer.

### Floor controls (v1.5)

| Input | Action |
| --- | --- |
| **PageUp** | `activeZ ← next higher floor` |
| **PageDown** | `activeZ ← next lower floor` |
| **`[` / `]`** | Same (if not conflicting — use Page keys if `[`/`]` taken by tileset) |
| Toolbar **Floor ▲ / ▼** | Optional P5b |

**Conflict note:** `[` / `]` switch tilesets today. Prefer **PageUp/PageDown** for floors;
document in [MAP_EDITOR.md](../MAP_EDITOR.md).

### HUD

```text
Building: house_09  |  floor z=0 (2/3)  |  grid 24×24
```

Label z-levels when helpful: `basement (z=-1)`, `ground (z=0)`, `roof (z=1)` — heuristic from
`om_terrain` suffix (`roof`, `basement`) optional.

---

## `MapgenPreviewService` extension

```java
public MapgenBuildingResult generateBuilding(
    CityBuildingDefinition building,
    LoadedGameData gameData,
    JsonMapgenRunOptions runOptions
);

public static final class MapgenBuildingResult {
    MapVolume volume;
    List<String> runWarnings;
}
```

Called from picker **Import building** path.

---

## Picker changes (P5)

```text
┌─ Import mapgen ─────────────────────────────┐
│ Filter: [ house_09___________ ]             │
│                                             │
│ ► house_09 (3 floors)     [Import building] │
│   house_09              house09.json #0     │
│   house_09_roof         house09.json #1     │
│   …                                         │
└─────────────────────────────────────────────┘
```

- **Import building** runs `MapVolumeBuilder` for highlighted bundle row.
- Single row **Generate** unchanged (one floor, replaces volume with null).

---

## Interaction with paint / save

| Feature | P5 behavior |
| --- | --- |
| Paint | Active floor only |
| Save map | Active floor → existing `MapFileIO` (warn: “single floor export”) |
| Resize grid | Disabled while `mapVolume != null` OR resizes active floor only (pick one; recommend disable) |
| Mapgen re-import | Replaces entire volume |

---

## Inputs

- `CityBuildingDefinition` + resolved `JsonMapgenDefinition` per piece
- `JsonMapgenRunner`, palettes, game data

## Outputs

- `MapVolume`
- Warnings per skipped floor/piece

## Failure modes

| Condition | Behavior |
| --- | --- |
| No runnable pieces | Error dialog; keep previous grid |
| Partial floors | Volume with available z only; warn |
| User on stitched floor (P6) | Same floor API; grid may be 48×48 |

## Verification

1. Import `house_09` building: PageDown/PageUp cycles basement → ground → roof
2. Paint on ground does not affect roof grid
3. Single-floor Generate clears `mapVolume`
4. `MapVolumeBuilderTest` on duplex fixture (two z, no GL)

---

## Related

- [06-preview-ui](./06-preview-ui.md)
- [12-omt-stitch-composer](./12-omt-stitch-composer.md)
- [map-editor/01-grid-model](../map-editor/01-grid-model.md)
