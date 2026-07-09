# 07b — Mutable overmap specials

Procedural `overmap_mutable` layouts: phased piece placement, join matching, and runtime expansion.

**Parent:** [07-specials-and-mutable.md](./07-specials-and-mutable.md)

---

## Data model

JSON type: `overmap_mutable` in `data/json/overmap/overmap_mutable/`.

| Concept | Role |
| --- | --- |
| `overmap_mutable_id` | Template id (e.g. lab wings) |
| Entries / pieces | OMT terrain + relative position |
| `join` strings | Edge labels that must match adjacent pieces |
| Phases | Ordered steps — later pieces attach to earlier joins |

Specials reference mutable templates via `overmap_special` with
`subtype: mutable` (or equivalent JSON) linking to mutable id.

**Header types:** `overmap_special_subtype::mutable_` in `src/overmap_special.h`.

---

## Placement vs expansion

| When | Function | Role |
| --- | --- | --- |
| Generate / `place_special` | Initial anchor + phase 0 | Seeds mutable layout on overmap |
| Missions / runtime | `place_mutable_special` helpers | Grows layout into adjacent OMTs |

Mutable helpers live in `src/overmap.cpp` (~1286+ region — search `place_mutable`, `overmap_mutable`).

Join resolution ensures compatible `join` ids on shared edges before accepting a piece.

---

## Join semantics (conceptual)

```text
piece A east edge join "lab_corridor"
piece B west edge join "lab_corridor"  → compatible attachment
```

Rotation: piece may be turned so join directions align with `oter` rotation suffix rules.

Failed join → piece omitted, `debugmsg` in debug builds.

---

## Relationship to submap visit

Mutable specials often set `overmap_special_placements` and join metadata consumed at visit:

- `MapVolumeBuilder` / `JoinContext` in nextgen (W7, W13)
- BN `mapgendata` join parameters for nested mapgen

See [08-omt-to-submap.md](./08-omt-to-submap.md).

---

## Validation

`overmap_special::check` / mutable finalize:

- Join ids referenced by phases exist on piece definitions
- All `terrain` ids exist in `overmap_terrain`
- Phase order acyclic

Runs during `--check-mods`.

---

## Nextgen

| BN | Nextgen |
| --- | --- |
| Full phase graph | `MutableSpecialRegistry`, `MutableSpecialPlacer` |
| Runtime expansion | Visit-time only for v1 |
| Lab join fidelity | Partial — fixture tests in W14 |

---

## Inputs

- Mutable JSON registry
- Anchor OMT + rotation
- Existing overmap terrain for join matching

## Outputs

- Multi-OMT mutable footprint
- Join state for later phases

## Failure modes

- No matching join anchor — phase stops
- Out of bounds — piece skipped
- Conflicting terrain — attempt rejected

## Verification

1. Lab special on new world spans multiple OMTs with consistent corridor joins.
2. `--check-mods` clean on stock mutable definitions.
3. Visit joined OMT — interior mapgen aligns at shared edge (W13 tests).

**BN anchors:** `src/overmap.cpp` (mutable helpers), `src/overmap_special.h`, `data/json/overmap/overmap_mutable/`.
