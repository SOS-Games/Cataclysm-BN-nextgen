package io.gdx.cdda.bn.nextgen.mapgen.json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Index of discovered json mapgen definitions (P2). */
public final class MapgenCatalog {

    private final List<JsonMapgenDefinition> definitions;
    private final Map<String, List<JsonMapgenDefinition>> byOmTerrain;
    private final Map<String, List<JsonMapgenDefinition>> byNestedMapgenId;
    private final Map<String, List<JsonMapgenDefinition>> byUpdateMapgenId;

    public MapgenCatalog(final List<JsonMapgenDefinition> definitions) {
        this.definitions = Collections.unmodifiableList(new ArrayList<>(definitions));
        this.byOmTerrain = buildOmTerrainIndex(definitions);
        this.byNestedMapgenId = buildNestedIndex(definitions);
        this.byUpdateMapgenId = buildUpdateIndex(definitions);
    }

    public List<JsonMapgenDefinition> all() {
        return definitions;
    }

    public List<JsonMapgenDefinition> runnableOnly() {
        final List<JsonMapgenDefinition> runnable = new ArrayList<>();
        for (final JsonMapgenDefinition definition : definitions) {
            if (definition.isJsonPreviewSupported()) {
                runnable.add(definition);
            }
        }
        return Collections.unmodifiableList(runnable);
    }

    public List<JsonMapgenDefinition> filter(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return definitions;
        }
        final String needle = query.trim().toLowerCase(Locale.ROOT);
        return definitions.stream()
            .filter(def -> matches(def, needle))
            .collect(Collectors.toList());
    }

    public List<JsonMapgenDefinition> findByOmTerrain(final String omTerrainId) {
        if (omTerrainId == null || omTerrainId.isEmpty()) {
            return Collections.emptyList();
        }
        final List<JsonMapgenDefinition> matches = byOmTerrain.get(omTerrainId);
        return matches == null ? Collections.emptyList() : Collections.unmodifiableList(matches);
    }

    public List<JsonMapgenDefinition> fromFile(final Path sourceFile) {
        if (sourceFile == null) {
            return Collections.emptyList();
        }
        final List<JsonMapgenDefinition> fromFile = new ArrayList<>();
        for (final JsonMapgenDefinition definition : definitions) {
            if (sourceFile.equals(definition.getSourceFile())) {
                fromFile.add(definition);
            }
        }
        return Collections.unmodifiableList(fromFile);
    }

    public Optional<JsonMapgenDefinition> findExact(
        final String omTerrainId,
        final Path sourceFile,
        final int indexInFile
    ) {
        for (final JsonMapgenDefinition definition : definitions) {
            if (definition.getIndexInFile() != indexInFile) {
                continue;
            }
            if (!sourceFile.equals(definition.getSourceFile())) {
                continue;
            }
            if (omTerrainId != null && !definition.getOmTerrain().contains(omTerrainId)) {
                continue;
            }
            return Optional.of(definition);
        }
        return Optional.empty();
    }

    public Optional<JsonMapgenDefinition> findFirstRunnableByOmTerrain(final String omTerrainId) {
        for (final JsonMapgenDefinition definition : findByOmTerrain(omTerrainId)) {
            if (definition.isJsonPreviewSupported()) {
                return Optional.of(definition);
            }
        }
        return Optional.empty();
    }

    public List<JsonMapgenDefinition> findByNestedMapgenId(final String nestedMapgenId) {
        if (nestedMapgenId == null || nestedMapgenId.isEmpty()) {
            return Collections.emptyList();
        }
        final List<JsonMapgenDefinition> matches = byNestedMapgenId.get(nestedMapgenId);
        return matches == null ? Collections.emptyList() : Collections.unmodifiableList(matches);
    }

    public Optional<JsonMapgenDefinition> pickNestedMapgen(
        final String nestedMapgenId,
        final java.util.Random rng
    ) {
        return pickWeighted(findByNestedMapgenId(nestedMapgenId), rng);
    }

    public List<JsonMapgenDefinition> findByUpdateMapgenId(final String updateMapgenId) {
        if (updateMapgenId == null || updateMapgenId.isEmpty()) {
            return Collections.emptyList();
        }
        final List<JsonMapgenDefinition> matches = byUpdateMapgenId.get(updateMapgenId);
        return matches == null ? Collections.emptyList() : Collections.unmodifiableList(matches);
    }

    public Optional<JsonMapgenDefinition> pickUpdateMapgen(
        final String updateMapgenId,
        final java.util.Random rng
    ) {
        return pickWeighted(findByUpdateMapgenId(updateMapgenId), rng);
    }

    public int size() {
        return definitions.size();
    }

    private static boolean matches(final JsonMapgenDefinition definition, final String needle) {
        for (final String omTerrain : definition.getOmTerrain()) {
            if (omTerrain.toLowerCase(Locale.ROOT).contains(needle)) {
                return true;
            }
        }
        final String path = definition.getSourceFile().toString().toLowerCase(Locale.ROOT);
        return path.contains(needle) || definition.displayName().toLowerCase(Locale.ROOT).contains(needle);
    }

    private static Map<String, List<JsonMapgenDefinition>> buildOmTerrainIndex(
        final List<JsonMapgenDefinition> definitions
    ) {
        final Map<String, List<JsonMapgenDefinition>> index = new HashMap<>();
        for (final JsonMapgenDefinition definition : definitions) {
            for (final String omTerrain : definition.getOmTerrain()) {
                index.computeIfAbsent(omTerrain, ignored -> new ArrayList<>()).add(definition);
            }
        }
        return index;
    }

    private static Map<String, List<JsonMapgenDefinition>> buildNestedIndex(
        final List<JsonMapgenDefinition> definitions
    ) {
        final Map<String, List<JsonMapgenDefinition>> index = new HashMap<>();
        for (final JsonMapgenDefinition definition : definitions) {
            definition.getNestedMapgenId().ifPresent(id ->
                index.computeIfAbsent(id, ignored -> new ArrayList<>()).add(definition)
            );
        }
        return index;
    }

    private static Map<String, List<JsonMapgenDefinition>> buildUpdateIndex(
        final List<JsonMapgenDefinition> definitions
    ) {
        final Map<String, List<JsonMapgenDefinition>> index = new HashMap<>();
        for (final JsonMapgenDefinition definition : definitions) {
            definition.getUpdateMapgenId().ifPresent(id ->
                index.computeIfAbsent(id, ignored -> new ArrayList<>()).add(definition)
            );
        }
        return index;
    }

    private static Optional<JsonMapgenDefinition> pickWeighted(
        final List<JsonMapgenDefinition> candidates,
        final java.util.Random rng
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }
        final List<JsonMapgenDefinition> runnable = new ArrayList<>();
        for (final JsonMapgenDefinition definition : candidates) {
            if (definition.isJsonPreviewSupported()) {
                runnable.add(definition);
            }
        }
        if (runnable.isEmpty()) {
            return Optional.empty();
        }
        if (runnable.size() == 1 || rng == null) {
            return Optional.of(runnable.get(0));
        }
        int totalWeight = 0;
        for (final JsonMapgenDefinition definition : runnable) {
            totalWeight += Math.max(1, definition.getWeight());
        }
        int roll = rng.nextInt(totalWeight);
        for (final JsonMapgenDefinition definition : runnable) {
            roll -= Math.max(1, definition.getWeight());
            if (roll < 0) {
                return Optional.of(definition);
            }
        }
        return Optional.of(runnable.get(runnable.size() - 1));
    }
}
