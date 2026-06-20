# 02 — Overmap terrain loader (W1)

Load BN **`type: overmap_terrain`** JSON into an **`OvermapTerrainRegistry`**.

**Status:** done (W1). See [implementation-plan](./implementation-plan.md).

---

## Purpose

Every OMT cell on the overmap references an **overmap terrain id** (e.g. `forest`, `house_09_north`).
That type defines:

- Display name, mapgen strategy, rotation, flags
- Which **mapgen** function(s) run when the submap is generated
- Spawn weights, travel cost, lake/river flags (for W5+)

Mapgen preview today resolves ids only via **mapgen catalog** string match. Worldgen needs the
**authoritative OMT layer** first.

---

## BN JSON shape (subset)

```json
{
  "type": "overmap_terrain",
  "id": "house_09",
  "name": "house",
  "sym": "H",
  "color": "light_green",
  "see_cost": 5,
  "travel_cost": 2,
  "flags": [ "NO_ROTATE" ],
  "mapgen": [ { "method": "json", "om_terrain": "house_09" } ]
}
```

Common variants:

| Field | Notes |
| --- | --- |
| `id` | string or array of aliases |
| `mapgen` | array of methods; json uses `om_terrain` key |
| `mapgen` + `weight` | on sibling mapgen entries (catalog side) |
| `extend` / `copy-from` | BN inheritance — v1: resolve one level or warn |
| `rotation` | on type; combined with `_north` suffix at run time |
| `flags` | `LINEAR`, `RIVER`, `LAKE`, … — W5+ |

---

## Scan paths

Reuse [game data mod order](../game-data-loader/09-mod-load-order.md):

```text
data/json/overmap/overmap_terrain/
mods/<mod>/overmap/overmap_terrain/
mods/<mod>/overmap_and_mapgen/     # some mods co-locate; scan if present
```

**Not:** `mapgen/` (that's mapgen preview).

---

## Planned Java types

```java
public final class OvermapTerrainDefinition {
    public final String id;
    public final List<MapgenRef> mapgenRefs;
    public final Set<String> flags;
    public final boolean rotatable;
    // … name, sym — optional for debug UI
}

public final class MapgenRef {
    public final String method;      // "json", "builtin", "lua"
    public final String omTerrain; // json om_terrain key
    public final int weight;
}

public final class OvermapTerrainRegistry {
    public Optional<OvermapTerrainDefinition> find(String id);
    public Collection<OvermapTerrainDefinition> all();
}
```

```java
public final class OvermapTerrainLoader {
    public static OvermapTerrainLoadResult load(OvermapTerrainScanOptions options);
}
```

---

## Load algorithm

```text
for mod in modOrder:
    for file in scan(overmap_terrain dirs):
        for object in JsonDataScanner.parseFile(file):
            if object.type != "overmap_terrain": continue
            for id in normalizeIds(object):
                registry.put(id, parseDefinition(object, id))
                later mods override earlier (same as terrain loader)
```

---

## Integration with mapgen catalog

W1 **indexes references only** — does not embed mapgen JSON.

W3 joins:

```text
MapgenPicker.pick(OvermapTerrainDefinition ter, z, rng):
    for ref in ter.mapgenRefs:
        if ref.method != "json": warn; continue
        candidates += catalog.findByOmTerrain(ref.omTerrain)
    return weightedPick(candidates, rng)
```

Rotation suffix: strip `_north` etc. when catalog miss — reuse
[OvermapTerrainResolver](../mapgen-preview/10-city-building-loader.md) pattern from mapgen preview building package (move or share in W1).

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Unknown `copy-from` parent | Warn; skip or partial def |
| `method: builtin` | Store ref; W3 warns at visit |
| Duplicate id in same mod | Last in file wins + warning |
| Missing `mapgen` | Valid — OMT is uniform terrain (no submap gen) |

---

## Test fixtures

`core/src/test/resources/worldgen-fixtures/overmap_terrain/minimal.json`:

```json
[
  {
    "type": "overmap_terrain",
    "id": "test_field",
    "name": "field",
    "mapgen": [ { "method": "json", "om_terrain": "test_field" } ]
  }
]
```

---

## BN source reference

| Concern | Location |
| --- | --- |
| Load OMT | `src/mapdata.cpp` — `load_overmap_terrain` |
| `oter_t` | `src/omdata.h` |
| Mapgen list on type | `overmap_terrain` JSON `mapgen` array |

---

## Verification

1. Load BN sibling data: `house_09` or `forest` present
2. Fixture id resolves with at least one `mapgen` ref
3. Mod override: patch mod replaces core OMT entry
4. `flags` contains `NO_ROTATE` when JSON specifies
