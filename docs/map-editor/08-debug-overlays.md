# 08 — Debug overlays (spawn markers)

Optional **debug draw** for mapgen metadata that does not live in ter/furn layers.

**Status:** done (**M6**). Spawner **collection** is done (P13b).

---

## Purpose

`place_items` and `place_monsters` do not change `MapCell` — they produce
`SpawnMarker` records ([18-place-spawners](../mapgen-preview/18-place-spawners.md)). Editors
cannot see where loot or monsters would spawn without an overlay.

M6 draws lightweight markers on the canvas and stores them on the last import result.

---

## Data model

### `SpawnMarker` (existing)

```java
public final class SpawnMarker {
    public enum Kind { ITEM_GROUP, MONSTER_GROUP }
    public final Kind kind;
    public final String groupId;
    public final int x, y;
    public final float density;
}
```

### Attach to editor session

Extend import result handling in `MapEditorScreen` / `MapgenPreviewService`:

```java
public final class MapgenPreviewResult {
    MapGrid grid;
    List<SpawnMarker> spawnMarkers;
    List<String> runWarnings;
}
```

Building / `MapVolume` import: markers for **active floor** only, or merge lists per z with
z-filter when switching floors.

Hand-painted maps: empty marker list.

---

## Render pass

After furniture, before hover highlight:

```text
if showSpawnOverlay && !spawnMarkers.isEmpty():
    for marker in spawnMarkers visible in cull rect:
        color ← kind == ITEM ? yellow : red
        draw small rect or glyph at (marker.x, marker.y)
        optional: label groupId when zoom >= 2
```

Use `ShapeRenderer` or 1×1 white pixel tint — no new textures required.

### Toggle

| Input | Action |
| --- | --- |
| **`O`** | Toggle spawn overlay |
| Toolbar | "Spawns" checkbox (v2 polish) |

Default **off** to avoid clutter; on after importing lab/military mapgen for debugging.

---

## Future overlays (same pass, later PRs)

| Layer | Source | Priority |
| --- | --- | --- |
| Fields (`place_fields`) | Not in `MapGrid` v1 — needs runner to emit field map | Low |
| Traps (`setmap` / `place_traps`) | Same | Low |
| Nested chunk bounds | Debug nested placement rects | Dev-only |

Keep M6 scoped to **`SpawnMarker` only**; extend `DebugOverlay` interface if adding layers.

---

## `DebugOverlay` sketch (optional refactor)

```java
public interface DebugOverlay {
    void draw(SpriteBatch batch, BitmapFont font, MapEditorCamera cam, int tilePx);
    boolean isEmpty();
}

public final class SpawnMarkerOverlay implements DebugOverlay { … }
```

---

## Inputs

- `List<SpawnMarker>`, camera, grid dimensions

## Outputs

- Screen-space markers

## Failure modes

| Condition | Behavior |
| --- | --- |
| Marker OOB | Skip |
| Empty list | Toggle no-op |

---

## Verification

1. Import mapgen fixture with `place_monsters` → toggle **`O`** shows markers at JSON coords
2. Toggle off → clean canvas
3. Floor switch on `MapVolume` shows markers for active z only
4. Save/load hand map does not persist markers (session-only unless extended later)

```bash
gradlew.bat :core:test --tests "io.gdx.cdda.bn.nextgen.mapgen.json.PlaceSpawnerApplierTest"
```

---

## Related

- [mapgen-preview 18](../mapgen-preview/18-place-spawners.md)
- [11 map volume](../mapgen-preview/11-map-volume-and-floors.md) — multi-floor
- [worldgen 10 G6+](../worldgen/10-game-data-g6-plus.md) — item/monster names on hover
