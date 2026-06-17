# 09 — Building bundles overview

Connect **multiple mapgen pieces** into one preview session: floors (z-levels) and adjacent
OMT tiles (same floor). Still not full world/overmap generation.

---

## Purpose

After P1–P4, the editor imports **one** `JsonMapgenDefinition` → **one** `MapGrid`. BN
buildings are usually **several** submaps:

| BN concept | Example (`house_09`) |
| --- | --- |
| Ground floor | `om_terrain: house_09` — `house09.json` #0 |
| Roof | `om_terrain: house_09_roof` — `house09.json` #1 |
| Basement | `om_terrain: basement_bionic` — separate mapgen, z = −1 |
| Multi-tile footprint | `2StoryModern04` — 2×2 OMT per floor |

Users want:

1. **Floor switch** — basement / ground / upper / roof without re-opening the picker.
2. **Stitched canvas** — pan across a 48×24 (or larger) layout for multi-OMT buildings.

This unit defines scope and PR split; details in [10](./10-city-building-loader.md),
[11](./11-map-volume-and-floors.md), [12](./12-omt-stitch-composer.md).

---

## BN reference (how pieces connect)

### `city_building` layout

`data/json/overmap/multitile_city_buildings.json`:

```json
{
  "type": "city_building",
  "id": "house_09",
  "overmaps": [
    { "point": [ 0, 0, 0 ], "overmap": "house_09_north" },
    { "point": [ 0, 0, 1 ], "overmap": "house_09_roof_north" },
    { "point": [ 0, 0, -1 ], "overmap": "basement_bionic_north" }
  ]
}
```

| `point` index | Meaning |
| --- | --- |
| `[0]` | OMT offset X (east) in 24×24 tiles |
| `[1]` | OMT offset Y (south) in 24×24 tiles |
| `[2]` | Z-level (floor): negative = basement, 0 = ground, positive = upper/roof |

Each `overmap` id resolves to an **overmap terrain** entry, which references **mapgen**
(`method: json` + `om_terrain` or weighted pick). Preview v1.5 resolves to existing
`JsonMapgenDefinition` rows in `MapgenCatalog`.

### Rotation suffixes

BN appends direction to overmap ids (`_north`, `_east`, …). Mapgen catalog keys on
`om_terrain` without direction. Resolver strips known suffixes before catalog lookup; **rotation
of rows** remains deferred ([08](./08-v2-parity-roadmap.md)).

### Submap size

BN submaps are **24×24** cells (`SEEX` / `SEEY`). Stitch offsets use `point[0] * 24` and
`point[1] * 24` unless a piece’s `rows` produce a different size (use actual `MapGrid` bounds).

---

## Preview v1.5 vs v1 vs worldgen

| Capability | P1–P4 (v1) | P5–P6 (v1.5 bundles) | Full worldgen |
| --- | --- | --- | --- |
| Single mapgen import | ✓ | ✓ | ✓ |
| Multi-floor one building | manual picker | ✓ floor switch | ✓ |
| Multi-OMT same floor | — | ✓ stitched grid | ✓ overmap placement |
| Weighted mapgen pick | — | first json / warn | ✓ |
| `predecessor_mapgen` | — | defer | ✓ |
| Regional `t_region_*` | pass-through id | same | resolve |

---

## PR slices

| PR | Focus | Units |
| --- | --- | --- |
| **P5** | `city_building` metadata, `MapVolume`, floor UI | [10](./10-city-building-loader.md), [11](./11-map-volume-and-floors.md) |
| **P6** | OMT stitch into one `MapGrid` per floor | [12](./12-omt-stitch-composer.md) |

**Order:** P5 first (`house_09` multi-floor without stitching). P6 adds multi-tile footprints
(`2StoryModern04`).

---

## User flows (target)

### Import building (P5)

```text
Mapgen picker → filter "house_09"
  Row: "house_09 (building, 3 floors)"  [Import building]
  Row: "house_09"                       [Single floor]  (existing)

Import building:
  CityBuildingLoader.find("house_09")
  for each distinct z: run mapgen → MapGrid in MapVolume
  editor.setVolume(volume); volume.setActiveZ(0)
  PageUp/PageDown or toolbar Floor ▲/▼
```

### Import stitched building (P6)

```text
Same picker → "2StoryModern04 (building, 5 z × 2×2 OMT)"
  for each z:
    stitch all pieces at point (px, py) → one MapGrid
  MapVolume stores stitched grid per z
  pan/zoom across full width (e.g. 48×24)
```

---

## Planned Java packages

```text
core/src/main/java/io/gdx/cdda/bn/nextgen/
  mapgen/
    building/
      CityBuildingLoader.java
      CityBuildingDefinition.java
      CityBuildingPiece.java
      OvermapTerrainResolver.java
    compose/
      MapVolume.java
      MapVolumeBuilder.java
      OmtStitchComposer.java
  map/
    MapGrid.java              # existing; optional blit helper
  view/
    MapEditorScreen.java      # volume + floor controls
    MapgenPickerDialog.java   # building vs single entry
```

---

## Failure modes (cross-cutting)

| Condition | Behavior |
| --- | --- |
| No `city_building` for picked om_terrain | Fall back to single-floor import (P3 path) |
| Piece has no json mapgen in catalog | Warn; skip piece or abort bundle (config flag) |
| Builtin-only overmap terrain | Warn; skip piece |
| Multiple json mapgens on one OMT | Pick first json with `rows`; warn |
| Stitch overlap | Later piece wins (document order = sorted by point) |
| Huge building (many OMT) | Cap max dimension (e.g. 8×8 OMT); warn |

---

## Verification

1. `house_09` bundle: 3 floors (basement, ground, roof) switchable without picker
2. Single-floor import still works unchanged
3. `2StoryModern04` P6: ground floor 48×24 visible via pan
4. Unit tests on fixture `city_building` JSON without BN checkout
5. Integration test optional: sibling BN `multitile_city_buildings.json`

---

## BN source reference

| Concern | Location |
| --- | --- |
| City building defs | `data/json/overmap/multitile_city_buildings.json` |
| Special stacks | `data/json/overmap/overmap_special/`, `overmap_mutable/` |
| Bundle inventory | [13-building-bundle-sources.md](./13-building-bundle-sources.md) |
| Overmap terrain | `data/json/overmap/overmap_terrain/` |
| OMT → mapgen | `src/mapgen.cpp` — `map::gen`, `oter_mapgen` |
| Submap size | `src/coordinates.h` — `SEEX`, `SEEY` |
| Z-level stack | `src/map.cpp`, `src/mapbuffer.cpp` |

---

## Related

- [01-overview-and-scope](./01-overview-and-scope.md) — v1 single-grid limits
- [06-preview-ui](./06-preview-ui.md) — picker baseline
- [08-v2-parity-roadmap](./08-v2-parity-roadmap.md) — rotation, predecessor, world scale
- [13-building-bundle-sources](./13-building-bundle-sources.md) — all BN bundle types + P7 gaps
