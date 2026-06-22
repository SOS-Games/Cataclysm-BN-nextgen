package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingIndex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Result of {@link OvermapGenerator#generate}. */
public final class OvermapGenerateResult {

    private final OvermapGrid grid;
    private final List<String> warnings;
    private final int cityBuildingsPlaced;
    private final int staticSpecialsPlaced;
    private final int mutableSpecialsPlaced;
    private final int riverCellsCarved;
    private final int roadCellsPlaced;
    private final PlacedBuildingIndex placementIndex;

    public OvermapGenerateResult(
        final OvermapGrid grid,
        final List<String> warnings,
        final int cityBuildingsPlaced,
        final int staticSpecialsPlaced
    ) {
        this(grid, warnings, cityBuildingsPlaced, staticSpecialsPlaced, 0, 0, 0, PlacedBuildingIndex.EMPTY);
    }

    public OvermapGenerateResult(
        final OvermapGrid grid,
        final List<String> warnings,
        final int cityBuildingsPlaced,
        final int staticSpecialsPlaced,
        final int riverCellsCarved,
        final int roadCellsPlaced
    ) {
        this(grid, warnings, cityBuildingsPlaced, staticSpecialsPlaced, 0, riverCellsCarved, roadCellsPlaced,
            PlacedBuildingIndex.EMPTY);
    }

    public OvermapGenerateResult(
        final OvermapGrid grid,
        final List<String> warnings,
        final int cityBuildingsPlaced,
        final int staticSpecialsPlaced,
        final int mutableSpecialsPlaced,
        final int riverCellsCarved,
        final int roadCellsPlaced
    ) {
        this(
            grid,
            warnings,
            cityBuildingsPlaced,
            staticSpecialsPlaced,
            mutableSpecialsPlaced,
            riverCellsCarved,
            roadCellsPlaced,
            PlacedBuildingIndex.EMPTY
        );
    }

    public OvermapGenerateResult(
        final OvermapGrid grid,
        final List<String> warnings,
        final int cityBuildingsPlaced,
        final int staticSpecialsPlaced,
        final int mutableSpecialsPlaced,
        final int riverCellsCarved,
        final int roadCellsPlaced,
        final PlacedBuildingIndex placementIndex
    ) {
        this.grid = grid;
        this.warnings = warnings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(warnings));
        this.cityBuildingsPlaced = cityBuildingsPlaced;
        this.staticSpecialsPlaced = staticSpecialsPlaced;
        this.mutableSpecialsPlaced = mutableSpecialsPlaced;
        this.riverCellsCarved = riverCellsCarved;
        this.roadCellsPlaced = roadCellsPlaced;
        this.placementIndex = placementIndex == null ? PlacedBuildingIndex.EMPTY : placementIndex;
    }

    public OvermapGrid getGrid() {
        return grid;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getCityBuildingsPlaced() {
        return cityBuildingsPlaced;
    }

    public int getStaticSpecialsPlaced() {
        return staticSpecialsPlaced;
    }

    public int getMutableSpecialsPlaced() {
        return mutableSpecialsPlaced;
    }

    public int getRiverCellsCarved() {
        return riverCellsCarved;
    }

    public int getRoadCellsPlaced() {
        return roadCellsPlaced;
    }

    public PlacedBuildingIndex getPlacementIndex() {
        return placementIndex;
    }
}
