# 07 — Specials and mutable (index)

Static `overmap_special` placement during `place_specials`, and procedural `overmap_mutable`
expansion. Split into **07a** (fixed footprints) and **07b** (mutable phases/joins).

**Pipeline position:** after `place_roads`, before `place_forest_trailheads` and `polish_rivers`.

---

## Sub-units

| Unit | File | Topics |
| --- | --- | --- |
| 07a | [07a-static-specials.md](./07a-static-specials.md) | `place_specials`, zones, density, `place_special_attempt` |
| 07b | [07b-mutable-specials.md](./07b-mutable-specials.md) | `overmap_mutable`, joins, `place_mutable_special` |

---

## Data paths

| Type | JSON path |
| --- | --- |
| Static specials | `data/json/overmap/overmap_special/` |
| Mutable specials | `data/json/overmap/overmap_mutable/` |
| Location tags | `data/json/overmap/special_locations.json` (`overmap_location`) |
| City building picks | Region `city_spec` → specials ids |

---

## Entry point

```cpp
void overmap::place_specials( overmap_special_batch &enabled_specials );
```

**Anchor:** `src/overmap.cpp` **6176–6346**.

---

## Enabled batch (before generate)

`overmap::populate(dim_id)` (~2926):

1. `overmap_specials::get_default_batch(loc)` — world options + scenario filters
2. Filter by region `overmap_feature_flag` blacklist/whitelist
3. Pass to `open` → `generate`

Region blacklist removes specials whose **flags intersect** blacklist set; whitelist requires
**intersection** with whitelist when whitelist mode active.

---

## Quick reference: special flags (JSON)

Common flags affecting placement (`overmap_special::has_flag`):

| Flag | Effect |
| --- | --- |
| `UNIQUE` | At most one instance per overmap |
| `GLOBALLY_UNIQUE` | Tracked across world |
| `ENDGAME` | Boost weight at abs origin `(0,0)` overmap; suppress elsewhere if also `GLOBALLY_UNIQUE` |
| `LAKE` / `RIVER` | Force lake/river zone |

---

## Nextgen

| BN | Nextgen module |
| --- | --- |
| Zone + density math | `RegionSpecialPlacer`, `StaticSpecialPlacer` |
| Mutable phases | `MutableSpecialPlacer`, `JoinContext` |
| ENDGAME center priority | Not implemented |

Milestones: W6, W14 — [../README.md](../README.md).

---

## Inputs

- `enabled_specials` batch
- Current `map_layer` after roads
- Options `SPECIALS_DENSITY`, `SPECIALS_SPACING`

## Outputs

- Painted special OMT footprints
- `overmap_special_placements` map (anchor → special id)

## Failure modes

- No valid anchor in zone — special skipped for this overmap
- Density crowd_ratio < 1 — lower-priority specials throttled

## Verification

1. `--check-mods` validates special terrains against `overmap_terrain`.
2. Mansion/lab appears with spacing ≥ `SPECIALS_SPACING` (Manhattan overlay).
3. `central_lab` / ENDGAME weighted toward `(0,0)` overmap center.

**BN anchors:** `src/overmap_special.h`, `src/overmap.cpp` (`place_specials`, `place_special_attempt`).
