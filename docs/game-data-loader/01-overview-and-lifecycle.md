# 01 — Overview and lifecycle

When and how Cataclysm-BN loads game JSON data. Orchestration only — JSON schemas and
parsing are in later units ([README](./README.md)).

---

## Purpose

BN separates **gfx** (tilesets) from **game data** (JSON definitions):

| System | When | What |
| --- | --- | --- |
| Tileset loader | SDL init / option change / reload | PNG atlases, `tile_config.json` |
| Game data loader | World load / core preload | Terrain, items, monsters, mapgen defs, … |

This spec covers the **game data loader** path. Gfx loading is documented in
[`docs/tileset-loader/`](../tileset-loader/README.md).

The loader answers: “given active mods, populate in-memory registries from all JSON under
each mod’s content path.” It does **not** execute mapgen, run Lua gameplay, or load saves.

---

## Core singleton: `DynamicDataLoader`

BN exposes one global instance (`DynamicDataLoader::get_instance()`). Responsibilities:

| Phase | Method | Effect |
| --- | --- | --- |
| Register handlers | `initialize()` (first use) | Fills `type` → callback map (~100 types) |
| Per-mod scan | `load_data_from_path(path, src, ui)` | Recursive `*.json` → dispatch |
| Deferred resolve | `load_deferred` (during finalize) | Objects that waited on `copy-from` |
| Finalize | `finalize_loaded_data(ui)` | Precalc tables, `set_ter_ids`, item finalize, … |
| Verify | `check_consistency(ui)` | Cross-reference validation |
| Tear down | `unload_data()` | Reset all factories before reload |

`finalized` flag: no additional `load_data_from_path` after finalize until `unload_data`.

Nextgen v1 implements the **scan + dispatch + registry** slice for `terrain` and
`furniture` only; skips Lua, deferred inheritance, and full finalize.

---

## Mod content paths

Each mod’s `modinfo.json` (`type: MOD_INFO`) includes `"path"` relative to the mod
directory containing the manifest.

Core mod `bn`:

```text
data/mods/bn/modinfo.json  →  "path": "../../json"
Resolved content root:     data/json/
```

Loading iterates mods in **order**; `generic_factory::insert` **replaces** an existing id
when the same id is loaded again from a later mod.

---

## Public entry points

### `init::load_core_bn_modfiles()`

```text
clear_loaded_data()                    // unload_data
load_and_finalize_packs(ui, msg, { core_mod_only })
```

Used for title-screen mod check and lightweight core-data validation. Single mod: default
core pack (`bn`).

### `init::load_world_modfiles(ui, world, artifacts_file)`

```text
clear_loaded_data()
normalize_mod_load_order(world->active_mod_order)
load_artifacts(world, artifacts_file)    // separate system
load_and_finalize_packs(ui, msg, mods)
```

Used when starting or loading a game. Mod list comes from world config (user mod selection
+ dependency resolution already applied).

### `init::is_data_loaded()`

Returns `DynamicDataLoader::is_data_finalized()`.

---

## `load_and_finalize_packs` algorithm

```text
ui.new_context(msg)
filter mods → available (valid mod_id) vs missing (debugmsg)

loader.lua = new cata::lua_state
init_global_state_tables(lua, available)

for mod in available with lua_api_version:
    warn if API version mismatch
    set_mod_being_loaded(lua, mod)
    run_mod_preload_script(lua, mod)     // Lua — skip in nextgen v1

reg_lua_icallback_actors(lua, item_controller)
ui.show()

for mod in available:
    loader.load_data_from_path(mod->path, mod.str(), ui)
    ui.proceed()

loader.finalize_loaded_data(ui)
resolve_lua_bionic_and_mutation_callbacks()

for mod in available with lua_api_version:
    set_mod_being_loaded(lua, mod)
    run_mod_finalize_script(lua, mod)    // Lua — skip in nextgen v1

loader.check_consistency(ui)
init::load_main_lua_scripts(lua, packs)
clear_mod_being_loaded(lua)
refresh_mapgen_postprocess_hook_presence(lua)
```

### Per-mod file scan (`load_data_from_path`)

```text
files = recursive "*.json" under path
if files empty and path is a file → treat path as single file

for file in files:
    read entire file to string stream
    JsonIn jsin(stream, file)
    load_all_from_json(jsin, src, ui, base_path, file)
    pump UI events
```

---

## `load_all_from_json` envelope

| File root | Behavior |
| --- | --- |
| JSON object | Dispatch once; trailing content → error |
| JSON array | Dispatch each element until array end |
| Other | Error: expected object or array |

Each object **must** contain `"type": "<handler_key>"`.

---

