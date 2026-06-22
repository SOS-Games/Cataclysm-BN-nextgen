# 21 — Exploration and world coordinates (W15)

Add **seen / visited** OMT state and a **world coordinate model** so the editor (and future client)
can treat the overmap as a space you move through — without save files yet.

**Status:** todo. See [v3-implementation-plan](./v3-implementation-plan.md).

**Prerequisite:** W13 visit quality acceptable on multitile corners (or W13a complete).

---

## Purpose

v2 overmap mode:

- Click OMT → visit → submap in editor
- `SubmapCache` LRU by `(seed, omtX, omtY, z)`
- No memory of which OMTs were viewed
- No avatar position — selection highlight only

v3 adds:

```text
WorldgenSession
  overmapGrid
  placementIndex
  explorationState   // seen[][] , visited[][]
  worldCoord         // current omx, omy, z
  seed, regionId
```

Persistence (W16) serialises this object later. W15 keeps it **in memory only**.

---

## User stories

| Story | W15 deliverable |
| --- | --- |
| "Where have I been?" | Visited OMTs visually distinct on overmap tint |
| "What's unexplored?" | Unseen cells dimmed or fogged |
| "Where am I?" | HUD: `WorldCoord (12, 45, 0)` |
| "Walk next door" | Arrow keys / click adjacent OMT → visit + move coord |
| "Revisit is fast" | Cache hit; optional "cached" in status line |

---

## W15a — Exploration state

### Model

```java
final class ExplorationState {
    boolean isSeen(int omx, int omy);
    boolean isVisited(int omx, int omy);
    void markSeen(int omx, int omy);
    void markVisited(int omx, int omy);
    void markSeenRect(int x0, int y0, int x1, int y1);  // optional zoom reveal
}
```

| Flag | Set when |
| --- | --- |
| **seen** | OMT entered viewport or adjacent to visited |
| **visited** | `SubmapGenerator.visit` completed for that cell |

BN analog: `overmap::seen`, `overmap::is_explored` — v3 implements subset (no NPC exploration).

### Rendering (`MapEditorScreen.drawOvermapGrid`)

```text
for visible cell (x,y):
    baseColor ← OMT tint
    if !exploration.isSeen(x,y):   baseColor *= 0.35  // dim
    else if exploration.isVisited(x,y): add subtle border or checkmark
```

Optional: only dim in "exploration mode" toggle (toolbar).

### Cache policy

| Event | Action |
| --- | --- |
| Visit OMT | `markVisited`; cache as today |
| Regenerate overmap `R` | Clear exploration + caches |
| Change single OMT (future) | Clear that cell + neighbors' visit cache |

Wire `WorldgenPreviewService` to hold `ExplorationState` or delegate to `WorldgenSession`.

### Tests

| Test | Assert |
| --- | --- |
| `ExplorationStateTest` | seen/visited independent; bounds safe |
| `SubmapGeneratorExplorationTest` | visit marks visited in session callback |

---

## W15b — World coordinates

### Model

```java
final class WorldCoord {
    int omx;
    int omy;
    int z;
}
```

Single-overmap v3: `omx`/`omy` are indices into `OvermapGrid` (0..width-1). No multi-overmap
`overmap_x`/`overmap_y` until v4.

### Editor UX

| Input | Action |
| --- | --- |
| Click OMT | Select + set `worldCoord` to `(x,y, visitZ)` |
| Enter | Visit + `markVisited` |
| Arrow keys (overmap mode) | Move selection to adjacent OMT; auto-visit optional setting |
| `[` / `]` (overmap) | Still cycle **overmap size** when no volume; when volume active, cycle z (W8) |

HUD line (extend `drawOvermapHud`):

```text
Position: (12, 45, z=0)  |  Seen: 142/1024  |  Visited: 38
```

### Avatar stub (optional)

- No sprite required v3
- `worldCoord` is enough for future game client to spawn avatar at submap center

### Tests

| Test | Assert |
| --- | --- |
| `WorldCoordTest` | equality, bounds |
| UI test optional | arrow moves selection within grid |

---

## WorldgenSession (facade)

Consolidate scattered state from `MapEditorScreen` + `WorldgenPreviewService`:

```java
final class WorldgenSession {
    OvermapGrid overmap;
    PlacedBuildingIndex placements;
    ExplorationState exploration;
    WorldCoord position;
    long seed;
    String regionId;

    VisitResult visitAt(int omx, int omy, int z);
    void regenerate(OvermapGenerateOptions options);
}
```

`MapEditorScreen` holds `WorldgenSession` instead of separate `overmapGrid` + flags.

**Migration:** incremental — session can wrap existing fields in W15a without big-bang refactor.

---

## Integration with W13

| W13 change | W15 impact |
| --- | --- |
| Stitch fix | May require `exploration.clearVisited(omx, omy)` on regen |
| Mapbuffer | Cache key may include submap offset — exploration still per OMT |

---

## Integration with W14

| W14 change | W15 impact |
| --- | --- |
| Regenerate layout | Clears exploration (same as today `R`) |
| Per-OMT edit (future) | Partial cache invalidation |

---

## Files to touch

| File | Change |
| --- | --- |
| `worldgen/explore/ExplorationState.java` | new |
| `worldgen/explore/WorldCoord.java` | new |
| `worldgen/session/WorldgenSession.java` | new |
| `worldgen/WorldgenPreviewService.java` | session hooks |
| `view/MapEditorScreen.java` | tint, HUD, arrow navigation |
| `view/MapEditorToolbar.java` | optional exploration toggle |

---

## Verification

1. Generate 32×32 → visit 3 cells → visited count = 3; revisiting shows cache hit in status
2. Unseen cells visibly dimmed when exploration overlay on
3. Arrow key moves coord; Enter visits new cell
4. Regenerate clears seen/visited counts

---

## Dependencies

| Requires | PR |
| --- | --- |
| W3 visit + cache | done |
| W10 large overmap | done |
| W13a recommended | stitch trust before exploration UX |

---

## Out of scope (W15)

| Topic | Notes |
| --- | --- |
| Save exploration to disk | W16 |
| Multi-overmap world position | v4 |
| NPC line-of-sight | Simulation |
| Fog re-hide of seen tiles | BN keeps seen; match BN |
| Terraform / edit OMT | Optional W15c or v4 |
