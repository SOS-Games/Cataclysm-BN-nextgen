# 14 — Multi-z visit (W8)

**Z-aware mapgen selection** and editor support when visiting OMT cells at non-ground levels.

**Status:** done. See [v2-implementation-plan](./v2-implementation-plan.md).

Extends [04](./04-visit-tile-mapgen.md) W3.1 notes.

---

## Purpose

BN stores multiple submaps per OMT column (`z` from basement to roof). W3 logs a warning and
ignores z:

```java
// MapgenPicker.java — current
if (z != 0) {
    addWarning(warnings, "z=" + z + " visit not fully supported; using z=0 mapgen for " + omtId);
}
```

W8 makes z affect both **building volume floor selection** (W7) and **wilderness mapgen pick**.

---

## BN behavior

```text
map::draw_map( tripoint_omt, z )
  → pick mapgen for om_terrain at that z
  → basement / ground / roof may be different om_terrain ids on the SAME overmap cell column
```

In nextgen v1 simplification, each **overmap cell** has one `omtId` string; multi-floor buildings
use **different ids per floor piece** blitted on the same `(x,y)` only when BN uses separate OMT
types (e.g. `2storyModern01_first` vs `_second` on different cells, or `_basement` on same cell
in some layouts).

**Two patterns in BN data:**

| Pattern | Example | W8 handling |
| --- | --- | --- |
| Different OMT ids per floor on different cells | `house_09_north` ground + `house_09_roof` on upper OMT slot | W7 volume + floor cycle |
| Single OMT id, multiple mapgen by z | Rare; mapgen `om_terrain` match | `MapgenPicker` z filter |
| Suffix encodes floor | `*_basement`, `*_roof`, `*_second` | `ZLevelResolver.inferFromOmtId` |

---

## Resolution paths

```text
visit(overmap, x, y, z):
    if PlacedBuildingIndex hit:
        volume ← build/load volume (W7)
        activeZ ← ZLevelResolver.activeZForVisit(volume, z, omtId, pieceAt(x,y))
        return grid at activeZ

    else:
        def ← MapgenPicker.pick(omtId, z, rng, ...)
        return JsonMapgenRunner.run(def, ...)
```

---

## `ZLevelResolver`

```java
public final class ZLevelResolver {

  /** BN-ish floor from OMT id suffix. */
  public static int inferFromOmtId(final String omtId);

  /** When visiting a building volume, which floor grid to show. */
  public static int activeZForVisit(
      final MapVolume volume,
      final int requestedZ,
      final String omtId,
      final Optional<CityBuildingPiece> pieceAtCell
  );
}
```

### Suffix table (initial heuristic)

| Substring in `omtId` | Inferred z |
| --- | --- |
| `_basement` | -1 (or min volume z if lower) |
| `_first`, `_ground`, `_1` (floor) | 0 |
| `_second`, `_2` | 1 |
| `_roof` | max volume z or explicit roof z |
| (none) | `requestedZ`, else volume.getActiveZ() |

Strip rotation suffix first (`_north`, …) via `OvermapTerrainResolver.stripRotation`.

### Piece-at-cell helper

When volume has `pieceLayouts` at active z:

```text
find piece where layout contains (localX, localY) relative to building
if piece.zLevel known → prefer that z in volume
```

---

## `MapgenPicker` extensions

```text
pick(omtId, z, rng, registry, catalog, warnings):

    candidates ← OterMapgenIndex.candidatesForOmt(omtId, ...)  // existing

    if z != 0 && candidates.size() > 1:
        filtered ← filter by z hint in def.object comment, om_terrain array, or id suffix
        if filtered not empty:
            candidates ← filtered

    if candidates empty → warn, return empty
    if size == 1 → return it
    return pickWeighted(candidates, rng)
```

### z hint sources (priority order)

1. Mapgen JSON `"zlevel"` field if present (BN uses on some defs — parse when found)
2. `om_terrain` entry exactly matching `omtId` with floor-specific suffix
3. Definition `//` comment convention (avoid in v1 — fragile)

**v2 simplification:** suffix on `omtId` + building volume path covers 95% of preview content.

---

## Cache and API

`SubmapKey` already mixes z:

```java
// SubmapSeed.java
mixed ^= key.getZ() * 83492791L;
```

| API | W8 change |
| --- | --- |
| `WorldgenPreviewService.visit(overmap, x, y)` | keep default z=0 |
| `visit(overmap, x, y, z)` | already exists — wire editor |
| `SubmapCache` | no change |
| `VolumeCache` (W7) | key excludes z — volume holds all floors |

Building visits: cache volume once; switching floor is in-memory (`mapVolume.setActiveZ`).

---

## Editor integration

| Control | Behavior |
| --- | --- |
| Overmap visit (Enter / double-click) | `visit(x, y, 0)` |
| After W7 building visit | `[` / `]` call `mapVolume.cycleFloor` (existing) |
| Optional W8.1 | Overmap HUD: `,` / `.` adjust visit z before enter |
| Status line | Show `floor z=` from `mapVolume` (existing `buildingFloorHint`) |

### Optional: visit z from OMT id

When user selects roof OMT cell on overmap (if blitted):

```text
visitZ ← ZLevelResolver.inferFromOmtId(selectedOmtId)
visit(overmap, x, y, visitZ)
```

---

## `MapVolume` editor alignment

`MapVolume` (mapgen preview) already exposes:

- `floorCount()`, `getActiveZ()`, `getGridAtZ(z)`
- `activeFloorIndex()`, `cycleFloor(delta)`

W8 ensures `replaceGrid` uses `volume.getGridAtZ(activeZ)` when cycling — same as picker import.

---

## Test plan

| Test | Assert |
| --- | --- |
| `ZLevelResolverTest` | `2storyModern01_basement` → -1 or basement z |
| `MapgenPickerZTest` | two defs same base terrain, different suffix → z selects |
| `SubmapGeneratorZTest` | same (x,y), z=0 vs z=-1 → different cache keys |
| `SubmapGeneratorZTest` | building visit: infer roof OMT → activeZ roof |
| Integration | visit basement OMT on placed house → furniture differs from ground |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| z≠0, no z-specific candidates | Warn once; use all candidates (current) |
| z below volume min | Clamp to min z + warn |
| `open_air` above roof | `hasGrid()` false; status explains |
| Building + requested z mismatch | Prefer piece z over requested |

---

## v2 simplifications

| Topic | W8 |
| --- | --- |
| Full `mapbuffer` 2×2 submaps per OMT | Still one 24×24 grid per visit |
| Underground below building footprint | Only OMT ids on overmap grid |
| Air z-levels | Not visitable |

---

## Files to touch

| File | Change |
| --- | --- |
| `worldgen/visit/ZLevelResolver.java` | new |
| `worldgen/submap/MapgenPicker.java` | z filter |
| `worldgen/submap/SubmapGenerator.java` | pass z to resolver |
| `view/MapEditorScreen.java` | optional visitZ from OMT id |

---

## Verification

1. `MapgenPicker` does not emit z=0 stub for `*_basement` OMT with dedicated mapgen
2. Cache hit only when same (seed,x,y,z) — not when z changes
3. Building: cycle floors after overmap visit without re-running full volume build
4. Wilderness: different z produces different grid when BN data has distinct defs

---

## Dependencies

| Requires | PR |
| --- | --- |
| W7 building visit | strongly recommended |
| W3 `SubmapKey` / `SubmapSeed` | done |
