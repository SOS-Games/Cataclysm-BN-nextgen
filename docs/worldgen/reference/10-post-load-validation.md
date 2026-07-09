# 10 — Post-load validation

Consistency checks on overmap-related game data before play and during mod test.

---

## When checks run

| Pass | Trigger | File |
| --- | --- | --- |
| `regional_settings::finalize` | Per region at load | `regional_settings.cpp` |
| `check_regional_settings` | After all regions loaded | `regional_settings.cpp` |
| `overmap_terrains::finalize` | End of terrain load | `overmap.cpp` |
| `overmap_terrains::check_consistency` | `--check-mods`, test mode | `overmap.cpp` |
| `overmap_connections::finalize` / `check_consistency` | Connection load | `overmap_connection.cpp` |
| `overmap_special::check` | Per special at load | `overmap_special.cpp` |
| Mutable finalize | Mutable JSON load | overmap mutable loaders |

Hard failures → non-zero `--check-mods` exit. Runtime first-use may `debugmsg`.

---

## OMT registry

### `overmap_terrains::finalize`

- `"default"` region must exist (via regional finalize)
- Every region's `default_oter` resolves
- Default terrain has mapgen or builtin

### `overmap_terrains::check_consistency`

- Each `mapgen` id → existing json mapgen, builtin, or lua
- `looks_like` targets exist (warning tier in some builds)
- Rotation variants consistent for rotatable types
- Duplicate id warnings across mods

---

## Regional settings

`check_regional_settings`:

- `default_oter`, forest/lake oter references valid
- Thresholds numeric
- `city_spec` building bins reference valid `overmap_special_id`
- `forest_trail.trail_connection` resolves
- `region_terrain_and_furniture` keys finalize to real ter/furn ids

---

## Connections

`overmap_connection::check`:

- Each subtype `terrain` exists in `overmap_terrain`
- Each `locations` entry → valid `overmap_location`
- `layout` enum valid
- Weighted subtype pools non-empty where required

Common failure: break `local_road` subtype terrain id → check names missing oter.

---

## Specials

`overmap_special::check`:

- Every footprint `terrain` id defined
- Nested special references resolvable
- Mutable join ids match piece definitions
- Connection embeds reference valid `overmap_connection_id`
- Occurrence intervals sane

Placement-time failures (overlap) are **not** validation errors — silent skip during generate.

---

## Runtime warnings (non-fatal)

| Situation | Behavior |
| --- | --- |
| `build_connection` no subtype | `debugmsg`, segment abort |
| Special placement failed | Silent skip |
| Missing gfx sprite | Tile loading report only |
| Generate on unknown oter | Fallback / debugmsg |

---

## Relationship to nextgen

Mirrored subset:

- G5 game data validation
- `OvermapTerrainLoaderTest`, `OvermapConnectionLoader` fixtures
- Mapgen catalog warnings

Full `--check-mods` parity: use BN binary for mod authoring.

---

## Inputs

- Merged JSON from core + mod stack

## Outputs

- Error/warning lists
- CI pass/fail

## Failure modes

- Hard: missing default region, broken mapgen on common oter
- Soft: optional looks_like missing

## Verification

1. `./cataclysm-bn-tiles --check-mods` exit 0 on stock data.
2. Test mod with invalid mapgen on dummy oter — consistency names file.
3. Break sewer subtype in connection JSON — connection check fails.

**BN anchors:** `src/overmap.cpp`, `src/regional_settings.cpp`, `src/overmap_connection.cpp`,
`src/overmap_special.cpp`.
