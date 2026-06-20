# 07 — Mutable specials and joins (W6)

**Procedural `overmap_special`** assembly: phases, joins, and multi-OMT layouts.

**Status:** done (W6). See [implementation-plan](./implementation-plan.md).

---

## Purpose

BN **mutable** specials (labs, malls, microlabs) are not static `overmaps[]` lists. JSON defines:

- Named **`overmaps`** nodes (pieces)
- **`phases`** — order of placement
- **`joins`** — edge matching between pieces
- **`root`** — starting piece

Worldgen **assembles** a layout at runtime. Mapgen preview today supports **static** specials only
([13-building-bundle-sources](../mapgen-preview/13-building-bundle-sources.md)).

---

## BN JSON shape (sketch)

```json
{
  "type": "overmap_special",
  "id": "lab_surface",
  "overmaps": {
    "surface": { "overmap_terrain": "lab_surface_north", … },
    "entrance": { … }
  },
  "phases": [ [ "surface", 100 ] ],
  "joins": [ … ]
}
```

See BN docs: `docs/en/mod/json/reference/overmap_special.md`.

---

## Assembly algorithm (target)

```text
assembleMutable(special, grid, anchor, rng):
    placed ← empty map pieceId → (omtX, omtY)
    root ← pick from phases[0]
    place(root at anchor)

    for phase in remaining phases:
        piece ← weighted pick
        edge ← findJoinEdge(placed, piece, special.joins)
        if edge found:
            place(piece at edge.offset)
        else:
            warn; skip piece

    for each placed piece:
        blit OMT ids from piece template onto grid
```

---

## Join context for nested mapgen

Nested mapgen in BN checks neighbor OMT types and joins ([21-nested](../mapgen-preview/21-nested-update-mapgen.md)).
W6 should populate:

```java
public final class VisitContext {
    public Map<Direction, String> neighborOmtIds;
    public Set<String> activeJoins;
}
```

Pass into `JsonMapgenRunOptions` for future neighbor-aware nested (v2.1).

---

## Planned Java types

```java
public final class MutableSpecialDefinition { … }
public final class MutableSpecialLoader { … }
public final class SpecialPhaseAssembler { … }
public final class JoinMatcher { … }
```

---

## Relationship to building bundles

| Static special | Mutable special |
| --- | --- |
| P7c `SpecialLayoutFloorComposer` | W6 assembler |
| Pre-defined `overmaps[]` points | Runtime layout |
| In mapgen picker today | W6+ only |

After W6, extend [BuildingBundleScanner](../mapgen-preview/13-building-bundle-sources.md) to register mutable ids for picker (preview assembled layout only).

---

## v1 simplifications

| Topic | v1 |
| --- | --- |
| Phases | Single phase |
| Joins | 2-piece labs only |
| Z-stack | Ground floor OMT only |
| Rotation | `_north` fixed |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Join mismatch | Warn; abort special |
| Out of bounds | Warn; shrink or skip |
| Unknown piece id | Warn |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Mutable placement | `src/overmap.cpp` |
| Data | `data/json/overmap/overmap_mutable/` |
| Join logic | `src/overmap.cpp`, `src/omdata.h` |

---

## Verification

1. One small mutable fixture places 2×2 OMT footprint
2. Visit each OMT runs correct mapgen (W3)
3. Arcana-style multi-piece special (stretch) — manual smoke

---

## Dependencies

| Requires | PR |
| --- | --- |
| Overmap grid + visit | W2, W3 |
| City/static placement patterns | W4 |
| Nested neighbor stubs | [08](./08-mapgen-post-v2-polish.md) optional |
