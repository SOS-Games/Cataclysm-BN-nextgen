package io.gdx.cdda.bn.nextgen.gamedata;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Resolves BN {@code data/} search roots from system properties or common relative paths. */
public final class DataPaths {

    public static final String DATA_ROOTS_PROPERTY = "cdda.data.roots";

    private static final List<String> DEFAULT_RELATIVE_GAME_ROOTS = Arrays.asList(
        "data",
        "../Cataclysm-BN/data"
    );

    /** Nextgen preview JSON (region_settings overlays); appended after BN roots when present. */
    private static final List<String> OVERLAY_RELATIVE_ROOTS = Arrays.asList(
        "data",
        "../data"
    );

    private DataPaths() {}

    public static List<Path> gameDataRoots() {
        final List<Path> fromProperty = pathsFromProperty(DATA_ROOTS_PROPERTY);
        if (!fromProperty.isEmpty()) {
            return appendOverlayRoots(fromProperty);
        }
        return appendOverlayRoots(existingDirectories(DEFAULT_RELATIVE_GAME_ROOTS));
    }

    private static List<Path> appendOverlayRoots(final List<Path> roots) {
        final List<Path> merged = new ArrayList<>(roots);
        for (final Path overlay : existingDirectories(OVERLAY_RELATIVE_ROOTS)) {
            if (!merged.contains(overlay)) {
                merged.add(overlay);
            }
        }
        return merged;
    }

    public static Path primaryDataRoot() {
        final List<Path> roots = gameDataRoots();
        return roots.isEmpty() ? null : roots.get(0);
    }

    public static Path coreJsonRoot() {
        final Path primary = primaryDataRoot();
        return primary == null ? null : primary.resolve("json").normalize();
    }

    public static Path modRoot() {
        final Path primary = primaryDataRoot();
        return primary == null ? null : primary.resolve("mods").normalize();
    }

    private static List<Path> pathsFromProperty(final String propertyName) {
        final String raw = System.getProperty(propertyName);
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.split("[,;]"))
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
