package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Side-panel terrain brush list with text filter (M3/M4). */
public final class MapPalettePanel {

    public static final int WIDTH = 220;
    private static final int MARGIN = 6;
    private static final int HEADER_LINES = 4;
    private static final int ROW_HEIGHT = 22;
    private static final int PREVIEW_SIZE = 16;
    private static final int FILTER_ROW_HEIGHT = 18;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    private TerrainRegistry terrainRegistry = new TerrainRegistry();
    private LoadedTileset tileset;
    private List<String> allTerrainIds = Collections.emptyList();
    private List<String> visibleTerrainIds = Collections.emptyList();
    private String selectedTerrainId = "t_grass";
    private String filterQuery = "";
    private boolean filterEditing;
    private int scrollOffset;
    private int lastViewportHeight = 720;
    private String tilesetId = "";
    private int tilesetIndex;
    private int tilesetCount;
    private boolean tilesetLoading;
    private String tilesetLoadStatus = "";

    public void setTilesetInfo(
        final String id,
        final int index,
        final int count,
        final boolean loading,
        final String loadStatus
    ) {
        tilesetId = id != null ? id : "";
        tilesetIndex = index;
        tilesetCount = count;
        tilesetLoading = loading;
        tilesetLoadStatus = loadStatus != null ? loadStatus : "";
    }

    public boolean isTilesetLoading() {
        return tilesetLoading;
    }

    public int panelCenterX(final int viewportWidth) {
        return viewportWidth - WIDTH / 2;
    }

    public void setTerrainRegistry(final TerrainRegistry terrainRegistry) {
        this.terrainRegistry = terrainRegistry;
        this.allTerrainIds = terrainRegistry.allIds();
        rebuildVisibleIds();
        if (!visibleTerrainIds.isEmpty() && !terrainRegistry.contains(selectedTerrainId)) {
            selectedTerrainId = visibleTerrainIds.get(0);
        }
        clampScroll(viewportHeightForScroll());
    }

    public void setTileset(final LoadedTileset tileset) {
        this.tileset = tileset;
        rebuildVisibleIds();
        clampScroll(viewportHeightForScroll());
    }

