package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.mapgen.palette.MergedFormatPlacings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Collects spawn markers from per-char {@code items} / {@code monster} format placings. */
public final class FormatPlacingCollector {

    private FormatPlacingCollector() {}

    public static List<SpawnMarker> collectFromRows(
        final List<String> rows,
        final MergedFormatPlacings placings,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (rows == null || rows.isEmpty() || placings == null || rng == null) {
            return List.of();
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final List<SpawnMarker> markers = new ArrayList<>();
        final Map<Integer, JsonValue> items = placings.getItemsByCodePoint();
        final Map<Integer, JsonValue> monsters = placings.getMonsterByCodePoint();
        if (items.isEmpty() && monsters.isEmpty()) {
            return List.of();
        }

        for (int y = 0; y < rows.size(); y++) {
            final String row = rows.get(y);
            final int rowWidth = RowsInterpreter.rowWidth(row);
            for (int x = 0; x < rowWidth; x++) {
                final int codePoint = RowsInterpreter.codePointAtColumn(row, x);
                final JsonValue itemNode = items.get(codePoint);
                if (itemNode != null) {
                    collectItemNodes(itemNode, x, y, markers, runOptions, rng);
                }
                final JsonValue monsterNode = monsters.get(codePoint);
                if (monsterNode != null) {
                    collectMonsterNodes(monsterNode, x, y, markers, runOptions, rng);
                }
            }
        }
        return List.copyOf(markers);
    }

    private static void collectItemNodes(
        final JsonValue node,
        final int x,
        final int y,
        final List<SpawnMarker> markers,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonValue child = node.child; child != null; child = child.next) {
                collectItemNodes(child, x, y, markers, options, rng);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        final String groupId = readItemGroupId(node, options);
        if (groupId == null || groupId.isEmpty()) {
            return;
        }
        final String displayName = PlaceSpawnerApplier.resolveItemGroupDisplayName(groupId, options);
        if (displayName == null) {
            return;
        }
        final int chance = JmapgenIntRange.rollOptional(node, "chance", 100, rng);
        if (chance <= 0 || chance > 100) {
            return;
        }
        final int repeat = JmapgenIntRange.rollOptional(node, "repeat", 1, rng);
        for (int attempt = 0; attempt < repeat; attempt++) {
            if (chance != 100 && rng.nextInt(100) >= chance) {
                continue;
            }
            markers.add(new SpawnMarker(SpawnMarker.Kind.ITEM_GROUP, groupId, displayName, x, y, 0f));
        }
    }

    private static void collectMonsterNodes(
        final JsonValue node,
        final int x,
        final int y,
        final List<SpawnMarker> markers,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (node == null) {
            return;
        }
        if (node.isArray()) {
            for (JsonValue child = node.child; child != null; child = child.next) {
                collectMonsterNodes(child, x, y, markers, options, rng);
            }
            return;
        }
        if (!node.isObject()) {
            return;
        }
        final String monsterId = PlaceSpawnerApplier.readFixedMonsterId(node, options);
        if (monsterId == null || monsterId.isEmpty()) {
            return;
        }
        final String displayName = PlaceSpawnerApplier.resolveMonsterDisplayName(monsterId, options);
        if (displayName == null) {
            return;
        }
        final int chance = JmapgenIntRange.rollOptional(node, "chance", 100, rng);
        if (chance <= 0 || chance > 100) {
            return;
        }
        if (chance != 100 && rng.nextInt(100) >= chance) {
            return;
        }
        markers.add(
            new SpawnMarker(SpawnMarker.Kind.MONSTER_GROUP, monsterId, displayName, x, y, 1f)
        );
    }

    private static String readItemGroupId(final JsonValue entry, final JsonMapgenRunOptions options) {
        String groupId = entry.getString("item", null);
        if (groupId == null || groupId.isEmpty()) {
            groupId = entry.getString("items", null);
        }
        if (groupId == null || groupId.isEmpty()) {
            options.addWarning("skipped format items entry: missing item/items");
        }
        return groupId;
    }
}
