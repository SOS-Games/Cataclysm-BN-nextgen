package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OvermapGeneratorRegionTest {

    private WorldgenPreviewService service;

    @BeforeEach
    void loadWorldgen() throws Exception {
        service = new WorldgenPreviewService();
        service.ensureLoaded(WorldgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
    }

    @Test
    void regionSwitchChangesSpecialMixOnSameSeed() {
        final long seed = 909L;
        final OvermapGenerateResult defaultResult = service.generateOvermap(
            OvermapGenerateOptions.forSize(32, 32).withSeed(seed).withRegionId("default")
        );
        final OvermapGenerateResult specialResult = service.generateOvermap(
            OvermapGenerateOptions.forSize(32, 32).withSeed(seed).withRegionId("special_heavy")
        );

        assertTrue(specialResult.getStaticSpecialsPlaced() >= defaultResult.getStaticSpecialsPlaced());
        assertNotEquals(gridFingerprint(defaultResult.getGrid()), gridFingerprint(specialResult.getGrid()));
    }

    private static String gridFingerprint(final OvermapGrid grid) {
        final StringBuilder out = new StringBuilder();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                out.append(grid.getOmtId(x, y)).append(';');
            }
        }
        return out.toString();
    }
}
