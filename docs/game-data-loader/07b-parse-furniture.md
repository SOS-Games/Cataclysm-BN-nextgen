# 07b — Parse furniture

Convert `type: furniture` JSON into `FurnitureDefinition`. Schema: [05b](./05b-furniture-config.md).

---

## Purpose

`FurnitureParser` — v1 subset of `furn_t::load` + `furniture_data.load`.

---

## Call chain (BN)

```text
load_furniture(jo, src)
  if furniture_data.empty(): insert null_furniture_t()
  furniture_data.load(jo, src)
```

---

## v1 parse algorithm

```text
parse(jo, src, registry):
    assert jo.type == "furniture"
    id ← jo.getString("id")

    def ← new FurnitureDefinition()
    def.id ← id
    def.sourceMod ← src
    def.name ← jo.getString("name", id)
    def.symbol ← parseSymbol(jo)      // shared with terrain
    def.color ← jo.optString("color")
    def.moveCostMod ← jo.optInt("move_cost_mod", 0)
    def.requiredStr ← jo.optInt("required_str", -1)
    def.flags ← jo.getStringArray("flags")
    def.looksLike ← jo.optString("looks_like")

    registry.put(id, def)
```

### Field name trap

| Type | Movement field |
| --- | --- |
| Terrain | `move_cost` |
| Furniture | `move_cost_mod` |

Do not read `move_cost` on furniture objects.

---

## Movable vs immovable

BN: `furn_t::is_movable()` → `move_str_req >= 0`.

v1: store `requiredStr`; palette may show icon for movable furniture later.

---

## Override / inheritance

Same as [07a](./07a-parse-terrain.md): HashMap replace; defer `copy-from` / id arrays.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Dispatch | `src/mapdata.cpp` — `load_furniture` |
| Fields | `src/mapdata.cpp` — `furn_t::load` |
| Factory | `src/mapdata.cpp` — `furniture_data` |

---

## Inputs

- `JsonObject` furniture entry
- `src` mod id
- `FurnitureRegistry`

## Outputs

- `FurnitureDefinition` in registry

## Failure modes

| Condition | v1 action |
| --- | --- |
| Missing `id` | Skip + warn |
| Confused `move_cost` on furniture | Ignore (wrong field) |
| Missing `move_cost_mod` | Default 0 (BN mandatory — log if strict) |

## Verification

1. Parse `f_rubble` or similar from fixture
2. `move_cost_mod` ≠ terrain `move_cost` in same test harness
3. Override behavior matches 07a
4. Integration count > 200 for core furniture files
