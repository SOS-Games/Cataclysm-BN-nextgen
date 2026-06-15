package io.gdx.cdda.bn.nextgen.tileset.mod;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Scans mod data directories and registers {@code mod_tileset} JSON files. */
public final class ModTilesetDiscovery {

    private static final Logger LOG = Logger.getLogger(ModTilesetDiscovery.class.getName());

    private ModTilesetDiscovery() {}

    public static ModTilesetRegistry build() {
        return build(io.gdx.cdda.bn.nextgen.tileset.ModPaths.modRoots());
    }

    public static ModTilesetRegistry build(final Iterable<Path> modRoots) {
        final ModTilesetRegistry registry = ModTilesetRegistry.empty();
        for (final Path modRoot : modRoots) {
            scanRoot(modRoot, registry);
        }
        return registry;
    }

    private static void scanRoot(final Path modRoot, final ModTilesetRegistry registry) {
        if (!Files.isDirectory(modRoot)) {
            return;
        }
        try {
            Files.walkFileTree(modRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
                    if (file.getFileName().toString().endsWith(".json")) {
                        registerJsonFile(file, registry);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "Failed to scan mod root {0}: {1}", new Object[] { modRoot, e.getMessage() });
        }
    }

    public static void registerJsonFile(final Path jsonFile, final ModTilesetRegistry registry) {
        if (!Files.isRegularFile(jsonFile)) {
            return;
        }
        try {
            final byte[] bytes = Files.readAllBytes(jsonFile);
            final String text = new String(bytes, StandardCharsets.UTF_8);
            final JsonValue root = new JsonReader().parse(text);
            final Path basePath = jsonFile.getParent();
            ModTilesetRegistrar.registerFromJson(root, basePath, jsonFile, registry);
        } catch (final Exception e) {
            LOG.log(Level.FINE, "Skipping mod json {0}: {1}", new Object[] { jsonFile, e.getMessage() });
        }
    }
}
