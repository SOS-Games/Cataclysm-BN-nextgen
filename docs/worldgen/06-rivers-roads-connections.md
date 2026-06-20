# 06 — Rivers, roads, and connections (W5)

**Rivers**, **highways**, and **`overmap_connection`** templates between OMT cells.

**Status:** done (W5). See [implementation-plan](./implementation-plan.md).

---

## Purpose

After base terrain and cities (W4), BN connects the map with:

- River OMT chains (`river`, `river_center`, …)
- Highway grids and rural roads
- Connection templates from `overmap_connection/` JSON

W5 makes generated overmaps **navigable** and visually coherent.

---

## BN concepts

| Concept | Role |
| --- | --- |
| `overmap_connection` | Named template: which OMT ids form N/S/E/W edges |
| Linear OMT flags | Road/rail rotation |
| River pass | Carves paths between lakes/ocean |
| `highway` / `rural_road` | OMT types placed by connection matcher |

---

## Load phase (W5.0)

```java
public final class OvermapConnectionLoader {
    public static OvermapConnectionRegistry load(ScanOptions options);
}
```

Parse `type: overmap_connection` JSON into edge → OMT id mappings.

Requires W1 for validating referenced OMT ids exist.

---

## River generator (v1 simplified)

```text
RiverGenerator.carve(grid, seed, options):
    pick source + sink (lake/ocean cells or grid edges)
    walk A* or drunkard path
    set cells along path to river OMT ids (center/bank from BN table)
```

Full BN hydrology is complex — v1: **one river** on 16×16 test maps.

---

## Highway / road generator (v1 simplified)

```text
HighwayGenerator.connectCities(grid, cities, connectionRegistry):
    for each pair of city centers (MST or nearest neighbor):
        draw straight or L-shaped road using connection template
        rotate edge pieces to match direction
```

Defer: bridge over river, rail, sewer.

---

## Integration order in `OvermapGenerator`

```text
1. base terrain (W4 partial)
2. rivers (W5) — may overwrite terrain
3. cities + specials (W4)
4. roads between cities (W5)
5. optional: rural roads stub
```

Exact order should match BN where easy; document divergences.

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| Connection template missing | Skip segment + warning |
| River hits city | v1: overwrite or divert — pick one |
| Linear flag mismatch | Skip rotation |

---

## BN source reference

| Concern | Location |
| --- | --- |
| Connections | `src/overmap.cpp`, `src/overmap_connection/` |
| River | `src/overmap.cpp` — river generation passes |
| Data | `data/json/overmap/overmap_connection/` |

---

## Verification

1. Fixture map has river chain of ≥3 OMT cells
2. Two city centers connected by road OMT ids
3. Connection loader finds `local_road` or BN equivalent from core data

---

## Risk note

W5 is the **largest BN port** in the W series. Keep v1 narrowly scoped; prefer visual smoke
on 16×16 over full hydrology parity.
