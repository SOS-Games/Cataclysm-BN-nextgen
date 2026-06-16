package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Mutable-at-load furniture registry with replacement semantics on duplicate ids. */
public final class FurnitureRegistry {

    private final Map<String, FurnitureDefinition> furnitureById = new LinkedHashMap<>();

    public Optional<FurnitureDefinition> find(final String id) {
        return Optional.ofNullable(furnitureById.get(id));
    }

    public void put(final FurnitureDefinition definition) {
        furnitureById.put(definition.getId(), definition);
    }

    public boolean contains(final String id) {
        return furnitureById.containsKey(id);
    }

    public int size() {
        return furnitureById.size();
    }

    public List<String> allIds() {
        final List<String> ids = new ArrayList<>(furnitureById.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }
}
