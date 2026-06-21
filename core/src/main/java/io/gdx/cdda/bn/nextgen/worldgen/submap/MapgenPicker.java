package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.MapgenRef;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Weighted json mapgen pick for an OMT visit (W3). */
public final class MapgenPicker {

    private MapgenPicker() {}

    public static Optional<JsonMapgenDefinition> pick(
        final String omtId,
        final int z,
        final Random rng,
        final OvermapTerrainRegistry registry,
        final MapgenCatalog catalog,
        final List<String> warnings
    ) {
        if (omtId == null || omtId.isEmpty() || catalog == null) {
            return Optional.empty();
        }
        if (z != 0) {
            addWarning(warnings, "z=" + z + " visit not fully supported; using z=0 mapgen for " + omtId);
        }

        final List<JsonMapgenDefinition> candidates = new ArrayList<>();
        for (final JsonMapgenDefinition definition
            : OterMapgenIndex.candidatesForOmt(omtId, registry, catalog)) {
            if (definition.isJsonPreviewSupported()) {
                candidates.add(definition);
            } else if (!"json".equals(definition.getMethod())) {
                addWarning(warnings, "skipped non-json mapgen method '" + definition.getMethod() + "' for " + omtId);
            }
        }

        if (candidates.isEmpty()) {
            warnNonJsonMapgenRefs(omtId, registry, warnings);
            addWarning(warnings, "no runnable json mapgen candidates for " + omtId);
            return Optional.empty();
        }
        if (candidates.size() == 1) {
            return Optional.of(candidates.get(0));
        }
        return pickWeighted(candidates, rng);
    }

    private static Optional<JsonMapgenDefinition> pickWeighted(
        final List<JsonMapgenDefinition> candidates,
        final Random rng
    ) {
        int totalWeight = 0;
        for (final JsonMapgenDefinition definition : candidates) {
            totalWeight += Math.max(1, definition.getWeight());
        }
        int roll = rng == null ? 0 : rng.nextInt(totalWeight);
        for (final JsonMapgenDefinition definition : candidates) {
            roll -= Math.max(1, definition.getWeight());
            if (roll < 0) {
                return Optional.of(definition);
            }
        }
        return Optional.of(candidates.get(candidates.size() - 1));
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null && !warnings.contains(message)) {
            warnings.add(message);
        }
    }

    private static void warnNonJsonMapgenRefs(
        final String omtId,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) {
        if (registry == null || omtId == null || omtId.isEmpty()) {
            return;
        }
        final Set<String> warnedMethods = new HashSet<>();
        warnNonJsonRefsForTerrain(registry.find(omtId), omtId, warnings, warnedMethods);
        final String stripped = OvermapTerrainResolver.stripRotation(omtId);
        if (!stripped.equals(omtId)) {
            warnNonJsonRefsForTerrain(registry.find(stripped), omtId, warnings, warnedMethods);
        }
    }

    private static void warnNonJsonRefsForTerrain(
        final Optional<OvermapTerrainDefinition> terrain,
        final String omtId,
        final List<String> warnings,
        final Set<String> warnedMethods
    ) {
        if (!terrain.isPresent()) {
            return;
        }
        for (final MapgenRef ref : terrain.get().getMapgenRefs()) {
            if (ref.isJsonMethod()) {
                continue;
            }
            if (warnedMethods.add(ref.getMethod())) {
                addWarning(warnings, "skipped non-json mapgen method '" + ref.getMethod() + "' for " + omtId);
            }
        }
    }
}
