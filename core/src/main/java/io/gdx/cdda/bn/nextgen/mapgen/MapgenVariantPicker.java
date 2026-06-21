package io.gdx.cdda.bn.nextgen.mapgen;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/** Weighted variant pick among catalog entries sharing an OMT label (v2.1c). */
public final class MapgenVariantPicker {

    private MapgenVariantPicker() {}

    public static List<JsonMapgenDefinition> variantsFor(
        final JsonMapgenDefinition definition,
        final MapgenCatalog catalog
    ) {
        if (definition == null || catalog == null || definition.getOmTerrain().isEmpty()) {
            return List.of();
        }
        final Set<String> keys = new LinkedHashSet<>();
        for (final String omTerrain : definition.getOmTerrain()) {
            keys.add(omTerrain.toLowerCase(Locale.ROOT));
            keys.add(MapgenPickerIndex.primaryLabel(definition).toLowerCase(Locale.ROOT));
        }
        final List<JsonMapgenDefinition> variants = new ArrayList<>();
        for (final JsonMapgenDefinition candidate : catalog.all()) {
            if (!candidate.isJsonPreviewSupported() || candidate.isDisabled()) {
                continue;
            }
            if (!candidate.isStandalonePickerEntry()) {
                continue;
            }
            for (final String omTerrain : candidate.getOmTerrain()) {
                if (keys.contains(omTerrain.toLowerCase(Locale.ROOT))) {
                    variants.add(candidate);
                    break;
                }
            }
        }
        return variants;
    }

    public static Optional<JsonMapgenDefinition> rollVariant(
        final JsonMapgenDefinition definition,
        final MapgenCatalog catalog,
        final Random rng
    ) {
        final List<JsonMapgenDefinition> variants = variantsFor(definition, catalog);
        if (variants.isEmpty()) {
            return Optional.empty();
        }
        if (variants.size() == 1) {
            return Optional.of(variants.get(0));
        }
        int totalWeight = 0;
        for (final JsonMapgenDefinition variant : variants) {
            totalWeight += Math.max(1, variant.getWeight());
        }
        int roll = rng == null ? 0 : rng.nextInt(totalWeight);
        for (final JsonMapgenDefinition variant : variants) {
            roll -= Math.max(1, variant.getWeight());
            if (roll < 0) {
                return Optional.of(variant);
            }
        }
        return Optional.of(variants.get(variants.size() - 1));
    }
}
