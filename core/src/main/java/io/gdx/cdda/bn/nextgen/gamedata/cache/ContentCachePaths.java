package io.gdx.cdda.bn.nextgen.gamedata.cache;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/** Resolves the on-disk content cache directory. */
public final class ContentCachePaths {

    public static final String CACHE_DIR_PROPERTY = "cdda.content.cache.dir";
    public static final String CACHE_ENABLED_PROPERTY = "cdda.content.cache";

    private ContentCachePaths() {}

    public static boolean isEnabled() {
        final String raw = System.getProperty(CACHE_ENABLED_PROPERTY, "true");
        final String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return !"false".equals(normalized) && !"0".equals(normalized) && !"off".equals(normalized);
    }

    public static Path cacheDirectory() {
        final String override = System.getProperty(CACHE_DIR_PROPERTY);
        if (override != null && !override.trim().isEmpty()) {
            return Paths.get(override.trim()).toAbsolutePath().normalize();
        }
        final String home = System.getProperty("user.home", ".");
        return Paths.get(home, ".cache", "cdda-bn-nextgen").toAbsolutePath().normalize();
    }

    public static Path stampFile(final String scope) {
        return cacheDirectory().resolve("content-stamp-" + sanitizeScope(scope) + ".json");
    }

    public static Path packFile(final String scope) {
        return cacheDirectory().resolve("content-pack-" + sanitizeScope(scope) + ".bin.gz");
    }

    private static String sanitizeScope(final String scope) {
        if (scope == null || scope.trim().isEmpty()) {
            return "default";
        }
        return scope.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
    }
}
