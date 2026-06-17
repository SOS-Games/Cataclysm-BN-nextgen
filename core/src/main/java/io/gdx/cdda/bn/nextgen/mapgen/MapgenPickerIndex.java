package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** Pre-built mapgen picker rows (filter + bundled-building collapse). */
public final class MapgenPickerIndex {

    private static final Comparator<JsonMapgenDefinition> SORT_ORDER = Comparator
        .comparing((JsonMapgenDefinition def) -> primaryLabel(def).toLowerCase(Locale.ROOT))
        .thenComparing(def -> def.getSourceFile().toString())
        .thenComparingInt(JsonMapgenDefinition::getIndexInFile);

    private final List<JsonMapgenDefinition> collapsedEntries;
    private final CityBuildingRegistry buildings;

    private MapgenPickerIndex(
        final List<JsonMapgenDefinition> collapsedEntries,
        final CityBuildingRegistry buildings
    ) {
        this.collapsedEntries = Collections.unmodifiableList(collapsedEntries);
        this.buildings = buildings;
    }

    public static MapgenPickerIndex build(final MapgenCatalog catalog, final CityBuildingRegistry buildings) {
        if (catalog == null) {
            return new MapgenPickerIndex(Collections.emptyList(), CityBuildingRegistry.empty());
        }
        final CityBuildingRegistry indexed = buildings == null ? CityBuildingRegistry.empty() : buildings;
        return new MapgenPickerIndex(collapseBundledBuildings(catalog.all(), indexed), indexed);
    }

    public List<JsonMapgenDefinition> all() {
        return collapsedEntries;
    }

    public List<JsonMapgenDefinition> filter(final String query, final MapgenCatalog catalog) {
        if (catalog == null) {
            return Collections.emptyList();
        }
        if (query == null || query.trim().isEmpty()) {
            return collapsedEntries;
        }
        final List<JsonMapgenDefinition> filtered = catalog.filter(query);
        return collapseBundledBuildings(filtered, buildings);
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

    private static List<JsonMapgenDefinition> collapseBundledBuildings(
        final List<JsonMapgenDefinition> definitions,
        final CityBuildingRegistry buildings
    ) {
        final List<JsonMapgenDefinition> sorted = new ArrayList<>(definitions);
        sorted.sort(SORT_ORDER);

        final List<JsonMapgenDefinition> collapsed = new ArrayList<>();
        final Set<String> seenBuildingIds = new HashSet<>();
        for (final JsonMapgenDefinition definition : sorted) {
            final Optional<CityBuildingDefinition> building = findBuildingForDefinition(definition, buildings);
            if (building.isPresent() && building.get().isBundledBuilding()) {
                if (seenBuildingIds.contains(building.get().getId())) {
                    continue;
                }
                seenBuildingIds.add(building.get().getId());
            }
            collapsed.add(definition);
        }
        return collapsed;
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
