package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapForestSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.OvermapTerrainSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Terrains city streets / lots may overwrite. Matches BN {@code local_road} locations
 * (field, forest*, swamp) rather than field-only clearables.
 */
public final class UrbanTerrainClearables {

    private UrbanTerrainClearables() {}

    public static Set<String> forCityGrowth(
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final RegionSettingsDefinition region
    ) {
        final Set<String> ids = new HashSet<>(OmtBuildingBlitter.defaultClearableIds(options, registry));
        add(ids, "forest", registry);
        add(ids, "forest_thick", registry);
        add(ids, "forest_water", registry);
        add(ids, "forest_trail", registry);
        add(ids, "swamp", registry);
        add(ids, "test_swamp", registry);
        add(ids, "test_forest_thick", registry);
        if (region != null) {
            final OvermapForestSettings forest = region.getForestSettings();
            if (forest != null) {
                add(ids, forest.getForestOter(), registry);
                add(ids, forest.getForestThickOter(), registry);
            }
            final OvermapTerrainSettings terrain = region.getTerrainSettings();
            if (terrain != null) {
                add(ids, terrain.getSwampOter(), registry);
            }
        }
        return Collections.unmodifiableSet(ids);
    }

    /** True when a city street may pave this OMT (BN local_road forest/swamp locations). */
    public static boolean isPaveable(
        final String omtId,
        final Set<String> explicitIds,
        final OvermapGenerateOptions options
    ) {
        if (omtId == null || omtId.isEmpty()) {
            return false;
        }
        if (explicitIds != null && explicitIds.contains(omtId)) {
            return true;
        }
        if (isRoadFamily(omtId)) {
            return true;
        }
        if (isWaterBody(omtId, options)) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        return n.contains("forest")
            || n.contains("swamp")
            || n.equals("field")
            || n.equals("test_field")
            || n.equals("open_air");
    }

    public static boolean isWaterBody(final String omtId, final OvermapGenerateOptions options) {
        if (omtId == null) {
            return false;
        }
        if (options != null) {
            if (omtId.equals(options.getRiverCenterId())
                || omtId.equals(options.getRiverBankId())
                || omtId.equals(options.getLakeId())) {
                return true;
            }
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        return n.contains("river") || n.contains("lake") || n.contains("beach");
    }

    public static boolean isRoadFamily(final String omtId) {
        if (omtId == null) {
            return false;
        }
        final String n = omtId.toLowerCase(Locale.ROOT);
        return n.equals("road")
            || n.startsWith("road_")
            || n.equals("test_road")
            || n.startsWith("test_road_")
            || n.startsWith("hiway_")
            || n.contains("manhole")
            || n.contains("bridge");
    }

    private static void add(final Set<String> ids, final String id, final OvermapTerrainRegistry registry) {
        if (id == null || id.isEmpty()) {
            return;
        }
        if (registry == null || registry.contains(id)) {
            ids.add(id);
        }
    }
}
