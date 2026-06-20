# 09 — Z-level and roof transparency

Improve **multi-floor** editing when `MapVolume` has more than one z-level.

**Status:** done (**M7**). Floor switching exists ([mapgen-preview 11](../mapgen-preview/11-map-volume-and-floors.md)).

---

## Purpose

Building imports stack floors (`house_09` ground + basement + roof). The editor shows one
active floor at a time via **`[` / `]`** (or `/`). Upper floors often use `t_open_air` and roof
terrain that **obscures** lower floors in BN's 3D view.

M7 adds optional **cutaway** rendering so lower floors remain readable while editing stacks —
without full BN z-transparency simulation.

---

## BN reference (simplified)

BN skips or alpha-blends tiles with flags like `TRANSPARENT`, `NO_FLOOR`, `Z_TRANSPARENT` when
viewing from above. Full rules need flag parsing on `TerrainDefinition` (already loaded in G2).

**Source:** `src/cata_tiles.cpp` — map draw z-order and transparency checks.

---

## M7 v1 policy (editor-only)

| Mode | Behavior |
| --- | --- |
| **Solid** (default) | Current floor only — today |
| **Cutaway** | When active z < top floor, render active floor fully; for cells on floors **above** active z within same column, skip opaque roof ter ids OR draw at 25% alpha |

Toggle: **`T`** — "transparent upper floors" (name TBD to avoid clash with tools).

### Heuristic roof ids

Without full flag simulation, treat as transparent when:

- `terrainId` equals `t_open_air`, or
- `flags` contains `TRANSPARENT` / `INDOORS` roof markers (table in impl), or
- id suffix `_roof` on non-active floor in cutaway mode

Refine with integration tests against `house_09` roof floor.

---

## Scope

### In scope

- Cutaway mode toggle + HUD
- Flag-based skip list from `TerrainDefinition.getFlags()`
- Performance: only when `mapVolume.floorCount() > 1`

### Out of scope

- Simultaneous multi-z ghost layers (see all floors at once)
- Furniture z-order between floors
- BN save z-level metadata

---

## Inputs

- `MapVolume`, active z, `TerrainRegistry`

## Outputs

- Modified draw visibility per cell

---

## Verification

1. Import whole special with 3 floors → cutaway shows ground floor layout when on z=0
2. Solid mode unchanged vs today
3. Single-floor mapgen → toggle no-op

---

## Related

- [mapgen-preview 11](../mapgen-preview/11-map-volume-and-floors.md)
- [03 render bridge](./03-render-bridge.md)
- [worldgen 09](../worldgen/09-editor-rendering-polish.md) — z UI when W3 lands
