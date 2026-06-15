package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Modifier group for UV warp lookup at draw time (unit 07d). */
public final class StateModifierGroup {

    private final String groupId;
    private final boolean overrideLower;
    private final boolean useOffsetMode;
    private final List<String> whitelist;
    private final List<String> blacklist;
    private final Map<String, StateModifierTile> tiles;

    public StateModifierGroup(
        final String groupId,
        final boolean overrideLower,
        final boolean useOffsetMode,
        final List<String> whitelist,
        final List<String> blacklist,
        final Map<String, StateModifierTile> tiles
    ) {
        this.groupId = groupId;
        this.overrideLower = overrideLower;
        this.useOffsetMode = useOffsetMode;
        this.whitelist = Collections.unmodifiableList(new ArrayList<>(whitelist));
        this.blacklist = Collections.unmodifiableList(new ArrayList<>(blacklist));
        this.tiles = Collections.unmodifiableMap(new LinkedHashMap<>(tiles));
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean isOverrideLower() {
        return overrideLower;
    }

    public boolean isUseOffsetMode() {
        return useOffsetMode;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    public Map<String, StateModifierTile> getTiles() {
        return tiles;
    }

    public boolean matchesMergeKey(final StateModifierGroup other) {
        return groupId.equals(other.groupId)
            && whitelist.equals(other.whitelist)
            && blacklist.equals(other.blacklist);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StateModifierGroup)) {
            return false;
        }
        final StateModifierGroup other = (StateModifierGroup) obj;
        return overrideLower == other.overrideLower
            && useOffsetMode == other.useOffsetMode
            && groupId.equals(other.groupId)
            && whitelist.equals(other.whitelist)
            && blacklist.equals(other.blacklist)
            && tiles.equals(other.tiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, overrideLower, useOffsetMode, whitelist, blacklist, tiles);
    }
}
