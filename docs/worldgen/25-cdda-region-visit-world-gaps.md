# 25 — CDDA region, visit, and world gaps

Gaps outside **overmap layout placement** — what region JSON we consume, what happens at
**visit** time, and what **world / persistence** features BN has that nextgen defers.

**Status:** draft

**Parent:** [23-cdda-parity-overview](./23-cdda-parity-overview.md) · **Layout gaps:**
[24](./24-cdda-layout-gaps.md)

---

## Purpose

A sparse overmap is mostly [24](./24-cdda-layout-gaps.md). This doc covers:

- Which `regional_map_settings.json` fields affect nextgen today
- Why click-to-visit can be good while the overmap still looks wrong
- Exploration, coords, save format — W15/W16 and beyond

---

## Region settings consumption

Source: `data/json/regional_map_settings.json` (`type: region_settings`).

Loader: `worldgen/region/RegionSettingsLoader.java` → `RegionSettingsRegistry`.

### Consumed today

| JSON block / field | Used by | Since |
| --- | --- | --- |
| `overmap_forest_settings` | `BaseTerrainFiller`, `ThickForestGenerator` | W9, W14c |
| `overmap_lake_settings` | `LakeGenerator` | W11b |
| `overmap_terrain_settings` | Swamp/beach/thick thresholds (W14c) | W14c |
| `city.houses` | `CityPlacer` building weights | W9 |
| `city.city_size`, `city.city_spacing` | `CitySitePicker`, `CityPlacer` | W14b |
| `overmap_special_settings` | `RegionSpecialPlacer` | W14a |

### Not consumed (layout-relevant)

