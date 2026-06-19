# 16 — Palette inheritance and weighted chars

Parent palette merging, **`translate`** remaps, weighted value arrays, and **`parameters`** rolls.

**Status:** todo (P10). See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

v1 `PaletteRegistry.merge(paletteIds)` loads each palette file’s char maps flatly. BN mapgen
objects reference palette ids that **inherit** from parents and use **weighted** symbol picks:

```json
"object": {
  "palettes": [ "standard_domestic_palette", "roof_palette" ],
  "terrain": { "#": "t_brick_wall" }
}
```

```json
"type": "palette",
"id": "child_palette",
"palettes": [ "parent_palette" ],
"terrain": { "#": "t_rock" }
```

```json
"`": [ [ "t_grass", 6 ], [ "t_grass_long", 4 ], "t_shrub", [ "t_dirt", 4 ] ]
```

Arcana [`mapgen_palettes.json`](../../../Cataclysm-BN/data/mods/Arcana_BN/overmap_and_mapgen/mapgen_palettes.json) — `arcana_palette` with weighted `` ` `` outdoor mix.

v1 takes **first string only** from weighted arrays → wrong or bare `fill_ter` for many outdoor chars.

---

## BN palette load

```text
mapgen_palette::load_internal(jo):
    result ← empty palette
    if jo has "palettes": [ parent_ids... ]:
        for parent_id:
            parent ← load_palette(parent_id)
            result.add(parent, context)
    result.add(jo terrain/furniture/translate/items/...)
    return result
```

`mapgen_palette::add` merges maps; **later entries override** earlier for same code point.

At mapgen time, `setup_common` builds a **temporary** palette from:

1. Merged `palettes: [ ... ]` list on the mapgen object
2. Inline `object.terrain` / `object.furniture` overrides

---

## Merge order (preview)

For `object.palettes: [ "A", "B" ]` + inline terrain:

```text
merged ← empty MergedCharMap
for id in paletteIds:
    resolved ← PaletteResolver.resolveWithParents(registry, id)
    merged.putAllTerrain(resolved.terrain)
    merged.putAllFurniture(resolved.furniture)
applyInlineOverrides(merged, object.terrain, object.furniture, rng)
applyTranslate(merged, resolved.translate)   // if stored on palette
```

Inline `object.terrain` wins over palette chars (same as v1).

### `resolveWithParents`

```text
resolveWithParents(registry, id, visiting):
    if id in visiting: throw cycle detected
    visiting.add(id)
    palette ← registry.find(id).orElse(warn missing)
    base ← empty
    for parentId in palette.parentIds:   // JSON "palettes": [ ... ] on palette object
        parent ← resolveWithParents(registry, parentId, visiting)
        base.merge(parent)
    base.merge(palette.localMaps)
    visiting.remove(id)
    return base
```

Store `parentIds` on `MapgenPalette` at load time (`PaletteParser`).

---

## Weighted value arrays

BN `mapgen_value` JSON forms:

| JSON | Resolution |
| --- | --- |
| `"t_floor"` | Fixed id |
| `[ "a", "b", "c" ]` | Uniform random among strings |
| `[ [ "a", 6 ], [ "b", 4 ], "c" ]` | Weighted pick; bare string = weight 1 |

```text
PaletteCharResolver.resolve(JsonValue node, Random rng):
    if node.isString(): return node.asString()
    if node.isArray():
        return weightedPick(parseEntries(node), rng)
    if node.isObject(): return resolveParameter(node)  // v2.1
    warn; return null
```

**v1 change:** `merge()` currently resolves arrays to first string at load time — move weighted
resolution to **run time** when building `MergedCharMap` for a specific mapgen run.

---

## `translate` section

Palette JSON may remap symbols after merge:

```json
"translate": { "A": "a", "B": "b" }
```

Apply as: when looking up code point `A`, use mapping for `a` instead. BN applies during palette
construction; preview can apply post-merge before rows loop.

---

## `parameters` (v2.1 stub)

BN mapgen parameters (`"parameters": { "roof_type": { ... } }`) feed into palette values like:

```json
"?": { "param": "roof_type", "fallback": "t_shingle_flat_roof" }
```

**v2.0:** Use `fallback` string only (matches v1 doc). **v2.1:** Roll parameter distributions
with `previewSeed`.

---

## `JsonMapgenRunOptions`

```java
public final class JsonMapgenRunOptions {
    private long previewSeed = 0xC0DA;

    public Random paletteRng() {
        return new Random(previewSeed);
    }
}
```

Same seed as [15](./15-setmap-applier.md) / [18](./18-place-spawners.md) for one deterministic
import.

---

## Planned Java types

| Class | Change |
| --- | --- |
| `MapgenPalette` | Add `List<String> parentIds`, optional `translate` map |
| `PaletteParser` | Read `palettes` array on palette objects |
| `PaletteResolver` | `resolveWithParents(id)` with cycle detection |
| `PaletteRegistry` | `merge(paletteIds, warnings, options)` uses resolver + rng |
| `PaletteCharResolver` | `resolve(JsonValue, Random)` for weighted arrays |

---

## Test fixtures

```text
mapgen-fixtures/json/mapgen_palettes/
  parent_palette.json       # id: parent — "." → t_floor, "#" → t_wall
  child_palette.json        # palettes: [parent], "#" → t_rock
  weighted_palette.json     # "`" weighted grass/dirt (existing file — extend test)

mapgen-fixtures/json/mapgen/
  palette_inherit_room.json # palettes: [child], rows use # and .
```

**Asserts:**

- `#` → `t_rock` (child overrides parent `t_wall`)
- `.` → `t_floor` (inherited)
- Weighted `` ` `` with seed → stable ter id across runs

---

## BN source reference

| Concern | Location |
| --- | --- |
| `load_internal` | `mapgen_palette::load_internal` — `src/mapgen.cpp` ~L3585 |
| `add` merge | `mapgen_palette::add` ~L3516 |
| Temp palette in mapgen | `mapgen_function_json_base::setup_common` |
| Weighted pick | `mapgen_value`, `distribution` in mapgen code |
| Parameters | `mapgen_parameters` — defer v2.1 |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Unknown parent id | Warning; treat as empty parent |
| Cycle `A → B → A` | `IllegalStateException` or warning + skip |
| Weighted array sum 0 | Warning; first string entry |
| Empty palette list on object | Inline terrain/furniture only |

---

## Verification

1. Child overrides parent for same char; child inherits unmentioned chars
2. Three-level chain fixture
3. `previewSeed` fixes weighted `` ` `` outcome
4. Integration: load `arcana_palette`; entrance mapgen `4` char resolves via palette
5. `translate` remaps lookup char before rows
6. `PaletteInheritanceTest` + extend `PaletteCharResolverTest`
