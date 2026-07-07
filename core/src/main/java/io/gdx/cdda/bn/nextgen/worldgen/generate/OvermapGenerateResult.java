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
    private final int urbanOmtsPlaced;
    private final int localRoadCellsPlaced;
    private final int forestTrailCellsPlaced;
    private final int undergroundCellsPlaced;
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
        this(
            grid,
            warnings,
            cityBuildingsPlaced,
            staticSpecialsPlaced,
            mutableSpecialsPlaced,
            riverCellsCarved,
            roadCellsPlaced,
            placementIndex,
            0
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
        final PlacedBuildingIndex placementIndex,
        final int urbanOmtsPlaced
    ) {
        this(
            grid,
            warnings,
            cityBuildingsPlaced,
            staticSpecialsPlaced,
            mutableSpecialsPlaced,
            riverCellsCarved,
            roadCellsPlaced,
            placementIndex,
            urbanOmtsPlaced,
            0
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
        final PlacedBuildingIndex placementIndex,
        final int urbanOmtsPlaced,
        final int localRoadCellsPlaced
    ) {
        this(
            grid,
            warnings,
            cityBuildingsPlaced,
            staticSpecialsPlaced,
            mutableSpecialsPlaced,
            riverCellsCarved,
            roadCellsPlaced,
            placementIndex,
            urbanOmtsPlaced,
            localRoadCellsPlaced,
            0
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
        final PlacedBuildingIndex placementIndex,
        final int urbanOmtsPlaced,
        final int localRoadCellsPlaced,
        final int forestTrailCellsPlaced
    ) {
        this(
            grid,
            warnings,
            cityBuildingsPlaced,
            staticSpecialsPlaced,
            mutableSpecialsPlaced,
            riverCellsCarved,
            roadCellsPlaced,
            placementIndex,
            urbanOmtsPlaced,
            localRoadCellsPlaced,
            forestTrailCellsPlaced,
            0
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
        final PlacedBuildingIndex placementIndex,
        final int urbanOmtsPlaced,
        final int localRoadCellsPlaced,
        final int forestTrailCellsPlaced,
        final int undergroundCellsPlaced
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
        this.urbanOmtsPlaced = Math.max(0, urbanOmtsPlaced);
        this.localRoadCellsPlaced = Math.max(0, localRoadCellsPlaced);
        this.forestTrailCellsPlaced = Math.max(0, forestTrailCellsPlaced);
        this.undergroundCellsPlaced = Math.max(0, undergroundCellsPlaced);
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

    public int getUrbanOmtsPlaced() {
        return urbanOmtsPlaced;
    }

    public int getLocalRoadCellsPlaced() {
        return localRoadCellsPlaced;
    }

    public int getForestTrailCellsPlaced() {
        return forestTrailCellsPlaced;
    }

    public int getUndergroundCellsPlaced() {
        return undergroundCellsPlaced;
    }

    public PlacedBuildingIndex getPlacementIndex() {
        return placementIndex;
    }
}