    public String getSelectedTerrainId() {
        return selectedTerrainId;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public boolean isFilterEditing() {
        return filterEditing;
    }

    public void setSelectedTerrainId(final String terrainId) {
        if (terrainId == null || terrainId.isEmpty()) {
            return;
        }
        selectedTerrainId = terrainId;
        ensureSelectionVisible();
    }

    public void beginFilterEdit() {
        filterEditing = true;
    }

    public void clearFilter() {
        filterQuery = "";
        filterEditing = false;
        rebuildVisibleIds();
        clampScroll(lastViewportHeight);
    }

    public boolean cancelFilterEdit() {
        if (!filterEditing) {
            return false;
        }
        filterEditing = false;
        return true;
    }

    public boolean onKeyDown(final int keycode) {
        if (!filterEditing) {
            return false;
        }
        if (keycode == Keys.BACKSPACE) {
            if (!filterQuery.isEmpty()) {
                filterQuery = filterQuery.substring(0, filterQuery.length() - 1);
                rebuildVisibleIds();
            }
            return true;
        }
        if (keycode == Keys.FORWARD_DEL) {
            filterQuery = "";
            rebuildVisibleIds();
            return true;
        }
        if (keycode == Keys.ENTER) {
            filterEditing = false;
            return true;
        }
        return false;
    }

    public boolean onKeyTyped(final char character) {
        if (!filterEditing) {
            return false;
        }
        if (Character.isISOControl(character)) {
            return false;
        }
        filterQuery += character;
        rebuildVisibleIds();
        return true;
    }

    public boolean containsPoint(final int screenX, final int viewportWidth) {
        return screenX >= viewportWidth - WIDTH;
    }

    public boolean onTouchDown(final int screenX, final int screenY, final int viewportWidth, final int viewportHeight) {
        if (!containsPoint(screenX, viewportWidth)) {
            return false;
        }
        if (isFilterRow(screenY, viewportHeight)) {
            filterEditing = true;
            return true;
        }
        final int listTopY = listTop(viewportHeight);
        if (screenY > listTopY || screenY < MARGIN) {
            return false;
        }
        final int localRow = (listTopY - screenY) / ROW_HEIGHT;
        final int index = scrollOffset + localRow;
        if (index < 0 || index >= visibleTerrainIds.size()) {
            return false;
        }
        selectedTerrainId = visibleTerrainIds.get(index);
        return true;
    }

    public boolean onScroll(final float amountY, final int viewportHeight) {
        if (visibleTerrainIds.isEmpty()) {
            return false;
        }
        scrollOffset += (int) amountY;
        clampScroll(viewportHeight);
        return true;
    }

    public void render(
        final SpriteBatch batch,
        final BitmapFont font,
        final int viewportWidth,
        final int viewportHeight,
        final long animationTick,
        final boolean animationPlayback
    ) {
        lastViewportHeight = viewportHeight;
        final int panelX = viewportWidth - WIDTH;
        final Color oldColor = font.getColor().cpy();
        font.setColor(0.75f, 0.78f, 0.85f, 1f);
        font.draw(batch, "Terrain palette", panelX + MARGIN, viewportHeight - MARGIN);

        final String tilesetLine = formatTilesetLine();
        font.draw(batch, fitLabel(font, tilesetLine, WIDTH - MARGIN * 2), panelX + MARGIN, viewportHeight - MARGIN - 16);

        final String filterLabel = "Filter: " + filterQuery + (filterEditing ? "_" : "");
        font.draw(batch, fitLabel(font, filterLabel, WIDTH - MARGIN * 2), panelX + MARGIN, viewportHeight - MARGIN - 32);

        final int paintableCount = countPaintableTerrain();
        final String countLabel = visibleTerrainIds.size() + "/" + paintableCount + " in tileset";
        font.draw(batch, countLabel, panelX + MARGIN, viewportHeight - MARGIN - 48);
        font.setColor(oldColor);

        if (tilesetLoading || tileset == null) {
            return;
        }

        final int listTop = listTop(viewportHeight);
        final int visibleRows = Math.max(1, (listTop - MARGIN) / ROW_HEIGHT);
        final int first = scrollOffset;
        final int last = Math.min(visibleTerrainIds.size(), first + visibleRows + 1);

        for (int index = first; index < last; index++) {
            final String id = visibleTerrainIds.get(index);
            final int row = index - scrollOffset;
            final int rowY = listTop - row * ROW_HEIGHT;
            final boolean selected = id.equals(selectedTerrainId);

            if (selected) {
                font.setColor(0.95f, 0.85f, 0.35f, 1f);
            } else {
                font.setColor(0.9f, 0.9f, 0.9f, 1f);
            }

            drawPreview(batch, id, panelX + MARGIN, rowY - PREVIEW_SIZE, animationTick, animationPlayback);

            final String label = formatLabel(id);
            final float labelX = panelX + MARGIN + PREVIEW_SIZE + 4;
            final float maxLabelWidth = WIDTH - PREVIEW_SIZE - MARGIN * 2 - 4;
            font.draw(batch, fitLabel(font, label, maxLabelWidth), labelX, rowY - 4);
        }
        font.setColor(oldColor);
    }

    private void rebuildVisibleIds() {
        final List<String> matched = matchFilterQuery();
        final List<String> paintable = new ArrayList<>();
        for (final String id : matched) {
            if (isPaintableInTileset(id)) {
                paintable.add(id);
            }
        }
        visibleTerrainIds = paintable;
        scrollOffset = 0;
        if (!visibleTerrainIds.isEmpty() && !visibleTerrainIds.contains(selectedTerrainId)) {
            selectedTerrainId = visibleTerrainIds.get(0);
        }
    }

    private List<String> matchFilterQuery() {
        if (filterQuery.isEmpty()) {
            return new ArrayList<>(allTerrainIds);
        }
        final String query = filterQuery.toLowerCase(Locale.ROOT);
        final List<String> filtered = new ArrayList<>();
        for (final String id : allTerrainIds) {
            if (id.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(id);
                continue;
            }
            final Optional<TerrainDefinition> def = terrainRegistry.find(id);
            if (def.isPresent() && def.get().getName().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    private boolean isPaintableInTileset(final String terrainId) {
        return TileSpriteResolver.hasDrawableArt(tileset, terrainId);
    }

    private int countPaintableTerrain() {
        if (tileset == null) {
            return 0;
        }
        int count = 0;
        for (final String id : allTerrainIds) {
            if (isPaintableInTileset(id)) {
                count++;
            }
        }
        return count;
    }

    private String formatTilesetLine() {
        if (tilesetId.isEmpty()) {
            return tilesetLoading ? "Tileset: (loading…)" : "Tileset: (none)";
        }
        final String indexLabel = tilesetCount > 0
            ? "  " + (tilesetIndex + 1) + "/" + tilesetCount
            : "";
        if (tilesetLoading) {
            return "Tileset: " + tilesetId + indexLabel + "  …";
        }
        return "Tileset: " + tilesetId + indexLabel;
    }

    private boolean isFilterRow(final int screenY, final int viewportHeight) {
        final int filterTop = viewportHeight - MARGIN - 32;
        final int filterBottom = filterTop - FILTER_ROW_HEIGHT;
        return screenY >= filterBottom && screenY <= filterTop + 4;
    }

    private void drawPreview(
        final SpriteBatch batch,
        final String terrainId,
        final float x,
        final float y,
        final long animationTick,
        final boolean animationPlayback
    ) {
        if (tileset == null) {
            return;
        }
        final Optional<TileDefinition> tileOpt = tileset.findTile(terrainId);
        if (!tileOpt.isPresent()) {
            return;
        }
        final TileDefinition tile = tileOpt.get();
        final int pick = TileSpriteResolver.animationPickIndex(tile, animationTick, animationPlayback);
        final TextureRegion bg = TileSpriteResolver.resolveBackground(tileset, tile, pick, TilesetFxType.NONE);
        final TextureRegion fg = TileSpriteResolver.resolveForeground(tileset, tile, pick, TilesetFxType.NONE);
        drawPreviewLayer(batch, tile, bg, x, y);
        drawPreviewLayer(batch, tile, fg, x, y);
    }

    private void drawPreviewLayer(
        final SpriteBatch batch,
        final TileDefinition tile,
        final TextureRegion region,
        final float x,
        final float y
    ) {
        if (region == null) {
            return;
        }
        final float baseTileW = tileset.getTileInfo().getWidth();
        final float scale = PREVIEW_SIZE / Math.max(1f, baseTileW);
        final float width = region.getRegionWidth() * scale;
        final float height = region.getRegionHeight() * scale;
        final float offsetX = tile.getOffsetX() * scale;
        final float offsetY = tile.getOffsetY() * scale;
        final float drawX = x + (PREVIEW_SIZE - width) / 2f + offsetX;
        final float drawY = y + (PREVIEW_SIZE - height) / 2f + offsetY;
        batch.draw(region, drawX, drawY, width, height);
    }

    private String formatLabel(final String id) {
        final Optional<TerrainDefinition> def = terrainRegistry.find(id);
        if (def.isPresent()) {
            return def.get().getName() + " (" + id + ")";
        }
        return id;
    }

    private String fitLabel(final BitmapFont font, final String text, final float maxWidth) {
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

    private void ensureSelectionVisible() {
        final int index = visibleTerrainIds.indexOf(selectedTerrainId);
        if (index < 0) {
            return;
        }
        if (index < scrollOffset) {
            scrollOffset = index;
        }
        final int visibleRows = visibleRowCount(lastViewportHeight);
        if (index >= scrollOffset + visibleRows) {
            scrollOffset = index - visibleRows + 1;
        }
        clampScroll(lastViewportHeight);
    }

    private void clampScroll(final int viewportHeight) {
        final int visibleRows = visibleRowCount(viewportHeight);
        final int maxScroll = Math.max(0, visibleTerrainIds.size() - visibleRows);
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
    }

    private int viewportHeightForScroll() {
        return lastViewportHeight;
    }

    private static int visibleRowCount(final int viewportHeight) {
        return Math.max(1, (listTop(viewportHeight) - MARGIN) / ROW_HEIGHT);
    }

    private static int listTop(final int viewportHeight) {
        return viewportHeight - MARGIN - HEADER_LINES * 16 - 8;
    }
}
