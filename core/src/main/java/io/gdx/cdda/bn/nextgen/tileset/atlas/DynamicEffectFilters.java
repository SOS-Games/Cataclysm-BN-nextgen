package io.gdx.cdda.bn.nextgen.tileset.atlas;

import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;

/** Resolves lazy composite filters for the dynamic atlas path (A1). */
public final class DynamicEffectFilters {

    private DynamicEffectFilters() {}

    public static ColorPixelFilter filterFor(
        final TilesetFxType fxType,
        final TilesetLoadOptions options
    ) {
        switch (fxType) {
            case SHADOW:
                return ColorPixelFilters.grayscale();
            case NIGHT:
            case ENHANCED_NIGHT:
                return ColorPixelFilters.nightVision();
            case OVEREXPOSED:
            case ENHANCED_OVEREXPOSED:
                return ColorPixelFilters.overexposed();
            case UNDERWATER:
                return ColorPixelFilters.underwater();
            case UNDERWATER_DARK:
                return ColorPixelFilters.underwaterDark();
            case MEMORY:
                return options.getMemoryMapMode() == TilesetLoadOptions.MemoryMapMode.DARKEN
                    ? ColorPixelFilters.darken()
                    : ColorPixelFilters.sepia();
            case Z_OVERLAY:
                return ColorPixelFilters.zOverlay(options.isStaticZEffect());
            case NONE:
            default:
                return null;
        }
    }
}
