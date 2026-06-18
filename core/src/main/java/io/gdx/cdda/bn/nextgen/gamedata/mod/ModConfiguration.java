package io.gdx.cdda.bn.nextgen.gamedata.mod;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

import io.gdx.cdda.bn.nextgen.DefaultContent;
import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** User-selected mod load order; falls back to BN {@code mods/default.json}. */
public final class ModConfiguration {

    private static final String PREFS_NAME = "io.gdx.cdda.bn.nextgen.mods";
    private static final String KEY_ENABLED_MOD_IDS = "enabled_mod_ids";

    private static boolean testMode;
    private static List<String> testStorage;

    private ModConfiguration() {}

    public static List<String> activeModIds() {
        return activeModIdsForRoots(DataPaths.gameDataRoots());
    }

    public static List<String> activeModIdsForRoots(final List<Path> dataRoots) {
        final List<String> saved = loadSavedModIds();
        if (saved != null) {
            return Collections.unmodifiableList(saved);
        }
        if (dataRoots == null || dataRoots.isEmpty()) {
            return Collections.emptyList();
        }
        return DefaultContent.defaultModIds(dataRoots.get(0));
    }

    public static boolean hasUserSelection() {
        return loadSavedModIds() != null;
    }

    public static void saveEnabledModIds(final List<String> modIds) {
        final List<String> copy = new ArrayList<>();
        if (modIds != null) {
            for (final String modId : modIds) {
                if (modId != null && !modId.isEmpty()) {
                    copy.add(modId);
                }
            }
        }
        if (testMode) {
            testStorage = Collections.unmodifiableList(copy);
            return;
        }
        if (Gdx.app == null) {
            return;
        }
        final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        if (copy.isEmpty()) {
            prefs.remove(KEY_ENABLED_MOD_IDS);
        } else {
            prefs.putString(KEY_ENABLED_MOD_IDS, String.join(",", copy));
        }
        prefs.flush();
    }

    public static void clearUserSelection() {
        if (testMode) {
            testStorage = null;
            return;
        }
        if (Gdx.app == null) {
            return;
        }
        final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.remove(KEY_ENABLED_MOD_IDS);
        prefs.flush();
    }

    static void setTestOverride(final List<String> modIds) {
        testMode = true;
        if (modIds == null) {
            testStorage = null;
            return;
        }
        testStorage = Collections.unmodifiableList(new ArrayList<>(modIds));
    }

    static void clearTestOverride() {
        testMode = false;
        testStorage = null;
    }

    private static List<String> loadSavedModIds() {
        if (testMode) {
            if (testStorage == null) {
                return null;
            }
            return new ArrayList<>(testStorage);
        }
        if (Gdx.app == null) {
            return null;
        }
        final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        if (!prefs.contains(KEY_ENABLED_MOD_IDS)) {
            return null;
        }
        final String raw = prefs.getString(KEY_ENABLED_MOD_IDS, "");
        if (raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> ids = new ArrayList<>();
        for (final String part : raw.split(",")) {
            final String modId = part.trim();
            if (!modId.isEmpty()) {
                ids.add(modId);
            }
        }
        return ids;
    }

    static List<String> parseStoredModIds(final String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> ids = new ArrayList<>();
        for (final String part : raw.split(",")) {
            final String modId = part.trim();
            if (!modId.isEmpty()) {
                ids.add(modId);
            }
        }
        return Collections.unmodifiableList(ids);
    }
}
