# 02 — Discovery and registry

How installed tilesets are found on disk and registered in a lookup table before any
`tile_config.json` or image loading occurs. Manifest fields used at load time (`JSON`,
`TILESET`) are defined in [04a](./04a-tileset-manifest.md); this unit covers only
**discovery parsing** of `NAME` and `VIEW`.

See [01](./01-overview-and-lifecycle.md) for when the registry is built in the overall
lifecycle.

---

## Purpose

Produce two artifacts:

1. **`TILESETS` registry** — map from internal tileset id → absolute or root-relative
   **directory path** containing that tileset's `tileset.txt`
2. **Option choices** — list of `(id, display_name)` pairs for `TILES` and
   `OVERMAP_TILES` settings

Discovery answers: “which tileset ids exist, where do they live, and what label do we show
in the UI?” It does **not** load tile definitions or images.

---

## Search roots

Registry build scans two directories **in order**:

| Order | Path | BN source |
| --- | --- | --- |
| 1 | Game graphics root | `PATH_INFO::gfxdir()` — typically `{data}/gfx/` or `{base}/gfx/` |
| 2 | User graphics root | `PATH_INFO::user_gfx()` — `{user_config_dir}/gfx/` |

If the root path does not exist, that scan contributes nothing (empty list).

User entries are merged **after** game-data entries. See [precedence](#name-collisions-and-precedence).

---

## Directory scan algorithm

### Trigger

`build_tilesets_list()` runs when graphics options are registered (BN calls it while defining
the `TILES` and `OVERMAP_TILES` options). Each call **clears** the global `TILESETS` map and
rebuilds from scratch.

### Find candidate directories

For each search root:

1. Recursively walk the directory tree (breadth-first search)
2. Find every file whose name **ends with** `tileset.txt` (exact suffix match on filename)
3. For each match, take the **parent directory** as the tileset directory (strip the filename)

Properties of the walk (BN):

- Files ending in `~` are excluded
- Results are ordered lexically (directories and files sorted by name)
- Returned paths use `/` as separator

### Parse each manifest

For each candidate directory `D`, open `D/tileset.txt` and extract:

| Field | Required for registry | Parsing rule |
| --- | --- | --- |
| `NAME` | Yes (for a valid entry) | See below |
| `VIEW` | No | Display label; stops parsing |

**Discovery parser** (differs from load-time manifest parser in 04a):

```text
resource_name = ""
view_name = ""

for each whitespace-delimited token from file:
    if token empty           → skip line
    if token starts with '#' → skip rest of line (comment)
    if token contains "NAME" → read rest of line → trim → resource_name
    if token contains "VIEW" → read rest of line → trim → view_name → STOP

display_name = view_name if non-empty, else resource_name
```

Important behaviors:

- Parsing **stops after `VIEW`** is read. Lines that follow (`JSON`, `TILESET`, comments) are
  **not read during discovery**. Typical manifests place `VIEW` before those keys, so discovery
  never sees them.
- Token matching uses substring check (`token` contains `"NAME"` or `"VIEW"`), so `NAME:` and
  `NAME` both work.
- If the file contains multiple `NAME` lines before `VIEW`, later `NAME` lines overwrite
  earlier ones. Only the final pair before `VIEW` matters.
- If `read_from_file` fails (missing/unreadable file), that directory is skipped silently.

### Register entry

After parsing:

```text
if resource_name already in registry for this build:
    emit debug warning: duplicate name, ignore new entry
else:
    registry[resource_name] = D
    append (resource_name, display_name) to scan result list
```

---

## Merging game and user scans

`build_tilesets_list()`:

```text
TILESETS.clear()
result = []

result += scan(game_gfx_root)     // also inserts into TILESETS
user_list = scan(user_gfx_root)   // also inserts into TILESETS

for each entry in user_list:
    if entry not in result (full pair equality: id AND display_name):
        append entry to result

if result is empty:
    result = [ ("hoder", "Hoder's"), ("deon", "Deon's") ]   // hardcoded legacy fallback

return result
```

### `TILESETS` insert semantics

Each per-root scan inserts into the global `TILESETS` map. `std::map::insert` does **not**
overwrite an existing key. Because game gfx is scanned first:

- **Same id in game and user dirs** → registry path points to **game** copy
- User copy is ignored for loading (debug warning if user scan hits duplicate name in its
  own pass; cross-root duplicate is silent on the map)

### Option list deduplication

User scan results are appended to the option list only when the full `(id, display_name)`
pair is not already present. The same id with a **different** display name can appear twice
in the option list; the registry still resolves by id to the game-data path.

---

## Registry data structures

### `TILESETS`

```text
map<tileset_id, directory_path>
```

- **Key:** `NAME` from `tileset.txt` (e.g. `retrodays`, `UNDEAD_PEOPLE_BASE`)
- **Value:** Directory containing that tileset's `tileset.txt` (not the path to the file itself)

Global, cleared and rebuilt on each `build_tilesets_list()` call. Used later by the loader
to resolve `tileset_id` from options to a folder (unit 05).

### Option entry `id_and_option`

```text
pair<tileset_id, display_name>
```

- `tileset_id` — stored in save/config as `TILES` / `OVERMAP_TILES` value
- `display_name` — localized UI string from `VIEW`, or `NAME` if `VIEW` omitted

`TILES` and `OVERMAP_TILES` options are populated from the same `build_tilesets_list()`
return value (BN registers the option twice with identical choice lists).

---

## Examples

### Typical `tileset.txt`

```text
NAME: retrodays
VIEW: RetroDays
JSON: tile_config.json
TILESET: tiles.png
```

Discovery reads only through `VIEW`. Registry entry:

```text
TILESETS["retrodays"] = "<gfx_root>/RetroDaysTileset"
option list += ("retrodays", "RetroDays")
```

### Name without VIEW

```text
NAME: hoder
```

Registry:

```text
TILESETS["hoder"] = "<gfx_root>/HoderTileset"
option list += ("hoder", "hoder")
```

### Nested install layout

Any depth under gfx root is valid:

```text
gfx/MyPack/SubFolder/tileset.txt  →  registry id maps to gfx/MyPack/SubFolder/
```

---

## Name collisions and precedence

| Scenario | Registry (`TILESETS`) | Option list |
| --- | --- | --- |
| Duplicate `NAME` in same scan pass | First wins; later emits debug warning | First wins |
| Same `NAME` in game + user dirs | Game directory wins | Both may appear if `VIEW` differs |
| No tilesets found anywhere | Empty map; fallback ids `hoder`/`deon` in options only | Fallback pair only — **no paths** until a real tileset exists |

The fallback `hoder` / `deon` entries exist so the options UI is never empty. They do not
create registry paths unless matching folders are actually discovered.

---

## Relationship to loading

| Stage | Uses registry? | Reads `JSON`/`TILESET`? |
| --- | --- | --- |
| Discovery (this unit) | Writes | No |
| Precheck / full load | Reads path by id | Yes — load-time parser (04a) |

If the selected option id is **not** in `TILESETS` at load time, the loader logs an error and
falls back to default config paths (not the missing directory). See unit 05.

---

## Shared machinery

Tileset discovery reuses the same helper as soundpack discovery (`build_resource_list`):

| Parameter | Tileset value |
| --- | --- |
| `operation_name` | `"tileset"` (for duplicate warning text) |
| `filename` | `tileset.txt` |

Soundpacks use a parallel path with a different manifest filename; behavior is otherwise
identical.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Registry build | `src/options.cpp` — `build_tilesets_list`, `load_tilesets_from` |
| Manifest scan parse | `src/options.cpp` — `build_resource_list` |
| Recursive file find | `src/filesystem.cpp` — `get_directories_with` |
| Path constants | `src/path_info.cpp` — `gfxdir`, `user_gfx` |
| Global registry | `src/options.cpp` — `TILESETS`; declared in `src/options.h` |
| Loader lookup | `src/cata_tiles.cpp` — `tileset_loader::load` |

---

## Inputs

- Game graphics root directory path
- User graphics root directory path
- Filesystem: recursive directory walk capability

## Outputs

- `TILESETS`: `map<tileset_id, directory_path>`
- `vector<id_and_option>` for populating tileset option choices
- Debug warnings on duplicate ids within a single scan pass

## Failure modes

| Condition | Behavior |
| --- | --- |
| Gfx root missing | Skip; continue with other root |
| Directory has no readable `tileset.txt` | Directory not registered |
| `tileset.txt` without `NAME` before `VIEW` | Entry with empty id may be registered (avoid in ports; BN does not validate) |
| `tileset.txt` without `VIEW` | Use `NAME` as display label |
| No tilesets anywhere | Fallback option ids only; empty `TILESETS` |
| Duplicate `NAME` in one scan | Keep first; warn on subsequent |

## Verification

A correct port should demonstrate:

1. `gfx/RetroDaysTileset/tileset.txt` registers id `retrodays` pointing at that folder
2. Recursive scan finds tilesets in nested subdirectories
3. User gfx tileset with new id appears in registry and options
4. User gfx tileset with same `NAME` as game copy does not override registry path
5. Re-running `build_tilesets_list` clears stale entries from removed folders
6. Discovery does not open `tile_config.json` or any `.png`
7. Parsing stops at `VIEW`; `JSON`/`TILESET` lines are ignored at this stage
