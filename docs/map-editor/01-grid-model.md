# 01 — Grid model

In-memory 2D map for the editor. Terrain ids from
[game data loader](../game-data-loader/08-in-memory-model.md); persistence:
[04](./04-map-file-format.md).

---

## Purpose

`MapGrid` — editable cell state independent of rendering, input, and file I/O.

---

## Cell model

```text
MapCell {
  terrainId: string        // required; e.g. "t_dirt"
  furnitureId: string?   // optional; v1 editor may ignore
}
```

### Invariants

| Rule | Notes |
| --- | --- |
| `terrainId` non-empty for placed cells | Use `t_null` or eraser policy explicitly |
| `furnitureId` null = no furniture | Distinct from `f_null` string |
| One layer each | No stacking multiple furniture |

---

## Coordinates

```text
(0, 0) = top-left cell
x increases right
y increases down (match screen row iteration in TileDisplayScreen)
```

Sub-tile positions not supported in v1.

---

## Grid operations

| Operation | Behavior |
| --- | --- |
| `create(w, h, fillTerrainId)` | All cells → fill id |
| `get(x, y)` | Out of bounds → throw |
| `setTerrain(x, y, id)` | Optional registry validate |
| `setFurniture(x, y, id?)` | v2 |
| `resize(w, h, fill)` | Preserve top-left overlap; new cells → fill |
| `fill(id)` | Whole grid |
| `width()`, `height()` | |

### Resize policy

```text
newGrid[w,h] ← fill
for y in 0..min(oldH, newH):
    for x in 0..min(oldW, newW):
        newGrid[x,y] ← old[x,y]
```

---

## Validation policy

| Mode | When |
| --- | --- |
| Eager | `setTerrain` checks `TerrainRegistry.contains(id)` |
| Lazy | Only validate on save ([04](./04-map-file-format.md)) |

Recommend **lazy** for brush performance; eager optional in debug.

---

## BN comparison

| BN | Map editor grid |
| --- | --- |
| `submap` 24×24 tiles | Arbitrary width×height |
| `ter_id` + `furn_id` per tile | String ids |
| z-levels | z=0 only v1 |
| Mapgen / saves | Local JSON only |

---

## Inputs

- Dimensions, default terrain id
- Optional `TerrainRegistry`

## Outputs

- Mutable `MapGrid`

## Failure modes

| Condition | Behavior |
| --- | --- |
| OOB access | `IndexOutOfBoundsException` |
| Unknown id on eager validate | Reject set or warn |

## Verification

1. 4×4 round-trip in memory
2. Resize 4×4 → 6×6 preserves top-left 4×4
3. `fill` replaces all cells
4. Serialize cell count = width × height
