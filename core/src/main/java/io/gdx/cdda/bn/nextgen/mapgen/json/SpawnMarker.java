package io.gdx.cdda.bn.nextgen.mapgen.json;

/** Metadata for entity spawns not rendered in ter/furn preview (P13b). */
public final class SpawnMarker {

    public enum Kind {
        ITEM_GROUP,
        MONSTER_GROUP
    }

    public final Kind kind;
    public final String groupId;
    public final String displayName;
    public final int x;
    public final int y;
    public final float density;

    public SpawnMarker(
        final Kind kind,
        final String groupId,
        final int x,
        final int y,
        final float density
    ) {
        this(kind, groupId, null, x, y, density);
    }

    public SpawnMarker(
        final Kind kind,
        final String groupId,
        final String displayName,
        final int x,
        final int y,
        final float density
    ) {
        this.kind = kind;
        this.groupId = groupId;
        this.displayName = displayName;
        this.x = x;
        this.y = y;
        this.density = density;
    }

    public String label() {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        return groupId;
    }
}
