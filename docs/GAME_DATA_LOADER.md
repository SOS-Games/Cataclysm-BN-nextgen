# Game data loader — implementation guide

Java/LibGDX implementation of **Cataclysm-BN game JSON** loading (terrain, furniture, and
eventually other `type` dispatch targets). Behavioral specs live under
[`docs/game-data-loader/`](./game-data-loader/README.md).

This loader reads **`data/json/`** (and mod paths) — not gfx tilesets. Gfx remains
[`TILESET_LOADER.md`](./TILESET_LOADER.md). The two meet at **string ids** (`t_dirt`, `f_chair`)
that tilesets also reference in `tile_config.json`.

---

## Where to implement

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/gamedata/
  DataPaths.java              # unit 02 — data/json root resolution
  ModDiscovery.java           # unit 03 — modinfo.json scan
  GameDataLoader.java         # unit 06 — blocking orchestration
  load/                       # load context, options, session (future)
  parse/                      # units 07a, 07b
  model/                      # unit 08 — TerrainRegistry, FurnitureRegistry, …
  validate/                   # unit 10
```

Map editor / grid UI stays in `view/` and `map/` — see [MAP_EDITOR.md](./MAP_EDITOR.md).

---

## Specification

| Document | Path |
| --- | --- |
| Index + scope | [game-data-loader/README.md](./game-data-loader/README.md) |
| **Implementation plan** | [game-data-loader/implementation-plan.md](./game-data-loader/implementation-plan.md) |
| Unit specs | [game-data-loader/01-…](./game-data-loader/01-overview-and-lifecycle.md) through [10-…](./game-data-loader/10-post-load-validation.md) |

Implement in unit dependency order. BN C++ sources listed in each unit doc are the authority
for ambiguous behavior.

---

## Data on disk

Point `DataPaths` at a BN checkout:

```text
data/
  mods/bn/modinfo.json       # core mod → path: "../../json"
  json/
    furniture_and_terrain/
      terrain-floors-outdoors.json
      furniture-recreation.json
    mapgen/ …
    items/ …
