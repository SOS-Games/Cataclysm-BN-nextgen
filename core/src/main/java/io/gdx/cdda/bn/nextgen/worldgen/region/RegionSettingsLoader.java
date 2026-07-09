package io.gdx.cdda.bn.nextgen.worldgen.region;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.ModDiscovery;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads overmap {@code region_settings} from BN data roots (W9). */
public final class RegionSettingsLoader {

    private static final String REGION_SETTINGS_TYPE = "region_settings";

    private RegionSettingsLoader() {}

    public static RegionSettingsLoadResult load(final MapgenScanOptions options) throws IOException {
        if (options == null || options.getDataRoots().isEmpty()) {
            return new RegionSettingsLoadResult(RegionSettingsRegistry.empty(), Collections.emptyList());
        }
        final List<String> warnings = new ArrayList<>();
        final Map<String, RegionSettingsDefinition> regions = new LinkedHashMap<>();

        for (final Path dataRoot : options.getDataRoots()) {
            loadFromJsonTree(dataRoot.resolve("json"), regions, warnings);
        }

        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());
        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            loadFromJsonTree(modInfo.getResolvedContentPath().resolve("json"), regions, warnings);
            loadFromJsonTree(modInfo.getResolvedContentPath(), regions, warnings);
        }

        return new RegionSettingsLoadResult(new RegionSettingsRegistry(regions), warnings);
    }

    private static void loadFromJsonTree(
        final Path jsonRoot,
        final Map<String, RegionSettingsDefinition> regions,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(jsonRoot)) {
            return;
        }
        for (final Path file : JsonDataScanner.listJsonFiles(jsonRoot, Collections.emptyList())) {
            try {
                for (final JsonDataObject object : JsonDataScanner.parseFile(file)) {
                    if (!REGION_SETTINGS_TYPE.equals(object.getType())) {
                        continue;
                    }
                    parseRegion(object.getRoot()).ifPresent(definition -> regions.put(definition.getId(), definition));
                }
            } catch (final RuntimeException e) {
                warnings.add("failed to parse region_settings file " + file + ": " + e.getMessage());
            }
        }
    }

    private static java.util.Optional<RegionSettingsDefinition> parseRegion(final JsonValue root) {
        if (root == null || !root.isObject()) {
            return java.util.Optional.empty();
        }
        final String id = root.getString("id", null);
        if (id == null || id.isEmpty()) {
            return java.util.Optional.empty();
        }
        final String defaultOter = root.getString("default_oter", "field");
        final String displayOter = root.getString("display_oter", "");
        final RegionGroundcoverSettings defaultGroundcover = RegionGroundcoverSettings.parse(
            root.get("default_groundcover")
        );
        final JsonValue forestRoot = forestSettingsRoot(root);
        final OvermapForestSettings forestSettings = parseForestSettings(forestRoot);
        final OvermapLakeSettings lakeSettings = parseLakeSettings(root.get("overmap_lake_settings"));
        final JsonValue cityRoot = root.get("city");
        final CityContentWeights cityContentWeights = parseCityContentWeights(cityRoot);
        final CitySizeSettings citySizeSettings = parseCitySizeSettings(cityRoot);
        final OvermapSpecialSettings specialSettings = parseSpecialSettings(root.get("overmap_special_settings"));
        final OvermapTerrainSettings terrainSettings = parseTerrainSettings(forestRoot);
        final ForestTrailSettings forestTrailSettings = parseForestTrailSettings(root.get("forest_trail_settings"));
        final UndergroundNetworkSettings undergroundNetworkSettings = parseUndergroundNetworkSettings(
            root.get("underground_network_settings")
        );
        final double riverScale = root.getDouble("river_scale", 4.0);
        return java.util.Optional.of(new RegionSettingsDefinition(
            id,
            defaultOter,
            displayOter,
            defaultGroundcover,
            forestSettings,
            lakeSettings,
            cityContentWeights,
            citySizeSettings,
            specialSettings,
            terrainSettings,
            forestTrailSettings,
            undergroundNetworkSettings,
            riverScale
        ));
    }

    private static UndergroundNetworkSettings parseUndergroundNetworkSettings(final JsonValue undergroundRoot) {
        if (undergroundRoot == null || !undergroundRoot.isObject()) {
            return UndergroundNetworkSettings.disabled();
        }
        final boolean subways = undergroundRoot.getBoolean("subways", false);
        final boolean rails = undergroundRoot.getBoolean("rails", false);
        final boolean sewers = undergroundRoot.getBoolean("sewers", false);
        if (!subways && !rails && !sewers) {
            return UndergroundNetworkSettings.disabled();
        }
        return new UndergroundNetworkSettings(subways, rails, sewers);
    }

    private static ForestTrailSettings parseForestTrailSettings(final JsonValue trailRoot) {
        if (trailRoot == null || !trailRoot.isObject()) {
            return ForestTrailSettings.disabled();
        }
        final int chance = trailRoot.getInt("chance", 0);
        if (chance <= 0) {
            return ForestTrailSettings.disabled();
        }
        return new ForestTrailSettings(
            chance,
            trailRoot.getInt("border_point_chance", 2),
            trailRoot.getInt("minimum_forest_size", 50),
            trailRoot.getInt("random_point_min", 4),
            trailRoot.getInt("random_point_max", 50),
            trailRoot.getInt("random_point_size_scalar", 100),
            trailRoot.getInt("trailhead_chance", 1),
            trailRoot.getInt("trailhead_road_distance", 6),
            parseWeightMap(trailRoot.get("trailheads"))
        );
    }

    private static OvermapSpecialSettings parseSpecialSettings(final JsonValue specialRoot) {
        if (specialRoot == null || !specialRoot.isObject()) {
            return OvermapSpecialSettings.disabled();
        }
        final JsonValue specialsNode = specialRoot.get("specials");
        final Map<String, Integer> weights = new LinkedHashMap<>();
        if (specialsNode != null && specialsNode.isObject()) {
            for (JsonValue member = specialsNode.child; member != null; member = member.next) {
                if (member.name == null || member.name.isEmpty() || !member.isNumber()) {
                    continue;
                }
                weights.put(member.name, Math.max(1, member.asInt()));
            }
        }
        final int min = specialRoot.getInt("min", 0);
        final int max = specialRoot.getInt("max", min);
        return new OvermapSpecialSettings(weights, min, max);
    }

    private static CitySizeSettings parseCitySizeSettings(final JsonValue cityRoot) {
        if (cityRoot == null || !cityRoot.isObject()) {
            return CitySizeSettings.disabled();
        }
        final int citySize = cityRoot.getInt("city_size", CitySizeSettings.USE_WORLD_OPTION);
        final int citySpacing = cityRoot.getInt("city_spacing", CitySizeSettings.USE_WORLD_OPTION);
        final boolean cityIsolated = cityRoot.getBoolean("city_isolated", false);
        return new CitySizeSettings(citySize, citySpacing, cityIsolated);
    }

    private static OvermapTerrainSettings parseTerrainSettings(final JsonValue forestRoot) {
        if (forestRoot == null || !forestRoot.isObject()) {
            return OvermapTerrainSettings.disabled();
        }
        final double swampAdjacent = forestRoot.getDouble("noise_threshold_swamp_adjacent_water", 0.0);
        final double swampIsolated = forestRoot.getDouble("noise_threshold_swamp_isolated", 0.0);
        final String swampOter = forestRoot.getString("oter_swamp", "forest_water");
        final String beachOter = forestRoot.getString("oter_beach", "beach");
        final boolean enabled = swampAdjacent > 0.0
            || swampIsolated > 0.0
            || forestRoot.has("oter_beach")
            || forestRoot.has("oter_swamp");
        if (!enabled) {
            return OvermapTerrainSettings.disabled();
        }
        return new OvermapTerrainSettings(enabled, swampAdjacent, swampIsolated, swampOter, beachOter);
    }

    private static OvermapLakeSettings parseLakeSettings(final JsonValue lakeRoot) {
        if (lakeRoot == null || !lakeRoot.isObject()) {
            return OvermapLakeSettings.disabled();
        }
        final double threshold = lakeRoot.getDouble("noise_threshold_lake", 0.25);
        final int minSize = lakeRoot.getInt("lake_size_min", 20);
        final String lakeOter = lakeRoot.getString("oter_lake", "lake");
        final String lakeSurface = lakeRoot.getString("oter_lake_surface", "");
        final String lakeShore = lakeRoot.getString("oter_lake_shore", "");
        final List<String> shoreTerrains = new ArrayList<>();
        final JsonValue shore = lakeRoot.get("shore_extendable_overmap_terrain");
        if (shore != null && shore.isArray()) {
            for (JsonValue entry = shore.child; entry != null; entry = entry.next) {
                if (entry.isString() && !entry.asString().isEmpty()) {
                    shoreTerrains.add(entry.asString());
                }
            }
        }
        return new OvermapLakeSettings(true, threshold, minSize, lakeOter, lakeSurface, lakeShore, shoreTerrains);
    }

    private static JsonValue forestSettingsRoot(final JsonValue regionRoot) {
        if (regionRoot == null || !regionRoot.isObject()) {
            return null;
        }
        final JsonValue settings = regionRoot.get("overmap_forest_settings");
        if (settings != null && settings.isObject()) {
            return settings;
        }
        final JsonValue forest = regionRoot.get("overmap_forest");
        if (forest != null && forest.isObject()) {
            return forest;
        }
        return null;
    }

    private static OvermapForestSettings parseForestSettings(final JsonValue forestRoot) {
        if (forestRoot == null || !forestRoot.isObject()) {
            return OvermapForestSettings.defaults();
        }
        final double forest = forestRoot.getDouble("noise_threshold_forest", 0.35);
        final double thick = forestRoot.getDouble("noise_threshold_forest_thick", 0.0);
        final String forestOter = forestRoot.getString("oter_forest", "forest");
        final String thickOter = forestRoot.getString("oter_forest_thick", "forest_thick");
        final int bufferMin = forestRoot.getInt("river_floodplain_buffer_distance_min", 3);
        final int bufferMax = forestRoot.getInt("river_floodplain_buffer_distance_max", 15);
        return new OvermapForestSettings(forest, thick, forestOter, thickOter, bufferMin, bufferMax);
    }

    private static CityContentWeights parseCityContentWeights(final JsonValue cityRoot) {
        if (cityRoot == null || !cityRoot.isObject()) {
            return CityContentWeights.empty();
        }
        return new CityContentWeights(
            parseWeightMap(cityRoot.get("houses")),
            parseWeightMap(cityRoot.get("shops")),
            parseWeightMap(cityRoot.get("parks")),
            parseWeightMap(cityRoot.get("finales"))
        );
    }

    private static Map<String, Integer> parseWeightMap(final JsonValue weightsRoot) {
        if (weightsRoot == null || !weightsRoot.isObject()) {
            return Collections.emptyMap();
        }
        final Map<String, Integer> weights = new LinkedHashMap<>();
        for (JsonValue member = weightsRoot.child; member != null; member = member.next) {
            if (member.name == null || member.name.isEmpty() || !member.isNumber()) {
                continue;
            }
            weights.put(member.name, Math.max(1, member.asInt()));
        }
        return weights;
    }
}
