package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mutable-at-load monster group registry with replacement semantics on duplicate ids (G7). */
public final class MonsterGroupRegistry {

    private final Map<String, MonsterGroupDefinition> groupsById = new LinkedHashMap<>();

    public Optional<MonsterGroupDefinition> find(final String id) {
        return Optional.ofNullable(groupsById.get(id));
    }

    public void put(final MonsterGroupDefinition definition) {
        groupsById.put(definition.getId(), definition);
    }

    public boolean contains(final String id) {
        return groupsById.containsKey(id);
    }

    public int size() {
        return groupsById.size();
    }

    public List<String> allIds() {
        final List<String> ids = new ArrayList<>(groupsById.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }
}
