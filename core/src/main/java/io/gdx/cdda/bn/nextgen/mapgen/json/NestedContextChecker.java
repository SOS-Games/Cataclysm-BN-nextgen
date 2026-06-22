package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Checks nested mapgen neighbor/join/connection constraints (v2.1b). */
public final class NestedContextChecker {

    private NestedContextChecker() {}

    public static boolean matches(final JsonValue entry, final JsonMapgenRunOptions options) {
        if (entry == null || !entry.isObject()) {
            return true;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        return matchesNeighbors(entry.get("neighbors"), runOptions)
            && matchesJoins(entry.get("joins"), runOptions)
            && matchesConnections(entry.get("connections"), runOptions);
    }

    private static boolean matchesNeighbors(
        final JsonValue neighborsNode,
        final JsonMapgenRunOptions options
    ) {
        if (neighborsNode == null || !neighborsNode.isObject()) {
            return true;
        }
        final Map<String, String> actualNeighbors = options.getNeighborsByDirection();
        for (JsonValue child = neighborsNode.child; child != null; child = child.next) {
            if (child.name == null || child.name.isEmpty() || !child.isArray()) {
                continue;
            }
            final String actual = actualNeighbors.get(child.name.toLowerCase(Locale.ROOT));
            if (actual == null || actual.isEmpty()) {
                return false;
            }
            boolean directionMatches = false;
            for (JsonValue allowed = child.child; allowed != null; allowed = allowed.next) {
                if (!allowed.isString()) {
                    continue;
                }
                if (OterMatchUtil.matchesContains(allowed.asString(), actual)) {
                    directionMatches = true;
                    break;
                }
            }
            if (!directionMatches) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesJoins(final JsonValue joinsNode, final JsonMapgenRunOptions options) {
        if (joinsNode == null || !joinsNode.isObject()) {
            return true;
        }
        final Set<String> activeJoins = options.getActiveJoins();
        for (JsonValue child = joinsNode.child; child != null; child = child.next) {
            if (child.name == null || child.name.isEmpty()) {
                continue;
            }
            final String requiredJoin = child.isString() ? child.asString() : null;
            if (requiredJoin == null || requiredJoin.isEmpty()) {
                continue;
            }
            if (!activeJoins.contains(requiredJoin)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesConnections(
        final JsonValue connectionsNode,
        final JsonMapgenRunOptions options
    ) {
        if (connectionsNode == null || !connectionsNode.isObject()) {
            return true;
        }
        final Map<String, String> actualConnections = options.getConnectionsByDirection();
        for (JsonValue child = connectionsNode.child; child != null; child = child.next) {
            if (child.name == null || child.name.isEmpty()) {
                continue;
            }
            final String direction = child.name.toLowerCase(Locale.ROOT);
            final String actual = actualConnections.get(direction);
            if (child.isString()) {
                final String required = child.asString();
                if (required == null || required.isEmpty()) {
                    continue;
                }
                if (actual == null || !required.equals(actual)) {
                    return false;
                }
                continue;
            }
            if (child.isArray()) {
                if (child.size == 0) {
                    continue;
                }
                if (actual == null || actual.isEmpty()) {
                    return false;
                }
                boolean directionMatches = false;
                for (JsonValue allowed = child.child; allowed != null; allowed = allowed.next) {
                    if (!allowed.isString()) {
                        continue;
                    }
                    if (actual.equals(allowed.asString())) {
                        directionMatches = true;
                        break;
                    }
                }
                if (!directionMatches) {
                    return false;
                }
            }
        }
        return true;
    }
}
