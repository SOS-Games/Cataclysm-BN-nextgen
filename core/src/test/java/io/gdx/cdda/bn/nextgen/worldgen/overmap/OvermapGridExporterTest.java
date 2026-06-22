package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OvermapGridExporterTest {

    @Test
    void roundTripsFixtureShape() throws Exception {
        final OvermapGrid original = OvermapGridFactory.fromJsonFile(
            io.gdx.cdda.bn.nextgen.worldgen.WorldgenTestFixtures.overmapFixturePath()
        );
        final String json = OvermapGridExporter.toJson(original, 42L, "default", null);
        final OvermapGrid restored = OvermapGridFactory.fromJson(json);

        assertEquals(original.width(), restored.width());
        assertEquals(original.height(), restored.height());
        for (int y = 0; y < original.height(); y++) {
            for (int x = 0; x < original.width(); x++) {
                assertEquals(original.getOmtId(x, y), restored.getOmtId(x, y), "cell " + x + "," + y);
            }
        }
    }
}
