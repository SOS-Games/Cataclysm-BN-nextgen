package io.gdx.cdda.bn.nextgen.mapgen.palette;

import io.gdx.cdda.bn.nextgen.gamedata.ModDiscovery;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModOrderResolver;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataObject;
import io.gdx.cdda.bn.nextgen.gamedata.parse.JsonDataScanner;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenLoadResult;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scans {@code mapgen_palettes/} and loads {@code type: palette} JSON (P1). */
public final class PaletteLoader {

    private static final String PALETTE_TYPE = "palette";

    private PaletteLoader() {}

    public static MapgenLoadResult load(final MapgenScanOptions options) throws IOException {
        final List<String> warnings = new ArrayList<>();
        final PaletteRegistry registry = new PaletteRegistry();
        final ModRegistry modRegistry = ModDiscovery.discover(options.getDataRoots());
        warnings.addAll(modRegistry.getDiscoveryWarnings());

        final List<String> orderedModIds = ModOrderResolver.resolve(options.getModIds(), modRegistry);
        for (final String modId : orderedModIds) {
            final ModInfo modInfo = modRegistry.find(modId).orElse(null);
            if (modInfo == null) {
                continue;
            }
            final Path contentRoot = modInfo.getResolvedContentPath();
            if (options.isIncludePaletteTree()) {
                loadPalettesFromTree(contentRoot.resolve("mapgen_palettes"), registry, warnings);
                loadPalettesFromTree(contentRoot.resolve("overmap_and_mapgen"), registry, warnings);
            }
            if (options.isIncludeInlinePalettes()) {
                loadPalettesFromTree(contentRoot.resolve("mapgen"), registry, warnings);
            }
        }

        return new MapgenLoadResult(registry, warnings);
    }

    private static void loadPalettesFromTree(
        final Path scanRoot,
        final PaletteRegistry registry,
        final List<String> warnings
    ) throws IOException {
        if (!Files.isDirectory(scanRoot)) {
            return;
        }
        final List<Path> files = JsonDataScanner.listJsonFiles(scanRoot, Collections.emptyList());
        for (final Path file : files) {
            loadPalettesFromFile(file, registry, warnings);
        }
    }

    private static void loadPalettesFromFile(
        final Path file,
        final PaletteRegistry registry,
        final List<String> warnings
    ) throws IOException {
        try {
            final List<JsonDataObject> objects = JsonDataScanner.parseFile(file);
            for (final JsonDataObject object : objects) {
                if (!PALETTE_TYPE.equals(object.getType())) {
                    continue;
                }
                final MapgenPalette palette = PaletteParser.parse(object.getRoot()).orElse(null);
                if (palette != null) {
                    registry.put(palette);
                } else {
                    warnings.add("skipped palette without id in " + file);
                }
            }
        } catch (final RuntimeException e) {
            warnings.add("failed to parse palette file " + file + ": " + e.getMessage());
        }
    }
}
