# Implementation plan — BN overmap reference → nextgen



Maps [reference unit docs](./README.md) to **Cataclysm-BN-nextgen** Java modules and milestone

PRs. Behavioral specs are language-agnostic; this file is the **porting index**.



---



## Target repository



| Item | Path |

| --- | --- |

| Java package | `core/src/main/java/io/gdx/cdda/bn/nextgen/worldgen/` |

| Agent guide | [`AGENTS.md`](../../AGENTS.md) |

| Nextgen milestone index | [`../README.md`](../README.md) |

| BN reference specs | This directory |

| BN C++ sources | Sibling [Cataclysm-BN](../../../../Cataclysm-BN) repo |



---



## Reference unit → nextgen module map



| Unit | BN source | Nextgen module(s) | Milestone |

| --- | --- | --- | --- |

| 01 Lifecycle | `overmapbuffer`, `populate`, `open` | `WorldgenPreviewService`, `OvermapGenerator` | W2, W10 |

| 02 Regional | `regional_settings.*` | `region.*`, `RegionPickerDialog` | W9, Tier B |

| 03 Oter | `overmap_terrains` | `overmap.OvermapTerrainLoader` | W1 ✓ |

| 04 Pipeline | `overmap::generate` (index) | `generate.OvermapGenerator` phase order | W5, W11, W17 |

| 04a Stitch | `populate_connections_out_from_neighbors` | — (not ported) | W16 / Tier C |

| 04b Hydrology | `place_rivers`, `place_lakes`, `polish_rivers` | `RiverEdgeStitcher`, `RiverGenerator`, `LakeOutletConnector`, `RiverPolisher` | W5, W17d (partial W16) |

| 04c Terrain | `place_forests`, `place_swamps`, `om_noise` | Forest/swamp in `OvermapGenerator` | W9, W17 |

| 04d Roads/trails | `place_roads`, `place_forest_trails` | `LocalRoadGenerator`, trails | W5, W17b |

| 05 Cities | `place_cities`, `build_city_street` | `CityGenerator` | W4, W17a |

| 06 Connections | `overmap_connection`, `build_connection` | `connection.*`, pathfind carve | W5 |

| 07 Specials | index | `placement.*` | W6 |

| 07a Static | `place_specials`, `place_special_attempt` | `StaticSpecialPlacer`, `RegionSpecialPlacer` | W6, W14 |

| 07b Mutable | `overmap_mutable`, joins | `mutable.*`, `JoinContext` | W6, W11, W13 |

| 08 Submap | `map::draw_map` | `submap.SubmapGenerator`, `mapgen.*` | W3, W13 |

| 09 Memory model | `overmap`, `map_layer` | `OvermapGrid`, `MapGrid` | W2 |

| 10 Validation | `check_consistency` | G5 validation, loader tests | G5 ✓ |

| A1 RNG | `om_noise`, seeds | `SubmapSeed`, generate `Random` | Ongoing |



✓ = milestone done at v1 scope; gaps in [../24-cdda-layout-gaps.md](../24-cdda-layout-gaps.md).



**Nextgen-only:** `HighwayGenerator` — not present in BN `place_roads`; document when comparing parity.



---



## Suggested work phases



| Phase | Reference units | Nextgen goal |

| --- | --- | --- |

| **A — Read BN truth** | 01, 04, 04a–d | Trace `overmap::generate` before changing Java order |

| **B — Hydrology v2** | 04b, 06, A1 | **Done** at single-overmap scope — remaining in [04b-hydrology.md](./04b-hydrology.md#remaining-defer-to-w16--tier-c-unless-noted) |

| **C — Region / terrain v2** | 02, 04c, 05 | 04c slice 2 ✓ (default-oter forest gate); **next:** region picker gaps ([02](./02-regional-settings.md)) |

| **D — Specials v2** | 07a, 07b | ENDGAME weight, `GLOBALLY_UNIQUE`, mutable phases |

| **E — Visit fidelity** | 08, 06 | `draw_connections` at submap edges |

| **F — World scale** | 01, 04a, 09 | `overmapbuffer` tiling, W15, W16 save |



---



## Parity tiers (from [../23-cdda-parity-overview.md](../23-cdda-parity-overview.md))



| Tier | Reference focus | Status |

| --- | --- | --- |

| **A** Layout art on one overmap | 04–07 | W17a–f largely done; tuning remains |

| **B** Region JSON fidelity | 02, 04c, 05 | Partial — picker + noise thresholds |

| **C** Multi-overmap world | 01, 04a, 06, 09 | Not started — no `overmapbuffer` |

| **D** Full simulation | 08 gameplay | Out of scope (mongroups, items on map) |



---



## Verification crosswalk



| Reference verification | Nextgen test / action |

| --- | --- |

| 04 phase order | `OvermapGeneratorTest` |

| 04b lake min size | Region fixture with high threshold |

| 04d road continuity | `LocalRoadGeneratorTest` |

| 06 bridge subtype | `OvermapConnectionLoaderTest` |

| 08 house submap | `JsonMapgenRunnerTest`, `SubmapGeneratorTest` |

| 03 oter mapgen | `OvermapTerrainLoaderTest` |

| 02 region default_oter | `RegionSettingsLoaderTest` |



---



## Changelog



| Date | Change |

| --- | --- |

| 2026-07-08 | Initial plan — reference folder + W1–W17 mapping |

| 2026-07-08 | Hydrology v2 done; Phase C started (04c swamp floodplain) |
| 2026-07-08 | Phase C slice 2 — `ForestGenerator` default-oter gate, `BaseTerrainFiller` default fill only |

