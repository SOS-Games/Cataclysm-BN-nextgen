# 09 — Post-load validation

Final steps at the end of `tileset_loader::load` that clean tile definitions and add
fallbacks. Optional **coverage report** runs later when game data is available. In-memory
model: [08](./08-in-memory-model.md). Sprite list parsing: [07a](./07a-sprite-lists.md).

---

## Purpose

Registration ([07b](./07b-tile-registration.md)) can leave:

- Sprite indices pointing past the last uploaded sprite
- Weighted variants with no valid frames
- Tiles with neither foreground nor background

Post-load validation fixes the `tile_ids` map before `tileset` is published. A separate report
compares loaded tile ids against core game definitions (terrain, items, monsters, …).

---

## When each phase runs

```text
tileset_loader::load()  (end, after all sheets + mods)
  ├─ 1. process_variations + prune empty tiles
  ├─ 2. unknown tile warning
  ├─ 3. ensure_default_item_highlight()
  ├─ 4. ts.tileset_id = …
  └─ 5. DYNAMIC_ATLAS: end_batch + readback_load

Later (not part of loader return):
  cata_tiles::do_tile_loading_report()   // requires init::is_data_loaded()
```

`do_tile_loading_report` is invoked from options reload / game startup after the tileset and
JSON game data are both loaded. It does **not** mutate the tileset.

---

## Phase 1 — `process_variations_after_loading`

Called for **each** remaining tile's `sprite.fg` and `sprite.bg` only (not mask lists).

```text
function process_variations_after_loading(vs):
    for each variant v in vs:
        remove from v.obj any id where id < 0 OR id >= offset

    remove variants with empty v.obj

    vs.precalc()
```

| Variable | Value at this point |
| --- | --- |
| `offset` | Total sprites uploaded across all sheets and mods (`N`) |
| Valid index range | `0 … N-1` |

### Per-variant sprite cleanup

```text
before: variant.obj = [10, 9999, 12]
after:  variant.obj = [10, 12]        // 9999 removed

before: variant.obj = [5000]  (all invalid)
after:  variant removed entirely
```

### `precalc()`

Rebuilds the weighted pick table on `weighted_int_list` ([07a](./07a-sprite-lists.md)). Required
before draw-time `pick()` is O(1). Skipping this after cleanup breaks random variants.

### Masks not processed

`masks.tint.fg/bg` are **not** run through `process_variations_after_loading`. They normally
hold `TILESET_NO_MASK` (`-1`) or indices parallel to cleaned fg lists. A port may optionally
mirror cleanup on mask lists.

---

## Phase 1b — Prune empty tiles

Immediately after fg/bg cleanup, iterates `ts.tile_ids`:

```text
for each (id, td) in tile_ids:
    process_variations(td.sprite.fg)
    process_variations(td.sprite.bg)

    if td.sprite.fg.empty() AND td.sprite.bg.empty():
        warn: tile {id} has no (valid) foreground nor background
        remove id from each tile_ids_by_season[*] if key == id   // see quirk below
        erase id from tile_ids
```

| Outcome | Example |
| --- | --- |
| fg only | Kept |
| bg only | Kept |
| Both empty after cleanup | **Removed** |
| All variants stripped as invalid | **Removed** |

### Seasonal cache quirk

Prune erases `tile_ids_by_season[season][it->first]` using the **full** map key (e.g.
`t_tree_season_winter`). Seasonal cache keys are **base** ids (`t_tree`). Pruning a seasonal
suffixed tile may leave a stale `season_tile` pointer until reload. Base-id pruning clears
the expected cache entry.

---

## Phase 2 — `unknown` warning

```text
if not ts.find_tile_type("unknown"):
    log warning: no 'unknown' tile defined
```

Non-fatal. Draw code falls back to `unknown` when no sprite exists ([08](./08-in-memory-model.md));
missing `unknown` yields nullptr / blank at draw time.

Category-specific fallbacks like `unknown_terrain_*` are draw-time only — not validated here.

---

## Phase 3 — `ensure_default_item_highlight`

Guarantees tile id `highlight_item` exists for UI item highlighting.

```text
if ts.find_tile_type("highlight_item"):
    return

// synthesize placeholder sprite + minimal tile def
```

### Standard build

```text
index = tile_values.size()
create solid-color surface (RGBA 0,0,127,127)
append texture to tile_values[index]
tile_ids["highlight_item"].sprite.fg.add([index], weight=1)
```

### `DYNAMIC_ATLAS`

Creates atlas sprite at `index = offset`, registers `tile_lookup` entry, same fg list.

### Quirks

| Topic | Behavior |
| --- | --- |
| `create_tile_type` | **Not** called — no seasonal cache update |
| `precalc()` | **Not** called on new fg list (single variant; pick still works via O(N) fallback) |
| `bg` | Left empty (tile kept because fg non-empty) |
| Pack-provided `highlight_item` | Untouched |

---

## Phase 4 — Finalize load

```text
ts.tileset_id = tileset_id

#if DYNAMIC_ATLAS
    tileset_atlas->end_batch()
    tileset_atlas->readback_load()
#endif
```

`cata_tiles::load_tileset` then assigns `tileset_ptr` and calls `set_draw_scale`.

---

## Coverage report — `do_tile_loading_report`

Diagnostic only. Compares **game entity ids** to **tileset ids**.

### Preconditions

```text
if not init::is_data_loaded():
    return    // only prints "Loaded tileset: …" line
```

