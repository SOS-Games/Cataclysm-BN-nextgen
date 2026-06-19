package io.gdx.cdda.bn.nextgen.mapgen.palette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves palette inheritance chains (P10). */
public final class PaletteResolver {

    private PaletteResolver() {}

    public static PaletteCharMaps resolveWithParents(
        final PaletteRegistry registry,
        final String paletteId,
        final List<String> warnings
    ) {
        if (registry == null || paletteId == null || paletteId.isEmpty()) {
            return new PaletteCharMaps();
        }
        return resolveWithParents(registry, paletteId, new HashSet<>(), warnings);
    }

    private static PaletteCharMaps resolveWithParents(
        final PaletteRegistry registry,
        final String paletteId,
        final Set<String> visiting,
        final List<String> warnings
    ) {
        if (visiting.contains(paletteId)) {
            throw new IllegalStateException("palette inheritance cycle detected: " + paletteId);
        }
        visiting.add(paletteId);

        final PaletteCharMaps merged = new PaletteCharMaps();
        final MapgenPalette palette = registry.find(paletteId).orElse(null);
        if (palette == null) {
            if (warnings != null) {
                warnings.add("unknown palette: " + paletteId);
            }
            visiting.remove(paletteId);
            return merged;
        }

        for (final String parentId : palette.getParentIds()) {
            merged.mergeFrom(resolveWithParents(registry, parentId, visiting, warnings));
        }
        merged.mergeFrom(palette.getLocalCharMaps());
        visiting.remove(paletteId);
        return merged;
    }
}
