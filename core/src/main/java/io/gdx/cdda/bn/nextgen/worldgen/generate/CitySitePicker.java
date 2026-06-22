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
            : estimateSiteCount(grid, options, citySize);
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
            sites.add(new int[] { x, y });
        }
        return sites;
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
        final CitySizeSettings citySize
    ) {
        final int area = grid.width() * grid.height();
        final int spacing = Math.max(1, citySize.getCitySpacing());
        final int bySpacing = Math.max(1, area / (spacing * spacing));
        return Math.min(bySpacing, Math.max(1, options.getCityBuildingQuota() / 2));
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
