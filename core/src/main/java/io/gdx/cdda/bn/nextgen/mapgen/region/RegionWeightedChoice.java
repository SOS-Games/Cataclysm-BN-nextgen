package io.gdx.cdda.bn.nextgen.mapgen.region;

import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** One weighted regional pick entry (P11). */
final class RegionWeightedChoice {

    private final String id;
    private final int weight;

    private RegionWeightedChoice(final String id, final int weight) {
        this.id = id;
        this.weight = weight;
    }

    static List<RegionWeightedChoice> parse(final JsonValue value) {
        final List<RegionWeightedChoice> choices = new ArrayList<>();
        if (value == null || value.isNull()) {
            return choices;
        }
        if (value.isString()) {
            choices.add(new RegionWeightedChoice(value.asString(), 1));
            return choices;
        }
        if (value.isObject()) {
            for (JsonValue member = value.child; member != null; member = member.next) {
                if (member.name == null || member.name.isEmpty()) {
                    continue;
                }
                final int weight = member.isNumber() ? Math.max(1, member.asInt()) : 1;
                choices.add(new RegionWeightedChoice(member.name, weight));
            }
            return choices;
        }
        if (value.isArray()) {
            for (JsonValue child = value.child; child != null; child = child.next) {
                if (child.isString()) {
                    choices.add(new RegionWeightedChoice(child.asString(), 1));
                } else if (child.isArray() && child.size >= 2 && child.get(1).isNumber()) {
                    final JsonValue idNode = child.get(0);
                    if (idNode != null && idNode.isString()) {
                        choices.add(new RegionWeightedChoice(idNode.asString(), Math.max(1, child.getInt(1))));
                    }
                }
            }
        }
        return choices;
    }

    static String pick(
        final List<RegionWeightedChoice> choices,
        final Random rng,
        final Consumer<String> warningSink
    ) {
        if (choices == null || choices.isEmpty()) {
            emitWarning(warningSink, "regional alias has no choices");
            return "";
        }
        if (rng == null) {
            return choices.get(0).id;
        }
        int totalWeight = 0;
        for (final RegionWeightedChoice choice : choices) {
            totalWeight += choice.weight;
        }
        if (totalWeight <= 0) {
            emitWarning(warningSink, "regional alias has zero total weight");
            return choices.get(0).id;
        }
        int roll = rng.nextInt(totalWeight);
        for (final RegionWeightedChoice choice : choices) {
            roll -= choice.weight;
            if (roll < 0) {
                return choice.id;
            }
        }
        return choices.get(choices.size() - 1).id;
    }

    static void emitWarning(final Consumer<String> warningSink, final String message) {
        if (warningSink != null) {
            warningSink.accept(message);
        }
    }
}
