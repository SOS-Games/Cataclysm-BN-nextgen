package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/** P7b — infers multi-floor bundles from co-located mapgens with shared id prefixes. */
public final class MapgenFileBundleInferrer {

    private static final int MIN_PREFIX_LENGTH = 4;

    private static final List<String> ROOF_SUFFIXES = Arrays.asList("roof", "rooftop");
    private static final List<String> BASE_SUFFIXES = Arrays.asList("base", "ground", "ground_floor");
    private static final List<String> BASEMENT_SUFFIXES = Arrays.asList("basement", "cellar", "underground");
    private static final List<String> UPPER_SUFFIXES = Arrays.asList(
        "second",
        "upper",
        "loft",
        "third",
        "fourth",
        "attic"
    );

    private MapgenFileBundleInferrer() {}

    public static CityBuildingRegistry augment(
        final CityBuildingRegistry explicitBuildings,
        final MapgenCatalog catalog
    ) {
        if (catalog == null) {
            return explicitBuildings == null ? CityBuildingRegistry.empty() : explicitBuildings;
        }

        final CityBuildingRegistry base = explicitBuildings == null
            ? CityBuildingRegistry.empty()
            : explicitBuildings;
        final Map<String, CityBuildingDefinition> byId = new LinkedHashMap<>();
        for (final CityBuildingDefinition building : base.all()) {
            byId.put(building.getId(), building);
        }

        final Set<String> claimedOmTerrains = new HashSet<>();
        for (final CityBuildingDefinition building : base.all()) {
            for (final CityBuildingPiece piece : building.getPieces()) {
                claimOmTerrain(claimedOmTerrains, piece.getOvermapId());
                claimOmTerrain(claimedOmTerrains, OvermapTerrainResolver.stripRotation(piece.getOvermapId()));
                OvermapTerrainResolver.resolvedOmTerrain(catalog, piece.getOvermapId())
                    .ifPresent(claimedOmTerrains::add);
            }
        }

        final List<String> warnings = new ArrayList<>(base.getWarnings());
        final Map<Path, List<JsonMapgenDefinition>> byFile = groupRunnableByFile(catalog);
        for (final Map.Entry<Path, List<JsonMapgenDefinition>> entry : byFile.entrySet()) {
            inferFromFile(entry.getKey(), entry.getValue(), byId, claimedOmTerrains, catalog, warnings);
        }

        return new CityBuildingRegistry(byId, warnings).withOmTerrainIndex(catalog);
    }

    private static void inferFromFile(
        final Path sourceFile,
        final List<JsonMapgenDefinition> definitions,
        final Map<String, CityBuildingDefinition> byId,
        final Set<String> claimedOmTerrains,
        final MapgenCatalog catalog,
        final List<String> warnings
    ) {
        if (definitions.size() < 2) {
            return;
        }

        final List<String> omTerrainIds = new ArrayList<>();
        for (final JsonMapgenDefinition definition : definitions) {
            if (definition.getOmTerrainGrid().isPresent()) {
                return;
            }
            final String primary = primaryOmTerrain(definition);
            if (primary.isEmpty()) {
                return;
            }
            if (!catalog.findFirstRunnableByOmTerrain(primary).isPresent()) {
                return;
            }
            if (claimedOmTerrains.contains(primary)) {
                return;
            }
            omTerrainIds.add(primary);
        }

        final String prefix = normalizeBundlePrefix(longestCommonPrefix(omTerrainIds), omTerrainIds);
        if (prefix.isEmpty()) {
            return;
        }

        final String buildingId = prefix.endsWith("_")
            ? prefix.substring(0, prefix.length() - 1)
            : prefix;
        if (buildingId.length() < MIN_PREFIX_LENGTH) {
            return;
        }
        if (byId.containsKey(buildingId)) {
            return;
        }

        final List<FloorCandidate> candidates = new ArrayList<>();
        for (int index = 0; index < definitions.size(); index++) {
            final JsonMapgenDefinition definition = definitions.get(index);
            final String omTerrain = primaryOmTerrain(definition);
            final String suffix = omTerrain.substring(prefix.length());
            candidates.add(new FloorCandidate(omTerrain, suffix));
        }

        final List<CityBuildingPiece> pieces = assignFloorPieces(candidates);
        if (pieces == null || pieces.size() < 2) {
            return;
        }

        final CityBuildingDefinition building = new CityBuildingDefinition(buildingId, sourceFile, pieces);
        if (!building.isBundledBuilding()) {
            return;
        }

        byId.put(buildingId, building);
        warnings.add("inferred implicit bundle '" + buildingId + "' from " + sourceFile.getFileName());
        for (final CityBuildingPiece piece : pieces) {
            claimOmTerrain(claimedOmTerrains, piece.getOvermapId());
        }
    }

