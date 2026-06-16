package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataScanResult;
import io.gdx.cdda.bn.nextgen.gamedata.parse.FurnitureParser;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;
import io.gdx.cdda.bn.nextgen.gamedata.parse.TerrainParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Entry point for game data loading (G1: scan and count typed JSON objects only). */
public final class GameDataLoader {

    private GameDataLoader() {}

    public static GameDataScanResult scan(final GameDataLoadOptions options) throws IOException {
        final List<Path> jsonFiles = listJsonFiles(options);
        final List<JsonDataObject> objects = JsonDataScanner.scanFiles(jsonFiles);
        final Map<String, Integer> counts = new HashMap<>();
        for (final JsonDataObject object : objects) {
            final String type = object.getType();
            counts.merge(type, 1, Integer::sum);
        }
        return new GameDataScanResult(jsonFiles, counts, objects.size());
    }

    public static List<Path> listJsonFiles(final GameDataLoadOptions options) throws IOException {
        final List<Path> allFiles = new ArrayList<>();
        for (final Path dataRoot : options.getDataRoots()) {
            final Path jsonRoot = dataRoot.resolve("json").normalize();
            allFiles.addAll(JsonDataScanner.listJsonFiles(jsonRoot, options.getScanSubdirs()));
        }
        Collections.sort(allFiles);
        return allFiles;
    }

    /** G3 loader: builds terrain/furniture registries from core JSON scan. */
    public static LoadedGameData loadCore(final GameDataLoadOptions options) throws IOException {
        final List<Path> files = listJsonFiles(options);
        final TerrainRegistry terrainRegistry = new TerrainRegistry();
        final FurnitureRegistry furnitureRegistry = new FurnitureRegistry();

        for (final Path file : files) {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if ("terrain".equals(object.getType())) {
                    TerrainParser.parseInto(object.getRoot(), "core", terrainRegistry);
                } else if ("furniture".equals(object.getType())) {
                    FurnitureParser.parseInto(object.getRoot(), "core", furnitureRegistry);
                }
            }
        }

        return new LoadedGameData(
            terrainRegistry,
            furnitureRegistry,
            Collections.singletonList("core")
        );
    }
}
