# Implementation plan — game data loader

Agent-oriented guide for implementing **Cataclysm-BN game JSON loading** in this repository.
Behavioral specs live in unit docs indexed by [README](./README.md); this file is the
**project plan and module map**.

---

## Target repository

| Item | Path |
| --- | --- |
| Java package | `core/src/main/java/io/gdx/cdda/bn/nextgen/gamedata/` |
| Agent guide | [`AGENTS.md`](../../AGENTS.md) |
| Implementation checklist | [`docs/GAME_DATA_LOADER.md`](../GAME_DATA_LOADER.md) |
| Gfx (separate) | [`docs/TILESET_LOADER.md`](../TILESET_LOADER.md) |
| Unit specs | This directory |

Java package root: `io.gdx.cdda.bn.nextgen.gamedata`

## Reference implementation (LibGDX patterns)

| Repo | Path |
| --- | --- |
| **cygnus-engine** | `../../../../Documents/cygnus-engine` |

Key files: `ModPaths.java`, `ModJson.java`, `*DataIO.java`.

BN C++ sources cited in unit docs live in sibling [Cataclysm-BN](../../../Cataclysm-BN).

---

## Goal

Build a library that:

1. Resolves BN `data/json/` (and mod paths)
2. Scans JSON files and parses `type: terrain` and `type: furniture` entries
3. Produces **`LoadedGameData`** registries your map editor and future game code query

You are **not** reimplementing BN’s full `DynamicDataLoader` (~100 types), Lua mod scripts,
or mapgen. You **are** reimplementing the slice needed to list terrain/furniture ids with
metadata and paint them on a grid using gfx from [`LoadedTileset`](../tileset-loader/08-in-memory-model.md).

---

## On-disk inputs

```text
data/
  mods/bn/modinfo.json          # core → "../../json"
  json/
    furniture_and_terrain/
      terrain-*.json            # arrays of { "type": "terrain", "id": "t_dirt", ... }
      furniture-*.json          # arrays of { "type": "furniture", "id": "f_chair", ... }
```

| Concept | Spec unit |
| --- | --- |
| Data root resolution | [02](./02-path-resolution.md) |
| Mod manifests | [03](./03-mod-discovery.md) |
| File scan + `type` dispatch | [04](./04-json-dispatch.md), [06](./06-load-pipeline.md) |
| Terrain JSON | [05a](./05a-terrain-config.md), [07a](./07a-parse-terrain.md) |
| Furniture JSON | [05b](./05b-furniture-config.md), [07b](./07b-parse-furniture.md) |

### Configurable roots

| Root | BN equivalent | Property (planned) |
| --- | --- | --- |
| Game data | `PATH_INFO::datadir()` | `cdda.data.roots` |
| Mod manifests | `PATH_INFO::moddir()` | `cdda.mod.roots` |

Mirror [`GfxPaths`](../../core/src/main/java/io/gdx/cdda/bn/nextgen/tileset/GfxPaths.java):
semicolon-separated paths, common relative fallbacks (`../Cataclysm-BN/data`).

---

## Deliverable: in-memory model

After `GameDataLoader.loadCore()` succeeds ([08](./08-in-memory-model.md)):

```text
LoadedGameData {
  terrain: TerrainRegistry    // id → TerrainDefinition
  furniture: FurnitureRegistry // id → FurnitureDefinition
  source_mods: string[]       // load order
}

TerrainDefinition {
  id: string                   // "t_dirt"
  name: string
  symbol: string               // ASCII fallback / editor label
  color: string                // BN color name; resolve later
  move_cost: int               // optional default
  flags: string[]              // v1: store raw; interpret later
  looks_like: string?          // optional ter id
}

FurnitureDefinition {
  id: string                   // "f_chair"
  name, symbol, color, flags, move_cost, looks_like  // same idea
}
```

### Required lookup API (v1)

```text
GameDataLoader.loadCore(options) → LoadedGameData
GameDataLoader.loadMods(mod_ids, options) → LoadedGameData   // v2

terrain.find("t_dirt") → TerrainDefinition?
furniture.find("f_chair") → FurnitureDefinition?
terrain.allIds() → sorted string[]
```

Cross-check with gfx (optional QA):

