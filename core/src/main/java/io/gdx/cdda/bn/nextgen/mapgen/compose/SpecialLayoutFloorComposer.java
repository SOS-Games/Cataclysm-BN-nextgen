package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.OmTerrainGrid;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Composes per-floor grids for a whole static {@code overmap_special} (P7c). */
public final class SpecialLayoutFloorComposer {

    private SpecialLayoutFloorComposer() {}

    public static MapVolumeBuilder.MapVolumeBuildResult build(
        final CityBuildingDefinition building,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions runOptions
    ) {
        if (building == null || !building.isWholeOvermapSpecial()) {
            throw new IllegalArgumentException("building must be a whole overmap_special layout");
        }
        if (catalog == null || palettes == null) {
            throw new IllegalStateException("catalog and palettes are required");
        }

        final JsonMapgenRunOptions options = runOptions == null ? new JsonMapgenRunOptions() : runOptions;
        final List<String> warnings = new ArrayList<>(options.getWarnings());
        final List<Integer> zLevels = new ArrayList<>(building.distinctZLevels());
        zLevels.sort(Integer::compareTo);
        if (zLevels.isEmpty()) {
            throw new IllegalArgumentException(
                "building '" + building.getId() + "' produced no runnable floors"
            );
        }

        final int referenceZ = pickDefaultZ(zLevels);
        final List<CityBuildingPiece> groundPieces = building.piecesAtZ(referenceZ);
        final Optional<CombinedGroundMatch> combinedGround = resolveCombinedGroundMapgen(groundPieces, catalog);
        if (!combinedGround.isPresent()) {
            warnings.add("whole special '" + building.getId()
                + "' has no combined ground mapgen; falling back to per-piece stitch at z=" + referenceZ);
            return MapVolumeBuilder.build(
                toStandardLayout(building),
                catalog,
                palettes,
                options
            );
        }

        final JsonMapgenDefinition groundDefinition = combinedGround.get().definition;
        final OmTerrainGrid referenceOmGrid = combinedGround.get().grid;
        final MapGrid referenceGrid = JsonMapgenRunner.run(groundDefinition, palettes, options);
        final Map<String, OmTerrainMapgenPlacer.GridCell> groundAnchors =
            buildGroundAnchors(groundPieces, referenceOmGrid, warnings);

        final Map<Integer, MapGrid> gridsByZ = new LinkedHashMap<>();
        final Map<Integer, List<OmtPieceRect>> pieceLayoutsByZ = new LinkedHashMap<>();
        gridsByZ.put(referenceZ, referenceGrid);
        pieceLayoutsByZ.put(referenceZ, layoutGroundPieceRects(groundPieces, referenceOmGrid, referenceGrid));

        for (final int zLevel : zLevels) {
            if (zLevel == referenceZ) {
                continue;
            }
            final List<CityBuildingPiece> pieces = building.piecesAtZ(zLevel);
            if (pieces.isEmpty()) {
                continue;
            }
            final FloorComposeResult floor = composeUpperFloor(
                pieces,
                groundAnchors,
                referenceOmGrid,
                referenceGrid,
                catalog,
                palettes,
                options,
                warnings,
                zLevel
            );
            if (floor.grid != null) {
                gridsByZ.put(zLevel, floor.grid);
                if (!floor.pieceLayouts.isEmpty()) {
                    pieceLayoutsByZ.put(zLevel, floor.pieceLayouts);
                }
            }
        }

        if (gridsByZ.isEmpty()) {
            throw new IllegalArgumentException(
                "building '" + building.getId() + "' produced no runnable floors"
            );
        }

        final List<Integer> builtZLevels = MapVolumeBuilder.builtZLevelsFrom(zLevels, gridsByZ);
        final int activeZ = MapVolumeBuilder.pickReferenceZ(builtZLevels, referenceZ);

        return new MapVolumeBuilder.MapVolumeBuildResult(
            new MapVolume(building.getId(), builtZLevels, gridsByZ, pieceLayoutsByZ, activeZ),
            warnings
        );
    }

    private static FloorComposeResult composeUpperFloor(
        final List<CityBuildingPiece> pieces,
        final Map<String, OmTerrainMapgenPlacer.GridCell> groundAnchors,
        final OmTerrainGrid referenceOmGrid,
        final MapGrid referenceGrid,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final List<String> warnings,
        final int zLevel
    ) {
        final List<CityBuildingPiece> sorted = new ArrayList<>(pieces);
        sorted.sort(Comparator
            .comparingInt(CityBuildingPiece::getOffsetY)
            .thenComparingInt(CityBuildingPiece::getOffsetX)
            .thenComparing(CityBuildingPiece::getOvermapId));

        final String fillTer = referenceGrid.getDefaultTerrainId();
        final MapGrid canvas = new MapGrid(referenceGrid.width(), referenceGrid.height(), fillTer);
        final List<OmtPieceRect> pieceLayouts = new ArrayList<>();

        for (final CityBuildingPiece piece : sorted) {
            final Optional<JsonMapgenDefinition> definition = OvermapTerrainResolver.resolveMapgen(
                catalog,
                piece.getOvermapId()
            );
            if (!definition.isPresent()) {
                warnings.add("no runnable mapgen for overmap '" + piece.getOvermapId() + "' at z=" + zLevel);
                continue;
            }
            if (!definition.get().isJsonPreviewSupported()) {
                warnings.add("unsupported mapgen for overmap '" + piece.getOvermapId() + "' at z=" + zLevel);
                continue;
            }

            final OmTerrainMapgenPlacer.GridCell anchor = groundAnchors.get(omtColumnKey(piece));
            if (anchor == null) {
                warnings.add("no ground anchor for overmap '" + piece.getOvermapId() + "' at z=" + zLevel);
                continue;
            }

            final MapGrid source = JsonMapgenRunner.run(definition.get(), palettes, options);
            final Optional<OmTerrainGrid> sourceOmGrid = definition.get().getOmTerrainGrid();
            if (!sourceOmGrid.isPresent()) {
                warnings.add("mapgen for '" + piece.getOvermapId() + "' has no om_terrain grid at z=" + zLevel);
                continue;
            }

            final Optional<OmTerrainMapgenPlacer.PlacementRect> placement = OmTerrainMapgenPlacer.blitAtReferenceCell(
                canvas,
                source,
                sourceOmGrid.get(),
                piece.getOvermapId(),
                referenceOmGrid,
                anchor.col,
                anchor.row,
                fillTer
            );
            if (!placement.isPresent()) {
                warnings.add("could not place overmap '" + piece.getOvermapId() + "' at z=" + zLevel);
                continue;
            }
            pieceLayouts.add(new OmtPieceRect(
                placement.get().destX,
                placement.get().destY,
                placement.get().width,
                placement.get().height,
                piece.getOvermapId()
            ));
        }

        return new FloorComposeResult(canvas, pieceLayouts);
    }

