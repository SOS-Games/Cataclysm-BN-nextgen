package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.gamedata.ModDiscovery;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanRoots;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scans {@code mapgen/} and indexes {@code type: mapgen} JSON (P2). */
public final class JsonMapgenLoader {

    private static final String MAPGEN_TYPE = "mapgen";

    private JsonMapgenLoader() {}

    public static MapgenCatalogResult load(final MapgenScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final List<JsonMapgenDefinition> definitions = new ArrayList<>();
        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());

        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            if (options.isIncludeMapgenTree()) {
                final Path contentRoot = modInfo.getResolvedContentPath();
                for (final String dir : MapgenScanRoots.mapgenDirs()) {
                    loadFromTree(contentRoot.resolve(dir), definitions, warnings);
                }
            }
        }

        return new MapgenCatalogResult(new MapgenCatalog(definitions), warnings);
    }

    private static void loadFromTree(
        final Path scanRoot,
        final List<JsonMapgenDefinition> definitions,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(scanRoot)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(scanRoot, Collections.emptyList());
        for (final Path file : files) {
            loadFromFile(file, definitions, warnings);
        }
    }

    private static void loadFromFile(
        final Path file,
        final List<JsonMapgenDefinition> definitions,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            int mapgenIndex = 0;
            for (final JsonDataObject object : objects) {
                if (!MAPGEN_TYPE.equals(object.getType())) {
                    continue;
                }
                final JsonMapgenDefinition definition = JsonMapgenParser.parse(
                    object.getRoot(),
                    file,
                    mapgenIndex
                ).orElse(null);
                if (definition != null) {
                    definitions.add(definition);
                } else {
                    warnings.add("skipped mapgen without object in " + file + "#" + mapgenIndex);
                }
                mapgenIndex++;
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse mapgen file " + file + ": " + e.getMessage());
        }
    }
}
