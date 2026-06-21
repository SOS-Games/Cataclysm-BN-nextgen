package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Blits multiple OMT mapgen pieces into one {@link MapGrid} per floor (P6). */
public final class OmtStitchComposer {

    public static final int DEFAULT_OMT_SIZE = 24;
    private static final int MAX_OMT_WIDTH = 8;
    private static final int MAX_OMT_HEIGHT = 8;
    private static final int MAX_CANVAS_WIDTH = 256;
    private static final int MAX_CANVAS_HEIGHT = 256;

    private OmtStitchComposer() {}

    public static boolean needsStitch(final List<CityBuildingPiece> pieces) {
        if (pieces == null || pieces.isEmpty()) {
            return false;
        }
        if (pieces.size() > 1) {
            return true;
        }
        final CityBuildingPiece piece = pieces.get(0);
        return piece.getOffsetX() != 0 || piece.getOffsetY() != 0;
    }

    public static StitchResult stitch(
        final List<CityBuildingPiece> pieces,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions runOptions
    ) {
        if (pieces == null || pieces.isEmpty()) {
            throw new IllegalArgumentException("pieces must not be empty");
        }
        if (catalog == null || palettes == null) {
            throw new IllegalStateException("catalog and palettes are required");
        }

        final JsonMapgenRunOptions options = runOptions == null ? new JsonMapgenRunOptions() : runOptions;
        final List<String> warnings = new ArrayList<>(options.getWarnings());
        final List<CityBuildingPiece> sorted = new ArrayList<>(pieces);
        sorted.sort(Comparator
            .comparingInt(CityBuildingPiece::getOffsetY)
            .thenComparingInt(CityBuildingPiece::getOffsetX)
            .thenComparing(CityBuildingPiece::getOvermapId));

        validateExtents(sorted, warnings);

        final List<ResolvedPiece> resolvedPieces = new ArrayList<>();
        final List<OmtPieceRect> pieceRects = new ArrayList<>();
        for (final CityBuildingPiece piece : sorted) {
            final Optional<JsonMapgenDefinition> definition = OvermapTerrainResolver.resolveMapgen(
                catalog,
                piece.getOvermapId()
            );
            if (!definition.isPresent()) {
                warnings.add("no runnable mapgen for overmap '" + piece.getOvermapId() + "'");
                continue;
            }
            if (!definition.get().isJsonPreviewSupported()) {
                warnings.add("unsupported mapgen for overmap '" + piece.getOvermapId() + "'");
                continue;
            }
            final boolean multitileCrop = definition.get().getOmTerrainGrid().isPresent();
            final JsonMapgenRunOptions pieceOptions = options.deriveWithOmtRotation(
                MapGridRotator.runnerOmtRotation(multitileCrop, piece.getOvermapId())
            );
            final MapGrid grid = JsonMapgenRunner.run(definition.get(), catalog, palettes, pieceOptions);
            warnings.addAll(pieceOptions.getWarnings());
            resolvedPieces.add(new ResolvedPiece(piece, definition.get(), grid, pieceOptions));
        }

        if (resolvedPieces.isEmpty()) {
            return StitchResult.empty(warnings);
        }

        final Bounds bounds = computeBounds(resolvedPieces, DEFAULT_OMT_SIZE);
        if (bounds.width > MAX_CANVAS_WIDTH || bounds.height > MAX_CANVAS_HEIGHT) {
            warnings.add("stitched canvas exceeds max size: " + bounds.width + "x" + bounds.height);
            return StitchResult.empty(warnings);
        }

        final String fillTer = resolveFillTer(resolvedPieces.get(0).definition, options);
        final MapGrid canvas = new MapGrid(bounds.width, bounds.height, fillTer);
        final List<SpawnMarker> floorMarkers = new ArrayList<>();

        for (final ResolvedPiece resolved : resolvedPieces) {
            final int destX = resolved.piece.getOffsetX() * DEFAULT_OMT_SIZE - bounds.originX;
            final int destY = resolved.piece.getOffsetY() * DEFAULT_OMT_SIZE - bounds.originY;
            final Optional<MapGrid> oriented = OmTerrainMapgenPlacer.extractOrientedOmtSubmap(
                resolved.grid,
                resolved.definition.getOmTerrainGrid().orElse(null),
                resolved.piece.getOvermapId()
            );
            if (!oriented.isPresent()) {
                warnings.add("could not crop mapgen for overmap '" + resolved.piece.getOvermapId() + "'");
                continue;
            }
            final MapGrid pieceGrid = oriented.get();
            final int overlaps = canvas.blitFrom(pieceGrid, destX, destY, fillTer);
            if (overlaps > 0) {
                warnings.add("stitch overlap at " + resolved.piece.getOvermapId() + ": " + overlaps + " cells");
            }
            pieceRects.add(new OmtPieceRect(
                destX,
                destY,
                pieceGrid.width(),
                pieceGrid.height(),
                resolved.piece.getOvermapId()
            ));
            floorMarkers.addAll(
                SpawnMarkerTransform.orientForStitchPiece(
                    resolved.pieceOptions.getSpawnMarkers(),
                    resolved.grid,
                    resolved.definition.getOmTerrainGrid().orElse(null),
                    resolved.piece.getOvermapId(),
                    destX,
                    destY
                )
            );
        }

        options.addSpawnMarkers(floorMarkers);

        return new StitchResult(canvas, pieceRects, warnings);
    }

