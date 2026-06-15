package io.gdx.cdda.bn.nextgen.tileset.atlas;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/** Blits sprite pixels into a packed atlas rectangle during load or composite. */
@FunctionalInterface
public interface SpriteBlitter {

    void blit(Pixmap destination, int destX, int destY, int width, int height);
}
