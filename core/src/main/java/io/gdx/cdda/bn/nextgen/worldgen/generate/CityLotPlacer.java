package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRotator;
import io.gdx.cdda.bn.nextgen.worldgen.region.CityContentWeights;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * BN {@code place_building} + distance zoning (C2). Places 1×1 urban OMTs on street flanks.
 */
public final class CityLotPlacer {

    /** BN {@code BUILDINGCHANCE = 4} → attempt place when {@code !one_in(4)} (~75%). */
    private static final int BUILDING_CHANCE = 4;
    private static final int DEFAULT_SHOP_RADIUS = 30;
    private static final int DEFAULT_SHOP_SIGMA = 50;
    private static final int DEFAULT_PARK_RADIUS = 20;
    private static final int DEFAULT_PARK_SIGMA = 80;

    private CityLotPlacer() {}

    public static int placeLots(
        final OvermapGrid grid,
        final UrbanSite site,
        final List<CityStreetGenerator.StreetNode> streetNodes,
        final CityContentWeights content,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final List<String> warnings
    ) {
        return placeLots(grid, site, streetNodes, content, options, registry, null, rng, warnings);
    }

    public static int placeLots(
        final OvermapGrid grid,
        final UrbanSite site,
        final List<CityStreetGenerator.StreetNode> streetNodes,
        final CityContentWeights content,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition region,
        final Random rng,
        final List<String> warnings
    ) {
        if (grid == null || site == null || streetNodes == null || content == null
            || !content.hasUrbanOmtTables() || rng == null) {
            return 0;
        }
        final Set<String> clearable = new java.util.HashSet<>(
            UrbanTerrainClearables.forCityGrowth(options, registry, region)
        );
        final Set<String> warned = new HashSet<>();
        int placed = 0;
        boolean finalePlaced = false;
        int finaleCounter = site.getTier().isAttemptFinale()
            ? rng.nextInt(Math.max(1, site.getRadius()))
            : -1;

        for (final CityStreetGenerator.StreetNode node : streetNodes) {
            if (node.travelDir < 0) {
                continue;
            }
            final boolean attemptFinale = site.getTier().isAttemptFinale()
                && !finalePlaced
                && finaleCounter == 0;
            if (finaleCounter > 0) {
                finaleCounter--;
            }

            if (rng.nextInt(BUILDING_CHANCE) != 0) {
                final boolean ok = placeOne(
                    grid,
                    site,
                    node.x,
                    node.y,
                    CityStreetGenerator.flankLeft(node.travelDir),
                    content,
                    clearable,
                    options,
                    registry,
                    rng,
                    warned,
                    warnings,
                    attemptFinale && !finalePlaced
                );
                if (ok) {
                    placed++;
                    if (attemptFinale) {
                        finalePlaced = true;
                    }
                } else if (attemptFinale) {
                    finaleCounter = -1;
                }
            }
            if (rng.nextInt(BUILDING_CHANCE) != 0) {
                final boolean ok = placeOne(
                    grid,
                    site,
                    node.x,
                    node.y,
                    CityStreetGenerator.flankRight(node.travelDir),
                    content,
                    clearable,
                    options,
                    registry,
                    rng,
                    warned,
                    warnings,
                    attemptFinale && !finalePlaced
                );
                if (ok) {
                    placed++;
                    if (attemptFinale) {
                        finalePlaced = true;
                    }
                } else if (attemptFinale) {
                    finaleCounter = -1;
                }
            }
        }
        return placed;
    }

    private static boolean placeOne(
        final OvermapGrid grid,
        final UrbanSite site,
        final int streetX,
        final int streetY,
        final int flankDir,
        final CityContentWeights content,
        final Set<String> clearable,
        final OvermapGenerateOptions options,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final Set<String> warned,
        final List<String> warnings,
        final boolean attemptFinale
    ) {
        final int[] lot = CityStreetGenerator.displace(streetX, streetY, flankDir);
        final int x = lot[0];
        final int y = lot[1];
        if (x < 0 || y < 0 || x >= grid.width() || y >= grid.height()) {
            return false;
        }
        final String existing = grid.getOmtId(x, y);
        if (!clearable.contains(existing)) {
            return false;
        }
        for (int retry = 0; retry < 10; retry++) {
            final Optional<String> pick = pickBuilding(
                site,
                x,
                y,
                content,
                registry,
                rng,
                warned,
                warnings,
                attemptFinale
            );
            if (pick.isEmpty()) {
                continue;
            }
            final String oriented = faceTowardStreet(pick.get(), flankDir, registry);
            grid.setOmtId(x, y, oriented);
            return true;
        }
        return false;
    }

    /**
     * BN {@code building_dir = opposite(flank)}: rotatable OMTs face the adjacent street.
     */
    static String faceTowardStreet(
        final String omtId,
        final int flankDir,
        final OvermapTerrainRegistry registry
    ) {
        if (omtId == null || omtId.isEmpty() || flankDir < 0 || flankDir > 3) {
            return omtId;
        }
        if (registry != null) {
            final OvermapTerrainDefinition def = registry.find(omtId).orElse(null);
            if (def != null && !def.isRotatable()) {
                return omtId;
            }
        }
        final int faceDir = CityStreetGenerator.faceStreet(flankDir);
        return OvermapTerrainRotator.rotateId(omtId, faceDir);
    }

    static Optional<String> pickBuilding(
        final UrbanSite site,
        final int lotX,
        final int lotY,
        final CityContentWeights content,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final Set<String> warned,
        final List<String> warnings,
        final boolean attemptFinale
    ) {
        if (attemptFinale && content.getFinales() != null && !content.getFinales().isEmpty()) {
            return WeightedOmtPicker.pick(
                content.getFinales(), registry, rng, warned, warnings, "unknown city finale OMT: "
            );
        }
        final int townSize = Math.max(1, site.getRadius());
        final double dist = Math.hypot(lotX - site.getCenterX(), lotY - site.getCenterY());
        final int townDist = (int) Math.round(dist * 100.0 / townSize);

        final int shopNormal = Math.max(
            (int) Math.round(normalRoll(rng, DEFAULT_SHOP_RADIUS, DEFAULT_SHOP_SIGMA)),
            DEFAULT_SHOP_RADIUS
        );
        final int parkNormal = Math.max(
            (int) Math.round(normalRoll(rng, DEFAULT_PARK_RADIUS, DEFAULT_PARK_SIGMA)),
            DEFAULT_PARK_RADIUS
        );

        if (shopNormal > townDist && !content.getShops().isEmpty()) {
            return WeightedOmtPicker.pick(
                content.getShops(), registry, rng, warned, warnings, "unknown city shop OMT: "
            );
        }
        if (parkNormal > townDist && !content.getParks().isEmpty()) {
            return WeightedOmtPicker.pick(
                content.getParks(), registry, rng, warned, warnings, "unknown city park OMT: "
            );
        }
        if (!content.getHouses().isEmpty()) {
            return WeightedOmtPicker.pick(
                content.getHouses(), registry, rng, warned, warnings, "unknown city house OMT: "
            );
        }
        if (!content.getShops().isEmpty()) {
            return WeightedOmtPicker.pick(
                content.getShops(), registry, rng, warned, warnings, "unknown city shop OMT: "
            );
        }
        if (!content.getParks().isEmpty()) {
            return WeightedOmtPicker.pick(
                content.getParks(), registry, rng, warned, warnings, "unknown city park OMT: "
            );
        }
        return Optional.empty();
    }

    private static double normalRoll(final Random rng, final double mean, final double sigma) {
        return mean + rng.nextGaussian() * Math.max(1.0, sigma);
    }
}
