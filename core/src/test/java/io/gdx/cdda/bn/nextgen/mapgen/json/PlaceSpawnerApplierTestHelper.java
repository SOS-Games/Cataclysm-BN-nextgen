package io.gdx.cdda.bn.nextgen.mapgen.json;

import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.ItemGroupRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.MonsterGroupRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;

import java.util.Collections;

final class PlaceSpawnerApplierTestHelper {

    private PlaceSpawnerApplierTestHelper() {}

    static LoadedGameData gameDataWithMonNullGroup() {
        final MonsterGroupRegistry monsterGroups = new MonsterGroupRegistry();
        monsterGroups.put(new MonsterGroupDefinition("GROUP_EMPTY", "mon_null", "test"));
        return new LoadedGameData(
            new TerrainRegistry(),
            new FurnitureRegistry(),
            new ItemGroupRegistry(),
            monsterGroups,
            Collections.singletonList("test")
        );
    }
}