    private static List<CityBuildingPiece> assignFloorPieces(final List<FloorCandidate> candidates) {
        final Map<String, Integer> assignedZ = new HashMap<>();
        final List<FloorCandidate> roofCandidates = new ArrayList<>();

        for (final FloorCandidate candidate : candidates) {
            final OptionalInt explicitZ = resolveExplicitZ(candidate.suffix);
            if (explicitZ.isPresent()) {
                if (assignedZ.putIfAbsent(candidate.omTerrain, explicitZ.getAsInt()) != null) {
                    return null;
                }
                continue;
            }
            if (isRoofSuffix(candidate.suffix)) {
                roofCandidates.add(candidate);
                continue;
            }
            return null;
        }

        int maxZ = assignedZ.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (final FloorCandidate roof : roofCandidates) {
            maxZ += 1;
            if (assignedZ.putIfAbsent(roof.omTerrain, maxZ) != null) {
                return null;
            }
        }

        if (assignedZ.size() < 2) {
            return null;
        }

        final List<CityBuildingPiece> pieces = new ArrayList<>();
        for (final FloorCandidate candidate : candidates) {
            final Integer zLevel = assignedZ.get(candidate.omTerrain);
            if (zLevel == null) {
                return null;
            }
            pieces.add(new CityBuildingPiece(0, 0, zLevel, candidate.omTerrain));
        }
        return pieces;
    }

    private static OptionalInt resolveExplicitZ(final String suffix) {
        final String normalized = suffix.toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return OptionalInt.of(0);
        }
        if (matchesSuffix(normalized, BASEMENT_SUFFIXES)) {
            return OptionalInt.of(-1);
        }
        if (matchesSuffix(normalized, BASE_SUFFIXES)) {
            return OptionalInt.of(0);
        }
        if (matchesSuffix(normalized, UPPER_SUFFIXES)) {
            return resolveUpperFloorZ(normalized);
        }
        if (normalized.matches("\\d+")) {
            return OptionalInt.of(Integer.parseInt(normalized));
        }
        return OptionalInt.empty();
    }

    private static OptionalInt resolveUpperFloorZ(final String suffix) {
        switch (suffix) {
            case "second":
            case "upper":
            case "loft":
                return OptionalInt.of(1);
            case "third":
                return OptionalInt.of(2);
            case "fourth":
                return OptionalInt.of(3);
            case "attic":
                return OptionalInt.of(4);
            default:
                return OptionalInt.empty();
        }
    }

    private static boolean isRoofSuffix(final String suffix) {
        return matchesSuffix(suffix.toLowerCase(Locale.ROOT), ROOF_SUFFIXES);
    }

    private static boolean matchesSuffix(final String suffix, final List<String> options) {
        for (final String option : options) {
            if (suffix.equals(option)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeBundlePrefix(final String prefix, final List<String> omTerrainIds) {
        if (prefix.isEmpty()) {
            return "";
        }
        String normalized = prefix;
        if (!normalized.endsWith("_")) {
            final int lastUnderscore = normalized.lastIndexOf('_');
            if (lastUnderscore <= 0) {
                return "";
            }
            normalized = normalized.substring(0, lastUnderscore + 1);
        }
        for (final String omTerrain : omTerrainIds) {
            if (!omTerrain.startsWith(normalized)) {
                return "";
            }
        }
        if (normalized.length() < MIN_PREFIX_LENGTH) {
            return "";
        }
        return normalized;
    }

    private static String longestCommonPrefix(final List<String> values) {
        if (values.isEmpty()) {
            return "";
        }
        String prefix = values.get(0);
        for (int index = 1; index < values.size(); index++) {
            final String value = values.get(index);
            while (!value.startsWith(prefix)) {
                if (prefix.isEmpty()) {
                    return "";
                }
                prefix = prefix.substring(0, prefix.length() - 1);
            }
        }
        return prefix;
    }

    private static Map<Path, List<JsonMapgenDefinition>> groupRunnableByFile(final MapgenCatalog catalog) {
        final Map<Path, List<JsonMapgenDefinition>> byFile = new LinkedHashMap<>();
        for (final JsonMapgenDefinition definition : catalog.runnableOnly()) {
            byFile.computeIfAbsent(definition.getSourceFile(), ignored -> new ArrayList<>()).add(definition);
        }
        for (final List<JsonMapgenDefinition> definitions : byFile.values()) {
            definitions.sort((left, right) -> Integer.compare(left.getIndexInFile(), right.getIndexInFile()));
        }
        return byFile;
    }

    private static String primaryOmTerrain(final JsonMapgenDefinition definition) {
        if (definition.getOmTerrain().isEmpty()) {
            return "";
        }
        return OvermapTerrainResolver.stripRotation(definition.getOmTerrain().get(0));
    }

    private static void claimOmTerrain(final Set<String> claimedOmTerrains, final String omTerrain) {
        if (omTerrain != null && !omTerrain.isEmpty()) {
            claimedOmTerrains.add(omTerrain);
        }
    }

    private static final class FloorCandidate {
        private final String omTerrain;
        private final String suffix;

        private FloorCandidate(final String omTerrain, final String suffix) {
            this.omTerrain = omTerrain;
            this.suffix = suffix;
        }
    }
}
