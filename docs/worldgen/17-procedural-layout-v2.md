# 17 — Procedural layout v2 (W11)

Incremental improvements to **rivers**, **roads**, **mutable specials**, and **join context**
beyond W5–W6 v1 subsets.

**Status:** done. See [v2-implementation-plan](./v2-implementation-plan.md).

Split into **sub-PRs** — one topic per branch.

---

## Purpose

W5–W6 (`RiverGenerator`, `HighwayGenerator`, `MutableSpecialPlacer`) prove connectivity on
8–16×16 maps. W11 moves toward BN `overmap::generate` ordering and edge cases.

**v1 river** (`RiverGenerator.java`):

```text
pick random (startX, 0) → (endX, height-1)
OrthogonalPathCarver.buildPath → paint river_center OMT
```

**v1 roads:** MST over `placedSites` centers → L-shaped `HighwayGenerator.connectSites`.

**v1 mutable:** `SpecialPhaseAssembler` — requires ≥2 pieces; single-phase bias.

---

## Sub-PRs overview

| PR | Focus | Primary classes |
| --- | --- | --- |
| **W11a** | Mutable multi-phase + rotation | `SpecialPhaseAssembler`, `MutableSpecialPlacer` |
| **W11b** | Lakes + river hydrology | `RiverGenerator`, new `LakeGenerator` |
| **W11c** | Connection-aware roads | `HighwayGenerator`, `OvermapConnectionRegistry` |
| **W11d** | Join context → nested mapgen | `JoinContext`, `SubmapGenerator`, `JsonMapgenRunOptions` |

---

## W11a — Mutable specials v2

### v1 limits ([07](./07-mutable-specials-and-joins.md))

| Topic | v1 |
| --- | --- |
| Phases | Effectively first phase with ≥2 pieces |
| Joins | Small lab joins |
| Rotation | `_north` fixed |
| Footprint | Must fit mini-overmap |

### Target

```text
assembleMutable(special, rng):
    placed ← {}
    for phase in special.phases:
        piece ← weightedPick(phase.entries, rng)
        anchor ← findJoinPlacement(placed, piece, special.joins)
        if anchor fails: warn; continue or abort
        placed.add(piece at anchor, rotation)

    return AssembledSpecialLayout(placed)
```

| Feature | Implementation note |
| --- | --- |
| Multi-phase | Loop all `MutableSpecialPhase` entries |
| Rotation | Apply `MapGridRotator.rotationFromOmSuffix` when blitting OMT ids |
| Large footprints | Allow up to 8×8 OMT on 64×64 map (config max) |
| Lab smoke | `lab_mutagen_6_level` or smaller fixture assembles on 32×32 |

### Tests

- `SpecialPhaseAssemblerTest` — 3-phase fixture places ≥4 pieces
- `MutableSpecialPlacerIntegrationTest` — BN `lab_surface` fragment on 32×32

---

## W11b — Hydrology v2

### v1

Single river; overwrites any clearable terrain; one `river_center` id.

### Target (BN subset)

```text
1. Lake noise pass (from region.overmap_lake_settings):
       cells above noise_threshold_lake → lake OMT
2. River sources at lake edges or map boundary
3. River path prefers downhill / toward ocean edge (simplified)
4. Banks: paint river vs river_center from connection table
```

Consume [W9](./15-region-settings-terrain.md) `overmap_lake_settings`:

```json
"overmap_lake_settings": {
  "noise_threshold_lake": 0.25,
  "lake_size_min": 20,
  "shore_extendable_overmap_terrain": [ "forest", "field", … ]
}
```

| Class | Role |
| --- | --- |
| `LakeGenerator` | Noise flood-fill lakes |
| `RiverGenerator` | v2: lake-to-lake or edge-to-edge paths |

**Collision:** river through city — v1 overwrites; W11b document policy (divert or bridge in W11c).

### Tests

- Fixture 32×32 with forced lake noise → `lake` OMT cluster
- River connects two lake regions (≥ N river cells)

---

## W11c — Roads and connections v2

### v1

`HighwayGenerator.connectSites` — straight/L paths; single connection id (`local_road`).

### Target

