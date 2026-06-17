# 08 — v2 parity roadmap

Deferred BN mapgen features — ordered for future work after P1–P4. Not required for first
visible building preview.

---

## Purpose

Track intentional v1 gaps so implementers do not expand scope mid-PR. Each row notes BN
behavior and suggested nextgen approach.

---

## Palette system

| Feature | BN behavior | Suggested v2 |
| --- | --- | --- |
| Weighted value arrays | Random pick weighted by int | `Random` with seed from preview options |
| `parameters` + distributions | Roll at mapgen time | `MapgenParameterRoll` stub or fixed seed |
| Palette `palettes: [ "parent" ]` | `mapgen_palette::add` merges parents | Recursive load + cycle detection |
| `translate` section | Remap chars | `CharRemap` pass before merge |
| `nested` in palette | Places sub-mapgen | Delegate to nested runner |

---

## Mapgen object

| Feature | BN behavior | Suggested v2 |
| --- | --- | --- |
| `set` array | Bulk ter/furn/field ops | `SetmapApplier` on `MapGrid` |
| `place_terrain` / `place_furniture` | Rectangles with chance | After rows pass |
| `place_items`, `place_monsters` | Spawn entities | Needs item/monster loaders |
| `predecessor_mapgen` | Run another OMT mapgen first | Run two defs sequentially |
| `rotation` | Rotate submap 0–3 | Rotate `MapGrid` or draw transform |
| `mapgensize` | Nested chunk dimensions | Required for nested |
| `flags`, `label` | Metadata | Store on `JsonMapgenDefinition` |

---

## Registry and selection

| Feature | BN behavior | Suggested v2 |
| --- | --- | --- |
| `oter_mapgen` weighted pick | `weight` + `disabled` on mapgen | `MapgenPicker` simulates BN |
| `om_terrain` match | Multiple mapgens per OMT | Catalog already indexes |
| `method: builtin` | C++ `mapgen_function_builtin` | Port select generators or JNI — unlikely |
| `method: lua` | `mapgen_function_lua` | Requires catalua port |

---

## World scale

| Feature | Notes |
| --- | --- |
| Overmap generation | New spec tree `docs/worldgen/` |
| `overmap_terrain` loader | Prerequisite for OMT typing |
| Submap buffer 24×24×Z | **P5–P6:** `MapVolume` + stitch without full worldgen — [09](./09-building-bundles-overview.md) |
| Regional `t_region_*` resolve | Needs `region_settings` / region id |

---

## Rendering

| Feature | Notes |
| --- | --- |
| Multitile autoconnect | Draw-time in `TileSpriteResolver` |
| `looks_like` chains | Game data + gfx |
| Field / trap overlays | Separate layer |
| Roof transparency | Z and `t_open_air` |

---

## Verification

1. Every v1 “deferred” row in units 03–05 appears here or is explicitly permanent cut
2. No v2 item blocks P1–P4 implementation
