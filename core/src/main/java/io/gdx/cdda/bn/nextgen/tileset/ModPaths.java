package io.gdx.cdda.bn.nextgen.tileset;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Resolves BN mod data roots for {@code mod_tileset} discovery (unit 04f). */
public final class ModPaths {

    public static final String MOD_ROOTS_PROPERTY = "cdda.mod.roots";

    private static final List<String> DEFAULT_RELATIVE_MOD_ROOTS = Arrays.asList(
        "data/mods",
        "../Cataclysm-BN/data/mods"
    );

    private ModPaths() {}

    public static List<Path> modRoots() {
        final List<Path> fromProperty = pathsFromProperty(MOD_ROOTS_PROPERTY);
        if (!fromProperty.isEmpty()) {
            return fromProperty;
        }
        return existingDirectories(DEFAULT_RELATIVE_MOD_ROOTS);
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
