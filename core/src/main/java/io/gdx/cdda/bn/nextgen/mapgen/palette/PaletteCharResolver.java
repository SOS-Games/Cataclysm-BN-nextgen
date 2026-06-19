package io.gdx.cdda.bn.nextgen.mapgen.palette;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

/** Resolves palette JSON values to terrain/furniture id strings (P1, P10). */
public final class PaletteCharResolver {

    private PaletteCharResolver() {}

    public static Optional<String> resolveId(final JsonValue value) {
        return resolve(value, null, null);
    }

    public static Optional<String> resolve(
        final JsonValue value,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        if (value == null || value.isNull()) {
            return Optional.empty();
        }
        if (value.isString()) {
            final String id = value.asString();
            return id == null || id.isEmpty() ? Optional.empty() : Optional.of(id);
        }
        if (value.isArray()) {
            return pickFromArray(value, rng, warningSink);
        }
        if (value.isObject()) {
            if (value.has("fallback")) {
                return resolve(value.get("fallback"), rng, warningSink);
            }
            if (value.has("param") && value.has("fallback")) {
                return resolve(value.get("fallback"), rng, warningSink);
            }
            emitWarning(warningSink, "unsupported palette value object");
        }
        return Optional.empty();
    }

    private static Optional<String> pickFromArray(
        final JsonValue array,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        if (array.size == 0) {
            return Optional.empty();
        }
        final List<WeightedChoice> choices = parseChoices(array);
        if (choices.isEmpty()) {
            emitWarning(warningSink, "palette weighted array has no valid entries");
            return Optional.empty();
        }
        if (rng == null) {
            return Optional.of(choices.get(0).id);
        }
        return weightedPick(choices, rng, warningSink);
    }

    private static List<WeightedChoice> parseChoices(final JsonValue array) {
        final List<WeightedChoice> choices = new ArrayList<>();
        for (JsonValue child = array.child; child != null; child = child.next) {
            if (child.isString()) {
                choices.add(new WeightedChoice(child.asString(), 1));
                continue;
            }
            if (!child.isArray()) {
                continue;
            }
            if (child.size >= 2 && child.get(1).isNumber()) {
                final JsonValue idNode = child.get(0);
                final String id = idNode == null ? null : idNode.asString();
                if (id != null && !id.isEmpty()) {
                    choices.add(new WeightedChoice(id, Math.max(1, child.getInt(1))));
                }
                continue;
            }
            for (JsonValue inner = child.child; inner != null; inner = inner.next) {
                if (inner.isString()) {
                    choices.add(new WeightedChoice(inner.asString(), 1));
                }
            }
        }
        return choices;
    }

    private static Optional<String> weightedPick(
        final List<WeightedChoice> choices,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        int totalWeight = 0;
        for (final WeightedChoice choice : choices) {
            totalWeight += choice.weight;
        }
        if (totalWeight <= 0) {
            emitWarning(warningSink, "palette weighted array has zero total weight");
            return Optional.of(choices.get(0).id);
        }
        int roll = rng.nextInt(totalWeight);
        for (final WeightedChoice choice : choices) {
            roll -= choice.weight;
            if (roll < 0) {
                return Optional.of(choice.id);
            }
        }
        return Optional.of(choices.get(choices.size() - 1).id);
    }

    private static void emitWarning(final Consumer<String> warningSink, final String message) {
        if (warningSink != null) {
            warningSink.accept(message);
        }
    }

    private static final class WeightedChoice {
        private final String id;
        private final int weight;

        private WeightedChoice(final String id, final int weight) {
            this.id = id;
            this.weight = weight;
        }
    }
}
