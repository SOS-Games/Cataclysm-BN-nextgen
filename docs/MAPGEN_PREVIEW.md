# Mapgen preview — implementation guide

Run **BN JSON mapgen** (`method: json`) into a [`MapGrid`](./map-editor/01-grid-model.md) and
view it in the map editor. This is a **preview slice** — not full world generation.

Specs: [`docs/mapgen-preview/`](./mapgen-preview/README.md)

---

## Why this exists

Full BN worldgen spans overmap layout, OMT types, weighted mapgen selection, z-levels, and
Lua hooks. The preview path isolates **palettes + rows → grid** so you can see real buildings
sooner.

| Full worldgen | Mapgen preview |
| --- | --- |
| `overmap.cpp` + `mapbuffer` | `JsonMapgenRunner` |
| Weighted `oter_mapgen` pick | User picks one definition or **building bundle** |
| 24×24×Z submaps | `MapVolume` per z (P5); stitched OMT (P6) |
| Regional terrain resolve | Literal `t_region_*` ids |

Detail: [01-overview-and-scope](./mapgen-preview/01-overview-and-scope.md). Building bundles:
[09-building-bundles-overview](./mapgen-preview/09-building-bundles-overview.md).
Bundle types and gaps: [13-building-bundle-sources](./mapgen-preview/13-building-bundle-sources.md).

---

## End-to-end data flow

```text
../Cataclysm-BN/data/json/
  mapgen_palettes/*.json  ──► PaletteRegistry
  mapgen/**/*.json        ──► MapgenCatalog

User picks JsonMapgenDefinition (e.g. house_09)
  JsonMapgenRunner        ──► MapGrid (terrain + furniture per cell)
  MapEditorScreen         ──► LoadedTileset sprites

Optional (P5–P6): city_building bundle
  CityBuildingLoader      ──► CityBuildingDefinition
  MapVolumeBuilder        ──► MapVolume (z → MapGrid)
  OmtStitchComposer       ──► stitched grid per floor (P6)
```

---

## Where to implement

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/
  mapgen/
    MapgenScanOptions.java
    MapgenPreviewService.java
    building/                   # P5 — city_building
      CityBuildingLoader.java
      MapVolume.java
    compose/                    # P6 — stitch
      OmtStitchComposer.java
    palette/
      PaletteLoader.java
      PaletteRegistry.java
      MapgenPalette.java
      PaletteCharResolver.java
      MergedCharMap.java
    json/
      JsonMapgenLoader.java
      JsonMapgenDefinition.java
      JsonMapgenRunner.java
    preview/
      MapgenCatalog.java
  map/                          # MapGrid (existing)
  view/
    MapEditorScreen.java        # P3 picker, P4 furniture draw
    MapgenPickerDialog.java     # optional P3
    MainMenuScreen.java
  gamedata/                     # G1–G5
  tileset/                      # LoadedTileset
