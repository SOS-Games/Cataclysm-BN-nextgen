# 09 — Mod load order

How BN orders mods before JSON scan and how later mods override earlier definitions.
Discovery: [03](./03-mod-discovery.md). Pipeline: [06](./06-load-pipeline.md).

---

## Purpose

Document deterministic mod sequencing so `GameDataLoader.loadMods()` reproduces BN
override semantics.

---

## `normalize_mod_load_order`

```text
input: vector<mod_id> mods

1. Deduplicate (set insertion order preserved for first occurrence)

2. Find first mod where mod->core == true
   if found:
       remove it from list
       insert at beginning
   else:
       insert get_default_core_content_pack() at beginning  // "bn"

return mods
```

**Note:** This does **not** sort arbitrary dependencies — world creation uses
`dependency_tree` to build a valid ordered list beforehand.

---

## World mod list

When user creates a world:

```text
dependency_tree.resolve(active_mod_selection)
  → ordered mod_id list with dependencies included
  → stored in world->info->active_mod_order
```

`load_world_modfiles` uses that list (after normalize).

Invalid/missing dependency → world creation fails before load.

---

## Per-mod JSON scan order

```text
for mod in ordered_mods:
    load_data_from_path(mod.path, mod.str())
```

Within one mod:

- Files ordered lexically (typical)
- Objects within file in array order
- Same id twice → second `insert` wins

Across mods:

- Later mod’s `insert` **replaces** entire `ter_t` / `furn_t` for that id

---

## Core-always-first rationale

Core mod `bn` provides base `t_dirt`, items, etc. Content mods patch/override.

If core not first, a content mod might load before base definitions exist (broken refs).

`normalize` guarantees core pack at index 0 even if user list omitted it.

---

## Conflicts vs dependencies

| Mechanism | When checked |
| --- | --- |
| `dependencies` | World/mod UI — must be satisfied |
| `conflicts` | World/mod UI — cannot co-exist |
| Duplicate `modinfo` id | Discovery — all duplicates dropped |

Load order does not re-check conflicts.

---

## Replacement mods (BN advanced)

`mod_manager::load_replacement_mods(user_path)` — user-provided mod overlays. Optional for
nextgen v2.

---

## Nextgen `ModOrderResolver` (v2)

```java
List<ModId> resolve(List<ModId> userSelection, ModRegistry registry, DependencyTree tree)
```

v1: `List.of(CORE_MOD_ID)` only.

---

## BN source reference

| Concern | Location |
| --- | --- |
| Normalize | `src/init.cpp` — `normalize_mod_load_order` |
| World load | `src/init.cpp` — `load_world_modfiles` |
| Dependency tree | `src/dependency_tree.cpp` |
| Factory replace | `src/generic_factory.h` — `insert` |
| Default core | mod_management / `worldfactory` |

---

## Inputs

- User-selected mod ids
- `ModRegistry` (discovery)
- Optional `DependencyTree` (v2)

## Outputs

- Ordered mod id list for [06](./06-load-pipeline.md)

## Failure modes

| Condition | BN |
| --- | --- |
| Unknown mod id in list | debugmsg; skipped in load_and_finalize |
| Missing dependency | Blocked at world setup |
| Core mod missing on disk | Fallback insert default core id — may fail at load path |

## Verification

1. Normalize moves `bn` to front when present anywhere in list
2. Normalize inserts default core when absent
3. Fixture mod A + mod B override same `t_test` → B's definition wins when B later
4. Deduplicate removes second copy of same mod id
5. Load order logged for debug builds
