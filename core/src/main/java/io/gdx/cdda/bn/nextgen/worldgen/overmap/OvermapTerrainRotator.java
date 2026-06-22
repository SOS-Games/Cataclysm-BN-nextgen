package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.CardinalDirection;

/** Quarter-turn rotation for overmap terrain id suffixes (W11a). */
public final class OvermapTerrainRotator {

    private static final String[] CARDINAL_SUFFIXES = {
        "_north",
        "_east",
        "_south",
        "_west"
    };

    private OvermapTerrainRotator() {}

    public static String rotateId(final String overmapTerrainId, final int quarterTurnsClockwise) {
        if (overmapTerrainId == null || overmapTerrainId.isEmpty()) {
            return overmapTerrainId;
        }
        final int turns = Math.floorMod(quarterTurnsClockwise, 4);
        if (turns == 0) {
            return overmapTerrainId;
        }
        final String base = OvermapTerrainResolver.stripRotation(overmapTerrainId);
        final int current = MapGridRotator.rotationFromOmSuffix(overmapTerrainId);
        final int rotated = Math.floorMod(current + turns, 4);
        return base + CARDINAL_SUFFIXES[rotated];
    }

    public static CardinalDirection worldToLocalDirection(
        final CardinalDirection worldDirection,
        final int quarterTurnsClockwise
    ) {
        return rotateDirectionCounterClockwise(worldDirection, quarterTurnsClockwise);
    }

    public static CardinalDirection rotateDirectionClockwise(
        final CardinalDirection direction,
        final int quarterTurnsClockwise
    ) {
        if (direction == null) {
            return null;
        }
        final CardinalDirection[] order = CardinalDirection.values();
        final int index = direction.ordinal();
        return order[Math.floorMod(index + quarterTurnsClockwise, order.length)];
    }

    public static CardinalDirection rotateDirectionCounterClockwise(
        final CardinalDirection direction,
        final int quarterTurnsClockwise
    ) {
        return rotateDirectionClockwise(direction, 4 - Math.floorMod(quarterTurnsClockwise, 4));
    }
}
