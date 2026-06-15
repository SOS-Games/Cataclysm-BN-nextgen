package io.gdx.cdda.bn.nextgen.tileset;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Builds {@link TilesetRegistry} from gfx search roots (unit 02). */
public final class TilesetDiscovery {

    private static final Logger LOG = Logger.getLogger(TilesetDiscovery.class.getName());

    private static final List<TilesetOption> LEGACY_FALLBACK_OPTIONS = Collections.unmodifiableList(
        Arrays.asList(
            new TilesetOption("hoder", "Hoder's"),
            new TilesetOption("deon", "Deon's")
        )
    );

    private TilesetDiscovery() {}

    public static TilesetRegistry build() {
        return build(GfxPaths.gameGfxRoots(), GfxPaths.userGfxRoots());
    }

    public static TilesetRegistry build(final List<Path> gameGfxRoots, final List<Path> userGfxRoots) {
        final Map<String, Path> directories = new LinkedHashMap<>();
        final List<TilesetOption> options = new ArrayList<>();

        for (final Path root : gameGfxRoots) {
            mergeScan(root, directories, options, false);
        }
        final List<TilesetOption> userOptions = new ArrayList<>();
        for (final Path root : userGfxRoots) {
            mergeScan(root, directories, userOptions, true);
        }
        for (final TilesetOption userOption : userOptions) {
            if (!options.contains(userOption)) {
                options.add(userOption);
            }
        }

        if (options.isEmpty()) {
            options.addAll(LEGACY_FALLBACK_OPTIONS);
        }
        return new TilesetRegistry(directories, options);
    }

    private static void mergeScan(
        final Path gfxRoot,
        final Map<String, Path> directories,
        final List<TilesetOption> options,
        final boolean warnDuplicates
    ) {
        for (final Path tilesetDir : TilesetScanner.findTilesetDirectories(gfxRoot)) {
            final Path manifest = tilesetDir.resolve(TilesetManifestParsers.MANIFEST_FILE_NAME);
            TilesetManifestParsers.parseDiscoveryManifest(manifest).ifPresent(option -> {
                final String id = option.getId();
                if (directories.containsKey(id)) {
                    if (warnDuplicates) {
                        LOG.log(
                            Level.WARNING,
                            "Duplicate tileset name \"{0}\" in {1} (ignored)",
                            new Object[] { id, tilesetDir }
                        );
                    }
                    return;
                }
                directories.put(id, tilesetDir);
                if (!options.contains(option)) {
                    options.add(option);
                }
            });
        }
    }
}
