package io.gdx.cdda.bn.nextgen.gamedata;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.ModInfoParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/** Scans {@code data/mods/.../modinfo.json} and builds a mod registry (G5 / unit 03). */
public final class ModDiscovery {

    private static final String MODINFO_FILENAME = "modinfo.json";
    private static final Logger LOG = Logger.getLogger(ModDiscovery.class.getName());

    private ModDiscovery() {}

    public static ModRegistry discover(final List<Path> dataRoots) throws IOException {
        final Map<String, List<ModInfo>> candidatesById = new LinkedHashMap<>();
        final List<String> warnings = new ArrayList<>();

        for (final Path dataRoot : dataRoots) {
            final Path modRoot = dataRoot.resolve("mods").normalize();
            if (!Files.isDirectory(modRoot)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(modRoot)) {
                walk.filter(Files::isRegularFile)
                    .filter(path -> MODINFO_FILENAME.equals(path.getFileName().toString()))
                    .forEach(path -> loadModinfoFile(path, candidatesById, warnings));
            }
        }

        final Map<String, ModInfo> accepted = new HashMap<>();
        for (final Map.Entry<String, List<ModInfo>> entry : candidatesById.entrySet()) {
            final List<ModInfo> candidates = entry.getValue();
            if (candidates.size() > 1) {
                final String message = "duplicate mod id '" + entry.getKey() + "' in modinfo files: "
                    + formatPaths(candidates);
                warnings.add(message);
                LOG.log(Level.WARNING, message);
                continue;
            }
            accepted.put(entry.getKey(), candidates.get(0));
        }

        return new ModRegistry(accepted, warnings);
    }

    private static void loadModinfoFile(
        final Path modinfoPath,
        final Map<String, List<ModInfo>> candidatesById,
        final List<String> warnings
    ) {
        try {
            final String text = new String(Files.readAllBytes(modinfoPath), StandardCharsets.UTF_8);
            final JsonValue root = new JsonReader().parse(text);
            if (root.isArray()) {
                for (JsonValue child = root.child; child != null; child = child.next) {
                    addCandidate(modinfoPath, child, candidatesById, warnings);
                }
            } else if (root.isObject()) {
                addCandidate(modinfoPath, root, candidatesById, warnings);
            }
        } catch (final RuntimeException | IOException e) {
            final String message = "failed to parse modinfo: " + modinfoPath + " (" + e.getMessage() + ")";
            warnings.add(message);
            LOG.log(Level.WARNING, message, e);
        }
    }

    private static void addCandidate(
        final Path modinfoPath,
        final JsonValue value,
        final Map<String, List<ModInfo>> candidatesById,
        final List<String> warnings
    ) {
        final ModInfo modInfo = ModInfoParser.parse(value, modinfoPath).orElse(null);
        if (modInfo == null) {
            return;
        }
        candidatesById.computeIfAbsent(modInfo.getId(), ignored -> new ArrayList<>()).add(modInfo);
    }

    private static String formatPaths(final List<ModInfo> mods) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(mods.get(i).getModinfoPath());
        }
        return builder.toString();
    }
}
