package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.ItemGroupDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupDefinition;
import io.gdx.cdda.bn.nextgen.map.MapGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Applies mapgen {@code place_terrain} / {@code place_furniture} (P13) and collects entity spawns (P13b). */
public final class PlaceSpawnerApplier {

    private PlaceSpawnerApplier() {}

    public static void applyTerrainAndFurniture(
        final MapGrid grid,
        final JsonValue object,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (grid == null || object == null || rng == null) {
            return;
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        applyArray(grid, object.get("place_terrain"), PlacementKind.TERRAIN, runOptions, rng);
        applyArray(grid, object.get("place_furniture"), PlacementKind.FURNITURE, runOptions, rng);
    }

    public static List<SpawnMarker> collectEntitySpawns(
        final JsonValue object,
        final MapGrid grid,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (object == null || rng == null) {
            return List.of();
        }
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final List<SpawnMarker> markers = new ArrayList<>();
        collectItemSpawns(object.get("place_items"), grid, markers, runOptions, rng);
        collectMonsterSpawns(object.get("place_monsters"), grid, markers, runOptions, rng);
        collectFixedMonsterSpawns(object.get("place_monster"), grid, markers, runOptions, rng);
        collectLootSpawns(object.get("place_loot"), grid, markers, runOptions, rng);
        summarizeSkippedSpawns(markers, runOptions);
        return List.copyOf(markers);
    }

    private static void applyArray(
        final MapGrid grid,
        final JsonValue array,
        final PlacementKind kind,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            applyEntry(grid, entry, kind, options, rng);
        }
    }

    private static void applyEntry(
        final MapGrid grid,
        final JsonValue entry,
        final PlacementKind kind,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (entry == null || !entry.isObject()) {
            options.addWarning("skipped place_* entry: not an object");
            return;
        }
        final String id = readPlacementId(entry, kind, options);
        if (id == null || id.isEmpty()) {
            return;
        }
        if (!entryBounds(entry, grid.width(), grid.height()).inGrid()) {
            options.addWarning("skipped place_" + kind.jsonKey + " entry: out of bounds");
            return;
        }

        final int repeat = JmapgenIntRange.rollOptional(entry, "repeat", 1, rng);
        for (int i = 0; i < repeat; i++) {
            final Point point = rollPoint(entry, rng);
            if (!inBounds(grid, point.x, point.y)) {
                options.addWarning(
                    "place_" + kind.jsonKey + " out of bounds at " + point.x + "," + point.y
                );
                continue;
            }
            if (kind == PlacementKind.TERRAIN) {
                grid.setTerrain(point.x, point.y, id);
            } else {
                grid.setFurniture(point.x, point.y, id);
            }
        }
    }

