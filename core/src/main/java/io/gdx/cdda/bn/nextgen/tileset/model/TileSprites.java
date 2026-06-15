package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Foreground/background sprite lists for one tile id (unit 08 partial). */
public final class TileSprites {

    private final WeightedSpriteList foreground = new WeightedSpriteList();
    private final WeightedSpriteList background = new WeightedSpriteList();

    public WeightedSpriteList getForeground() {
        return foreground;
    }

    public WeightedSpriteList getBackground() {
        return background;
    }

    public boolean hasAnySprites() {
        return !foreground.isEmpty() || !background.isEmpty();
    }
}
