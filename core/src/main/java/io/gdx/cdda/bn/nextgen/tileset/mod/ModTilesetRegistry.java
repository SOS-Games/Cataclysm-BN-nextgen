package io.gdx.cdda.bn.nextgen.tileset.mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registry of mod tilesets to merge at load time (unit 04f). */
public final class ModTilesetRegistry {

    private final List<ModTilesetEntry> entries = new ArrayList<>();

    public static ModTilesetRegistry empty() {
        return new ModTilesetRegistry();
    }

    public void clear() {
        entries.clear();
    }

    public void register(final ModTilesetEntry entry) {
        entries.add(entry);
    }

    public List<ModTilesetEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public int countForFullPath(final java.nio.file.Path fullPath) {
        int count = 0;
        final java.nio.file.Path normalized = fullPath.toAbsolutePath().normalize();
        for (final ModTilesetEntry entry : entries) {
            if (entry.getFullPath().toAbsolutePath().normalize().equals(normalized)) {
                count++;
            }
        }
        return count;
    }
}
