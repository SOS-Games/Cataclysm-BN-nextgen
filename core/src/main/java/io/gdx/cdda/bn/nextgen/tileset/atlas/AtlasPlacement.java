package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.tileset.model.SpriteSlot;

/** Packed sprite rectangle inside a dynamic atlas page (A1). */
public final class AtlasPlacement {

    private final Texture texture;
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public AtlasPlacement(
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

    public SpriteSlot toSlot() {
        return SpriteSlot.of(texture, x, y, width, height);
    }

    public TextureRegion toRegion() {
        return new TextureRegion(texture, x, y, width, height);
    }
}
