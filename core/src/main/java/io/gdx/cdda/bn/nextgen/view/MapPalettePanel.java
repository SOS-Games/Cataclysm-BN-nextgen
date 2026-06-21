package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
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

/** Side-panel terrain/furniture brush list with text filter (M3/M4/M5). */
public final class MapPalettePanel {

    public static final String CLEAR_FURNITURE_ROW_ID = "__clear_furniture__";

    public static final int WIDTH = 220;
    private static final int MARGIN = 6;
    private static final int HEADER_LINES = 4;
    private static final int ROW_HEIGHT = 22;
    private static final int PREVIEW_SIZE = 16;
    private static final int FILTER_ROW_HEIGHT = 18;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    private TerrainRegistry terrainRegistry = new TerrainRegistry();
    private FurnitureRegistry furnitureRegistry = new FurnitureRegistry();
    private LoadedTileset tileset;
    private List<String> allTerrainIds = Collections.emptyList();
    private List<String> allFurnitureIds = Collections.emptyList();
    private List<String> visibleTerrainIds = Collections.emptyList();
    private List<String> visibleFurnitureIds = Collections.emptyList();
    private String selectedTerrainId = "t_grass";
    private String selectedFurnitureId = "";
    private boolean clearFurnitureBrush;
    private PaletteBrushLayer brushLayer = PaletteBrushLayer.TERRAIN;
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

    public void setFurnitureRegistry(final FurnitureRegistry furnitureRegistry) {
        this.furnitureRegistry = furnitureRegistry;
        this.allFurnitureIds = furnitureRegistry.allIds();
        rebuildVisibleIds();
        clampScroll(viewportHeightForScroll());
    }

    public PaletteBrushLayer getBrushLayer() {
        return brushLayer;
    }

    public void setBrushLayer(final PaletteBrushLayer brushLayer) {
        this.brushLayer = brushLayer == null ? PaletteBrushLayer.TERRAIN : brushLayer;
        rebuildVisibleIds();
        clampScroll(viewportHeightForScroll());
    }

    public PaletteBrushLayer cycleBrushLayer() {
        brushLayer = brushLayer.next();
        rebuildVisibleIds();
        clampScroll(viewportHeightForScroll());
        return brushLayer;
    }

    public boolean isClearFurnitureBrush() {
        return brushLayer == PaletteBrushLayer.FURNITURE && clearFurnitureBrush;
    }