```text
for each edge in MST(siteA, siteB):
    path ← carvePath(grid, A, B)
    for each step on path:
        dir ← step direction
        omtId ← connectionRegistry.pick(connectionId, dir, neighbors)
        grid.setOmtId(x, y, omtId)
```

| Feature | Notes |
| --- | --- |
| Directional edges | `OvermapConnectionDefinition` N/S/E/W variants |
| Bridge over river | If cell is `river_center`, use bridge OMT from connection pack |
| Highway vs rural | `highway` connection id on MST; `rural_road` stub optional |
| City attachment | Last mile to building center — defer |

### Tests

- `HighwayGeneratorTest` — path uses different OMT for E vs N turn
- Bridge test: river cell + road → `bridge` or `road_north` per data

---

## W11d — Join context for nested mapgen

### Problem

Nested mapgen (`NestedContextChecker`) can require active join ids on neighbors. W3 passes
neighbor OMT ids only:

```java
.withNeighborsByDirection(collectNeighborsByDirection(overmap, omtX, omtY))
```

BN also passes **which joins are active** for assembled specials.

### Target

When visiting OMT inside a **mutable** placement:

```java
final JoinContext ctx = JoinContext.fromOvermap(
    overmap, placementIndex, omtX, omtY, assembledLayout
);
options.withActiveJoins(ctx.getActiveJoinIds())
       .withNeighborsByDirection(ctx.getNeighborsByDirection());
```

| Source | Data |
| --- | --- |
| `AssembledSpecialLayout` | join ids satisfied at assembly time |
| Overmap grid | neighbor omt ids per direction |
| `JoinMatcher` | re-evaluate which joins touch this cell |

### Tests

- Nested fixture with `join` requirement — warns without W11d; silent with
- Lab security wing nested chunk — manual smoke

---

## Integration order (target vs v1)

**v1 actual** (`OvermapGenerator.java`):

```text
1. BaseTerrainFiller
2. RiverGenerator
3. CityPlacer
4. StaticSpecialPlacer
5. MutableSpecialPlacer
6. HighwayGenerator.connectSites
```

**W11 target** (closer to BN):

```text
1. region-weighted base terrain (W9)
2. lakes (W11b)
3. rivers (W11b)
4. cities + static specials (W4)
5. mutable specials (W11a)
6. roads / highways (W11c)
7. rural roads (optional)
```

Changing order is a **breaking** visual change — document in PR; add `OvermapGenerateOptions.legacyOrder` flag for one release if needed.

---

## Files to touch (by sub-PR)

| File | W11a | W11b | W11c | W11d |
| --- | --- | --- | --- | --- |
| `SpecialPhaseAssembler.java` | ✓ | | | |
| `MutableSpecialPlacer.java` | ✓ | | | |
| `RiverGenerator.java` | | ✓ | | |
| `LakeGenerator.java` | | ✓ new | | |
| `HighwayGenerator.java` | | | ✓ | |
| `OvermapConnectionRegistry.java` | | | ✓ | |
| `JoinContext.java` | | | | ✓ |
| `SubmapGenerator.java` | | | | ✓ |
| `OvermapGenerator.java` | reorder | reorder | reorder | |

---

## v2 out of scope (W11)

| Topic | Notes |
| --- | --- |
| Sewers / subways | Separate OMT + connection graph |
| Rail networks | `overmap_connection` rail entries exist |
| Faction camps | Late BN pass |
| Full `overmap.cpp` port | Subset only |
| Polymorphic line rivers | BN advanced — defer |

---

## BN source reference

| Concern | Location |
| --- | --- |
| `overmap::generate` | `src/overmap.cpp` |
| Lakes / rivers | search `lake`, `river` in `overmap.cpp` |
| Highways | `overmap::build_highways` (name may vary) |
| Mutable | `place_overmap_special` / mutable variants |
| Connections | `src/overmap_connection.cpp` |

---

## Verification

1. Each sub-PR: isolated unit tests + 32×32 manual screenshot smoke
2. W11 does not break W7 building visit on same seed
3. PR title states W11a / W11b / W11c / W11d
4. Warnings remain bounded (no thousands of join warnings)

---

## Dependencies

| Requires | PR |
| --- | --- |
| W5 rivers/roads | done |
| W6 mutable | done |
| W7 placement index | recommended for W11d |
| W9 region / lake settings | recommended for W11b |
