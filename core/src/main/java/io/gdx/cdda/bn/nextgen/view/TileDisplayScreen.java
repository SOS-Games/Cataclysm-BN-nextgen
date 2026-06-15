package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.utils.ScreenUtils;

import io.gdx.cdda.bn.nextgen.tileset.GfxPaths;
import io.gdx.cdda.bn.nextgen.tileset.TilesetDiscovery;
import io.gdx.cdda.bn.nextgen.tileset.TilesetOption;
import io.gdx.cdda.bn.nextgen.tileset.TilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadSession;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Grid browser for loaded tileset sprites (in-game reference viewer). */
public final class TileDisplayScreen {

    private static final String[] PREFERRED_TILE_IDS = {
        "t_dirt",
        "t_grass",
        "t_water_sh",
        "t_floor",
        "t_wall",
        "t_tree",
        "t_shrub",
        "t_rock",
        "t_lava",
        "t_sidewalk",
        "t_door_c",
        "t_window",
        "t_fence",
        "monster",
        "player",
        "unknown",
        "highlight_item"
    };

    private static final String[] PREFERRED_TILESET_IDS = { "hoder", "retrodays", "UltimateCataclysm" };

    private static final TilesetFxType[] FX_CYCLE = {
        TilesetFxType.NONE,
        TilesetFxType.SHADOW,
        TilesetFxType.NIGHT,
        TilesetFxType.OVEREXPOSED,
        TilesetFxType.UNDERWATER,
        TilesetFxType.UNDERWATER_DARK,
        TilesetFxType.MEMORY,
        TilesetFxType.Z_OVERLAY
    };

    private static final int MARGIN = 8;
    private static final int HUD_HEIGHT = 56;
    private static final int CELL_GUTTER = 4;
    private static final int CELL_PADDING = 4;
    private static final int FONT_EXTRA_PIXELS = 2;

    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final LoadingSpinner loadingSpinner = new LoadingSpinner();

    private TilesetLoadSession loadSession;

    private LoadedTileset tileset;
    private TilesetRegistry registry;
    private List<String> tilesetIds = Collections.emptyList();
    private int tilesetIndex;
    private List<String> previewTileIds = Collections.emptyList();
    private String statusMessage = "Loading…";
    private boolean loadingTileset;
    private String loadingTilesetId;
    private TilesetFxType fxType = TilesetFxType.NONE;
    private int page;
    private int tilesPerPage = 1;
    private int zoom = 2;
    private int gridColumns = 1;
    private int gridRows = 1;
    private int cellWidth;
    private int cellHeight;
    private int gridStartX;
    private int gridStartY;
    private int labelLineHeight;
    private int labelColumnWidth;
    private int lastViewportWidth;
    private int lastViewportHeight;

    public TileDisplayScreen(final SpriteBatch batch) {
        this.batch = batch;
        this.font = new BitmapFont();
        final float baseLineHeight = font.getLineHeight();
        font.getData().setScale((baseLineHeight + FONT_EXTRA_PIXELS) / baseLineHeight);
        font.setUseIntegerPositions(true);
        labelLineHeight = Math.round(font.getLineHeight());
        labelColumnWidth = measurePreferredLabelWidth();
    }

    public void loadFromRegistry(final TilesetRegistry newRegistry) {
        final String preserveId = currentTilesetId();
        registry = newRegistry;
        tilesetIds = buildTilesetIds(newRegistry);
        disposeTileset();
        previewTileIds = Collections.emptyList();
        page = 0;

        if (tilesetIds.isEmpty()) {
            cancelLoadSession();
            loadingTileset = false;
            statusMessage = "No gfx roots. Set -Dcdda.gfx.roots=… or run from repo with ../Cataclysm-BN/gfx";
            Gdx.app.log("tileset", statusMessage);
            return;
        }

        int index = indexOfTileset(preserveId);
        if (index < 0) {
            index = indexOfTileset(resolvePreferredTilesetId(newRegistry));
        }
        loadTilesetAtIndex(index);
    }

    public void render() {
        advanceLoadSession();
        ensureGridFresh();
        ScreenUtils.clear(0.12f, 0.12f, 0.16f, 1f);
        applyBatchProjection();
        batch.begin();
        drawHud();
        if (loadingTileset) {
            loadingSpinner.update(Gdx.graphics.getDeltaTime());
            drawLoadingOverlay();
        } else if (tileset != null && !previewTileIds.isEmpty()) {
            drawGrid();
        }
        batch.end();
    }

