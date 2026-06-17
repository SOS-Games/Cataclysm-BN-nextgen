package io.gdx.cdda.bn.nextgen;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultContentTest {

    @Test
    void readsModIdsFromDefaultJsonFile() throws Exception {
        final Path dataRoot = Files.createTempDirectory("default-content-test");
        Files.createDirectories(dataRoot.resolve("mods"));
        Files.writeString(
            dataRoot.resolve("mods/default.json"),
            "[\"bn\",\"udp_redux\",\"pride_flags\"]"
        );

        final List<String> modIds = DefaultContent.defaultModIds(dataRoot);

        assertEquals(List.of("bn", "udp_redux", "pride_flags"), modIds);
    }

    @Test
    void preferredTilesetPutsUndeadPeopleBaseFirst() {
        assertTrue(DefaultContent.PREFERRED_TILESET_IDS.length > 0);
        assertEquals(DefaultContent.UNDEAD_PEOPLE_TILESET_ID, DefaultContent.PREFERRED_TILESET_IDS[0]);
    }
}
