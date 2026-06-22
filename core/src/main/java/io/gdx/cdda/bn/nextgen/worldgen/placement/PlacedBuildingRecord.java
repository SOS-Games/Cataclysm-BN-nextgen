package io.gdx.cdda.bn.nextgen.worldgen.placement;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.worldgen.generate.BuildingFootprint;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.AssembledSpecialLayout;

import java.util.Optional;

/** One successful building blit on the overmap grid (W7, W11d mutable layout). */
public final class PlacedBuildingRecord {

    private final String buildingId;
    private final int anchorX;
    private final int anchorY;
    private final CityBuildingDefinition definition;
    private final PlacementSource source;
    private final AssembledSpecialLayout mutableLayout;

    public PlacedBuildingRecord(
        final String buildingId,
        final int anchorX,
        final int anchorY,
        final CityBuildingDefinition definition,
        final PlacementSource source
    ) {
        this(buildingId, anchorX, anchorY, definition, source, null);
    }

    public PlacedBuildingRecord(
        final String buildingId,
        final int anchorX,
        final int anchorY,
        final CityBuildingDefinition definition,
        final PlacementSource source,
        final AssembledSpecialLayout mutableLayout
    ) {
        this.buildingId = buildingId == null ? "" : buildingId;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        this.definition = definition;
        this.source = source == null ? PlacementSource.CITY : source;
        this.mutableLayout = mutableLayout;
    }

    public static PlacedBuildingRecord of(
        final CityBuildingDefinition definition,
        final int anchorX,
        final int anchorY,
        final PlacementSource source
    ) {
        final String id = definition == null ? "" : definition.getId();
        return new PlacedBuildingRecord(id, anchorX, anchorY, definition, source);
    }

    public static PlacedBuildingRecord ofMutable(
        final CityBuildingDefinition definition,
        final int anchorX,
        final int anchorY,
        final AssembledSpecialLayout mutableLayout
    ) {
        final String id = definition == null ? "" : definition.getId();
        return new PlacedBuildingRecord(id, anchorX, anchorY, definition, PlacementSource.MUTABLE, mutableLayout);
    }

    public String getBuildingId() {
        return buildingId;
    }

    public int getAnchorX() {
        return anchorX;
    }

    public int getAnchorY() {
        return anchorY;
    }

    public CityBuildingDefinition getDefinition() {
        return definition;
    }

    public PlacementSource getSource() {
        return source;
    }

    public Optional<AssembledSpecialLayout> getMutableLayout() {
        return Optional.ofNullable(mutableLayout);
    }

    public boolean contains(final int omtX, final int omtY) {
        if (definition == null) {
            return false;
        }
        final BuildingFootprint footprint = BuildingFootprint.atZ(definition, 0);
        for (final CityBuildingPiece piece : footprint.getPieces()) {
            if (anchorX + piece.getOffsetX() == omtX && anchorY + piece.getOffsetY() == omtY) {
                return true;
            }
        }
        return false;
    }
}
