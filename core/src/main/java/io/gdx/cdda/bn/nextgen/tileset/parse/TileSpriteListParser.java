package io.gdx.cdda.bn.nextgen.tileset.parse;

import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.tileset.model.WeightedSpriteList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Parses {@code fg}/{@code bg} JSON into weighted sprite lists (unit 07a). */
public final class TileSpriteListParser {

    private TileSpriteListParser() {}

    public static void parseInto(
        final JsonValue entry,
        final WeightedSpriteList target,
        final String fieldName,
        final int spriteIdOffset
    ) throws IOException {
        if (!entry.has(fieldName)) {
            return;
        }
        final JsonValue value = entry.get(fieldName);
        if (value == null || value.isNull()) {
            return;
        }
        if (value.isArray()) {
            parseArray(value, target, fieldName, spriteIdOffset);
            return;
        }
        if (value.isNumber()) {
            final int jsonIndex = value.asInt();
            if (jsonIndex >= 0) {
                final List<Integer> frames = new ArrayList<>(1);
                frames.add(jsonIndex + spriteIdOffset);
                target.add(frames, 1);
            }
        }
    }

    private static void parseArray(
        final JsonValue array,
        final WeightedSpriteList target,
        final String fieldName,
        final int spriteIdOffset
    ) throws IOException {
        if (array.size == 0) {
            return;
        }
        final JsonValue first = array.child;
        if (first.isNumber()) {
            final List<Integer> frames = new ArrayList<>();
            for (JsonValue child = array.child; child != null; child = child.next) {
                final int spriteId = child.asInt() + spriteIdOffset;
                if (spriteId >= 0) {
                    frames.add(spriteId);
                }
            }
            target.add(frames, 1);
            return;
        }
        if (first.isObject()) {
            for (JsonValue child = array.child; child != null; child = child.next) {
                parseWeightedVariant(child, target, fieldName, spriteIdOffset);
            }
        }
    }

    private static void parseWeightedVariant(
        final JsonValue variantObject,
        final WeightedSpriteList target,
        final String fieldName,
        final int spriteIdOffset
    ) throws IOException {
        final int weight = variantObject.getInt("weight", 0);
        if (weight < 0) {
            throw new IOException("Invalid weight for sprite variation (<0) in \"" + fieldName + "\"");
        }
        final List<Integer> frames = new ArrayList<>();
        if (variantObject.has("sprite")) {
            final JsonValue sprite = variantObject.get("sprite");
            if (sprite.isNumber()) {
                final int spriteId = sprite.asInt() + spriteIdOffset;
                if (spriteId >= 0) {
                    frames.add(spriteId);
                }
            } else if (sprite.isArray()) {
                for (JsonValue child = sprite.child; child != null; child = child.next) {
                    final int spriteId = child.asInt() + spriteIdOffset;
                    if (spriteId >= 0) {
                        frames.add(spriteId);
                    }
                }
            }
        }
        final int frameCount = frames.size();
        if (frameCount != 1 && frameCount != 2 && frameCount != 4) {
            throw new IOException("Invalid number of sprites (not 1, 2, or 4) in \"" + fieldName + "\"");
        }
        target.add(frames, weight);
    }
}
