# 04 — JSON dispatch framework

How BN scans JSON files and routes each object to a type-specific handler. v1 nextgen
implements this for **`terrain`** and **`furniture`**; other types are skipped.

Orchestration: [06](./06-load-pipeline.md). Per-type schemas: [05a](./05a-terrain-config.md),
[05b](./05b-furniture-config.md).

---

## Purpose

Given a content directory, enumerate JSON files and for each object:

1. Parse file envelope (object or array)
2. Read `"type"` string
3. Invoke registered handler or apply skip policy
4. Pass `src` mod id for provenance and strict-mode checks

---

## File discovery

`DynamicDataLoader::load_data_from_path(path, src, ui)`:

```text
files = get_files_from_path(".json", path, recursive=true, sorted=true)

if files.empty():
    if path is readable file:
        files = [path]          // single-file load
    else:
        return                  // nothing to do
```

Properties (BN filesystem helper):

- Recursive walk under mod content root
- Typically sorted lexically by path
- `~` backup files excluded (filesystem layer)

**Implication:** A single mod load touches **all** JSON under its path — thousands of files
for core `data/json/`. v1 nextgen may restrict to `furniture_and_terrain/` subdirectory.

---

## Per-file read

```text
read_entire_file(file) → string
JsonIn jsin(istringstream, file_path)
load_all_from_json(jsin, src, ui, base_path, full_path)
inp_mngr.pump_events()    // UI responsiveness
```

Parse errors throw `JsonError` → wrapped in `std::runtime_error` with file path.

---

## Envelope: `load_all_from_json`

### Single object (legacy)

```text
jo = jsin.get_object()
load_object(jo, src, base_path, full_path)
jo.finish()
if jsin has trailing content → error
```

BN comment: single-object support temporary until 0.G.

### Array (common)

```text
jsin.start_array()
while not jsin.end_array():
    jo = jsin.get_object()
    load_object(jo, src, base_path, full_path)
    jo.finish()
```

Most terrain files use array form:

```json
[
  { "type": "terrain", "id": "t_dirt", ... },
  { "type": "terrain", "id": "t_sand", ... }
]
```

Mixed-type arrays are normal — one file may contain only terrain; another only items.

---

## Dispatch: `load_object`

```text
type = jo.get_string("type")
it = type_function_map.find(type)
if it == end:
    jo.throw_error("unrecognized JSON object", "type")
it->second(jo, src, base_path, full_path)
```

Handler signatures vary; terrain/furniture use:

```cpp
void load_terrain(const JsonObject &jo, const std::string &src);
void load_furniture(const JsonObject &jo, const std::string &src);
```

(Registered via lambda wrapping in `DynamicDataLoader::initialize`.)

### Registered types (BN) — partial list

| `type` | Handler area |
| --- | --- |
| `terrain` | `load_terrain` → `terrain_data` |
| `furniture` | `load_furniture` → `furniture_data` |
| `GUN`, `ARMOR`, `TOOL`, … | `item_factory` |
| `mapgen` | `load_mapgen` |
| `overmap_terrain` | `overmap_terrains` |
| `MOD_INFO` | ignored at scan (loaded via mod_manager) |
| `mod_tileset` | gfx registration (tiles build) |

Full table: `DynamicDataLoader::initialize()` in `src/init.cpp` (~100 entries).

---

## v1 nextgen handler registry

```text
HANDLERS = {
  "terrain"   → TerrainParser.accept
  "furniture" → FurnitureParser.accept
}

for object in scan:
    handler = HANDLERS.get(object.type)
    if handler == null:
        skip (optional trace at FINE level)
    else:
        handler.parse(object, src)
```

**Do not** throw on unknown types in shared files (e.g. `items/` JSON referenced from same
tree if scanning whole `json/`).

---

## `generic_factory` load (terrain/furniture)

Handlers delegate to `terrain_data.load(jo, src)` / `furniture_data.load(jo, src)`:

```text
handle_inheritance(def, jo, src)   // copy-from / abstract — see below
resolve id from "id" string | "id" array | legacy keys
def.load(jo, src)
insert(def)                        // replace if id exists
```

### `id` forms

| JSON | Effect |
| --- | --- |
| `"id": "t_dirt"` | One definition |
| `"id": ["t_dirt", "t_dirtmound"]` | Same JSON → multiple ids |
| `"abstract": "base"` | Template only; no registry insert |
| `"copy-from": "base"` | Merge from abstract; may defer |

v1 nextgen: support string `id` only; **defer** `copy-from` / `abstract` / id arrays or
implement minimal `copy-from` if common in terrain files.

---

## Deferred loading (`copy-from`)

If `copy-from` references an abstract not yet loaded:

```text
deferred_json.push_back({location, src})
```

Resolved during `generic_factory::finalize()` → `load_deferred`.

v1: log warning and skip entry if parent missing, OR shallow-merge if parent already loaded.

---

## Strict mode

`is_json_check_strict(src)` — mod may enable extra JSON validation. Unknown keys may error in
strict mods. v1 nextgen: ignore unknown keys (forward-compatible).

---

## Stream cache

During `finalize_loaded_data`, BN caches file contents for deferred reload. Not needed for
v1 nextgen.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Directory scan | `src/init.cpp` — `load_data_from_path` |
| Envelope | `src/init.cpp` — `load_all_from_json` |
| Dispatch | `src/init.cpp` — `load_object` |
| Handler table | `src/init.cpp` — `DynamicDataLoader::initialize` |
| Factory load | `src/generic_factory.h` — `generic_factory::load` |
| Inheritance | `src/generic_factory.h` — `handle_inheritance` |
| File enum | `src/filesystem.cpp` — `get_files_from_path` |

---

## Inputs

- Content root path (mod `path` resolved)
- Mod id string (`src`)
- Handler registry

## Outputs

- Side effects on registries via handlers
- Stats: `files_read`, `objects_dispatched`, `objects_skipped`, `errors`

## Failure modes

| Condition | BN | Nextgen v1 |
| --- | --- | --- |
| Invalid JSON syntax | Abort with path | Same |
| Missing `type` | Error | Error |
| Unknown `type` | Error | Skip |
| Missing `id` on terrain | Error | Error |
| `copy-from` unresolved | Deferred / finalize error | Warn + skip |

## Verification

1. Fixture array file dispatches N terrain objects
2. Single-object file dispatches once; trailing garbage errors
3. File with `"type": "GUN"` does not abort v1 load
4. Malformed JSON reports full path in exception
5. Recursive scan finds nested `furniture_and_terrain/terrain-x.json`
6. Same `id` twice in one load → second wins (factory insert)
