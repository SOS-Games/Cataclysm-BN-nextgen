package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.MapgenRef;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Joins OMT ids with runnable json mapgen catalog entries (W3). */
public final class OterMapgenIndex {

    private OterMapgenIndex() {}

    public static List<JsonMapgenDefinition> candidatesForOmt(
        final String omtId,
        final OvermapTerrainRegistry registry,
        final MapgenCatalog catalog
    ) {
        if (omtId == null || omtId.isEmpty() || catalog == null) {
            return java.util.Collections.emptyList();
        }

        final Map<String, JsonMapgenDefinition> unique = new LinkedHashMap<>();
        addCandidatesForOmTerrainKey(catalog, omtId, unique);
        final String stripped = OvermapTerrainResolver.stripRotation(omtId);
        if (!stripped.equals(omtId)) {
            addCandidatesForOmTerrainKey(catalog, stripped, unique);
        }

        if (registry != null) {
            final Optional<OvermapTerrainDefinition> direct = registry.find(omtId);
            final Optional<OvermapTerrainDefinition> strippedDef = stripped.equals(omtId)
                ? Optional.empty()
                : registry.find(stripped);
            addFromTerrainDefinition(catalog, direct.orElse(null), unique);
            if (strippedDef.isPresent() && !strippedDef.equals(direct)) {
                addFromTerrainDefinition(catalog, strippedDef.get(), unique);
            }
        }

        return new ArrayList<>(unique.values());
    }

    private static void addFromTerrainDefinition(
        final MapgenCatalog catalog,
        final OvermapTerrainDefinition terrain,
        final Map<String, JsonMapgenDefinition> unique
    ) {
        if (terrain == null) {
            return;
        }
        for (final MapgenRef ref : terrain.getMapgenRefs()) {
            if (!ref.isJsonMethod()) {
                continue;
            }
            addCandidatesForOmTerrainKey(catalog, ref.getOmTerrain(), unique);
        }
    }

    private static void addCandidatesForOmTerrainKey(
        final MapgenCatalog catalog,
        final String omTerrainKey,
        final Map<String, JsonMapgenDefinition> unique
    ) {
        if (omTerrainKey == null || omTerrainKey.isEmpty()) {
            return;
        }
        for (final JsonMapgenDefinition definition : catalog.findByOmTerrain(omTerrainKey)) {
            unique.put(definitionKey(definition), definition);
        }
    }

    private static String definitionKey(final JsonMapgenDefinition definition) {
        return definition.getSourceFile().toString() + "#" + definition.getIndexInFile();
    }
}
