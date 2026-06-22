package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRotator;

/** One placed piece from {@link SpecialPhaseAssembler} (W6). */
public final class PlacedMutablePiece {

    private final String pieceId;
    private final int offsetX;
    private final int offsetY;
    private final String overmapTerrainId;
    private final int rotation;

    public PlacedMutablePiece(
        final String pieceId,
        final int offsetX,
        final int offsetY,
        final String overmapTerrainId
    ) {
        this(pieceId, offsetX, offsetY, overmapTerrainId, 0);
    }

    public PlacedMutablePiece(
        final String pieceId,
        final int offsetX,
        final int offsetY,
        final String overmapTerrainId,
        final int rotation
    ) {
        this.pieceId = pieceId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.overmapTerrainId = overmapTerrainId;
        this.rotation = Math.floorMod(rotation, 4);
    }

    public String getPieceId() {
        return pieceId;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public String getOvermapTerrainId() {
        return overmapTerrainId;
    }

    public int getRotation() {
        return rotation;
    }

    public String resolveOvermapTerrainId() {
        return OvermapTerrainRotator.rotateId(overmapTerrainId, rotation);
    }
}
