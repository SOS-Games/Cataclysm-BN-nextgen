package io.gdx.cdda.bn.nextgen.worldgen.submap;

import io.gdx.cdda.bn.nextgen.gamedata.GameDataLoader;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsLoader;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubmapGeneratorRegionalResolveTest {

    @Test
    void visitSingleCellResolvesRegionalAliasesForField() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        final RegionSettingsDefinition region = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("forest_trails").orElseThrow();

        final OvermapGrid overmap = OvermapGridFactory.empty(3, 3, "test_field");
        overmap.setOmtId(1, 1, "field");

        final VisitResult result = SubmapGenerator.visitSingleCell(
            overmap,
            1,
            1,
            0,
            77L,
            null,
            null,
            service,
            OvermapTerrainLoader.load(
                OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
            ).getRegistry(),
            GameDataLoader.loadMods(
                Collections.singletonList("bn"),
                GameDataLoadOptions.fromRoots(Collections.singletonList(WorldgenTestFixtures.fixtureDataRoot()))
            ),
            null,
            null,
            region
        );

        assertTrue(result.hasGrid());
        assertFalse(VisitRegionalResolver.hasUnresolvedRegionalIds(result.getGrid()));
    }

    @Test
    void visitResolvesRegionalAliasesForMapgenFill() throws Exception {
        final MapgenPreviewService service = new MapgenPreviewService();
        service.ensureLoaded(MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot()));
        final RegionSettingsDefinition region = RegionSettingsLoader.load(
            MapgenScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry().find("test_region").orElseThrow();

        final OvermapGrid overmap = OvermapGridFactory.empty(3, 3, "open_air");
        overmap.setOmtId(1, 1, "test_region_fill");

        final VisitResult result = SubmapGenerator.visitSingleCell(
            overmap,
            1,
            1,
            0,
            88L,
            null,
            null,
            service,
            OvermapTerrainLoader.load(
                OvermapTerrainScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
            ).getRegistry(),
            GameDataLoader.loadMods(
                Collections.singletonList("bn"),
                GameDataLoadOptions.fromRoots(Collections.singletonList(WorldgenTestFixtures.fixtureDataRoot()))
            ),
            null,
            null,
            region
        );

        assertTrue(result.hasGrid());
        assertFalse(VisitRegionalResolver.hasUnresolvedRegionalIds(result.getGrid()));
    }
}
