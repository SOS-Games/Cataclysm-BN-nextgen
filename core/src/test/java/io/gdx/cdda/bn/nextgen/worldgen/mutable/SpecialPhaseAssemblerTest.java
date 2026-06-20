package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpecialPhaseAssemblerTest {

    private MutableSpecialRegistry registry;

    @BeforeEach
    void loadFixtures() throws Exception {
        registry = MutableSpecialLoader.load(
            MutableSpecialScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        ).getRegistry();
    }

    @Test
    void assemblesTwoPieceLab() {
        final MutableSpecialDefinition lab = registry.find("test_mutable_lab").orElseThrow();
        final AssembledSpecialLayout layout = SpecialPhaseAssembler.assemble(
            lab,
            new Random(1L),
            new ArrayList<>()
        ).orElseThrow();

        assertEquals(2, layout.getPieces().size());
        assertEquals(2, layout.getWidth());
        assertEquals(1, layout.getHeight());
    }

    @Test
    void assemblesTwoByTwoQuadFootprint() {
        final MutableSpecialDefinition quad = registry.find("test_mutable_quad").orElseThrow();
        final AssembledSpecialLayout layout = SpecialPhaseAssembler.assemble(
            quad,
            new Random(2L),
            new ArrayList<>()
        ).orElseThrow();

        assertEquals(4, layout.getPieces().size());
        assertEquals(2, layout.getWidth());
        assertEquals(2, layout.getHeight());

        boolean foundNw = false;
        boolean foundSe = false;
        for (final PlacedMutablePiece piece : layout.getPieces()) {
            if ("test_mutable_nw_north".equals(piece.getOvermapTerrainId())) {
                foundNw = true;
            }
            if ("test_mutable_se_north".equals(piece.getOvermapTerrainId())) {
                foundSe = true;
            }
        }
        assertTrue(foundNw);
        assertTrue(foundSe);
    }
}
