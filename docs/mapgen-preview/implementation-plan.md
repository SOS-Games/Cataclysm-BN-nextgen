# Implementation plan — mapgen preview

Agent-oriented guide for JSON mapgen → grid preview. Spec units: [README](./README.md).

---

## Goal

Generate a **visible BN building** into `MapGrid` and show it in the map editor — without
porting overmap, `mapbuffer`, or `oter_mapgen` weighted selection.

**First success criterion:** `house09.json` ground floor recognizable (walls + floor + some
furniture after P4) with RetroDays or similar tileset beside BN data.

---

## Dependencies

| Upstream | Provides |
| --- | --- |
| [Game data G1–G5](../game-data-loader/implementation-plan.md) | `loadMods`, `TerrainRegistry`, `FurnitureRegistry` |
| [Map editor M1–M4](../map-editor/implementation-plan.md) | `MapGrid`, `MapEditorScreen`, `TileSpriteResolver` |
| [Tileset loader](../tileset-loader/implementation-plan.md) | `LoadedTileset`, `TilesetLoadSession` |
| [JsonDataScanner](../game-data-loader/04-json-dispatch.md) | JSON envelope parse |

---

## Deliverables by PR

### P1 — Palettes

| Class | Responsibility |
| --- | --- |
| `MapgenScanOptions` | Roots, mod ids, scan flags |
| `PaletteCharResolver` | String / weighted / param → id |
| `MapgenPalette` | Code point maps |
| `PaletteRegistry` | Id lookup + `merge()` |
| `PaletteLoader` | Scan `mapgen_palettes/` |
| `MapgenLoadResult` | Registry + warnings |

**Tests:** `PaletteLoaderTest`, `PaletteCharResolverTest`, `PaletteRegistryMergeTest`

### P2 — Runner

| Class | Responsibility |
| --- | --- |
| `MergedCharMap` | Ter + furn maps after merge |
| `JsonMapgenDefinition` | Catalog entry + `object` JsonValue |
| `JsonMapgenLoader` | Scan `mapgen/` |
| `MapgenCatalog` | Index by `om_terrain` |
| `JsonMapgenRunner` | `rows` → `MapGrid` |
| `JsonMapgenRunOptions` | `defaultFillTer`, warnings |

**Tests:** `JsonMapgenRunnerTest` with `mapgen-fixtures/`

### P3 — UI

| Class | Responsibility |
| --- | --- |
| `MapgenPreviewService` | Load catalog + generate |
| `MapgenPickerDialog` | Filter + select |
| `MapEditorScreen` | Toolbar button, `Ctrl+G`, `importMapgen()` |

**Tests:** Manual integration; optional headless runner test without GL

### P4 — Furniture draw

| Change | Responsibility |
| --- | --- |
| `MapEditorScreen.drawCellFurniture` | Layer over terrain |
| `showFurnitureLayer` + `F` key | Toggle |

**Tests:** Manual visual

### P5 — Building floors

| Class | Responsibility |
| --- | --- |
| `CityBuildingLoader` | Scan `multitile_city_buildings.json` |
| `CityBuildingDefinition` / `CityBuildingPiece` | Layout metadata |
| `OvermapTerrainResolver` | Strip rotation; catalog lookup |
| `MapVolume` | z-level → `MapGrid` |
| `MapVolumeBuilder` | Run mapgen per floor |
| `MapgenPreviewService.generateBuilding` | Orchestrate bundle |
| `MapEditorScreen` | PageUp/PageDown floor switch |
| `MapgenPickerDialog` | **Import building** action |

**Tests:** `CityBuildingLoaderTest`, `MapVolumeBuilderTest`, fixture duplex

**Spec:** [09](./09-building-bundles-overview.md), [10](./10-city-building-loader.md), [11](./11-map-volume-and-floors.md)

### P6 — OMT stitch

| Class | Responsibility |
| --- | --- |
| `OmtStitchComposer` | Blit multiple pieces per z |
| `MapGrid.blit` or compose helper | Copy ter/furn into canvas |
| `MapVolumeBuilder` | Call stitch when multi-OMT |

**Tests:** `OmtStitchComposerTest` with two fixture mapgens at offset

**Spec:** [12](./12-omt-stitch-composer.md)

