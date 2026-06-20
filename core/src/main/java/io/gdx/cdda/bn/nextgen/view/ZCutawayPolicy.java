package io.gdx.cdda.bn.nextgen.view;

import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Upper-floor ghost overlay rules for multi-z building preview (M7). */
public final class ZCutawayPolicy {

    public static final float UPPER_FLOOR_OVERLAY_ALPHA = 0.25f;

    private ZCutawayPolicy() {}

    public static boolean isCutawayActive(final MapVolume volume, final boolean cutawayEnabled) {
        return cutawayEnabled
            && volume != null
            && volume.floorCount() > 1
            && !zLevelsAboveActive(volume).isEmpty();
    }

    public static List<Integer> zLevelsAboveActive(final MapVolume volume) {
        if (volume == null || volume.floorCount() <= 1) {
            return Collections.emptyList();
        }
        final int activeIndex = volume.activeFloorIndex();
        if (activeIndex < 0 || activeIndex >= volume.getZLevels().size() - 1) {
            return Collections.emptyList();
        }
        final List<Integer> above = new ArrayList<>();
        final List<Integer> zLevels = volume.getZLevels();
        for (int i = activeIndex + 1; i < zLevels.size(); i++) {
            above.add(zLevels.get(i));
        }
        return Collections.unmodifiableList(above);
    }

    /** Skip open air / roof / transparent flags in the upper-floor ghost pass. */
    public static boolean skipUpperFloorOverlay(
        final String terrainId,
        final TerrainRegistry terrains
    ) {
        if (terrainId == null || terrainId.isEmpty()) {
            return true;
        }
        if ("t_open_air".equals(terrainId)) {
            return true;
        }
        final String lower = terrainId.toLowerCase(Locale.ROOT);
        if (lower.endsWith("_roof") || lower.contains("open_air")) {
            return true;
        }
        if (terrains == null) {
            return false;
        }
        return terrains.find(terrainId)
            .map(ZCutawayPolicy::hasTransparentDrawFlag)
            .orElse(false);
    }

    private static boolean hasTransparentDrawFlag(final TerrainDefinition definition) {
        for (final String flag : definition.getFlags()) {
            if (flag == null || flag.isEmpty()) {
                continue;
            }
            if ("TRANSPARENT".equals(flag)
                || "NO_FLOOR".equals(flag)
                || "Z_TRANSPARENT".equals(flag)) {
                return true;
            }
        }
        return false;
    }
}