```

Example roots:

| Path | Purpose |
| --- | --- |
| `../Cataclysm-BN/data` | Primary game data |
| `cdda.data.roots` (property) | Semicolon-separated overrides |

System properties (planned): `cdda.data.roots`, `cdda.mod.roots` — mirror
[`GfxPaths`](../../core/src/main/java/io/gdx/cdda/bn/nextgen/tileset/GfxPaths.java).

---

## v1 milestone (map editor slice)

| Item | Status |
| --- | --- |
| Spec index (`docs/game-data-loader/`) | done (deep-dive units 01–10) |
| `DataPaths` — resolve `data/json` | done (G1) |
| Scan `furniture_and_terrain/*.json` | done (G1) |
| Parse `type: terrain` entries | done (G2) |
| Parse `type: furniture` entries | done (G3) |
| `TerrainRegistry` / `FurnitureRegistry` lookup by id | done (G2/G3) |
| Core mod only (skip mod dependency tree) | done (G1 scan; no mod order yet) |
| Mod load order + overrides | todo (G5) |
| Post-load validation / gfx cross-check | todo (G4) |
| Full BN `DynamicDataLoader` parity (~100 types) | out of scope |
| Items, monsters, recipes, mapgen execution | out of scope |

**Acceptance:** `GameDataLoader.loadCore()` returns registries where `findTerrain("t_dirt")`
has id and name from BN JSON; ids match `LoadedTileset.findTile("t_dirt")` when gfx is loaded.

---

## Implementation modules

| Module / class | Responsibility | Spec units |
| --- | --- | --- |
| `DataPaths` | Resolve `data/`, `data/json`, `data/mods` | [02](./game-data-loader/02-path-resolution.md) |
| `ModDiscovery` | Scan `modinfo.json` → `ModRegistry` | [03](./game-data-loader/03-mod-discovery.md) |
| `ModOrderResolver` | Core-first + dependency order | [09](./game-data-loader/09-mod-load-order.md) |
| `JsonDataScanner` | Recursive `*.json` scan + envelope parse | [04](./game-data-loader/04-json-dispatch.md) |
| `GameDataLoadOptions` | Roots, scan subdirs, strict flags | [06](./game-data-loader/06-load-pipeline.md) |
| `GameDataLoader` | `loadCore()` / `loadMods()` orchestration | [06](./game-data-loader/06-load-pipeline.md) |
| `TerrainParser` | `type: terrain` → model | [05a](./game-data-loader/05a-terrain-config.md), [07a](./game-data-loader/07a-parse-terrain.md) |
| `FurnitureParser` | `type: furniture` → model | [05b](./game-data-loader/05b-furniture-config.md), [07b](./game-data-loader/07b-parse-furniture.md) |
| `TerrainDefinition`, `FurnitureDefinition` | Immutable records | [08](./game-data-loader/08-in-memory-model.md) |
| `TerrainRegistry`, `FurnitureRegistry` | Id lookup + sorted `allIds()` | [08](./game-data-loader/08-in-memory-model.md) |
| `LoadedGameData` | Aggregate returned to consumers | [08](./game-data-loader/08-in-memory-model.md) |
| `GameDataValidator` | Post-load warnings / gfx cross-check | [10](./game-data-loader/10-post-load-validation.md) |

Package root: `io.gdx.cdda.bn.nextgen.gamedata`

---

## Suggested PR slices (game data)

Implement in order. Each PR should add tests under `core/src/test/java/.../gamedata/`.

| PR | Classes | Units | Done when |
| --- | --- | --- | --- |
| **G1** | `DataPaths`, `JsonDataScanner`, `GameDataLoadOptions`, stub `GameDataLoader` | 02, 04, 06 | `DataPaths` resolves `../Cataclysm-BN/data/json`; scanner lists `furniture_and_terrain/*.json`; envelope parses array fixture |
| **G2** | `TerrainDefinition`, `TerrainRegistry`, `TerrainParser`, `LoadedGameData`, `GameDataLoader.loadCore` | 05a, 07a, 08 | `loadCore()` → `find("t_dirt").name == "dirt"`; terrain count > 500 on real data |
| **G3** | `FurnitureDefinition`, `FurnitureRegistry`, `FurnitureParser` | 05b, 07b, 08 | `find("f_*")` works; `LoadedGameData` exposes both registries |
| **G4** | `GameDataValidator`, `ValidationReport` | 10 | `looks_like` warnings; optional `LoadedTileset` missing-gfx report |
| **G5** | `ModDiscovery`, `ModRegistry`, `ModOrderResolver`, `GameDataLoader.loadMods` | 03, 09, 06 | Two-mod fixture: later mod overrides same terrain id |

### G1 — paths + scan

```text
gamedata/
  DataPaths.java
  load/GameDataLoadOptions.java
  parse/JsonDataScanner.java
  GameDataLoader.java          // scan-only entry: listJsonFiles(), countObjects(type)
```

**Tests:** `DataPathsTest`, `JsonDataScannerTest` (fixture dir in `src/test/resources/gamedata/`)

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.gamedata.DataPathsTest"
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.gamedata.JsonDataScannerTest"
```

### G2 — terrain registry

```text
gamedata/
  model/TerrainDefinition.java
  model/TerrainRegistry.java
  model/LoadedGameData.java
  parse/TerrainParser.java
  GameDataLoader.java          // loadCore() complete
```

**Tests:** `TerrainParserTest` (inline JSON), `GameDataLoaderTest` (integration, `-Dcdda.data.roots=...`)

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.gamedata.GameDataLoaderTest"
```

**Done when:** `GameDataLoader.loadCore()` returns non-empty `TerrainRegistry`; `t_dirt` present.

### G3 — furniture registry

```text
gamedata/
  model/FurnitureDefinition.java
  model/FurnitureRegistry.java
  parse/FurnitureParser.java
```

**Tests:** `FurnitureParserTest`, extend `GameDataLoaderTest` for furniture count.

### G4 — validation

```text
gamedata/
  validate/GameDataValidator.java
  validate/ValidationReport.java
```

**Tests:** `GameDataValidatorTest` with synthetic bad `looks_like` + optional tileset mock.

### G5 — mods (v2)

```text
gamedata/
  ModDiscovery.java
  model/ModInfo.java, ModRegistry.java
  mod/ModOrderResolver.java
```

**Tests:** `ModDiscoveryTest`, `GameDataLoaderModTest` with two fixture mod folders.

---

## End-to-end slices (game data + map editor)

Full path from specs to paintable grid. Tileset loader **PR 1–3** is already done.

| Step | PR | Focus | Status |
| --- | --- | --- | --- |
| 1 | **G1** | Data paths + JSON scan | done |
| 2 | **G2** | Terrain registry | done |
| 3 | **G3** | Furniture registry | done |
| 4 | **M1** | `MapGrid` + `MapFileIO` | done |
| 5 | **M2** | `MapEditorScreen` render | done |
| 6 | **M3** | `MapPalettePanel` + paint | done |
| 7 | **M4** | Editor polish | done |
| 8 | **G4** | Validation | done |
| 9 | **G5** | Mod load order | done |

Map editor PR details: [MAP_EDITOR.md § Suggested PR slices](./MAP_EDITOR.md#suggested-pr-slices-map-editor).

---

## Agent entry point

1. Read [AGENTS.md](../AGENTS.md).
2. Read [game-data-loader/implementation-plan.md](./game-data-loader/implementation-plan.md).
3. Implement the current slice; run `gradlew.bat compileJava` and `gradlew.bat test`.

---

## Related

- [TILESET_LOADER.md](./TILESET_LOADER.md) — gfx / sprite loading (done)
- [MAP_EDITOR.md](./MAP_EDITOR.md) — paintable grid (M1–M4 done)
- [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) — tile catalog UI
- BN reference: `src/init.cpp`, `src/mapdata.cpp`, `src/mod_manager.cpp`, `src/path_info.cpp`
