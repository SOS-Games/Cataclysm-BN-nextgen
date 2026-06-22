package io.gdx.cdda.bn.nextgen.worldgen.mutable;

/** Cardinal OMT edge for mutable special joins (W6). */
public enum CardinalDirection {

    NORTH(0, -1),
    EAST(1, 0),
    SOUTH(0, 1),
    WEST(-1, 0);

    private final int dx;
    private final int dy;

    CardinalDirection(final int dx, final int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    public CardinalDirection opposite() {
        switch (this) {
            case NORTH:
                return SOUTH;
            case EAST:
                return WEST;
            case SOUTH:
                return NORTH;
            case WEST:
                return EAST;
            default:
                return this;
        }
    }

    public CardinalDirection rotateClockwise(final int quarterTurns) {
        final CardinalDirection[] order = values();
        return order[Math.floorMod(ordinal() + quarterTurns, order.length)];
    }

    public static CardinalDirection fromJsonKey(final String key) {
        if (key == null) {
            return null;
        }
        switch (key) {
            case "north":
                return NORTH;
            case "east":
                return EAST;
            case "south":
                return SOUTH;
            case "west":
                return WEST;
            default:
                return null;
        }
    }
}
