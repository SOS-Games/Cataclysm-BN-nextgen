package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Parses {@code type: mapgen} JSON objects (P2). */
public final class JsonMapgenParser {

    private JsonMapgenParser() {}

    public static Optional<JsonMapgenDefinition> parse(
        final JsonValue root,
        final Path sourceFile,
        final int indexInFile
    ) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        if (!"mapgen".equals(root.getString("type", ""))) {
            return Optional.empty();
        }

        final JsonValue objectRoot = root.get("object");
        if (objectRoot == null || !objectRoot.isObject()) {
            return Optional.empty();
        }

        return Optional.of(new JsonMapgenDefinition(
            parseOmTerrain(root.get("om_terrain")),
            root.getString("method", ""),
            root.getInt("weight", 1000),
            root.getBoolean("disabled", false),
            sourceFile,
            indexInFile,
            objectRoot
        ));
    }

    private static List<String> parseOmTerrain(final JsonValue value) {
        final List<String> ids = new ArrayList<>();
        if (value == null) {
            return ids;
        }
        if (value.isString()) {
            final String id = value.asString();
            if (id != null && !id.isEmpty()) {
                ids.add(id);
            }
            return ids;
        }
        if (value.isArray()) {
            for (JsonValue child = value.child; child != null; child = child.next) {
                if (child.isString()) {
                    final String id = child.asString();
                    if (id != null && !id.isEmpty()) {
                        ids.add(id);
                    }
                }
            }
        }
        return ids;
    }
}
