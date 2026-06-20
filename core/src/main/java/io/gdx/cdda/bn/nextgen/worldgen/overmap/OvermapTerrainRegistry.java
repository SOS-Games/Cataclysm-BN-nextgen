package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** OMT id lookup with BN mod override semantics (W1). */
public final class OvermapTerrainRegistry {

    private final Map<String, OvermapTerrainDefinition> byId = new LinkedHashMap<>();

    public Optional<OvermapTerrainDefinition> find(final String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public void put(final OvermapTerrainDefinition definition) {
        byId.put(definition.getId(), definition);
    }

    public boolean contains(final String id) {
        return byId.containsKey(id);
    }

    public int size() {
        return byId.size();
    }

    public List<String> allIds() {
        final List<String> ids = new ArrayList<>(byId.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }
}
