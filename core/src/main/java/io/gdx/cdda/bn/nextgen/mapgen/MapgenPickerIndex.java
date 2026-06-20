package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/** Pre-built mapgen picker rows (filter + bundled-building collapse). */
public final class MapgenPickerIndex {

    private static final Comparator<JsonMapgenDefinition> SORT_ORDER = Comparator
        .comparing((JsonMapgenDefinition def) -> primaryLabel(def).toLowerCase(Locale.ROOT))
        .thenComparing(def -> def.getSourceFile().toString())
        .thenComparingInt(JsonMapgenDefinition::getIndexInFile);

    private final List<MapgenPickerRow> rows;
    private final CityBuildingRegistry buildings;

    private MapgenPickerIndex(
        final List<MapgenPickerRow> rows,
        final CityBuildingRegistry buildings
    ) {
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.buildings = buildings;
    }

    public static MapgenPickerIndex build(final MapgenCatalog catalog, final CityBuildingRegistry buildings) {
        if (catalog == null) {
            return new MapgenPickerIndex(Collections.emptyList(), CityBuildingRegistry.empty());
        }
        final CityBuildingRegistry indexed = buildings == null ? CityBuildingRegistry.empty() : buildings;
        return new MapgenPickerIndex(buildRows(catalog, indexed), indexed);
    }

    public List<MapgenPickerRow> all() {
        return rows;
    }

