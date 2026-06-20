# Implementation plan — map editor v2 (rendering + UX)

Agent-oriented guide for **post–mapgen-v2** editor work. Spec units: [README](./README.md) units 05–10.

**v1 milestones M1–M4 are done.** Mapgen preview (P1–P15) feeds the editor via import; generation
logic stays in [mapgen-preview](../mapgen-preview/README.md).

---

## Goal

Make imported and hand-painted grids **look like BN in-game** and support basic editing beyond
terrain-only paint:

1. **Draw-time** multitile autoconnect and `looks_like` fallback (today: base tile id only)
2. **Furniture** paint brush (today: view-only toggle `F`)
3. **Debug overlays** for mapgen spawns (today: metadata only, not drawn)
4. **Overmap debug view** when worldgen W2 lands (OMT sym + click-to-visit)

---

## Dependencies

| Upstream | Provides |
| --- | --- |
| [Tileset loader 07b](../tileset-loader/07b-tile-registration.md) | Multitile parents + `{id}_{subid}` subtiles, `availableSubtiles` |
| [Game data G2–G3](../game-data-loader/README.md) | `TerrainDefinition` / `FurnitureDefinition`, `looks_like`, flags |
| [Mapgen preview P13b](../mapgen-preview/18-place-spawners.md) | `SpawnMarker` collection |
| [Mapgen preview 07](../mapgen-preview/07-furniture-render.md) | Furniture display (done) |
| [Worldgen W1–W2](../worldgen/README.md) | `OvermapTerrainRegistry`, `OvermapGrid` for R3 |

---

## Deliverables

| Class / module | Unit | PR |
| --- | --- | --- |
| `MultitileConnectResolver` | [05](./05-multitile-autoconnect.md) | **R1** |
| `TileLooksLikeResolver` | [06](./06-looks-like-draw-fallback.md) | **R2** |
| `TileSpriteResolver` extensions | 05, 06 | R1, R2 |
| `MapPalettePanel` furniture mode | [07](./07-furniture-paint.md) | **M5** |
| `MapEditorScreen` overlay pass | [08](./08-debug-overlays.md) | **M6** |
| Roof / z transparency polish | [09](./09-z-roof-transparency.md) | **M7** — done |
| `OvermapEditorMode` / mode switch | [10](./10-overmap-debug-view.md) | **R3** (with **W2**) |

---

## PR slices (canonical)

| PR | Scope | Done when |
| --- | --- | --- |
| **R1** | Terrain multitile autoconnect at draw | done | `MultitileConnectResolver`, `MultitileConnectResolverTest` |
| **R2** | `looks_like` chain at draw (terrain + furniture) | done | `TileLooksLikeResolver`, palette paintable check |
| **M5** | Furniture paint brush + palette mode | Paint/eyedropper `furnitureId`; map JSON round-trip |
| **M6** | Spawn marker overlay toggle | Dots/labels for `SpawnMarker` from last mapgen import |
| **M7** | Z-floor roof transparency (optional) | done | `ZCutawayPolicy`, `[T]` toggle, upper-floor ghost pass |
| **R3** | Overmap debug view in editor | done | W1/W2 loaders, `[M]` mode, Enter visit via `SubmapGenerator` |

**Suggested order:**

```text
R1 → R2 → M5 → M6
R3 after worldgen W2 (+ W3 for visit)
M7 when MapVolume multi-floor editing is painful without it
```

R1 and R2 are independent of worldgen and give the biggest visual win on mapgen import.

---

## Package layout

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/
  view/
    TileSpriteResolver.java          # extend: resolve with grid context
    MultitileConnectResolver.java    # R1 — neighbor mask → subtile id + rotation
    TileLooksLikeResolver.java       # R2 — game id → drawable tile id
    MapEditorScreen.java             # wire context; overlays; mode switch
    MapPalettePanel.java             # M5 furniture rows
  gamedata/
    connect/                         # R1.1 optional: connects_to group index
  mapgen/json/SpawnMarker.java       # existing — M6 consumes
  worldgen/                          # R3 — see worldgen plan
```

---

## What stays out of scope

| Topic | Where |
| --- | --- |
| Mapgen runner changes | [mapgen-preview](../mapgen-preview/README.md) |
| Full BN draw stack (lighting, visibility, z-transparency rules) | Future game client |
| Item sprites on ground | [G6+](../worldgen/10-game-data-g6-plus.md) |
| Seasonal tile variants at draw | Tileset loader follow-up |
| Scene2D UI skin | Text/batch UI acceptable |

---

## Verification (program-wide)

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.view.*"
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.map.*"
```

Manual: import `house_09` or lab mapgen → walls/grass edges look connected; toggle spawn overlay.

---

## Related

- [MAP_EDITOR.md](../MAP_EDITOR.md) — controls and run guide
- [worldgen/09-editor-rendering-polish.md](../worldgen/09-editor-rendering-polish.md) — parallel index (links here)
- [mapgen-preview/08-v2-parity-roadmap.md](../mapgen-preview/08-v2-parity-roadmap.md) — rendering rows
