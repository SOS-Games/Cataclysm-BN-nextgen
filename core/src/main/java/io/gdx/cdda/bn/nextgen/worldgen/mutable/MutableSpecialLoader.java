package io.gdx.cdda.bn.nextgen.worldgen.mutable;

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

/** Scans BN {@code overmap_mutable/} JSON into {@link MutableSpecialRegistry} (W6). */
public final class MutableSpecialLoader {

    private static final String OVERMAP_MUTABLE_DIR = "overmap_mutable";

    private MutableSpecialLoader() {}

    public static MutableSpecialLoadResult load(final MutableSpecialScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final MutableSpecialRegistry registry = new MutableSpecialRegistry();
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

        return new MutableSpecialLoadResult(registry, warnings);
    }

    private static void loadFromContentRoot(
        final Path contentRoot,
        final MutableSpecialRegistry registry,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(contentRoot)) {
            return;
        }
        final Path mutableRoot = contentRoot.resolve("overmap").resolve(OVERMAP_MUTABLE_DIR);
        if (!Files.isDirectory(mutableRoot)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(mutableRoot, Collections.emptyList());
        for (final Path file : files) {
            loadFromFile(file, registry, warnings);
        }
    }

    private static void loadFromFile(
        final Path file,
        final MutableSpecialRegistry registry,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if (!"overmap_special".equals(object.getType())) {
                    continue;
                }
                final MutableSpecialDefinition definition = MutableSpecialParser.parseObject(object.getRoot());
                if (definition != null) {
                    registry.put(definition);
                }
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse mutable special file " + file + ": " + e.getMessage());
        }
    }
}
