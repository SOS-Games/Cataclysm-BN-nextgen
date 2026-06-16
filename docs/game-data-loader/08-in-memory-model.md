# 08 — In-memory model

Public Java types and lookup API after [06](./06-load-pipeline.md). Consumers:
[map editor](../map-editor/README.md), future simulation.

---

## Purpose

Immutable registries keyed by string id — simplified BN `generic_factory<ter_t>` /
`generic_factory<furn_t>` without `int_id` compression.

---

## BN reference structures

```text
generic_factory<ter_t> terrain_data
  list: vector<ter_t>           // dense int_id index
  map:  string_id → int_id

ter_t / furn_t:
  string_id id
  map_data_common_t base fields
  type-specific fields
  check() const
```

BN gameplay uses `ter_id` / `furn_id` (int) for map cells after `set_ter_ids()`.

Nextgen map editor stores **strings** in `MapGrid` and resolves gfx by string.

---

## Types (nextgen)

### `TerrainDefinition`

```java
final class TerrainDefinition {
    String id;
    String name;
    String description;      // optional
    String symbol;           // 1 char or LINE_* token
    String color;            // BN color name, unresolved
    int moveCost;
    List<String> flags;      // unmodifiable
    String looksLike;        // nullable
    String sourceMod;        // provenance
}
```

### `FurnitureDefinition`

```java
final class FurnitureDefinition {
    String id;
    String name;
    String symbol;
    String color;
    int moveCostMod;
    int requiredStr;
    List<String> flags;
    String looksLike;
    String sourceMod;
}
```

### `TerrainRegistry` / `FurnitureRegistry`

```java
interface TerrainRegistry {
    Optional<TerrainDefinition> find(String id);
    List<String> allIds();           // sorted, unmodifiable
    int size();
    boolean contains(String id);
}
```

Built with `Map<String, Definition>` + cached sorted id list invalidated on build only
(immutable after load).

### `LoadedGameData`

```java
final class LoadedGameData {
    TerrainRegistry getTerrain();
    FurnitureRegistry getFurniture();
    List<String> getSourceMods();
}
```

---

## Invariants

| Rule | Reason |
| --- | --- |
| Immutable after `GameDataLoader.load*` returns | Thread-safe read for UI |
| `allIds()` lexicographically sorted | Stable palette order |
| `find(id)` case-sensitive | BN ids are case-sensitive |
| Empty id never stored | |
| `t_null` / `f_null` optional | BN always has null; editor may omit |

---

## Gfx bridge

```text
terrain.find("t_dirt").id  →  tileset.findTile("t_dirt")
```

No guarantee every terrain id has gfx. See [10](./10-post-load-validation.md).

`looks_like` chain resolution is **draw-time** in BN, not at load. v1 editor does not walk
chains.

---

## Lifecycle

```text
LoadedGameData data = GameDataLoader.loadCore(opts);
// read-only use
// reload: new load replaces reference; old registries GC'd
```

No native GPU resources in v1 — no `dispose()` required (unlike `LoadedTileset`).

---

## Future extensions

| Field | When |
| --- | --- |
| `int_id` table | Simulation performance |
| Flag enums | Collision / pathfinding |
| `TerBucket` by category | Palette grouping |
| Merged view terrain+furniture | Single palette search |

---

## BN source reference

| Concern | Location |
| --- | --- |
| ter_t | `src/mapdata.h` |
| furn_t | `src/mapdata.h` |
| Factories | `src/mapdata.cpp` — `terrain_data`, `furniture_data` |
| Globals | `src/mapdata.cpp` — `set_ter_ids` |

---

## Inputs

- Parsed definitions from 07a / 07b

## Outputs

- `LoadedGameData` ready for consumers

## Failure modes

- Empty registries when load failed — caller checks `size()`

## Verification

1. `find("t_dirt")` non-empty after core load
2. `allIds()` sorted; binary search compatible
3. Public API has no mutators on registries
4. `LoadedGameData` holds both terrain and furniture registries
5. `sourceMod` set correctly in v2 multi-mod test
