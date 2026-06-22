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
        final OvermapForestSettings forestSettings = parseForestSettings(root.get("overmap_forest_settings"));
        final OvermapLakeSettings lakeSettings = parseLakeSettings(root.get("overmap_lake_settings"));
        final JsonValue cityRoot = root.get("city");
        final Map<String, Integer> cityHouseWeights = parseCityHouseWeights(cityRoot);
        final CitySizeSettings citySizeSettings = parseCitySizeSettings(cityRoot);
        final OvermapSpecialSettings specialSettings = parseSpecialSettings(root.get("overmap_special_settings"));
        final OvermapTerrainSettings terrainSettings = parseTerrainSettings(root.get("overmap_forest_settings"));
        return java.util.Optional.of(new RegionSettingsDefinition(
            id,
            defaultOter,
            forestSettings,
            lakeSettings,
            cityHouseWeights,
            citySizeSettings,
            specialSettings,
            terrainSettings
        ));
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
        final int citySize = cityRoot.getInt("city_size", 0);
        final int citySpacing = cityRoot.getInt("city_spacing", 0);
        final boolean cityIsolated = cityRoot.getBoolean("city_isolated", false);
        if (citySize <= 0 && citySpacing <= 0 && !cityIsolated) {
            return CitySizeSettings.disabled();
        }
        return new CitySizeSettings(citySize, citySpacing, cityIsolated);
    }

    private static OvermapTerrainSettings parseTerrainSettings(final JsonValue forestRoot) {
        if (forestRoot == null || !forestRoot.isObject()) {
            return OvermapTerrainSettings.disabled();
        }
        final double swampAdjacent = forestRoot.getDouble("noise_threshold_swamp_adjacent_water", 0.0);
        final double swampIsolated = forestRoot.getDouble("noise_threshold_swamp_isolated", 0.0);
        final String swampOter = forestRoot.getString("oter_swamp", "swamp");
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
        final List<String> shoreTerrains = new ArrayList<>();
        final JsonValue shore = lakeRoot.get("shore_extendable_overmap_terrain");
        if (shore != null && shore.isArray()) {
            for (JsonValue entry = shore.child; entry != null; entry = entry.next) {
                if (entry.isString() && !entry.asString().isEmpty()) {
                    shoreTerrains.add(entry.asString());
                }
            }
        }
        return new OvermapLakeSettings(true, threshold, minSize, lakeOter, shoreTerrains);
    }

    private static OvermapForestSettings parseForestSettings(final JsonValue forestRoot) {
        if (forestRoot == null || !forestRoot.isObject()) {
            return OvermapForestSettings.defaults();
        }
        final double forest = forestRoot.getDouble("noise_threshold_forest", 0.35);
        final double thick = forestRoot.getDouble("noise_threshold_forest_thick", 0.0);
        final String forestOter = forestRoot.getString("oter_forest", "forest");
        final String thickOter = forestRoot.getString("oter_forest_thick", "forest_thick");
        return new OvermapForestSettings(forest, thick, forestOter, thickOter);
    }

    private static Map<String, Integer> parseCityHouseWeights(final JsonValue cityRoot) {
        if (cityRoot == null || !cityRoot.isObject()) {
            return Collections.emptyMap();
        }
        final JsonValue houses = cityRoot.get("houses");
        if (houses == null || !houses.isObject()) {
            return Collections.emptyMap();
        }
        final Map<String, Integer> weights = new LinkedHashMap<>();
        for (JsonValue member = houses.child; member != null; member = member.next) {
            if (member.name == null || member.name.isEmpty() || !member.isNumber()) {
                continue;
            }
            weights.put(member.name, Math.max(1, member.asInt()));
        }
        return weights;
    }
}