    private static List<OmtPieceRect> layoutGroundPieceRects(
        final List<CityBuildingPiece> groundPieces,
        final OmTerrainGrid referenceOmGrid,
        final MapGrid referenceGrid
    ) {
        final int omtCellW = referenceGrid.width() / referenceOmGrid.width();
        final int omtCellH = referenceGrid.height() / referenceOmGrid.height();
        final List<OmtPieceRect> rects = new ArrayList<>();
        for (final CityBuildingPiece piece : groundPieces) {
            final Optional<OmTerrainMapgenPlacer.GridCell> cell = OmTerrainMapgenPlacer.findCell(
                referenceOmGrid,
                piece.getOvermapId()
            );
            if (!cell.isPresent()) {
                continue;
            }
            rects.add(new OmtPieceRect(
                cell.get().col * omtCellW,
                cell.get().row * omtCellH,
                omtCellW,
                omtCellH,
                piece.getOvermapId()
            ));
        }
        return rects;
    }

    private static Map<String, OmTerrainMapgenPlacer.GridCell> buildGroundAnchors(
        final List<CityBuildingPiece> groundPieces,
        final OmTerrainGrid referenceOmGrid,
        final List<String> warnings
    ) {
        final Map<String, OmTerrainMapgenPlacer.GridCell> anchors = new HashMap<>();
        for (final CityBuildingPiece piece : groundPieces) {
            final Optional<OmTerrainMapgenPlacer.GridCell> cell = OmTerrainMapgenPlacer.findCell(
                referenceOmGrid,
                piece.getOvermapId()
            );
            if (!cell.isPresent()) {
                warnings.add("ground piece '" + piece.getOvermapId() + "' not found in reference om_terrain grid");
                continue;
            }
            anchors.put(omtColumnKey(piece), cell.get());
        }
        return anchors;
    }

    private static Optional<CombinedGroundMatch> resolveCombinedGroundMapgen(
        final List<CityBuildingPiece> groundPieces,
        final MapgenCatalog catalog
    ) {
        if (groundPieces == null || groundPieces.isEmpty()) {
            return Optional.empty();
        }

        CombinedGroundMatch best = null;
        int bestArea = -1;
        for (final JsonMapgenDefinition definition : catalog.runnableOnly()) {
            final Optional<OmTerrainGrid> grid = definition.getOmTerrainGrid();
            if (!grid.isPresent() || !gridCoversAllPieces(groundPieces, grid.get())) {
                continue;
            }
            final int area = grid.get().width() * grid.get().height();
            if (area > bestArea) {
                bestArea = area;
                best = new CombinedGroundMatch(definition, grid.get());
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean gridCoversAllPieces(
        final List<CityBuildingPiece> pieces,
        final OmTerrainGrid grid
    ) {
        for (final CityBuildingPiece piece : pieces) {
            if (!OmTerrainMapgenPlacer.findCell(grid, piece.getOvermapId()).isPresent()) {
                return false;
            }
        }
        return true;
    }

    private static CityBuildingDefinition toStandardLayout(final CityBuildingDefinition building) {
        return new CityBuildingDefinition(building.getId(), building.getSourceFile(), building.getPieces());
    }

    private static String omtColumnKey(final CityBuildingPiece piece) {
        return piece.getOffsetX() + "," + piece.getOffsetY();
    }

    private static int pickDefaultZ(final List<Integer> zLevels) {
        if (zLevels.contains(0)) {
            return 0;
        }
        return zLevels.get(0);
    }

    private static final class CombinedGroundMatch {
        private final JsonMapgenDefinition definition;
        private final OmTerrainGrid grid;

        private CombinedGroundMatch(final JsonMapgenDefinition definition, final OmTerrainGrid grid) {
            this.definition = definition;
            this.grid = grid;
        }
    }

    private static final class FloorComposeResult {
        private final MapGrid grid;
        private final List<OmtPieceRect> pieceLayouts;

        private FloorComposeResult(final MapGrid grid, final List<OmtPieceRect> pieceLayouts) {
            this.grid = grid;
            this.pieceLayouts = pieceLayouts;
        }
    }
}