    private static void collectItemSpawns(
        final JsonValue array,
        final MapGrid grid,
        final List<SpawnMarker> markers,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            if (entry == null || !entry.isObject()) {
                continue;
            }
            final String groupId = readItemGroupId(entry, options);
            if (groupId == null || groupId.isEmpty()) {
                continue;
            }
            final String displayName = resolveItemGroupDisplayName(groupId, options);
            if (displayName == null) {
                continue;
            }
            if (!entryBounds(entry, grid == null ? 0 : grid.width(), grid == null ? 0 : grid.height()).inGrid()) {
                options.addWarning("skipped place_items entry: out of bounds");
                continue;
            }

            final int chance = JmapgenIntRange.rollOptional(entry, "chance", 100, rng);
            if (chance <= 0 || chance > 100) {
                options.addWarning("skipped place_items entry: invalid chance " + chance);
                continue;
            }
            final int spawnCount = rollRemainder(chance / 100.0f, rng);
            for (int i = 0; i < spawnCount; i++) {
                final Point point = rollPoint(entry, rng);
                markers.add(
                    new SpawnMarker(SpawnMarker.Kind.ITEM_GROUP, groupId, displayName, point.x, point.y, 0f)
                );
            }
        }
    }

    private static void collectMonsterSpawns(
        final JsonValue array,
        final MapGrid grid,
        final List<SpawnMarker> markers,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            if (entry == null || !entry.isObject()) {
                continue;
            }
            final String groupId = entry.getString("monster", null);
            if (groupId == null || groupId.isEmpty()) {
                options.addWarning("skipped place_monsters entry: missing monster");
                continue;
            }
            final String displayName = resolveMonsterDisplayName(groupId, options);
            if (displayName == null) {
                continue;
            }
            final Bounds bounds = entryBounds(
                entry,
                grid == null ? 0 : grid.width(),
                grid == null ? 0 : grid.height()
            );
            if (!bounds.inGrid()) {
                options.addWarning("skipped place_monsters entry: out of bounds");
                continue;
            }

            final int chance = JmapgenIntRange.rollOptional(entry, "chance", 1, rng);
            if (chance > 1 && rng.nextInt(chance) != 0) {
                continue;
            }

            final float density = entry.getFloat("density", -1f);
            final float effectiveDensity = density < 0f ? 1f : density;
            final float thenum = effectiveDensity * (10f + rng.nextFloat() * 40f);
            final int spawnCount = rollRemainder(thenum, rng);
            for (int i = 0; i < spawnCount; i++) {
                final Point point = rollPointInBounds(bounds, rng);
                markers.add(
                    new SpawnMarker(
                        SpawnMarker.Kind.MONSTER_GROUP,
                        groupId,
                        displayName,
                        point.x,
                        point.y,
                        effectiveDensity
                    )
                );
            }
        }
    }

    private static void collectFixedMonsterSpawns(
        final JsonValue array,
        final MapGrid grid,
        final List<SpawnMarker> markers,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            if (entry == null || !entry.isObject()) {
                continue;
            }
            final String monsterId = readFixedMonsterId(entry, options);
            if (monsterId == null || monsterId.isEmpty()) {
                continue;
            }
            final String displayName = resolveMonsterDisplayName(monsterId, options);
            if (displayName == null) {
                continue;
            }
            final Bounds bounds = entryBounds(
                entry,
                grid == null ? 0 : grid.width(),
                grid == null ? 0 : grid.height()
            );
            if (!bounds.inGrid()) {
                options.addWarning("skipped place_monster entry: out of bounds");
                continue;
            }

            final int chance = JmapgenIntRange.rollOptional(entry, "chance", 100, rng);
            if (chance <= 0 || chance > 100) {
                options.addWarning("skipped place_monster entry: invalid chance " + chance);
                continue;
            }
            final int repeat = JmapgenIntRange.rollOptional(entry, "repeat", 1, rng);
            final int packSize = JmapgenIntRange.rollOptional(entry, "pack_size", 1, rng);
            for (int attempt = 0; attempt < repeat; attempt++) {
                final int spawnCount;
                if (chance == 100) {
                    spawnCount = packSize;
                } else if (rng.nextInt(100) < chance) {
                    spawnCount = packSize;
                } else {
                    continue;
                }
                for (int i = 0; i < spawnCount; i++) {
                    final Point point = rollPoint(entry, rng);
                    if (grid != null && !inBounds(grid, point.x, point.y)) {
                        options.addWarning(
                            "place_monster out of bounds at " + point.x + "," + point.y
                        );
                        continue;
                    }
                    markers.add(
                        new SpawnMarker(
                            SpawnMarker.Kind.MONSTER_GROUP,
                            monsterId,
                            displayName,
                            point.x,
                            point.y,
                            1f
                        )
                    );
                }
            }
        }
    }

    private static void collectLootSpawns(
        final JsonValue array,
        final MapGrid grid,
        final List<SpawnMarker> markers,
        final JsonMapgenRunOptions options,
        final Random rng
    ) {
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonValue entry = array.child; entry != null; entry = entry.next) {
            if (entry == null || !entry.isObject()) {
                continue;
            }
            String groupId = entry.getString("group", null);
            if (groupId == null || groupId.isEmpty()) {
                groupId = entry.getString("item", null);
            }
            if (groupId == null || groupId.isEmpty()) {
                options.addWarning("skipped place_loot entry: missing item/group");
                continue;
            }
            final String displayName = resolveItemGroupDisplayName(groupId, options);
            if (displayName == null) {
                continue;
            }
            if (!entryBounds(entry, grid == null ? 0 : grid.width(), grid == null ? 0 : grid.height()).inGrid()) {
                options.addWarning("skipped place_loot entry: out of bounds");
                continue;
            }
            final int chance = JmapgenIntRange.rollOptional(entry, "chance", 100, rng);
            if (chance <= 0 || chance > 100) {
                options.addWarning("skipped place_loot entry: invalid chance " + chance);
                continue;
            }
            if (chance != 100 && rng.nextInt(100) >= chance) {
                continue;
            }
            final Point point = rollPoint(entry, rng);
            if (grid != null && !inBounds(grid, point.x, point.y)) {
                options.addWarning("place_loot out of bounds at " + point.x + "," + point.y);
                continue;
            }
            markers.add(
                new SpawnMarker(SpawnMarker.Kind.ITEM_GROUP, groupId, displayName, point.x, point.y, 0f)
            );
        }
    }

    static String readFixedMonsterId(final JsonValue entry, final JsonMapgenRunOptions options) {
        if (entry.has("group")) {
            final String groupId = entry.getString("group", null);
            if (groupId == null || groupId.isEmpty()) {
                options.addWarning("skipped place_monster entry: missing group");
            }
            return groupId;
        }
        final JsonValue monsterField = entry.get("monster");
        if (monsterField == null) {
            options.addWarning("skipped place_monster entry: missing monster/group");
            return null;
        }
        if (monsterField.isString()) {
            return monsterField.asString();
        }
        if (monsterField.isArray() && monsterField.size > 0) {
            final JsonValue first = monsterField.get(0);
            if (first.isString()) {
                return first.asString();
            }
            if (first.isArray() && first.size > 0) {
                final JsonValue idNode = first.get(0);
                if (idNode != null && idNode.isString()) {
                    return idNode.asString();
                }
            }
        }
        options.addWarning("skipped place_monster entry: unsupported monster field");
        return null;
    }

    private static void summarizeSkippedSpawns(final List<SpawnMarker> markers, final JsonMapgenRunOptions options) {
        if (markers.isEmpty() || options.getGameData() != null) {
            return;
        }
        long itemCount = markers.stream().filter(m -> m.kind == SpawnMarker.Kind.ITEM_GROUP).count();
        long monsterCount = markers.stream().filter(m -> m.kind == SpawnMarker.Kind.MONSTER_GROUP).count();
        if (itemCount > 0) {
            options.addWarning(itemCount + " item spawns not shown in preview");
        }
        if (monsterCount > 0) {
            options.addWarning(monsterCount + " monster spawns not shown in preview");
        }
    }

    private static String readPlacementId(
        final JsonValue entry,
        final PlacementKind kind,
        final JsonMapgenRunOptions options
    ) {
        if (kind == PlacementKind.TERRAIN) {
            String id = entry.getString("ter", null);
            if (id == null || id.isEmpty()) {
                id = entry.getString("terrain", null);
            }
            if (id == null || id.isEmpty()) {
                options.addWarning("skipped place_terrain entry: missing ter/terrain");
            }
            return id;
        }
        String id = entry.getString("furn", null);
        if (id == null || id.isEmpty()) {
            id = entry.getString("furniture", null);
        }
        if (id == null || id.isEmpty()) {
            options.addWarning("skipped place_furniture entry: missing furn/furniture");
        }
        return id;
    }

    private static String readItemGroupId(final JsonValue entry, final JsonMapgenRunOptions options) {
        String groupId = entry.getString("item", null);
        if (groupId == null || groupId.isEmpty()) {
            groupId = entry.getString("items", null);
        }
        if (groupId == null || groupId.isEmpty()) {
            options.addWarning("skipped place_items entry: missing item/items");
        }
        return groupId;
    }

    static String resolveItemGroupDisplayName(
        final String groupId,
        final JsonMapgenRunOptions options
    ) {
        final LoadedGameData gameData = options.getGameData();
        if (gameData == null) {
            return groupId;
        }
        final java.util.Optional<ItemGroupDefinition> def = gameData.getItemGroups().find(groupId);
        if (!def.isPresent()) {
            options.addWarning("unknown item_group '" + groupId + "' in place_items");
            return groupId;
        }
        return def.get().getDisplayName();
    }

    static String resolveMonsterDisplayName(
        final String monsterId,
        final JsonMapgenRunOptions options
    ) {
        if ("mon_null".equals(monsterId)) {
            return null;
        }
        final LoadedGameData gameData = options.getGameData();
        if (gameData == null) {
            return monsterId;
        }
        final java.util.Optional<MonsterGroupDefinition> def = gameData.getMonsterGroups().find(monsterId);
        if (def.isPresent()) {
            final String display = def.get().getDisplayName();
            if ("mon_null".equals(display)) {
                return null;
            }
            return display;
        }
        if (monsterId.startsWith("mon_") || monsterId.startsWith("GROUP_")) {
            return monsterId;
        }
        options.addWarning("unknown monster '" + monsterId + "' in place_monster(s)");
        return null;
    }

    private static Point rollPoint(final JsonValue entry, final Random rng) {
        if (entry.has("x2") || entry.has("y2")) {
            final Bounds bounds = rectangleBounds(entry);
            return rollPointInBounds(bounds, rng);
        }
        return new Point(
            JmapgenIntRange.rollOptional(entry, "x", 0, rng),
            JmapgenIntRange.rollOptional(entry, "y", 0, rng)
        );
    }

    private static Bounds entryBounds(final JsonValue entry, final int gridWidth, final int gridHeight) {
        if (entry.has("x2") || entry.has("y2")) {
            final Bounds rect = rectangleBounds(entry);
            return new Bounds(rect.minX, rect.maxX, rect.minY, rect.maxY, gridWidth, gridHeight);
        }
        final IntRange xRange = readFieldRange(entry, "x", 0);
        final IntRange yRange = readFieldRange(entry, "y", 0);
        return new Bounds(xRange.min, xRange.max, yRange.min, yRange.max, gridWidth, gridHeight);
    }

    private static Bounds rectangleBounds(final JsonValue entry) {
        final IntRange xRange = readFieldRange(entry, "x", 0);
        final IntRange x2Range = readFieldRange(entry, "x2", xRange.max);
        final IntRange yRange = readFieldRange(entry, "y", 0);
        final IntRange y2Range = readFieldRange(entry, "y2", yRange.max);
        return new Bounds(
            Math.min(xRange.min, x2Range.min),
            Math.max(xRange.max, x2Range.max),
            Math.min(yRange.min, y2Range.min),
            Math.max(yRange.max, y2Range.max),
            Integer.MAX_VALUE,
            Integer.MAX_VALUE
        );
    }

    private static IntRange readFieldRange(final JsonValue entry, final String key, final int defaultValue) {
        if (entry == null || !entry.has(key)) {
            return new IntRange(defaultValue, defaultValue);
        }
        final JsonValue field = entry.get(key);
        if (field.isNumber()) {
            final int value = field.asInt();
            return new IntRange(value, value);
        }
        if (field.isArray() && field.size > 0) {
            int min = field.getInt(0);
            int max = field.size >= 2 ? field.getInt(1) : min;
            if (min > max) {
                final int swap = min;
                min = max;
                max = swap;
            }
            return new IntRange(min, max);
        }
        return new IntRange(defaultValue, defaultValue);
    }

    private static Point rollPointInBounds(final Bounds bounds, final Random rng) {
        final int x = bounds.minX == bounds.maxX
            ? bounds.minX
            : bounds.minX + rng.nextInt(bounds.maxX - bounds.minX + 1);
        final int y = bounds.minY == bounds.maxY
            ? bounds.minY
            : bounds.minY + rng.nextInt(bounds.maxY - bounds.minY + 1);
        return new Point(x, y);
    }

    private static int rollRemainder(final float value, final Random rng) {
        final int whole = (int) value;
        final float fraction = value - whole;
        return whole + (rng.nextFloat() < fraction ? 1 : 0);
    }

    private static boolean inBounds(final MapGrid grid, final int x, final int y) {
        return x >= 0 && y >= 0 && x < grid.width() && y < grid.height();
    }

    private enum PlacementKind {
        TERRAIN("terrain"),
        FURNITURE("furniture");

        private final String jsonKey;

        PlacementKind(final String jsonKey) {
            this.jsonKey = jsonKey;
        }
    }

    private static final class Point {
        private final int x;
        private final int y;

        private Point(final int x, final int y) {
            this.x = x;
            this.y = y;
        }
    }

    private static final class IntRange {
        private final int min;
        private final int max;

        private IntRange(final int min, final int max) {
            this.min = min;
            this.max = max;
        }
    }

    private static final class Bounds {
        private final int minX;
        private final int maxX;
        private final int minY;
        private final int maxY;
        private final int gridWidth;
        private final int gridHeight;

        private Bounds(
            final int minX,
            final int maxX,
            final int minY,
            final int maxY,
            final int gridWidth,
            final int gridHeight
        ) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.gridWidth = gridWidth;
            this.gridHeight = gridHeight;
        }

        private boolean inGrid() {
            if (gridWidth <= 0 || gridHeight <= 0) {
                return true;
            }
            if (minX < 0 || minY < 0) {
                return false;
            }
            return maxX < gridWidth && maxY < gridHeight;
        }
    }
}
