package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.util.Random;

/** Rolls BN {@code jmapgen_int} fields (int or {@code [min,max]} inclusive). */
public final class JmapgenIntRange {

    private JmapgenIntRange() {}

    public static int roll(final JsonValue field, final Random rng) {
        if (field == null || rng == null) {
            return 0;
        }
        if (field.isNumber()) {
            return field.asInt();
        }
        if (!field.isArray() || field.size == 0) {
            return 0;
        }
        int min = field.getInt(0);
        int max = field.size >= 2 ? field.getInt(1) : min;
        if (min > max) {
            final int swap = min;
            min = max;
            max = swap;
        }
        if (min == max) {
            return min;
        }
        return min + rng.nextInt(max - min + 1);
    }

    public static int rollOptional(
        final JsonValue object,
        final String key,
        final int defaultValue,
        final Random rng
    ) {
        if (object == null || !object.has(key)) {
            return defaultValue;
        }
        return roll(object.get(key), rng);
    }
}
