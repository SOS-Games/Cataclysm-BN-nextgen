# Appendix A1 — Algorithms and RNG

Notable algorithms and randomness sources in BN overmap generation.

---

## Noise layers (`om_noise`)

**Files:** `src/overmap_noise.h`, `src/overmap_noise.cpp`.

Base class stores `global_base_point` (overmap origin in absolute OMT coords) + world seed.

| Layer | Class | Used by |
| --- | --- | --- |
| Forest | `om_noise_layer_forest` | `place_forests` |
| Floodplain | `om_noise_layer_floodplain` | `place_swamps` |
| Lake | `om_noise_layer_lake` | `place_lakes` |

### Formulas (`noise_at`)

All use `scaled_octave_noise_3d(octaves, persistence, scale, …, abs_x, abs_y, seed)` then power curve:

| Layer | Octaves | Scale | Result |
| --- | --- | --- | --- |
| Forest | 8 + 12 | 0.03, 0.07 | `max(0, r² − d²×0.5)` |
| Floodplain | 8 | 0.05 | `r²` |
| Lake | 16 | 0.002 | `r⁴` |

**Determinism:** same world seed + absolute OMT → same noise. Overmap file boundaries invisible.

---

## `connect_closest_points`

**Anchor:** `src/overmap.cpp` **5400–5421**.

Greedy: for each point index `i`, connect to closest `j > i` if distance > 0.

Not guaranteed minimum spanning tree — order-dependent.

Used by: `place_roads`, `place_forest_trails`.

Path inside: `build_connection` → `lay_out_connection` (A* with subtype costs).

---

## `place_river` drunkard walk

**Anchor:** **4696–4779**.

Parameters from `river_scale`:

```cpp
river_chance = max(1, int(1.0 / river_scale))
river_scale  = max(1, int(river_scale))  // brush radius
```

Loop: random walk + biased steps toward endpoint + sporadic `river_center` paint in brush.
Skips lakes. Avoids map edges except near target.

---

## `polish_rivers`

**Anchor:** **5460+**.

Graph rewrite on all `river*` prefix OMTs. Neighbor water test includes adjacent overmaps;
missing neighbor file → assume water off edge.

Runs **after** specials so bridges and shores exist.

---

## `roll_remainder` / city count

`NUM_CITIES = roll_remainder(omts_per_overmap * coverage / omts_per_city)` — unbiased integer
rounding for expected city count. See [05-cities-and-urban.md](./05-cities-and-urban.md).

City size tier: weighted `rng(op_city_size-1, op_city_size+1)` with `one_in` fractions.

---

## Special spacing

`specials_overlay` bitmap: land specials respect `SPECIALS_SPACING` (Manhattan/circular — overlay impl).

Sort large specials first; ENDGAME ×1000 weight at origin overmap.

Density: `zone_ratio = (eligible_cells / OMAP_AREA) * crowd_ratio * SPECIALS_DENSITY`.

---

## Submap seed mixing

Set during `mapgendata` construction before `oter_mapgen.pick`. Combines world seed with
OMT/submap coordinates — grep `mapgendata` init in `mapgen.cpp` / `mapbuffer` for exact XOR/mix.

Nextgen: `SubmapSeed.mix(worldSeed, SubmapKey)`.

---

## RNG streams

| Stream | Use |
| --- | --- |
| World seed → noise | Deterministic layout fields |
| `rng_get_engine()` | City placement, river walk, shuffles, special rolls |
| Order sensitivity | Any change to generate phase order alters RNG consumption |

Golden tests: fix seed **and** generation path (skip conditions, mod set).

---

## BN divergences (vs generic CDDA wiki)

| Area | BN behavior |
| --- | --- |
| Defense mode | No `generate` |
| Pocket dimensions | No full generate |
| Lake after river | Lakes absorb river OMTs |
| `place_roads` | `local_road` only — no core highway connection |
| `display_oter` | UI-only override |
| Lua mapgen | Supported when enabled |

Authoritative: **Cataclysm-BN** `src/overmap.cpp`, not upstream wiki.

---

## Inputs

- World seed, options, region thresholds

## Outputs

- Pseudorandom layout decisions

## Failure modes

- RNG order change → different world
- Different skip flags → different stream even with same seed

## Verification

1. Two new worlds, same seed/options — identical `(0,0)` overmap terrain sample.
2. `river_scale` 4 vs 1 — measurable river cell count change.
3. Forest threshold 0 — no forest except pre-placed.

**BN anchors:** `src/overmap.cpp`, `src/overmap_noise.cpp`, `src/simplexnoise.cpp`, `src/mapgen.cpp`.
