package io.gdx.cdda.bn.nextgen.view;

import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZCutawayPolicyTest {

    @Test
    void zLevelsAboveActiveReturnsHigherFloorsOnly() {
        final MapVolume volume = volumeWithZ(-1, 0, 1);
        volume.setActiveZ(0);
        assertEquals(Arrays.asList(1), ZCutawayPolicy.zLevelsAboveActive(volume));

        volume.setActiveZ(1);
        assertTrue(ZCutawayPolicy.zLevelsAboveActive(volume).isEmpty());
    }

    @Test
    void isCutawayActiveRequiresEnabledVolumeAndUpperFloors() {
        final MapVolume volume = volumeWithZ(0, 1);
        volume.setActiveZ(0);
        assertFalse(ZCutawayPolicy.isCutawayActive(volume, false));
        assertTrue(ZCutawayPolicy.isCutawayActive(volume, true));
        assertFalse(ZCutawayPolicy.isCutawayActive(null, true));

        volume.setActiveZ(1);
        assertFalse(ZCutawayPolicy.isCutawayActive(volume, true));
    }

    @Test
    void skipUpperFloorOverlayForOpenAirAndRoofSuffix() {
        final TerrainRegistry terrains = new TerrainRegistry();
        assertTrue(ZCutawayPolicy.skipUpperFloorOverlay("t_open_air", terrains));
        assertTrue(ZCutawayPolicy.skipUpperFloorOverlay("t_shingle_flat_roof", terrains));
        assertFalse(ZCutawayPolicy.skipUpperFloorOverlay("t_floor", terrains));
    }

    @Test
    void skipUpperFloorOverlayForTransparentFlags() {
        final TerrainRegistry terrains = new TerrainRegistry();
        terrains.put(new TerrainDefinition(
            "t_window",
            "window",
            null,
            "#",
            "cyan",
            0,
            Collections.singletonList("TRANSPARENT"),
            null,
            "test"
        ));
        assertTrue(ZCutawayPolicy.skipUpperFloorOverlay("t_window", terrains));
    }

    private static MapVolume volumeWithZ(final int... zLevels) {
        final Map<Integer, MapGrid> grids = new LinkedHashMap<>();
        for (final int z : zLevels) {
            grids.put(z, new MapGrid(4, 4, "t_grass"));
        }
        final List<Integer> ordered = new ArrayList<>();
        for (final int z : zLevels) {
            ordered.add(z);
        }
        return new MapVolume("test", ordered, grids, zLevels[0]);
    }
}