    public boolean onKeyDown(final int keycode) {
        if (loadingTileset) {
            if (keycode == Keys.LEFT_BRACKET || keycode == Keys.RIGHT_BRACKET) {
                if (keycode == Keys.LEFT_BRACKET) {
                    requestTilesetAtIndex((tilesetIndex - 1 + tilesetIds.size()) % tilesetIds.size());
                } else {
                    requestTilesetAtIndex((tilesetIndex + 1) % tilesetIds.size());
                }
                return true;
            }
            return false;
        }
        if (keycode == Keys.LEFT || keycode == Keys.A) {
            previousPage();
            return true;
        }
        if (keycode == Keys.RIGHT || keycode == Keys.D) {
            nextPage();
            return true;
        }
        if (keycode == Keys.F) {
            cycleFx();
            return true;
        }
        if (keycode >= Keys.NUM_1 && keycode <= Keys.NUM_8) {
            fxType = FX_CYCLE[keycode - Keys.NUM_1];
            return true;
        }
        if (keycode == Keys.EQUALS || keycode == Keys.PLUS) {
            zoom = Math.min(zoom + 1, 6);
            recomputeCellMetrics();
            recomputeLayoutForViewport();
            return true;
        }
        if (keycode == Keys.MINUS) {
            zoom = Math.max(zoom - 1, 1);
            recomputeCellMetrics();
            recomputeLayoutForViewport();
            return true;
        }
        if (keycode == Keys.R) {
            loadFromRegistry(TilesetDiscovery.build());
            return true;
        }
        if (keycode == Keys.LEFT_BRACKET) {
            previousTileset();
            return true;
        }
        if (keycode == Keys.RIGHT_BRACKET) {
            nextTileset();
            return true;
        }
        return false;
    }

    public boolean onScroll(final float amountY) {
        if (loadingTileset) {
            return true;
        }
        if (amountY > 0f) {
            nextPage();
        } else if (amountY < 0f) {
            previousPage();
        }
        return true;
    }

    public void resize(final int width, final int height) {
        refreshViewportLayout();
    }

    public void dispose() {
        cancelLoadSession();
        disposeTileset();
        loadingSpinner.dispose();
        font.dispose();
    }

    private void ensureGridFresh() {
        refreshViewportLayout();
    }

    private void refreshViewportLayout() {
        if (tileset == null) {
            return;
        }
        final int width = viewportPixelWidth();
        final int height = viewportPixelHeight();
        if (width != lastViewportWidth || height != lastViewportHeight) {
            lastViewportWidth = width;
            lastViewportHeight = height;
            recomputeGridLayout(width, height);
        }
    }

    private void recomputeLayoutForViewport() {
        final int width = viewportPixelWidth();
        final int height = viewportPixelHeight();
        lastViewportWidth = width;
        lastViewportHeight = height;
        recomputeGridLayout(width, height);
    }

    private static int viewportPixelWidth() {
        return Gdx.graphics.getBackBufferWidth();
    }

    private static int viewportPixelHeight() {
        return Gdx.graphics.getBackBufferHeight();
    }

    private void applyBatchProjection() {
        batch.getProjectionMatrix().setToOrtho2D(
            0f,
            0f,
            viewportPixelWidth(),
            viewportPixelHeight()
        );
    }

    private void drawHud() {
        final int pageCount = Math.max(1, pageCount());
        final String tilesetLine = formatTilesetPickerLine();
        final String hud = statusMessage
            + "  |  FX: " + fxType.name()
            + "  |  page " + (page + 1) + "/" + pageCount
            + "  |  zoom " + zoom + "x";
        final String controls = loadingTileset
            ? "Loading…  [[ ]] switch tileset"
            : "[←/→] page  [[ ]] tileset  [F] FX  [1-8] FX  [+/-] zoom  [R] reload";
        font.draw(batch, tilesetLine, MARGIN, screenHeight() - 8);
        font.draw(batch, hud, MARGIN, screenHeight() - 24);
        font.draw(batch, "Gfx: " + GfxPaths.gameGfxRoots() + "  |  " + controls, MARGIN, screenHeight() - 40);
    }

    private void drawLoadingOverlay() {
        final int centerX = viewportPixelWidth() / 2;
        final int centerY = viewportPixelHeight() / 2;
        loadingSpinner.draw(batch, centerX, centerY);
        final String label = loadSession != null ? loadSession.getProgressLabel() : "Loading…";
        glyphLayout.setText(font, label);
        font.draw(
            batch,
            label,
            centerX - Math.round(glyphLayout.width) / 2,
            centerY - 48
        );
    }

