# 04 — Map file format

Nextgen-local JSON persistence for [MapGrid](./01-grid-model.md). **Not** BN `.sav2` or
mapgen templates.

---

## Purpose

`MapFileIO.save(path, grid)` / `load(path, options) → MapGrid`.

---

## Schema version 1

```json
{
  "format": "cdda-bn-nextgen-map",
  "version": 1,
  "width": 32,
  "height": 32,
  "default_terrain": "t_dirt",
  "terrain": [
    "t_dirt",
    "t_grass"
  ],
  "furniture": null
}
```

### Field reference

| Field | Type | Required | Notes |
| --- | --- | --- | --- |
| `format` | string | yes | Constant `"cdda-bn-nextgen-map"` |
| `version` | int | yes | `1` |
| `width` | int | yes | > 0 |
| `height` | int | yes | > 0 |
| `default_terrain` | string | yes | Eraser + resize fill |
| `terrain` | string[] | yes | `width * height`, row-major |
| `furniture` | string[] or null | no | Same length as terrain when present |

### Indexing

```text
index = y * width + x
terrain[index] = cell(x, y).terrainId
```

Row 0 = top of map (y=0).

---

## Save algorithm

```text
array ← new String[w*h]
for y in 0..h:
    for x in 0..w:
        array[y*w+x] ← grid.get(x,y).terrainId
write JSON with pretty print
```

Optional: omit `furniture` key when all null.

---

## Load algorithm

```text
parse JSON
assert format == "cdda-bn-nextgen-map"
assert version == 1
assert terrain.length == width * height
grid ← MapGrid(width, height, default_terrain)
for each index:
    grid.setTerrain(x, y, terrain[index])   // lazy validate
if furniture present:
    load furniture layer (v2)
return grid
```

---

## Validation

| Check | On load |
| --- | --- |
| Wrong format/version | Error |
| Array length mismatch | Error |
| Unknown terrain id | Warn; keep id (gfx may fail) |
| Negative width/height | Error |

---

## File locations

| Context | Path |
| --- | --- |
| User maps | `{user_dir}/maps/` (future) |
| Test fixtures | `core/src/test/resources/maps/*.json` |
| Dev examples | `maps/example_8x8.json` in repo (optional) |

---

## Versioning

Version 2 might add:

- `metadata: { name, author }`
- `z_level`
- `furniture` required layer
- Compressed terrain as run-length encoding

v1 reader must reject unknown `version > 1` with clear message.

---

## BN mapgen relationship

Mapgen JSON (`data/json/mapgen/`) uses different schema (`type: mapgen`, palette chars).
**No import** in v1. Future converter is separate spec.

---

## Inputs

- `MapGrid` or file path

## Outputs

- JSON file or `MapGrid`

## Failure modes

| Condition | Behavior |
| --- | --- |
| IO error | Propagate |
| Schema error | Parse exception with field name |

## Verification

1. Round-trip 3×3 preserves ids
2. Wrong length array → error message cites expected vs actual
3. Fixture in test resources loads in unit test
4. Hand-edited file with `version: 2` rejected
5. `default_terrain` applied on load for validation fallback only — not overwrite cells
