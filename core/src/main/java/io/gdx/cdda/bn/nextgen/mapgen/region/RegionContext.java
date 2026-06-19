package io.gdx.cdda.bn.nextgen.mapgen.region;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.ModDiscovery;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Loaded BN {@code region_settings} alias tables (P11). */
public final class RegionContext {

    private static final String REGION_SETTINGS_TYPE = "region_settings";
    private static final RegionAliasTable EMPTY_TABLE = new RegionAliasTable();

    private final Map<String, RegionAliasTable> regionsById;

    private RegionContext(final Map<String, RegionAliasTable> regionsById) {
        this.regionsById = Collections.unmodifiableMap(new LinkedHashMap<>(regionsById));
    }

    public static RegionContext empty() {
        return new RegionContext(Collections.emptyMap());
    }

    public boolean isEmpty() {
        return regionsById.isEmpty();
    }

    public List<String> regionIds() {
        final List<String> ids = new ArrayList<>(regionsById.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }

    public static RegionContext load(final MapgenScanOptions options, final List<String> warnings) throws IOException {
        if (options == null || options.getDataRoots().isEmpty()) {
            return empty();
        }
        final Map<String, RegionAliasTable> regions = new LinkedHashMap<>();
        final List<String> loadWarnings = warnings == null ? new ArrayList<>() : warnings;

        for (final Path dataRoot : options.getDataRoots()) {
            loadFromJsonTree(dataRoot.resolve("json"), regions, loadWarnings);
        }

        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        loadWarnings.addAll(modRegistry.getDiscoveryWarnings());
        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            loadFromJsonTree(modInfo.getResolvedContentPath().resolve("json"), regions, loadWarnings);
            loadFromJsonTree(modInfo.getResolvedContentPath(), regions, loadWarnings);
        }

        return new RegionContext(regions);
    }

    public String resolveTerrain(
        final String regionId,
        final String terrainId,
        final Random rng,
        final java.util.function.Consumer<String> warningSink
    ) {
        return tableFor(regionId, warningSink).resolveTerrain(terrainId, rng, warningSink);
    }

    public String resolveFurniture(
        final String regionId,
        final String furnitureId,
        final Random rng,
        final java.util.function.Consumer<String> warningSink
    ) {
        return tableFor(regionId, warningSink).resolveFurniture(furnitureId, rng, warningSink);
    }

    private RegionAliasTable tableFor(
        final String regionId,
        final java.util.function.Consumer<String> warningSink
    ) {
        final String requested = regionId == null || regionId.isEmpty() ? "default" : regionId;
        final RegionAliasTable table = regionsById.get(requested);
        if (table != null) {
            return table;
        }
        if (!"default".equals(requested)) {
            final RegionAliasTable fallback = regionsById.get("default");
            if (fallback != null) {
                RegionWeightedChoice.emitWarning(
                    warningSink,
                    "unknown previewRegionId '" + requested + "'; using default"
                );
                return fallback;
            }
            RegionWeightedChoice.emitWarning(
                warningSink,
                "unknown previewRegionId '" + requested + "' and no default region loaded"
            );
        }
        return EMPTY_TABLE;
    }

    private static void loadFromJsonTree(
        final Path jsonRoot,
        final Map<String, RegionAliasTable> regions,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(jsonRoot)) {
            return;
        }
        for (final Path file : JsonDataScanner.listJsonFiles(jsonRoot, Collections.emptyList())) {
            try {
                for (final JsonDataObject object : JsonDataScanner.parseFile(file)) {
                    if (!REGION_SETTINGS_TYPE.equals(object.getType())) {
                        continue;
                    }
                    mergeRegion(object.getRoot(), regions);
                }
            } catch (final RuntimeException e) {
                warnings.add("failed to parse region_settings file " + file + ": " + e.getMessage());
            }
        }
    }

    private static void mergeRegion(final JsonValue root, final Map<String, RegionAliasTable> regions) {
        if (root == null || !root.isObject()) {
            return;
        }
        final String id = root.getString("id", null);
        if (id == null || id.isEmpty()) {
            return;
        }
        final RegionAliasTable table = regions.computeIfAbsent(id, ignored -> new RegionAliasTable());
        mergeAliasSection(root.get("terrain"), table, true);
        mergeAliasSection(root.get("furniture"), table, false);

        final JsonValue regional = root.get("region_terrain_and_furniture");
        if (regional != null && regional.isObject()) {
            mergeAliasSection(regional.get("terrain"), table, true);
            mergeAliasSection(regional.get("furniture"), table, false);
        }
    }

    private static void mergeAliasSection(
        final JsonValue section,
        final RegionAliasTable table,
        final boolean terrain
    ) {
        if (section == null || !section.isObject()) {
            return;
        }
        for (JsonValue member = section.child; member != null; member = member.next) {
            if (member.name == null || member.name.isEmpty()) {
                continue;
            }
            if (terrain) {
                table.putTerrainAlias(member.name, member);
            } else {
                table.putFurnitureAlias(member.name, member);
            }
        }
    }
}
