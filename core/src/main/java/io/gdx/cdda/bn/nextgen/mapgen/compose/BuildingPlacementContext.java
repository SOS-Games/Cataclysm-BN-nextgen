package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.JoinContext;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;

import java.util.Optional;

/** Per-piece neighbor/join/connection context when stitching a placed building (W13a). */
public final class BuildingPlacementContext {

    private final OvermapGrid overmap;
    private final PlacedBuildingRecord record;
    private final MutableSpecialRegistry mutableSpecials;
    private final OvermapConnectionRegistry connectionRegistry;

    public BuildingPlacementContext(
        final OvermapGrid overmap,
        final PlacedBuildingRecord record,
        final MutableSpecialRegistry mutableSpecials,
        final OvermapConnectionRegistry connectionRegistry
    ) {
        this.overmap = overmap;
        this.record = record;
        this.mutableSpecials = mutableSpecials;
        this.connectionRegistry = connectionRegistry;
    }

    public JsonMapgenRunOptions forPiece(
        final CityBuildingPiece piece,
        final JsonMapgenRunOptions base,
        final int omtRotation
    ) {
        final JsonMapgenRunOptions options = base == null ? new JsonMapgenRunOptions() : base;
        if (overmap == null || record == null || piece == null) {
            return options.deriveWithOmtRotation(omtRotation);
        }
        final MutableSpecialDefinition definition = mutableSpecials == null
            ? null
            : mutableSpecials.find(record.getBuildingId()).orElse(null);
        final JoinContext context = JoinContext.fromPlacement(
            overmap,
            record,
            record.getAnchorX() + piece.getOffsetX(),
            record.getAnchorY() + piece.getOffsetY(),
            definition,
            connectionRegistry
        );
        return options.deriveWithOmtRotation(omtRotation)
            .withNeighborsByDirection(context.getNeighborsByDirection())
            .withActiveJoins(context.getActiveJoins())
            .withConnectionsByDirection(context.getConnectionsByDirection());
    }

    public Optional<PlacedBuildingRecord> getRecord() {
        return Optional.ofNullable(record);
    }
}
