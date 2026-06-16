# 03 — Mod discovery

How BN finds mod manifests and builds a mod registry before JSON content load. Content
loading from mod paths is [06](./06-load-pipeline.md); load ordering is [09](./09-mod-load-order.md).

---

## Purpose

Scan `data/mods/**/modinfo.json`, parse `type: MOD_INFO` entries, produce:

```text
ModRegistry: mod_id → ModInfo {
  id, name, description, path, path_full,
  core, dependencies, conflicts, category, obsolete, ...
}
```

Discovery answers: “which mods exist, where is their manifest, where is their JSON content?”
It does **not** load terrain/items or resolve the dependency tree for a world.

---

## Search roots

```text
PATH_INFO::moddir()  →  datadir + "mods/"
```

`mod_manager` loads from this tree (and may merge replacement mods from user config in BN —
see `load_replacement_mods`; optional for nextgen v2).

---

## Scan algorithm

### `load_mods_from(path)`

```text
for info_file in get_files_from_path("modinfo.json", path, recursive=true):
    load_mod_info(info_file, out)

dedupe by mod id:
  if duplicate id in same scan:
    debugmsg listing all paths
    remove ALL conflicting entries (BN rejects entire id group)

return vector<MOD_INFORMATION>
```

### `load_mod_info(info_file_path, out)`

Parent directory = `main_path` (directory containing `modinfo.json`).

| File shape | Behavior |
| --- | --- |
| Single JSON object | `load_modfile(jo, main_path)` once |
| JSON array | `load_modfile` for each object |

Non-`MOD_INFO` objects in array are ignored (`load_modfile` returns nullopt).

---

## `load_modfile` — `MOD_INFO` fields

### Required / identity

| Field | JSON key | Notes |
| --- | --- | --- |
| Type | `type` | Must be `"MOD_INFO"` |
| Id | `id` or legacy `ident` | `mod_id` string |

### Content path

```text
if jo has "path":
    mod.path = main_path + "/" + jo.path    // string concat, normalized
else:
    mod.path = main_path                    // JSON next to modinfo
```

Examples:

| modinfo location | `path` in JSON | Resolved content |
| --- | --- | --- |
| `data/mods/bn/modinfo.json` | `"../../json"` | `data/json/` |
| `data/mods/MyMod/modinfo.json` | `"."` | `data/mods/MyMod/` |
| `data/mods/Foo/modinfo.json` | absent | `data/mods/Foo/` |

### Metadata (stored, not used in v1 terrain load)

| Field | Purpose |
| --- | --- |
| `name`, `description` | UI |
| `category` | Mod list grouping |
| `core` | Core pack flag → forced first in load order |
| `dependencies` | Required mod ids |
| `conflicts` | Incompatible mod ids |
| `obsolete` | Hide from picker |
| `lua_api_version` | Lua script compatibility |
| `loading_images` | Loading screen art |
| `version`, `license`, `authors`, `maintainers` | Metadata |

### Validation rules

- Mod cannot list itself in `dependencies` → JSON error
- Mod cannot list itself in `conflicts` → JSON error
- Same id in both `dependencies` and `conflicts` → JSON error

---

## Duplicate id policy

If two `modinfo.json` files claim the same `id`:

```text
debugmsg: lists all paths
NONE of the duplicate-id mods are loaded
```

Strict policy avoids silent merge corruption. Fix mod packs or rename ids.

---

## Core mod `bn`

```json
{
  "type": "MOD_INFO",
  "id": "bn",
  "name": "Bright Nights",
  "category": "core",
  "core": true,
  "path": "../../json"
}
```

`mod_management::get_default_core_content_pack()` returns this id when no core in list.

---

## Dependency tree (discovery vs resolution)

| Stage | Component | Role |
| --- | --- | --- |
| Discovery | `mod_manager` | All mods on disk |
| World setup | `dependency_tree` | Valid mod sets, order, errors |
| Load | `normalize_mod_load_order` | Core first; see [09](./09-mod-load-order.md) |

v1 nextgen may skip dependency tree and load core `json/` only.

---

## Nextgen `ModDiscovery` (planned)

```java
ModDiscovery.build(DataPaths) → ModRegistry
ModInfo resolvedContentPath()  // absolute Path to JSON root
```

v1 shortcut: `CoreModPaths.jsonRoot()` without full scan.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Scan | `src/mod_manager.cpp` — `load_mods_from` |
| Parse manifest | `src/mod_manager.cpp` — `load_modfile`, `load_mod_info` |
| Mod id type | `src/mod_manager.h` — `MOD_INFORMATION` |
| Default core | `src/worldfactory.cpp` / mod_management |
| Example | `data/mods/bn/modinfo.json` |

---

## Inputs

- `data/mods/` path from [02](./02-path-resolution.md)

## Outputs

- `ModRegistry`: id → metadata + resolved content `Path`
- `path_full`: absolute path to `modinfo.json` (debug)

## Failure modes

| Condition | Behavior |
| --- | --- |
| Invalid `modinfo.json` | Parse error or skip entry |
| Duplicate mod ids | All conflicting mods rejected |
| Missing `path` target | Load-time file-not-found later |
| `MOD_INFO` missing `id` | Parse failure |

## Verification

1. `bn` mod discovered; `resolvedContentPath` equals `data/json/`
2. Nested `data/mods/Pack/modinfo.json` found recursively
3. Duplicate-id fixture removes both mods and logs warning
4. Mod with `"path": "."` resolves JSON beside modinfo
5. Discovery does not open terrain JSON files
