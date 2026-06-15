package io.gdx.cdda.bn.nextgen.tileset.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Weighted list of sprite variants with optional precalc pick table (unit 07a). */
public final class WeightedSpriteList {

    private final List<SpriteVariant> variants = new ArrayList<>();
    private final List<Integer> precalc = new ArrayList<>();
    private int totalWeight;

    public boolean isEmpty() {
        return variants.isEmpty();
    }

    public List<SpriteVariant> getVariants() {
        return Collections.unmodifiableList(variants);
    }

    public int getTotalWeight() {
        return totalWeight;
    }

    public void add(final List<Integer> frames, final int weight) {
        if (weight < 0) {
            return;
        }
        variants.add(new SpriteVariant(frames, weight));
        totalWeight += weight;
        precalc.clear();
    }

    public void clear() {
        variants.clear();
        totalWeight = 0;
        precalc.clear();
    }

    public void precalc() {
        precalc.clear();
        if (totalWeight <= 0) {
            return;
        }
        for (int i = 0; i < variants.size(); i++) {
            final int weight = variants.get(i).getWeight();
            for (int w = 0; w < weight; w++) {
                precalc.add(i);
            }
        }
    }

    public SpriteVariant pick(final Random random) {
        if (variants.isEmpty()) {
            return null;
        }
        if (!precalc.isEmpty()) {
            final int index = precalc.get(random.nextInt(precalc.size()));
            return variants.get(index);
        }
        if (totalWeight <= 0) {
            return variants.get(0);
        }
        int roll = random.nextInt(totalWeight);
        for (final SpriteVariant variant : variants) {
            roll -= variant.getWeight();
            if (roll < 0) {
                return variant;
            }
        }
        return variants.get(variants.size() - 1);
    }

    /** BN {@code weighted_int_list::pick(unsigned int)} — {@code picked = (randi % totalWeight) + 1}. */
    public SpriteVariant pickByIndex(final int randi) {
        if (variants.isEmpty() || totalWeight <= 0) {
            return null;
        }
        if (variants.size() == 1) {
            return variants.get(0);
        }
        final int picked = Math.floorMod(randi, totalWeight) + 1;
        if (!precalc.isEmpty()) {
            return variants.get(precalc.get(picked - 1));
        }
        int accumulated = 0;
        for (int i = 0; i < variants.size(); i++) {
            accumulated += variants.get(i).getWeight();
            if (accumulated >= picked) {
                return variants.get(i);
            }
        }
        return variants.get(variants.size() - 1);
    }

    public int getFirstSpriteIndex() {
        if (variants.isEmpty()) {
            return -1;
        }
        final List<Integer> frames = variants.get(0).getFrames();
        if (frames.isEmpty()) {
            return -1;
        }
        return frames.get(0);
    }
}
