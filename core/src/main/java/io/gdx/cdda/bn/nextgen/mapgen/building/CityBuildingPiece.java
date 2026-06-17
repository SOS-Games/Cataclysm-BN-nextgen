package io.gdx.cdda.bn.nextgen.mapgen.building;

/** One OMT placement inside a {@link CityBuildingDefinition}. */
public final class CityBuildingPiece {

    private final int offsetX;
    private final int offsetY;
    private final int zLevel;
    private final String overmapId;

    public CityBuildingPiece(
        final int offsetX,
        final int offsetY,
        final int zLevel,
        final String overmapId
    ) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.zLevel = zLevel;
        this.overmapId = overmapId;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public int getZLevel() {
        return zLevel;
    }

    public String getOvermapId() {
        return overmapId;
    }
}
