# 04a — Tileset manifest (`tileset.txt`)

Load-time parsing of `tileset.txt` to resolve paths into a tileset directory. This is
**separate** from discovery parsing in [02](./02-discovery-and-registry.md), which only reads
`NAME` and `VIEW` to build the registry.

---

## Purpose

Given a selected `tileset_id` and its directory from the `TILESETS` registry, determine:

1. Which JSON config file to open (`json_path`)
2. Which image file is the **legacy** default sheet (`img_path`) — used only when the config
   uses the old `tiles` array (unit 04b)

Modern configs (`tiles-new`) name image files per sheet inside `tile_config.json`; the
manifest `TILESET` value is still present in most packs but is not the primary sheet source
for those configs.

---

## File location

For a registry entry `TILESETS[id] = D`:

```text
manifest_path = D + "/" + "tileset.txt"
```

BN uses `PATH_INFO::tileset_conf()` which returns `"tileset.txt"`.

---

## Manifest format

`tileset.txt` is a line-oriented text file.

### Keys used at load time

| Key | Syntax | Purpose |
| --- | --- | --- |
| `JSON` | `JSON: <path>` | Relative path to tile config from `D` |
| `TILESET` | `TILESET: <path>` | Relative path to default PNG for legacy `tiles` mode |

### Keys used only at discovery (not read here)

| Key | Purpose |
| --- | --- |
| `NAME` | Internal id → registry key (unit 02) |
| `VIEW` | UI display label (unit 02) |

### Comments

Lines where the first token starts with `#` are comments; remainder of line ignored.

### Example

```text
NAME: retrodays
VIEW: RetroDays
JSON: tile_config.json
TILESET: tiles.png
```

---

## Load-time parser algorithm

BN function: `get_tile_information(manifest_path, json_out, tileset_out)`

```text
defaults:
    json_default  = "tile_config.json"
    image_default = "tinytile.png"

json_out  = ""
tileset_out = ""

if read_file(manifest_path) fails:
    json_out  = json_default
    tileset_out = image_default
    return

for each whitespace-delimited token from file:
    if token empty           → skip line
    if token starts with '#' → skip rest of line
    if token contains "JSON"    → next token → json_out
    if token contains "TILESET" → next token → tileset_out
    else                     → skip rest of line (unknown key)

if json_out is empty:
    json_out = json_default
if tileset_out is empty:
    tileset_out = image_default
```

### Differences from discovery parser (unit 02)

| Aspect | Discovery (02) | Load (this unit) |
| --- | --- | --- |
| Keys read | `NAME`, `VIEW` | `JSON`, `TILESET` |
| Stops early | Yes, after `VIEW` | No, reads entire file |
| Value syntax | Rest of line after `NAME`/`VIEW` | Next whitespace token for `JSON`/`TILESET` |
| When it runs | Registry build | Each tileset load |

Because discovery stops at `VIEW`, a manifest that orders keys as `NAME`, `VIEW`, `JSON`,
`TILESET` is fine for both stages: discovery never needs the later keys; load reads the
whole file.

### Token matching

Keys match by **substring** (`token` contains `"JSON"` or `"TILESET"`), so `JSON:` works.

### Path values

`JSON` and `TILESET` values are read with `operator>>` (next whitespace-delimited token),
not the rest of the line. Paths must not contain unquoted spaces.

---

## Path resolution in the loader

After manifest parse, the loader (`tileset_loader::load`) builds absolute paths:

```text
if tileset_id in TILESETS:
    tileset_root = TILESETS[tileset_id]
    parse manifest at tileset_root/tileset.txt → json_conf, tileset_path
else:
    log error: invalid tileset id
    json_conf     = "tile_config.json"    // defaults only, no root
    tileset_path  = "tinytile.png"
    tileset_root  = ""                    // empty

json_path = tileset_root + "/" + json_conf
img_path  = tileset_root + "/" + tileset_path
```

Then open `json_path` as JSON. Failure to open throws.

### Invalid id fallback

When the option value is not in `TILESETS`, `tileset_root` stays empty. Resolved paths
become:

```text
json_path = "/tile_config.json"   // leading slash from empty_root + "/" + name
img_path  = "/tinytile.png"
```

This is almost certainly not a valid location; the open step should fail unless the port
normalizes empty-root joining differently. Treat invalid ids as **load errors** in a new
implementation; BN relies on the registry being populated before load.

### Valid id, missing manifest

If `read_from_file` on `tileset.txt` fails, both outputs fall back to defaults
(`tile_config.json`, `tinytile.png`) relative to `tileset_root`.

---

## How resolved paths are used

| Config style | `json_path` | `img_path` (manifest `TILESET`) |
| --- | --- | --- |
| `tiles-new` array | Opened; per-sheet `file` fields are relative to `tileset_root` | Not used for sheet load |
| Legacy `tiles` array | Opened | Loaded as the single sprite atlas before parsing `tiles` |
| Neither array (tints-only mod) | Opened | Unused |

See [04b](./04b-tile-config-structure.md) for config structure; [05](./05-load-pipeline.md)
for branch selection.

---

## Defaults reference

| Constant | Value | When applied |
| --- | --- | --- |
| Default JSON | `tile_config.json` | Manifest missing, unreadable, or `JSON` empty |
| Default image | `tinytile.png` | Manifest missing, unreadable, or `TILESET` empty |

Most shipped tilesets explicitly set `TILESET: tiles.png` rather than the default
`tinytile.png`.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Manifest parse | `src/cata_tiles.cpp` — `get_tile_information` |
| Path join + open | `src/cata_tiles.cpp` — `tileset_loader::load` |
| Default filenames | `src/path_info.cpp` — `defaulttilejson`, `defaulttilepng`, `tileset_conf` |
| Registry lookup | `TILESETS` map — unit 02 |

---

## Inputs

- `tileset_id` from options (`TILES` or `OVERMAP_TILES`)
- `TILESETS` registry: `id → directory_path`
- File `directory_path/tileset.txt`

## Outputs

- `tileset_root` — directory path for relative joins
- `json_path` — path to `tile_config.json` (or override)
- `img_path` — path to legacy default atlas (or override)
- Opened JSON object (next step in loader; `tile_info` required — see 04b)

## Failure modes

| Condition | Behavior |
| --- | --- |
| Id not in registry | Default relative names; empty root; expect JSON open failure |
| `tileset.txt` not readable | Use default `json` + `image` names under `tileset_root` |
| `JSON` key absent | `tile_config.json` |
| `TILESET` key absent | `tinytile.png` |
| `json_path` not found | Throw / load error |
| Valid manifest, wrong `JSON` path | Throw on open |

## Verification

A correct port should demonstrate:

1. `gfx/HoderTileset/tileset.txt` with `JSON: tile_config.json` → opens
   `gfx/HoderTileset/tile_config.json`
2. `TILESET: hodertiles.png` → legacy mode resolves
   `gfx/HoderTileset/hodertiles.png`
3. Missing `JSON` line → default `tile_config.json` under tileset root
4. Missing manifest file → same defaults under tileset root
5. Load parser reads keys after `VIEW`; discovery parser does not (cross-check with unit 02)
6. `tiles-new` config loads sheets from `file` in JSON, not from manifest `TILESET`
