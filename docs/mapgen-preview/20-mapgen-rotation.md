# 20 — Mapgen rotation

Apply **`object.rotation`** and overmap terrain rotation to preview `MapGrid` geometry.

**Status:** P14 done. See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

BN rotates the 24×24 submap 0–3 quarter-turns clockwise so building entrances align with OMT
facing (`_north`, `_east`, …). v1 preview shows **unrotated JSON rows** — wrong layout for
rotated pieces in [OmtStitchComposer](./12-omt-stitch-composer.md) and single-mapgen import.

Rotation also interacts with [predecessor mapgen](./17-predecessor-mapgen.md) (undo/redo) and
final OMT type rotation from `overmap_terrain` definition.

---

## BN behavior

End of `mapgen_function_json::generate`:

```text
m.rotate(mapgen.rotation.get())           // object.rotation 0-3
if ter.is_rotatable() || ter.is_linear():
    m.rotate(ter.get_rotation())          // from OMT type + suffix
```

Rotation is **clockwise** quarter turns on the whole submap (terrain + furniture + items).

Predecessor block **before** main content applies inverse rotation so the outdoor template stays
aligned when the building rotates ([17](./17-predecessor-mapgen.md)).

---

## Two rotation sources

| Source | When | Preview input |
| --- | --- | --- |
| `object.rotation` | Json mapgen field | `JsonValue object.getInt("rotation", 0)` |
| OMT type | `overmap_terrain` + `_north` suffix | `JsonMapgenRunOptions.omtRotation` or strip suffix → enum |

`OvermapTerrainResolver.stripRotation` is for **catalog lookup only** — does not rotate geometry.

### Suffix → quarter turns (BN convention)

Implementer must match BN `oter_rotation` / `overmap_terrain::get_rotation()` — typical mapping:

| Suffix | Rotation |
| --- | --- |
| (none) / `_north` | 0 |
| `_east` | 1 |
| `_south` | 2 |
| `_west` | 3 |

Verify against `src/overmap.cpp` / `overmap_terrain` when coding.

---

## `MapGridRotator` algorithm

Clockwise 90° on width×height grid:

```text
rotate(grid, turns):
    turns ← turns % 4
    if turns == 0: return grid
    result ← grid
    repeat turns times:
        result ← rotate90CW(result)
    return result

rotate90CW(grid):
    w ← grid.width(); h ← grid.height()
    out ← new MapGrid(h, w, defaultTer)   // dimensions swap
    for y in 0 .. h-1:
        for x in 0 .. w-1:
            (nx, ny) ← (h - 1 - y, x)   // verify index formula against test
            copy ter+furn from (x,y) to (nx,ny)
    return out
```

Use asymmetric 3×5 test pattern to validate index math in unit test.

---

## Integration points

### `JsonMapgenRunner`

```text
grid ← generateUnrotated(...)   // full pipeline through regional
totalTurns ← object.rotation + options.omtRotation
return MapGridRotator.rotate(grid, totalTurns)
```

### `OmtStitchComposer` (v2.1)

Per-piece rotation before blit:

```text
pieceGrid ← JsonMapgenRunner.run(def, ..., omtRotation from piece.overmapId suffix)
blit(canvas, pieceGrid, destX, destY)
```

Avoid rotating entire stitched canvas — different pieces may have different facings in edge cases.

### `MapVolumeBuilder`

Pass piece `overmapId` into run options when generating each floor piece.

---

## Planned Java types

```java
public final class MapGridRotator {
    public static MapGrid rotate(MapGrid source, int quarterTurnsClockwise);

    public static int rotationFromOmSuffix(String overmapId);
}
```

```java
// JsonMapgenRunOptions
private int omtRotation = 0;
```

---

## Test fixtures

### `test_rotation_l.json`

5×3 grid with distinct ter at (0,0) only — after `rotate(1)` landmark moves to expected cell.

### Integration

`test_crop_east` multitile — east suffix piece differs from west; with rotation enabled, stitched
layout may match BN crop field orientation.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Final rotate | `mapgen_function_json::generate` ~L4232 |
| Predecessor unrotate | ~L4185–4191 |
| `map::rotate` | `src/map.cpp` |
| OMT rotation | `overmap_terrain` type |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| `rotation` outside 0–3 | Mod 4 |
| Non-square grid | Rotate bounding box (swap w/h each 90°) |
| Predecessor without unrotate | Documented v2.0 gap |

---

## Verification

1. `MapGridRotatorTest` — 90°, 180°, 270°, 360° identity
2. Asymmetric pattern landmark cell coordinates
3. `rotationFromOmSuffix("foo_east") == 1`
4. Runner integration: same def + `omtRotation` differs → different grid hash
5. Optional: stitch crop multitile visual/manual
