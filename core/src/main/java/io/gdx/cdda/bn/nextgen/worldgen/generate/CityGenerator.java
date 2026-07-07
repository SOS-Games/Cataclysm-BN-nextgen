package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.region.CityContentWeights;
import io.gdx.cdda.bn.nextgen.worldgen.region.CitySizeSettings;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Urban blob fill + limited multitile placement (W17a). */
public final class CityGenerator {

    private CityGenerator() {}

    public static CityGenerateResult placeAll(
        final OvermapGrid grid,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters,
        final List<io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord> placedBuildings
    ) {
        if (grid == null || options == null) {
            return CityGenerateResult.empty();
        }
        if (shouldUseUrbanFill(options, region, oterRegistry)) {
            return placeUrbanBlobs(
                grid,
                buildings,
                oterRegistry,
                connectionRegistry,
                options,
                region,
                rng,
                warnings,
                placedCenters,
                placedBuildings
            );
        }
        final int multitile = CityPlacer.placeAll(
            grid,
            buildings,
            oterRegistry,
            options,
            region,
            rng,
            warnings,
            placedCenters,
            placedBuildings
        );
        return new CityGenerateResult(0, multitile, 0, Collections.emptyList());
    }

    private static boolean shouldUseUrbanFill(
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final OvermapTerrainRegistry oterRegistry
    ) {
        if (options.isLegacyGenerationOrder() || region == null) {
            return false;
        }
        if (!region.getCitySizeSettings().isEnabled(options.getWorldOptions())) {
            return false;
        }
        final CityContentWeights content = region.getCityContentWeights();
        if (content == null || !content.hasUrbanOmtTables()) {
            return false;
        }
        return !content.getShops().isEmpty()
            || !content.getParks().isEmpty()
            || !content.getFinales().isEmpty()
            || hasOmtHouseWeight(content, oterRegistry);
    }

    private static boolean hasOmtHouseWeight(
        final CityContentWeights content,
        final OvermapTerrainRegistry oterRegistry
    ) {
        if (oterRegistry == null) {
            return false;
        }
        for (final String houseId : content.getHouses().keySet()) {
            if (oterRegistry.contains(houseId)) {
                return true;
            }
        }
        return false;
    }

    private static CityGenerateResult placeUrbanBlobs(
        final OvermapGrid grid,
        final CityBuildingRegistry buildings,
        final OvermapTerrainRegistry oterRegistry,
        final OvermapConnectionRegistry connectionRegistry,
        final OvermapGenerateOptions options,
        final RegionSettingsDefinition region,
        final Random rng,
        final List<String> warnings,
        final List<int[]> placedCenters,
        final List<io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord> placedBuildings
    ) {
        final CitySizeSettings citySize = region == null
            ? CitySizeSettings.disabled()
            : region.getCitySizeSettings().resolve(options.getWorldOptions());
        final CityContentWeights content = region.getCityContentWeights();
        List<int[]> siteCoords = CitySitePicker.pickSites(grid, citySize, options, rng);
        if (siteCoords.isEmpty()) {
            siteCoords = List.of(new int[] { grid.width() / 2, grid.height() / 2 });
        }

        final int baseCitySize = citySize.getCitySize() > 0 ? citySize.getCitySize() : 4;
        final List<UrbanSite> urbanSites = new ArrayList<>();
        int urbanOmts = 0;
        for (final int[] coord : siteCoords) {
            final CityTier tier = CityTier.roll(rng, baseCitySize);
            final UrbanSite site = new UrbanSite(coord[0], coord[1], tier.effectiveRadius(baseCitySize), tier);
            urbanSites.add(site);
            urbanOmts += UrbanOmtPlacer.fillBlob(
                grid,
                site,
                content,
                options,
                oterRegistry,
                rng,
                warnings
            );
            if (placedCenters != null) {
                placedCenters.add(site.center());
            }
        }

        final int localRoadCells = LocalRoadGenerator.carveSites(
            grid,
            urbanSites,
            connectionRegistry,
            options,
            oterRegistry,
            rng,
            warnings
        );

        final int multitileQuota = Math.min(
            options.getCityBuildingQuota(),
            Math.max(1, urbanSites.size())
        );
        final int multitilePlaced = buildings == null || multitileQuota <= 0
            ? 0
            : CityPlacer.placeMultitileAtCitySites(
                grid,
                buildings,
                oterRegistry,
                options,
                region,
                rng,
                warnings,
                placedCenters,
                placedBuildings,
                siteCoords,
                citySize,
                multitileQuota,
                1
            );

        return new CityGenerateResult(urbanOmts, multitilePlaced, localRoadCells, urbanSites);
    }

    public static final class CityGenerateResult {
        private final int urbanOmtsPlaced;
        private final int multitileBuildingsPlaced;
        private final int localRoadCellsPlaced;
        private final List<UrbanSite> urbanSites;

        public CityGenerateResult(
            final int urbanOmtsPlaced,
            final int multitileBuildingsPlaced,
            final int localRoadCellsPlaced,
            final List<UrbanSite> urbanSites
        ) {
            this.urbanOmtsPlaced = Math.max(0, urbanOmtsPlaced);
            this.multitileBuildingsPlaced = Math.max(0, multitileBuildingsPlaced);
            this.localRoadCellsPlaced = Math.max(0, localRoadCellsPlaced);
            this.urbanSites = urbanSites == null || urbanSites.isEmpty()
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(urbanSites));
        }

        public static CityGenerateResult empty() {
            return new CityGenerateResult(0, 0, 0, Collections.emptyList());
        }

        public int getUrbanOmtsPlaced() {
            return urbanOmtsPlaced;
        }

        public int getMultitileBuildingsPlaced() {
            return multitileBuildingsPlaced;
        }

        public int getLocalRoadCellsPlaced() {
            return localRoadCellsPlaced;
        }

        public int totalCityPlacements() {
            return urbanOmtsPlaced + multitileBuildingsPlaced;
        }

        public List<UrbanSite> getUrbanSites() {
            return urbanSites;
        }
    }
}
