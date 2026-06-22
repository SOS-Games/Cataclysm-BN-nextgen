package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacementSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JoinContextTest {

    private MutableSpecialRegistry registry;

    @BeforeEach
    void loadFixtures() throws Exception {
        registry = MutableSpecialLoader.load(
            MutableSpecialScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
    }

    @Test
    void exposesActiveJoinBetweenMutablePieces() {
        final MutableSpecialDefinition lab = registry.find("test_mutable_lab").orElseThrow();
        final AssembledSpecialLayout layout = SpecialPhaseAssembler.assemble(
            lab,
            new Random(1L),
            new ArrayList<String>()
        ).orElseThrow();

        final OvermapGrid overmap = new OvermapGrid(4, 4, "test_field");
        for (final PlacedMutablePiece piece : layout.getPieces()) {
            overmap.setOmtId(1 + piece.getOffsetX(), 1 + piece.getOffsetY(), piece.resolveOvermapTerrainId());
        }

        final PlacedBuildingRecord record = PlacedBuildingRecord.ofMutable(
            null,
            1,
            1,
            layout
        );
        final JoinContext westContext = JoinContext.fromPlacement(
            overmap,
            record,
            1,
            1,
            lab
        );
        assertTrue(westContext.getActiveJoins().contains("test_join"));
    }

    @Test
    void emptyJoinsForNonMutablePlacement() {
        final OvermapGrid overmap = new OvermapGrid(4, 4, "test_field");
        final PlacedBuildingRecord record = PlacedBuildingRecord.of(null, 0, 0, PlacementSource.CITY);
        final JoinContext context = JoinContext.fromPlacement(overmap, record, 0, 0, null);
        assertTrue(context.getActiveJoins().isEmpty());
    }
}
