package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.region.CitySizeSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Picks urban site centers with minimum spacing (W14b). */
public final class CitySitePicker {

    private CitySitePicker() {}

    public static List<int[]> pickSites(
        final OvermapGrid grid,
        final CitySizeSettings citySize,
        final OvermapGenerateOptions options,
        final Random rng
    ) {
        if (grid == null || citySize == null || !citySize.isEnabled() || rng == null) {
            return Collections.emptyList();
        }
        final int margin = Math.max(2, citySize.getCitySize());
        if (grid.width() <= margin * 2 || grid.height() <= margin * 2) {
            return Collections.emptyList();
        }
        final int maxSites = citySize.isCityIsolated()
            ? 1
            : estimateSiteCount(grid, options, citySize, rng);
        final int spacing = citySize.getCitySpacing();
        final List<int[]> sites = new ArrayList<>();
        int attempts = 0;
        final int maxAttempts = Math.max(maxSites * 30, 30);
        while (sites.size() < maxSites && attempts < maxAttempts) {
            attempts++;
            final int x = margin + rng.nextInt(grid.width() - margin * 2);
            final int y = margin + rng.nextInt(grid.height() - margin * 2);
            if (spacing > 0 && !isSpacedFrom(sites, x, y, spacing)) {
                continue;
            }
            if (!isClearableCitySeed(grid, x, y, options)) {
                continue;
            }
            sites.add(new int[] { x, y });
        }
        return sites;
    }

    private static boolean isClearableCitySeed(
        final OvermapGrid grid,
        final int x,
        final int y,
        final OvermapGenerateOptions options
    ) {
        return UrbanTerrainClearables.isPaveable(grid.getOmtId(x, y), null, options);
    }

    public static boolean isWithinCityBlob(
        final List<int[]> citySites,
        final int citySize,
        final int originX,
        final int originY
    ) {
        if (citySites == null || citySites.isEmpty() || citySize <= 0) {
            return true;
        }
        for (final int[] site : citySites) {
            if (Math.abs(originX - site[0]) <= citySize && Math.abs(originY - site[1]) <= citySize) {
                return true;
            }
        }
        return false;
    }

    public static int siteKey(final int x, final int y) {
        return (x << 16) ^ y;
    }

    private static int estimateSiteCount(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final CitySizeSettings citySize,
        final Random rng
    ) {
        // BN place_cities coverage: area * (1/2^spacing) / omts_per_city
        final int area = grid.width() * grid.height();
        final int spacing = Math.max(0, citySize.getCitySpacing());
        final int size = Math.max(1, citySize.getCitySize());
        final double coverage = 1.0 / Math.pow(2.0, spacing);
        final double omtsPerCity = (2.0 * size + 1.0) * (2.0 * size + 1.0) * 0.75;
        final double expected = area * coverage / Math.max(1.0, omtsPerCity);
        final int fromCoverage = Math.max(1, rollRemainder(expected, rng));
        // Soft upper bound from packing; do not use building quota (that caps buildings, not towns).
        final int bySpacing = Math.max(1, area / Math.max(1, spacing <= 0 ? 1 : spacing * spacing));
        return Math.min(fromCoverage, bySpacing);
    }

    /** BN {@code roll_remainder}: floor(x) plus one with probability of the fractional part. */
    static int rollRemainder(final double value, final Random rng) {
        if (value <= 0) {
            return 0;
        }
        final int whole = (int) Math.floor(value);
        final double frac = value - whole;
        if (frac > 0 && rng != null && rng.nextDouble() < frac) {
            return whole + 1;
        }
        return Math.max(1, whole);
    }

    private static boolean isSpacedFrom(
        final List<int[]> sites,
        final int x,
        final int y,
        final int spacing
    ) {
        for (final int[] site : sites) {
            final int dx = x - site[0];
            final int dy = y - site[1];
            if (dx * dx + dy * dy < spacing * spacing) {
                return false;
            }
        }
        return true;
    }
}