| JSON block / field | BN effect | Notes |
| --- | --- | --- |
| `city.shops` | Shop OMTs in cities | → [24 § Cities](./24-cdda-layout-gaps.md#cities-place_cities) |
| `city.parks` | Parks in cities | Same |
| `city.finales` | Large city landmarks | Same |
| `forest_trail` | Trails + trailheads | → [24 § Roads / trails](./24-cdda-layout-gaps.md#roads-place_roads) |
| `map_extras` | Submap extras at visit | Gameplay; not overmap |
| `weather`, `field_coverage` | Simulation | Out of scope |

### Not consumed (gameplay)

| JSON block | BN effect |
| --- | --- |
| `default_oter` | Fallback OMT — nextgen uses `OvermapGenerateOptions.fieldId` |
| Monster / item region hooks | Spawn tables |
| Faction region data | Camps, diplomacy |

---

## Editor region selection gap

| Topic | BN | Nextgen |
| --- | --- | --- |
| Active region | World / scenario choice | `OvermapGenerateOptions.regionId` (default `"default"`) |
| UI picker | In-game world setup | **Missing** — no region dropdown in map editor |
| Data path | Loaded at game start | `WorldgenPreviewService` loads registry when BN `data/` roots resolve |

**Workaround for testing:** `OvermapGenerateOptions.forSize(w, h).withRegionId("your_region")`
in code, or extend `MapEditorScreen` regenerate path to pass selected region id.

Exported overmap JSON includes `regionId` for reproducibility (`OvermapGridExporter`).

---

## Visit time (click OMT → submap)

### BN

```text
draw_map / oter_mapgen
  → pick weighted mapgen for OMT id + z + context
  → apply neighbors, joins, connections
  → fill 2×2 submap buffer per OMT (mapbuffer)
  → builtin + Lua + JSON generators
```

### Nextgen (post-W13)

```text
WorldgenPreviewService.visit(overmap, x, y, z)
  → PlacedBuildingIndex.findAt(x, y)
  → if building: MapVolumeBuilder + OmtStitchComposer (joins + connections)
  → else: MapgenPicker → JsonMapgenRunner (JSON only)
  → single 24×24 MapGrid (+ MapVolume for buildings)
  → SubmapCache by seed + cell + z
```

### Visit gap matrix

| Feature | BN | Nextgen | Status |
| --- | --- | --- | --- |
| JSON mapgen | Yes | Yes | **Done** |
| Weighted pick | Yes | `MapgenPicker` | **Done** |
| Multi-z (roof/basement) | Yes | `ZLevelResolver` (W8) | **Done** |
| Building volume stitch | Yes | `MapVolumeBuilder` (W7) | **Done** |
| Active joins | Yes | W11d + W13 placement context | **Done** |
| `overmap_connection` at edge | Yes | `OvermapConnectionResolver` (W13) | **Done** |
| Nested `connections` checks | Yes | `NestedContextChecker` (W13) | **Done** |
| 2×2 submaps per OMT (`mapbuffer`) | Yes | One 24×24 preview grid | **Missing** (W13b optional) |
| Builtin mapgen | Yes | Warn + skip | **Missing** |
| Lua mapgen | Yes | Warn + skip | **Missing** |
| `map_extras` at visit | Yes | Not implemented | **Out of scope** v3 |

**Important:** Visit fidelity is **much closer** to BN than layout fidelity. Fixing W17 city/road
layout does not require mapbuffer unless corner submap alignment still fails after W13a.

See [19-visit-mapbuffer-fidelity](./19-visit-mapbuffer-fidelity.md) for W13 scope.

---

## Exploration and coordinates (W15 — todo)

| Feature | BN | Nextgen v2/v3 | Target |
| --- | --- | --- | --- |
| Seen OMT flags | `overmap::seen` | None | W15 |
| Explored / visited | `overmap::explored` | None | W15 |
| World position | Overmap + OMT + z | Cell + z in editor | W15 `WorldCoord` |
| Avatar movement between OMTs | Full game | Click visit only | W15b optional |
| Cache invalidation on regen | N/A in editor | LRU `SubmapCache` | W15 policy |

Unit spec: [21-exploration-and-world-coords](./21-exploration-and-world-coords.md).

---

## World model and persistence

| Feature | BN | Nextgen |
| --- | --- | --- |
| `overmapbuffer` (many overmaps) | Yes | Single `OvermapGrid` |
| Save format | `.sav2` binary | None |
| Exploration + layout persist | Yes | W16 deferred |
| Submap cache on disk | Optional | Not planned v3 |

Unit spec: [22-world-persistence](./22-world-persistence.md) — **deferred** until W15.

---

## Game systems (intentionally out of scope)

These affect BN worlds but not the map editor preview mandate:

| System | BN location | Nextgen |
| --- | --- | --- |
| `place_mongroups` | `overmap::generate` tail | — |
| `place_radios` | Same | — |
| NPCs, factions, missions | Simulation | — |
| Items / monsters on map | `map` / spawn | Spawn overlay from game data only |
| Turn loop, avatar stats | `game` | — |

---

## Tier B checklist (visit + region UX)

| Item | Effort | User win |
| --- | --- | --- |
| Region id picker in editor | Small | Compare BN regions without code change |
| Document builtin skip list | Small | Clear warnings when OMT lacks JSON mapgen |
| W13b mapbuffer (if needed) | Large | Interior OMT corners match BN 2×2 model |
| Builtin mapgen subset | Large | Common oters work without JSON equivalents |

---

## Java reference (visit path)

| Concern | Class |
| --- | --- |
| Visit entry | `worldgen/WorldgenPreviewService.java` |
| Submap gen | `worldgen/submap/SubmapGenerator.java` |
| Building stitch | `mapgen/compose/MapVolumeBuilder.java`, `OmtStitchComposer.java` |
| Connections | `worldgen/connection/OvermapConnectionResolver.java` |
| Placement lookup | `worldgen/placement/PlacedBuildingIndex.java` |
| Cache | `worldgen/submap/SubmapCache.java` |
| Editor wiring | `view/MapEditorScreen.java` |

---

## Verification

1. Region table lists consumed vs missing JSON blocks with loader class names
2. Visit matrix references W13/W8 unit docs
3. W15/W16 cross-linked without duplicating their full specs
4. Editor `regionId` default documented with workaround
