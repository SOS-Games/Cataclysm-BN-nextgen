package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable load product for game data (G3 terrain/furniture; G6/G7 spawn registries). */
public final class LoadedGameData {

    private final TerrainRegistry terrain;
    private final FurnitureRegistry furniture;
    private final ItemGroupRegistry itemGroups;
    private final MonsterGroupRegistry monsterGroups;
    private final List<String> sourceMods;

    public LoadedGameData(
        final TerrainRegistry terrain,
        final FurnitureRegistry furniture,
        final List<String> sourceMods
    ) {
        this(terrain, furniture, new ItemGroupRegistry(), new MonsterGroupRegistry(), sourceMods);
    }

    public LoadedGameData(
        final TerrainRegistry terrain,
        final FurnitureRegistry furniture,
        final ItemGroupRegistry itemGroups,
        final MonsterGroupRegistry monsterGroups,
        final List<String> sourceMods
    ) {
        this.terrain = terrain;
        this.furniture = furniture;
        this.itemGroups = itemGroups;
        this.monsterGroups = monsterGroups;
        this.sourceMods = Collections.unmodifiableList(new ArrayList<>(sourceMods));
    }

    public TerrainRegistry getTerrain() {
        return terrain;
    }

    public FurnitureRegistry getFurniture() {
        return furniture;
    }

    public ItemGroupRegistry getItemGroups() {
        return itemGroups;
    }

    public MonsterGroupRegistry getMonsterGroups() {
        return monsterGroups;
    }

    public List<String> getSourceMods() {
        return sourceMods;
    }
}
