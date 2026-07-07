package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.UndergroundNetworkSettings;

import java.util.List;
import java.util.Random;

/** Subway, rail, and sewer carve passes (W17f). */
public final class UndergroundNetworkGenerator {

    private UndergroundNetworkGenerator() {}

    public static int placeAll(
        final OvermapGrid grid,
        final List<UrbanSite> urbanSites,
        final RegionSettingsDefinition region,
        final OvermapConnectionRegistry connections,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || region == null || options == null || urbanSites == null || urbanSites.isEmpty()) {
            return 0;
        }
        final UndergroundNetworkSettings settings = region.getUndergroundNetworkSettings();
        if (!settings.isEnabled()) {
            return 0;
        }

        int painted = 0;
        if (settings.isSubwaysEnabled() && urbanSites.size() >= 2) {
            painted += SubwayGenerator.connectCities(
                grid,
                urbanSites,
                connections,
                options,
                registry,
                rng,
                warnings
            );
        }
        if (settings.isRailsEnabled() && urbanSites.size() >= 2) {
            painted += RailGenerator.connectCities(
                grid,
                urbanSites,
                connections,
                options,
                registry,
                rng,
                warnings
            );
        }
        if (settings.isSewersEnabled()) {
            painted += SewerGenerator.carveSites(
                grid,
                urbanSites,
                connections,
                options,
                registry,
                rng,
                warnings
            );
        }
        return painted;
    }
}
