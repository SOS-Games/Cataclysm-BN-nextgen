# 05 — Multitile autoconnect (terrain)

Neighbor-aware terrain sprite selection at **draw time**. Fixes wrong wall/grass edges when
mapgen data is correct but the editor draws only the parent tile id.

**Status:** done (**R1**). See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

BN tilesets register multitile parents (`t_wall`, `t_grass`) plus connection subtiles
(`t_wall_corner`, `t_grass_edge`, …). The loader already stores these in `LoadedTileset`
([07b](../tileset-loader/07b-tile-registration.md)); the editor still calls
`tileset.findTile(terrainId)` and picks variant index 0.

After R1, `MapEditorScreen` passes `(x, y, MapGrid)` into resolution so connected cells pick
the correct subtile and rotation.

---

## BN reference

| Function | Role |
| --- | --- |
| `get_terrain_orientation` | Same-id 4-neighbor mask → subtile + rotation |
| `get_connect_values` | `connects_to` group mask → subtile + rotation |
| `get_rotation_and_subtile` | 4-bit mask → enum subtile + 0–3 rotation |

Neighbor bit order (cardinal, same as BN `get_terrain_orientation`):

```text
bit 0 → south neighbor
bit 1 → east
bit 2 → west
bit 3 → north
```

Mask `val = Σ (neighbor matches ? 1 << i : 0)` then lookup:

| `val` | Subtile | Rotation |
| --- | --- | --- |
| 0 | `unconnected` | 0 |
| 15 | `center` | 0 |
| 1,2,4,8 | `end_piece` | 0,1,3,2 |
| 9,6 | `edge` | 0,1 |
| 3,10,12,5 | `corner` | 0,1,2,3 |
| 7,11,14,13 | `t_connection` | 0,1,2,3 |

Subtile names match `TileRegistrar.MULTITILE_KEYS` and gfx `additional_tiles[].id`.

**Source:** `src/cata_tiles.cpp` — `get_rotation_and_subtile`, `get_terrain_orientation`.

---

## R1 phases

### R1.0 — Same-id neighbors (v1 of R1)

Enough for grass, dirt mounds, simple fences where connection = identical terrain id.

```text
resolveTerrainTileId(terId, x, y, grid, tileset):
    parent ← tileset.findTile(terId)
    if parent == null or !parent.isMultitile():
        return terId

    mask ← 0
    for each direction d in [S, E, W, N]:
        nId ← grid.getTerrain(neighbor(x,y,d))
        if nId == terId:
            mask |= bit(d)

    subtile, rotation ← getRotationAndSubtile(mask)
    subId ← terId + "_" + multitileKey(subtile)
    if tileset.findTile(subId).present:
        return subId   // rotation applied in R1.2
    return terId
```

### R1.1 — `connects_to` groups (stretch)

Walls, pavement, counters connect via game data `connects_to: "WALL"` not identical ids.
Requires indexing terrain/furniture by connect group (new small parser helper or flag on
`TerrainDefinition`).

Defer until R1.0 is green on house previews; document in `gamedata/connect/ConnectGroupIndex.java`.

### R1.2 — Rotation

When `TileDefinition.isRotates()` and subtile is connection type, pick sprite variant by
`rotation` (0–3). BN rotates fg/bg for non-multitile-subtile tiles; subtiles often bake rotation
into art — match sprite viewer behavior first, then BN parity.

Furniture multitile: same pipeline with `MapCell.furnitureId` — **lower priority** (M5+ or R1.1b).

---

## `MultitileConnectResolver` API

```java
public final class MultitileConnectResolver {

    public enum Subtile {
        UNCONNECTED, CENTER, CORNER, EDGE, T_CONNECTION, END_PIECE, OPEN, BROKEN
    }

    public record ConnectResult(Subtile subtile, int rotation) {}

    /** 4-bit mask from same-id cardinal neighbors. */
    public static int neighborMaskSameId(
        MapGrid grid, int x, int y, String terrainId
    );

    public static ConnectResult rotationAndSubtile(int mask);

    /** Drawable tile id, or original id when no subtile art. */
    public static String resolveTerrainTileId(
        LoadedTileset tileset,
        MapGrid grid,
        int x,
        int y,
        String terrainId
    );
}
```

Keep pure/static — no LibGDX types — for unit tests.

---

## Integration in `MapEditorScreen`

Today (simplified):

```text
drawCellTerrain(terrainId, x, y):
    tile ← tileset.findTile(terrainId)
    draw layers from TileSpriteResolver
```

After R1:

```text
drawCellTerrain(terrainId, x, y):
    drawableId ← MultitileConnectResolver.resolveTerrainTileId(tileset, grid, x, y, terrainId)
    drawableId ← TileLooksLikeResolver.resolve(drawableId, …)   // R2 order: connect then looks_like
    tile ← tileset.findTile(drawableId).orElse(findTile("unknown"))
    …
```

**Order:** multitile connect on **game** id, then `looks_like` on resolved drawable id (see [06](./06-looks-like-draw-fallback.md)).

Optional compile-time flag `enableMultitileConnect` (default true) for A/B in tests.

---

## Edge cases

| Case | Behavior |
| --- | --- |
| Out-of-grid neighbor | Treat as non-matching (mask bit 0) |
| Parent multitile but subtile id missing | Fall back to parent id sprites |
| Non-multitile id | No mask; unchanged |
| `t_null` / empty | Skip draw (existing) |
| Mapgen rotation | Grid already rotated; mask uses stored ids |
| Diagonal neighbors | Ignored (BN cardinal only for this path) |

---

## Performance

For visible region only (existing culling in [03](./03-render-bridge.md)):

- O(visible cells × 4) neighbor reads — fine for 24×24 and stitched 48×48 floors
- Cache mask per cell per frame optional if profiling shows hotspot

---

## Inputs

- `MapGrid`, cell `(x,y)`, `LoadedTileset`

## Outputs

- Resolved tile id string for `TileSpriteResolver`

## Failure modes

| Condition | Behavior |
| --- | --- |
| tileset null | Skip multitile; existing fallback |
| grid null | Use base id |

---

## Verification

1. **Unit:** `MultitileConnectResolverTest` — mask 0 → `unconnected`, 15 → `center`, 5 → `corner` rot 3
2. **Fixture grid:** 3×3 all `t_grass` → center cell resolves to `t_grass_center` when tileset defines subtiles
3. **Integration (manual):** Import house mapgen with RetroDays — exterior grass and interior walls show edges, not repeating center tile
4. **Regression:** Single-tile `t_dirt` unchanged

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.view.MultitileConnectResolverTest"
```

---

## Related

- [03 render bridge](./03-render-bridge.md) — draw loop
- [07b tile registration](../tileset-loader/07b-tile-registration.md) — subtile ids
- [mapgen-preview 07](../mapgen-preview/07-furniture-render.md) — furniture layer (no multitile yet)
