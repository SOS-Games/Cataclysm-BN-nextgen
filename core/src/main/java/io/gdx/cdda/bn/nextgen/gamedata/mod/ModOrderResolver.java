package io.gdx.cdda.bn.nextgen.gamedata.mod;

import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Normalizes mod load order before JSON scan (G5 / unit 09). */
public final class ModOrderResolver {

    public static final String DEFAULT_CORE_MOD_ID = "bn";

    private ModOrderResolver() {}

    public static List<String> resolve(final List<String> userSelection, final ModRegistry registry) {
        final LinkedHashSet<String> deduped = new LinkedHashSet<>();
        if (userSelection != null) {
            for (final String modId : userSelection) {
                if (modId != null && !modId.isEmpty()) {
                    deduped.add(modId);
                }
            }
        }

        final List<String> ordered = new ArrayList<>(deduped);
        String coreModId = null;
        for (final String modId : ordered) {
            final ModInfo modInfo = registry.find(modId).orElse(null);
            if (modInfo != null && modInfo.isCore()) {
                coreModId = modId;
                break;
            }
        }

        if (coreModId != null) {
            ordered.remove(coreModId);
            ordered.add(0, coreModId);
        } else if (!ordered.contains(DEFAULT_CORE_MOD_ID)) {
            ordered.add(0, DEFAULT_CORE_MOD_ID);
        }

        return List.copyOf(ordered);
    }
}
