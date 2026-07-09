package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Carves river chains on a mini-overmap (W5 v1, W17d multi-pass, hydrology v2 drunkard walk). */
public final class RiverGenerator {

    /** XOR mask for the optional second hydrology carve pass (W17d). */
    public static final long SECOND_PASS_SEED_XOR = 0x52495632L;

    private static final double DEFAULT_RIVER_SCALE = 4.0;

    private RiverGenerator() {}

    public static int carve(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        return carveSimple(grid, options, null, registry, rng, warnings);
    }

    public static int carve(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        return carve(grid, options, region, OvermapNeighborContext.empty(), registry, rng, warnings);
    }

    /** W5 v1 single drunkard segment (legacy generation order). */
    public static int carveSimple(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || !options.isRiversEnabled()) {
            return 0;
        }
        final double riverScale = resolveRiverScale(region);
        if (riverScale <= 0.0) {
            return 0;
        }

        final String riverCenterId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverCenterId(),
            "river_center",
            registry
        );
        final String riverBankId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverBankId(),
            "river",
            registry
        );
        if (registry != null && !registry.contains(riverCenterId)) {
            addWarning(warnings, "river terrain '" + riverCenterId + "' not in registry; skipping river carve");
            return 0;
        }

        final String lakeId = OrthogonalPathCarver.resolveTerrainId(options.getLakeId(), "lake", registry);
        final Set<String> overwritable = OrthogonalPathCarver.terrainOverwritableIds(options, registry);
        int painted = carveLakeOrFallbackSegment(
            grid,
            lakeId,
            riverCenterId,
            riverScale,
            overwritable,
            options,
            registry,
            rng
        );
        if (registry != null && registry.contains(riverBankId) && !riverBankId.equals(riverCenterId)) {
            painted += paintRiverBanksFromCenter(grid, riverCenterId, riverBankId, lakeId, overwritable);
        }
        if (painted == 0) {
            addWarning(warnings, "river drunkard walk painted no cells");
        }
        return painted;
    }

    public static int carve(
        final OvermapGrid grid,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapNeighborContext neighbors,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || options == null || !options.isRiversEnabled()) {
            return 0;
        }
        final double riverScale = resolveRiverScale(region);
        if (riverScale <= 0.0) {
            return 0;
        }

        final String riverCenterId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverCenterId(),
            "river_center",
            registry
        );
        final String riverBankId = OrthogonalPathCarver.resolveTerrainId(
            options.getRiverBankId(),
            "river",
            registry
        );
        if (registry != null && !registry.contains(riverCenterId)) {
            addWarning(warnings, "river terrain '" + riverCenterId + "' not in registry; skipping river carve");
            return 0;
        }

        final String lakeId = OrthogonalPathCarver.resolveTerrainId(options.getLakeId(), "lake", registry);
        final Set<String> overwritable = OrthogonalPathCarver.terrainOverwritableIds(options, registry);

        final RiverEdgeStitcher.RiverEndpointPlan plan = RiverEdgeStitcher.stitchAndCollect(
            grid,
            neighbors,
            options,
            registry,
            riverCenterId,
            riverScale,
            rng
        );
        int painted = plan.getStitchedCells();
        painted += carvePairedSegments(
            grid,
            plan.getStarts(),
            plan.getEnds(),
            lakeId,
            riverCenterId,
            riverScale,
            overwritable,
            options,
            registry,
            rng
        );

        if (registry != null && registry.contains(riverBankId) && !riverBankId.equals(riverCenterId)) {
            painted += paintRiverBanksFromCenter(grid, riverCenterId, riverBankId, lakeId, overwritable);
        }
        if (painted == 0) {
            addWarning(warnings, "river drunkard walk painted no cells");
        }
        return painted;
    }

    private static int carvePairedSegments(
        final OvermapGrid grid,
        final List<int[]> starts,
        final List<int[]> ends,
        final String lakeId,
        final String riverCenterId,
        final double riverScale,
        final Set<String> overwritable,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        final List<int[]> workStarts = new ArrayList<>(starts);
        final List<int[]> workEnds = new ArrayList<>(ends);

        if (workStarts.isEmpty() && workEnds.isEmpty()) {
            return carveLakeOrFallbackSegment(
                grid,
                lakeId,
                riverCenterId,
                riverScale,
                overwritable,
                options,
                registry,
                rng
            );
        }

        int painted = 0;
        if (workStarts.size() > workEnds.size() && !workEnds.isEmpty()) {
            final List<int[]> endsCopy = new ArrayList<>(workEnds);
            while (!workStarts.isEmpty()) {
                final int[] start = removeRandom(workStarts, rng);
                if (!workEnds.isEmpty()) {
                    final int[] end = workEnds.remove(0);
                    painted += RiverDrunkardCarver.carve(
                        grid, start[0], start[1], end[0], end[1],
                        riverCenterId, lakeId, riverScale, overwritable, rng
                    );
                } else {
                    final int[] end = endsCopy.get(rng.nextInt(endsCopy.size()));
                    painted += RiverDrunkardCarver.carve(
                        grid, start[0], start[1], end[0], end[1],
                        riverCenterId, lakeId, riverScale, overwritable, rng
                    );
                }
            }
        } else if (workEnds.size() > workStarts.size() && !workStarts.isEmpty()) {
            final List<int[]> startsCopy = new ArrayList<>(workStarts);
            while (!workEnds.isEmpty()) {
                final int[] end = removeRandom(workEnds, rng);
                if (!workStarts.isEmpty()) {
                    final int[] start = workStarts.remove(0);
                    painted += RiverDrunkardCarver.carve(
                        grid, start[0], start[1], end[0], end[1],
                        riverCenterId, lakeId, riverScale, overwritable, rng
                    );
                } else {
                    final int[] start = startsCopy.get(rng.nextInt(startsCopy.size()));
                    painted += RiverDrunkardCarver.carve(
                        grid, start[0], start[1], end[0], end[1],
                        riverCenterId, lakeId, riverScale, overwritable, rng
                    );
                }
            }
        } else if (!workEnds.isEmpty()) {
            if (workStarts.size() != workEnds.size()) {
                workStarts.add(new int[] {
                    rng.nextInt(Math.max(1, grid.width() / 2)) + grid.width() / 4,
                    rng.nextInt(Math.max(1, grid.height() / 2)) + grid.height() / 4
                });
            }
            final int pairCount = Math.min(workStarts.size(), workEnds.size());
            for (int i = 0; i < pairCount; i++) {
                final int[] start = workStarts.get(i);
                final int[] end = workEnds.get(i);
                painted += RiverDrunkardCarver.carve(
                    grid, start[0], start[1], end[0], end[1],
                    riverCenterId, lakeId, riverScale, overwritable, rng
                );
            }
        }
        return painted;
    }

    private static int carveLakeOrFallbackSegment(
        final OvermapGrid grid,
        final String lakeId,
        final String riverCenterId,
        final double riverScale,
        final Set<String> overwritable,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng
    ) {
        final List<int[]> endpoints = collectLakeShoreEndpoints(grid, lakeId, options, registry);
        final int startX;
        final int startY;
        final int endX;
        final int endY;
        if (endpoints.size() >= 2) {
            final int[] start = endpoints.get(rng.nextInt(endpoints.size()));
            int[] end = endpoints.get(rng.nextInt(endpoints.size()));
            int guard = 8;
            while ((end[0] == start[0] && end[1] == start[1]) && guard-- > 0) {
                end = endpoints.get(rng.nextInt(endpoints.size()));
            }
            startX = start[0];
            startY = start[1];
            endX = end[0];
            endY = end[1];
        } else {
            startX = rng.nextInt(Math.max(1, grid.width()));
            startY = 0;
            endX = rng.nextInt(Math.max(1, grid.width()));
            endY = grid.height() - 1;
        }
        return RiverDrunkardCarver.carve(
            grid,
            startX,
            startY,
            endX,
            endY,
            riverCenterId,
            lakeId,
            riverScale,
            overwritable,
            rng
        );
    }

    private static int[] removeRandom(final List<int[]> points, final Random rng) {
        final int index = rng.nextInt(points.size());
        return points.remove(index);
    }

    private static double resolveRiverScale(final RegionSettingsDefinition region) {
        if (region == null) {
            return DEFAULT_RIVER_SCALE;
        }
        return region.getRiverScale();
    }

    private static List<int[]> collectLakeShoreEndpoints(
        final OvermapGrid grid,
        final String lakeId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        final List<int[]> endpoints = new ArrayList<>();
        if (grid == null) {
            return endpoints;
        }
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String omtId = grid.getOmtId(x, y);
                if (!HydrologyTerrainClassifier.isLakeOmt(omtId, options, registry)
                    && !lakeId.equals(omtId)) {
                    continue;
                }
                if (isLakeShore(grid, x, y, lakeId, options, registry)) {
                    endpoints.add(new int[] { x, y });
                }
            }
        }
        return endpoints;
    }

    private static boolean isLakeShore(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String lakeId,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry
    ) {
        return !HydrologyTerrainClassifier.isRiverOrLakeAt(grid, x + 1, y, options, registry)
            || !HydrologyTerrainClassifier.isRiverOrLakeAt(grid, x - 1, y, options, registry)
            || !HydrologyTerrainClassifier.isRiverOrLakeAt(grid, x, y + 1, options, registry)
            || !HydrologyTerrainClassifier.isRiverOrLakeAt(grid, x, y - 1, options, registry);
    }

    private static int paintRiverBanksFromCenter(
        final OvermapGrid grid,
        final String centerId,
        final String bankId,
        final String lakeId,
        final Set<String> overwritableIds
    ) {
        int painted = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (!centerId.equals(grid.getOmtId(x, y))) {
                    continue;
                }
                painted += paintBankAt(grid, x + 1, y, bankId, centerId, lakeId, overwritableIds);
                painted += paintBankAt(grid, x - 1, y, bankId, centerId, lakeId, overwritableIds);
                painted += paintBankAt(grid, x, y + 1, bankId, centerId, lakeId, overwritableIds);
                painted += paintBankAt(grid, x, y - 1, bankId, centerId, lakeId, overwritableIds);
            }
        }
        return painted;
    }

    private static int paintBankAt(
        final OvermapGrid grid,
        final int x,
        final int y,
        final String bankId,
        final String centerId,
        final String lakeId,
        final Set<String> overwritableIds
    ) {
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return 0;
        }
        final String current = grid.getOmtId(x, y);
        if (centerId.equals(current) || (lakeId != null && lakeId.equals(current))) {
            return 0;
        }
        if (!overwritableIds.contains(current)) {
            return 0;
        }
        grid.setOmtId(x, y, bankId);
        return 1;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
