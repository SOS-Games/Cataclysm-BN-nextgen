package io.gdx.cdda.bn.nextgen.worldgen.connection;

/**
 * BN {@code om_lines} bitmask for LINEAR overmap terrains (N=1, E=2, S=4, W=8).
 * See docs/worldgen/reference/06a-linear-oter-paint.md.
 */
public final class OmLines {

    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;
    public static final int BITS = 15;
    public static final int SIZE = 16;
    public static final int INVALID = 0;

    private static final String[] SUFFIXES = {
        "_isolated",
        "_end_south",
        "_end_west",
        "_ne",
        "_end_north",
        "_ns",
        "_es",
        "_nes",
        "_end_east",
        "_wn",
        "_ew",
        "_new",
        "_sw",
        "_nsw",
        "_esw",
        "_nesw"
    };

    private static final int[] MAPGEN_BUCKET = {
        4, 2, 2, 1, 2, 0, 1, 3, 2, 1, 0, 3, 1, 3, 3, 4
    };

    private static final String[] MAPGEN_SUFFIXES = {
        "_straight", "_curved", "_end", "_tee", "_four_way"
    };

    private OmLines() {}

    public static String suffix(final int line) {
        if (line < 0 || line >= SIZE) {
            return SUFFIXES[INVALID];
        }
        return SUFFIXES[line];
    }

    public static int lineFromSuffix(final String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return INVALID;
        }
        final String normalized = suffix.startsWith("_") ? suffix : "_" + suffix;
        for (int i = 0; i < SUFFIXES.length; i++) {
            if (SUFFIXES[i].equals(normalized)) {
                return i;
            }
        }
        return -1;
    }

    public static String idFor(final String baseId, final int line) {
        if (baseId == null || baseId.isEmpty()) {
            return baseId;
        }
        return baseId + suffix(line & BITS);
    }

    public static String mapgenIdFor(final String baseId, final int line) {
        if (baseId == null || baseId.isEmpty()) {
            return baseId;
        }
        final int bucket = MAPGEN_BUCKET[line & BITS];
        return baseId + MAPGEN_SUFFIXES[bucket];
    }

    public static int setSegment(final int line, final int dirBit) {
        return line | dirBit;
    }

    public static boolean hasSegment(final int line, final int dirBit) {
        return (line & dirBit) != 0;
    }

    public static boolean isStraight(final int line) {
        return line == 1 || line == 2 || line == 4 || line == 5 || line == 8 || line == 10;
    }

    public static int fromCardinals(
        final boolean north,
        final boolean east,
        final boolean south,
        final boolean west
    ) {
        int line = 0;
        if (north) {
            line = setSegment(line, NORTH);
        }
        if (east) {
            line = setSegment(line, EAST);
        }
        if (south) {
            line = setSegment(line, SOUTH);
        }
        if (west) {
            line = setSegment(line, WEST);
        }
        return line;
    }

    public static int bitsFromId(final String omtId, final String baseId) {
        if (omtId == null || omtId.isEmpty() || baseId == null || baseId.isEmpty()) {
            return INVALID;
        }
        if (omtId.equals(baseId)) {
            return INVALID;
        }
        if (!omtId.startsWith(baseId)) {
            return -1;
        }
        final String rest = omtId.substring(baseId.length());
        if (rest.isEmpty()) {
            return INVALID;
        }
        if ("_nesw_manhole".equals(rest)) {
            return BITS;
        }
        return lineFromSuffix(rest);
    }

    public static String stripToBase(final String omtId, final String... candidateBases) {
        if (omtId == null || omtId.isEmpty()) {
            return omtId;
        }
        String best = null;
        for (final String base : candidateBases) {
            if (base == null || base.isEmpty()) {
                continue;
            }
            if (omtId.equals(base) || omtId.startsWith(base + "_")) {
                if (best == null || base.length() > best.length()) {
                    best = base;
                }
            }
        }
        return best;
    }
}
