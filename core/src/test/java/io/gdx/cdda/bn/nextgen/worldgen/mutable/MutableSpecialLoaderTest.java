package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MutableSpecialLoaderTest {

    @Test
    void loadsFixtureMutableLab() throws Exception {
        final MutableSpecialLoadResult result = MutableSpecialLoader.load(
            MutableSpecialScanOptions.fromDataRoot(WorldgenTestFixtures.fixtureDataRoot())
        );
        assertTrue(result.getRegistry().contains("test_mutable_lab"));
        assertTrue(result.getRegistry().contains("test_mutable_quad"));
    }
}