```

Package root: `io.gdx.cdda.bn.nextgen.mapgen`

---

## v1 milestones

| Item | PR | Status |
| --- | --- | --- |
| Spec index (`docs/mapgen-preview/`) | — | done |
| Palette scan + char resolver | **P1** | done |
| JSON mapgen rows runner | **P2** | done |
| Preview UI (picker → editor) | **P3** | done |
| Furniture layer render | **P4** | done |
| Building bundle + floor switch | **P5** | done |
| Multi-OMT stitch per floor | **P6** | done |
| Full overmap / worldgen | — | out of scope ([08](./mapgen-preview/08-v2-parity-roadmap.md)) |

---

## Suggested PR slices

| Step | PR | Focus | Unit docs |
| --- | --- | --- | --- |
| 1 | **P1** | `PaletteLoader`, `PaletteRegistry`, `PaletteCharResolver` | [02](./mapgen-preview/02-scan-paths.md), [03](./mapgen-preview/03-palette-loader.md) |
| 2 | **P2** | `JsonMapgenLoader`, `JsonMapgenRunner`, fixtures | [04](./mapgen-preview/04-json-mapgen-format.md), [05](./mapgen-preview/05-rows-runner.md) |
| 3 | **P3** | `MapgenPreviewService`, picker, toolbar hook | [06](./mapgen-preview/06-preview-ui.md) |
| 4 | **P4** | `drawCellFurniture` in `MapEditorScreen` | [07](./mapgen-preview/07-furniture-render.md) |
| 5 | **P5** | `CityBuildingLoader`, `MapVolume`, floor UI | [09](./mapgen-preview/09-building-bundles-overview.md)–[11](./mapgen-preview/11-map-volume-and-floors.md) |
| 6 | **P6** | `OmtStitchComposer` | [12](./mapgen-preview/12-omt-stitch-composer.md) |

**Prerequisites:** game data **G1–G5**, map editor **M1–M4**, tileset loader v1, **P1–P4**.

---

## BN data paths

```text
../Cataclysm-BN/data/json/
  mapgen_palettes/house_general_palette.json   # standard_domestic_palette
  mapgen/house/house09.json                    # house_09 + roof
```

```bash
# Manual run when sibling clone present
gradlew.bat lwjgl3:run -Dcdda.data.roots=../Cataclysm-BN/data -Dcdda.gfx.roots=../Cataclysm-BN/gfx
```

Gradle `lwjgl3` may set these automatically when paths exist.

---

## v1 semantics (intentional limits)

| BN feature | Preview v1 |
| --- | --- |
| Palette weighted arrays | First entry only |
| `parameters` | `fallback` string only |
| Palette `palettes: [ parent ]` inheritance | Not loaded — use flat ids |
| `place_monsters`, `place_items`, `set`, `nested` | Ignored |
| `method: builtin` / `lua` | Not supported |
| `rotation`, `predecessor_mapgen` | Ignored |
| Fixed 24×24 size | Use actual row bounds |
| Multitile autoconnect | Not drawn |

---

## Run (after P3)

```bash
gradlew.bat lwjgl3:run
```

Main menu → **Map Editor** → toolbar **Mapgen** (or **`Ctrl+G`**) → filter `house_09` →
**Generate**.

Roof: pick `house_09_roof` from same file’s second entry — or **Import building** (P5) for all floors.

After P5: **PageUp** / **PageDown** switch z-level. After P6: pan across multi-OMT footprints.

---

## Tests

| PR | Tests |
| --- | --- |
| P1 | `PaletteLoaderTest`, `PaletteCharResolverTest`, merge test |
| P2 | `JsonMapgenRunnerTest` + `core/src/test/resources/mapgen-fixtures/` |
| P3 | Manual: `house09` from sibling BN data |
| P4 | Manual: furniture visible; optional `F` toggle |
| P5 | Manual: `house_09` 3 floors; `MapVolumeBuilderTest` |
| P6 | Manual: `2StoryModern04` 48×24 pan; `OmtStitchComposerTest` |

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.mapgen.*"
```

### Fixture layout (P1/P2)

```text
core/src/test/resources/mapgen-fixtures/
  palettes/minimal_palette.json
  mapgen/simple_room.json
```

Self-contained — no BN checkout required for CI.

---

## Agent entry point

1. [AGENTS.md](../AGENTS.md)
2. [mapgen-preview/implementation-plan.md](./mapgen-preview/implementation-plan.md)
3. Current PR unit doc → implement → `gradlew.bat compileJava test`

---

## Related

- [MAP_EDITOR.md](./MAP_EDITOR.md) — grid UI
- [GAME_DATA_LOADER.md](./GAME_DATA_LOADER.md) — registries
- [TILESET_LOADER.md](./TILESET_LOADER.md) — sprites
- BN: `src/mapgen.cpp`, `src/mapgenformat.cpp`
- BN author: `docs/en/mod/json/reference/mapgen.md`
