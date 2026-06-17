package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Index of {@link CityBuildingDefinition} by id and linked mapgen om_terrain (P5). */
public final class CityBuildingRegistry {

    private final Map<String, CityBuildingDefinition> byId;
    private final Map<String, CityBuildingDefinition> byOmTerrain;
    private final List<String> warnings;

    public CityBuildingRegistry(
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings
    ) {
        this(byId, warnings, Collections.emptyMap());
    }

    private CityBuildingRegistry(
        final Map<String, CityBuildingDefinition> byId,
        final List<String> warnings,
        final Map<String, CityBuildingDefinition> byOmTerrain
    ) {
        this.byId = Collections.unmodifiableMap(new LinkedHashMap<>(byId));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        this.byOmTerrain = Collections.unmodifiableMap(new HashMap<>(byOmTerrain));
    }

    public static CityBuildingRegistry empty() {
        return new CityBuildingRegistry(Collections.emptyMap(), Collections.emptyList());
    }

    /** Adds O(1) om_terrain lookups using resolved piece ids from {@code catalog}. */
    public CityBuildingRegistry withOmTerrainIndex(final MapgenCatalog catalog) {
        if (catalog == null || byId.isEmpty()) {
            return this;
        }
        final Map<String, CityBuildingDefinition> index = new HashMap<>();
        for (final CityBuildingDefinition building : byId.values()) {
            linkOmTerrain(index, building.getId(), building);
            for (final CityBuildingPiece piece : building.getPieces()) {
                linkOmTerrain(index, piece.getOvermapId(), building);
                final String stripped = OvermapTerrainResolver.stripRotation(piece.getOvermapId());
                if (!stripped.equals(piece.getOvermapId())) {
                    linkOmTerrain(index, stripped, building);
                }
                OvermapTerrainResolver.resolvedOmTerrain(catalog, piece.getOvermapId())
                    .ifPresent(resolved -> linkOmTerrain(index, resolved, building));
                if (!stripped.equals(piece.getOvermapId())) {
                    OvermapTerrainResolver.resolvedOmTerrain(catalog, stripped)
                        .ifPresent(resolved -> linkOmTerrain(index, resolved, building));
                }
            }
        }
        return new CityBuildingRegistry(byId, warnings, index);
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<CityBuildingDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }

    public Optional<CityBuildingDefinition> findById(final String cityBuildingId) {
        if (cityBuildingId == null || cityBuildingId.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(cityBuildingId));
    }

    public Optional<CityBuildingDefinition> findByOmTerrain(final String omTerrain) {
        if (omTerrain == null || omTerrain.isEmpty()) {
            return Optional.empty();
        }
        final CityBuildingDefinition indexed = byOmTerrain.get(omTerrain);
        if (indexed != null) {
            return Optional.of(indexed);
        }
        return findById(omTerrain);
    }

    /** @deprecated use {@link #findByOmTerrain(String)} after {@link #withOmTerrainIndex(MapgenCatalog)} */
    @Deprecated
    public Optional<CityBuildingDefinition> findByOmTerrain(
        final String omTerrain,
        final MapgenCatalog catalog
    ) {
        final Optional<CityBuildingDefinition> indexed = findByOmTerrain(omTerrain);
        if (indexed.isPresent() || catalog == null) {
            return indexed;
        }
        for (final CityBuildingDefinition building : byId.values()) {
            if (buildingLinksOmTerrain(building, omTerrain, catalog)) {
                return Optional.of(building);
            }
        }
        return Optional.empty();
    }

    public int size() {
        return byId.size();
    }

    private static void linkOmTerrain(
        final Map<String, CityBuildingDefinition> index,
        final String omTerrain,
        final CityBuildingDefinition building
    ) {
        if (omTerrain == null || omTerrain.isEmpty()) {
            return;
        }
        index.putIfAbsent(omTerrain, building);
    }

    private static boolean buildingLinksOmTerrain(
        final CityBuildingDefinition building,
        final String omTerrain,
        final MapgenCatalog catalog
    ) {
        if (catalog == null) {
            return false;
        }
        for (final CityBuildingPiece piece : building.getPieces()) {
            final Optional<String> resolved = OvermapTerrainResolver.resolvedOmTerrain(
                catalog,
                piece.getOvermapId()
            );
            if (resolved.isPresent() && omTerrain.equals(resolved.get())) {
                return true;
            }
        }
        return false;
    }
}
