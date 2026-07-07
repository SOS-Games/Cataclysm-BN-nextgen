package io.gdx.cdda.bn.nextgen.worldgen.generate;

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Weighted overmap terrain id selection (W17a). */
final class WeightedOmtPicker {

    private WeightedOmtPicker() {}

    static Optional<String> pick(
        final Map<String, Integer> weights,
        final OvermapTerrainRegistry registry,
        final Random rng,
        final Set<String> warnedTokens,
        final List<String> warnings,
        final String unknownTokenPrefix
    ) {
        if (weights == null || weights.isEmpty() || rng == null) {
            return Optional.empty();
        }
        int totalWeight = 0;
        final List<String> ids = new ArrayList<>();
        final List<Integer> poolWeights = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : weights.entrySet()) {
            final String id = entry.getKey();
            if (id == null || id.isEmpty()) {
                continue;
            }
            if (registry != null && !registry.contains(id)) {
                if (warnedTokens != null && warnedTokens.add(id)) {
                    addWarning(warnings, unknownTokenPrefix + id);
                }
                continue;
            }
            final int weight = Math.max(1, entry.getValue());
            ids.add(id);
            poolWeights.add(weight);
            totalWeight += weight;
        }
        if (ids.isEmpty() || totalWeight <= 0) {
            return Optional.empty();
        }
        int roll = rng.nextInt(totalWeight);
        for (int i = 0; i < ids.size(); i++) {
            roll -= poolWeights.get(i);
            if (roll < 0) {
                return Optional.of(ids.get(i));
            }
        }
        return Optional.of(ids.get(ids.size() - 1));
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
