package io.gdx.cdda.bn.nextgen.worldgen.generate;

/** Options for procedural mini-overmap generation (W4). */
public final class OvermapGenerateOptions {

    private final int width;
    private final int height;
    private final long seed;
    private final String regionId;
    private final int cityBuildingQuota;
    private final int staticSpecialQuota;
    private final int mutableSpecialQuota;
    private final String fieldId;
    private final String forestId;
    private final boolean riversEnabled;
    private final boolean roadsEnabled;
    private final String connectionId;
    private final String riverCenterId;
    private final String riverBankId;

    public OvermapGenerateOptions(
        final int width,
        final int height,
        final long seed,
        final String regionId,
        final int cityBuildingQuota,
        final int staticSpecialQuota,
        final int mutableSpecialQuota,
        final String fieldId,
        final String forestId,
        final boolean riversEnabled,
        final boolean roadsEnabled,
        final String connectionId,
        final String riverCenterId,
        final String riverBankId
    ) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.seed = seed;
        this.regionId = regionId == null || regionId.isEmpty() ? "default" : regionId;
        this.cityBuildingQuota = Math.max(0, cityBuildingQuota);
        this.staticSpecialQuota = Math.max(0, staticSpecialQuota);
        this.mutableSpecialQuota = Math.max(0, mutableSpecialQuota);
        this.fieldId = fieldId == null || fieldId.isEmpty() ? "field" : fieldId;
        this.forestId = forestId == null || forestId.isEmpty() ? "forest" : forestId;
        this.riversEnabled = riversEnabled;
        this.roadsEnabled = roadsEnabled;
        this.connectionId = connectionId == null || connectionId.isEmpty() ? "local_road" : connectionId;
        this.riverCenterId = riverCenterId == null || riverCenterId.isEmpty() ? "river_center" : riverCenterId;
        this.riverBankId = riverBankId == null || riverBankId.isEmpty() ? "river" : riverBankId;
    }

    public static OvermapGenerateOptions forSize(final int width, final int height) {
        final int area = width * height;
        final int cityQuota = Math.max(1, area / 32);
        final int specialQuota = area >= 144 ? 1 : 0;
        return new OvermapGenerateOptions(
            width,
            height,
            12345L,
            "default",
            cityQuota,
            specialQuota,
            area >= 144 ? 1 : 0,
            "field",
            "forest",
            area >= 64,
            true,
            "local_road",
            "river_center",
            "river"
        );
    }

    public OvermapGenerateOptions withSeed(final long seed) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId);
    }

    public OvermapGenerateOptions withQuotas(final int cityBuildings, final int staticSpecials) {
        return copyWith(seed, regionId, cityBuildings, staticSpecials, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId);
    }

    public OvermapGenerateOptions withQuotas(
        final int cityBuildings,
        final int staticSpecials,
        final int mutableSpecials
    ) {
        return copyWith(seed, regionId, cityBuildings, staticSpecials, mutableSpecials, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId);
    }

    public OvermapGenerateOptions withTerrainIds(final String fieldId, final String forestId) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId);
    }

    public OvermapGenerateOptions withConnectivity(
        final boolean riversEnabled,
        final boolean roadsEnabled,
        final String connectionId,
        final String riverCenterId,
        final String riverBankId
    ) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId);
    }

    private OvermapGenerateOptions copyWith(
        final long seed,
        final String regionId,
        final int cityBuildingQuota,
        final int staticSpecialQuota,
        final int mutableSpecialQuota,
        final String fieldId,
        final String forestId,
        final boolean riversEnabled,
        final boolean roadsEnabled,
        final String connectionId,
        final String riverCenterId,
        final String riverBankId
    ) {
        return new OvermapGenerateOptions(
            width,
            height,
            seed,
            regionId,
            cityBuildingQuota,
            staticSpecialQuota,
            mutableSpecialQuota,
            fieldId,
            forestId,
            riversEnabled,
            roadsEnabled,
            connectionId,
            riverCenterId,
            riverBankId
        );
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public long getSeed() {
        return seed;
    }

    public String getRegionId() {
        return regionId;
    }

    public int getCityBuildingQuota() {
        return cityBuildingQuota;
    }

    public int getStaticSpecialQuota() {
        return staticSpecialQuota;
    }

    public int getMutableSpecialQuota() {
        return mutableSpecialQuota;
    }

    public String getFieldId() {
        return fieldId;
    }

    public String getForestId() {
        return forestId;
    }

    public boolean isRiversEnabled() {
        return riversEnabled;
    }

    public boolean isRoadsEnabled() {
        return roadsEnabled;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public String getRiverCenterId() {
        return riverCenterId;
    }

    public String getRiverBankId() {
        return riverBankId;
    }
}
