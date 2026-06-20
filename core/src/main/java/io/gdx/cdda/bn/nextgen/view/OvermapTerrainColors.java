package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.Color;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** BN overmap color names and hash fallback for unknown OMT ids (R3). */
public final class OvermapTerrainColors {

    private static final Map<String, Color> NAMED = new HashMap<>();

    static {
        NAMED.put("white", Color.WHITE);
        NAMED.put("black", Color.BLACK);
        NAMED.put("red", Color.RED);
        NAMED.put("green", Color.GREEN);
        NAMED.put("blue", Color.BLUE);
        NAMED.put("cyan", Color.CYAN);
        NAMED.put("yellow", Color.YELLOW);
        NAMED.put("magenta", Color.MAGENTA);
        NAMED.put("brown", new Color(0.55f, 0.35f, 0.18f, 1f));
        NAMED.put("light_green", new Color(0.55f, 0.85f, 0.35f, 1f));
        NAMED.put("dark_gray", new Color(0.28f, 0.28f, 0.30f, 1f));
        NAMED.put("light_gray", new Color(0.65f, 0.65f, 0.68f, 1f));
        NAMED.put("dark_green", new Color(0.15f, 0.45f, 0.18f, 1f));
        NAMED.put("i_cyan", new Color(0.35f, 0.85f, 0.85f, 1f));
        NAMED.put("i_red", new Color(0.95f, 0.35f, 0.35f, 1f));
    }

    private OvermapTerrainColors() {}

    public static Color resolve(final String colorName, final String omtId) {
        if (colorName != null && !colorName.isEmpty()) {
            final Color named = NAMED.get(colorName.toLowerCase(Locale.ROOT));
            if (named != null) {
                return named;
            }
        }
        return hashColor(omtId == null ? "" : omtId);
    }

    public static Color hashColor(final String omtId) {
        final int hash = omtId.hashCode();
        final float r = ((hash & 0xFF0000) >> 16) / 255f * 0.7f + 0.2f;
        final float g = ((hash & 0x00FF00) >> 8) / 255f * 0.7f + 0.2f;
        final float b = (hash & 0x0000FF) / 255f * 0.7f + 0.2f;
        return new Color(r, g, b, 1f);
    }
}
