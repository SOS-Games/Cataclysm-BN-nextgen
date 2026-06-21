package io.gdx.cdda.bn.nextgen.mapgen.compose;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.map.MapGridRotator;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.building.OvermapTerrainResolver;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunner;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;
import io.gdx.cdda.bn.nextgen.mapgen.json.OmTerrainGrid;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;
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
        if (building.isWholeOvermapSpecial()) {
            return SpecialLayoutFloorComposer.build(building, catalog, palettes, runOptions);
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

        final int preferredZ = pickDefaultZ(zLevels);
        final Map<Integer, FloorBuildResult> rawFloors = new LinkedHashMap<>();
        for (final int zLevel : zLevels) {
            options.clearSpawnMarkers();
            final Optional<FloorBuildResult> floor = buildFloorGrid(
                building.piecesAtZ(zLevel),
                catalog,
                palettes,
                options,
                warnings,
                zLevel
            );
            floor.ifPresent(value -> rawFloors.put(zLevel, value));
        }

        if (rawFloors.isEmpty()) {
            throw new IllegalArgumentException(
                "building '" + building.getId() + "' produced no runnable floors"
            );
        }

        final List<Integer> builtZLevels = builtZLevelsFrom(zLevels, rawFloors);
        final int referenceZ = pickReferenceZ(builtZLevels, preferredZ);
        if (!rawFloors.containsKey(preferredZ)) {
            warnings.add("floor z=" + preferredZ + " could not be built; using z=" + referenceZ + " as reference");
        }

        final FloorBuildResult referenceFloor = rawFloors.get(referenceZ);
        final Optional<OmTerrainGrid> referenceOmGrid = referenceFloor.referenceOmGrid;
        final int canvasWidth = referenceFloor.grid.width();
        final int canvasHeight = referenceFloor.grid.height();
        final boolean alignFloors = builtZLevels.size() > 1 && referenceOmGrid.isPresent();
        final String groundOvermapId = resolveGroundOvermapId(building, referenceZ);

        final Map<Integer, MapGrid> gridsByZ = new LinkedHashMap<>();
        final Map<Integer, List<OmtPieceRect>> pieceLayoutsByZ = new LinkedHashMap<>();
        final Map<Integer, List<SpawnMarker>> spawnMarkersByZ = new LinkedHashMap<>();
        for (final int zLevel : zLevels) {
            final FloorBuildResult raw = rawFloors.get(zLevel);
            if (raw == null) {
                continue;
            }
            final MapGrid grid = alignFloors && zLevel != referenceZ
                ? placeFloorOnReferenceCanvas(
                    raw,
                    referenceOmGrid.get(),
                    canvasWidth,
                    canvasHeight,
                    groundOvermapId,
                    warnings,
                    zLevel
                )
                : raw.grid;
            gridsByZ.put(zLevel, grid);
            if (!raw.pieceLayouts.isEmpty()) {
                pieceLayoutsByZ.put(zLevel, raw.pieceLayouts);
            }
            if (!raw.spawnMarkers.isEmpty()) {
                spawnMarkersByZ.put(zLevel, raw.spawnMarkers);
            }
        }

        final List<Integer> volumeZLevels = builtZLevelsFrom(zLevels, gridsByZ);
        final int activeZ = pickReferenceZ(volumeZLevels, referenceZ);

        return new MapVolumeBuildResult(
            new MapVolume(building.getId(), volumeZLevels, gridsByZ, pieceLayoutsByZ, activeZ),
            warnings,
            spawnMarkersByZ
        );
    }

    private static MapGrid placeFloorOnReferenceCanvas(
        final FloorBuildResult raw,
        final OmTerrainGrid referenceOmGrid,
        final int canvasWidth,
        final int canvasHeight,
        final String groundOvermapId,
        final List<String> warnings,
        final int zLevel
    ) {
        if (!raw.sourceOmGrid.isPresent() || raw.singlePiece == null) {
            warnings.add("floor z=" + zLevel + " could not be aligned to reference canvas; using raw grid");
            return raw.grid;
        }

        final Optional<OmTerrainMapgenPlacer.GridCell> anchor = OmTerrainMapgenPlacer.findCell(
            referenceOmGrid,
            groundOvermapId
        );
        final int anchorCol = anchor.map(cell -> cell.col).orElse(raw.singlePiece.getOffsetX());
        final int anchorRow = anchor.map(cell -> cell.row).orElse(raw.singlePiece.getOffsetY());

        final Optional<OmTerrainMapgenPlacer.GridCell> sourceCell = OmTerrainMapgenPlacer.findCell(
            raw.sourceOmGrid.get(),
            raw.singlePiece.getOvermapId()
        );
        final int destOmtCol = anchorCol + sourceCell.map(cell -> cell.col).orElse(0);
        final int destOmtRow = anchorRow + sourceCell.map(cell -> cell.row).orElse(0);

        final MapGrid canvas = new MapGrid(canvasWidth, canvasHeight, raw.grid.getDefaultTerrainId());
        final Optional<OmTerrainMapgenPlacer.PlacementRect> placement = OmTerrainMapgenPlacer.blitAtReferenceCell(
            canvas,
            raw.grid,
            raw.sourceOmGrid.get(),
            raw.singlePiece.getOvermapId(),
            referenceOmGrid,
            destOmtCol,
            destOmtRow,
            raw.grid.getDefaultTerrainId()
        );
        if (!placement.isPresent()) {
            warnings.add("floor z=" + zLevel + " could not be placed on reference canvas; using raw grid");
            return raw.grid;
        }
        return canvas;
    }

    private static String resolveGroundOvermapId(
        final CityBuildingDefinition building,
        final int referenceZ
    ) {
        final List<CityBuildingPiece> groundPieces = building.piecesAtZ(referenceZ);
        if (groundPieces.isEmpty()) {
            return "";
        }
        return groundPieces.get(0).getOvermapId();
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
                final MapGrid grid = JsonMapgenRunner.run(definition, catalog, palettes, options);
                return Optional.of(new FloorBuildResult(
                    grid,
                    combined.get().getPieceRects(),
                    definition.getOmTerrainGrid(),
                    Optional.empty(),
                    List.copyOf(options.getSpawnMarkers())
                ));
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
            return Optional.of(new FloorBuildResult(
                stitched.getGrid().get(),
                stitched.getPieceRects(),
                Optional.empty(),
                Optional.empty(),
                List.copyOf(options.getSpawnMarkers())
            ));
        }
        return runSinglePiece(pieces.get(0), catalog, palettes, options, warnings, zLevel);
    }

    private static Optional<FloorBuildResult> runSinglePiece(
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
        final boolean multitileCrop = definition.get().getOmTerrainGrid().isPresent();
        final JsonMapgenRunOptions pieceOptions = options.deriveWithOmtRotation(
            MapGridRotator.runnerOmtRotation(multitileCrop, piece.getOvermapId())
        );
        final MapGrid grid = JsonMapgenRunner.run(definition.get(), catalog, palettes, pieceOptions);
        warnings.addAll(pieceOptions.getWarnings());
        options.addSpawnMarkers(pieceOptions.getSpawnMarkers());
        return Optional.of(new FloorBuildResult(
            grid,
            Collections.emptyList(),
            definition.get().getOmTerrainGrid(),
            Optional.of(piece),
            List.copyOf(options.getSpawnMarkers())
        ));
    }

    private static int pickDefaultZ(final List<Integer> zLevels) {
        if (zLevels.contains(0)) {
            return 0;
        }
        return zLevels.get(0);
    }

    static List<Integer> builtZLevelsFrom(
        final List<Integer> zLevels,
        final Map<Integer, ?> builtByZ
    ) {
        final List<Integer> built = new ArrayList<>();
        for (final int z : zLevels) {
            if (builtByZ.containsKey(z)) {
                built.add(z);
            }
        }
        if (built.isEmpty()) {
            throw new IllegalArgumentException("no floors were built");
        }
        return built;
    }

    static int pickReferenceZ(final List<Integer> builtZLevels, final int preferredZ) {
        if (builtZLevels.contains(preferredZ)) {
            return preferredZ;
        }
        return builtZLevels.get(0);
    }

    private static final class FloorBuildResult {
        private final MapGrid grid;
        private final List<OmtPieceRect> pieceLayouts;
        private final Optional<OmTerrainGrid> sourceOmGrid;
        private final Optional<OmTerrainGrid> referenceOmGrid;
        private final CityBuildingPiece singlePiece;
        private final List<SpawnMarker> spawnMarkers;

        private FloorBuildResult(
            final MapGrid grid,
            final List<OmtPieceRect> pieceLayouts,
            final Optional<OmTerrainGrid> sourceOmGrid,
            final Optional<CityBuildingPiece> singlePiece,
            final List<SpawnMarker> spawnMarkers
        ) {
            this.grid = grid;
            this.pieceLayouts = pieceLayouts;
            this.sourceOmGrid = sourceOmGrid;
            this.referenceOmGrid = sourceOmGrid;
            this.singlePiece = singlePiece.orElse(null);
            this.spawnMarkers = spawnMarkers == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(spawnMarkers));
        }
    }

    public static final class MapVolumeBuildResult {
        private final MapVolume volume;
        private final List<String> warnings;
        private final Map<Integer, List<SpawnMarker>> spawnMarkersByZ;

        public MapVolumeBuildResult(
            final MapVolume volume,
            final List<String> warnings,
            final Map<Integer, List<SpawnMarker>> spawnMarkersByZ
        ) {
            this.volume = volume;
            this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
            this.spawnMarkersByZ = spawnMarkersByZ == null || spawnMarkersByZ.isEmpty()
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(spawnMarkersByZ));
        }

        public MapVolume getVolume() {
            return volume;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public Map<Integer, List<SpawnMarker>> getSpawnMarkersByZ() {
            return spawnMarkersByZ;
        }
    }
}
