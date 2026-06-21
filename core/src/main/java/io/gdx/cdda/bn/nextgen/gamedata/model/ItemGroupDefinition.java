package io.gdx.cdda.bn.nextgen.gamedata.model;

/** Immutable item group definition parsed from BN JSON (G6). */
public final class ItemGroupDefinition {

    private final String id;
    private final String subtype;
    private final String sourceMod;

    public ItemGroupDefinition(final String id, final String subtype, final String sourceMod) {
        this.id = id;
        this.subtype = subtype;
        this.sourceMod = sourceMod;
    }

    public String getId() {
        return id;
    }

    public String getSubtype() {
        return subtype;
    }

    public String getSourceMod() {
        return sourceMod;
    }

    /** Label for spawn overlays — BN item groups have no separate name field. */
    public String getDisplayName() {
        return id;
    }
}
