package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable furniture definition parsed from BN JSON (G3). */
public final class FurnitureDefinition {

    private final String id;
    private final String name;
    private final String symbol;
    private final String color;
    private final int moveCostMod;
    private final int requiredStr;
    private final List<String> flags;
    private final String looksLike;
    private final String sourceMod;

    public FurnitureDefinition(
        final String id,
        final String name,
        final String symbol,
        final String color,
        final int moveCostMod,
        final int requiredStr,
        final List<String> flags,
        final String looksLike,
        final String sourceMod
    ) {
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.color = color;
        this.moveCostMod = moveCostMod;
        this.requiredStr = requiredStr;
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

    public String getSymbol() {
        return symbol;
    }

    public String getColor() {
        return color;
    }

    public int getMoveCostMod() {
        return moveCostMod;
    }

    public int getRequiredStr() {
        return requiredStr;
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
