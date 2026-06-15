# Incremental tileset loading

Large tilesets (e.g. `hoder`) can take many seconds to decode and upload. The sprite viewer must
stay responsive during that work. This document describes how loading is **split across frames**
on the LibGDX **render thread** — not a background-thread loader.

---

## Why not a background thread?

LWJGL3 binds the OpenGL context to the render thread. Creating `com.badlogic.gdx.graphics.Texture`
from a worker thread crashes the JVM:

```text
No context is current or a function that is not available in the current context was called.
```

LibGDX `AssetManager` avoids this by finishing GPU uploads on the thread that owns the context.
Our loader uses the same constraint: **all `Texture` creation stays on the render thread**.

PNG decode and JSON parsing could move off-thread later; GPU upload cannot without a larger
refactor (Pixmap queue + main-thread drain).

---

## Two load APIs

| API | Use when | Behavior |
| --- | --- | --- |
| `TilesetLoader.load(…)` | Tests, tools, batch startup | Full load in one call; blocks caller |
| `TilesetLoadSession` | UI (sprite viewer, future game) | One GPU step per `step()`; caller pumps each frame |

Both produce the same `LoadedTileset` when complete. `TilesetLoadSession` mirrors the pipeline in
[05-load-pipeline.md](./tileset-loader/05-load-pipeline.md): config → sheets → mod merge → validate.

---

## Components

```text
TileDisplayScreen.render()
  └─ advanceLoadSession()     one session.step() per frame
       └─ TilesetLoadSession
            ├─ CONFIG         open tile_config.json, build sheet queue
            ├─ SHEET_DECODE   PixmapSheetLoader (PNG → Pixmap)
            ├─ SHEET_UPLOAD   SheetTextureUploader.IncrementalUpload (one chunk / step)
            ├─ SHEET_REGISTER TileRegistrar + StateModifierParser; advance global offset
            ├─ MOD_NEXT       compatible mod_tileset sheets (same per-sheet loop)
            └─ FINALIZE       PostLoadValidator → LoadedTileset

LoadingSpinner              arc drawn while session.isActive()
```

| Class | Package | Role |
| --- | --- | --- |
| `TilesetLoadSession` | `tileset.load` | Stateful load orchestration; `start`, `step`, `cancel` |
| `SheetTextureUploader.IncrementalUpload` | `tileset.atlas` | Splits one sheet into atlas chunks; one chunk (×8 FX tables) per `step()` |
| `TilesetLoadContext` | `tileset.load` | Shared loader state; `registerSheetAfterUpload`, `addOffset` |
| `LoadingSpinner` | `view` | Visual feedback; resets `SpriteBatch` color after `ShapeRenderer` |
| `TileDisplayScreen` | `view` | Owns session; forces grid layout when load completes |

---

## Session usage

```java
TilesetLoadSession session = TilesetLoadSession.start(
    registry,
    "hoder",
    TilesetLoadOptions.defaults(),
    ModTilesetRegistry.empty()
);

// Each frame (e.g. in ApplicationListener.render):
while (session.isActive()) {
    session.step();   // viewer calls this once per frame
    drawSpinner(session.getProgressLabel());
}
if (session.isComplete()) {
    LoadedTileset loaded = session.getResult();
}
if (session.isFailed()) {
    String error = session.getErrorMessage();
}
```

**Cancellation:** `session.cancel()` disposes partial textures and pixmaps. The viewer calls this
when switching tilesets with **`[`** / **`]`** during a load.

**Progress label:** e.g. `Loading hoder: tiles.png (2/5)  upload 3/12` — sheet index and chunk
upload within the current sheet.

---

## Viewer integration notes

1. **One `step()` per frame** — keeps the spinner animating and input responsive.
2. **Grid layout after load** — do not call `recomputeGridLayout` while `tileset == null` and then
   treat the viewport as laid out. The viewer uses `recomputeLayoutForViewport()` when
   `applyLoadedTileset` runs so `gridStartY` is below the HUD.
3. **`HdpiMode.Pixels`** — ortho projection uses back-buffer size; see [SPRITE_VIEWER.md](./SPRITE_VIEWER.md).

---

## Extending

| Idea | Notes |
| --- | --- |
| Decode PNG on worker thread | Queue `Pixmap`; drain uploads on render thread in `step()` |
| Multiple steps per frame | Time-budget loop in `advanceLoadSession` (e.g. 8 ms) |
| Game boot | Reuse `TilesetLoadSession` on a loading screen instead of blocking `TilesetLoader.load` |
| Dynamic atlas | Session uploads whole sheet in `SHEET_DECODE` (no chunk iterator yet) |

---

## Related

- [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) — controls and HUD
- [TILESET_LOADER.md](./TILESET_LOADER.md) — synchronous loader API
- [tileset-loader/06b-texture-upload.md](./tileset-loader/06b-texture-upload.md) — atlas chunk spec
