package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.Optional;

/** One state entry inside a modifier group (unit 07d). */
public final class StateModifierTile {

    private final String stateId;
    private final Integer foregroundSprite;
    private final int offsetX;
    private final int offsetY;

    public StateModifierTile(
        final String stateId,
        final Integer foregroundSprite,
        final int offsetX,
        final int offsetY
    ) {
        this.stateId = stateId;
        this.foregroundSprite = foregroundSprite;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public String getStateId() {
        return stateId;
    }

    public Optional<Integer> getForegroundSprite() {
        return Optional.ofNullable(foregroundSprite);
    }

    public boolean isIdentity() {
        return foregroundSprite == null;
    }

    public int getOffsetX() {
        return offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }
}
