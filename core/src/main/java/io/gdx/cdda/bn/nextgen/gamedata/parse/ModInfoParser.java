package io.gdx.cdda.bn.nextgen.gamedata.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Parses BN {@code MOD_INFO} objects from {@code modinfo.json} (G5). */
public final class ModInfoParser {

    private ModInfoParser() {}

    public static Optional<ModInfo> parse(final JsonValue root, final Path modinfoPath) {
        if (root == null || !root.isObject()) {
            return Optional.empty();
        }
        if (!"MOD_INFO".equals(root.getString("type", ""))) {
            return Optional.empty();
        }

        final String id = readId(root);
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }

        final List<String> dependencies = readStringArray(root, "dependencies");
        final List<String> conflicts = readStringArray(root, "conflicts");
        if (dependencies.contains(id) || conflicts.contains(id)) {
            return Optional.empty();
        }
        for (final String dependency : dependencies) {
            if (conflicts.contains(dependency)) {
                return Optional.empty();
            }
        }

        final Path modDirectory = modinfoPath.getParent().normalize();
        final Path resolvedContentPath = root.has("path")
            ? modDirectory.resolve(root.getString("path")).normalize()
            : modDirectory;

        return Optional.of(new ModInfo(
            id,
            root.getString("name", id),
            root.getString("description", ""),
            modinfoPath.normalize(),
            modDirectory,
            resolvedContentPath,
            root.getBoolean("core", false),
            dependencies,
            conflicts
        ));
    }

    private static String readId(final JsonValue root) {
        if (root.has("id")) {
            return root.getString("id", null);
        }
        return root.getString("ident", null);
    }

    private static List<String> readStringArray(final JsonValue root, final String field) {
        if (!root.has(field)) {
            return Collections.emptyList();
        }
        final JsonValue array = root.get(field);
        if (array == null || !array.isArray()) {
            return Collections.emptyList();
        }
        final List<String> values = new ArrayList<>();
        for (JsonValue child = array.child; child != null; child = child.next) {
            if (child.isString()) {
                final String value = child.asString();
                if (value != null && !value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values;
    }
}
