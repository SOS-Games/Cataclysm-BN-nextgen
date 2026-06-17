package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.palette.PaletteRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Runs mapgen per floor for a {@link CityBuildingDefinition} (P5/P6). */
public final class MapVolumeBuilder {

    private MapVolumeBuilder() {}

    public static MapVolumeBuildResult build(
        final CityBuildingDefinition building,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions runOptions
    ) {
        if (building == null) {
            throw new IllegalArgumentException("building is required");
        }
        if (catalog == null || palettes == null) {
            throw new IllegalStateException("catalog and palettes are required");
        }

        final JsonMapgenRunOptions options = runOptions == null ? new JsonMapgenRunOptions() : runOptions;
        final List<String> warnings = new ArrayList<>(options.getWarnings());
        final Map<Integer, MapGrid> gridsByZ = new LinkedHashMap<>();
        final Map<Integer, List<OmtPieceRect>> pieceLayoutsByZ = new LinkedHashMap<>();

        for (final int zLevel : building.distinctZLevels()) {
            final List<CityBuildingPiece> pieces = building.piecesAtZ(zLevel);
            final Optional<FloorBuildResult> floor = buildFloorGrid(
                pieces,
                catalog,
                palettes,
                options,
                warnings,
                zLevel
            );
            if (floor.isPresent()) {
                gridsByZ.put(zLevel, floor.get().grid);
                if (!floor.get().pieceLayouts.isEmpty()) {
                    pieceLayoutsByZ.put(zLevel, floor.get().pieceLayouts);
                }
            }
        }

        if (gridsByZ.isEmpty()) {
            throw new IllegalArgumentException(
                "building '" + building.getId() + "' produced no runnable floors"
            );
        }

        final List<Integer> zLevels = new ArrayList<>(gridsByZ.keySet());
        zLevels.sort(Integer::compareTo);
        final int defaultZ = pickDefaultZ(zLevels);
        return new MapVolumeBuildResult(
            new MapVolume(building.getId(), zLevels, gridsByZ, pieceLayoutsByZ, defaultZ),
            warnings
        );
    }

    private static Optional<FloorBuildResult> buildFloorGrid(
        final List<CityBuildingPiece> pieces,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final List<String> warnings,
        final int zLevel
    ) {
        if (pieces.isEmpty()) {
            warnings.add("no pieces at z=" + zLevel);
            return Optional.empty();
        }
        if (OmtStitchComposer.needsStitch(pieces)) {
            final Optional<CombinedFloorMapgenResolver.CombinedFloorMatch> combined =
                CombinedFloorMapgenResolver.resolve(catalog, pieces);
            if (combined.isPresent()) {
                final JsonMapgenDefinition definition = combined.get().getDefinition();
                if (!definition.isJsonPreviewSupported()) {
                    warnings.add("unsupported combined floor mapgen at z=" + zLevel);
                    return Optional.empty();
                }
                final MapGrid grid = JsonMapgenRunner.run(definition, palettes, options);
                return Optional.of(new FloorBuildResult(grid, combined.get().getPieceRects()));
            }
            final OmtStitchComposer.StitchResult stitched = OmtStitchComposer.stitch(
                pieces,
                catalog,
                palettes,
                options
            );
            warnings.addAll(stitched.getWarnings());
            if (!stitched.getGrid().isPresent()) {
                warnings.add("stitch failed for z=" + zLevel);
                return Optional.empty();
            }
            return Optional.of(new FloorBuildResult(stitched.getGrid().get(), stitched.getPieceRects()));
        }
        final Optional<MapGrid> grid = runSinglePiece(pieces.get(0), catalog, palettes, options, warnings, zLevel);
        return grid.map(value -> new FloorBuildResult(value, Collections.emptyList()));
    }

    private static Optional<MapGrid> runSinglePiece(
        final CityBuildingPiece piece,
        final MapgenCatalog catalog,
        final PaletteRegistry palettes,
        final JsonMapgenRunOptions options,
        final List<String> warnings,
        final int zLevel
    ) {
        final Optional<JsonMapgenDefinition> definition = OvermapTerrainResolver.resolveMapgen(
            catalog,
            piece.getOvermapId()
        );
        if (!definition.isPresent()) {
            warnings.add("no runnable mapgen for overmap '" + piece.getOvermapId() + "' at z=" + zLevel);
            return Optional.empty();
        }
        if (!definition.get().isJsonPreviewSupported()) {
            warnings.add("unsupported mapgen for overmap '" + piece.getOvermapId() + "' at z=" + zLevel);
            return Optional.empty();
        }
        return Optional.of(JsonMapgenRunner.run(definition.get(), palettes, options));
    }

    private static int pickDefaultZ(final List<Integer> zLevels) {
        if (zLevels.contains(0)) {
            return 0;
        }
        return zLevels.get(0);
    }

    private static final class FloorBuildResult {
        private final MapGrid grid;
        private final List<OmtPieceRect> pieceLayouts;

        private FloorBuildResult(final MapGrid grid, final List<OmtPieceRect> pieceLayouts) {
            this.grid = grid;
            this.pieceLayouts = pieceLayouts;
        }
    }

    public static final class MapVolumeBuildResult {
        private final MapVolume volume;
        private final List<String> warnings;

        public MapVolumeBuildResult(final MapVolume volume, final List<String> warnings) {
            this.volume = volume;
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        }

        public MapVolume getVolume() {
            return volume;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