    public static List<OmtPieceRect> layoutPieceRects(
        final List<CityBuildingPiece> pieces,
        final int stride
    ) {
        if (pieces == null || pieces.isEmpty()) {
            return Collections.emptyList();
        }
        final List<CityBuildingPiece> sorted = new ArrayList<>(pieces);
        sorted.sort(Comparator
            .comparingInt(CityBuildingPiece::getOffsetY)
            .thenComparingInt(CityBuildingPiece::getOffsetX)
            .thenComparing(CityBuildingPiece::getOvermapId));

        int minOriginX = Integer.MAX_VALUE;
        int minOriginY = Integer.MAX_VALUE;
        for (final CityBuildingPiece piece : sorted) {
            minOriginX = Math.min(minOriginX, piece.getOffsetX() * stride);
            minOriginY = Math.min(minOriginY, piece.getOffsetY() * stride);
        }
        if (minOriginX == Integer.MAX_VALUE) {
            minOriginX = 0;
        }
        if (minOriginY == Integer.MAX_VALUE) {
            minOriginY = 0;
        }

        final List<OmtPieceRect> rects = new ArrayList<>();
        for (final CityBuildingPiece piece : sorted) {
            rects.add(new OmtPieceRect(
                piece.getOffsetX() * stride - minOriginX,
                piece.getOffsetY() * stride - minOriginY,
                stride,
                stride,
                piece.getOvermapId()
            ));
        }
        return rects;
    }

    private static void validateExtents(final List<CityBuildingPiece> pieces, final List<String> warnings) {
        int maxOffsetX = 0;
        int maxOffsetY = 0;
        for (final CityBuildingPiece piece : pieces) {
            maxOffsetX = Math.max(maxOffsetX, piece.getOffsetX());
            maxOffsetY = Math.max(maxOffsetY, piece.getOffsetY());
        }
        final int omtWidth = maxOffsetX + 1;
        final int omtHeight = maxOffsetY + 1;
        if (omtWidth > MAX_OMT_WIDTH || omtHeight > MAX_OMT_HEIGHT) {
            warnings.add("building footprint " + omtWidth + "x" + omtHeight
                + " OMT exceeds limit " + MAX_OMT_WIDTH + "x" + MAX_OMT_HEIGHT);
        }
    }

    private static String resolveFillTer(
        final JsonMapgenDefinition definition,
        final JsonMapgenRunOptions options
    ) {
        if (definition.getObjectRoot() != null && definition.getObjectRoot().has("fill_ter")) {
            final String fillTer = definition.getObjectRoot().getString("fill_ter", "");
            if (!fillTer.isEmpty()) {
                return fillTer;
            }
        }
        return options.getDefaultFillTer();
    }

    private static Bounds computeBounds(final List<ResolvedPiece> pieces, final int stride) {
        int minOriginX = Integer.MAX_VALUE;
        int minOriginY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        for (final ResolvedPiece resolved : pieces) {
            final int originX = resolved.piece.getOffsetX() * stride;
            final int originY = resolved.piece.getOffsetY() * stride;
            minOriginX = Math.min(minOriginX, originX);
            minOriginY = Math.min(minOriginY, originY);
            final Optional<MapGrid> oriented = OmTerrainMapgenPlacer.extractOrientedOmtSubmap(
                resolved.grid,
                resolved.definition.getOmTerrainGrid().orElse(null),
                resolved.piece.getOvermapId()
            );
            final int blitW = oriented.map(MapGrid::width).orElse(resolved.grid.width());
            final int blitH = oriented.map(MapGrid::height).orElse(resolved.grid.height());
            maxX = Math.max(maxX, originX + blitW);
            maxY = Math.max(maxY, originY + blitH);
        }
        if (minOriginX == Integer.MAX_VALUE) {
            minOriginX = 0;
        }
        if (minOriginY == Integer.MAX_VALUE) {
            minOriginY = 0;
        }
        return new Bounds(minOriginX, minOriginY, maxX - minOriginX, maxY - minOriginY);
    }

    private static final class ResolvedPiece {
        private final CityBuildingPiece piece;
        private final JsonMapgenDefinition definition;
        private final MapGrid grid;
        private final JsonMapgenRunOptions pieceOptions;

        private ResolvedPiece(
            final CityBuildingPiece piece,
            final JsonMapgenDefinition definition,
            final MapGrid grid,
            final JsonMapgenRunOptions pieceOptions
        ) {
            this.piece = piece;
            this.definition = definition;
            this.grid = grid;
            this.pieceOptions = pieceOptions;
        }
    }

    private static final class Bounds {
        private final int originX;
        private final int originY;
        private final int width;
        private final int height;

        private Bounds(final int originX, final int originY, final int width, final int height) {
            this.originX = originX;
            this.originY = originY;
            this.width = width;
            this.height = height;
        }
    }

    public static final class StitchResult {
        private final MapGrid grid;
        private final List<OmtPieceRect> pieceRects;
        private final List<String> warnings;

        private StitchResult(
            final MapGrid grid,
            final List<OmtPieceRect> pieceRects,
            final List<String> warnings
        ) {
            this.grid = grid;
            this.pieceRects = Collections.unmodifiableList(new ArrayList<>(pieceRects));
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        }

        public static StitchResult empty(final List<String> warnings) {
            return new StitchResult(null, Collections.emptyList(), warnings);
        }

        public Optional<MapGrid> getGrid() {
            return Optional.ofNullable(grid);
        }

        public List<OmtPieceRect> getPieceRects() {
            return pieceRects;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
