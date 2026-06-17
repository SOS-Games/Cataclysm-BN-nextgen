# 12 — OMT stitch composer

Combine multiple **OMT-sized mapgen pieces** on the same z-level into one `MapGrid` for panning
across multi-tile buildings.

---

## Purpose

`2StoryModern04` and similar `city_building` entries place several 24×24 submaps at
`point: [1, 0, 0]` offsets. P5 loads one piece per floor; P6 **stitches** same-z pieces so the
editor canvas matches BN’s horizontal footprint.

---

## When stitching runs

```text
MapVolumeBuilder.build():
    for z in building.distinctZLevels():
        pieces ← building.piecesAtZ(z)
        if needsStitch(pieces):
            grid ← OmtStitchComposer.stitch(pieces, catalog, palettes, gameData, options)
        else:
            grid ← single JsonMapgenRunner.run(…)
        gridsByZ.put(z, grid)
```

```text
needsStitch(pieces):
    return pieces.size() > 1
        OR any piece with offsetX != 0 OR offsetY != 0
```

---

## Submap stride

BN default **OMT submap** = 24×24 cells.

```java
public final class OmtStitchComposer {
    public static final int DEFAULT_OMT_SIZE = 24;
}
```

If a piece’s `rows` produce width/height ≠ 24, use **actual** `MapGrid` dimensions for that
piece’s footprint when computing pixel offset (document per-piece bounds).

### Offset in cells

```text
destOriginX = piece.offsetX * strideX
destOriginY = piece.offsetY * strideY
```

`strideX` / `strideY` default to `DEFAULT_OMT_SIZE`; v1.5 uses fixed 24. v2 may read from
`mapgensize` when implemented.

---

## Stitch algorithm

```text
stitch(pieces, …):
    warnings ← []
    // Sort for deterministic overlap: offsetY, then offsetX, then overmap id
    sorted ← sort(pieces)

    bounds ← computeBoundingBox(sorted, stride)
    canvas ← MapGrid(bounds.width, bounds.height, defaultFillTer)
    defaultFillTer ← from first successful piece or t_open_air / t_dirt

    for piece in sorted:
        def ← resolveMapgen(catalog, piece)
        if def == null: warn; continue
        pieceGrid ← JsonMapgenRunner.run(def, …)
        blit(canvas, pieceGrid, destX(piece), destY(piece))

    return canvas
```

### `blit`

```text
blit(dest, src, destX, destY):
    for y in 0 .. src.height-1:
        for x in 0 .. src.width-1:
            if inside(dest, destX+x, destY+y):
                dest.setTerrain(destX+x, destY+y, src.terrain(x,y))
                if src.furniture(x,y) not empty:
                    dest.setFurniture(destX+x, destY+y, src.furniture(x,y))
```

**Overlap:** later piece in sort order wins (warn if non-identical overlap).

**Padding:** bounding box includes all piece extents; unset cells use `defaultFillTer` (first
piece’s `fill_ter` from json object if available).

---

## Bounding box

```text
minX = min(piece.offsetX * stride) over pieces
minY = min(piece.offsetY * stride) over pieces
maxX = max(piece.offsetX * stride + pieceWidth) over pieces
maxY = max(piece.offsetY * stride + pieceHeight) over pieces

width  = maxX - minX
height = maxY - minY

// Normalize: if minX/minY > 0, shift blit destinations subtract min
```

Negative `point` offsets (uncommon) supported via normalization.

---

## Camera / pan

Stitched grid may be 48×24, 48×48, etc. Existing [map editor camera](../map-editor/03-render-bridge.md)
and smooth zoom apply without changes. Optional **Fit building** toolbar action:

```text
fitZoomToGrid(): zoom so full grid width/height visible in canvas
```

---

## Limits

| Limit | Default | Reason |
| --- | --- | --- |
| Max OMT width | 8 | 192 cells — memory / UI |
| Max OMT height | 8 | same |
| Max cells | 256×256 | hard cap with warn |

---

## Picker label

```text
2StoryModern04 (building, 5 floors, 2×2 OMT)
```

`isMultiTileAtZ(z)` true for any z → show `2×2 OMT` hint in building summary.

---

## Inputs

- `List<CityBuildingPiece>` at one z-level
- Runnable mapgen per piece

## Outputs

- Single `MapGrid` covering stitched footprint
- Warnings (skipped pieces, overlaps)

## Failure modes

| Condition | Behavior |
| --- | --- |
| All pieces fail | No grid for that z; omit from volume |
| One piece fails | Stitch remainder; warn |
| Piece larger than stride | Expand bounding box; may overlap neighbor |

## Verification

1. Fixture: two 5×5 mapgens at (0,0) and (1,0) with stride 8 → 13×5 or 16×5 grid
2. BN integration: `2StoryModern04` z=0 is 48×24 (when sibling data + mapgens present)
3. Pan from left wing to right wing shows continuous walls
4. Floor switch still works; each z stitched independently

---

## Out of scope (P6)

- Seam / door alignment between OMTs (BN overmap connection logic)
- Rotation per piece
- `predecessor_mapgen` outdoor underlay before stitch
- Vertical stacks in one grid (z remains separate floors)

---

## BN source reference

| Concern | Location |
| --- | --- |
| Multi-tile specials | `multitile_city_buildings.json` |
| Submap placement | `src/mapbuffer.cpp` |
| `mapgensize` | `src/mapgen.cpp` — nested chunks |

---

## Related

- [09-building-bundles-overview](./09-building-bundles-overview.md)
- [13-building-bundle-sources](./13-building-bundle-sources.md)
- [12-omt-stitch-composer](./12-omt-stitch-composer.md)
- [11-map-volume-and-floors](./11-map-volume-and-floors.md)