    private void advanceLoadSession() {
        if (loadSession == null || !loadSession.isActive()) {
            return;
        }
        loadSession.step();
        if (loadSession.isComplete()) {
            applyLoadedTileset(loadSession.getResult());
            loadSession = null;
        } else if (loadSession.isFailed()) {
            loadingTileset = false;
            statusMessage = loadSession.getErrorMessage();
            Gdx.app.error("tileset", statusMessage);
            loadSession = null;
        } else if (loadSession.isCancelled()) {
            loadSession = null;
        }
    }

    private void cancelLoadSession() {
        if (loadSession != null) {
            loadSession.cancel();
            loadSession = null;
        }
    }

    private static int screenHeight() {
        return viewportPixelHeight();
    }

    private void drawGrid() {
        final int tileScale = effectiveTileScale();
        final int start = page * tilesPerPage;
        final int end = Math.min(start + tilesPerPage, previewTileIds.size());

        for (int index = start; index < end; index++) {
            final int local = index - start;
            final int col = local % gridColumns;
            final int row = local / gridColumns;
            final int cellX = gridStartX + col * (cellWidth + CELL_GUTTER);
            final int cellTop = gridStartY - row * (cellHeight + CELL_GUTTER);
            drawCell(previewTileIds.get(index), cellX, cellTop, tileScale);
        }
    }

    private void drawCell(final String tileId, final int cellX, final int cellTop, final int tileScale) {
        final int cellBottom = cellTop - cellHeight;
        final String label = fitLabel(tileId, cellWidth - CELL_PADDING * 2);
        glyphLayout.setText(font, label);
        final int labelX = cellX + (cellWidth - Math.round(glyphLayout.width)) / 2;
        final int labelY = cellBottom + CELL_PADDING + labelLineHeight;
        font.draw(batch, label, labelX, labelY);

        final int spriteCenterX = cellX + cellWidth / 2;
        final int spriteAreaBottom = labelY + CELL_PADDING;
        final int spriteAreaTop = cellTop - CELL_PADDING;
        final int spriteAnchorY = spriteAreaBottom + (spriteAreaTop - spriteAreaBottom) / 2;

        final TileDefinition tile = tileset.findTile(tileId).orElse(null);
        if (tile == null) {
            return;
        }

        drawLayer(tile.getBackgroundSpriteIndex(), tile, tileScale, spriteCenterX, spriteAnchorY);
        drawLayer(tile.getForegroundSpriteIndex(), tile, tileScale, spriteCenterX, spriteAnchorY);
    }

    private void drawLayer(
        final int spriteIndex,
        final TileDefinition tile,
        final int tileScale,
        final int centerX,
        final int centerY
    ) {
        if (spriteIndex < 0) {
            return;
        }
        final TextureRegion region = tileset.getTexture(spriteIndex, fxType);
        if (region == null) {
            return;
        }
        final int width = region.getRegionWidth() * tileScale;
        final int height = region.getRegionHeight() * tileScale;
        final int offsetX = Math.round(tile.getOffsetX() * tileScale);
        final int offsetY = Math.round(tile.getOffsetY() * tileScale);
        final int drawX = centerX - width / 2 + offsetX;
        final int drawY = centerY - height / 2 + offsetY;
        batch.draw(region, drawX, drawY, width, height);
    }

    private void recomputeCellMetrics() {
        if (tileset == null) {
            return;
        }

        final int tileScale = effectiveTileScale();
        final int spriteSlotW = maxSpriteDrawWidth(tileScale);
        final int spriteSlotH = maxSpriteDrawHeight(tileScale);

        cellWidth = Math.max(spriteSlotW, labelColumnWidth) + CELL_PADDING * 2;
        cellHeight = spriteSlotH + labelLineHeight + CELL_PADDING * 3;
    }

    private void recomputeGridLayout(final int viewportWidth, final int viewportHeight) {
        if (tileset == null) {
            return;
        }

        final int availableWidth = viewportWidth - MARGIN * 2;
        final int availableHeight = viewportHeight - HUD_HEIGHT - MARGIN;
        gridColumns = fitAxisCount(availableWidth, cellWidth);
        gridRows = fitAxisCount(availableHeight, cellHeight);
        tilesPerPage = gridColumns * gridRows;

        gridStartX = MARGIN;
        gridStartY = viewportHeight - HUD_HEIGHT - MARGIN;

        final int maxPage = pageCount() - 1;
        if (page > maxPage) {
            page = maxPage;
        }
    }

