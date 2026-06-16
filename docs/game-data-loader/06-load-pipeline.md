# 06 — Load pipeline

End-to-end algorithm from data roots to populated registries. Combines [02](./02-path-resolution.md),
[03](./03-mod-discovery.md), [04](./04-json-dispatch.md), [07a](./07a-parse-terrain.md),
[07b](./07b-parse-furniture.md).

---

## Purpose

Define `GameDataLoader` orchestration — BN analogue: `load_and_finalize_packs` +
`load_data_from_path`, reduced for nextgen.

---

## BN full pipeline (reference)

```text
unload_data()
for mod in ordered_mods:
    load_data_from_path(mod.path, mod.id)
finalize_loaded_data()
    → terrain: set_ter_ids, furniture: finalize_furn, + ~40 other systems
check_consistency()
    → check_furniture_and_terain, items, mapgen, ...
```

---

## Nextgen v1: `GameDataLoader.loadCore(options)`

```text
options ← GameDataLoadOptions.defaults()
roots   ← DataPaths.gameDataRoots()
jsonRoot ← options.jsonRootOverride ?? DataPaths.coreJsonRoot()

terrainRegistry  ← new TerrainRegistry()
furnitureRegistry ← new FurnitureRegistry()

scanPaths ← options.scanSubdirs empty?
    [jsonRoot]                           // v2: full tree
    [jsonRoot / "furniture_and_terrain"] // v1 default

for scanRoot in scanPaths:
    for each file in recursive "*.json"(scanRoot):
        for each object in parseEnvelope(file):
            switch object.type:
                case "terrain":
                    TerrainParser.parse(object, "bn", terrainRegistry)
                case "furniture":
                    FurnitureParser.parse(object, "bn", furnitureRegistry)
                default:
                    skip

Validation.runOptional(terrainRegistry, furnitureRegistry, options)

return LoadedGameData(terrainRegistry, furnitureRegistry, ["bn"])
```

### Options struct (planned)

```java
GameDataLoadOptions {
    List<Path> dataRoots;
    List<String> scanSubdirs;   // empty = full json tree; ["furniture_and_terrain"] = v1
    boolean strictUnknownFields;
    boolean failOnParseError;   // false = warn and skip object
    LoadedTileset tilesetForCrossCheck;  // optional, for unit 10
}
```

---

## Nextgen v2: `loadMods(modIds, options)`

```text
mods ← ModOrderResolver.resolve(modIds)     // unit 09
registries ← empty

for mod in mods:
    for scanRoot in resolveScanRoots(mod):
        scan and dispatch with src = mod.id

Validation.run(...)
return LoadedGameData(..., mod load order)
```

Override: same `id` from later mod replaces earlier (`HashMap.put`).

---

## Scan scope tradeoff

| Scope | Files (approx) | Use |
| --- | --- | --- |
| `furniture_and_terrain/` only | ~25 files | v1 palette — fast |
| Full `data/json/` | ~1900+ files | v2 — needs skip-unknown-type |
| Single mod path | varies | Matching BN per-mod load |

BN always scans **entire** mod `path` recursively.

---

## Error handling policies

| Event | Recommended v1 |
| --- | --- |
| JSON syntax error in file | Fail load; include path |
| Single object parse error | Skip object + warn (continue) |
| Missing `id` | Skip + warn |
| Unknown `type` | Skip (silent or FINE log) |
| Duplicate id same mod | Last object in file order wins |
| Duplicate id across mods | Later mod wins (v2) |

---

## Blocking vs incremental

Unlike tilesets (GPU), game JSON load is **CPU-only**. v1 may use blocking load on worker
thread; no OpenGL constraint.

Optional `GameDataLoadSession` for huge full-tree scans (progress bar) — not required for
`furniture_and_terrain/` only.

---

## Relationship to finalize (BN)

BN `finalize_loaded_data` does not change terrain/furniture **definitions** heavily; it:

- Resolves deferred `copy-from`
- Runs `set_ter_ids()` — binds global `t_dirt` C++ variables
- Runs `finalize_furn()`

Nextgen skips globals; optional pass: build sorted `allIds()` cache.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Pack loop | `src/init.cpp` — `load_and_finalize_packs` |
| Per-path scan | `src/init.cpp` — `load_data_from_path` |
| Core entry | `src/init.cpp` — `load_core_bn_modfiles` |
| World entry | `src/init.cpp` — `load_world_modfiles` |

---

## Inputs

- `GameDataLoadOptions`
- Optional ordered mod list (v2)

## Outputs

- `LoadedGameData` ([08](./08-in-memory-model.md))
- Load statistics log

## Failure modes

| Condition | Result |
| --- | --- |
| No json root | Empty registries or error |
| Zero terrain after load | Error if core data expected |
| Partial file failure | Per policy above |

## Verification

1. `loadCore()` on real BN checkout: terrain count > 500, furniture count > 200
2. `findTerrain("t_dirt")` in < 2s with v1 scan scope
3. Re-load produces identical `allIds()` sets
4. Inject bad JSON line → reports filename
5. v2: two-mod override fixture changes `t_test` name
