package io.gdx.cdda.bn.nextgen.gamedata.mod;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModConfigurationTest {

    @AfterEach
    void tearDown() {
        ModConfiguration.clearTestOverride();
    }

    @Test
    void parsesStoredModIds() {
        assertEquals(
            Arrays.asList("bn", "Arcana", "DinoMod"),
            ModConfiguration.parseStoredModIds("bn, Arcana,DinoMod")
        );
    }

    @Test
    void usesTestOverrideWhenSet() {
        ModConfiguration.setTestOverride(Arrays.asList("bn", "Arcana"));
        assertEquals(Arrays.asList("bn", "Arcana"), ModConfiguration.activeModIds());
        assertTrue(ModConfiguration.hasUserSelection());

        ModConfiguration.saveEnabledModIds(Arrays.asList("bn", "DinoMod"));
        assertEquals(Arrays.asList("bn", "DinoMod"), ModConfiguration.activeModIds());

        ModConfiguration.clearUserSelection();
        assertFalse(ModConfiguration.hasUserSelection());
    }
}
