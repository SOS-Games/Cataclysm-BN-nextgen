package io.gdx.cdda.bn.nextgen.mapgen.building;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.ModDiscovery;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scans {@code city_building} and {@code overmap_special} vertical stacks for building bundles (P5). */
public final class CityBuildingLoader {

    private static final String CITY_BUILDING_TYPE = "city_building";
    private static final String OVERMAP_DIR = "overmap";

    private CityBuildingLoader() {}

    public static CityBuildingRegistry load(final MapgenScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final Map<String, CityBuildingDefinition> byId = new LinkedHashMap<>();

        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());

        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            loadFromOvermapDir(modInfo.getResolvedContentPath().resolve(OVERMAP_DIR), byId, warnings);
        }

        return new CityBuildingRegistry(byId, warnings);
    }

    private static void loadFromOvermapDir(
        final Path overmapRoot,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(overmapRoot)) {
            return;
        }
        loadCityBuildingFiles(overmapRoot, byId, warnings);
        OvermapSpecialBuildingLoader.loadFromOvermapTree(overmapRoot, byId, warnings);
    }

    private static void loadCityBuildingFiles(
        final Path overmapRoot,
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(overmapRoot)) {
            return;
        }
        try (java.util.stream.Stream<Path> stream = Files.list(overmapRoot)) {
            final List<Path> multitileFiles = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                .filter(path -> {
                    final String name = path.getFileName().toString().toLowerCase();
                    return name.startsWith("multitile") && name.endsWith(".json");
                })
                .sorted()
                .forEach(multitileFiles::add);
            for (final Path file : multitileFiles) {
                loadFromFile(file, byId, warnings);
            }
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
                if (!CITY_BUILDING_TYPE.equals(object.getType())) {
                    continue;
                }
                parseBuilding(object.getRoot(), file, byId, warnings);
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse city building file " + file + ": " + e.getMessage());
        }
    }

    private static void parseBuilding(
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
            warnings.add("city_building '" + id + "' has no overmaps array in " + sourceFile);
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
