package io.gdx.cdda.bn.nextgen.gamedata.cache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Disk cache for BN JSON file texts used by mapgen/worldgen loaders.
 * On hit, {@link io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner} serves list/read
 * from an in-memory pack (still parses JSON in process).
 */
public final class JsonContentDiskCache {

    private static final int PACK_VERSION = 1;
    private static final byte[] MAGIC = new byte[] {'B', 'N', 'J', 'P'};

    private static final ThreadLocal<JsonFilePack> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<JsonFilePackBuilder> RECORDING = new ThreadLocal<>();

    @FunctionalInterface
    public interface LoadAction {
        void run() throws IOException;
    }

    private JsonContentDiskCache() {}

    public static Optional<JsonFilePack> activePack() {
        return Optional.ofNullable(ACTIVE.get());
    }

    public static Optional<JsonFilePackBuilder> recordingBuilder() {
        return Optional.ofNullable(RECORDING.get());
    }

    public static void withSession(
        final String scope,
        final List<Path> dataRoots,
        final List<String> modIds,
        final LoadAction load
    ) throws IOException {
        if (ACTIVE.get() != null || RECORDING.get() != null) {
            load.run();
            return;
        }
        if (!ContentCachePaths.isEnabled()) {
            load.run();
            return;
        }

        final Optional<JsonFilePack> hit = tryLoad(scope, dataRoots, modIds);
        if (hit.isPresent()) {
            ACTIVE.set(hit.get());
            try {
                load.run();
            } finally {
                ACTIVE.remove();
            }
            return;
        }

        RECORDING.set(new JsonFilePackBuilder());
        try {
            load.run();
            final JsonFilePack pack = RECORDING.get().build();
            if (pack.size() > 0) {
                save(scope, dataRoots, modIds, pack);
            }
        } finally {
            RECORDING.remove();
        }
    }

    public static Optional<JsonFilePack> tryLoad(
        final String scope,
        final List<Path> dataRoots,
        final List<String> modIds
    ) {
        if (!ContentCachePaths.isEnabled()) {
            return Optional.empty();
        }
        final Path stampPath = ContentCachePaths.stampFile(scope);
        final Path packPath = ContentCachePaths.packFile(scope);
        if (!Files.isRegularFile(stampPath) || !Files.isRegularFile(packPath)) {
            return Optional.empty();
        }
        try {
            final ContentCacheStamp stamp = ContentCacheStamp.read(stampPath);
            if (!stamp.matchesContext(scope, dataRoots, modIds)) {
                return Optional.empty();
            }
            if (!stamp.matchesDisk()) {
                return Optional.empty();
            }
            return Optional.of(readPack(packPath));
        } catch (final IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    public static void save(
        final String scope,
        final List<Path> dataRoots,
        final List<String> modIds,
        final JsonFilePack pack
    ) throws IOException {
        if (!ContentCachePaths.isEnabled() || pack == null || pack.size() == 0) {
            return;
        }
        Files.createDirectories(ContentCachePaths.cacheDirectory());
        final ContentCacheStamp stamp = ContentCacheStamp.fromPack(scope, dataRoots, modIds, pack);
        writePack(ContentCachePaths.packFile(scope), pack);
        stamp.write(ContentCachePaths.stampFile(scope));
    }

    public static JsonFilePack readPack(final Path packPath) throws IOException {
        try (InputStream fileIn = new BufferedInputStream(Files.newInputStream(packPath));
             InputStream gzipIn = new GZIPInputStream(fileIn);
             DataInputStream in = new DataInputStream(gzipIn)) {
            final byte[] magic = in.readNBytes(MAGIC.length);
            if (magic.length != MAGIC.length
                || magic[0] != MAGIC[0]
                || magic[1] != MAGIC[1]
                || magic[2] != MAGIC[2]
                || magic[3] != MAGIC[3]) {
                throw new IOException("bad content pack magic");
            }
            final int version = in.readInt();
            if (version != PACK_VERSION) {
                throw new IOException("unsupported content pack version: " + version);
            }
            final int count = in.readInt();
            if (count < 0) {
                throw new IOException("negative file count in content pack");
            }
            final Map<String, String> map = new LinkedHashMap<>(Math.max(16, count));
            for (int i = 0; i < count; i++) {
                final String path = readUtf(in);
                final String text = readUtf(in);
                map.put(path, text);
            }
            return new JsonFilePack(map);
        }
    }

    public static void writePack(final Path packPath, final JsonFilePack pack) throws IOException {
        Files.createDirectories(packPath.getParent());
        try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(packPath));
             OutputStream gzipOut = new GZIPOutputStream(fileOut);
             DataOutputStream out = new DataOutputStream(gzipOut)) {
            out.write(MAGIC);
            out.writeInt(PACK_VERSION);
            final Map<String, String> map = pack.asMap();
            out.writeInt(map.size());
            for (final Map.Entry<String, String> entry : map.entrySet()) {
                writeUtf(out, entry.getKey());
                writeUtf(out, entry.getValue());
            }
        }
    }

    private static String readUtf(final DataInputStream in) throws IOException {
        final int length = in.readInt();
        if (length < 0) {
            throw new IOException("negative utf length");
        }
        final byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) {
            throw new IOException("truncated utf payload");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeUtf(final DataOutputStream out, final String value) throws IOException {
        final byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}
