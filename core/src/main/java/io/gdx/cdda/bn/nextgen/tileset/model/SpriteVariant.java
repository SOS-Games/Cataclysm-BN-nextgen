package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** One weighted sprite variant (rotation frames + pick weight). */
public final class SpriteVariant {

    private final List<Integer> frames;
    private final int weight;

    public SpriteVariant(final List<Integer> frames, final int weight) {
        this.frames = Collections.unmodifiableList(new ArrayList<>(frames));
        this.weight = weight;
    }

    public List<Integer> getFrames() {
        return frames;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public int getFrame(final int rotationIndex) {
        if (frames.isEmpty()) {
            return -1;
        }
        return frames.get(rotationIndex % frames.size());
    }
}
