package io.gdx.cdda.bn.nextgen.gamedata.cache;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Stamp describing which on-disk JSON files a content pack was built from.
 * Validation is stat-only (path + size + mtime) so cold starts avoid re-reading file bodies.
 */
public final class ContentCacheStamp {

    public static final int FORMAT_VERSION = 1;

    public static final class FileEntry {
        public final String path;
        public final long size;
        public final long mtimeMillis;

        public FileEntry(final String path, final long size, final long mtimeMillis) {
            this.path = path;
            this.size = size;
            this.mtimeMillis = mtimeMillis;
        }
    }

    private final int formatVersion;
    private final String scope;
    private final List<String> dataRoots;
    private final List<String> modIds;
    private final List<FileEntry> files;

    public ContentCacheStamp(
        final int formatVersion,
        final String scope,
        final List<String> dataRoots,
        final List<String> modIds,
        final List<FileEntry> files
    ) {
        this.formatVersion = formatVersion;
        this.scope = scope == null ? "" : scope;
        this.dataRoots = dataRoots == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(dataRoots));
        this.modIds = modIds == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(modIds));
        this.files = files == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(files));
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public String getScope() {
        return scope;
    }

    public List<String> getDataRoots() {
        return dataRoots;
    }

    public List<String> getModIds() {
        return modIds;
    }

    public List<FileEntry> getFiles() {
        return files;
    }

    public static ContentCacheStamp fromPack(
        final String scope,
        final List<Path> dataRoots,
        final List<String> modIds,
        final JsonFilePack pack
    ) throws IOException {
        final List<String> rootStrings = new ArrayList<>();
        if (dataRoots != null) {
            for (final Path root : dataRoots) {
                if (root != null) {
                    rootStrings.add(JsonFilePack.normalizeKey(root));
                }
            }
        }
        final List<String> mods = modIds == null ? Collections.emptyList() : new ArrayList<>(modIds);
        final List<FileEntry> entries = new ArrayList<>();
        for (final Map.Entry<String, String> entry : pack.asMap().entrySet()) {
            final Path path = Path.of(entry.getKey());
            if (!Files.isRegularFile(path)) {
                // Pack keys are normalized; try original casing via walking isn't needed —
                // store the key as path and skip missing during build (shouldn't happen).
                continue;
            }
            final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            entries.add(new FileEntry(
                JsonFilePack.normalizeKey(path),
                attrs.size(),
                attrs.lastModifiedTime().toMillis()
            ));
        }
        Collections.sort(entries, (a, b) -> a.path.compareTo(b.path));
        return new ContentCacheStamp(FORMAT_VERSION, scope, rootStrings, mods, entries);
    }

    public boolean matchesContext(
        final String expectedScope,
        final List<Path> expectedRoots,
        final List<String> expectedMods
    ) {
        if (formatVersion != FORMAT_VERSION) {
            return false;
        }
        if (!Objects.equals(scope, expectedScope == null ? "" : expectedScope)) {
            return false;
        }
        final List<String> rootStrings = new ArrayList<>();
        if (expectedRoots != null) {
            for (final Path root : expectedRoots) {
                if (root != null) {
                    rootStrings.add(JsonFilePack.normalizeKey(root));
                }
            }
        }
        if (!dataRoots.equals(rootStrings)) {
            return false;
        }
        final List<String> mods = expectedMods == null ? Collections.emptyList() : expectedMods;
        return modIds.equals(mods);
    }

    /** Returns true when every stamped file still exists with the same size and mtime. */
    public boolean matchesDisk() {
        for (final FileEntry entry : files) {
            final Path path = Path.of(entry.path);
            if (!Files.isRegularFile(path)) {
                return false;
            }
            try {
                final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                if (attrs.size() != entry.size) {
                    return false;
                }
                if (attrs.lastModifiedTime().toMillis() != entry.mtimeMillis) {
                    return false;
                }
            } catch (final IOException e) {
                return false;
            }
        }
        return true;
    }

    public String toJson() {
        final StringBuilder sb = new StringBuilder(256 + files.size() * 64);
        sb.append("{\n");
        sb.append("  \"formatVersion\": ").append(formatVersion).append(",\n");
        sb.append("  \"scope\": ").append(jsonString(scope)).append(",\n");
        sb.append("  \"dataRoots\": ").append(jsonStringArray(dataRoots)).append(",\n");
        sb.append("  \"modIds\": ").append(jsonStringArray(modIds)).append(",\n");
        sb.append("  \"files\": [\n");
        for (int i = 0; i < files.size(); i++) {
            final FileEntry entry = files.get(i);
            sb.append("    {\"path\": ").append(jsonString(entry.path))
                .append(", \"size\": ").append(entry.size)
                .append(", \"mtimeMillis\": ").append(entry.mtimeMillis)
                .append('}');
            if (i + 1 < files.size()) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static ContentCacheStamp fromJson(final String json) {
        final JsonValue root = new JsonReader().parse(json);
        final int version = root.getInt("formatVersion", 0);
        final String scope = root.getString("scope", "");
        final List<String> roots = readStringArray(root.get("dataRoots"));
        final List<String> mods = readStringArray(root.get("modIds"));
        final List<FileEntry> files = new ArrayList<>();
        final JsonValue filesNode = root.get("files");
        if (filesNode != null && filesNode.isArray()) {
            for (JsonValue child = filesNode.child; child != null; child = child.next) {
                files.add(new FileEntry(
                    child.getString("path", ""),
                    child.getLong("size", -1L),
                    child.getLong("mtimeMillis", -1L)
                ));
            }
        }
        return new ContentCacheStamp(version, scope, roots, mods, files);
    }

    public void write(final Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, toJson(), StandardCharsets.UTF_8);
    }

    public static ContentCacheStamp read(final Path file) throws IOException {
        final String json = Files.readString(file, StandardCharsets.UTF_8);
        return fromJson(json);
    }

    private static List<String> readStringArray(final JsonValue node) {
        if (node == null || !node.isArray()) {
            return Collections.emptyList();
        }
        final List<String> out = new ArrayList<>();
        for (JsonValue child = node.child; child != null; child = child.next) {
            if (child.isString()) {
                out.add(child.asString());
            }
        }
        return out;
    }

    private static String jsonString(final String value) {
        final String raw = value == null ? "" : value;
        return '"' + raw.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String jsonStringArray(final List<String> values) {
        final StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(jsonString(values.get(i)));
        }
        sb.append(']');
        return sb.toString();
    }

    /** Convenience for building stamp entries while recording. */
    public static Map<String, FileEntry> statFiles(final Iterable<Path> paths) throws IOException {
        final Map<String, FileEntry> out = new LinkedHashMap<>();
        for (final Path path : paths) {
            if (path == null || !Files.isRegularFile(path)) {
                continue;
            }
            final BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            final String key = JsonFilePack.normalizeKey(path);
            out.put(key, new FileEntry(key, attrs.size(), attrs.lastModifiedTime().toMillis()));
        }
        return out;
    }
}
