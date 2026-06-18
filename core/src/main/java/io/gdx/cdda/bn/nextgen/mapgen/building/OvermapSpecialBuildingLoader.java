package io.gdx.cdda.bn.nextgen.mapgen.building;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Builds multi-floor bundles from BN static {@code overmap_special} vertical stacks (P5 / P7a). */
public final class OvermapSpecialBuildingLoader {

    private static final String OVERMAP_SPECIAL_TYPE = "overmap_special";
    private static final String OVERMAP_SPECIAL_DIR = "overmap_special";
    private static final String OVERMAP_MUTABLE_DIR = "overmap_mutable";

    private OvermapSpecialBuildingLoader() {}

    /** @deprecated use {@link BuildingBundleScanner}; kept for narrow scans in tests if needed */
    @Deprecated
    public static void loadFromOvermapTree(
        final Path overmapRoot,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(overmapRoot)) {
            return;
        }
        loadFromDirectory(overmapRoot.resolve(OVERMAP_SPECIAL_DIR), byId, warnings);
        loadFromDirectory(overmapRoot.resolve(OVERMAP_MUTABLE_DIR), byId, warnings);
    }

    private static void loadFromDirectory(
        final Path directory,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(directory, Collections.emptyList());
        for (final Path file : files) {
            loadFromFile(file, byId, warnings);
        }
    }

    private static void loadFromFile(
        final Path file,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if (!OVERMAP_SPECIAL_TYPE.equals(object.getType())) {
                    continue;
                }
                parseSpecial(object.getRoot(), file, byId, warnings);
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse overmap_special file " + file + ": " + e.getMessage());
        }
    }

    static void parseSpecial(
        final JsonValue root,
        final Path sourceFile,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) {
        final String specialId = root.getString("id", "");
        final JsonValue overmaps = root.get("overmaps");
        if (overmaps == null || !overmaps.isArray()) {
            return;
        }

        final List<CityBuildingPiece> pieces = new ArrayList<>();
        for (JsonValue entry = overmaps.child; entry != null; entry = entry.next) {
            final CityBuildingPiece piece = parsePiece(entry, specialId, sourceFile, warnings);
            if (piece != null) {
                pieces.add(piece);
            }
        }
        if (pieces.isEmpty()) {
            return;
        }

        final Map<String, List<CityBuildingPiece>> stacksByOmt = new LinkedHashMap<>();
        for (final CityBuildingPiece piece : pieces) {
            final String key = piece.getOffsetX() + "," + piece.getOffsetY();
            stacksByOmt.computeIfAbsent(key, ignored -> new ArrayList<>()).add(piece);
        }

        for (final List<CityBuildingPiece> stack : stacksByOmt.values()) {
            registerVerticalStack(stack, specialId, sourceFile, byId, warnings);
        }
        SpecialLayoutImporter.registerIfMultiColumn(specialId, pieces, sourceFile, byId);
    }

    private static void registerVerticalStack(
        final List<CityBuildingPiece> stack,
        final String specialId,
        final Path sourceFile,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) {
        final TreeSet<Integer> zLevels = new TreeSet<>();
        for (final CityBuildingPiece piece : stack) {
            zLevels.add(piece.getZLevel());
        }
        if (zLevels.size() <= 1) {
            return;
        }

        final String buildingId = pickBuildingId(stack);
        if (buildingId.isEmpty()) {
            warnings.add("skipped overmap_special stack without building id in " + sourceFile);
            return;
        }
        if (byId.containsKey(buildingId)) {
            return;
        }

        final List<CityBuildingPiece> normalized = normalizeStackToOrigin(stack);
        byId.put(buildingId, new CityBuildingDefinition(buildingId, sourceFile, normalized));
    }

    private static String pickBuildingId(final List<CityBuildingPiece> stack) {
        final CityBuildingPiece ground = stack.stream()
            .min(Comparator
                .comparingInt(CityBuildingPiece::getZLevel)
                .thenComparing(CityBuildingPiece::getOvermapId))
            .orElse(null);
        if (ground == null) {
            return "";
        }
        return OvermapTerrainResolver.stripRotation(ground.getOvermapId());
    }

    private static List<CityBuildingPiece> normalizeStackToOrigin(final List<CityBuildingPiece> stack) {
        final List<CityBuildingPiece> normalized = new ArrayList<>();
        for (final CityBuildingPiece piece : stack) {
            normalized.add(new CityBuildingPiece(0, 0, piece.getZLevel(), piece.getOvermapId()));
        }
        normalized.sort(Comparator
            .comparingInt(CityBuildingPiece::getZLevel)
            .thenComparing(CityBuildingPiece::getOvermapId));
        return normalized;
    }

    private static CityBuildingPiece parsePiece(
        final JsonValue entry,
        final String specialId,
        final Path sourceFile,
        final List<String> warnings
    ) {
        final String overmapId = entry.getString("overmap", "");
        if (overmapId.isEmpty()) {
            warnings.add("skipped overmap_special piece without overmap in " + sourceFile + " (" + specialId + ")");
            return null;
        }
        final JsonValue point = entry.get("point");
        if (point == null || !point.isArray() || point.size < 3) {
            warnings.add("skipped overmap_special piece with invalid point in " + sourceFile + " (" + specialId + ")");
            return null;
        }
        return new CityBuildingPiece(
            point.getInt(0),
            point.getInt(1),
            point.getInt(2),
            overmapId
        );
    }
}
