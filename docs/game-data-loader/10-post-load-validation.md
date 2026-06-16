# 10 — Post-load validation

Checks after [06](./06-load-pipeline.md) populates [08](./08-in-memory-model.md). BN
`check_consistency` is much larger — v1 implements a practical subset.

---

## Purpose

Catch data issues before map editor uses registries; optional gfx cross-check with
`LoadedTileset`.

---

## BN: `check_furniture_and_terrain`

```text
check_furniture_and_terrain():
    terrain_data.check()    // foreach ter_t::check
    furniture_data.check()  // foreach furn_t::check
```

Called from `DynamicDataLoader::check_consistency` after all mods finalized.

### `ter_t::check` examples

| Check | Severity |
| --- | --- |
| `open` / `close` / `transforms_into` id valid | debugmsg |
| `open == id` / `close == id` | debugmsg |
| `bash.ter_set` valid; not self | debugmsg |
| `bash` drop group exists | debugmsg |
| `move_cost == 1` | debugmsg (invalid for terrain) |
| `roof` chain sanity | debugmsg |
| `deconstruct` targets valid | debugmsg |

### `furn_t::check` examples

| Check | Severity |
| --- | --- |
| `open` / `close` valid | debugmsg |
| `EMITTER` flag requires `emissions` | debugmsg |
| `fluid_grid` tank capacity configured | debugmsg |
| Bash/deconstruct/pry same family as terrain | debugmsg |

BN uses `debugmsg` (continues running). Not fatal.

---

## Nextgen v1 validation

### `ValidationReport`

```java
class ValidationReport {
    List<String> errors;    // fatal if policy says so
    List<String> warnings;
}
```

### Checks

| Id | Check | Severity |
| --- | --- | --- |
| V1 | `terrain.size() > 0` after core load | error if empty |
| V2 | `looks_like` target in terrain or furniture registry | warn |
| V3 | `looks_like` target missing | warn |
| V4 | Duplicate ids during single load (stats) | info log |
| V5 | Gfx: terrain id ∉ tileset | warn (optional) |
| V6 | Gfx: `unknown` tile exists when many missing | info |

### Gfx cross-check (V5)

```text
if options.tileset != null:
    for id in terrain.allIds():
        if tileset.findTile(id).empty():
            warn("terrain %s: no gfx tile", id)
```

Run when tileset already loaded (map editor startup). Not part of bare `loadCore()`.

### `looks_like` (V2–V3)

v1 does **not** resolve chains at draw. Validation only checks immediate target exists:

```text
if def.looksLike != null && !registry.contains(def.looksLike):
    warn
```

---

## Deferred BN parity

Port when simulation needs:

- Bash `ter_set` / `furn_set` existence
- Trap id validity
- Item group references in bash drops
- Harvest + examine consistency

---

## Strict mod reporting

BN `json_report_strict` per mod source — extra warnings (e.g. `t_open_air` vs `t_null`).
Skip in v1.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Global check | `src/init.cpp` — `check_consistency` |
| F&T entry | `src/mapdata.cpp` — `check_furniture_and_terrain` |
| ter_t::check | `src/mapdata.cpp` |
| furn_t::check | `src/mapdata.cpp` |
| Gfx report | `src/cata_tiles.cpp` — `do_tile_loading_report` |

---

## Inputs

- `LoadedGameData`
- Optional `LoadedTileset`
- `ValidationOptions` (which checks enabled)

## Outputs

- `ValidationReport`
- Optional: throw if `errors` non-empty and `failOnError`

## Failure modes

| Policy | Behavior |
| --- | --- |
| `failOnError=true` | Throw on V1 empty terrain |
| `failOnError=false` | Return report; UI shows warnings |

## Verification

1. Core load: V1 passes; V5 lists known unassigned terrain ids for a minimal tileset
2. Synthetic `looks_like: "t_nonexistent"` → V3 warning
3. Validation completes < 1s on full terrain registry
4. Empty fixture dir → V1 error
5. Report strings include terrain id and check id
