package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves BN {@code copy-from} / {@code extend} / {@code delete} for overmap terrain flags.
 * Abstracts are keyed by {@code abstract}; concretes may also be copy-from parents by {@code id}.
 */
final class OvermapTerrainFlagInheritance {

    private OvermapTerrainFlagInheritance() {}

    static List<String> resolveFlags(
        final JsonValue root,
        final Map<String, JsonValue> byAbstractOrId,
        final Set<String> resolving
    ) {
        if (root == null || !root.isObject()) {
            return Collections.emptyList();
        }
        final String selfKey = identityKey(root);
        if (selfKey != null && !resolving.add(selfKey)) {
            return Collections.emptyList();
        }
        try {
            final LinkedHashSet<String> flags = new LinkedHashSet<>();
            final String copyFrom = root.getString("copy-from", "");
            if (copyFrom != null && !copyFrom.isEmpty()) {
                final JsonValue parent = byAbstractOrId.get(copyFrom);
                if (parent != null) {
                    flags.addAll(resolveFlags(parent, byAbstractOrId, resolving));
                }
            }
            if (root.has("flags")) {
                flags.clear();
                flags.addAll(parseFlagArray(root.get("flags")));
            }
            final JsonValue extend = root.get("extend");
            if (extend != null && extend.isObject() && extend.has("flags")) {
                flags.addAll(parseFlagArray(extend.get("flags")));
            }
            final JsonValue delete = root.get("delete");
            if (delete != null && delete.isObject() && delete.has("flags")) {
                flags.removeAll(parseFlagArray(delete.get("flags")));
            }
            return new ArrayList<>(flags);
        } finally {
            if (selfKey != null) {
                resolving.remove(selfKey);
            }
        }
    }

    static String identityKey(final JsonValue root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        final String abs = root.getString("abstract", null);
        if (abs != null && !abs.isEmpty()) {
            return abs;
        }
        final JsonValue idValue = root.get("id");
        if (idValue == null || idValue.isNull()) {
            return null;
        }
        if (idValue.isArray()) {
            final JsonValue first = idValue.child;
            return first == null ? null : first.asString();
        }
        final String id = idValue.asString();
        return id == null || id.isEmpty() ? null : id;
    }

    static void indexParents(final JsonValue root, final Map<String, JsonValue> byAbstractOrId) {
        if (root == null || !root.isObject()) {
            return;
        }
        final String abs = root.getString("abstract", null);
        if (abs != null && !abs.isEmpty()) {
            byAbstractOrId.put(abs, root);
        }
        final JsonValue idValue = root.get("id");
        if (idValue == null || idValue.isNull()) {
            return;
        }
        if (idValue.isArray()) {
            for (JsonValue child = idValue.child; child != null; child = child.next) {
                final String id = child.asString();
                if (id != null && !id.isEmpty()) {
                    byAbstractOrId.putIfAbsent(id, root);
                }
            }
            return;
        }
        final String id = idValue.asString();
        if (id != null && !id.isEmpty()) {
            byAbstractOrId.putIfAbsent(id, root);
        }
    }

    private static List<String> parseFlagArray(final JsonValue flagsValue) {
        if (flagsValue == null || !flagsValue.isArray()) {
            return Collections.emptyList();
        }
        final List<String> flags = new ArrayList<>();
        for (JsonValue child = flagsValue.child; child != null; child = child.next) {
            final String flag = child.asString();
            if (flag != null && !flag.isEmpty()) {
                flags.add(flag);
            }
        }
        return flags;
    }
}
