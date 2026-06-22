package io.gdx.cdda.bn.nextgen.worldgen.placement;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.worldgen.generate.BuildingFootprint;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Maps OMT grid cells to the building placement that owns them (W7). */
public final class PlacedBuildingIndex {

    public static final PlacedBuildingIndex EMPTY = new PlacedBuildingIndex(Collections.emptyMap());

    private final Map<Long, PlacedBuildingRecord> cellToRecord;

    private PlacedBuildingIndex(final Map<Long, PlacedBuildingRecord> cellToRecord) {
        this.cellToRecord = cellToRecord;
    }

    public static PlacedBuildingIndex fromRecords(
        final List<PlacedBuildingRecord> records,
        final List<String> warnings
    ) {
        if (records == null || records.isEmpty()) {
            return EMPTY;
        }
        final Map<Long, PlacedBuildingRecord> index = new HashMap<>();
        for (final PlacedBuildingRecord record : records) {
            if (record == null || record.getDefinition() == null) {
                continue;
            }
            final BuildingFootprint footprint = BuildingFootprint.atZ(record.getDefinition(), 0);
            for (final CityBuildingPiece piece : footprint.getPieces()) {
                final int cellX = record.getAnchorX() + piece.getOffsetX();
                final int cellY = record.getAnchorY() + piece.getOffsetY();
                final long key = cellKey(cellX, cellY);
                final PlacedBuildingRecord previous = index.put(key, record);
                if (previous != null && previous != record && warnings != null) {
                    warnings.add("overlapping building placement at (" + cellX + "," + cellY + "): "
                        + previous.getBuildingId() + " vs " + record.getBuildingId());
                }
            }
        }
        return index.isEmpty() ? EMPTY : new PlacedBuildingIndex(Collections.unmodifiableMap(index));
    }

    public Optional<PlacedBuildingRecord> findAt(final int omtX, final int omtY) {
        final PlacedBuildingRecord record = cellToRecord.get(cellKey(omtX, omtY));
        return Optional.ofNullable(record);
    }

    public boolean isEmpty() {
        return cellToRecord.isEmpty();
    }

    public int cellCount() {
        return cellToRecord.size();
    }

    private static long cellKey(final int x, final int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }
}