    public String getSelectedFurnitureId() {
        return isClearFurnitureBrush() ? null : selectedFurnitureId;
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

    public void setSelectedFurnitureId(final String furnitureId) {
        if (furnitureId == null || furnitureId.isEmpty()) {
            clearFurnitureBrush = true;
            selectedFurnitureId = "";
        } else {
            clearFurnitureBrush = false;
            selectedFurnitureId = furnitureId;
        }
        ensureSelectionVisible();
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
        filterEditing = false;
        final int listTopY = listTop(viewportHeight);
        if (screenY > listTopY || screenY < MARGIN) {
            return false;
        }
        final int localRow = (listTopY - screenY) / ROW_HEIGHT;
        final int index = scrollOffset + localRow;
        if (brushLayer == PaletteBrushLayer.TERRAIN) {
            if (index < 0 || index >= visibleTerrainIds.size()) {
                return false;
            }
            selectedTerrainId = visibleTerrainIds.get(index);
            return true;
        }
        if (index < 0 || index >= visibleFurnitureIds.size()) {
            return false;
        }
        final String id = visibleFurnitureIds.get(index);
        if (CLEAR_FURNITURE_ROW_ID.equals(id)) {
            clearFurnitureBrush = true;
            selectedFurnitureId = "";
        } else {
            clearFurnitureBrush = false;
            selectedFurnitureId = id;
        }
        return true;
    }

    public boolean onScroll(final float amountY, final int viewportHeight) {
        if (activeVisibleIds().isEmpty()) {
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
        final String paletteTitle = brushLayer == PaletteBrushLayer.TERRAIN ? "Terrain palette" : "Furniture palette";
        font.draw(batch, paletteTitle, panelX + MARGIN, viewportHeight - MARGIN);

        final String tilesetLine = formatTilesetLine();
        font.draw(batch, fitLabel(font, tilesetLine, WIDTH - MARGIN * 2), panelX + MARGIN, viewportHeight - MARGIN - 16);

        final String filterLabel = "Filter: " + filterQuery + (filterEditing ? "_" : "");
        font.draw(batch, fitLabel(font, filterLabel, WIDTH - MARGIN * 2), panelX + MARGIN, viewportHeight - MARGIN - 32);

        final int paintableCount = brushLayer == PaletteBrushLayer.TERRAIN
            ? countPaintableTerrain()
            : countPaintableFurniture();
        final String countLabel = activeVisibleIds().size() + "/" + paintableCount + " in tileset";
        font.draw(batch, countLabel, panelX + MARGIN, viewportHeight - MARGIN - 48);
        font.setColor(oldColor);

        if (tilesetLoading || tileset == null) {
            return;
        }

        final int listTop = listTop(viewportHeight);
        final int visibleRows = Math.max(1, (listTop - MARGIN) / ROW_HEIGHT);
        final List<String> visibleIds = activeVisibleIds();
        final int first = scrollOffset;
        final int last = Math.min(visibleIds.size(), first + visibleRows + 1);

        for (int index = first; index < last; index++) {
            final String id = visibleIds.get(index);
            final int row = index - scrollOffset;
            final int rowY = listTop - row * ROW_HEIGHT;
            final boolean selected = isRowSelected(id);

            if (selected) {
                font.setColor(0.95f, 0.85f, 0.35f, 1f);
            } else {
                font.setColor(0.9f, 0.9f, 0.9f, 1f);
            }

            if (brushLayer == PaletteBrushLayer.TERRAIN) {
                drawTerrainPreview(batch, id, panelX + MARGIN, rowY - PREVIEW_SIZE, animationTick, animationPlayback);
            } else if (!CLEAR_FURNITURE_ROW_ID.equals(id)) {
                drawFurniturePreview(batch, id, panelX + MARGIN, rowY - PREVIEW_SIZE, animationTick, animationPlayback);
            }

            final String label = formatRowLabel(id);
            final float labelX = panelX + MARGIN + PREVIEW_SIZE + 4;
            final float maxLabelWidth = WIDTH - PREVIEW_SIZE - MARGIN * 2 - 4;
            font.draw(batch, fitLabel(font, label, maxLabelWidth), labelX, rowY - 4);
        }
        font.setColor(oldColor);
    }

    private void rebuildVisibleIds() {
        visibleTerrainIds = buildVisibleTerrainIds();
        visibleFurnitureIds = buildVisibleFurnitureIds();
        scrollOffset = 0;
        if (brushLayer == PaletteBrushLayer.TERRAIN) {
            if (!visibleTerrainIds.isEmpty() && !visibleTerrainIds.contains(selectedTerrainId)) {
                selectedTerrainId = visibleTerrainIds.get(0);
            }
        } else if (!clearFurnitureBrush) {
            if (!visibleFurnitureIds.isEmpty()
                && !visibleFurnitureIds.contains(selectedFurnitureId)
                && !visibleFurnitureIds.contains(CLEAR_FURNITURE_ROW_ID)) {
                selectedFurnitureId = firstFurnitureRowId();
            }
        }
    }

    private List<String> buildVisibleTerrainIds() {
        final List<String> matched = matchTerrainFilterQuery();
        final List<String> paintable = new ArrayList<>();
        for (final String id : matched) {
            if (isPaintableTerrainInTileset(id)) {
                paintable.add(id);
            }
        }
        return paintable;
    }

    private List<String> buildVisibleFurnitureIds() {
        final List<String> rows = new ArrayList<>();
        rows.add(CLEAR_FURNITURE_ROW_ID);
        final List<String> matched = matchFurnitureFilterQuery();
        for (final String id : matched) {
            if (isPaintableFurnitureInTileset(id)) {
                rows.add(id);
            }
        }
        return rows;
    }

    private String firstFurnitureRowId() {
        for (final String id : visibleFurnitureIds) {
            if (!CLEAR_FURNITURE_ROW_ID.equals(id)) {
                return id;
            }
        }
        return "";
    }

    private List<String> activeVisibleIds() {
        return brushLayer == PaletteBrushLayer.TERRAIN ? visibleTerrainIds : visibleFurnitureIds;
    }

    private boolean isRowSelected(final String id) {
        if (brushLayer == PaletteBrushLayer.TERRAIN) {
            return id.equals(selectedTerrainId);
        }
        if (CLEAR_FURNITURE_ROW_ID.equals(id)) {
            return clearFurnitureBrush;
        }
        return !clearFurnitureBrush && id.equals(selectedFurnitureId);
    }

    private List<String> matchTerrainFilterQuery() {
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

    private List<String> matchFurnitureFilterQuery() {
        if (filterQuery.isEmpty()) {
            return new ArrayList<>(allFurnitureIds);
        }
        final String query = filterQuery.toLowerCase(Locale.ROOT);
        final List<String> filtered = new ArrayList<>();
        for (final String id : allFurnitureIds) {
            if (id.toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(id);
                continue;
            }
            final Optional<FurnitureDefinition> def = furnitureRegistry.find(id);
            if (def.isPresent() && def.get().getName().toLowerCase(Locale.ROOT).contains(query)) {
                filtered.add(id);
            }
        }
        return filtered;
    }

    private boolean isPaintableTerrainInTileset(final String terrainId) {
        return TileLooksLikeResolver.hasDrawableTerrainArt(tileset, terrainId, terrainRegistry);
    }

    private boolean isPaintableFurnitureInTileset(final String furnitureId) {
        return TileLooksLikeResolver.hasDrawableFurnitureArt(tileset, furnitureId, furnitureRegistry);
    }

    private int countPaintableFurniture() {
        if (tileset == null) {
            return 0;
        }
        int count = 0;
        for (final String id : allFurnitureIds) {
            if (isPaintableFurnitureInTileset(id)) {
                count++;
            }
        }
        return count;
    }

    private int countPaintableTerrain() {
        if (tileset == null) {
            return 0;
        }
        int count = 0;
        for (final String id : allTerrainIds) {
            if (isPaintableTerrainInTileset(id)) {
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

    private void drawTerrainPreview(
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
        final String drawId = TileLooksLikeResolver.resolveTerrainDrawId(terrainId, tileset, terrainRegistry);
        final Optional<TileDefinition> tileOpt = tileset.findTile(drawId);
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

    private void drawFurniturePreview(
        final SpriteBatch batch,
        final String furnitureId,
        final float x,
        final float y,
        final long animationTick,
        final boolean animationPlayback
    ) {
        if (tileset == null) {
            return;
        }
        final String drawId = TileLooksLikeResolver.resolveFurnitureDrawId(furnitureId, tileset, furnitureRegistry);
        final Optional<TileDefinition> tileOpt = tileset.findTile(drawId);
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

    private void drawPreview(
        final SpriteBatch batch,
        final String terrainId,
        final float x,
        final float y,
        final long animationTick,
        final boolean animationPlayback
    ) {
        drawTerrainPreview(batch, terrainId, x, y, animationTick, animationPlayback);
    }

    private void drawPreviewLayer(
        final SpriteBatch batch,
        final TileDefinition tile,
        final TextureRegion region,
        final float cellLeft,
        final float cellBottom
    ) {
        if (region == null || tileset == null) {
            return;
        }
        final TileDrawMath.DrawRect rect = TileDrawMath.computeDrawRect(
            tileset,
            tile,
            region,
            cellLeft,
            cellBottom,
            PREVIEW_SIZE
        );
        batch.draw(region, rect.x, rect.y, rect.width, rect.height);
    }

    private String formatRowLabel(final String id) {
        if (CLEAR_FURNITURE_ROW_ID.equals(id)) {
            return "Clear furniture";
        }
        if (brushLayer == PaletteBrushLayer.FURNITURE) {
            final Optional<FurnitureDefinition> def = furnitureRegistry.find(id);
            if (def.isPresent()) {
                return def.get().getName() + " (" + id + ")";
            }
            return id;
        }
        return formatTerrainLabel(id);
    }

    private String formatTerrainLabel(final String id) {
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
        final List<String> visibleIds = activeVisibleIds();
        final String selectedId = brushLayer == PaletteBrushLayer.TERRAIN
            ? selectedTerrainId
            : (clearFurnitureBrush ? CLEAR_FURNITURE_ROW_ID : selectedFurnitureId);
        final int index = visibleIds.indexOf(selectedId);
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
        final int maxScroll = Math.max(0, activeVisibleIds().size() - visibleRows);
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
