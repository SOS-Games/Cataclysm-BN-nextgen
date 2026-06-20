# 11 — Building bundle gaps

Remaining **building discovery** and **layout** gaps from [13-building-bundle-sources](../mapgen-preview/13-building-bundle-sources.md). Affects picker and W4 placement.

**Status:** todo. Parallel track.

---

## Purpose

Not every BN building is importable today. Closing gaps increases content available for:

- Mapgen picker / building import
- W4 city and special placement

---

## Gap inventory

| Gap | BN source | Current nextgen | Target |
| --- | --- | --- | --- |
| **Mutable `overmap_special`** | `overmap_mutable/` | Not in picker | [07](./07-mutable-specials-and-joins.md) W6 |
| **`copy-from` stubs** | Innawoods-style | No `overmaps` array | Resolve or skip with doc |
| **Mod-only bundles** | `regional_overlay.json`, etc. | P7b inferrer | Extend scanner paths |
| **Builtin-only OMT** | No json mapgen | Warn on import | Oter registry + W3 warn |
| **Weighted building pick** | BN city gen | N/A | W4 `CityPlacer` |

---

## Mutable specials in picker (post-W6)

After W6 assembler:

```text
BuildingBundleScanner:
    register mutable special id
    on pick: assemble layout for seed → OvermapGrid fragment → MapVolumeBuilder
```

Preview-only — not full world save.

---

## Scan path extensions

From [14-mod-scan-paths](../mapgen-preview/14-mod-scan-paths.md):

| Path | Content |
| --- | --- |
| `overmap_and_mapgen/` | Already P8 |
| `mods/*/overmap/*.json` | Non-standard bundle files |
| `worldgen/` mod dirs | DinoMod-style layouts |

Document each new root in scanner spec when added.

---

## `copy-from` resolution

BN inheritance on `city_building` / `overmap_special`:

| v1 | Resolve one level inline |
| v2 | Small `CopyFromResolver` shared with OMT loader |

Until resolved: log `"bundle X skipped: copy-from unresolved"`.

---

## Combined floor mapgen

Already handled by [CombinedFloorMapgenResolver](../mapgen-preview/12-omt-stitch-composer.md).
Ensure W4 placement uses **OMT ids from grid**, not combined json keys.

---

## Verification

1. Arcana mod buildings appear in picker (smoke list)
2. Mutable special id listed after W6
3. Unresolved `copy-from` bundle produces single clear warning

---

## Cross-links

| System | Doc |
| --- | --- |
| Worldgen placement | [05](./05-city-and-special-placement.md) |
| Bundle inventory | [13](../mapgen-preview/13-building-bundle-sources.md) |
| Whole specials | [13](../mapgen-preview/13-building-bundle-sources.md) P7c |
