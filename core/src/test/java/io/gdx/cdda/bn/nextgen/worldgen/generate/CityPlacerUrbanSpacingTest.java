package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.region.CitySizeSettings;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CityPlacerUrbanSpacingTest {

    @Test
    void citySitesRespectMinimumSpacingOn64x64() throws Exception {
        // Spacing low enough that BN coverage expects multiple sites on 64×64.
        final CitySizeSettings settings = new CitySizeSettings(3, 4, false);

        final OvermapGrid grid = new OvermapGrid(64, 64, "open_air");
        final List<int[]> sites = CitySitePicker.pickSites(
            grid,
            settings,
            OvermapGenerateOptions.forSize(64, 64).withQuotas(12, 0),
            new Random(42L)
        );

        assertTrue(sites.size() >= 2, "expected multiple city sites, got " + sites.size());
        for (int i = 0; i < sites.size(); i++) {
            for (int j = i + 1; j < sites.size(); j++) {
                final int dx = sites.get(i)[0] - sites.get(j)[0];
                final int dy = sites.get(i)[1] - sites.get(j)[1];
                assertTrue(
                    dx * dx + dy * dy >= settings.getCitySpacing() * settings.getCitySpacing(),
                    "sites (" + sites.get(i)[0] + "," + sites.get(i)[1] + ") and ("
                        + sites.get(j)[0] + "," + sites.get(j)[1] + ") too close"
                );
            }
        }
    }
}
