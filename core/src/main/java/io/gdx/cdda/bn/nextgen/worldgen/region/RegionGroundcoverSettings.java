package io.gdx.cdda.bn.nextgen.worldgen.region;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.region.RegionContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** Weighted {@code default_groundcover} choices from region settings (Phase C). */
public final class RegionGroundcoverSettings {

    private static final String DEFAULT_TERRAIN = "t_grass";

    private final List<WeightedTerrain> choices;

    private RegionGroundcoverSettings(final List<WeightedTerrain> choices) {
        this.choices = choices == null || choices.isEmpty()
            ? List.of(new WeightedTerrain(DEFAULT_TERRAIN, 1))
            : Collections.unmodifiableList(new ArrayList<>(choices));
    }

    public static RegionGroundcoverSettings defaults() {
        return new RegionGroundcoverSettings(List.of(new WeightedTerrain(DEFAULT_TERRAIN, 1)));
    }

    public static RegionGroundcoverSettings single(final String terrainId) {
        if (terrainId == null || terrainId.isEmpty()) {
            return defaults();
        }
        return new RegionGroundcoverSettings(List.of(new WeightedTerrain(terrainId, 1)));
    }

    public static RegionGroundcoverSettings parse(final JsonValue groundcoverRoot) {
        if (groundcoverRoot == null) {
            return defaults();
        }
        final List<WeightedTerrain> parsed = parseChoices(groundcoverRoot);
        if (parsed.isEmpty()) {
            return defaults();
        }
        return new RegionGroundcoverSettings(parsed);
    }

    public boolean isWeighted() {
        return choices.size() > 1;
    }

    public String getDefaultTerrainId() {
        return choices.get(0).terrainId;
    }

    public String pick(final Random rng) {
        return pickFromChoices(choices, rng);
    }

    public String pickAt(final long seed, final int x, final int y) {
        return pickFromChoices(choices, new Random(mixCellSeed(seed, x, y)));
    }

    public String pickResolved(
        final RegionContext regionContext,
        final String regionId,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        return resolveTerrain(regionContext, regionId, pick(rng), rng, warningSink);
    }

    public String pickAtResolved(
        final RegionContext regionContext,
        final String regionId,
        final long seed,
        final int x,
        final int y,
        final Consumer<String> warningSink
    ) {
        final Random cellRng = new Random(mixCellSeed(seed, x, y));
        return resolveTerrain(regionContext, regionId, pickAt(seed, x, y), cellRng, warningSink);
    }

    /** Re-roll cells that still use the visit-wide base fill (mapgen path). */
    public void applyPerCellBaseFill(
        final MapGrid grid,
        final long previewSeed,
        final String visitBaseFillTer
    ) {
        applyPerCellBaseFill(grid, previewSeed, visitBaseFillTer, null, "default", null);
    }

    /** Re-roll base-fill cells and resolve regional aliases (mapgen path). */
    public void applyPerCellBaseFill(
        final MapGrid grid,
        final long previewSeed,
        final String visitBaseFillTer,
        final RegionContext regionContext,
        final String regionId,
        final Consumer<String> warningSink
    ) {
        if (grid == null || !isWeighted() || visitBaseFillTer == null || visitBaseFillTer.isEmpty()) {
            return;
        }
        final Random visitRng = new Random(previewSeed ^ 0x47504C41L);
        final String resolvedVisitBase = resolveTerrain(
            regionContext,
            regionId,
            visitBaseFillTer,
            visitRng,
            warningSink
        );
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String terrainId = grid.get(x, y).getTerrainId();
                if (!visitBaseFillTer.equals(terrainId) && !resolvedVisitBase.equals(terrainId)) {
                    continue;
                }
                grid.setTerrain(
                    x,
                    y,
                    pickAt(previewSeed, x, y)
                );
            }
        }
    }

    private static String resolveTerrain(
        final RegionContext regionContext,
        final String regionId,
        final String terrainId,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        if (regionContext == null || regionContext.isEmpty() || terrainId == null || terrainId.isEmpty()) {
            return terrainId;
        }
        return regionContext.resolveTerrain(regionId, terrainId, rng, warningSink);
    }

    private static long mixCellSeed(final long seed, final int x, final int y) {
        return seed ^ 0x47504C41L ^ (x * 374761393L) ^ (y * 668265263L);
    }

    private static String pickFromChoices(final List<WeightedTerrain> choices, final Random rng) {
        if (choices.isEmpty()) {
            return DEFAULT_TERRAIN;
        }
        if (rng == null) {
            return choices.get(0).terrainId;
        }
        int totalWeight = 0;
        for (final WeightedTerrain choice : choices) {
            totalWeight += choice.weight;
        }
        if (totalWeight <= 0) {
            return choices.get(0).terrainId;
        }
        int roll = rng.nextInt(totalWeight);
        for (final WeightedTerrain choice : choices) {
            roll -= choice.weight;
            if (roll < 0) {
                return choice.terrainId;
            }
        }
        return choices.get(choices.size() - 1).terrainId;
    }

    private static List<WeightedTerrain> parseChoices(final JsonValue value) {
        final List<WeightedTerrain> choices = new ArrayList<>();
        if (value.isString()) {
            final String terrain = value.asString();
            if (terrain != null && !terrain.isEmpty()) {
                choices.add(new WeightedTerrain(terrain, 1));
            }
            return choices;
        }
        if (value.isObject()) {
            if (value.has("terrain")) {
                final String terrain = value.getString("terrain", "");
                if (!terrain.isEmpty()) {
                    choices.add(new WeightedTerrain(terrain, Math.max(1, value.getInt("weight", 1))));
                }
                return choices;
            }
            if (value.has("item")) {
                final String terrain = value.getString("item", "");
                if (!terrain.isEmpty()) {
                    choices.add(new WeightedTerrain(terrain, Math.max(1, value.getInt("weight", 1))));
                }
                return choices;
            }
            for (JsonValue member = value.child; member != null; member = member.next) {
                if (member.name == null || member.name.isEmpty()) {
                    continue;
                }
                final int weight = member.isNumber() ? Math.max(1, member.asInt()) : 1;
                choices.add(new WeightedTerrain(member.name, weight));
            }
            return choices;
        }
        if (value.isArray()) {
            for (JsonValue child = value.child; child != null; child = child.next) {
                if (child.isString() && !child.asString().isEmpty()) {
                    choices.add(new WeightedTerrain(child.asString(), 1));
                } else if (child.isArray() && child.size >= 2 && child.get(1).isNumber()) {
                    final JsonValue idNode = child.get(0);
                    if (idNode != null && idNode.isString() && !idNode.asString().isEmpty()) {
                        choices.add(new WeightedTerrain(idNode.asString(), Math.max(1, child.getInt(1))));
                    }
                } else if (child.isObject()) {
                    final String terrain = child.getString("terrain", child.getString("item", ""));
                    if (!terrain.isEmpty()) {
                        choices.add(new WeightedTerrain(terrain, Math.max(1, child.getInt("weight", 1))));
                    }
                }
            }
        }
        return choices;
    }

    private static final class WeightedTerrain {
        private final String terrainId;
        private final int weight;

        private WeightedTerrain(final String terrainId, final int weight) {
            this.terrainId = terrainId;
            this.weight = Math.max(1, weight);
        }
    }
}
