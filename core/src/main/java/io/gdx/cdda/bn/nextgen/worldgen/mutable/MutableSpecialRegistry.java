package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Loaded mutable special ids (W6). */
public final class MutableSpecialRegistry {

    private final Map<String, MutableSpecialDefinition> byId = new LinkedHashMap<>();

    public void put(final MutableSpecialDefinition definition) {
        if (definition == null || definition.getId() == null || definition.getId().isEmpty()) {
            return;
        }
        byId.put(definition.getId(), definition);
    }

    public Optional<MutableSpecialDefinition> find(final String id) {
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

    public List<MutableSpecialDefinition> all() {
        return Collections.unmodifiableList(new ArrayList<>(byId.values()));
    }
}