```text
terrain.ids().filter(id => tileset.findTile(id).isEmpty()) → missing sprites
```

---

## Implementation modules

| Module | Responsibility | Spec units |
| --- | --- | --- |
| `DataPaths` | Resolve `data/`, `data/json` | [02](./02-path-resolution.md) |
| `ModDiscovery` | Scan `modinfo.json` | [03](./03-mod-discovery.md) |
| `JsonDataScanner` | Recursive `.json` scan, envelope parse | [04](./04-json-dispatch.md) |
| `GameDataLoader` | Orchestrate load | [06](./06-load-pipeline.md) |
| `TerrainParser` | `type: terrain` → model | [05a](./05a-terrain-config.md), [07a](./07a-parse-terrain.md) |
| `FurnitureParser` | `type: furniture` → model | [05b](./05b-furniture-config.md), [07b](./07b-parse-furniture.md) |
| `model` | Registries + `LoadedGameData` | [08](./08-in-memory-model.md) |
| `validate` | Post-load checks | [10](./10-post-load-validation.md) |
| `mod` | Load order, overrides | [09](./09-mod-load-order.md) |

---

## Version tiers

### v1 — Map editor slice (recommended first milestone)

| Include | Skip |
| --- | --- |
| `DataPaths` + core `data/json` only | Full mod dependency tree |
| Terrain + furniture parse (minimal fields) | Items, monsters, recipes, mapgen |
| `TerrainRegistry` / `FurnitureRegistry` | `ter_id` enums, `generic_factory` |
| Blocking `GameDataLoader.loadCore()` | Lua preload/finalize |
| Unit tests on real BN JSON files | `check_consistency` full BN parity |

### v2 — Mod-aware load

- [03](./03-mod-discovery.md), [09](./09-mod-load-order.md)
- `loadMods(world_mod_list)` with override semantics
- Optional tileset cross-check ([10](./10-post-load-validation.md))

### v3 — Simulation data (future)

- Additional `type` handlers as game features need them
- Deferred JSON, finalize passes — follow BN `init.cpp` pattern

---

## Explicitly out of scope

- BN save format (`.sav2`)
- Mapgen / overmap generation execution
- Item factory, crafting, monsters
- Lua mod scripts (`catalua`)
- Multitile neighbor resolution at load time

---

## Acceptance tests

Run against `../Cataclysm-BN/data/json` when available.

| # | Check | Spec reference |
| --- | --- | --- |
| 1 | `DataPaths` resolves existing `data/json` | 02 |
| 2 | `loadCore()` completes without error | 06 |
| 3 | `findTerrain("t_dirt")` present with name `"dirt"` | 07a |
| 4 | `findFurniture("f_null")` or null furniture exists | 07b |
| 5 | Terrain count > 500 (sanity on full core data) | 08 |
| 6 | Unknown `type` in file does not abort load (v1: skip) | 04 |
| 7 | Duplicate id from second mod overrides first (v2) | 09 |

---

## Suggested first PR slices

| PR | Units | Done when |
| --- | --- | --- |
| 1 | 02, 04, 06 (scan only) | Lists JSON files under `furniture_and_terrain/` |
| 2 | 05a, 07a, 08 | `TerrainRegistry` populated from core data |
| 3 | 05b, 07b | `FurnitureRegistry` added |
| 4 | 10 | Validation + tileset id cross-check helper |
| 5 | 03, 09 | Mod list load |

---

## Agent workflow

1. Read [README](./README.md) scope and this plan.
2. Implement in unit dependency order (01 → 02 → … → 08).
3. For each unit: implement → run **Verification** section → proceed.
4. Point `cdda.data.roots` at a BN install.
5. Cross-check ambiguous behavior against BN sources in each unit.

---

## Related documents

| Document | Role |
| --- | --- |
| [README](./README.md) | Unit index, scope, progress |
| [01](./01-overview-and-lifecycle.md) | BN load lifecycle |
| [08](./08-in-memory-model.md) | Java types and lookup API |
| [../map-editor/README.md](../map-editor/README.md) | Paintable grid + map file format |
| [../TILESET_LOADER.md](../TILESET_LOADER.md) | Gfx loader (already implemented) |

BN author docs: `docs/en/mod/json/reference/json_info.md`.
