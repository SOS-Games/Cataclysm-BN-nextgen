package io.gdx.cdda.bn.nextgen.worldgen.mutable;

/** One placed piece from {@link SpecialPhaseAssembler} (W6). */
public final class PlacedMutablePiece {

    private final String pieceId;
    private final int offsetX;
    private final int offsetY;
    private final String overmapTerrainId;

    public PlacedMutablePiece(
        final String pieceId,
        final int offsetX,
        final int offsetY,
        final String overmapTerrainId
    ) {
        this.pieceId = pieceId;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.overmapTerrainId = overmapTerrainId;
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
}
