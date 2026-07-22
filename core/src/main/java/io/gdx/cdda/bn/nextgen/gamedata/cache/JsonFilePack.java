package io.gdx.cdda.bn.nextgen.gamedata.cache;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** In-memory map of absolute JSON paths → file text for cache hits / recording. */
public final class JsonFilePack {

    private final Map<String, String> pathToContent;
    private final List<Path> sortedPaths;

    public JsonFilePack(final Map<String, String> pathToContent) {
        final Map<String, String> copy = new LinkedHashMap<>();
        if (pathToContent != null) {
            for (final Map.Entry<String, String> entry : pathToContent.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null) {
                    continue;
                }
                copy.put(normalizeKey(entry.getKey()), entry.getValue());
            }
        }
        this.pathToContent = Collections.unmodifiableMap(copy);
        final List<Path> paths = new ArrayList<>();
        for (final String key : this.pathToContent.keySet()) {
            paths.add(Path.of(key));
        }
        Collections.sort(paths);
        this.sortedPaths = Collections.unmodifiableList(paths);
    }

    public static String normalizeKey(final Path path) {
        return path == null ? "" : normalizeKey(path.toAbsolutePath().normalize().toString());
    }

    public static String normalizeKey(final String path) {
        if (path == null) {
            return "";
        }
        // Preserve case: lowercasing breaks case-sensitive filesystems.
        return path.replace('\\', '/');
    }

    public int size() {
        return pathToContent.size();
    }

    public Optional<String> getText(final Path file) {
        if (file == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pathToContent.get(normalizeKey(file)));
    }

    public List<Path> listJsonFilesUnder(final Path root) {
        if (root == null) {
            return Collections.emptyList();
        }
        final String prefix = normalizeKey(root);
        final String prefixWithSlash = prefix.endsWith("/") ? prefix : prefix + "/";
        final List<Path> matches = new ArrayList<>();
        for (final Path path : sortedPaths) {
            final String key = normalizeKey(path);
            if (key.equals(prefix) || key.startsWith(prefixWithSlash)) {
                if (key.endsWith(".json")) {
                    matches.add(path);
                }
            }
        }
        return Collections.unmodifiableList(matches);
    }

    public Map<String, String> asMap() {
        return pathToContent;
    }
}
