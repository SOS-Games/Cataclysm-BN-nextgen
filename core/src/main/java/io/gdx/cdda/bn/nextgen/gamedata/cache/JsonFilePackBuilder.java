package io.gdx.cdda.bn.nextgen.gamedata.cache;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mutable builder for {@link JsonFilePack}. */
public final class JsonFilePackBuilder {

    private final Map<String, String> pathToContent = new LinkedHashMap<>();

    public void put(final Path file, final String text) {
        if (file == null || text == null) {
            return;
        }
        pathToContent.put(JsonFilePack.normalizeKey(file), text);
    }

    public void putAll(final Map<String, String> entries) {
        if (entries == null) {
            return;
        }
        for (final Map.Entry<String, String> entry : entries.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            pathToContent.put(JsonFilePack.normalizeKey(entry.getKey()), entry.getValue());
        }
    }

    public int size() {
        return pathToContent.size();
    }

    public JsonFilePack build() {
        return new JsonFilePack(pathToContent);
    }
}