Requires core JSON (terrain, items, …) loaded. Runs when tileset option changes or at game
start — not inside `tileset_loader::load`.

### Output

First line:

```text
Loaded tileset: <TILES option value>
```

Then, per category, two lines:

```text
Missing <category>: <space-separated ids>
Missing <category> (but looks_like tile exists): <space-separated ids>
```

### Categories scanned

| Category enum | Label | Id prefix for `find_tile_type` |
| --- | --- | --- |
| `C_TERRAIN` | `terrain` | `""` → `t_dirt` |
| `C_FURNITURE` | `furniture` | `""` |
| `C_ITEM` | `item` | `""` |
| `C_MONSTER` | `monster` | `""` |
| `C_VEHICLE_PART` | `vehicle_part` | `"vp_"` |
| `C_TRAP` | `trap` | `""` |
| `C_FIELD` | `field` | `""` |

Not scanned in BN: lighting, bullet, weather, overmap terrain, hit entity, etc.

### Per-entity check (`lr_generic`)

For each game id string:

```text
tile_id = prefix + id_string

if NOT find_tile_type(tile_id) AND NOT find_tile_looks_like(id_string, category):
    → list under "Missing …"

else if NOT find_tile_type(tile_id):
    → list under "Missing … (but looks_like tile exists)"
```

`find_tile_looks_like` (default jump limit **10**):

1. Try `find_tile_with_season(id)` on the **unprefixed** id (seasonal + direct)
2. Follow game object's `looks_like` chain by category (terrain → `ter_t`, item → `itype`, etc.)

So an item missing `rock` but with `looks_like: stone` and `stone` tiled → second bucket.

Vehicle parts use `vp_` prefix for tile ids but not for game vpart ids.

---

## Full end-of-load sequence

```text
// tileset_loader::load — after mod tilesets loop

for (id, td) in tile_ids:
    clean_and_precalc(td.sprite.fg)
    clean_and_precalc(td.sprite.bg)
    drop if both fg and bg empty

warn if no "unknown"
ensure_default_item_highlight()
tileset_id = …
[DYNAMIC_ATLAS finalize]

// cata_tiles — after successful load
tileset_ptr = move(new_tileset)
set_draw_scale(…)

// later, when game data ready
do_tile_loading_report(out)   // read-only audit
```

---

## Port guidance

| Component | Required for minimal port? |
| --- | --- |
| `process_variations_after_loading` | **Yes** — prevents OOB texture access |
| Empty tile prune | **Yes** |
| `unknown` warning | Optional (log only) |
| `ensure_default_item_highlight` | Yes if UI highlight needed; else omit or stub |
| `do_tile_loading_report` | Optional dev/CI tool |
| Mask list cleanup | Optional |
| Seasonal cache on prune | Match BN quirk or fix by erasing base id |

Minimal pseudocode:

```text
N = texture_table.length
for (id, tile) in tile_ids:
    for list in [tile.sprite.fg, tile.sprite.bg]:
        for variant in list:
            variant.frames = filter(f -> 0 <= f < N, variant.frames)
        list.variants = filter(v -> v.frames not empty, list.variants)
        list.precalc()
    if tile.sprite.fg.empty and tile.sprite.bg.empty:
        delete tile_ids[id]
```

---

## BN source reference

| Concern | Location |
| --- | --- |
| End-of-load loop | `src/cata_tiles.cpp` — `tileset_loader::load` |
| Sprite cleanup | `src/cata_tiles.cpp` — `process_variations_after_loading` |
| Highlight fallback | `src/cata_tiles.cpp` — `ensure_default_item_highlight` |
| Coverage report | `src/cata_tiles.cpp` — `do_tile_loading_report`, `lr_generic` |
| Report callers | `src/options.cpp`, `src/sdltiles.cpp`, `src/game.cpp` |
| `looks_like` chain | `src/cata_tiles.cpp` — `find_tile_looks_like` |

---

## Inputs

- Fully loaded `tileset` (textures + `tile_ids` before cleanup)
- Loader `offset` = final sprite count `N`

## Outputs

- Cleaned `tile_ids` with valid sprite indices and `precalc` on fg/bg lists
- Optional synthesized `highlight_item`
- Warning if `unknown` absent
- Coverage report lines (separate call, read-only)

## Failure modes

| Condition | Behavior |
| --- | --- |
| All sprite indices invalid for a tile | Tile removed |
| No `unknown` | Warning only; load succeeds |
| `ensure_default_item_highlight` surface fail | `throwErrorIf` / assert (load fails) |
| Report without game data | Early return after first line |
| Skip `precalc` after cleanup | Random variants may misbehave at draw |

## Verification

A correct port should demonstrate:

1. Tile with `fg` index `N+5` → index stripped; if fg empty and bg empty → tile removed
2. Weighted fg `[valid, invalid]` → invalid stripped; variant kept; `precalc` sum of weights unchanged relative to valid weights
3. Tile with valid fg only survives prune
4. No `unknown` in pack → warning logged; load still succeeds
5. No `highlight_item` → solid placeholder created; `find_tile_type("highlight_item")` non-null
6. Pack with `highlight_item` → synthesis skipped
7. `do_tile_loading_report` before game data → no category lines
8. Missing `t_rock` but `looks_like` resolves → appears in second missing bucket, not first
9. After cleanup, every sprite index in every remaining tile satisfies `0 ≤ idx < N`
