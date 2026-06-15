package io.gdx.cdda.bn.nextgen.tileset.model;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** One sprite slot: shared GPU texture plus source rectangle (unit 06b). */
public final class SpriteSlot {

    private static final SpriteSlot EMPTY = new SpriteSlot(null, 0, 0, 0, 0);

    private final Texture texture;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private SpriteSlot(
        final Texture texture,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public static SpriteSlot empty() {
        return EMPTY;
    }

    public static SpriteSlot of(
        final Texture texture,
        final int x,
        final int y,
        final int width,
        final int height
    ) {
        return new SpriteSlot(texture, x, y, width, height);
    }

    public boolean isEmpty() {
        return texture == null || width <= 0 || height <= 0;
    }

    public Texture getTexture() {
        return texture;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public TextureRegion toRegion() {
        if (isEmpty()) {
            return null;
        }
        return new TextureRegion(texture, x, y, width, height);
    }
}
