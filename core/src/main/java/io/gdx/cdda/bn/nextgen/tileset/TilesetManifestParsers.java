package io.gdx.cdda.bn.nextgen.tileset;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Scanner;

/**
 * Parsers for {@code tileset.txt}. Discovery (unit 02) and load-time (unit 04a) use different
 * rules and must not be merged.
 */
public final class TilesetManifestParsers {

    public static final String MANIFEST_FILE_NAME = "tileset.txt";
    public static final String DEFAULT_JSON = "tile_config.json";
    public static final String DEFAULT_IMAGE = "tinytile.png";

    private TilesetManifestParsers() {}

    /** Discovery parser: {@code NAME}, {@code VIEW} only; stops after {@code VIEW}. */
    public static Optional<TilesetOption> parseDiscoveryManifest(final Path manifestFile) {
        if (!Files.isRegularFile(manifestFile)) {
            return Optional.empty();
        }
        try (BufferedReader reader = Files.newBufferedReader(manifestFile, StandardCharsets.UTF_8)) {
            String resourceName = "";
            String viewName = "";
            String line;
            while ((line = reader.readLine()) != null) {
                final String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                final Scanner scanner = new Scanner(trimmed);
                if (!scanner.hasNext()) {
                    continue;
                }
                final String token = scanner.next();
                if (token.startsWith("#")) {
                    continue;
                }
                if (token.contains("NAME")) {
                    resourceName = remainderOfLine(trimmed, token, scanner).trim();
                } else if (token.contains("VIEW")) {
                    viewName = remainderOfLine(trimmed, token, scanner).trim();
                    break;
                }
            }
            if (resourceName.isEmpty()) {
                return Optional.empty();
            }
            final String displayName = viewName.isEmpty() ? resourceName : viewName;
            return Optional.of(new TilesetOption(resourceName, displayName));
        } catch (final IOException ignored) {
            return Optional.empty();
        }
    }

    /** Load-time parser: {@code JSON} and {@code TILESET} tokens; reads entire file. */
    public static LoadManifest parseLoadManifest(final Path manifestFile) {
        String jsonPath = "";
        String imagePath = "";
        if (Files.isRegularFile(manifestFile)) {
            try (BufferedReader reader = Files.newBufferedReader(manifestFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    final String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    final Scanner scanner = new Scanner(trimmed);
                    if (!scanner.hasNext()) {
                        continue;
                    }
                    final String token = scanner.next();
                    if (token.startsWith("#")) {
                        continue;
                    }
                    if (token.contains("JSON")) {
                        if (scanner.hasNext()) {
                            jsonPath = scanner.next();
                        }
                    } else if (token.contains("TILESET")) {
                        if (scanner.hasNext()) {
                            imagePath = scanner.next();
                        }
                    }
                }
            } catch (final IOException ignored) {
                // fall through to defaults
            }
        }
        if (jsonPath.isEmpty()) {
            jsonPath = DEFAULT_JSON;
        }
        if (imagePath.isEmpty()) {
            imagePath = DEFAULT_IMAGE;
        }
        return new LoadManifest(jsonPath, imagePath);
    }

    private static String remainderOfLine(
        final String line,
        final String firstToken,
        final Scanner scanner
    ) {
        final StringBuilder builder = new StringBuilder();
        while (scanner.hasNext()) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(scanner.next());
        }
        if (builder.length() > 0) {
            return builder.toString();
        }
        final int tokenIndex = line.indexOf(firstToken);
        if (tokenIndex < 0) {
            return "";
        }
        String rest = line.substring(tokenIndex + firstToken.length());
        if (rest.startsWith(":")) {
            rest = rest.substring(1);
        }
        return rest.trim();
    }

    public static final class LoadManifest {
        private final String jsonRelativePath;
        private final String imageRelativePath;

        public LoadManifest(final String jsonRelativePath, final String imageRelativePath) {
            this.jsonRelativePath = jsonRelativePath;
            this.imageRelativePath = imageRelativePath;
        }

        public String getJsonRelativePath() {
            return jsonRelativePath;
        }

        public String getImageRelativePath() {
            return imageRelativePath;
        }
    }
}
