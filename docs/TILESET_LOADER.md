# Tileset loader — implementation guide

Java/LibGDX implementation of the Cataclysm-BN tileset loader. Behavioral specs are in
**this repo** under [`docs/tileset-loader/`](./tileset-loader/README.md) (ported from
[Cataclysm-BN](../Cataclysm-BN)).

Use the sprite viewer ([SPRITE_VIEWER.md](./SPRITE_VIEWER.md)) to inspect loaded packs at runtime.

---

## Where to implement

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/tileset/
  GfxPaths.java              # gfx root resolution
  TilesetDiscovery.java      # unit 02
  TilesetConfigLoader.java   # unit 04b
  TilesetLoader.java         # units 05–09 orchestration
  load/                      # load context, options
  parse/                     # 07a, 07b, 07d
  atlas/                     # 06a, 06b, 06c, A1
  mod/                       # 04f
  model/                     # unit 08 — LoadedTileset, TileDefinition, …
  validate/                  # unit 09
```

Rendering and the sprite viewer stay in `view/` and consume `LoadedTileset`.

---

## Specification

| Document | Path |
| --- | --- |
| Index + scope | [tileset-loader/README.md](./tileset-loader/README.md) |
| **Implementation plan** | [tileset-loader/implementation-plan.md](./tileset-loader/implementation-plan.md) |
| Unit specs | [tileset-loader/01-…](./tileset-loader/01-overview-and-lifecycle.md) through [09-…](./tileset-loader/09-post-load-validation.md), [appendix-dynamic-atlas.md](./tileset-loader/appendix-dynamic-atlas.md) |

Implement in unit dependency order (01 → 02 → 04a → … → 09). BN C++ source files listed in
each unit doc are the authority for ambiguous behavior.

---

## Gfx data on disk

Point `GfxPaths` at a BN or CDDA-Tilesets checkout:

```text
gfx/
  UndeadPeopleTileset/
    tileset.txt
    tile_config.json
    *.png
```

Example roots:

| Path | Purpose |
| --- | --- |
| `../Cataclysm-BN/gfx` | Primary game packs (auto-detected by `lwjgl3:run`) |
| `../CDDA-Tilesets/gfx` | External tileset repo |
| `../Cataclysm-BN/data/mods` | `mod_tileset` merge (`cdda.mod.roots`) |

System properties: `cdda.gfx.roots`, `cdda.mod.roots` (semicolon-separated).

---

## Public API (current)

```java
TilesetRegistry registry = TilesetDiscovery.build();
LoadedTileset loaded = TilesetLoader.load(registry, "hoder", TilesetLoadOptions.defaults());
// or TilesetLoadOptions.dynamicAtlas() for A1 path

loaded.findTile("t_dirt");
loaded.getTexture(spriteIndex, TilesetFxType.NIGHT);
loaded.getStateModifiers();
loaded.dispose();
```

---

## v1 milestone

| Item | Status |
| --- | --- |
| Spec available (`docs/tileset-loader/`) | done |
| `list_tilesets()` — `TilesetDiscovery.build` | done |
| Open `tile_config.json` — `TilesetConfigLoader` | done |
| `load_tileset(id)` — sheets + `tile_ids` | done |
| Post-load validate (09) | done |
| Mod tileset merge (04f) | done |
| Eight baked FX tables (06c) | done |
| State modifiers parsing (07d) | done |
| Dynamic atlas (A1) — opt-in | done |
| Skip ASCII (04d/07c) | skipped (sprites-only) |
| Sprite viewer | done — [SPRITE_VIEWER.md](./SPRITE_VIEWER.md) |

**Acceptance:** `load_tileset("hoder")` returns a `LoadedTileset` where `findTile("t_dirt")` has
valid fg indices and `getTexture(index)` returns a LibGDX `TextureRegion`.

### Optional gaps (not blocking v1)

- Seasonal tile resolution at draw time
- Config tint / `overlay_ordering` application in renderer
- Global warp whitelist/blacklist at draw time
- Full A1 UV warp compositing at draw time

---

## Reference project

[cygnus-engine](../../Documents/cygnus-engine) — LibGDX Gradle layout, path resolution, JSON I/O
patterns (`ModPaths.java`, `ModJson.java`). Do not copy game logic.

---

## Tests

Under `core/src/test/java/.../tileset/`:

- JSON/fixture unit tests per spec **Verification** sections
- Integration tests with `-Dcdda.gfx.roots=../Cataclysm-BN/gfx`

```bash
gradlew.bat test
```

---

## Agent entry point

1. Read [AGENTS.md](../AGENTS.md) in this repo.
2. Read [tileset-loader/implementation-plan.md](./tileset-loader/implementation-plan.md).
3. Implement the current slice; run `gradlew.bat compileJava` and `gradlew.bat test`.
