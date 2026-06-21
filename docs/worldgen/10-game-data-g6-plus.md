# 10 — Game data G6+ (simulation slice)

Extend [game data loader](../game-data-loader/README.md) beyond terrain/furniture for **spawn preview** and future simulation.

**Status:** done. Parallel track; unblocks rich mapgen markers.

---

## Purpose

Mapgen preview P13b collects `place_items` / `place_monsters` as **warnings only**. Loading item
and monster definitions enables:

- Debug overlay dots in map editor
- Validation of group ids at mapgen time
- Future gameplay systems

---

## Suggested load order (G6+)

| PR | `type` | Registry | Priority |
| --- | --- | --- | --- |
| **G6** | `item_group` | `ItemGroupRegistry` | High — `place_items` |
| **G7** | `monster` + `monstergroup` | `MonsterRegistry` | High — `place_monsters` |
| **G8** | `vehicle` | `VehicleRegistry` | Low — `place_vehicles` |
| **G9** | `trap` / `field` | `TrapRegistry`, `FieldRegistry` | Overlay render |

Follow BN dispatch in `src/init.cpp` — add handlers incrementally.

---

## Mapgen integration

Extend [PlaceSpawnerApplier](../mapgen-preview/18-place-spawners.md) P13b:

```text
collectEntitySpawns():
    validate group id against ItemGroupRegistry / MonsterRegistry
    if valid: SpawnMarker with resolved display name
    if invalid: warning (today)
```

Optional editor layer: draw `@` for item spawn, `m` for monster density heatmap.

---

## Mod order

Reuse [G5 mod merge](../game-data-loader/09-mod-load-order.md) — same override semantics as terrain.

---

## Out of scope (G6 track)

| Topic | Notes |
| --- | --- |
| Crafting recipes | G10+ |
| Item spawning on ground | Simulation |
| Monster AI | Game client |
| Full `DynamicDataLoader` parity | Years-scale |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Item groups | `src/item_group.cpp` |
| Monsters | `src/monstergroup.cpp` |
| JSON types | `docs/en/mod/json/reference/` |

---

## Verification

1. `findItemGroup("grocery")` on BN data
2. Mapgen with `place_items` → marker count > 0 when group exists
3. Unknown group still warns, grid unchanged
