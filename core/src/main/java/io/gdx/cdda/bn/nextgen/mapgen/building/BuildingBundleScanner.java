package io.gdx.cdda.bn.nextgen.mapgen.building;

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

/** P7a — discovers building bundles from all mod JSON, not only standard overmap subpaths. */
public final class BuildingBundleScanner {

    private static final String CITY_BUILDING_TYPE = "city_building";
    private static final String OVERMAP_SPECIAL_TYPE = "overmap_special";

    private BuildingBundleScanner() {}

    public static CityBuildingRegistry load(final MapgenScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final Map<String, CityBuildingDefinition> byId = new LinkedHashMap<>();

        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());

        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            scanModContent(modInfo.getResolvedContentPath(), byId, warnings);
        }

        return new CityBuildingRegistry(byId, warnings);
    }

    private static void scanModContent(
        final Path contentRoot,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(contentRoot)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(contentRoot, Collections.emptyList());
        for (final Path file : files) {
            ingestBundleFile(file, byId, warnings);
        }
    }

    private static void ingestBundleFile(
        final Path file,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                final String type = object.getType();
                if (CITY_BUILDING_TYPE.equals(type)) {
                    CityBuildingLoader.parseBuilding(object.getRoot(), file, byId, warnings);
                } else if (OVERMAP_SPECIAL_TYPE.equals(type)) {
                    OvermapSpecialBuildingLoader.parseSpecial(object.getRoot(), file, byId, warnings);
                }
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse bundle file " + file + ": " + e.getMessage());
        }
    }
}
