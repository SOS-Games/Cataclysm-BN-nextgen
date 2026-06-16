package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mutable-at-load terrain registry with replacement semantics on duplicate ids. */
public final class TerrainRegistry {

    private final Map<String, TerrainDefinition> terrainById = new LinkedHashMap<>();

    public Optional<TerrainDefinition> find(final String id) {
        return Optional.ofNullable(terrainById.get(id));
    }

    public void put(final TerrainDefinition definition) {
        terrainById.put(definition.getId(), definition);
    }

    public boolean contains(final String id) {
        return terrainById.containsKey(id);
    }

    public int size() {
        return terrainById.size();
    }

    public List<String> allIds() {
        final List<String> ids = new ArrayList<>(terrainById.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }
}
