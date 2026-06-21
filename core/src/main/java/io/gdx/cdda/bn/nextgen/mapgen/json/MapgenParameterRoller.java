package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

/** Rolls mapgen {@code parameters} at run time (v2.1a). */
public final class MapgenParameterRoller {

    private MapgenParameterRoller() {}

    public static Map<String, String> roll(
        final JsonValue parametersNode,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        if (parametersNode == null || !parametersNode.isObject()) {
            return Collections.emptyMap();
        }
        final Map<String, String> rolled = new HashMap<>();
        for (JsonValue child = parametersNode.child; child != null; child = child.next) {
            if (child.name == null || child.name.isEmpty()) {
                continue;
            }
            final String parameterName = child.name;
            resolveParameter(child, rng, warningSink).ifPresent(value -> rolled.put(parameterName, value));
        }
        return Collections.unmodifiableMap(rolled);
    }

    private static Optional<String> resolveParameter(
        final JsonValue parameterNode,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        if (parameterNode == null || !parameterNode.isObject()) {
            return Optional.empty();
        }
        final String type = parameterNode.getString("type", "");
        final JsonValue defaultNode = parameterNode.get("default");
        if (defaultNode == null) {
            emitWarning(warningSink, "parameter missing default: " + parameterNode.name);
            return Optional.empty();
        }
        if ("int".equals(type)) {
            return rollIntDefault(defaultNode, rng, warningSink, parameterNode.name);
        }
        if ("string".equals(type) || "ter_str_id".equals(type) || type.isEmpty()) {
            return rollStringDefault(defaultNode, rng, warningSink, parameterNode.name);
        }
        emitWarning(warningSink, "unsupported parameter type '" + type + "' for " + parameterNode.name);
        return rollStringDefault(defaultNode, rng, warningSink, parameterNode.name);
    }

    private static Optional<String> rollStringDefault(
        final JsonValue defaultNode,
        final Random rng,
        final Consumer<String> warningSink,
        final String parameterName
    ) {
        if (defaultNode.isString()) {
            final String value = defaultNode.asString();
            return value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
        }
        if (defaultNode.isObject() && defaultNode.has("distribution")) {
            return pickFromDistribution(defaultNode.get("distribution"), rng, warningSink, parameterName);
        }
        if (defaultNode.isArray()) {
            return pickFromDistribution(defaultNode, rng, warningSink, parameterName);
        }
        emitWarning(warningSink, "unsupported string parameter default for " + parameterName);
        return Optional.empty();
    }

    private static Optional<String> rollIntDefault(
        final JsonValue defaultNode,
        final Random rng,
        final Consumer<String> warningSink,
        final String parameterName
    ) {
        if (defaultNode.isNumber()) {
            return Optional.of(Integer.toString(defaultNode.asInt()));
        }
        if (defaultNode.isObject() && defaultNode.has("distribution")) {
            return pickFromDistribution(defaultNode.get("distribution"), rng, warningSink, parameterName)
                .map(value -> {
                    try {
                        return Integer.toString(Integer.parseInt(value));
                    } catch (final NumberFormatException e) {
                        return value;
                    }
                });
        }
        emitWarning(warningSink, "unsupported int parameter default for " + parameterName);
        return Optional.empty();
    }

    private static Optional<String> pickFromDistribution(
        final JsonValue distribution,
        final Random rng,
        final Consumer<String> warningSink,
        final String parameterName
    ) {
        if (distribution == null || !distribution.isArray() || distribution.size == 0) {
            emitWarning(warningSink, "empty parameter distribution for " + parameterName);
            return Optional.empty();
        }
        final java.util.List<WeightedChoice> choices = new java.util.ArrayList<>();
        for (JsonValue child = distribution.child; child != null; child = child.next) {
            if (child.isArray() && child.size >= 2 && child.get(1).isNumber()) {
                final JsonValue valueNode = child.get(0);
                final String value = valueNode == null ? null : valueNode.asString();
                if (value != null && !value.isEmpty()) {
                    choices.add(new WeightedChoice(value, Math.max(1, child.getInt(1))));
                }
                continue;
            }
            if (child.isString()) {
                choices.add(new WeightedChoice(child.asString(), 1));
            }
        }
        if (choices.isEmpty()) {
            emitWarning(warningSink, "parameter distribution has no valid entries for " + parameterName);
            return Optional.empty();
        }
        if (rng == null) {
            return Optional.of(choices.get(0).value);
        }
        int totalWeight = 0;
        for (final WeightedChoice choice : choices) {
            totalWeight += choice.weight;
        }
        int roll = rng.nextInt(totalWeight);
        for (final WeightedChoice choice : choices) {
            roll -= choice.weight;
            if (roll < 0) {
                return Optional.of(choice.value);
            }
        }
        return Optional.of(choices.get(choices.size() - 1).value);
    }

    private static void emitWarning(final Consumer<String> warningSink, final String message) {
        if (warningSink != null) {
            warningSink.accept(message);
        }
    }

    private static final class WeightedChoice {
        private final String value;
        private final int weight;

        private WeightedChoice(final String value, final int weight) {
            this.value = value;
            this.weight = weight;
        }
    }
}
