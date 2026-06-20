# 09 — Editor and rendering polish

Draw-time and UX improvements for **map editor** (and worldgen overmap view). Generation logic
stays in [mapgen-preview](../mapgen-preview/README.md) and [worldgen](./README.md).

**Status:** todo. **Canonical specs:** [map-editor v2](../map-editor/v2-implementation-plan.md).

---

## Purpose

Generated submaps and hand-painted grids should **look like BN in-game** when a tileset is loaded.
Generation can be correct while rendering still shows wrong sprites (base id only, no autoconnect).

This file is the **worldgen-track index**. Detailed algorithms, APIs, and verification live in
`docs/map-editor/` units 05–10.

---

## PR slices (canonical)

| ID | Unit | Topic |
| --- | --- | --- |
| **R1** | [05-multitile-autoconnect](../map-editor/05-multitile-autoconnect.md) | Terrain neighbor mask → subtile id |
| **R2** | [06-looks-like-draw-fallback](../map-editor/06-looks-like-draw-fallback.md) | `looks_like` chain at draw |
| **M5** | [07-furniture-paint](../map-editor/07-furniture-paint.md) | Furniture brush + palette mode |
| **M6** | [08-debug-overlays](../map-editor/08-debug-overlays.md) | `SpawnMarker` overlay |
| **M7** | [09-z-roof-transparency](../map-editor/09-z-roof-transparency.md) | Multi-floor cutaway (optional) |
| **R3** | [10-overmap-debug-view](../map-editor/10-overmap-debug-view.md) | OMT grid UI (**requires W2**) |

**Order:** R1 → R2 → M5 → M6; R3 with [W2](./03-mini-overmap-grid.md); M7 as needed.

---

## Feature summary

| Feature | BN | Target doc |
| --- | --- | --- |
| Terrain multitile | `get_rotation_and_subtile` | [05](../map-editor/05-multitile-autoconnect.md) |
| `connects_to` groups | `get_connect_values` | 05 § R1.1 |
| `looks_like` at draw | `find_tile_looks_like` | [06](../map-editor/06-looks-like-draw-fallback.md) |
| Furniture paint | — | [07](../map-editor/07-furniture-paint.md) |
| Spawn / field overlays | Debug | [08](../map-editor/08-debug-overlays.md) |
| Overmap sym + visit | `overmap` UI | [10](../map-editor/10-overmap-debug-view.md) |

**Not** mapgen loader scope — renderer walks `MapGrid` neighbors.

---

## Verification (smoke)

1. Wall corners render connected tiles with RetroDays after **R1**
2. `t_region_*` falls back via `looks_like` after **R2**
3. Overmap mode shows distinct OMT syms after **R3** + W2

See each unit doc for JUnit checklists.

---

## Related

- [MAP_EDITOR.md](../MAP_EDITOR.md)
- [map-editor/v2-implementation-plan.md](../map-editor/v2-implementation-plan.md)
- [mapgen-preview 07](../mapgen-preview/07-furniture-render.md) — furniture display (done)
- [mapgen-preview 08 § Rendering](../mapgen-preview/08-v2-parity-roadmap.md)
