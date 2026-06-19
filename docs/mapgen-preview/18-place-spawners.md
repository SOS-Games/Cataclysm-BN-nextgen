# 18 — Place spawners (`place_*`)

Apply **`place_terrain`**, **`place_furniture`**, **`place_items`**, **`place_monsters`**, and
related keys — rectangle spawners with chance, repeat, and density.

**Status:** P13 / P13b done. See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

After setmap and row placings, BN’s `jmapgen_objects` applies **`place_*`** arrays to scatter
terrain, furniture, items, monsters, vehicles, and NPCs. v1 ignores them — labs and Arcana
structures miss desks, traps-as-furniture, and loot (items/monsters are invisible in ter/furn
preview anyway but furniture matters).

Arcana entrance example:

```json
"place_monsters": [
  { "monster": "GROUP_ARCHON_RESURGENCE", "x": [ 0, 23 ], "y": [ 0, 23 ], "density": 0.05 }
]
```

---

## BN generation order

Inside `apply_contents` after setmap:

```text
objects.apply(md)  includes:
    - row-derived jmapgen_piece (terrain/furniture from chars)
    - place_terrain, place_furniture, place_items, place_monsters, ...
    - nested mapgen pieces (phase nested_mapgen)
```

Setup in `mapgen_function_json_base::setup_internal`:

```text
objects.load_objects<jmapgen_ter>(jo, "place_terrain")
objects.load_objects<jmapgen_furn>(jo, "place_furniture")
objects.load_objects<jmapgen_item_group>(jo, "place_items")
objects.load_objects<jmapgen_monster_group>(jo, "place_monsters")
...
```

Pieces sort by `mapgen_phase` — terrain before furniture before nested.

---

## Key reference (subset)

| JSON key | BN type | Preview phase |
| --- | --- | --- |
| `place_terrain` | `jmapgen_ter` | P13 |
| `place_furniture` | `jmapgen_furn` | P13 |
| `place_items` | `jmapgen_item_group` | P13b |
| `place_monsters` | `jmapgen_monster_group` | P13b |
| `place_vehicles` | vehicle spawner | P13b — skip v2 |
| `place_npcs` | NPC spawner | P13b — skip v2 |
| `place_loot` | alias | P13b |
| `place_fields` | field spawner | defer |

### Common entry fields

| Field | Notes |
| --- | --- |
| `x`, `y` | int or `[min,max]` — random roll per placement |
| `x2`, `y2` | Optional second corner → rectangle spawn |
| `chance` | Often **percent** for items (75 = 75%) — differs from setmap `one_in`! |
| `repeat` | `[min,max]` placement count |
| `density` | Monsters: spawns per tile probability |
| `ter` / `terrain` | Ter id |
| `furn` / `furniture` | Furn id |
| `item` / `items` | Item group id |
| `monster` | Monster group id |

**Implementer note:** Read BN parser per type — `chance` semantics differ between
`jmapgen_item_group` and `jmapgen_setmap`.

---

## Preview algorithm (P13 — ter/furn)

```text
applyPlaceTerrainAndFurniture(grid, object, rng, warnings):
    applyArray(grid, object.get("place_terrain"), TER, rng)
    applyArray(grid, object.get("place_furniture"), FURN, rng)

applyArray(grid, array, kind, rng):
    for entry in array:
        count ← rollRepeat(entry, rng)
        for i in 0 .. count-1:
            if !passesChance(entry, kind, rng): continue
            (x,y) ← rollPoint(entry, rng)   // or rect pick
            if kind == TER: grid.setTerrain(x,y, id)
            if kind == FURN: grid.setFurniture(x,y, id)
```

Run **after** rows loop, **before** regional resolve — same as BN `objects.apply` batch with rows.

Furniture phase does not clear terrain under furn (BN keeps floor ter).

---

## P13b — entity spawns (metadata only)

Preview cannot simulate monsters/items without game data loaders. Collect markers:

```java
public final class SpawnMarker {
    public enum Kind { ITEM_GROUP, MONSTER_GROUP }
    public final Kind kind;
    public final String groupId;
    public final int x, y;
    public final float density;
}
```

`MapEditorScreen` may draw debug dots (optional); default: attach to `JsonMapgenRunOptions` warnings
summary (`"12 monster spawns (GROUP_X) not shown"`).

---

## Shared helpers

Reuse `JmapgenIntRange` from [15](./15-setmap-applier.md) for `x`/`y`/`repeat`.

```java
public final class PlaceSpawnerApplier {
    public static void applyTerrainAndFurniture(
        MapGrid grid,
        JsonValue object,
        JsonMapgenRunOptions options
    );

    public static List<SpawnMarker> collectEntitySpawns(
        JsonValue object,
        JsonMapgenRunOptions options
    );
}
```

---

## Test fixtures

### `test_place_furniture.json`

```json
"object": {
  "fill_ter": "t_floor",
  "rows": [ ".....", ".....", ".....", ".....", "....." ],
  "place_furniture": [
    { "furn": "f_chair", "x": 2, "y": 2 }
  ]
}
```

### `test_place_monsters_warn.json` (P13b)

```json
"place_monsters": [
  { "monster": "GROUP_TEST", "x": [ 0, 4 ], "y": [ 0, 4 ], "density": 1.0 }
]
```

Assert warnings list mentions skipped spawns; grid ter unchanged.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Load place arrays | `setup_internal` ~L3886+ |
| `jmapgen_item_group` | `src/mapgen.cpp` ~L2095 |
| `mapgen_phase` order | `mapgen.h` enum |
| Apply | `jmapgen_objects::apply` |

---

## Dependencies

| Feature | Requires |
| --- | --- |
| `place_terrain` / `place_furniture` | `MapGrid` |
| `place_items` | Item groups + `ItemRegistry` (G6+) |
| `place_monsters` | Monster groups + `MonsterRegistry` |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Unknown furn/ter | Warn; optional skip |
| Unknown group id | Warn (P13b) |
| Rectangle inverted | Normalize min/max |

---

## Verification

1. Single `place_furniture` at (2,2) → `f_chair`
2. Seeded repeat places N chairs in rect
3. Runs after rows: row space `.` keeps ter under new furn
4. P13b: `collectEntitySpawns` count ∝ density × area at fixed seed
5. Arcana entrance: no crash on `place_monsters`; warning ok
