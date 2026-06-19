package io.gdx.cdda.bn.nextgen.mapgen.region;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

/** Weighted alias table for one region_settings id (P11). */
final class RegionAliasTable {

    private final Map<String, List<RegionWeightedChoice>> terrainAliases = new HashMap<>();
    private final Map<String, List<RegionWeightedChoice>> furnitureAliases = new HashMap<>();

    void mergeFrom(final RegionAliasTable other) {
        if (other == null) {
            return;
        }
        mergeAliases(terrainAliases, other.terrainAliases);
        mergeAliases(furnitureAliases, other.furnitureAliases);
    }

    private static void mergeAliases(
        final Map<String, List<RegionWeightedChoice>> target,
        final Map<String, List<RegionWeightedChoice>> source
    ) {
        for (final Map.Entry<String, List<RegionWeightedChoice>> entry : source.entrySet()) {
            target.put(entry.getKey(), entry.getValue());
        }
    }

    void putTerrainAlias(final String alias, final JsonValue value) {
        putAlias(terrainAliases, alias, value);
    }

    void putFurnitureAlias(final String alias, final JsonValue value) {
        putAlias(furnitureAliases, alias, value);
    }

    private static void putAlias(
        final Map<String, List<RegionWeightedChoice>> aliases,
        final String alias,
        final JsonValue value
    ) {
        if (alias == null || alias.isEmpty() || value == null) {
            return;
        }
        final List<RegionWeightedChoice> choices = RegionWeightedChoice.parse(value);
        if (!choices.isEmpty()) {
            aliases.put(alias, choices);
        }
    }

    String resolveTerrain(final String id, final Random rng, final Consumer<String> warningSink) {
        return resolve(id, terrainAliases, "t_region_", rng, warningSink);
    }

    String resolveFurniture(final String id, final Random rng, final Consumer<String> warningSink) {
        return resolve(id, furnitureAliases, "f_region_", rng, warningSink);
    }

    private static String resolve(
        final String id,
        final Map<String, List<RegionWeightedChoice>> aliases,
        final String regionalPrefix,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        if (id == null || id.isEmpty()) {
            return id;
        }
        String current = id;
        final Set<String> visiting = new HashSet<>();
        while (aliases.containsKey(current)) {
            if (visiting.contains(current)) {
                RegionWeightedChoice.emitWarning(warningSink, "regional alias cycle at " + current);
                break;
            }
            visiting.add(current);
            current = RegionWeightedChoice.pick(aliases.get(current), rng, warningSink);
        }
        if (current.equals(id) && current.startsWith(regionalPrefix) && !aliases.containsKey(id)) {
            RegionWeightedChoice.emitWarning(
                warningSink,
                "unresolved regional alias: " + id + " (no region_settings mapping)"
            );
        }
        return current;
    }

    boolean isEmpty() {
        return terrainAliases.isEmpty() && furnitureAliases.isEmpty();
    }
}
