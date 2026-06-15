package io.gdx.cdda.bn.nextgen.tileset.mod;

import com.badlogic.gdx.utils.JsonValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Phase-1 registration of {@code mod_tileset} JSON (unit 04f). */
public final class ModTilesetRegistrar {

    private ModTilesetRegistrar() {}

    public static void registerFromJson(
        final JsonValue root,
        final Path basePath,
        final Path fullPath,
        final ModTilesetRegistry registry
    ) {
        if (root == null) {
            return;
        }
        if (root.isArray()) {
            for (JsonValue child = root.child; child != null; child = child.next) {
                if (isModTilesetObject(child)) {
                    registerObject(child, basePath, fullPath, registry);
                }
            }
            return;
        }
        if (root.isObject() && isModTilesetObject(root)) {
            registerObject(root, basePath, fullPath, registry);
        }
    }

    private static void registerObject(
        final JsonValue object,
        final Path basePath,
        final Path fullPath,
        final ModTilesetRegistry registry
    ) {
        final int numInFile = registry.countForFullPath(fullPath) + 1;
        final List<String> compatibility = readCompatibility(object);
        registry.register(new ModTilesetEntry(basePath, fullPath, numInFile, compatibility));
    }

    private static List<String> readCompatibility(final JsonValue object) {
        final List<String> compatibility = new ArrayList<>();
        if (!object.has("compatibility")) {
            return compatibility;
        }
        final JsonValue array = object.get("compatibility");
        if (array == null || !array.isArray()) {
            return compatibility;
        }
        for (JsonValue child = array.child; child != null; child = child.next) {
            if (child.isString()) {
                final String id = child.asString();
                if (!id.isEmpty()) {
                    compatibility.add(id);
                }
            }
        }
        return compatibility;
    }

    private static boolean isModTilesetObject(final JsonValue object) {
        return "mod_tileset".equals(object.getString("type", ""));
    }
}