    public List<MapgenPickerRow> filter(final String query, final MapgenCatalog catalog) {
        if (catalog == null) {
            return Collections.emptyList();
        }
        if (query == null || query.trim().isEmpty()) {
            return rows;
        }
        final String normalized = query.trim().toLowerCase(Locale.ROOT);
        final Set<JsonMapgenDefinition> filteredDefinitions = new HashSet<>(catalog.filter(query));
        final List<MapgenPickerRow> filtered = new ArrayList<>();
        for (final MapgenPickerRow row : rows) {
            if (matchesFilter(row, normalized, filteredDefinitions, buildings)) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    public static String formatRow(
        final MapgenPickerRow row,
        final CityBuildingRegistry buildings
    ) {
        if (row == null) {
            return "";
        }
        if (row.isWholeSpecialRow()) {
            return formatWholeSpecialRow(row.getWholeSpecial().orElseThrow());
        }
        return formatRow(row.getDefinition().orElseThrow(), buildings);
    }

    public static String primaryLabel(final JsonMapgenDefinition definition) {
        if (!definition.getOmTerrain().isEmpty()) {
            return definition.getOmTerrain().get(0);
        }
        return definition.displayName();
    }

    public static String formatRow(
        final JsonMapgenDefinition definition,
        final CityBuildingRegistry buildings
    ) {
        final StringBuilder line = new StringBuilder();
        final Optional<CityBuildingDefinition> building = findBuildingForDefinition(definition, buildings);
        if (building.isPresent() && building.get().isBundledBuilding()) {
            line.append(building.get().getId());
        } else {
            line.append(primaryLabel(definition));
        }
        final String summary = building.map(CityBuildingDefinition::buildingSummaryLabel).orElse("");
        if (!summary.isEmpty()) {
            line.append(" (").append(summary).append(')');
        }
        line.append("   ");
        line.append(definition.getSourceFile().getFileName());
        line.append(" #").append(definition.getIndexInFile());
        if (definition.getWeight() != 1000) {
            line.append("   w:").append(definition.getWeight());
        }
        if (!definition.isJsonPreviewSupported()) {
            line.append("   (unsupported)");
        }
        return line.toString();
    }

    private static String formatWholeSpecialRow(final CityBuildingDefinition building) {
        final StringBuilder line = new StringBuilder();
        line.append(building.getId());
        final String summary = building.buildingSummaryLabel();
        if (!summary.isEmpty()) {
            line.append(" (").append(summary).append(')');
        }
        line.append("   ");
        if (building.getSourceFile() != null) {
            line.append(building.getSourceFile().getFileName());
        }
        line.append("   overmap_special");
        return line.toString();
    }

    public static Optional<CityBuildingDefinition> findBuildingForDefinition(
        final JsonMapgenDefinition definition,
        final CityBuildingRegistry buildings
    ) {
        if (buildings == null || definition == null) {
            return Optional.empty();
        }
        for (final String omTerrain : definition.getOmTerrain()) {
            final Optional<CityBuildingDefinition> building = findBuildingForOmTerrain(omTerrain, buildings);
            if (building.isPresent()) {
                return building;
            }
        }
        return Optional.empty();
    }

    private static List<MapgenPickerRow> buildRows(
        final MapgenCatalog catalog,
        final CityBuildingRegistry buildings
    ) {
        final Set<String> hiddenColumnBundleIds = hiddenColumnBundleIds(buildings);
        final List<MapgenPickerRow> rows = new ArrayList<>();
        for (final JsonMapgenDefinition definition : collapseBundledBuildings(
            catalog.all().stream()
                .filter(JsonMapgenDefinition::isStandalonePickerEntry)
                .toList(),
            buildings,
            hiddenColumnBundleIds
        )) {
            rows.add(MapgenPickerRow.forDefinition(definition));
        }
        for (final CityBuildingDefinition building : buildings.all()) {
            if (!building.isWholeOvermapSpecial() || !building.isBundledBuilding()) {
                continue;
            }
            rows.add(MapgenPickerRow.forWholeSpecial(building));
        }
        rows.sort(Comparator.comparing(row -> rowSortKey(row, buildings).toLowerCase(Locale.ROOT)));
        return rows;
    }

    private static String rowSortKey(final MapgenPickerRow row, final CityBuildingRegistry buildings) {
        if (row.isWholeSpecialRow()) {
            return row.getWholeSpecial().map(CityBuildingDefinition::getId).orElse("");
        }
        return row.getDefinition().map(MapgenPickerIndex::primaryLabel).orElse("");
    }

    private static boolean matchesFilter(
        final MapgenPickerRow row,
        final String normalizedQuery,
        final Set<JsonMapgenDefinition> filteredDefinitions,
        final CityBuildingRegistry buildings
    ) {
        if (row.isWholeSpecialRow()) {
            final CityBuildingDefinition building = row.getWholeSpecial().orElse(null);
            if (building == null) {
                return false;
            }
            if (building.getId().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
            if (building.buildingSummaryLabel().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
            if (building.getSourceFile() != null
                && building.getSourceFile().toString().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                return true;
            }
            for (final String stackId : columnStackBundleIds(building)) {
                if (stackId.toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                    return true;
                }
            }
            return false;
        }
        final JsonMapgenDefinition definition = row.getDefinition().orElse(null);
        if (definition == null) {
            return false;
        }
        if (filteredDefinitions.contains(definition)) {
            return true;
        }
        return findBuildingForDefinition(definition, buildings)
            .map(CityBuildingDefinition::getId)
            .orElse("")
            .toLowerCase(Locale.ROOT)
            .contains(normalizedQuery);
    }

    private static List<JsonMapgenDefinition> collapseBundledBuildings(
        final List<JsonMapgenDefinition> definitions,
        final CityBuildingRegistry buildings,
        final Set<String> hiddenColumnBundleIds
    ) {
        final List<JsonMapgenDefinition> sorted = new ArrayList<>(definitions);
        sorted.sort(SORT_ORDER);

        final List<JsonMapgenDefinition> collapsed = new ArrayList<>();
        final Set<String> seenBuildingIds = new HashSet<>();
        for (final JsonMapgenDefinition definition : sorted) {
            final Optional<CityBuildingDefinition> building = findBuildingForDefinition(definition, buildings);
            if (building.isPresent() && building.get().isBundledBuilding()) {
                final String buildingId = building.get().getId();
                if (hiddenColumnBundleIds.contains(buildingId)) {
                    continue;
                }
                if (seenBuildingIds.contains(buildingId)) {
                    continue;
                }
                seenBuildingIds.add(buildingId);
            }
            collapsed.add(definition);
        }
        return collapsed;
    }

    private static Set<String> hiddenColumnBundleIds(final CityBuildingRegistry buildings) {
        final Set<String> hidden = new HashSet<>();
        for (final CityBuildingDefinition building : buildings.all()) {
            if (!building.isWholeOvermapSpecial()) {
                continue;
            }
            hidden.addAll(columnStackBundleIds(building));
        }
        return hidden;
    }

    private static Set<String> columnStackBundleIds(final CityBuildingDefinition whole) {
        final Map<String, List<CityBuildingPiece>> stacksByColumn = new LinkedHashMap<>();
        for (final CityBuildingPiece piece : whole.getPieces()) {
            final String key = piece.getOffsetX() + "," + piece.getOffsetY();
            stacksByColumn.computeIfAbsent(key, ignored -> new ArrayList<>()).add(piece);
        }

        final Set<String> ids = new HashSet<>();
        for (final List<CityBuildingPiece> stack : stacksByColumn.values()) {
            final TreeSet<Integer> zLevels = new TreeSet<>();
            for (final CityBuildingPiece piece : stack) {
                zLevels.add(piece.getZLevel());
            }
            if (zLevels.size() <= 1) {
                continue;
            }
            stack.stream()
                .min(Comparator
                    .comparingInt(CityBuildingPiece::getZLevel)
                    .thenComparing(CityBuildingPiece::getOvermapId))
                .ifPresent(ground -> ids.add(OvermapTerrainResolver.stripRotation(ground.getOvermapId())));
        }
        return ids;
    }

    private static Optional<CityBuildingDefinition> findBuildingForOmTerrain(
        final String omTerrain,
        final CityBuildingRegistry buildings
    ) {
        if (omTerrain == null || omTerrain.isEmpty()) {
            return Optional.empty();
        }
        Optional<CityBuildingDefinition> building = buildings.findByOmTerrain(omTerrain);
        if (building.isPresent()) {
            return building;
        }
        final String stripped = OvermapTerrainResolver.stripRotation(omTerrain);
        if (!stripped.equals(omTerrain)) {
            building = buildings.findByOmTerrain(stripped);
        }
        return building;
    }
}
