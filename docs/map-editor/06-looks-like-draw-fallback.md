# 06 — `looks_like` draw fallback

Resolve drawable tile ids when gfx is registered under a **chain target** id, not the game
object id on the cell.

**Status:** done (**R2**). See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

BN game JSON often uses ids without dedicated tile entries:

- Regional terrain: `t_region_grass`, `t_region_groundcover`
- Mod placeholders copying base types via `looks_like`
- Furniture with `looks_like: f_chair` when only `f_chair` is tiled

Load-time validation already walks chains for reports
([game-data 10](../game-data-loader/10-post-load-validation.md),
[tileset 09](../tileset-loader/09-post-load-validation.md)). The editor **draw path** still
looks up exact cell ids only → missing art or `unknown` even when a parent tile exists.

R2 adds draw-time resolution parallel to BN `find_tile_looks_like`.

---

## BN reference

```text
find_tile_looks_like(id, category, jump_limit=10):
    if find_tile(category_prefix + id): return that tile
    obj ← game object for id
    while obj.has looks_like and jumps < limit:
        obj ← resolve looks_like target
        if find_tile(category_prefix + obj.id): return tile
    return not found
```

Category prefixes: terrain → `t_` (tile id is full string `t_dirt`), furniture → `f_*`, items →
`itype` ids in tileset.

**Source:** `src/cata_tiles.cpp` — `find_tile_looks_like`, `do_tile_loading_report`.

---

## `TileLooksLikeResolver` API

```java
public final class TileLooksLikeResolver {

    private static final int DEFAULT_JUMP_LIMIT = 10;

    /** First id in chain that has drawable art in tileset (or exact match). */
    public static String resolveTerrainId(
        String terrainId,
        LoadedTileset tileset,
        TerrainRegistry terrains
    );

    public static String resolveFurnitureId(
        String furnitureId,
        LoadedTileset tileset,
        FurnitureRegistry furniture
    );

    /** Shared walker with cycle detection. */
    public static String resolveChain(
        String startId,
        LoadedTileset tileset,
        Function<String, String> nextLooksLike
    );
}
```

### Algorithm

```text
resolveChain(startId, tileset, nextLooksLike):
    if hasDrawableArt(tileset, startId):
        return startId

    seen ← { startId }
    current ← startId
    for jump in 1..DEFAULT_JUMP_LIMIT:
        target ← nextLooksLike(current)
        if target empty or target in seen: break
        seen.add(target)
        if hasDrawableArt(tileset, target):
            return target
        current ← target

    return startId   // caller may fall back to "unknown"
```

Use `TileSpriteResolver.hasDrawableArt` — same rule as palette filtering.

---

## Integration order

Per cell in `MapEditorScreen`:

```text
1. terrainId ← cell.terrainId (game id — used for eyedropper / save)
2. connectId ← resolveTerrainConnectId(terrainId)   // full game-data looks_like chain
3. multitile ← MultitileConnectResolver on connectId (neighbors compare connect ids)
4. drawableId ← resolveTerrainDrawId(multitile result)   // first chain target with tileset art
5. draw terrain sprites

6. furnitureId ← resolveFurnitureDrawId(cell.furnitureId, …)
7. draw furniture (if layer on)
```

**Multitile before looks_like:** neighbor matching must use **game** terrain ids from `MapGrid`,
not resolved gfx ids. Connection subtiles (`t_wall_corner`) usually have direct art — chain
walk stops immediately.

---

## Palette interaction

| Surface | Policy |
| --- | --- |
| Terrain palette | Still lists **game** ids (paintable-only = chain has art somewhere) |
| Canvas | Resolved id for display |
| Eyedropper | Stores **game** id from cell (not resolved gfx id) |

Extend `hasDrawableArtForTerrain(TerrainRegistry, tileset, terId)` for palette:

```text
hasDrawableArtForTerrain:
    return hasDrawableArt(tileset, terId)
        || hasDrawableArt(tileset, resolveTerrainId(terId, tileset, terrains))
```

Optional M5 follow-up for furniture palette rows.

---

## Inputs

- Cell id string, `LoadedTileset`, `TerrainRegistry` / `FurnitureRegistry`

## Outputs

- Drawable tile id for `TileSpriteResolver`

## Failure modes

| Condition | Behavior |
| --- | --- |
| Cycle in `looks_like` | Stop at cycle; use last non-art id → `unknown` on canvas |
| Jump limit exceeded | Return original id |
| Null registry | Skip chain; exact id only |

---

## Verification

1. **Unit:** `A → B → C`, art only on `C` → resolve `A` to `C`
2. **Unit:** cycle `A → B → A` → no infinite loop
3. **Fixture:** synthetic terrain with `looks_like: t_dirt` and only `t_dirt` tiled → regional id renders dirt
4. **Integration:** mapgen with `t_region_*` fills shows ground cover with RetroDays (manual)
5. **Palette:** regional id appears in list when chain target has art

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.view.TileLooksLikeResolverTest"
```

---

## Related

- [05 multitile autoconnect](./05-multitile-autoconnect.md)
- [02 palette](./02-palette-and-paint.md) — paintable-only policy
- [game-data 05a](../game-data-loader/05a-terrain-config.md) — `looks_like` field