**Success criterion:** `2StoryModern04` ground floor pan-able at 48×24 (sibling BN data).

### P7a — Bundle discovery

| Class | Responsibility |
| --- | --- |
| `BuildingBundleScanner` | Walk each mod content tree for `city_building` + static `overmap_special` |
| `CityBuildingLoader` | Delegate to scanner; parse `city_building` objects |
| `OvermapSpecialBuildingLoader` | Parse static specials; vertical stack grouping |

**Tests:** `BuildingBundleScannerTest`, Arcana integration (`house_arcana` when mod present)

### P7b — Implicit bundles — **done**

| Class | Responsibility |
| --- | --- |
| `MapgenFileBundleInferrer` | Same-file mapgens with shared `om_terrain` prefix + floor suffixes |
| `MapgenPreviewService` | `augment()` after explicit bundle scan |

**Tests:** `MapgenFileBundleInferrerTest` (`implicit_cottage.json` fixture)

**Spec:** [13](./13-building-bundle-sources.md) — P7b–P7c done

### P7c — Whole static special — **done**

| Class | Responsibility |
| --- | --- |
| `SpecialLayoutImporter` | Register multi-column `overmap_special` as one bundle (id = special id) |
| `SpecialLayoutFloorComposer` | Combined ground mapgen + per-z placement via ground anchors |
| `OmTerrainMapgenPlacer.blitAtReferenceCell` | Blit upper-floor mapgen at reference-grid cell |

**Tests:** `SpecialLayoutFloorComposerTest` (`test_special_wide` fixture; `Farm_2side` integration)

---

## PR checklist

| PR | Compile | Unit tests | Manual smoke |
| --- | --- | --- | --- |
| P1 | ✓ | palette fixtures ✓ | — |
| P2 | ✓ | room 5×5 grid asserts | — |
| P3 | ✓ | `MapgenPreviewServiceTest` | `Ctrl+G` + toolbar Mapgen |
| P4 | ✓ | — | furniture visible |
| P5 | ✓ | city building + volume fixtures ✓ | `house_09` floor switch |
| P6 | ✓ | stitch fixture | `2StoryModern04` 2×2 pan |

Each PR: `gradlew.bat compileJava` && `gradlew.bat :core:test`

---

## Fixture design

### `minimal_palette.json`

```json
{
  "type": "palette",
  "id": "minimal",
  "terrain": { ".": "t_floor", "#": "t_wall" },
  "furniture": { "H": "f_chair" }
}
```

### `simple_room.json`

5×5 room — see [05-rows-runner.md](./05-rows-runner.md).

Place under `core/src/test/resources/mapgen-fixtures/` with loader resolving fixture data root
(similar to `gamedata-mod-fixtures` pattern).

### BN integration (optional, `@EnabledIf`)

```java
@EnabledIf("siblingBnDataPresent")
void house09Integration() {
    // load palettes from ../Cataclysm-BN/data
    // run house09.json index 0
    assertEquals(24, grid.width());
}
```

---

## v1 simplifications (do not expand mid-PR)

Documented in [01](./01-overview-and-scope.md) and [08](./08-v2-parity-roadmap.md):

- Weighted palette → first entry
- Parameters → fallback only
- No parent palette inheritance
- No `place_*`, `set`, nested, rotation
- No builtin/lua mapgen

---

## Code style

Match [AGENTS.md](../../AGENTS.md):

- Reuse `JsonDataScanner` / LibGDX `JsonValue`
- Small classes per table above
- Warnings as `List<String>`, log with `java.util.logging` or `Gdx.app.log` in UI layer
- No mapgen load on worker thread if it touches `JsonValue` tied to UI (CPU-only is fine on worker; v1 main thread ok)

---

## Agent entry point

1. Read [MAPGEN_PREVIEW.md](../MAPGEN_PREVIEW.md)
2. Implement current PR only
3. Turn unit **Verification** sections into tests
4. Do not start overmap / worldgen code in P1–P6 branches

---

## Related

- [MAPGEN_PREVIEW.md](../MAPGEN_PREVIEW.md)
- [v2-implementation-plan.md](./v2-implementation-plan.md)
- [08-v2-parity-roadmap.md](./08-v2-parity-roadmap.md)
- [09-building-bundles-overview.md](./09-building-bundles-overview.md)
