package io.gdx.cdda.bn.nextgen.gamedata;

import io.gdx.cdda.bn.nextgen.DefaultContent;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataScanResult;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/** Entry point for game data loading (G1–G5). */
public final class GameDataLoader {

    private static final Logger LOG = Logger.getLogger(GameDataLoader.class.getName());

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

    /** Core load plus BN {@code mods/default.json} when present (G5). */
    public static LoadedGameData loadCore(final GameDataLoadOptions options) throws IOException {
        return loadMods(DefaultContent.defaultModIdsForRoots(options.getDataRoots()), options);
    }

    /** Loads terrain/furniture from an ordered mod list with BN override semantics (G5). */
    public static LoadedGameData loadMods(final List<String> modIds, final GameDataLoadOptions options)
        throws IOException {
        final ModRegistry registry = ModDiscovery.discover(options.getDataRoots());
        final List<String> orderedMods = ModOrderResolver.resolve(modIds, registry);
        final TerrainRegistry terrainRegistry = new TerrainRegistry();
        final FurnitureRegistry furnitureRegistry = new FurnitureRegistry();
        final List<String> loadedMods = new ArrayList<>();

        for (final String modId : orderedMods) {
            final ModInfo modInfo = registry.find(modId).orElse(null);
            if (modInfo == null) {
                final String message = "mod '" + modId + "' not found in registry; skipping";
                LOG.log(Level.WARNING, message);
                continue;
            }
            loadedMods.add(modId);
            loadModContent(modInfo, options, terrainRegistry, furnitureRegistry);
        }

        return new LoadedGameData(terrainRegistry, furnitureRegistry, loadedMods);
    }

    private static void loadModContent(
        final ModInfo modInfo,
        final GameDataLoadOptions options,
        final TerrainRegistry terrainRegistry,
        final FurnitureRegistry furnitureRegistry
    ) throws IOException {
        final List<Path> files = JsonDataScanner.listJsonFiles(
            modInfo.getResolvedContentPath(),
            options.getScanSubdirs()
        );
        for (final Path file : files) {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if ("terrain".equals(object.getType())) {
                    TerrainParser.parseInto(object.getRoot(), modInfo.getId(), terrainRegistry);
                } else if ("furniture".equals(object.getType())) {
                    FurnitureParser.parseInto(object.getRoot(), modInfo.getId(), furnitureRegistry);
                }
            }
        }
    }
}
