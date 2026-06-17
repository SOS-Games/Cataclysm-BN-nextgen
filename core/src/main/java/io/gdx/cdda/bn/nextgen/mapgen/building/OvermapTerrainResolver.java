package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Maps BN overmap terrain ids to runnable json mapgen definitions (P5). */
public final class OvermapTerrainResolver {

    private static final List<String> ROTATION_SUFFIXES = Arrays.asList(
        "_north_east",
        "_north_west",
        "_south_east",
        "_south_west",
        "_north",
        "_south",
        "_east",
        "_west"
    );

    private OvermapTerrainResolver() {}

    public static String stripRotation(final String overmapId) {
        if (overmapId == null || overmapId.isEmpty()) {
            return overmapId;
        }
        final String lower = overmapId.toLowerCase(Locale.ROOT);
        for (final String suffix : ROTATION_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return overmapId.substring(0, overmapId.length() - suffix.length());
            }
        }
        return overmapId;
    }

    public static Optional<JsonMapgenDefinition> resolveMapgen(
        final MapgenCatalog catalog,
        final String overmapId
    ) {
        if (catalog == null || overmapId == null || overmapId.isEmpty()) {
            return Optional.empty();
        }
        Optional<JsonMapgenDefinition> match = catalog.findFirstRunnableByOmTerrain(overmapId);
        if (match.isPresent()) {
            return match;
        }
        final String stripped = stripRotation(overmapId);
        if (!stripped.equals(overmapId)) {
            match = catalog.findFirstRunnableByOmTerrain(stripped);
        }
        return match;
    }

    public static Optional<String> resolvedOmTerrain(
        final MapgenCatalog catalog,
        final String overmapId
    ) {
        return resolveMapgen(catalog, overmapId)
            .flatMap(def -> def.getOmTerrain().isEmpty()
                ? Optional.empty()
                : Optional.of(def.getOmTerrain().get(0)));
    }
}
