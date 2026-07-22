package io.gdx.cdda.bn.nextgen.mapgen.building;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Registers whole static {@code overmap_special} layouts as preview bundles (P7c). */
public final class SpecialLayoutImporter {

    private SpecialLayoutImporter() {}

    public static void registerIfMultiColumn(
        final String specialId,
        final List<CityBuildingPiece> pieces,
        final Path sourceFile,
        final Map<String, CityBuildingDefinition> byId
    ) {
        registerIfMultiColumn(specialId, pieces, sourceFile, byId, List.of());
    }

    public static void registerIfMultiColumn(
        final String specialId,
        final List<CityBuildingPiece> pieces,
        final Path sourceFile,
        final Map<String, CityBuildingDefinition> byId,
        final List<OvermapSpecialConnection> connections
    ) {
        if (specialId.isEmpty() || countDistinctOmtColumns(pieces) < 2) {
            return;
        }
        if (byId.containsKey(specialId)) {
            return;
        }
        final List<CityBuildingPiece> sorted = new ArrayList<>(pieces);
        sorted.sort(Comparator
            .comparingInt(CityBuildingPiece::getZLevel)
            .thenComparingInt(CityBuildingPiece::getOffsetY)
            .thenComparingInt(CityBuildingPiece::getOffsetX)
            .thenComparing(CityBuildingPiece::getOvermapId));
        byId.put(
            specialId,
            new CityBuildingDefinition(
                specialId,
                sourceFile,
                sorted,
                CityBuildingDefinition.LayoutKind.OVERMAP_SPECIAL_WHOLE,
                connections
            )
        );
    }

    private static int countDistinctOmtColumns(final List<CityBuildingPiece> pieces) {
        final TreeSet<String> columns = new TreeSet<>();
        for (final CityBuildingPiece piece : pieces) {
            columns.add(piece.getOffsetX() + "," + piece.getOffsetY());
        }
        return columns.size();
    }
}
