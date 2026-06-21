package io.gdx.cdda.bn.nextgen.mapgen.building;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parses {@code city_building} bundles (P5 / P7a via {@link BuildingBundleScanner}). */
public final class CityBuildingLoader {

    private static final String CITY_BUILDING_TYPE = "city_building";

    private CityBuildingLoader() {}

    public static CityBuildingRegistry load(final MapgenScanOptions options) throws IOException {
        return BuildingBundleScanner.load(options);
    }

    static void parseBuilding(
        final JsonValue root,
        final Path sourceFile,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) {
        final String id = root.getString("id", "");
        if (id.isEmpty()) {
            warnings.add("skipped city_building without id in " + sourceFile);
            return;
        }
        if (byId.containsKey(id)) {
            warnings.add("duplicate city_building id '" + id + "' in " + sourceFile + " (first wins)");
            return;
        }

        final List<CityBuildingPiece> pieces = new ArrayList<>();
        final JsonValue overmaps = root.get("overmaps");
        if (overmaps == null || !overmaps.isArray()) {
            final String copyFrom = root.getString("copy-from", "");
            if (!copyFrom.isEmpty()) {
                warnings.add("bundle " + id + " skipped: copy-from unresolved");
            } else {
                warnings.add("city_building '" + id + "' has no overmaps array in " + sourceFile);
            }
            return;
        }
        for (JsonValue entry = overmaps.child; entry != null; entry = entry.next) {
            final CityBuildingPiece piece = parsePiece(entry, id, sourceFile, warnings);
            if (piece != null) {
                pieces.add(piece);
            }
        }
        if (pieces.isEmpty()) {
            warnings.add("city_building '" + id + "' has no valid pieces in " + sourceFile);
            return;
        }
        byId.put(id, new CityBuildingDefinition(id, sourceFile, pieces));
    }

    private static CityBuildingPiece parsePiece(
        final JsonValue entry,
        final String buildingId,
        final Path sourceFile,
        final List<String> warnings
    ) {
        final String overmapId = entry.getString("overmap", "");
        if (overmapId.isEmpty()) {
            warnings.add("skipped city_building piece without overmap in " + sourceFile + " (" + buildingId + ")");
            return null;
        }
        final JsonValue point = entry.get("point");
        if (point == null || !point.isArray() || point.size < 3) {
            warnings.add("skipped city_building piece with invalid point in " + sourceFile + " (" + buildingId + ")");
            return null;
        }
        return new CityBuildingPiece(
            point.getInt(0),
            point.getInt(1),
            point.getInt(2),
            overmapId
        );
    }

    /** Parse a single JSON array file without mod discovery (tests). */
    static CityBuildingRegistry loadFromFixtureFile(final Path file) throws IOException {
        final Map<String, CityBuildingDefinition> byId = new LinkedHashMap<>();
        final List<String> warnings = new ArrayList<>();
        final byte[] bytes = Files.readAllBytes(file);
        final String text = new String(bytes, StandardCharsets.UTF_8);
        final JsonValue root = new JsonReader().parse(text);
        if (root.isArray()) {
            for (JsonValue child = root.child; child != null; child = child.next) {
                if (CITY_BUILDING_TYPE.equals(child.getString("type", ""))) {
                    parseBuilding(child, file, byId, warnings);
                }
            }
        } else if (CITY_BUILDING_TYPE.equals(root.getString("type", ""))) {
            parseBuilding(root, file, byId, warnings);
        }
        return new CityBuildingRegistry(byId, warnings);
    }
}
