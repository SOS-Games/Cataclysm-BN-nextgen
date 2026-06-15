package io.gdx.cdda.bn.nextgen.tileset.model;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Global sprite texture table (one of eight baked effect tables; unit 06b/06c). */
public final class SpriteTextureTable {

    private final List<SpriteSlot> slots = new ArrayList<>();
    private final Set<Texture> ownedTextures = new HashSet<>();

    public int size() {
        return slots.size();
    }

    public void ensureCapacity(final int requiredSize) {
        while (slots.size() < requiredSize) {
            slots.add(SpriteSlot.empty());
        }
    }

    public SpriteSlot get(final int index) {
        if (index < 0 || index >= slots.size()) {
            return SpriteSlot.empty();
        }
        return slots.get(index);
    }

    public TextureRegion getRegion(final int index) {
        return get(index).toRegion();
    }

    public void set(final int index, final SpriteSlot slot, final Texture ownedTexture) {
        ensureCapacity(index + 1);
        if (!slots.get(index).isEmpty()) {
            throw new IllegalStateException("Sprite slot already set at index " + index);
        }
        slots.set(index, slot);
        if (ownedTexture != null) {
            ownedTextures.add(ownedTexture);
        }
    }

    public int append(final SpriteSlot slot, final Texture ownedTexture) {
        final int index = slots.size();
        set(index, slot, ownedTexture);
        return index;
    }

    public void dispose() {
        for (final Texture texture : ownedTextures) {
            texture.dispose();
        }
        ownedTextures.clear();
        slots.clear();
    }
}
