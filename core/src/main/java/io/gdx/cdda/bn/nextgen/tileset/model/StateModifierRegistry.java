package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ordered state modifier groups with BN merge semantics (unit 07d). */
public final class StateModifierRegistry {

    private final List<StateModifierGroup> groups = new ArrayList<>();

    public List<StateModifierGroup> getGroups() {
        return Collections.unmodifiableList(groups);
    }

    public int size() {
        return groups.size();
    }

    public void merge(final StateModifierGroup group) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).matchesMergeKey(group)) {
                groups.set(i, group);
                return;
            }
        }
        groups.add(group);
    }
}
