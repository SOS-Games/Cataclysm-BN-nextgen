# 02 — Path resolution

How BN locates the `data/` tree and how nextgen resolves equivalent paths before any JSON is
opened. See [01](./01-overview-and-lifecycle.md) for when paths are used; mod manifests in
[03](./03-mod-discovery.md).

---

## Purpose

Produce stable paths for:

1. **Data root** — `data/` (parent of `json/`, `mods/`, `raw/`)
2. **Core JSON root** — `data/json/` (via core mod `path`)
3. **Mod manifest root** — `data/mods/`
4. **User config/save dirs** — not game JSON, but referenced during full BN startup

Gfx uses a **separate** root (`gfx/`); see [tileset 02](../tileset-loader/02-discovery-and-registry.md).

---

## BN path initialization

### `PATH_INFO::init_base_path(path)`

Sets `base_path_value` (install / working directory prefix, normalized with trailing `/`).

### `PATH_INFO::set_standard_filenames()`

Derives standard paths from `base_path`:

| Variable | Typical value (dev tree) | Typical value (Linux share install) |
| --- | --- | --- |
| `datadir_value` | `{base}/data/` | `{base}/share/cataclysm-bn/` |
| `gfxdir_value` | `{base}/gfx/` | `{datadir}/gfx/` |
| `savedir_value` | `{user}/save/` | same |
| `config_dir_value` | `{user}/config/` or XDG | platform-specific |

If `base_path` empty:

```text
datadir  = "data/"
gfxdir   = "gfx/"
```

### Key accessors

| API | Returns |
| --- | --- |
| `PATH_INFO::datadir()` | Game data root |
| `PATH_INFO::gfxdir()` | Gfx root (tilesets) |
| `PATH_INFO::moddir()` | `datadir + "mods/"` |
| `PATH_INFO::config_dir()` | User options, keybindings |
| `PATH_INFO::savedir()` | Save games |

### User directory

`PATH_INFO::init_user_dir` sets per-platform user folder (`~/.cataclysm-bn/`, XDG, Android
Documents, etc.). Used for saves and config — **not** for core `data/json` in a default install.

---

## Core JSON location (not a direct PATH_INFO helper)

BN does **not** hardcode `datadir/json`. Core content is reached through the core mod:

```text
datadir/mods/bn/modinfo.json
  "path": "../../json"

mod directory:  datadir/mods/bn/
resolved path:  datadir/mods/bn/../../json  →  datadir/json/
```

`load_data_from_path` receives the **resolved mod content path**, not `datadir` itself.

---

## Install layout examples

### Developer / portable (Windows/Linux)

```text
Cataclysm-BN/
  data/
    json/furniture_and_terrain/...
    mods/bn/modinfo.json
    raw/colors.json
  gfx/UltimateCataclysm/...
  cataclysm-bn-tiles.exe
```

`base_path` = repo root → `datadir` = `data/`.

### Nextgen sibling checkout

```text
Cataclysm-BN-nextgen/
  ../Cataclysm-BN/data/     ← point DataPaths here
  ../Cataclysm-BN/gfx/      ← GfxPaths (existing)
```

---

## Nextgen `DataPaths` (planned)

Mirror [`GfxPaths`](../../core/src/main/java/io/gdx/cdda/bn/nextgen/tileset/GfxPaths.java).

### System property

```text
cdda.data.roots  — semicolon-separated list of data roots
```

Each entry should be a directory containing `json/` and `mods/` (or at minimum `json/` for
v1 core-only load).

### Default relative candidates (in order)

```text
data
../Cataclysm-BN/data
../../Cataclysm-BN/data
```

### Planned API

```java
DataPaths.gameDataRoots()       // List<Path> existing dirs
DataPaths.primaryDataRoot()     // first root or empty
DataPaths.coreJsonRoot()        // primary + /json
DataPaths.modRoot()             // primary + /mods
DataPaths.resolveCoreModJson()  // optional: read bn modinfo path
```

### Validation

| Check | v1 policy |
| --- | --- |
| Root exists | Required |
| `json/furniture_and_terrain/` exists | Required for terrain load |
| `mods/bn/modinfo.json` exists | Warn if missing; v1 may hardcode `json/` |

### Gradle / launcher

Extend `lwjgl3:run` to set `-Dcdda.data.roots=../Cataclysm-BN/data` when sibling exists
(parallel to gfx auto-detect).

---

## Relationship to mod `path` field

| Step | Path used |
| --- | --- |
| Discover mod | `data/mods/MyMod/modinfo.json` |
| Resolve content | `dirname(modinfo) + "/" + mod.path` |
| Scan JSON | Recursive under content path only |

A mod with `"path": "."` loads JSON next to `modinfo.json`. Core mod uses `"../../json"` to
share the global json tree.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Path setup | `src/path_info.cpp` — `set_standard_filenames`, `init_base_path` |
| datadir / moddir | `src/path_info.cpp` — `datadir()`, `moddir()` |
| Mod path join | `src/mod_manager.cpp` — `load_modfile` (`path` field) |
| Core mod example | `data/mods/bn/modinfo.json` |

---

## Inputs

- Optional `cdda.data.roots` property
- Process working directory
- Optional explicit base path (BN launcher)

## Outputs

- Ordered list of valid `data/` roots
- Resolved `Path` to `json/`, `mods/`

## Failure modes

| Condition | Behavior |
| --- | --- |
| No root found | Empty list; caller shows error (like sprite viewer gfx message) |
| Root without `json/` | Fail fast with hint to set `cdda.data.roots` |
| Multiple roots | First existing wins for primary; merge policy v2 |

## Verification

1. With `../Cataclysm-BN/data` present, `coreJsonRoot()` ends with `furniture_and_terrain`
2. Property override replaces defaults
3. Unit test uses minimal fixture tree (no full BN install)
4. `coreJsonRoot()` + `bn` mod path equals same directory as hardcoded `data/json`
5. DataPaths independent of GfxPaths (can point at different installs)
