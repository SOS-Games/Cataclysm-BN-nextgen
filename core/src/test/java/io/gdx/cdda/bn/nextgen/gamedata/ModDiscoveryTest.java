package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ModDiscoveryTest {

    @Test
    void discoversCoreModAndResolvesJsonRoot() throws Exception {
        final ModRegistry registry = ModDiscovery.discover(
            Collections.singletonList(modFixtureDataRoot())
        );

        final ModInfo bn = registry.find("bn").orElseThrow();
        assertTrue(bn.isCore());
        assertEquals(
            modFixtureDataRoot().resolve("json").normalize(),
            bn.getResolvedContentPath()
        );
    }

    @Test
    void discoversNestedModinfoRecursively() throws Exception {
        final ModRegistry registry = ModDiscovery.discover(
            Collections.singletonList(modFixtureDataRoot())
        );

        assertTrue(registry.contains("nested_pack"));
    }

    @Test
    void modWithDotPathResolvesContentBesideModinfo() throws Exception {
        final ModRegistry registry = ModDiscovery.discover(
            Collections.singletonList(modFixtureDataRoot())
        );

        final ModInfo patchA = registry.find("patch_a").orElseThrow();
        assertEquals(
            modFixtureDataRoot().resolve("mods/patch_a").normalize(),
            patchA.getResolvedContentPath()
        );
    }

    @Test
    void duplicateModIdsRejectAllConflictingEntries() throws Exception {
        final ModRegistry registry = ModDiscovery.discover(
            Collections.singletonList(modFixtureDataRoot())
        );

        assertFalse(registry.contains("dup_mod"));
        assertTrue(registry.getDiscoveryWarnings().stream()
            .anyMatch(w -> w.contains("dup_mod")));
    }

    @Test
    void integrationDiscoversBnModFromSiblingCheckout() throws Exception {
        final Path bnData = Paths.get("").toAbsolutePath()
            .resolve("../Cataclysm-BN/data")
            .normalize();
        assumeTrue(bnData.toFile().isDirectory(), "Cataclysm-BN/data not found beside nextgen");

        final ModRegistry registry = ModDiscovery.discover(Collections.singletonList(bnData));
        final ModInfo bn = registry.find("bn").orElse(null);
        assertNotNull(bn);
        assertEquals(bnData.resolve("json").normalize(), bn.getResolvedContentPath());
    }

    static Path modFixtureDataRoot() throws URISyntaxException {
        final URL url = ModDiscoveryTest.class.getResource("/gamedata-mod-fixtures");
        assertNotNull(url, "fixture gamedata-mod-fixtures missing from test classpath");
        return Paths.get(url.toURI());
    }
}
