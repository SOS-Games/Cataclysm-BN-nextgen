package io.gdx.cdda.bn.nextgen.worldgen.visit;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Maps OMT ids and visit z to building volume floors (W8). */
public final class ZLevelResolver {

    /** Internal sentinel resolved to the top volume z-level. */
    public static final int ROOF_Z = 1000;

    private ZLevelResolver() {}

    public static int visitZForOmt(final String omtId) {
        return inferFromOmtIdOptional(omtId).orElse(0);
    }

    public static int inferFromOmtId(final String omtId) {
        return inferFromOmtIdOptional(omtId).orElse(0);
    }

    public static Optional<Integer> inferFromOmtIdOptional(final String omtId) {
        if (omtId == null || omtId.isEmpty()) {
            return Optional.empty();
        }
        final String stripped = OvermapTerrainResolver.stripRotation(omtId).toLowerCase(Locale.ROOT);
        if (stripped.endsWith("_basement")) {
            return Optional.of(-1);
        }
        if (stripped.endsWith("_roof")) {
            return Optional.of(ROOF_Z);
        }
        if (stripped.endsWith("_second")) {
            return Optional.of(1);
        }
        if (stripped.endsWith("_ground") || stripped.endsWith("_first")) {
            return Optional.of(0);
        }
        return Optional.empty();
    }

    public static int activeZForVisit(
        final MapVolume volume,
        final int requestedZ,
        final String omtId,
        final Optional<CityBuildingPiece> pieceAtCell,
        final List<String> warnings
    ) {
        if (volume == null) {
            return requestedZ;
        }
        final List<Integer> levels = volume.getZLevels();
        int target;
        if (pieceAtCell.isPresent()) {
            target = pieceAtCell.get().getZLevel();
        } else {
            final Optional<Integer> inferred = inferFromOmtIdOptional(omtId);
            if (inferred.isPresent()) {
                target = resolveRoofPlaceholder(inferred.get(), levels);
            } else if (requestedZ != 0) {
                target = requestedZ;
            } else {
                target = volume.getActiveZ();
            }
        }
        return clampToVolume(volume, target, warnings);
    }

    public static Optional<CityBuildingPiece> pieceAtCell(
        final CityBuildingDefinition building,
        final PlacedBuildingRecord record,
        final int omtX,
        final int omtY,
        final String omtId,
        final int requestedZ
    ) {
        if (building == null || record == null) {
            return Optional.empty();
        }
        final String normalizedVisit = normalizeOmId(omtId);
        CityBuildingPiece fallback = null;
        CityBuildingPiece omtMatch = null;
        CityBuildingPiece requestedMatch = null;
        for (final CityBuildingPiece piece : building.getPieces()) {
            if (record.getAnchorX() + piece.getOffsetX() != omtX
                || record.getAnchorY() + piece.getOffsetY() != omtY) {
                continue;
            }
            if (fallback == null) {
                fallback = piece;
            }
            if (normalizeOmId(piece.getOvermapId()).equals(normalizedVisit)) {
                omtMatch = piece;
            }
            if (requestedZ != 0 && piece.getZLevel() == requestedZ) {
                requestedMatch = piece;
            }
        }
        if (requestedMatch != null) {
            return Optional.of(requestedMatch);
        }
        if (omtMatch != null) {
            return Optional.of(omtMatch);
        }
        return Optional.ofNullable(fallback);
    }

    public static boolean omTerrainMatchesZ(final String omTerrain, final int targetZ) {
        if (omTerrain == null || omTerrain.isEmpty()) {
            return targetZ == 0;
        }
        final Optional<Integer> inferred = inferFromOmtIdOptional(omTerrain);
        if (!inferred.isPresent()) {
            return targetZ == 0;
        }
        if (inferred.get() == ROOF_Z) {
            return targetZ >= 1;
        }
        return inferred.get() == targetZ;
    }

    public static int resolveRoofPlaceholder(final int zHint, final List<Integer> volumeZLevels) {
        if (zHint != ROOF_Z) {
            return zHint;
        }
        if (volumeZLevels == null || volumeZLevels.isEmpty()) {
            return 1;
        }
        return volumeZLevels.get(volumeZLevels.size() - 1);
    }

    private static int clampToVolume(
        final MapVolume volume,
        final int targetZ,
        final List<String> warnings
    ) {
        final List<Integer> levels = volume.getZLevels();
        if (levels.contains(targetZ)) {
            return targetZ;
        }
        int clamped = targetZ;
        if (targetZ < levels.get(0)) {
            clamped = levels.get(0);
        } else if (targetZ > levels.get(levels.size() - 1)) {
            clamped = levels.get(levels.size() - 1);
        } else {
            int nearest = levels.get(0);
            int bestDistance = Math.abs(targetZ - nearest);
            for (final int level : levels) {
                final int distance = Math.abs(targetZ - level);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    nearest = level;
                }
            }
            clamped = nearest;
        }
        if (warnings != null && clamped != targetZ) {
            warnings.add("visit z=" + targetZ + " clamped to volume z=" + clamped);
        }
        return clamped;
    }

    private static String normalizeOmId(final String omtId) {
        if (omtId == null) {
            return "";
        }
        return OvermapTerrainResolver.stripRotation(omtId).toLowerCase(Locale.ROOT);
    }
}
