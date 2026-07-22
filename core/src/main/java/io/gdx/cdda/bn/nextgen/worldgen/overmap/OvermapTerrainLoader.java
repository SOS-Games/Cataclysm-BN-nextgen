package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import com.badlogic.gdx.utils.JsonValue;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Scans BN overmap terrain JSON into {@link OvermapTerrainRegistry} (W1). */
public final class OvermapTerrainLoader {

    private static final String OVERMAP_TERRAIN_TYPE = "overmap_terrain";

    private OvermapTerrainLoader() {}

    public static OvermapTerrainLoadResult load(final OvermapTerrainScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final OvermapTerrainRegistry registry = new OvermapTerrainRegistry();
        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());

        final List<PendingTerrain> pending = new ArrayList<>();
        final Map<String, JsonValue> parents = new HashMap<>();

        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            collectFromContentRoot(modInfo.getResolvedContentPath(), modId, pending, parents, warnings);
        }

        for (final Path dataRoot : options.getDataRoots()) {
            final Path jsonRoot = dataRoot.resolve("json").normalize();
            collectFromContentRoot(jsonRoot, "core", pending, parents, warnings);
        }

        for (final PendingTerrain entry : pending) {
            final List<String> flags = OvermapTerrainFlagInheritance.resolveFlags(
                entry.root,
                parents,
                new HashSet<>()
            );
            for (final OvermapTerrainDefinition definition
                : OvermapTerrainParser.parseObject(entry.root, entry.sourceMod, flags)) {
                registry.put(definition);
            }
        }

        return new OvermapTerrainLoadResult(registry, warnings);
    }

    private static void collectFromContentRoot(
        final Path contentRoot,
        final String sourceMod,
        final List<PendingTerrain> pending,
        final Map<String, JsonValue> parents,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(contentRoot)) {
            return;
        }
        for (final String dir : OvermapScanRoots.scanDirs()) {
            collectFromTree(contentRoot.resolve(dir), sourceMod, pending, parents, warnings);
        }
        for (final String dir : OvermapScanRoots.coLocatedMapgenDirs()) {
            collectFromTree(contentRoot.resolve(dir), sourceMod, pending, parents, warnings);
        }
    }

    private static void collectFromTree(
        final Path scanRoot,
        final String sourceMod,
        final List<PendingTerrain> pending,
        final Map<String, JsonValue> parents,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(scanRoot)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(scanRoot, Collections.emptyList());
        for (final Path file : files) {
            collectFromFile(file, sourceMod, pending, parents, warnings);
        }
    }

    private static void collectFromFile(
        final Path file,
        final String sourceMod,
        final List<PendingTerrain> pending,
        final Map<String, JsonValue> parents,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if (!OVERMAP_TERRAIN_TYPE.equals(object.getType())) {
                    continue;
                }
                final JsonValue root = object.getRoot();
                OvermapTerrainFlagInheritance.indexParents(root, parents);
                // Abstract-only entries are parents; concretes (with id) are registered.
                if (root.has("abstract") && !root.has("id")) {
                    continue;
                }
                pending.add(new PendingTerrain(root, sourceMod));
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse overmap terrain file " + file + ": " + e.getMessage());
        }
    }

    private static final class PendingTerrain {
        final JsonValue root;
        final String sourceMod;

        PendingTerrain(final JsonValue root, final String sourceMod) {
            this.root = root;
            this.sourceMod = sourceMod;
        }
    }
}
