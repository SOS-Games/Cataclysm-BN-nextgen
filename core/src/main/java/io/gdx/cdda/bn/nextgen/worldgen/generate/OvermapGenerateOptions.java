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
    private final String lakeId;
    private final boolean lakesEnabled;
    private final boolean legacyGenerationOrder;

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
        this(
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
            riverBankId,
            "lake",
            true,
            false
        );
    }

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
        final String riverBankId,
        final String lakeId,
        final boolean lakesEnabled,
        final boolean legacyGenerationOrder
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
        this.lakeId = lakeId == null || lakeId.isEmpty() ? "lake" : lakeId;
        this.lakesEnabled = lakesEnabled;
        this.legacyGenerationOrder = legacyGenerationOrder;
    }

    public static OvermapGenerateOptions forSize(final int width, final int height) {
        if (width == 180 && height == 180) {
            return bnScale();
        }
        return scaledForArea(width, height);
    }

    /** Dev preset for 64×64 overmap preview (W10). */
    public static OvermapGenerateOptions preview64() {
        return scaledForArea(64, 64);
    }

    /** BN-scale 180×180 with reduced placement quotas (W10). */
    public static OvermapGenerateOptions bnScale() {
        return new OvermapGenerateOptions(
            180,
            180,
            12345L,
            "default",
            10,
            3,
            1,
            "field",
            "forest",
            true,
            true,
            "local_road",
            "river_center",
            "river"
        );
    }

    private static OvermapGenerateOptions scaledForArea(final int width, final int height) {
        final int area = width * height;
        final int cityQuota;
        final int staticQuota;
        final int mutableQuota;
        final boolean riversEnabled;
        if (area >= 256 * 256) {
            cityQuota = 12;
            staticQuota = 3;
            mutableQuota = 1;
            riversEnabled = true;
        } else if (area >= 128 * 128) {
            cityQuota = Math.min(12, Math.max(8, area / 2700));
            staticQuota = 3;
            mutableQuota = 1;
            riversEnabled = true;
        } else if (area >= 64 * 64) {
            cityQuota = Math.min(24, Math.max(8, area / 256));
            staticQuota = Math.min(4, Math.max(2, area / 4096));
            mutableQuota = 1;
            riversEnabled = true;
        } else {
            cityQuota = Math.max(1, area / 32);
            staticQuota = area >= 144 ? 1 : 0;
            mutableQuota = area >= 144 ? 1 : 0;
            riversEnabled = area >= 64;
        }
        return new OvermapGenerateOptions(
            width,
            height,
            12345L,
            "default",
            cityQuota,
            staticQuota,
            mutableQuota,
            "field",
            "forest",
            riversEnabled,
            true,
            "local_road",
            "river_center",
            "river"
        );
    }

    public OvermapGenerateOptions withSeed(final long seed) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
    }

    public OvermapGenerateOptions withRegionId(final String regionId) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
    }

    public OvermapGenerateOptions withQuotas(final int cityBuildings, final int staticSpecials) {
        return copyWith(seed, regionId, cityBuildings, staticSpecials, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
    }

    public OvermapGenerateOptions withQuotas(
        final int cityBuildings,
        final int staticSpecials,
        final int mutableSpecials
    ) {
        return copyWith(seed, regionId, cityBuildings, staticSpecials, mutableSpecials, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
    }

    public OvermapGenerateOptions withTerrainIds(final String fieldId, final String forestId) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
    }

    public OvermapGenerateOptions withConnectivity(
        final boolean riversEnabled,
        final boolean roadsEnabled,
        final String connectionId,
        final String riverCenterId,
        final String riverBankId
    ) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
    }

    public OvermapGenerateOptions withLegacyGenerationOrder(final boolean legacyGenerationOrder) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
    }

    public OvermapGenerateOptions withLakesEnabled(final boolean lakesEnabled) {
        return copyWith(seed, regionId, cityBuildingQuota, staticSpecialQuota, mutableSpecialQuota, fieldId, forestId,
            riversEnabled, roadsEnabled, connectionId, riverCenterId, riverBankId, lakeId, lakesEnabled,
            legacyGenerationOrder);
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
        final String riverBankId,
        final String lakeId,
        final boolean lakesEnabled,
        final boolean legacyGenerationOrder
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
            riverBankId,
            lakeId,
            lakesEnabled,
            legacyGenerationOrder
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

    public String getLakeId() {
        return lakeId;
    }

    public boolean isLakesEnabled() {
        return lakesEnabled;
    }

    public boolean isLegacyGenerationOrder() {
        return legacyGenerationOrder;
    }
}
