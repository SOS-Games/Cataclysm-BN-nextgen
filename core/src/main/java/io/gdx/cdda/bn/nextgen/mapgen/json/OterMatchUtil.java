package io.gdx.cdda.bn.nextgen.mapgen.json;

import java.util.Locale;

/** BN {@code ot_match_type::contains} helper for nested neighbor checks. */
public final class OterMatchUtil {

    private OterMatchUtil() {}

    public static boolean matchesContains(final String pattern, final String actualOterId) {
        if (pattern == null || pattern.isEmpty() || actualOterId == null || actualOterId.isEmpty()) {
            return false;
        }
        return actualOterId.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT));
    }
}