    private int effectiveTileScale() {
        final int pixelScale = Math.max(1, Math.round(tileset.getTileInfo().getPixelScale()));
        return Math.max(1, zoom * pixelScale);
    }

    private int maxSpriteDrawWidth(final int tileScale) {
        int max = tileset.getTileInfo().getWidth() * tileScale;
        for (final String tileId : layoutSampleTileIds()) {
            max = Math.max(max, spriteDrawWidth(tileId, tileScale));
        }
        return max;
    }

    private int maxSpriteDrawHeight(final int tileScale) {
        int max = tileset.getTileInfo().getHeight() * tileScale;
        for (final String tileId : layoutSampleTileIds()) {
            max = Math.max(max, spriteDrawHeight(tileId, tileScale));
        }
        return max;
    }

    private List<String> layoutSampleTileIds() {
        final List<String> sample = new ArrayList<>();
        for (final String tileId : PREFERRED_TILE_IDS) {
            if (tileset.findTile(tileId).isPresent()) {
                sample.add(tileId);
            }
        }
        if (sample.isEmpty() && !previewTileIds.isEmpty()) {
            final int end = Math.min(8, previewTileIds.size());
            sample.addAll(previewTileIds.subList(0, end));
        }
        return sample;
    }

    private int spriteDrawWidth(final String tileId, final int tileScale) {
        final TileDefinition tile = tileset.findTile(tileId).orElse(null);
        if (tile == null) {
            return 0;
        }
        int max = 0;
        max = Math.max(max, layerDrawWidth(tile.getBackgroundSpriteIndex(), tile, tileScale));
        max = Math.max(max, layerDrawWidth(tile.getForegroundSpriteIndex(), tile, tileScale));
        return max;
    }

    private int spriteDrawHeight(final String tileId, final int tileScale) {
        final TileDefinition tile = tileset.findTile(tileId).orElse(null);
        if (tile == null) {
            return 0;
        }
        int max = 0;
        max = Math.max(max, layerDrawHeight(tile.getBackgroundSpriteIndex(), tile, tileScale));
        max = Math.max(max, layerDrawHeight(tile.getForegroundSpriteIndex(), tile, tileScale));
        return max;
    }

    private int layerDrawWidth(final int spriteIndex, final TileDefinition tile, final int tileScale) {
        if (spriteIndex < 0) {
            return 0;
        }
        final TextureRegion region = tileset.getTexture(spriteIndex, fxType);
        if (region == null) {
            return 0;
        }
        return region.getRegionWidth() * tileScale + Math.abs(Math.round(tile.getOffsetX() * tileScale));
    }

    private int layerDrawHeight(final int spriteIndex, final TileDefinition tile, final int tileScale) {
        if (spriteIndex < 0) {
            return 0;
        }
        final TextureRegion region = tileset.getTexture(spriteIndex, fxType);
        if (region == null) {
            return 0;
        }
        return region.getRegionHeight() * tileScale + Math.abs(Math.round(tile.getOffsetY() * tileScale));
    }

    /** Largest count that fully fits without clipping (strict). */
    private static int fitAxisCount(final int available, final int cellSize) {
        if (available <= 0 || cellSize <= 0) {
            return 1;
        }
        int count = 1;
        while (gridSpan(count + 1, cellSize) <= available) {
            count++;
        }
        return count;
    }

    private static int gridSpan(final int count, final int cellSize) {
        if (count <= 0) {
            return 0;
        }
        return count * cellSize + (count - 1) * CELL_GUTTER;
    }

    private int measurePreferredLabelWidth() {
        float max = 0f;
        for (final String tileId : PREFERRED_TILE_IDS) {
            glyphLayout.setText(font, tileId);
            max = Math.max(max, glyphLayout.width);
        }
        return Math.max(Math.round(max), Math.round(glyphLayout.width));
    }

