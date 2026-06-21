package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.ItemGroupDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.ItemGroupRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.map.MapGrid;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceSpawnerApplierTest {

    @Test
    void singleFurniturePlacement() {
        final MapGrid grid = new MapGrid(5, 5, "t_floor");
        final JsonValue object = new JsonReader().parse(
            "{"
                + "\"place_furniture\":[{\"furn\":\"f_chair\",\"x\":2,\"y\":2}]"
                + "}"
        );

        PlaceSpawnerApplier.applyTerrainAndFurniture(
            grid,
            object,
            new JsonMapgenRunOptions(),
            new Random(1L)
        );

        assertEquals("f_chair", grid.get(2, 2).getFurnitureId());
        assertEquals("t_floor", grid.get(2, 2).getTerrainId());
    }

    @Test
    void repeatScattersFurnitureWithStableSeed() {
        final JsonValue object = new JsonReader().parse(
            "{"
                + "\"place_furniture\":[{"
                + "\"furn\":\"f_chair\","
                + "\"x\":[0,4],"
                + "\"y\":[0,4],"
                + "\"repeat\":[3,3]"
                + "}]"
                + "}"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(99L);
        final Random rng = new Random(99L);

        final MapGrid grid = new MapGrid(5, 5, "t_floor");
        PlaceSpawnerApplier.applyTerrainAndFurniture(grid, object, options, rng);

        assertEquals(3, countFurniture(grid, "f_chair"));

        final MapGrid again = new MapGrid(5, 5, "t_floor");
        PlaceSpawnerApplier.applyTerrainAndFurniture(again, object, options, new Random(99L));
        assertEquals(3, countFurniture(again, "f_chair"));
    }

    @Test
    void placeTerrainOverwritesCell() {
        final MapGrid grid = new MapGrid(3, 3, "t_floor");
        final JsonValue object = new JsonReader().parse(
            "{\"place_terrain\":[{\"ter\":\"t_dirt\",\"x\":1,\"y\":1}]}"
        );

        PlaceSpawnerApplier.applyTerrainAndFurniture(
            grid,
            object,
            new JsonMapgenRunOptions(),
            new Random(1L)
        );

        assertEquals("t_dirt", grid.get(1, 1).getTerrainId());
    }

    @Test
    void monsterSpawnsCollectMarkersAndWarn() {
        final MapGrid grid = new MapGrid(5, 5, "t_floor");
        final JsonValue object = new JsonReader().parse(
            "{"
                + "\"place_monsters\":[{"
                + "\"monster\":\"GROUP_TEST\","
                + "\"x\":[0,4],"
                + "\"y\":[0,4],"
                + "\"density\":1.0"
                + "}]"
                + "}"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withPreviewSeed(7L);

        final List<SpawnMarker> markers = PlaceSpawnerApplier.collectEntitySpawns(
            object,
            grid,
            options,
            new Random(7L)
        );

        assertTrue(markers.size() > 0);
        assertEquals(SpawnMarker.Kind.MONSTER_GROUP, markers.get(0).kind);
        assertEquals("GROUP_TEST", markers.get(0).groupId);
        assertTrue(options.getWarnings().stream().anyMatch(w -> w.contains("monster spawns not shown")));
    }

    @Test
    void higherMonsterDensityProducesMoreMarkersAtFixedSeed() {
        final JsonValue lowDensity = new JsonReader().parse(
            "{\"place_monsters\":[{\"monster\":\"GROUP_TEST\",\"x\":[0,4],\"y\":[0,4],\"density\":0.1}]}"
        );
        final JsonValue highDensity = new JsonReader().parse(
            "{\"place_monsters\":[{\"monster\":\"GROUP_TEST\",\"x\":[0,4],\"y\":[0,4],\"density\":2.0}]}"
        );
        final MapGrid grid = new MapGrid(5, 5, "t_floor");

        final int lowCount = PlaceSpawnerApplier.collectEntitySpawns(
            lowDensity,
            grid,
            new JsonMapgenRunOptions().withPreviewSeed(11L),
            new Random(11L)
        ).size();
        final int highCount = PlaceSpawnerApplier.collectEntitySpawns(
            highDensity,
            grid,
            new JsonMapgenRunOptions().withPreviewSeed(11L),
            new Random(11L)
        ).size();

        assertTrue(highCount > lowCount);
    }

    @Test
    void unknownMonsterGroupStillShowsMarkerWhenGameDataLoaded() {
        final MapGrid grid = new MapGrid(5, 5, "t_floor");
        final JsonValue object = new JsonReader().parse(
            "{\"place_monsters\":[{\"monster\":\"GROUP_MISSING\",\"x\":2,\"y\":2,\"density\":1.0}]}"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withGameData(emptyGameData());

        final List<SpawnMarker> markers = PlaceSpawnerApplier.collectEntitySpawns(
            object,
            grid,
            options,
            new Random(3L)
        );

        assertTrue(markers.size() > 0);
        assertEquals("GROUP_MISSING", markers.get(0).label());
    }

    @Test
    void placeMonsterCollectsSingleMonsterId() {
        final MapGrid grid = new MapGrid(48, 3, "t_floor");
        final JsonValue object = new JsonReader().parse(
            "{\"place_monster\":[{\"monster\":\"mon_skitterbot\",\"x\":3,\"y\":0,\"chance\":100}]}"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withPreviewSeed(1L)
            .withGameData(emptyGameData());

        final List<SpawnMarker> markers = PlaceSpawnerApplier.collectEntitySpawns(
            object,
            grid,
            options,
            new Random(1L)
        );

        assertEquals(1, markers.size());
        assertEquals("mon_skitterbot", markers.get(0).groupId);
        assertEquals(3, markers.get(0).x);
        assertEquals(0, markers.get(0).y);
    }

    @Test
    void singleMonsterIdAcceptedWithGameData() {
        final MapGrid grid = new MapGrid(5, 5, "t_floor");
        final JsonValue object = new JsonReader().parse(
            "{\"place_monster\":[{\"monster\":\"mon_skitterbot\",\"x\":2,\"y\":2,\"chance\":100}]}"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withGameData(emptyGameData());

        final List<SpawnMarker> markers = PlaceSpawnerApplier.collectEntitySpawns(
            object,
            grid,
            options,
            new Random(3L)
        );

        assertEquals(1, markers.size());
        assertEquals("mon_skitterbot", markers.get(0).label());
    }

    @Test
    void knownMonsterGroupResolvesDisplayNameAndSkipsLegacyWarning() {
        final MapGrid grid = new MapGrid(5, 5, "t_floor");
        final JsonValue object = new JsonReader().parse(
            "{\"place_monsters\":[{\"monster\":\"GROUP_TEST\",\"x\":2,\"y\":2,\"density\":2.0}]}"
        );
        final MonsterGroupRegistry monsterGroups = new MonsterGroupRegistry();
        monsterGroups.put(new MonsterGroupDefinition("GROUP_TEST", "mon_test", "test"));
        final LoadedGameData gameData = new LoadedGameData(
            new TerrainRegistry(),
            new FurnitureRegistry(),
            new ItemGroupRegistry(),
            monsterGroups,
            Collections.singletonList("test")
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions().withGameData(gameData);

        final List<SpawnMarker> markers = PlaceSpawnerApplier.collectEntitySpawns(
            object,
            grid,
            options,
            new Random(5L)
        );

        assertTrue(markers.size() > 0);
        assertEquals("mon_test", markers.get(0).label());
        assertTrue(options.getWarnings().stream().noneMatch(w -> w.contains("not shown")));
    }

    private static LoadedGameData emptyGameData() {
        return new LoadedGameData(
            new TerrainRegistry(),
            new FurnitureRegistry(),
            new ItemGroupRegistry(),
            new MonsterGroupRegistry(),
            Collections.emptyList()
        );
    }

    private static int countFurniture(final MapGrid grid, final String furnitureId) {
        int count = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                if (furnitureId.equals(grid.get(x, y).getFurnitureId())) {
                    count++;
                }
            }
        }
        return count;
    }
}
