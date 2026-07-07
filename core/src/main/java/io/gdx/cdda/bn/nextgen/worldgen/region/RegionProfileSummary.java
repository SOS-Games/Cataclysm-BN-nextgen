package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** One-line summary of overmap-relevant region settings for the map editor HUD. */
public final class RegionProfileSummary {

    private RegionProfileSummary() {}

    public static String describe(final RegionSettingsDefinition region) {
        if (region == null) {
            return "unknown region";
        }
        final List<String> parts = new ArrayList<>();
        parts.add("base=" + region.getDefaultOter());
        parts.add(forestSummary(region.getForestSettings()));
        if (region.getLakeSettings().isEnabled()) {
            parts.add("lakes");
        }
        if (region.getTerrainSettings().isEnabled()) {
            parts.add("swamp/beach");
        }
        if (region.getForestTrailSettings().isEnabled()) {
            parts.add("trails");
        }
        final UndergroundNetworkSettings underground = region.getUndergroundNetworkSettings();
        if (underground.isEnabled()) {
            final List<String> nets = new ArrayList<>();
            if (underground.isSubwaysEnabled()) {
                nets.add("subway");
            }
            if (underground.isRailsEnabled()) {
                nets.add("rail");
            }
            if (underground.isSewersEnabled()) {
                nets.add("sewer");
            }
            parts.add(String.join("+", nets));
        }
        if (region.getSpecialSettings().isEnabled()) {
            parts.add("region-specials");
        }
        parts.add(citySummary(region));
        return String.join(", ", parts);
    }

    public static boolean isLayoutPreviewProfile(final String regionId) {
        if (regionId == null || regionId.isEmpty()) {
            return false;
        }
        return regionId.startsWith("preview_")
            || "forest_trails".equals(regionId)
            || "underground_networks".equals(regionId);
    }

    private static String forestSummary(final OvermapForestSettings forest) {
        final int forestPct = (int) Math.round(forest.getNoiseThresholdForest() * 100);
        if (forest.hasThickForest()) {
            final int thickPct = (int) Math.round(forest.getNoiseThresholdForestThick() * 100);
            return "forest~" + forestPct + "% thick~" + thickPct + "%";
        }
        return "forest~" + forestPct + "%";
    }

    private static String citySummary(final RegionSettingsDefinition region) {
        final CitySizeSettings size = region.getCitySizeSettings();
        final CityContentWeights content = region.getCityContentWeights();
        if (!size.isEnabled() && !content.hasUrbanOmtTables()) {
            return "no-cities";
        }
        if (content.hasUrbanOmtTables()) {
            final List<String> bits = new ArrayList<>();
            if (!content.getHouses().isEmpty()) {
                bits.add("houses");
            }
            if (!content.getShops().isEmpty()) {
                bits.add("shops");
            }
            if (!content.getParks().isEmpty()) {
                bits.add("parks");
            }
            if (!content.getFinales().isEmpty()) {
                bits.add("finales");
            }
            final String sizeHint = size.getRawCitySize() == CitySizeSettings.USE_WORLD_OPTION
                ? "world-city-size"
                : "city@" + size.getCitySize();
            return sizeHint + " " + String.join("+", bits);
        }
        if (size.isEnabled()) {
            return "multitile-cities-only";
        }
        return "no-cities";
    }
}
