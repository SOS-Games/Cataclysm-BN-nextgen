package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.mapgen.palette.MergedFormatPlacings;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormatPlacingCollectorTest {

    @Test
    void collectsItemMarkersFromRowsAndInlineItems() {
        final JsonValue object = new JsonReader().parse(
            "{"
                + "\"palettes\":[],"
                + "\"rows\":[\"V..\"],"
                + "\"items\":{\"V\":{\"item\":\"consumer_electronics\",\"chance\":100,\"repeat\":1}}"
                + "}"
        );
        final MergedFormatPlacings placings = MergedFormatPlacings.merge(
            new PaletteRegistry(),
            List.of(),
            object,
            List.of()
        );

        final List<SpawnMarker> markers = FormatPlacingCollector.collectFromRows(
            List.of("V.."),
            placings,
            new JsonMapgenRunOptions().withPreviewSeed(3L),
            new Random(3L)
        );

        assertEquals(1, markers.size());
        assertEquals(SpawnMarker.Kind.ITEM_GROUP, markers.get(0).kind);
        assertEquals("consumer_electronics", markers.get(0).groupId);
        assertEquals(0, markers.get(0).x);
        assertEquals(0, markers.get(0).y);
    }

    @Test
    void collectsPlaceLootFromEntitySpawns() {
        final JsonValue object = new JsonReader().parse(
            "{\"place_loot\":[{\"item\":\"television\",\"x\":2,\"y\":1,\"chance\":100}]}"
        );

        final List<SpawnMarker> markers = PlaceSpawnerApplier.collectEntitySpawns(
            object,
            null,
            new JsonMapgenRunOptions(),
            new Random(1L)
        );

        assertEquals(1, markers.size());
        assertEquals("television", markers.get(0).groupId);
        assertEquals(2, markers.get(0).x);
        assertEquals(1, markers.get(0).y);
    }

    @Test
    void skipsMonNullMonsterGroups() {
        final JsonValue object = new JsonReader().parse(
            "{\"place_monsters\":[{\"monster\":\"GROUP_EMPTY\",\"x\":1,\"y\":1,\"density\":1.0}]}"
        );
        final JsonMapgenRunOptions options = new JsonMapgenRunOptions()
            .withGameData(PlaceSpawnerApplierTestHelper.gameDataWithMonNullGroup());

        final List<SpawnMarker> markers = PlaceSpawnerApplier.collectEntitySpawns(
            object,
            null,
            options,
            new Random(2L)
        );

        assertTrue(markers.isEmpty());
    }
}
