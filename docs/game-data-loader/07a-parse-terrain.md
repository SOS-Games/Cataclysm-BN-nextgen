# 07a — Parse terrain

Convert `type: terrain` JSON into `TerrainDefinition` records. Schema: [05a](./05a-terrain-config.md).
Model: [08](./08-in-memory-model.md).

---

## Purpose

Implement `TerrainParser` — v1 subset of BN `generic_factory<ter_t>::load` + `ter_t::load`.

---

## Call chain (BN)

```text
load_object → load_terrain(jo, src)
  if terrain_data.empty(): insert null_terrain_t()
  terrain_data.load(jo, src)
    handle_inheritance
    resolve id (string | array)
    ter_t::load(jo, src)
    insert(ter_t)    // replace existing id
```

---

## v1 parse algorithm

```text
parse(jo, src, registry):
    assert jo.type == "terrain"
    id ← jo.getString("id")
    if id empty: throw or skip

    def ← new TerrainDefinition()
    def.id ← id
    def.sourceMod ← src

    def.name ← jo.getString("name", id)
    def.description ← jo.optString("description")
    def.symbol ← parseSymbol(jo)           // string or first season entry
    def.color ← jo.optString("color")      // raw BN color name
    def.moveCost ← jo.optInt("move_cost", 0)
    def.flags ← jo.getStringArray("flags")
    def.looksLike ← jo.optString("looks_like")

    registry.put(id, def)    // replaces previous
```

### Symbol parsing

BN `load_symbol` supports seasonal arrays and `LINE_XOXO` / `LINE_OXOX`. v1:

```text
if symbol is string:
    if symbol in {LINE_XOXO, LINE_OXOX}: store as-is for ASCII renderer
    else: store first character
if symbol is array:
    use first element (season support v2)
```

### Color vs bgcolor

BN requires exactly one. v1: prefer `color`; if only `bgcolor`, store with note
`colorRole=background`.

---

## Override semantics

```text
registry.put(id, def)   // HashMap — O(1) replace
```

Order within file: later array entries with same id overwrite earlier.

Across mods (v2): later mod scan pass wins.

---

## Deferred BN features

| Feature | BN behavior | v1 |
| --- | --- | --- |
| `id` array | Multiple ids per object | Defer or expand loop |
| `copy-from` / `abstract` | Inheritance + deferred | Defer |
| `was_loaded` patch mode | Partial updates | N/A first load |
| Flag → bitflags | `set_flag` | Store strings only |
| Seasonal symbol/color | Arrays indexed by season | First element only |
| `mandatory` fields | `name`, `move_cost` | Enforce |

---

## Null terrain

BN `null_terrain_t()` inserted once. v1 editor may use:

- Absent cell terrain id → not applicable
- Explicit `t_null` in palette for eraser

---

## Strict validation (optional)

If `options.strict`:

- Reject unknown top-level keys (list from 05a)
- Require `name` and `move_cost`

---

## BN source reference

| Concern | Location |
| --- | --- |
| Dispatch | `src/mapdata.cpp` — `load_terrain` |
| Common fields | `src/mapdata.cpp` — `map_data_common_t::load`, `load_symbol` |
| Terrain fields | `src/mapdata.cpp` — `ter_t::load` |
| Insert/replace | `src/generic_factory.h` — `insert` |
| Inheritance | `src/generic_factory.h` — `handle_inheritance` |

---

## Inputs

- `JsonObject` terrain entry
- `src` mod id
- Mutable `TerrainRegistry`

## Outputs

- Updated registry entry for `id`

## Failure modes

| Condition | v1 action |
| --- | --- |
| Missing `id` | Skip + warn |
| Missing `color` and `bgcolor` | Skip + warn (BN errors) |
| Invalid `move_cost` type | Skip + warn |
| `copy-from` only entry | Skip until inheritance implemented |

## Verification

1. Parse inline JSON `t_dirt` fixture → all v1 fields
2. Parse twice same id → second name wins
3. Core integration: count matches manual grep of `"type": "terrain"` in scoped files (approx)
4. `looks_like` round-trips
5. Symbol `"."` stored; `LINE_XOXO` stored literally
