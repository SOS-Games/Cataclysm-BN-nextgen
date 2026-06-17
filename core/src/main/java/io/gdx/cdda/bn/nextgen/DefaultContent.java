package io.gdx.cdda.bn.nextgen;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** BN-aligned default tileset and mod selection for nextgen viewers/editors. */
public final class DefaultContent {

    private static final Logger LOG = Logger.getLogger(DefaultContent.class.getName());

    public static final String UNDEAD_PEOPLE_TILESET_ID = "UNDEAD_PEOPLE_BASE";

    public static final String[] PREFERRED_TILESET_IDS = {
        UNDEAD_PEOPLE_TILESET_ID,
        "hoder",
        "retrodays",
        "UltimateCataclysm"
    };

    private static final String DEFAULT_MOD_LIST_RELATIVE = "mods/default.json";

    private DefaultContent() {}

    public static List<String> defaultModIds(final Path dataRoot) {
        if (dataRoot == null) {
            return Collections.emptyList();
        }
        final Path defaultList = dataRoot.resolve(DEFAULT_MOD_LIST_RELATIVE).normalize();
        if (!Files.isRegularFile(defaultList)) {
            return Collections.emptyList();
        }
        try {
            final String text = new String(Files.readAllBytes(defaultList), StandardCharsets.UTF_8);
            final JsonValue root = new JsonReader().parse(text);
            if (!root.isArray()) {
                LOG.log(Level.WARNING, "Ignoring non-array default mod list: {0}", defaultList);
                return Collections.emptyList();
            }
            final List<String> modIds = new ArrayList<>();
            for (JsonValue child = root.child; child != null; child = child.next) {
                if (child.isString()) {
                    final String modId = child.asString();
                    if (modId != null && !modId.isEmpty()) {
                        modIds.add(modId);
                    }
                }
            }
            return Collections.unmodifiableList(modIds);
        } catch (final IOException | RuntimeException e) {
            LOG.log(Level.WARNING, "Failed to read default mod list {0}: {1}", new Object[] { defaultList, e.getMessage() });
            return Collections.emptyList();
        }
    }

    public static List<String> defaultModIds() {
        return defaultModIds(DataPaths.primaryDataRoot());
    }

    public static List<String> defaultModIdsForRoots(final List<Path> dataRoots) {
        if (dataRoots == null || dataRoots.isEmpty()) {
            return Collections.emptyList();
        }
        return defaultModIds(dataRoots.get(0));
    }
}
