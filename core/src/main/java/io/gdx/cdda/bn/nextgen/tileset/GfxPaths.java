package io.gdx.cdda.bn.nextgen.tileset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Resolves BN {@code gfx/} search roots from env, system properties, or common relative paths. */
public final class GfxPaths {

    public static final String GFX_ROOTS_PROPERTY = "cdda.gfx.roots";
    public static final String USER_GFX_ROOTS_PROPERTY = "cdda.user.gfx.roots";

    private static final List<String> DEFAULT_RELATIVE_GAME_ROOTS = Arrays.asList(
        "gfx",
        "../Cataclysm-BN/gfx",
        "../CDDA-Tilesets/gfx"
    );

    private GfxPaths() {}

    public static List<Path> gameGfxRoots() {
        final List<Path> fromProperty = pathsFromProperty(GFX_ROOTS_PROPERTY);
        if (!fromProperty.isEmpty()) {
            return fromProperty;
        }
        return existingDirectories(DEFAULT_RELATIVE_GAME_ROOTS);
    }

    public static List<Path> userGfxRoots() {
        final List<Path> fromProperty = pathsFromProperty(USER_GFX_ROOTS_PROPERTY);
        if (!fromProperty.isEmpty()) {
            return fromProperty;
        }
        return Collections.emptyList();
    }

    private static List<Path> pathsFromProperty(final String propertyName) {
        final String raw = System.getProperty(propertyName);
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(Paths::get)
            .map(Path::toAbsolutePath)
            .map(Path::normalize)
            .filter(Files::isDirectory)
            .collect(Collectors.toList());
    }

    private static List<Path> existingDirectories(final List<String> relativePaths) {
        final Path cwd = Paths.get("").toAbsolutePath().normalize();
        final List<Path> found = new ArrayList<>();
        for (final String relative : relativePaths) {
            final Path candidate = cwd.resolve(relative).normalize();
            if (Files.isDirectory(candidate) && !found.contains(candidate)) {
                found.add(candidate);
            }
        }
        return found;
    }
}
