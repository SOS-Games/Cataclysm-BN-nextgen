package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable terrain definition parsed from BN JSON (G2). */
public final class TerrainDefinition {

    private final String id;
    private final String name;
    private final String description;
    private final String symbol;
    private final String color;
    private final int moveCost;
    private final List<String> flags;
    private final String looksLike;
    private final String sourceMod;

    public TerrainDefinition(
        final String id,
        final String name,
        final String description,
        final String symbol,
        final String color,
        final int moveCost,
        final List<String> flags,
        final String looksLike,
        final String sourceMod
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.symbol = symbol;
        this.color = color;
        this.moveCost = moveCost;
        this.flags = Collections.unmodifiableList(new ArrayList<>(flags));
        this.looksLike = looksLike;
        this.sourceMod = sourceMod;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getColor() {
        return color;
    }

    public int getMoveCost() {
        return moveCost;
    }

    public List<String> getFlags() {
        return flags;
    }

    public String getLooksLike() {
        return looksLike;
    }

    public String getSourceMod() {
        return sourceMod;
    }
}
