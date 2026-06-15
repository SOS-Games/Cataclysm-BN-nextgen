package io.gdx.cdda.bn.nextgen.tileset;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Registry of tileset id → directory containing {@code tileset.txt}. */
public final class TilesetRegistry {

    private final Map<String, Path> directoriesById;
    private final List<TilesetOption> options;

    public TilesetRegistry(
        final Map<String, Path> directoriesById,
        final List<TilesetOption> options
    ) {
        this.directoriesById = Collections.unmodifiableMap(new LinkedHashMap<>(directoriesById));
        this.options = Collections.unmodifiableList(options);
    }

    public Map<String, Path> getDirectoriesById() {
        return directoriesById;
    }

    public List<TilesetOption> getOptions() {
        return options;
    }

    public Optional<Path> findDirectory(final String tilesetId) {
        return Optional.ofNullable(directoriesById.get(tilesetId));
    }

    public boolean contains(final String tilesetId) {
        return directoriesById.containsKey(tilesetId);
    }
}
