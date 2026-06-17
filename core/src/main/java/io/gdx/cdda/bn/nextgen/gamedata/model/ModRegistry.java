package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Discovered mods keyed by id (G5). */
public final class ModRegistry {

    private final Map<String, ModInfo> modsById;
    private final List<String> discoveryWarnings;

    public ModRegistry(final Map<String, ModInfo> modsById, final List<String> discoveryWarnings) {
        this.modsById = Collections.unmodifiableMap(new LinkedHashMap<>(modsById));
        this.discoveryWarnings = Collections.unmodifiableList(new ArrayList<>(discoveryWarnings));
    }

    public Optional<ModInfo> find(final String id) {
        return Optional.ofNullable(modsById.get(id));
    }

    public boolean contains(final String id) {
        return modsById.containsKey(id);
    }

    public int size() {
        return modsById.size();
    }

    public List<String> allIds() {
        final List<String> ids = new ArrayList<>(modsById.keySet());
        Collections.sort(ids);
        return Collections.unmodifiableList(ids);
    }

    public List<String> getDiscoveryWarnings() {
        return discoveryWarnings;
    }
}
