package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Options for {@link JsonMapgenRunner} (P2). */
public final class JsonMapgenRunOptions {

    private String defaultFillTer = "t_dirt";
    private long previewSeed = 0xC0DA1L;
    private String previewRegionId = "default";
    private int omtRotation = 0;
    private RegionContext regionContext;
    private MapgenCatalog mapgenCatalog;
    private final List<String> warnings = new ArrayList<>();

    public JsonMapgenRunOptions() {}

    public String getDefaultFillTer() {
        return defaultFillTer;
    }

    public JsonMapgenRunOptions withDefaultFillTer(final String defaultFillTer) {
        this.defaultFillTer = defaultFillTer;
        return this;
    }

    public long getPreviewSeed() {
        return previewSeed;
    }

    public JsonMapgenRunOptions withPreviewSeed(final long previewSeed) {
        this.previewSeed = previewSeed;
        return this;
    }

    public Random createRng(final JsonMapgenDefinition definition) {
        final long salt = definition == null ? 0L : definition.displayName().hashCode();
        return new Random(previewSeed ^ salt);
    }

    public Random paletteRng() {
        return new Random(previewSeed ^ 0xC0DA10L);
    }

    public String getPreviewRegionId() {
        return previewRegionId;
    }

    public JsonMapgenRunOptions withPreviewRegionId(final String previewRegionId) {
        this.previewRegionId = previewRegionId == null || previewRegionId.isEmpty()
            ? "default"
            : previewRegionId;
        return this;
    }

    public int getOmtRotation() {
        return omtRotation;
    }

    public JsonMapgenRunOptions withOmtRotation(final int omtRotation) {
        this.omtRotation = Math.floorMod(omtRotation, 4);
        return this;
    }

    public JsonMapgenRunOptions deriveWithOmtRotation(final int omtRotation) {
        final JsonMapgenRunOptions derived = new JsonMapgenRunOptions();
        derived.defaultFillTer = defaultFillTer;
        derived.previewSeed = previewSeed;
        derived.previewRegionId = previewRegionId;
        derived.regionContext = regionContext;
        derived.mapgenCatalog = mapgenCatalog;
        derived.omtRotation = Math.floorMod(omtRotation, 4);
        return derived;
    }

    public RegionContext getRegionContext() {
        return regionContext;
    }

    public JsonMapgenRunOptions withRegionContext(final RegionContext regionContext) {
        this.regionContext = regionContext;
        return this;
    }

    public MapgenCatalog getMapgenCatalog() {
        return mapgenCatalog;
    }

    public JsonMapgenRunOptions withMapgenCatalog(final MapgenCatalog mapgenCatalog) {
        this.mapgenCatalog = mapgenCatalog;
        return this;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    public void addWarning(final String warning) {
        warnings.add(warning);
    }
}
