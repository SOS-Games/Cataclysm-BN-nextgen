package io.gdx.cdda.bn.nextgen.tileset;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Recursive scan for {@code tileset.txt} manifests (unit 02). */
public final class TilesetScanner {

    private static final Logger LOG = Logger.getLogger(TilesetScanner.class.getName());

    private TilesetScanner() {}

    public static List<Path> findTilesetDirectories(final Path gfxRoot) {
        if (gfxRoot == null || !Files.isDirectory(gfxRoot)) {
            return Collections.emptyList();
        }
        try {
            return Files.walk(gfxRoot, FileVisitOption.FOLLOW_LINKS)
                .filter(Files::isRegularFile)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .filter(TilesetScanner::isTilesetManifest)
                .map(Path::getParent)
                .distinct()
                .sorted(Comparator.comparing(Path::toString))
                .collect(Collectors.toList());
        } catch (final IOException e) {
            LOG.log(Level.WARNING, "Failed to scan gfx root: " + gfxRoot, e);
            return Collections.emptyList();
        }
    }

    private static boolean isTilesetManifest(final Path file) {
        final String name = file.getFileName().toString();
        return name.endsWith(TilesetManifestParsers.MANIFEST_FILE_NAME) && !name.endsWith("~");
    }

    /**
     * Breadth-first directory listing (BN orders directories lexically). Used only where a
     * non-walking fallback is needed; {@link #findTilesetDirectories} is preferred.
     */
    static List<Path> findTilesetDirectoriesBfs(final Path gfxRoot) {
        if (gfxRoot == null || !Files.isDirectory(gfxRoot)) {
            return Collections.emptyList();
        }
        final List<Path> manifests = new ArrayList<>();
        final Deque<Path> queue = new ArrayDeque<>();
        queue.add(gfxRoot.toAbsolutePath().normalize());
        while (!queue.isEmpty()) {
            final Path dir = queue.removeFirst();
            final List<Path> children;
            try {
                children = Files.list(dir)
                    .map(Path::toAbsolutePath)
                    .map(Path::normalize)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
            } catch (final IOException e) {
                continue;
            }
            for (final Path child : children) {
                if (Files.isDirectory(child)) {
                    queue.addLast(child);
                } else if (isTilesetManifest(child)) {
                    manifests.add(child);
                }
            }
        }
        return manifests.stream()
            .map(Path::getParent)
            .distinct()
            .sorted(Comparator.comparing(Path::toString))
            .collect(Collectors.toList());
    }
}
