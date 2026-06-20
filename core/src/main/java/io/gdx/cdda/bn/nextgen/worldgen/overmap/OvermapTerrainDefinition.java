package io.gdx.cdda.bn.nextgen.worldgen.overmap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed BN {@code type: overmap_terrain} entry (W1). */
public final class OvermapTerrainDefinition {

    private final String id;
    private final String name;
    private final String symbol;
    private final String color;
    private final List<String> flags;
    private final List<MapgenRef> mapgenRefs;
    private final String sourceMod;

    public OvermapTerrainDefinition(
        final String id,
        final String name,
        final String symbol,
        final String color,
        final List<String> flags,
        final List<MapgenRef> mapgenRefs,
        final String sourceMod
    ) {
        this.id = id;
        this.name = name == null || name.isEmpty() ? id : name;
        this.symbol = symbol == null || symbol.isEmpty() ? "?" : symbol;
        this.color = color;
        this.flags = Collections.unmodifiableList(new ArrayList<>(flags));
        this.mapgenRefs = Collections.unmodifiableList(new ArrayList<>(mapgenRefs));
        this.sourceMod = sourceMod == null ? "" : sourceMod;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getColor() {
        return color;
    }

    public List<String> getFlags() {
        return flags;
    }

    public List<MapgenRef> getMapgenRefs() {
        return mapgenRefs;
    }

    public String getSourceMod() {
        return sourceMod;
    }

    public boolean hasSymbol() {
        return symbol != null && !symbol.isEmpty() && !"?".equals(symbol);
    }

    public boolean isRotatable() {
        for (final String flag : flags) {
            if ("NO_ROTATE".equals(flag)) {
                return false;
            }
        }
        return true;
    }

    public int jsonMapgenRefCount() {
        int count = 0;
        for (final MapgenRef ref : mapgenRefs) {
            if (ref.isJsonMethod()) {
                count++;
            }
        }
        return count;
    }
}
