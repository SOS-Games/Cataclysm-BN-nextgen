package io.gdx.cdda.bn.nextgen.gamedata.parse;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import io.gdx.cdda.bn.nextgen.gamedata.cache.JsonContentDiskCache;
import io.gdx.cdda.bn.nextgen.gamedata.cache.JsonFilePack;
import io.gdx.cdda.bn.nextgen.gamedata.cache.JsonFilePackBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Recursively lists BN JSON files and extracts typed objects from array or object envelopes. */
public final class JsonDataScanner {

    private JsonDataScanner() {}

    public static List<Path> listJsonFiles(final Path jsonRoot, final List<String> subdirs) throws IOException {
        final Optional<JsonFilePack> active = JsonContentDiskCache.activePack();
        if (active.isPresent()) {
            return listFromPack(active.get(), jsonRoot, subdirs);
        }

        if (!Files.isDirectory(jsonRoot)) {
            return Collections.emptyList();
        }

        final List<Path> scanRoots = resolveScanRoots(jsonRoot, subdirs);
        final List<Path> files = new ArrayList<>();
        for (final Path scanRoot : scanRoots) {
            try (Stream<Path> walk = Files.walk(scanRoot)) {
                walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(files::add);
            }
        }
        Collections.sort(files);
        return files;
    }

    public static List<JsonDataObject> parseFile(final Path file) throws IOException {
        final String text = readText(file);
        final JsonValue root = new JsonReader().parse(text);
        return extractObjects(root, file);
    }

    public static List<JsonDataObject> scanFiles(final List<Path> files) throws IOException {
        final List<JsonDataObject> all = new ArrayList<>();
        for (final Path file : files) {
            all.addAll(parseFile(file));
        }
        return all;
    }

    private static String readText(final Path file) throws IOException {
        final Optional<JsonFilePack> active = JsonContentDiskCache.activePack();
        if (active.isPresent()) {
            final Optional<String> cached = active.get().getText(file);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        final String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        final Optional<JsonFilePackBuilder> recording = JsonContentDiskCache.recordingBuilder();
        if (recording.isPresent()) {
            recording.get().put(file, text);
        }
        return text;
    }

    private static List<Path> listFromPack(
        final JsonFilePack pack,
        final Path jsonRoot,
        final List<String> subdirs
    ) {
        if (jsonRoot == null) {
            return Collections.emptyList();
        }
        if (subdirs == null || subdirs.isEmpty()) {
            return pack.listJsonFilesUnder(jsonRoot);
        }
        final List<Path> files = new ArrayList<>();
        for (final String subdir : subdirs) {
            files.addAll(pack.listJsonFilesUnder(jsonRoot.resolve(subdir)));
        }
        Collections.sort(files);
        return files;
    }

    private static List<Path> resolveScanRoots(final Path jsonRoot, final List<String> subdirs) {
        if (subdirs == null || subdirs.isEmpty()) {
            return Collections.singletonList(jsonRoot);
        }
        return subdirs.stream()
            .map(jsonRoot::resolve)
            .filter(Files::isDirectory)
            .collect(Collectors.toList());
    }

    private static List<JsonDataObject> extractObjects(final JsonValue root, final Path file) {
        final List<JsonDataObject> out = new ArrayList<>();
        if (root.isArray()) {
            for (JsonValue child = root.child; child != null; child = child.next) {
                maybeAdd(child, file, out);
            }
        } else if (root.isObject()) {
            maybeAdd(root, file, out);
        }
        return out;
    }

    private static void maybeAdd(
        final JsonValue value,
        final Path file,
        final List<JsonDataObject> out
    ) {
        if (value == null || !value.isObject() || !value.has("type")) {
            return;
        }
        final String type = value.getString("type", null);
        if (type == null || type.isEmpty()) {
            return;
        }
        out.add(new JsonDataObject(type, file, value));
    }
}
