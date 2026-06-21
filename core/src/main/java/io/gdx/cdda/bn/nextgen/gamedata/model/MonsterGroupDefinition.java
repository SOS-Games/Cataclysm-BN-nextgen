package io.gdx.cdda.bn.nextgen.gamedata.model;

/** Immutable monster group definition parsed from BN JSON (G7). */
public final class MonsterGroupDefinition {

    private final String id;
    private final String defaultMonsterId;
    private final String sourceMod;

    public MonsterGroupDefinition(
        final String id,
        final String defaultMonsterId,
        final String sourceMod
    ) {
        this.id = id;
        this.defaultMonsterId = defaultMonsterId;
        this.sourceMod = sourceMod;
    }

    public String getId() {
        return id;
    }

    public String getDefaultMonsterId() {
        return defaultMonsterId;
    }

    public String getSourceMod() {
        return sourceMod;
    }

    /** Prefer default monster id for overlay labels when present. */
    public String getDisplayName() {
        if (defaultMonsterId != null && !defaultMonsterId.isEmpty()) {
            return defaultMonsterId;
        }
        return id;
    }
}
