package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.building.MutableSpecialBuildingConverter;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** One row in the mapgen picker — mapgen-backed, whole static special, or mutable special (P7c / unit 11). */
public final class MapgenPickerRow {

    private final JsonMapgenDefinition definition;
    private final CityBuildingDefinition wholeSpecial;
    private final MutableSpecialDefinition mutableSpecial;

    private MapgenPickerRow(
        final JsonMapgenDefinition definition,
        final CityBuildingDefinition wholeSpecial,
        final MutableSpecialDefinition mutableSpecial
    ) {
        this.definition = definition;
        this.wholeSpecial = wholeSpecial;
        this.mutableSpecial = mutableSpecial;
    }

    public static MapgenPickerRow forDefinition(final JsonMapgenDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        return new MapgenPickerRow(definition, null, null);
    }

    public static MapgenPickerRow forWholeSpecial(final CityBuildingDefinition building) {
        if (building == null || !building.isWholeOvermapSpecial()) {
            throw new IllegalArgumentException("whole overmap_special building is required");
        }
        return new MapgenPickerRow(null, building, null);
    }

    public static MapgenPickerRow forMutableSpecial(final MutableSpecialDefinition special) {
        if (special == null) {
            throw new IllegalArgumentException("mutable special is required");
        }
        return new MapgenPickerRow(null, null, special);
    }

    public Optional<JsonMapgenDefinition> getDefinition() {
        return Optional.ofNullable(definition);
    }

    public Optional<CityBuildingDefinition> getWholeSpecial() {
        return Optional.ofNullable(wholeSpecial);
    }

    public Optional<MutableSpecialDefinition> getMutableSpecial() {
        return Optional.ofNullable(mutableSpecial);
    }

    public boolean isWholeSpecialRow() {
        return wholeSpecial != null;
    }

    public boolean isMutableSpecialRow() {
        return mutableSpecial != null;
    }

    public Optional<CityBuildingDefinition> bundledBuilding(final CityBuildingRegistry buildings) {
        return resolveImportBuilding(buildings, new Random(0L), new ArrayList<>());
    }

    public Optional<CityBuildingDefinition> resolveImportBuilding(
        final CityBuildingRegistry buildings,
        final Random rng,
        final List<String> warnings
    ) {
        if (wholeSpecial != null) {
            return wholeSpecial.isBundledBuilding() ? Optional.of(wholeSpecial) : Optional.empty();
        }
        if (mutableSpecial != null) {
            return MutableSpecialBuildingConverter.assembleBuilding(mutableSpecial, rng, warnings);
        }
        return MapgenPickerIndex.findBuildingForDefinition(definition, buildings)
            .filter(CityBuildingDefinition::isBundledBuilding);
    }
}
