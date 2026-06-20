# Implementation plan — world generation

Agent-oriented guide for overmap + submap generation. Unit specs: [README](./README.md).

---

## Goal

**Pan an overmap, visit tiles, see BN-accurate submaps generate on demand** — starting small
(W2 8×8 grid) before full 180×180 BN scale.

**First success criterion (W2+W3):** 8×8 overmap filled with hand-placed or noise `forest` /
`field` OMT ids; clicking a cell loads a 24×24 submap in the map editor with stable seed.

---

## Dependencies

| Upstream | Provides |
| --- | --- |
| [Mapgen preview v2](../mapgen-preview/v2-implementation-plan.md) | `JsonMapgenRunner`, nested, regional, rotation |
| [Game data G1–G5](../game-data-loader/implementation-plan.md) | Registries, mod order |
| [Map editor M1–M4](../map-editor/implementation-plan.md) | Grid view, camera |
| [Building bundles](../mapgen-preview/09-building-bundles-overview.md) | `CityBuildingLoader`, stitch |

---

## Deliverables by PR

### W1 — Overmap terrain loader

| Class | Responsibility |
| --- | --- |
| `OvermapTerrainScanOptions` | Roots, mod ids (reuse game-data patterns) |
| `OvermapTerrainDefinition` | Parsed `type: overmap_terrain` entry |
| `OvermapTerrainLoader` | Scan `overmap_terrain/` trees |
| `OvermapTerrainRegistry` | Id lookup; `mapgen` id list + weights |

**Spec:** [02](./02-overmap-terrain-loader.md)

**Tests:** `OvermapTerrainLoaderTest` — core `t_forest` / `house_09` ids present

**Touches:** new package `io.gdx.cdda.bn.nextgen.worldgen.overmap`

---

### W2 — Mini-overmap grid

| Class | Responsibility |
| --- | --- |
| `OvermapGrid` | width × height array of OMT id strings |
| `OvermapGridFactory` | Empty, fill, noise stub, load from JSON fixture |
| `OvermapEditorMode` | Map editor screen: OMT tint + click select |
| `OvermapTerrainColors` | Debug color per OMT type (optional) |

**Spec:** [03](./03-mini-overmap-grid.md)

**Tests:** `OvermapGridTest`, UI smoke manual

**Does not:** procedural city placement (W4)

---

### W3 — Visit-tile mapgen

| Class | Responsibility |
| --- | --- |
| `OterMapgenIndex` | Join `OvermapTerrainRegistry` + `MapgenCatalog` |
| `MapgenPicker` | Weighted pick by `om_terrain` + z (seeded) |
| `SubmapCache` | `(omt_x, omt_y, z) → MapGrid` LRU |
| `SubmapGenerator` | Build `JsonMapgenRunOptions` with OMT rotation; call runner |
| `WorldgenPreviewService` | Facade: load registries, `visit(x,y,z)` |

**Spec:** [04](./04-visit-tile-mapgen.md)

**Tests:** `MapgenPickerTest`, `SubmapGeneratorTest` — matches direct runner for trivial weight

---

### W4 — City and static special placement ✓

| Class | Responsibility |
| --- | --- |
| `CityPlacer` | Place `city_building` defs on overmap grid (BN subset) |
| `StaticSpecialPlacer` | Static `overmap_special` from JSON |
| `OvermapGenerator` | Orchestrate: region → terrain fill → cities → specials |

**Spec:** [05](./05-city-and-special-placement.md)

**Tests:** `CityPlacerTest`, `OvermapGeneratorTest` — seed reproducibility; fixture building omt ids on grid

**Reuses:** [CityBuildingLoader](../mapgen-preview/10-city-building-loader.md)

---

### W5 — Rivers, roads, connections ✓

| Class | Responsibility |
| --- | --- |
| `OvermapConnectionLoader` | Load connection templates |
| `RiverGenerator` | BN river pass port (simplified v1) |
| `HighwayGenerator` | Road grid / connection stitching |

**Spec:** [06](./06-rivers-roads-connections.md)

**Tests:** `OvermapConnectionLoaderTest`, `RiverGeneratorTest`, `HighwayGeneratorTest`, `OvermapGeneratorTest`

**Note:** v1 straight river + orthogonal roads between placed sites; full BN hydrology deferred

---

### W6 — Mutable specials and joins ✓

| Class | Responsibility |
| --- | --- |
| `MutableSpecialLoader` | Parse `overmap_mutable/` |
| `SpecialPhaseAssembler` | Phases, joins, root node |
| `JoinContext` | Neighbor OMT ids for nested mapgen (feeds runner warnings) |

**Spec:** [07](./07-mutable-specials-and-joins.md)

**Tests:** `MutableSpecialLoaderTest`, `SpecialPhaseAssemblerTest`, `MutableSpecialPlacerIntegrationTest`

**Depends on:** W3 nested neighbor checks (optional v1: ignore joins)

---

## PR checklist

| PR | Compile | Unit tests | Manual smoke |
| --- | --- | --- | --- |
| W1 | ✓ | `OvermapTerrainLoaderTest` | registry size > 100 on BN data |
| W2 | ✓ | `OvermapGridTest` | overmap mode in editor |
| W3 | ✓ | `SubmapGeneratorTest` | click OMT → 24×24 grid |
| W4 | ✓ | `CityPlacerTest` | city on generated map |
| W5 | ✓ | connection + river/road tests | river/road visible on grid |
| W6 | ✓ | assembler tests | lab special footprint |

Each PR: `gradlew.bat compileJava` && `gradlew.bat :core:test`

---

## v1 simplifications (do not expand mid-PR)

| Topic | v1 choice |
| --- | --- |
| Overmap size | 8×8 or 16×16 until W4 stable; not full 180×180 |
| Z-levels | z=0 only for W3; multi-z submap stack in W3.1 |
| Builtin mapgen | Warn + skip; json only |
| Lua mapgen | Out of scope |
| Save format | No `.sav2`; in-memory only |
| NPCs / monsters | No spawn simulation |
| Region | Single `default` region from [region_settings](../mapgen-preview/19-regional-terrain.md) |
| RNG | Seeded per world; document salt per OMT coord |

---

## Package layout (target)

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/worldgen/
  overmap/
    OvermapTerrainLoader.java
    OvermapTerrainRegistry.java
    OvermapGrid.java
  generate/
    OvermapGenerator.java
    CityPlacer.java
  submap/
    SubmapGenerator.java
    SubmapCache.java
    MapgenPicker.java
  WorldgenPreviewService.java
  view/
    OvermapEditorMode.java   # optional W2 — may live under view/
```

---

## Agent workflow

1. Read [WORLDGEN.md](../WORLDGEN.md) and the unit doc for the PR
2. Implement one W slice; wire through existing loaders — do not fork mapgen runner
3. Add fixture under `core/src/test/resources/worldgen-fixtures/`
4. Update unit doc **Status** when done
