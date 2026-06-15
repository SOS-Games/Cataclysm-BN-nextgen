package io.gdx.cdda.bn.nextgen.tileset.mod;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Phase-2 merge of compatible mod tilesets (unit 04f). */
public final class ModTilesetMerger {

    private static final Logger LOG = Logger.getLogger(ModTilesetMerger.class.getName());

    private ModTilesetMerger() {}

    public static void mergeCompatible(
        final String tilesetId,
        final ModTilesetRegistry registry,
        final TilesetLoadContext context,
        final Path baseLegacyImagePath
    ) throws IOException {
        for (final ModTilesetEntry entry : registry.getEntries()) {
            if (!entry.isCompatible(tilesetId)) {
                LOG.log(
                    Level.INFO,
                    "Mod tileset in \"{0}\" is not compatible with {1}.",
                    new Object[] { entry.getFullPath(), tilesetId }
                );
                continue;
            }
            mergeEntry(entry, context, baseLegacyImagePath);
        }
    }

    private static void mergeEntry(
        final ModTilesetEntry entry,
        final TilesetLoadContext context,
        final Path baseLegacyImagePath
    ) throws IOException {
        final Path jsonPath = entry.getFullPath();
        if (!Files.isRegularFile(jsonPath)) {
            throw new IOException("Failed to open mod tileset json: " + jsonPath);
        }
        LOG.log(Level.INFO, "Attempting to load mod tileset JSON {0}", jsonPath);
        final byte[] bytes = Files.readAllBytes(jsonPath);
        final String text = new String(bytes, StandardCharsets.UTF_8);
        final JsonValue root = new JsonReader().parse(text);
        final JsonValue config = resolveConfigObject(root, entry.getNumInFile());
        if (config == null) {
            LOG.log(
                Level.WARNING,
                "No mod_tileset object at index {0} in {1}",
                new Object[] { entry.getNumInFile(), jsonPath }
            );
            return;
        }
        final int spriteIdOffset = context.getOffset();
        context.loadInternal(config, entry.getBasePath(), baseLegacyImagePath, spriteIdOffset);
    }

    static JsonValue resolveConfigObject(final JsonValue root, final int numInFile) {
        if (root.isArray()) {
            int index = 1;
            for (JsonValue child = root.child; child != null; child = child.next) {
                if ("mod_tileset".equals(child.getString("type", ""))) {
                    if (index == numInFile) {
                        return child;
                    }
                    index++;
                }
            }
            return null;
        }
        if (root.isObject()) {
            return root;
        }
        return null;
    }
}