## Finalize — terrain / furniture hooks

Among many finalize steps, these touch map data:

| Step | Function | Role |
| --- | --- | --- |
| Terrain | `set_ter_ids()` | Bind global `t_dirt`, `t_wall`, … C++ variables to factory indices |
| Furniture | `finalize_furn()` | Furniture factory finalize pass |

`generic_factory::finalize()` also runs `load_deferred` for `copy-from` resolution.

Nextgen v1: no `ter_id` globals; string-keyed maps only.

---

## `check_consistency` — map slice

```text
check_furniture_and_terrain():
  terrain_data.check()    // each ter_t::check
  furniture_data.check()  // each furn_t::check
```

Per-entry checks include valid `open`/`close`/`bash.ter_set` ids, move_cost ranges, etc.
See [10](./10-post-load-validation.md).

---

## Unload / reload

`unload_data()` resets dozens of subsystems (items, terrain, furniture, mapgen, …). Pattern
for any new type:

1. `reset()` on factory
2. Re-register in `initialize()` if needed

Full reload = unload → load path loop → finalize → check.

Nextgen: `LoadedGameData` replaced on reload; no static globals.

---

## Relationship to tilesets

| Concern | Game data | Gfx |
| --- | --- | --- |
| When loaded | World / core init | SDL init, option change |
| Terrain id | `t_dirt` in JSON | Same string in `tile_config.json` |
| Missing gfx | Still valid game data | `do_tile_loading_report` warns |
| `mod_tileset` | Registered during **game** load | Sprites loaded during **tileset** load |

Independent pipelines; map editor joins them at draw time.

---

## Lifecycle timeline

```text
Application start
  → PATH_INFO::set_standard_filenames (datadir, gfxdir, user dirs)
  → mod_manager scans data/mods/**/modinfo.json
  → (optional) load_core_bn_modfiles

User creates/loads world
  → active_mod_order resolved (dependency_tree)
  → load_world_modfiles
  → play uses ter_id / furn_id lookups

Parallel: TILES option → cata_tiles::load_tileset (see tileset unit 01)
```

---

## Nextgen v1 mapping

| BN step | Nextgen v1 |
| --- | --- |
| `load_core_bn_modfiles` | `GameDataLoader.loadCore()` |
| `load_world_modfiles` | `GameDataLoader.loadMods()` (v2) |
| `load_data_from_path` | `JsonDataScanner` + handlers |
| `finalize_loaded_data` | Optional no-op or light precalc |
| `check_consistency` | [10](./10-post-load-validation.md) subset |
| Lua preload/finalize | Skipped |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Pack load loop | `src/init.cpp` — `load_and_finalize_packs` |
| Core-only | `src/init.cpp` — `init::load_core_bn_modfiles` |
| World load | `src/init.cpp` — `init::load_world_modfiles` |
| Mod order normalize | `src/init.cpp` — `normalize_mod_load_order` |
| File scan | `src/init.cpp` — `load_data_from_path`, `load_all_from_json` |
| Dispatch | `src/init.cpp` — `load_object`, `DynamicDataLoader::initialize` |
| Finalize list | `src/init.cpp` — `finalize_loaded_data` |
| Consistency | `src/init.cpp` — `check_consistency` |
| Unload | `src/init.cpp` — `unload_data` |
| Loader class doc | `src/init.h` — `DynamicDataLoader` |

---

## Inputs

- Resolved mod list with content paths (or core-only for v1)
- `data/` root on disk
- Filesystem recursive JSON enumeration

## Outputs

- Populated `terrain_data` / `furniture_data` factories (BN)
- `LoadedGameData` registries (nextgen)
- `finalized == true` after finalize (BN)
- Optional consistency warnings

## Failure modes

| Condition | BN behavior | Nextgen v1 suggestion |
| --- | --- | --- |
| Unrecognized `"type"` | JSON error, load aborts | Skip object + debug log |
| Invalid JSON syntax | Exception with file path | Same |
| Missing mod dependency | Blocked before load (world creation) | v2: dependency_tree |
| `copy-from` abstract missing | Deferred until finalize; may fail | v1: skip or error on unresolved |
| Load after finalize | Assert / debugmsg | Reject |

## Verification

1. `load_core_bn_modfiles` loads only `bn` mod; content path resolves to `data/json/`
2. After load, `ter_t::count() > 0` and `t_dirt` is valid (`t_dirt.is_valid()`)
3. `load_world_modfiles` with extra mod appends/overrides ids from mod path
4. Second load after `unload_data` does not duplicate entries
5. Game data load does not open `gfx/` or `tile_config.json`
6. Document nextgen entry points mapping to core vs full mod load
