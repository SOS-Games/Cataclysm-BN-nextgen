package io.gdx.cdda.bn.nextgen.tileset.atlas;

import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.model.SpriteTextureTable;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;

/** Maps baked tables to BN color filters (unit 06c). */
public final class FilteredTableBake {

    public static final class Entry {
        private final SpriteTextureTable table;
        private final ColorPixelFilter filter;

        public Entry(final SpriteTextureTable table, final ColorPixelFilter filter) {
            this.table = table;
            this.filter = filter;
        }

        public SpriteTextureTable getTable() {
            return table;
        }

        public ColorPixelFilter getFilter() {
            return filter;
        }
    }

    private FilteredTableBake() {}

    public static Entry[] entriesFor(
        final io.gdx.cdda.bn.nextgen.tileset.model.SpriteTextureTables tables,
        final TilesetLoadOptions options
    ) {
        final ColorPixelFilter memoryFilter = options.getMemoryMapMode() == TilesetLoadOptions.MemoryMapMode.DARKEN
            ? ColorPixelFilters.darken()
            : ColorPixelFilters.sepia();
        return new Entry[] {
            new Entry(tables.getTable(TilesetFxType.NONE), null),
            new Entry(tables.getTable(TilesetFxType.SHADOW), ColorPixelFilters.grayscale()),
            new Entry(tables.getTable(TilesetFxType.NIGHT), ColorPixelFilters.nightVision()),
            new Entry(tables.getTable(TilesetFxType.OVEREXPOSED), ColorPixelFilters.overexposed()),
            new Entry(tables.getTable(TilesetFxType.UNDERWATER), ColorPixelFilters.underwater()),
            new Entry(tables.getTable(TilesetFxType.UNDERWATER_DARK), ColorPixelFilters.underwaterDark()),
            new Entry(tables.getTable(TilesetFxType.Z_OVERLAY), ColorPixelFilters.zOverlay(options.isStaticZEffect())),
            new Entry(tables.getTable(TilesetFxType.MEMORY), memoryFilter),
        };
    }
}
