package io.gdx.cdda.bn.nextgen.worldgen.connection;

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

/** Scans BN overmap connection JSON into {@link OvermapConnectionRegistry} (W5). */
public final class OvermapConnectionLoader {

    private OvermapConnectionLoader() {}

    public static OvermapConnectionLoadResult load(final OvermapConnectionScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final OvermapConnectionRegistry registry = new OvermapConnectionRegistry();
        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());

        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            loadFromContentRoot(modInfo.getResolvedContentPath(), registry, warnings);
        }

        for (final Path dataRoot : options.getDataRoots()) {
            loadFromContentRoot(dataRoot.resolve("json").normalize(), registry, warnings);
        }

        return new OvermapConnectionLoadResult(registry, warnings);
    }

    private static void loadFromContentRoot(
        final Path contentRoot,
        final OvermapConnectionRegistry registry,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(contentRoot)) {
            return;
        }
        final Path overmapRoot = contentRoot.resolve("overmap");
        if (!Files.isDirectory(overmapRoot)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(overmapRoot, Collections.emptyList());
        for (final Path file : files) {
            loadFromFile(file, registry, warnings);
        }
    }

    private static void loadFromFile(
        final Path file,
        final OvermapConnectionRegistry registry,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if (!"overmap_connection".equals(object.getType())) {
                    continue;
                }
                final OvermapConnectionDefinition definition = OvermapConnectionParser.parseObject(object.getRoot());
                if (definition != null) {
                    registry.put(definition);
                }
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse overmap connection file " + file + ": " + e.getMessage());
        }
    }
}
