package io.gdx.cdda.bn.nextgen.map;

/** One map cell with terrain and optional furniture id. */
public final class MapCell {

    private String terrainId;
    private String furnitureId;

    public MapCell(final String terrainId, final String furnitureId) {
        setTerrainId(terrainId);
        this.furnitureId = furnitureId;
    }

    public String getTerrainId() {
        return terrainId;
    }

    public void setTerrainId(final String terrainId) {
        if (terrainId == null || terrainId.trim().isEmpty()) {
            throw new IllegalArgumentException("terrainId must be non-empty");
        }
        this.terrainId = terrainId;
    }

    public String getFurnitureId() {
        return furnitureId;
    }

    public void setFurnitureId(final String furnitureId) {
        this.furnitureId = furnitureId;
    }
}
