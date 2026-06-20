package io.gdx.cdda.bn.nextgen.worldgen.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loaded {@link OvermapConnectionDefinition} ids (W5). */
public final class OvermapConnectionRegistry {

    private final Map<String, OvermapConnectionDefinition> byId = new LinkedHashMap<>();

    public void put(final OvermapConnectionDefinition definition) {
        if (definition == null || definition.getId() == null || definition.getId().isEmpty()) {
            return;
        }
        byId.put(definition.getId(), definition);
    }

    public Optional<OvermapConnectionDefinition> find(final String id) {
        if (id == null || id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(id));
    }

    public boolean contains(final String id) {
        return id != null && byId.containsKey(id);
    }

    public int size() {
        return byId.size();
    }

    public List<OvermapConnectionDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }
}
