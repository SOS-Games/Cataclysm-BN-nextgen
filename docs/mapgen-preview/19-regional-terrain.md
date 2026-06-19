# 19 — Regional terrain resolve

Resolve **`t_region_*`** and **`f_region_*`** ids to concrete terrain/furniture for preview.

**Status:** todo (P11). See [v2-implementation-plan](./v2-implementation-plan.md).

---

## Purpose

BN mapgen references **regional aliases** resolved at generate time from the submap’s region
settings (forest, desert, default, …):

```json
"fill_ter": "t_region_groundcover",
"terrain": {
  "4": "t_region_groundcover",
  ",": "t_region_groundcover_barren"
}
```

Arcana entrance/surface mapgens use `4` and `,` for outdoor ground — ids stay literal `t_region_*`
in v1 → tileset may not find sprites.

---

## BN behavior

```text
resolve_regional_terrain_and_furniture(mapgendata dat):
    for p in dat.m.points_on_zlevel():
        tid_after ← dat.region.region_terrain_and_furniture.resolve(tid_before)
        if changed: dat.m.ter_set(p, tid_after)
        fid_after ← resolve(furn_before)
        if changed: dat.m.furn_set(p, fid_after)
```

Called **after** setmap + objects.apply, **before** final submap rotation.

Source: `resolve_regional_terrain_and_furniture` — `src/mapgen_functions.cpp`.

---

## Region settings data

BN core: `data/json/region_settings.json` (and mod overrides). Structure (simplified):

```json
{
  "type": "region_settings",
  "id": "default",
  "terrain": {
    "t_region_groundcover": "t_grass",
    "t_region_groundcover_barren": "t_dirt",
    "t_region_groundcover_swamp": "t_moss"
  },
  "furniture": {
    "f_region_forest": "f_null"
  }
}
```

Multiple region ids (`forest`, `desert`, …) exist; worldgen picks one per overmap. Preview uses
a **single selected region** for all cells.

---

## Preview algorithm

### Load context

```text
RegionContext.load(dataRoots, modIds, regionId):
    merge region_settings from mods in order
    pick object where id == regionId (default "default")
    build Map<String,String> terrainAliases
    build Map<String,String> furnitureAliases
```

Lazy-load once in `MapgenPreviewService` or per `ensureLoaded`.

### Resolve pass

```text
RegionalTerrainResolver.applyToGrid(grid, ctx, warnings):
    for y in 0 .. h-1:
        for x in 0 .. w-1:
            cell ← grid.get(x,y)
            ter ← resolveTer(cell.terrainId, ctx)
            furn ← resolveFurn(cell.furnitureId, ctx)
            if ter != cell.terrainId: grid.setTerrain(x,y, ter)
            if furn changed: grid.setFurniture(x,y, furn)

resolveTer(id, ctx):
    if !id.startsWith("t_region_"): return id
    return ctx.terrainAliases.getOrDefault(id, id)  // passthrough + warn if missing
```

Also resolve **during** palette merge? No — BN resolves **post-placement** on final ids. Chars
mapping to `t_region_groundcover` in rows are resolved in this pass.

### `fill_ter`

Grid init uses raw `fill_ter` id — regional pass updates those cells too when iterated.

---

## `JsonMapgenRunOptions`

```java
private String previewRegionId = "default";

public String getPreviewRegionId() { return previewRegionId; }
```

Future: editor dropdown populated from discovered region ids in loaded data.

---

## Planned Java types

```java
package io.gdx.cdda.bn.nextgen.mapgen.region;

public final class RegionContext {
    public static RegionContext load(
        List<Path> dataRoots,
        List<String> modIds,
        String regionId,
        List<String> warnings
    ) throws IOException;

    public String resolveTerrain(String id);
    public String resolveFurniture(String id);
}

public final class RegionalTerrainResolver {
    public static void applyToGrid(
        MapGrid grid,
        RegionContext ctx,
        List<String> warnings
    );
}
```

Package `mapgen.region` keeps game-data concerns separate from `gamedata` loader until G6 merges
region_settings into `LoadedGameData`.

---

## Interaction with [16](./16-palette-inheritance.md)

Weighted palette may pick `t_region_shrub` — still resolved here. Order:

```text
rows/set/place → cells hold t_region_* → regional pass → concrete t_grass → rotation
```

---

## Test fixtures

### Minimal region pack (test resources)

```json
// mapgen-fixtures/json/region_settings/test_region.json
{
  "type": "region_settings",
  "id": "test_region",
  "terrain": {
    "t_region_groundcover": "t_grass",
    "t_region_groundcover_barren": "t_dirt"
  }
}
```

### `test_region_fill.json`

```json
"object": {
  "fill_ter": "t_region_groundcover",
  "rows": [ "........................", "... 24 lines ..." ]
}
```

Run with `previewRegionId = "test_region"` → all cells `t_grass`.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Resolve pass | `mapgen_functions.cpp` — `resolve_regional_terrain_and_furniture` |
| Region class | `region` / `region_settings` loaders in BN |
| Data | `data/json/region_settings.json` |

---

## Failure modes

| Condition | Behavior |
| --- | --- |
| No region_settings loaded | Passthrough; warn once per unique alias |
| Unknown `previewRegionId` | Fall back to `default` |
| Alias maps to missing ter id | `validateIds` warning; keep mapped string |
| Mod overrides region | Later mod wins per merge order |

---

## Verification

1. Fixture region + fill_ter → concrete `t_grass`
2. Per-cell `t_region_groundcover_barren` in rows → `t_dirt`
3. Furniture alias `f_region_*` if fixture includes
4. Integration: Arcana mapgen after resolve — outdoor chars drawable with RetroDays
5. Switching `previewRegionId` changes output (when multiple regions loaded)
6. Regional pass runs **after** setmap (set id `t_region_*` also resolved)
