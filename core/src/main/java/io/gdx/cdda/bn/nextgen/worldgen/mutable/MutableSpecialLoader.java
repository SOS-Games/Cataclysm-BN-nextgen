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

/** Scans mod JSON for {@code subtype: mutable} overmap specials (W6 / unit 11). */
public final class MutableSpecialLoader {

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
        final List<Path> files = JsonDataScanner.listJsonFiles(contentRoot, Collections.emptyList());
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
                    registry.put(new MutableSpecialDefinition(
                        definition.getId(),
                        file,
                        definition.getRootPieceId(),
                        definition.getNodes(),
                        definition.getPhases(),
                        definition.getJoinOpposites()
                    ));
                }
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse mutable special file " + file + ": " + e.getMessage());
        }
    }
}
