package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses {@code subtype: mutable} overmap special JSON (W6 v1). */
public final class MutableSpecialParser {

    private MutableSpecialParser() {}

    public static MutableSpecialDefinition parseObject(final JsonValue root) {
        if (root == null || !root.isObject()) {
            return null;
        }
        if (!"overmap_special".equals(root.getString("type", null))) {
            return null;
        }
        if (!"mutable".equals(root.getString("subtype", null))) {
            return null;
        }
        final String id = root.getString("id", null);
        if (id == null || id.isEmpty()) {
            return null;
        }
        final JsonValue overmaps = root.get("overmaps");
        if (overmaps == null || !overmaps.isObject()) {
            return null;
        }

        final Map<String, MutableOvermapNode> nodes = parseNodes(overmaps);
        if (nodes.isEmpty()) {
            return null;
        }
        final String rootPieceId = root.getString("root", "");
        if (rootPieceId.isEmpty() || !nodes.containsKey(rootPieceId)) {
            return null;
        }

        return new MutableSpecialDefinition(
            id,
            rootPieceId,
            nodes,
            parsePhases(root.get("phases")),
            parseJoinOpposites(root.get("joins"))
        );
    }

    private static Map<String, MutableOvermapNode> parseNodes(final JsonValue overmaps) {
        final Map<String, MutableOvermapNode> nodes = new LinkedHashMap<>();
        for (JsonValue child = overmaps.child; child != null; child = child.next) {
            if (child.name == null || child.name.isEmpty() || !child.isObject()) {
                continue;
            }
            final String overmapTerrainId = child.getString("overmap", "");
            if (overmapTerrainId.isEmpty()) {
                continue;
            }
            final EnumMap<CardinalDirection, String> edgeJoins = new EnumMap<>(CardinalDirection.class);
            for (final CardinalDirection direction : CardinalDirection.values()) {
                final String joinId = parseJoinValue(child.get(direction.name().toLowerCase()));
                if (joinId != null && !joinId.isEmpty()) {
                    edgeJoins.put(direction, joinId);
                }
            }
            nodes.put(child.name, new MutableOvermapNode(child.name, overmapTerrainId, edgeJoins));
        }
        return nodes;
    }

    private static String parseJoinValue(final JsonValue value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.isObject()) {
            return value.getString("id", null);
        }
        return null;
    }

    private static List<MutableSpecialPhase> parsePhases(final JsonValue phasesValue) {
        final List<MutableSpecialPhase> phases = new ArrayList<>();
        if (phasesValue == null || !phasesValue.isArray()) {
            return phases;
        }
        for (JsonValue phaseValue = phasesValue.child; phaseValue != null; phaseValue = phaseValue.next) {
            if (phaseValue == null || !phaseValue.isArray()) {
                continue;
            }
            final List<MutablePhaseEntry> entries = new ArrayList<>();
            for (JsonValue entryValue = phaseValue.child; entryValue != null; entryValue = entryValue.next) {
                final MutablePhaseEntry entry = parsePhaseEntry(entryValue);
                if (entry != null) {
                    entries.add(entry);
                }
            }
            if (!entries.isEmpty()) {
                phases.add(new MutableSpecialPhase(entries));
            }
        }
        return phases;
    }

    private static MutablePhaseEntry parsePhaseEntry(final JsonValue entryValue) {
        if (entryValue == null || !entryValue.isObject()) {
            return null;
        }
        if (entryValue.has("chunk")) {
            return null;
        }
        final String pieceId = entryValue.getString("overmap", "");
        if (pieceId.isEmpty()) {
            return null;
        }
        final int maxCount = entryValue.has("max") && entryValue.get("max").isNumber()
            ? Math.max(0, entryValue.get("max").asInt())
            : 1;
        return new MutablePhaseEntry(pieceId, maxCount);
    }

    private static Map<String, String> parseJoinOpposites(final JsonValue joinsValue) {
        final Map<String, String> opposites = new LinkedHashMap<>();
        if (joinsValue == null || !joinsValue.isArray()) {
            return opposites;
        }
        for (JsonValue joinValue = joinsValue.child; joinValue != null; joinValue = joinValue.next) {
            if (joinValue.isString()) {
                final String id = joinValue.asString();
                opposites.put(id, id);
                continue;
            }
            if (!joinValue.isObject()) {
                continue;
            }
            final String id = joinValue.getString("id", "");
            if (id.isEmpty()) {
                continue;
            }
            final String opposite = joinValue.getString("opposite", id);
            opposites.put(id, opposite);
            opposites.put(opposite, id);
        }
        return opposites;
    }
}
