package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registered tile definition (unit 07b / 08 partial). */
public final class TileDefinition {

    public static final int TILESET_NO_MASK = -1;

    private final String id;
    private final TileSprites sprites = new TileSprites();
    private final List<String> availableSubtiles = new ArrayList<>();
    private int offsetX;
    private int offsetY;
    private int offsetRetractedX;
    private int offsetRetractedY;
    private float pixelScale = 1f;
    private boolean multitile;
    private boolean rotates;
    private boolean multitileSubtile;
    private boolean hasOmTransparency;
    private boolean animated;
    private int height3d;

    public TileDefinition(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public TileSprites getSprites() {
        return sprites;
    }

    public List<String> getAvailableSubtiles() {
        return Collections.unmodifiableList(availableSubtiles);
    }

    public void addAvailableSubtile(final String subtileId) {
        availableSubtiles.add(subtileId);
    }

    public int getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(final int offsetX) {
        this.offsetX = offsetX;
    }

    public int getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(final int offsetY) {
        this.offsetY = offsetY;
    }

    public int getOffsetRetractedX() {
        return offsetRetractedX;
    }

    public void setOffsetRetractedX(final int offsetRetractedX) {
        this.offsetRetractedX = offsetRetractedX;
    }

    public int getOffsetRetractedY() {
        return offsetRetractedY;
    }

    public void setOffsetRetractedY(final int offsetRetractedY) {
        this.offsetRetractedY = offsetRetractedY;
    }

    public float getPixelScale() {
        return pixelScale;
    }

    public void setPixelScale(final float pixelScale) {
        this.pixelScale = pixelScale;
    }

    public boolean isMultitile() {
        return multitile;
    }

    public void setMultitile(final boolean multitile) {
        this.multitile = multitile;
    }

    public boolean isRotates() {
        return rotates;
    }

    public void setRotates(final boolean rotates) {
        this.rotates = rotates;
    }

    public boolean isMultitileSubtile() {
        return multitileSubtile;
    }

    public void setMultitileSubtile(final boolean multitileSubtile) {
        this.multitileSubtile = multitileSubtile;
    }

    public boolean isHasOmTransparency() {
        return hasOmTransparency;
    }

    public void setHasOmTransparency(final boolean hasOmTransparency) {
        this.hasOmTransparency = hasOmTransparency;
    }

    public boolean isAnimated() {
        return animated;
    }

    public void setAnimated(final boolean animated) {
        this.animated = animated;
    }

    public int getHeight3d() {
        return height3d;
    }

    public void setHeight3d(final int height3d) {
        this.height3d = height3d;
    }

    public int getForegroundSpriteIndex() {
        return sprites.getForeground().getFirstSpriteIndex();
    }

    public int getBackgroundSpriteIndex() {
        return sprites.getBackground().getFirstSpriteIndex();
    }
}
