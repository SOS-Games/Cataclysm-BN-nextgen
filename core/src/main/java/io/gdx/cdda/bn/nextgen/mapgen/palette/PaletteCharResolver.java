package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonValue;

import java.util.Optional;

/** Resolves palette JSON values to terrain/furniture id strings (P1). */
public final class PaletteCharResolver {

    private PaletteCharResolver() {}

    public static Optional<String> resolveId(final JsonValue value) {
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (value.isString()) {
            final String id = value.asString();
            return id == null || id.isEmpty() ? Optional.empty() : Optional.of(id);
        }
        if (value.isArray()) {
            if (value.size == 0) {
                return Optional.empty();
            }
            return resolveArrayEntry(value.child);
        }
        if (value.isObject()) {
            if (value.has("fallback")) {
                return resolveId(value.get("fallback"));
            }
            if (value.has("param") && value.has("fallback")) {
                return resolveId(value.get("fallback"));
            }
        }
        return Optional.empty();
    }

    private static Optional<String> resolveArrayEntry(final JsonValue entry) {
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isArray()) {
            if (entry.size == 0) {
                return Optional.empty();
            }
            return resolveId(entry.child);
        }
        return resolveId(entry);
    }
}
