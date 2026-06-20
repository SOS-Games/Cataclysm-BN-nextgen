package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import io.gdx.cdda.bn.nextgen.gamedata.ModDiscovery;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scans BN overmap terrain JSON into {@link OvermapTerrainRegistry} (W1). */
public final class OvermapTerrainLoader {

    private static final String OVERMAP_TERRAIN_TYPE = "overmap_terrain";

    private OvermapTerrainLoader() {}

    public static OvermapTerrainLoadResult load(final OvermapTerrainScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final OvermapTerrainRegistry registry = new OvermapTerrainRegistry();
        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());

        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            loadFromContentRoot(modInfo.getResolvedContentPath(), modId, registry, warnings);
        }

        for (final Path dataRoot : options.getDataRoots()) {
            final Path jsonRoot = dataRoot.resolve("json").normalize();
            loadFromContentRoot(jsonRoot, "core", registry, warnings);
        }

        return new OvermapTerrainLoadResult(registry, warnings);
    }

    private static void loadFromContentRoot(
        final Path contentRoot,
        final String sourceMod,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(contentRoot)) {
            return;
        }
        for (final String dir : OvermapScanRoots.scanDirs()) {
            loadFromTree(contentRoot.resolve(dir), sourceMod, registry, warnings);
        }
        for (final String dir : OvermapScanRoots.coLocatedMapgenDirs()) {
            loadFromTree(contentRoot.resolve(dir), sourceMod, registry, warnings);
        }
    }

    private static void loadFromTree(
        final Path scanRoot,
        final String sourceMod,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(scanRoot)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(scanRoot, Collections.emptyList());
        for (final Path file : files) {
            loadFromFile(file, sourceMod, registry, warnings);
        }
    }

    private static void loadFromFile(
        final Path file,
        final String sourceMod,
        final OvermapTerrainRegistry registry,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if (!OVERMAP_TERRAIN_TYPE.equals(object.getType())) {
                    continue;
                }
                for (final OvermapTerrainDefinition definition
                    : OvermapTerrainParser.parseObject(object.getRoot(), sourceMod)) {
                    registry.put(definition);
                }
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse overmap terrain file " + file + ": " + e.getMessage());
        }
    }
}
