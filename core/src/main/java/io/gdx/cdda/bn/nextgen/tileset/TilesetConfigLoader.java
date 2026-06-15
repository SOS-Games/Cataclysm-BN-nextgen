package io.gdx.cdda.bn.nextgen.tileset;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves load paths and opens {@code tile_config.json} (units 04a, 04b entry). */
public final class TilesetConfigLoader {

    private TilesetConfigLoader() {}

    public static ResolvedTilesetPaths resolvePaths(
        final TilesetRegistry registry,
        final String tilesetId
    ) {
        final Path tilesetRoot = registry.findDirectory(tilesetId).orElseThrow(
            () -> new IllegalArgumentException("Unknown tileset id: " + tilesetId)
        );
        final Path manifestFile = tilesetRoot.resolve(TilesetManifestParsers.MANIFEST_FILE_NAME);
        final TilesetManifestParsers.LoadManifest manifest =
            TilesetManifestParsers.parseLoadManifest(manifestFile);
        final Path jsonPath = tilesetRoot.resolve(manifest.getJsonRelativePath()).normalize();
        final Path imagePath = tilesetRoot.resolve(manifest.getImageRelativePath()).normalize();
        return new ResolvedTilesetPaths(tilesetId, tilesetRoot, jsonPath, imagePath, manifest);
    }

    public static JsonValue openConfigJson(final Path jsonPath) throws IOException {
        if (!Files.isRegularFile(jsonPath)) {
            throw new IOException("Tile config not found: " + jsonPath);
        }
        final byte[] bytes = Files.readAllBytes(jsonPath);
        final String text = new String(bytes, StandardCharsets.UTF_8);
        return new JsonReader().parse(text);
    }

    public static LoadedTilesetConfig loadConfig(
        final TilesetRegistry registry,
        final String tilesetId
    ) throws IOException {
        final ResolvedTilesetPaths paths = resolvePaths(registry, tilesetId);
        final JsonValue root = openConfigJson(paths.getJsonPath());
        if (!root.has("tile_info")) {
            throw new IOException("tile_config missing required \"tile_info\": " + paths.getJsonPath());
        }
        return new LoadedTilesetConfig(paths, root);
    }

    public static final class ResolvedTilesetPaths {
        private final String tilesetId;
        private final Path tilesetRoot;
        private final Path jsonPath;
        private final Path imagePath;
        private final TilesetManifestParsers.LoadManifest manifest;

        public ResolvedTilesetPaths(
            final String tilesetId,
            final Path tilesetRoot,
            final Path jsonPath,
            final Path imagePath,
            final TilesetManifestParsers.LoadManifest manifest
        ) {
            this.tilesetId = tilesetId;
            this.tilesetRoot = tilesetRoot;
            this.jsonPath = jsonPath;
            this.imagePath = imagePath;
            this.manifest = manifest;
        }

        public String getTilesetId() {
            return tilesetId;
        }

        public Path getTilesetRoot() {
            return tilesetRoot;
        }

        public Path getJsonPath() {
            return jsonPath;
        }

        public Path getImagePath() {
            return imagePath;
        }

        public TilesetManifestParsers.LoadManifest getManifest() {
            return manifest;
        }
    }

    public static final class LoadedTilesetConfig {
        private final ResolvedTilesetPaths paths;
        private final JsonValue configRoot;

        public LoadedTilesetConfig(final ResolvedTilesetPaths paths, final JsonValue configRoot) {
            this.paths = paths;
            this.configRoot = configRoot;
        }

        public ResolvedTilesetPaths getPaths() {
            return paths;
        }

        public JsonValue getConfigRoot() {
            return configRoot;
        }
    }
}
