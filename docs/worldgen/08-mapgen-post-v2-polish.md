# 08 — Mapgen post-v2 polish

Runner and catalog improvements **after** mapgen preview v2 (P8–P15). Parallel to worldgen W1+.

**Status:** done. Not a worldgen PR — improves both preview and visit-tile quality.

---

## Purpose

v2 closed the main JSON mapgen pipeline gaps. Remaining BN differences affect building imports
and future submap visits.

---

## Palette and parameters

| Feature | BN | Target | Notes |
| --- | --- | --- | --- |
| `parameters` + distributions | Roll at mapgen time | v2.1 | [16-palette](../mapgen-preview/16-palette-inheritance.md) stub |
| Palette `translate` | Char remap | Done P10 | verify edge cases |
| Nested in **palette** format | Char → nested chunk | Uses [21-nested](../mapgen-preview/21-nested-update-mapgen.md) | |

---

## Mapgen selection (preview UI)

| Feature | BN | Target doc |
| --- | --- | --- |
| Weighted `oter_mapgen` pick | Random at worldgen | [04-visit](./04-visit-tile-mapgen.md) W3 |
| Picker simulates weights | N/A in BN UI | Optional: "roll variant" button in mapgen picker |
| `disabled: true` on mapgen | Skip | Filter in catalog |

---

## Nested neighbor checks

| Feature | BN | Target |
| --- | --- | --- |
| `neighbors` on nested | Cardinal OMT type match | Pass `VisitContext` from W6 |
| `joins` on nested | Join id match | W6 |
| `connections` on nested | Road connection match | W5 |

v1 nested runner ignores these (always uses `chunks`). v2.1: skip chunk when checks fail.

---

## Unsupported methods

| Method | Preview behavior |
| --- | --- |
| `builtin` | Warn; skip visit |
| `lua` | Warn; skip — requires catalua port |

Document per-id fallbacks in catalog warnings summary.

---

## Metadata on definitions

Optional fields on `JsonMapgenDefinition`:

| Field | Use |
| --- | --- |
| `flags` | Filter picker |
| `label` | Display name in UI |

Low priority.

---

## Suggested PR slices

| ID | Scope |
| --- | --- |
| **V2.1a** | Palette `parameters` minimal (int/string) |
| **V2.1b** | Nested neighbor skip when context fails |
| **V2.1c** | Mapgen picker "random variant" |

---

## Verification

1. Arcana palette params do not crash runner
2. Lab nested with wrong neighbor skips chunk (fixture)
3. Builtin-only OMT warns once on visit
