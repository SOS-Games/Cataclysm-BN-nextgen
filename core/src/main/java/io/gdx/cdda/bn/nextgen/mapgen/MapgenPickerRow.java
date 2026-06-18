package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;

import java.util.Optional;

/** One row in the mapgen picker — mapgen-backed or whole {@code overmap_special} (P7c). */
public final class MapgenPickerRow {

    private final JsonMapgenDefinition definition;
    private final CityBuildingDefinition wholeSpecial;

    private MapgenPickerRow(
        final JsonMapgenDefinition definition,
        final CityBuildingDefinition wholeSpecial
    ) {
        this.definition = definition;
        this.wholeSpecial = wholeSpecial;
    }

    public static MapgenPickerRow forDefinition(final JsonMapgenDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        return new MapgenPickerRow(definition, null);
    }

    public static MapgenPickerRow forWholeSpecial(final CityBuildingDefinition building) {
        if (building == null || !building.isWholeOvermapSpecial()) {
            throw new IllegalArgumentException("whole overmap_special building is required");
        }
        return new MapgenPickerRow(null, building);
    }

    public Optional<JsonMapgenDefinition> getDefinition() {
        return Optional.ofNullable(definition);
    }

    public Optional<CityBuildingDefinition> getWholeSpecial() {
        return Optional.ofNullable(wholeSpecial);
    }

    public boolean isWholeSpecialRow() {
        return wholeSpecial != null;
    }

    public Optional<CityBuildingDefinition> bundledBuilding(final CityBuildingRegistry buildings) {
        if (wholeSpecial != null) {
            return wholeSpecial.isBundledBuilding() ? Optional.of(wholeSpecial) : Optional.empty();
        }
        return MapgenPickerIndex.findBuildingForDefinition(definition, buildings)
            .filter(CityBuildingDefinition::isBundledBuilding);
    }
}
