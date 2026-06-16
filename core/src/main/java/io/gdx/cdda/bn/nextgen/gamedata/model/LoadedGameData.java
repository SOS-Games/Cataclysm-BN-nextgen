package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable load product for game data (G3 includes terrain + furniture + source mods). */
public final class LoadedGameData {

    private final TerrainRegistry terrain;
    private final FurnitureRegistry furniture;
    private final List<String> sourceMods;

    public LoadedGameData(
        final TerrainRegistry terrain,
        final FurnitureRegistry furniture,
        final List<String> sourceMods
    ) {
        this.terrain = terrain;
        this.furniture = furniture;
        this.sourceMods = Collections.unmodifiableList(new ArrayList<>(sourceMods));
    }

    public TerrainRegistry getTerrain() {
        return terrain;
    }

    public FurnitureRegistry getFurniture() {
        return furniture;
    }

    public List<String> getSourceMods() {
        return sourceMods;
    }
}
