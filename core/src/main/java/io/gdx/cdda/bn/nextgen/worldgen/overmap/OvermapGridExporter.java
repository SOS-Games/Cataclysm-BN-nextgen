package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateResult;

import java.util.LinkedHashMap;
import java.util.Map;

/** Serializes {@link OvermapGrid} to the fixture JSON shape (W14 debug). */
public final class OvermapGridExporter {

    private OvermapGridExporter() {}

    public static String toJson(
        final OvermapGrid grid,
        final long seed,
        final String regionId,
        final OvermapGenerateResult stats
    ) {
        if (grid == null) {
            throw new IllegalArgumentException("grid is required");
        }
        final String fill = dominantFillId(grid);
        final StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"width\": ").append(grid.width()).append(",\n");
        out.append("  \"height\": ").append(grid.height()).append(",\n");
        out.append("  \"fill\": \"").append(escapeJson(fill)).append("\",\n");
        out.append("  \"seed\": ").append(seed).append(",\n");
        if (regionId != null && !regionId.isEmpty()) {
            out.append("  \"regionId\": \"").append(escapeJson(regionId)).append("\",\n");
        }
        if (stats != null) {
            out.append("  \"stats\": {\n");
            out.append("    \"cityBuildings\": ").append(stats.getCityBuildingsPlaced()).append(",\n");
            out.append("    \"staticSpecials\": ").append(stats.getStaticSpecialsPlaced()).append(",\n");
            out.append("    \"mutableSpecials\": ").append(stats.getMutableSpecialsPlaced()).append(",\n");
            out.append("    \"riverCells\": ").append(stats.getRiverCellsCarved()).append(",\n");
            out.append("    \"roadCells\": ").append(stats.getRoadCellsPlaced()).append("\n");
            out.append("  },\n");
        }
        out.append("  \"rows\": [\n");
        for (int y = 0; y < grid.height(); y++) {
            out.append("    [");
            for (int x = 0; x < grid.width(); x++) {
                if (x > 0) {
                    out.append(", ");
                }
                out.append('"').append(escapeJson(grid.getOmtId(x, y))).append('"');
            }
            out.append("]");
            out.append(y < grid.height() - 1 ? ",\n" : "\n");
        }
        out.append("  ]\n");
        out.append("}\n");
        return out.toString();
    }

    private static String dominantFillId(final OvermapGrid grid) {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String id = grid.getOmtId(x, y);
                counts.put(id, counts.getOrDefault(id, 0) + 1);
            }
        }
        String best = "field";
        int bestCount = -1;
        for (final Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static String escapeJson(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
