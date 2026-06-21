package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

/** Discovered mapgen palettes keyed by id (P1, P10). */
public final class PaletteRegistry {

    private final Map<String, MapgenPalette> palettesById = new LinkedHashMap<>();

    public void put(final MapgenPalette palette) {
        palettesById.put(palette.getId(), palette);
    }

    public Optional<MapgenPalette> find(final String id) {
        return Optional.ofNullable(palettesById.get(id));
    }

    public boolean contains(final String id) {
        return palettesById.containsKey(id);
    }

    public int size() {
        return palettesById.size();
    }

    public List<String> allIds() {
        final List<String> ids = new ArrayList<>(palettesById.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }

    public MergedCharMap merge(final List<String> paletteIds, final List<String> warnings) {
        return merge(paletteIds, warnings, new JsonMapgenRunOptions());
    }

    public MergedCharMap merge(
        final List<String> paletteIds,
        final List<String> warnings,
        final JsonMapgenRunOptions options
    ) {
        final JsonMapgenRunOptions runOptions = options == null ? new JsonMapgenRunOptions() : options;
        final Random rng = runOptions.paletteRng();
        final PaletteCharMaps mergedNodes = new PaletteCharMaps();
        if (paletteIds != null) {
            for (final String paletteId : paletteIds) {
                if (paletteId == null || paletteId.isEmpty()) {
                    continue;
                }
                mergedNodes.mergeFrom(PaletteResolver.resolveWithParents(this, paletteId, warnings));
            }
        }
        return resolveToMergedCharMap(mergedNodes, rng, warnings, runOptions.getRolledParameters());
    }

    static MergedCharMap resolveToMergedCharMap(
        final PaletteCharMaps mergedNodes,
        final Random rng,
        final List<String> warnings,
        final Map<String, String> rolledParameters
    ) {
        final MergedCharMap merged = new MergedCharMap();
        if (mergedNodes == null) {
            return merged;
        }

        final Map<Integer, String> terrain = new HashMap<>();
        final Map<Integer, String> furniture = new HashMap<>();
        final Map<String, String> parameters = rolledParameters == null
            ? Collections.emptyMap()
            : rolledParameters;
        resolveNodes(mergedNodes.getTerrainByCodePoint(), terrain, rng, warning -> {
            if (warnings != null) {
                warnings.add(warning);
            }
        }, parameters);
        resolveNodes(mergedNodes.getFurnitureByCodePoint(), furniture, rng, warning -> {
            if (warnings != null) {
                warnings.add(warning);
            }
        }, parameters);
        applyTranslate(terrain, mergedNodes.getTranslateByCodePoint());
        applyTranslate(furniture, mergedNodes.getTranslateByCodePoint());

        merged.putAllTerrain(terrain);
        merged.putAllFurniture(furniture);
        return merged;
    }

    public static void applyInlineNodes(
        final MergedCharMap merged,
        final Map<Integer, JsonValue> terrainNodes,
        final Map<Integer, JsonValue> furnitureNodes,
        final Random rng,
        final Consumer<String> warningSink,
        final Map<String, String> rolledParameters
    ) {
        if (merged == null) {
            return;
        }
        final Map<Integer, String> terrain = new HashMap<>();
        final Map<Integer, String> furniture = new HashMap<>();
        final Map<String, String> parameters = rolledParameters == null
            ? Collections.emptyMap()
            : rolledParameters;
        if (terrainNodes != null) {
            resolveNodes(terrainNodes, terrain, rng, warningSink, parameters);
        }
        if (furnitureNodes != null) {
            resolveNodes(furnitureNodes, furniture, rng, warningSink, parameters);
        }
        merged.putAllTerrain(terrain);
        merged.putAllFurniture(furniture);
    }

    public static void applyInlineNodes(
        final MergedCharMap merged,
        final Map<Integer, JsonValue> terrainNodes,
        final Map<Integer, JsonValue> furnitureNodes,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        applyInlineNodes(merged, terrainNodes, furnitureNodes, rng, warningSink, Collections.emptyMap());
    }

    private static void resolveNodes(
        final Map<Integer, JsonValue> nodes,
        final Map<Integer, String> target,
        final Random rng,
        final Consumer<String> warningSink,
        final Map<String, String> rolledParameters
    ) {
        for (final Map.Entry<Integer, JsonValue> entry : nodes.entrySet()) {
            PaletteCharResolver.resolve(entry.getValue(), rng, warningSink, rolledParameters)
                .ifPresent(id -> target.put(entry.getKey(), id));
        }
    }

    private static void applyTranslate(
        final Map<Integer, String> resolved,
        final Map<Integer, Integer> translate
    ) {
        for (final Map.Entry<Integer, Integer> entry : translate.entrySet()) {
            final String targetId = resolved.get(entry.getValue());
            if (targetId != null) {
                resolved.put(entry.getKey(), targetId);
            }
        }
    }
}
