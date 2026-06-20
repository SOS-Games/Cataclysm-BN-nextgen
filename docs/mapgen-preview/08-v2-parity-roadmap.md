# 08 — v2 parity roadmap

Deferred BN mapgen features — ordered for future work after P1–P7c. Not required for first
visible building preview.

**Implementation guide:** [v2-implementation-plan.md](./v2-implementation-plan.md)  
**Unit specs:** units [14](./14-mod-scan-paths.md)–[21](./21-nested-update-mapgen.md) below.

---

## Purpose

Track intentional v1 gaps so implementers do not expand scope mid-PR. Each row links to a unit
doc with algorithms, Java types, and verification.

---

## Palette system

| Feature | BN behavior | Unit / approach |
| --- | --- | --- |
| Weighted value arrays | Random pick weighted by int | [16](./16-palette-inheritance.md) — seeded `Random` |
| `parameters` + distributions | Roll at mapgen time | [16](./16-palette-inheritance.md) — v2.1 stub |
| Palette `palettes: [ "parent" ]` | `mapgen_palette::add` merges parents | [16](./16-palette-inheritance.md) |
| `translate` section | Remap chars | [16](./16-palette-inheritance.md) |
| `nested` in palette | Places sub-mapgen | [21](./21-nested-update-mapgen.md) |

---

## Mapgen object

| Feature | BN behavior | Unit / approach |
| --- | --- | --- |
| `set` array | Bulk ter/furn/field ops | [15](./15-setmap-applier.md) — `SetmapApplier` |
| `place_terrain` / `place_furniture` | Rectangles with chance | [18](./18-place-spawners.md) |
| `place_items`, `place_monsters` | Spawn entities | [18](./18-place-spawners.md) — P13b |
| `predecessor_mapgen` | Run another OMT mapgen first | [17](./17-predecessor-mapgen.md) |
| `rotation` | Rotate submap 0–3 | [20](./20-mapgen-rotation.md) |
| `mapgensize` | Nested chunk dimensions | [21](./21-nested-update-mapgen.md) |
| `flags`, `label` | Metadata | Store on `JsonMapgenDefinition` (optional) |

---

## Scan paths

| Feature | Notes | Unit |
| --- | --- | --- |
| `overmap_and_mapgen/` tree | Arcana-style mod layout | [14](./14-mod-scan-paths.md) — **done** (P8) |

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

| Feature | Notes | Unit |
| --- | --- | --- |
| Overmap generation | [docs/worldgen/](../worldgen/README.md) — W1–W6 | [WORLDGEN.md](../WORLDGEN.md) |
| `overmap_terrain` loader | Prerequisite for OMT typing | [02](../worldgen/02-overmap-terrain-loader.md) |
| Submap buffer 24×24×Z | P5–P7c `MapVolume` + stitch; visit-tile W3 | [09](./09-building-bundles-overview.md), [04](../worldgen/04-visit-tile-mapgen.md) |
| Regional `t_region_*` resolve | Needs `region_settings` | [19](./19-regional-terrain.md) — **done** P11 |

---

## Rendering (post–mapgen-v2)

| Feature | Notes | Unit |
| --- | --- | --- |
| Multitile autoconnect | Draw-time in `MultitileConnectResolver` | [map-editor 05](../map-editor/05-multitile-autoconnect.md) **R1** |
| `looks_like` chains | Draw-time in `TileLooksLikeResolver` | [map-editor 06](../map-editor/06-looks-like-draw-fallback.md) **R2** |
| Field / trap overlays | Debug layer | [map-editor 08](../map-editor/08-debug-overlays.md) **M6** |
| Roof transparency | Multi-floor cutaway | [map-editor 09](../map-editor/09-z-roof-transparency.md) **M7** |

**Plan:** [map-editor/v2-implementation-plan.md](../map-editor/v2-implementation-plan.md)

---

## Suggested PR order

See [v2-implementation-plan.md](./v2-implementation-plan.md):

P8 scan paths (done) → P9 setmap → P10 palettes → P11 regional → P12 predecessor → P13 place_* → P14 rotation → P15 nested

---

## Verification

1. Every v1 “deferred” row in units 03–05 appears here or in units 14–21
2. No v2 item blocks v1 maintenance
3. Each unit 14–21 has **Verification** checklist for JUnit / integration