    private String fitLabel(final String text, final int maxWidth) {
        glyphLayout.setText(font, text);
        if (glyphLayout.width <= maxWidth) {
            return text;
        }
        final String ellipsis = "...";
        for (int length = text.length() - 1; length > 0; length--) {
            final String candidate = text.substring(0, length) + ellipsis;
            glyphLayout.setText(font, candidate);
            if (glyphLayout.width <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
    }

    private int pageCount() {
        if (previewTileIds.isEmpty() || tilesPerPage <= 0) {
            return 1;
        }
        return (previewTileIds.size() + tilesPerPage - 1) / tilesPerPage;
    }

    private void previousPage() {
        if (page > 0) {
            page--;
        }
    }

    private void nextPage() {
        if (page < pageCount() - 1) {
            page++;
        }
    }

    private void cycleFx() {
        int index = 0;
        for (int i = 0; i < FX_CYCLE.length; i++) {
            if (FX_CYCLE[i] == fxType) {
                index = i;
                break;
            }
        }
        fxType = FX_CYCLE[(index + 1) % FX_CYCLE.length];
    }

    private void nextTileset() {
        if (tilesetIds.size() <= 1) {
            return;
        }
        requestTilesetAtIndex((tilesetIndex + 1) % tilesetIds.size());
    }

    private void previousTileset() {
        if (tilesetIds.size() <= 1) {
            return;
        }
        requestTilesetAtIndex((tilesetIndex - 1 + tilesetIds.size()) % tilesetIds.size());
    }

    private void requestTilesetAtIndex(final int index) {
        if (tilesetIds.isEmpty()) {
            return;
        }
        loadTilesetAtIndex(index);
    }

    private void loadTilesetAtIndex(final int index) {
        tilesetIndex = index;
        page = 0;
        disposeTileset();
        previewTileIds = Collections.emptyList();

        loadingTilesetId = tilesetIds.get(tilesetIndex);
        loadingTileset = true;
        statusMessage = "Loading " + loadingTilesetId + "…";
        cancelLoadSession();
        loadSession = TilesetLoadSession.start(
            registry,
            loadingTilesetId,
            TilesetLoadOptions.defaults(),
            ModTilesetRegistry.empty()
        );
    }

    private void applyLoadedTileset(final LoadedTileset loaded) {
        loadingTileset = false;
        tileset = loaded;
        previewTileIds = buildPreviewList(tileset);
        statusMessage = previewTileIds.size() + " preview tiles, " + tileset.getSpriteCount() + " sprites";
        Gdx.app.log("tileset", "Preview: " + loadingTilesetId + " — " + statusMessage);
        recomputeCellMetrics();
        recomputeLayoutForViewport();
    }

    private String currentTilesetId() {
        if (tilesetIds.isEmpty() || tilesetIndex < 0 || tilesetIndex >= tilesetIds.size()) {
            return null;
        }
        return tilesetIds.get(tilesetIndex);
    }

    private String formatTilesetPickerLine() {
        if (tilesetIds.isEmpty()) {
            return "Tileset: (none)";
        }
        final String id = loadingTileset ? loadingTilesetId : tilesetIds.get(tilesetIndex);
        final String displayName = findDisplayName(id);
        final String suffix = loadingTileset ? "  (loading)" : "";
        return "Tileset: " + id + " (\"" + displayName + "\")  "
            + (tilesetIndex + 1) + "/" + tilesetIds.size() + suffix;
    }

    private String findDisplayName(final String tilesetId) {
        if (registry == null) {
            return tilesetId;
        }
        for (final TilesetOption option : registry.getOptions()) {
            if (option.getId().equals(tilesetId)) {
                return option.getDisplayName();
            }
        }
        return tilesetId;
    }

    private static List<String> buildTilesetIds(final TilesetRegistry tilesetRegistry) {
        final List<String> ids = new ArrayList<>();
        for (final TilesetOption option : tilesetRegistry.getOptions()) {
            if (tilesetRegistry.contains(option.getId())) {
                ids.add(option.getId());
            }
        }
        return ids;
    }

    private int indexOfTileset(final String tilesetId) {
        if (tilesetId == null) {
            return -1;
        }
        return tilesetIds.indexOf(tilesetId);
    }

    private static String resolvePreferredTilesetId(final TilesetRegistry tilesetRegistry) {
        for (final String preferred : PREFERRED_TILESET_IDS) {
            if (tilesetRegistry.contains(preferred)) {
                return preferred;
            }
        }
        return tilesetRegistry.getOptions().get(0).getId();
    }

    private static List<String> buildPreviewList(final LoadedTileset loaded) {
        final List<String> ids = new ArrayList<>();
        for (final String preferred : PREFERRED_TILE_IDS) {
            if (loaded.findTile(preferred).isPresent()) {
                ids.add(preferred);
            }
        }
        final List<String> remaining = new ArrayList<>(loaded.getTiles().keySet());
        Collections.sort(remaining);
        for (final String tileId : remaining) {
            if (!ids.contains(tileId)) {
                ids.add(tileId);
            }
        }
        return ids;
    }

    private void disposeTileset() {
        if (tileset != null) {
            tileset.dispose();
            tileset = null;
        }
    }
}
