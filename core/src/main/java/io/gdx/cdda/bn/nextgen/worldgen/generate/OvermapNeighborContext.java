package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;

/** Optional adjacent overmap grids for edge stitch (hydrology v2, W16 prep). */
public final class OvermapNeighborContext {

    private final OvermapGrid north;
    private final OvermapGrid east;
    private final OvermapGrid south;
    private final OvermapGrid west;

    public OvermapNeighborContext(
        final OvermapGrid north,
        final OvermapGrid east,
        final OvermapGrid south,
        final OvermapGrid west
    ) {
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    public static OvermapNeighborContext empty() {
        return new OvermapNeighborContext(null, null, null, null);
    }

    public OvermapGrid getNorth() {
        return north;
    }

    public OvermapGrid getEast() {
        return east;
    }

    public OvermapGrid getSouth() {
        return south;
    }

    public OvermapGrid getWest() {
        return west;
    }
}
