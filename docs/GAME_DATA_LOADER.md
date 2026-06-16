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
| `DataPaths` — resolve `data/json` | todo |
| Scan `furniture_and_terrain/*.json` | todo |
| Parse `type: terrain` entries | todo |
| Parse `type: furniture` entries | todo |
| `TerrainRegistry` / `FurnitureRegistry` lookup by id | todo |
| Core mod only (skip mod dependency tree) | todo |
| Mod load order + overrides | todo |
| Full BN `DynamicDataLoader` parity (~100 types) | out of scope |
| Items, monsters, recipes, mapgen execution | out of scope |

**Acceptance:** `GameDataLoader.loadCore()` returns registries where `findTerrain("t_dirt")`
has id and name from BN JSON; ids match `LoadedTileset.findTile("t_dirt")` when gfx is loaded.

---

## Agent entry point

1. Read [AGENTS.md](../AGENTS.md).
2. Read [game-data-loader/implementation-plan.md](./game-data-loader/implementation-plan.md).
3. Implement the current slice; run `gradlew.bat compileJava` and `gradlew.bat test`.

---

## Related

- [TILESET_LOADER.md](./TILESET_LOADER.md) — gfx / sprite loading (done)
- [MAP_EDITOR.md](./MAP_EDITOR.md) — paintable grid (planned)
- [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) — tile catalog UI
- BN reference: `src/init.cpp`, `src/mapdata.cpp`, `src/mod_manager.cpp`, `src/path_info.cpp`
