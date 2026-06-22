# 19 — Visit / mapbuffer fidelity (W13)

Close the gap between **one 24×24 preview submap per OMT** and BN **`mapbuffer`** visit
behavior at multitile edges and nested mapgen context.

**Status:** done. See [v3-implementation-plan](./v3-implementation-plan.md).

**Prerequisite:** W7–W8 building visit, W11d active joins.

---

## Purpose

v2 visit path:

```text
SubmapGenerator.visit(overmap, omtX, omtY, z)
  → PlacedBuildingIndex? → MapVolumeBuilder
  → else MapgenPicker → JsonMapgenRunner → one MapGrid
```

BN visit path:

```text
map::draw_map
  → 2×2 submaps per OMT (mapbuffer)
  → oter_mapgen weighted pick with full neighbor + join + connection context
  → may run multiple mapgens into adjacent submap slots
```

W13 makes **corner and edge OMT visits** trustworthy for content QA — either by fixing stitch
gaps without full mapbuffer, or by introducing a minimal `Mapbuffer` layer.

---

## Current gaps

| Gap | v2 behavior | BN behavior |
| --- | --- | --- |
| Submaps per OMT | 1× `MapGrid` 24×24 | 2×2 submaps in `mapbuffer` |
| Volume stitch | `MapVolumeBuilder` for buildings | Same idea at game scale |
| Nested `connections` | Not passed to runner | Road template match per direction |
| Builtin / Lua | Skipped with warning | Full game mapgen |
| Cache key | `seed + omtX + omtY + z` | Includes submap offset within OMT |

Reference: [01-overview-and-scope](./01-overview-and-scope.md), [13-building-aware-visit](./13-building-aware-visit.md).

---

## Strategy: W13a before W13b

| Phase | When | Deliverable |
| --- | --- | --- |
| **W13a** | First | Audit + fix top stitch/nested failures on multitile corners |
| **W13b** | Only if W13a insufficient | `Mapbuffer` with 2×2 `MapGrid` per OMT |
| **W13c** | Parallel or after W13a | Nested `connections` in `JsonMapgenRunOptions` |

**Gate for W13b:** Document ≥3 failing corner cases that cannot be fixed in `MapVolumeBuilder` /
`SubmapGenerator` alone (fixture list in repo).

---

## W13a — Stitch audit and edge fixes

### Target cases

| Case | OMT position | Expected |
| --- | --- | --- |
| Multitile house NW | Corner of footprint | Matches picker import for that OMT id |
| Multitile house SE | Opposite corner | Rotation + stitch consistent |
| Mutable lab join | Interior edge | Active join; nested silent |
| Duplex ground vs upper | Same `(x,y)` different z | W8 routing still correct after fixes |

### Approach

```text
1. Fixture: 16×16 or 32×32 overmap with known building placement (seed fixed)
2. For each footprint cell: visit via SubmapGenerator vs picker import
3. Diff ter/furn layers (hash or sample cells on shared edges)
4. File failures as W13a tickets with OMT id + coord
```

### Files to touch

| File | Change |
| --- | --- |
| `submap/SubmapGenerator.java` | Neighbor context completeness |
| `mapgen/compose/MapVolumeBuilder.java` | Edge piece alignment |
| `mapgen/json/NestedContextChecker.java` | Stricter tests |
| `mapgen/json/JsonMapgenRunOptions.java` | Connection context fields (W13c) |

### Tests

| Test | Assert |
| --- | --- |
| `SubmapGeneratorCornerStitchTest` | NW + SE OMT grids match golden hashes |
| `NestedContextConnectionTest` | Chunk skipped when connection fails; passes when set |

---

## W13b — Minimal mapbuffer (optional)

### Model

```text
Mapbuffer
  key: (worldSeed, omtX, omtY, z)
  value: MapGrid[2][2]   // or flat 48×48 with BN submap indices

SubmapCoord
  omtX, omtY, z
  subX, subY  // 0..1 within OMT
```

BN uses `SEEX`×`SEEY` (12×12) per submap × 2×2 = 24×24 per OMT. Nextgen mapgen JSON is already
24×24 — W13b may **logical** split for cache/visit without changing runner output size in v1.

**Decision point (implementer):**

| Option | Pros | Cons |
| --- | --- | --- |
| A. Logical 2×2 cache slots, one 24×24 grid | Smaller change | Not BN-identical |
| B. Four 12×12 runners | BN-faithful | 4× mapgen cost |
| C. Stay 24×24; fix edges only (W13a) | Cheapest | Documented BN gap |

Default recommendation: **W13a + option C** unless content authors require option B.

### Files (if W13b lands)

```text
worldgen/mapbuffer/Mapbuffer.java
worldgen/mapbuffer/SubmapCoord.java
worldgen/submap/SubmapGenerator.java  — route through mapbuffer
```

---

## W13c — Nested connection context

Extend `JoinContext` or parallel `ConnectionContext`:

```java
options.withConnectionsByDirection(ctx.getConnectionsByDirection());
```

`NestedContextChecker.matchesConnections` today fails when connections array non-empty — pass
empty only when unknown; pass resolved road template ids when on a highway OMT.

### Tests

- Fixture mapgen with `connections: { "north": "local_road" }` — passes when north OMT is road
- Fails when north is `field`

---

## Builtin / Lua mapgen

| Method | W13 stance |
| --- | --- |
| `json` | In scope (done) |
| `builtin` | Document; optional read-only stub that logs id |
| `lua` | Out of scope unless catalua port exists |

---

## Integration

W13 does **not** change `OvermapGenerator` layout order. It affects **visit only**.

Cache invalidation interacts with [21-exploration-and-world-coords](./21-exploration-and-world-coords.md)
(W15) — document when stitch fixes require cache bust.

---

## Verification

1. W13a: corner stitch tests green on fixture multitile house
2. W13c: nested connection test green
3. Manual: 64×64 overmap → visit all cells of a house → no visible seam vs picker
4. W13b: only if gate met — document BN gap closed or remaining

---

## Dependencies

| Requires | PR |
| --- | --- |
| W7 volume visit | done |
| W8 z routing | done |
| W11d joins | done |
| Mapgen P6 stitch | done |

---

## Out of scope (W13)

| Topic | Notes |
| --- | --- |
| Save submaps to disk | W16 |
| Regenerate single OMT on overmap | W15 / editor |
| Full builtin mapgen port | Game client |
