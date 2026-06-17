package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import io.gdx.cdda.bn.nextgen.DefaultContent;
import io.gdx.cdda.bn.nextgen.gamedata.GameDataLoader;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.validate.GameDataValidationException;
import io.gdx.cdda.bn.nextgen.gamedata.validate.GameDataValidator;
import io.gdx.cdda.bn.nextgen.gamedata.validate.ValidationOptions;
import io.gdx.cdda.bn.nextgen.gamedata.validate.ValidationReport;
import io.gdx.cdda.bn.nextgen.map.MapCell;
import io.gdx.cdda.bn.nextgen.map.MapFileIO;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.tileset.TilesetDiscovery;
import io.gdx.cdda.bn.nextgen.tileset.TilesetOption;
import io.gdx.cdda.bn.nextgen.tileset.TilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadOptions;
import io.gdx.cdda.bn.nextgen.tileset.load.TilesetLoadSession;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetDiscovery;
import io.gdx.cdda.bn.nextgen.tileset.mod.ModTilesetRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;
import io.gdx.cdda.bn.nextgen.tileset.model.TileDefinition;
import io.gdx.cdda.bn.nextgen.tileset.model.TilesetFxType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Map editor with terrain palette and paint gestures (M3). */
public final class MapEditorScreen {

    private static final long ANIMATION_TICK_MS = 17L;
    private static final int HUD_MARGIN = 8;
    private static final int HUD_LINES = 5;
    private static final Path DEFAULT_MAP_PATH = Paths.get("maps/autosave.json");
    private static final int[][] GRID_PRESETS = {
        { 10, 10 },
        { 20, 20 },
        { 32, 32 },
        { 48, 48 },
        { 64, 64 }
    };
    private static final String[] PREFERRED_TILESET_IDS = DefaultContent.PREFERRED_TILESET_IDS;

    private static final int CLICK_DRAG_THRESHOLD_PX = 5;
    private static final long POINTER_DEBUG_LOG_INTERVAL_MS = 250L;
    private static final float MIN_VIEW_ZOOM = 0.5f;
    private static final float MAX_VIEW_ZOOM = 8f;
    private static final float ZOOM_SCROLL_STEP = 0.24f;
    private static final float ZOOM_KEY_STEP = 0.5f;

    private final SpriteBatch batch;
    private final BitmapFont font;
    private final MapGrid grid;
    private final MapPalettePanel palette = new MapPalettePanel();
    private final MapEditorToolbar toolbar = new MapEditorToolbar();
    private final LoadingOverlay loadingOverlay = new LoadingOverlay();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final TextureRegion whitePixel;

    private LoadedGameData gameData;
    private LoadedTileset tileset;
    private TilesetLoadSession loadSession;
    private TilesetRegistry tilesetRegistry;
    private ModTilesetRegistry modTilesetRegistry = ModTilesetRegistry.empty();
    private List<String> tilesetIds = new ArrayList<>();
    private int tilesetIndex;
    private boolean loadingTileset;
    private String loadingTilesetId = "";
    private int gridPresetIndex = 1;
    private Path currentMapPath = DEFAULT_MAP_PATH;
    private String statusMessage;
    private float viewZoom = 2f;
    private float cameraX;
    private float cameraY;
    private boolean animationPlayback = true;
    private long animationTick;

    private boolean painting;
    private boolean panning;
    private boolean rightClickPending;
    private int rightClickStartX;
    private int rightClickStartY;
    private int lastPointerX;
    private int lastPointerY;
    private int lastPointerRawY;
    private int lastPaintCellX = -1;
    private int lastPaintCellY = -1;
    private float panAnchorScreenX;
    private float panAnchorScreenY;
    private float panAnchorCameraX;
    private float panAnchorCameraY;
    private int hoverCellX = -1;
    private int hoverCellY = -1;
    private boolean pointerDebugLogging;
    private long lastPointerDebugLogMs;
    private int lastPointerDebugCellX = -1;
    private int lastPointerDebugCellY = -1;

    private final MapgenPreviewService mapgenPreviewService = new MapgenPreviewService();
    private final MapgenPickerDialog mapgenPicker = new MapgenPickerDialog();
    private boolean loadingMapgenCatalog;

    public MapEditorScreen(final SpriteBatch batch) {
        this.batch = batch;
        this.font = new BitmapFont();
        this.font.setUseIntegerPositions(true);
        this.whitePixel = createWhitePixel();
        this.grid = createCheckerboardGrid();
        this.statusMessage = "Map editor";
        loadGameData();
        loadDefaultTileset();
        palette.setSelectedTerrainId("t_grass");
        centerCamera();
    }

    public void render() {
        advanceLoadSession();
        updateAnimationFrame();
        updateHoverCell();
        updatePaletteTilesetInfo();
        ScreenUtils.clear(0.10f, 0.11f, 0.14f, 1f);
        applyProjection();

        batch.begin();
        if (!loadingTileset) {
            drawGrid();
            drawHoverCell();
        }
        drawPalette();
        drawToolbar();
        drawHud();
        if (loadingTileset) {
            drawTilesetLoadingOverlay();
        }
        if (loadingMapgenCatalog) {
            drawMapgenCatalogLoadingOverlay();
        }
        mapgenPicker.render(batch, font, whitePixel, viewportPixelWidth(), viewportPixelHeight());
        pollMapgenPickerSelection();
        batch.end();
    }

    public boolean onEscape() {
        if (mapgenPicker.isOpen()) {
            if (mapgenPicker.isFilterEditing() && !mapgenPicker.getFilterQuery().isEmpty()) {
                mapgenPicker.clearFilter();
                return true;
            }
            mapgenPicker.cancel();
            return true;
        }
        if (palette.isFilterEditing()) {
            palette.cancelFilterEdit();
            return true;
        }
        if (!palette.getFilterQuery().isEmpty()) {
            palette.clearFilter();
            return true;
        }
        return false;
    }

    public boolean onKeyDown(final int keycode) {
        if (mapgenPicker.isOpen()) {
            if (mapgenPicker.onKeyDown(keycode)) {
                return true;
            }
            return !mapgenPicker.isFilterEditing();
        }
        if (palette.isFilterEditing() && palette.onKeyDown(keycode)) {
            return true;
        }
        if (keycode == Keys.SLASH) {
            palette.beginFilterEdit();
            return true;
        }
        if (keycode == Keys.CONTROL_LEFT || keycode == Keys.CONTROL_RIGHT) {
            return false;
        }
        if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT)) {
            if (keycode == Keys.S) {
                saveMap();
                return true;
            }
            if (keycode == Keys.O) {
                loadMap();
                return true;
            }
            if (keycode == Keys.G) {
                openMapgenPicker();
                return true;
            }
        }

        final float panStep = tilePixelSize() * 2f;
        if (keycode == Keys.LEFT) {
            cameraX -= panStep;
            return true;
        }
        if (keycode == Keys.RIGHT) {
            cameraX += panStep;
            return true;
        }
        if (keycode == Keys.UP) {
            cameraY += panStep;
            return true;
        }
        if (keycode == Keys.DOWN) {
            cameraY -= panStep;
            return true;
        }
        if (keycode == Keys.EQUALS || keycode == Keys.PLUS) {
            adjustViewZoom(-ZOOM_KEY_STEP, gridZoomAnchorX(), gridZoomAnchorY());
            return true;
        }
        if (keycode == Keys.MINUS) {
            adjustViewZoom(ZOOM_KEY_STEP, gridZoomAnchorX(), gridZoomAnchorY());
            return true;
        }
        if (keycode == Keys.C) {
            centerCamera();
            return true;
        }
        if (keycode == Keys.P) {
            animationPlayback = !animationPlayback;
            return true;
        }
        if (keycode == Keys.R) {
            reloadTileset();
            return true;
        }
        if (keycode == Keys.G) {
            cycleGridPreset();
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
        if (keycode == Keys.F3) {
            pointerDebugLogging = !pointerDebugLogging;
            statusMessage = pointerDebugLogging
                ? "Pointer debug ON (F3) — see console"
                : "Pointer debug OFF";
            if (pointerDebugLogging) {
                logPointerDebug(true);
            }
            return true;
        }
        return false;
    }

    public boolean onKeyTyped(final char character) {
        if (mapgenPicker.isOpen() && mapgenPicker.onKeyTyped(character)) {
            return true;
        }
        return palette.onKeyTyped(character);
    }

    public boolean onScroll(final float amountY) {
        if (mapgenPicker.isOpen()) {
            return mapgenPicker.onScroll(amountY);
        }
        final int mouseX = ScreenInput.pointerX();
        final int mouseY = ScreenInput.pointerY();
        if (palette.containsPoint(mouseX, viewportPixelWidth())) {
            return palette.onScroll(amountY, viewportPixelHeight());
        }
        if (isToolbarPoint(mouseX, mouseY)) {
            return false;
        }
        if (amountY != 0f) {
            adjustViewZoom(amountY * ZOOM_SCROLL_STEP, mouseX, mouseY);
        }
        return true;
    }

    public boolean onTouchDown(final int screenX, final int screenY, final int button) {
        if (mapgenPicker.isOpen()) {
            return mapgenPicker.onTouchDown(
                screenX,
                ScreenInput.fromInputY(screenY),
                viewportPixelWidth(),
                viewportPixelHeight()
            );
        }
        final int y = ScreenInput.fromInputY(screenY);
        lastPointerX = screenX;
        lastPointerY = y;
        lastPointerRawY = screenY;

        if (palette.containsPoint(screenX, viewportPixelWidth())) {
            palette.onTouchDown(screenX, y, viewportPixelWidth(), viewportPixelHeight());
            return true;
        }
        if (isToolbarPoint(screenX, y)) {
            handleToolbarHit(toolbar.hitTest(screenX, y, canvasWidth()));
            return true;
        }
        if (palette.isFilterEditing()) {
            return false;
        }
        if (loadingTileset) {
            return true;
        }
        if (button == Buttons.LEFT) {
            if (shouldPanWithPrimaryButton()) {
                beginPan(screenX, y);
                return true;
            }
            if (toolbar.getActiveTool() == MapEditorToolbar.Tool.EYEDROPPER) {
                eyedropAt(screenX, y);
                return true;
            }
            painting = true;
            lastPaintCellX = -1;
            lastPaintCellY = -1;
            paintAt(screenX, y);
            return true;
        }
        if (button == Buttons.RIGHT) {
            rightClickPending = true;
            rightClickStartX = screenX;
            rightClickStartY = y;
            storePanAnchor(screenX, y);
            return true;
        }
        if (button == Buttons.MIDDLE) {
            beginPan(screenX, y);
            return true;
        }
        return false;
    }

    public boolean onTouchDragged(final int screenX, final int screenY) {
        if (mapgenPicker.isOpen() || loadingMapgenCatalog) {
            return true;
        }
        final int y = ScreenInput.fromInputY(screenY);
        lastPointerX = screenX;
        lastPointerY = y;
        lastPointerRawY = screenY;

        if (rightClickPending && Gdx.input.isButtonPressed(Buttons.RIGHT) && !panning) {
            final int dx = screenX - rightClickStartX;
            final int dy = y - rightClickStartY;
            if (dx * dx + dy * dy > CLICK_DRAG_THRESHOLD_PX * CLICK_DRAG_THRESHOLD_PX) {
                panning = true;
            }
        }
        if (panning) {
            cameraX = panAnchorCameraX + (screenX - panAnchorScreenX);
            cameraY = panAnchorCameraY + (y - panAnchorScreenY);
            return true;
        }
        if (painting) {
            paintAt(screenX, y);
            return true;
        }
        return false;
    }

    public boolean onTouchUp(final int screenX, final int screenY, final int button) {
        if (mapgenPicker.isOpen() || loadingMapgenCatalog) {
            return true;
        }
        final int y = ScreenInput.fromInputY(screenY);
        lastPointerX = screenX;
        lastPointerY = y;
        lastPointerRawY = screenY;

        if (button == Buttons.LEFT) {
            painting = false;
            lastPaintCellX = -1;
            lastPaintCellY = -1;
            panning = false;
            return true;
        }
        if (button == Buttons.RIGHT) {
            if (rightClickPending && !panning) {
                eyedropAt(screenX, y);
            }
            rightClickPending = false;
            panning = false;
            return true;
        }
        if (button == Buttons.MIDDLE) {
            panning = false;
            return true;
        }
        return false;
    }

    public boolean onMouseMoved(final int screenX, final int screenY) {
        lastPointerX = screenX;
        lastPointerY = ScreenInput.fromInputY(screenY);
        lastPointerRawY = screenY;
        return false;
    }

    public void resize(final int width, final int height) {
        // Projection reads live viewport sizes each frame.
    }

    public void dispose() {
        cancelLoadSession();
        if (tileset != null) {
            tileset.dispose();
            tileset = null;
        }
        loadingOverlay.dispose();
        whitePixel.getTexture().dispose();
        font.dispose();
    }

    private void handleToolbarHit(final MapEditorToolbar.ToolbarHit hit) {
        if (!hit.isHit()) {
            return;
        }
        switch (hit.getAction()) {
            case SET_TOOL:
                toolbar.setActiveTool(hit.getTool());
                statusMessage = "Tool: " + hit.getTool().name().toLowerCase();
                break;
            case ZOOM_IN:
                adjustViewZoom(-ZOOM_KEY_STEP, gridZoomAnchorX(), gridZoomAnchorY());
                break;
            case ZOOM_OUT:
                adjustViewZoom(ZOOM_KEY_STEP, gridZoomAnchorX(), gridZoomAnchorY());
                break;
            case CENTER:
                centerCamera();
                statusMessage = "Centered view";
                break;
            case SAVE:
                saveMap();
                break;
            case LOAD:
                loadMap();
                break;
            case OPEN_MAPGEN:
                openMapgenPicker();
                break;
            case CYCLE_GRID:
                cycleGridPreset();
                break;
            case TILESET_PREV:
                previousTileset();
                break;
            case TILESET_NEXT:
                nextTileset();
                break;
            default:
                break;
        }
    }

    private void beginPan(final int screenX, final int screenY) {
        panning = true;
        storePanAnchor(screenX, screenY);
    }

    private void storePanAnchor(final int screenX, final int screenY) {
        panAnchorScreenX = screenX;
        panAnchorScreenY = screenY;
        panAnchorCameraX = cameraX;
        panAnchorCameraY = cameraY;
    }

    private boolean shouldPanWithPrimaryButton() {
        return toolbar.getActiveTool() == MapEditorToolbar.Tool.PAN || Gdx.input.isKeyPressed(Keys.SPACE);
    }

    private boolean isToolbarPoint(final int screenX, final int screenY) {
        return screenX >= 0
            && screenX < canvasWidth()
            && screenY >= 8
            && screenY <= 8 + MapEditorToolbar.HEIGHT + 4;
    }

    private void drawToolbar() {
        toolbar.render(batch, font, whitePixel, canvasWidth());
    }

    private void drawHoverCell() {
        if (hoverCellX < 0 || hoverCellY < 0) {
            return;
        }
        if (toolbar.getActiveTool() == MapEditorToolbar.Tool.PAN) {
            return;
        }
        final float tilePx = tilePixelSize();
        final float x = cameraX + hoverCellX * tilePx;
        final float y = cellBottomY(hoverCellY);
        batch.setColor(1f, 1f, 1f, 0.28f);
        batch.draw(whitePixel, x, y, tilePx, tilePx);
        batch.setColor(Color.WHITE);
    }

    private static TextureRegion createWhitePixel() {
        final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        final Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    private void loadGameData() {
        try {
            gameData = GameDataLoader.loadCore(GameDataLoadOptions.defaults());
            palette.setTerrainRegistry(gameData.getTerrain());
            statusMessage = "Loaded " + gameData.getTerrain().size() + " terrain ids";
            appendValidationSummary(runGameDataValidation(null));
        } catch (final IOException e) {
            statusMessage = "Game data load failed: " + e.getMessage();
            Gdx.app.error("map-editor", statusMessage, e);
        }
    }

    private ValidationReport runGameDataValidation(final LoadedTileset withTileset) {
        if (gameData == null) {
            return new ValidationReport(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        }
        final ValidationOptions options = withTileset != null
            ? ValidationOptions.withTileset(withTileset)
            : ValidationOptions.defaults();
        try {
            final ValidationReport report = GameDataValidator.validate(gameData, options);
            logValidationReport(report);
            return report;
        } catch (final GameDataValidationException e) {
            logValidationReport(e.getReport());
            Gdx.app.error("map-editor", "Game data validation failed", e);
            return e.getReport();
        }
    }

    private void logValidationReport(final ValidationReport report) {
        for (final String error : report.getErrors()) {
            Gdx.app.error("game-data", error);
        }
        for (final String warning : report.getWarnings()) {
            Gdx.app.log("game-data", warning);
        }
        for (final String info : report.getInfos()) {
            Gdx.app.log("game-data", info);
        }
    }

    private void appendValidationSummary(final ValidationReport report) {
        final int issues = report.getErrors().size() + report.getWarnings().size();
        if (issues > 0) {
            statusMessage += " | " + issues + " validation issue(s)";
        }
    }

    private void drawGrid() {
        final float tilePx = tilePixelSize();
        final int canvasWidth = canvasWidth();
        final int viewH = viewportPixelHeight();
        final float baseY = gridBaseY();
        final float topY = gridTopY();

        final int firstCol = Math.max(0, (int) Math.floor((-cameraX) / tilePx) - 1);
        final int lastCol = Math.min(grid.width(), (int) Math.ceil((canvasWidth - cameraX) / tilePx) + 1);
        final int firstRow = Math.max(0, (int) Math.floor((topY - viewH) / tilePx));
        final int lastRow = Math.min(grid.height(), (int) Math.ceil((topY - baseY) / tilePx));

        for (int y = firstRow; y < lastRow; y++) {
            for (int x = firstCol; x < lastCol; x++) {
                final float worldX = cameraX + x * tilePx;
                final float worldY = cellBottomY(y);
                drawCellTerrain(grid.get(x, y).getTerrainId(), worldX, worldY, tilePx);
            }
        }
    }

    private void drawPalette() {
        palette.render(
            batch,
            font,
            viewportPixelWidth(),
            viewportPixelHeight(),
            animationTick,
            animationPlayback
        );
    }

    private void drawTilesetLoadingOverlay() {
        loadingOverlay.update(Gdx.graphics.getDeltaTime());
        loadingOverlay.draw(
            batch,
            font,
            whitePixel,
            0,
            0,
            canvasWidth(),
            viewportPixelHeight(),
            "Loading " + loadingTilesetId,
            formatTilesetLoadDetail()
        );
    }

    private String formatTilesetLoadDetail() {
        if (loadSession == null) {
            return "";
        }
        return loadSession.getProgressSummary();
    }

    private void updatePaletteTilesetInfo() {
        final String id = loadingTileset ? loadingTilesetId : currentTilesetId();
        final String status = loadingTileset && loadSession != null
            ? loadSession.getProgressLabel()
            : "";
        palette.setTilesetInfo(
            id != null ? id : "",
            tilesetIndex,
            tilesetIds.size(),
            loadingTileset,
            status
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
            Gdx.app.error("map-editor", statusMessage);
            loadSession = null;
            palette.setTileset(null);
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

    private void applyLoadedTileset(final LoadedTileset loaded) {
        loadingTileset = false;
        tileset = loaded;
        palette.setTileset(tileset);
        statusMessage = "Tileset: " + loadingTilesetId;
        if (gameData != null) {
            statusMessage += " | " + palette.getSelectedTerrainId() + " brush";
            appendValidationSummary(runGameDataValidation(loaded));
        }
    }

    private String currentTilesetId() {
        if (tilesetIds.isEmpty() || tilesetIndex < 0 || tilesetIndex >= tilesetIds.size()) {
            return null;
        }
        return tilesetIds.get(tilesetIndex);
    }

    private String fitHudLabel(final String text, final int maxWidth) {
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

    private void drawCellTerrain(final String terrainId, final float drawX, final float drawY, final float tilePx) {
        if (tileset == null) {
            return;
        }

        final TileDefinition tile = findTileWithFallback(terrainId).orElse(null);
        if (tile == null) {
            return;
        }

        final int pick = TileSpriteResolver.animationPickIndex(tile, animationTick, animationPlayback);
        final TextureRegion bg = TileSpriteResolver.resolveBackground(tileset, tile, pick, TilesetFxType.NONE);
        final TextureRegion fg = TileSpriteResolver.resolveForeground(tileset, tile, pick, TilesetFxType.NONE);

        if (bg != null) {
            drawLayer(tile, bg, drawX, drawY, tilePx);
        }
        if (fg != null) {
            drawLayer(tile, fg, drawX, drawY, tilePx);
        }
    }

    private void drawLayer(
        final TileDefinition tile,
        final TextureRegion region,
        final float drawX,
        final float drawY,
        final float tilePx
    ) {
        final float baseTileW = tileset.getTileInfo().getWidth();
        final float baseTileH = tileset.getTileInfo().getHeight();
        final float tileScale = tilePx / Math.max(1f, baseTileW);
        final float width = region.getRegionWidth() * tileScale;
        final float height = region.getRegionHeight() * tileScale;
        final float offsetX = tile.getOffsetX() * tileScale;
        final float offsetY = tile.getOffsetY() * tileScale;
        final float x = drawX + (baseTileW * tileScale - width) / 2f + offsetX;
        final float y = drawY + (baseTileH * tileScale - height) / 2f + offsetY;
        batch.draw(region, x, y, width, height);
    }

    private Optional<TileDefinition> findTileWithFallback(final String terrainId) {
        final Optional<TileDefinition> exact = tileset.findTile(terrainId);
        if (exact.isPresent()) {
            return exact;
        }
        final Optional<TileDefinition> unknown = tileset.findTile("unknown");
        return unknown.isPresent() ? unknown : tileset.findTile("t_dirt");
    }

    private void drawHud() {
        final String brush = palette.getSelectedTerrainId();
        final String cursor = hoverCellX >= 0
            ? hoverCellX + "," + hoverCellY + " = " + grid.get(hoverCellX, hoverCellY).getTerrainId()
            : "—";
        final String tilesetLine = formatTilesetLine();
        final String mapPath = currentMapPath.toAbsolutePath().normalize().toString();
        final String line1 = statusMessage;
        final String line2 = tilesetLine + "  |  map: " + mapPath;
        final String line3 = "Grid " + grid.width() + "x" + grid.height()
            + "  |  brush " + brush + "  |  cursor " + cursor
            + "  |  tool " + toolbar.getActiveTool().name().toLowerCase()
            + "  |  zoom " + formatViewZoom();
        final String line4 = "Toolbar: Paint / Pan / Pick / Mapgen — Ctrl+G import json mapgen";
        final String line5 = pointerDebugLogging
            ? "F3 pointer debug ON — delta = mouse minus highlight center  |  Esc menu"
            : "Palette: click terrain, scroll wheel, click filter  |  F3 debug  |  Esc menu";

        final int top = viewportPixelHeight() - HUD_MARGIN;
        font.draw(batch, line1, HUD_MARGIN, top);
        font.draw(batch, line2, HUD_MARGIN, top - 16);
        font.draw(batch, line3, HUD_MARGIN, top - 32);
        font.draw(batch, line4, HUD_MARGIN, top - 48);
        font.draw(batch, line5, HUD_MARGIN, top - 64);
    }

    private String formatTilesetLine() {
        if (tilesetIds.isEmpty()) {
            return "Tileset: (none)";
        }
        final String id = tilesetIds.get(tilesetIndex);
        return "Tileset: " + id + "  " + (tilesetIndex + 1) + "/" + tilesetIds.size();
    }

    private void paintAt(final int screenX, final int screenY) {
        if (pointerDebugLogging) {
            logPointerDebugAt(screenX, screenY, "paint");
        }
        final CellCoord cell = screenToCell(screenX, screenY);
        if (cell == null) {
            return;
        }
        if (cell.x == lastPaintCellX && cell.y == lastPaintCellY) {
            return;
        }
        grid.setTerrain(cell.x, cell.y, palette.getSelectedTerrainId());
        lastPaintCellX = cell.x;
        lastPaintCellY = cell.y;
    }

    private void eyedropAt(final int screenX, final int screenY) {
        final CellCoord cell = screenToCell(screenX, screenY);
        if (cell == null) {
            return;
        }
        palette.setSelectedTerrainId(grid.get(cell.x, cell.y).getTerrainId());
        statusMessage = "Eyedropper: " + palette.getSelectedTerrainId();
    }

    private void updateHoverCell() {
        final int mouseX = pointerScreenX();
        final int mouseY = pointerScreenY();
        lastPointerRawY = ScreenInput.rawPointerY();
        final CellCoord cell = screenToCell(mouseX, mouseY);
        if (cell == null) {
            hoverCellX = -1;
            hoverCellY = -1;
        } else {
            hoverCellX = cell.x;
            hoverCellY = cell.y;
        }
        if (pointerDebugLogging) {
            logPointerDebug(false);
        }
    }

    private int pointerScreenX() {
        return ScreenInput.pointerX();
    }

    private int pointerScreenY() {
        return ScreenInput.pointerY();
    }

    private CellCoord screenToCell(final int screenX, final int screenY) {
        if (screenX < 0 || screenX >= canvasWidth()) {
            return null;
        }
        if (isToolbarPoint(screenX, screenY)) {
            return null;
        }

        final float baseY = gridBaseY();
        final float topY = gridTopY();
        if (screenY < baseY || screenY >= topY) {
            return null;
        }

        final float tilePx = tilePixelSize();
        final int cellX = (int) Math.floor((screenX - cameraX) / tilePx);
        final float rowOffset = gridTopY() - screenY;
        final int cellY = (int) Math.floor(rowOffset / tilePx);
        if (cellX < 0 || cellY < 0 || cellX >= grid.width() || cellY >= grid.height()) {
            return null;
        }
        return new CellCoord(cellX, cellY);
    }

    /** Bottom edge of the grid in screen space (LibGDX y-up). */
    private float gridBaseY() {
        return cameraY + hudHeight();
    }

    /** Top edge of the grid in screen space; grid row 0 is just below this. */
    private float gridTopY() {
        return gridBaseY() + grid.height() * tilePixelSize();
    }

    /** Bottom-left draw position for a cell; row 0 is the top row of the map. */
    private float cellBottomY(final int gridY) {
        return gridTopY() - (gridY + 1) * tilePixelSize();
    }

    private void logPointerDebug(final boolean force) {
        logPointerDebugAt(pointerScreenX(), pointerScreenY(), "hover", force);
    }

    private void logPointerDebugAt(final int screenX, final int screenY, final String source) {
        logPointerDebugAt(screenX, screenY, source, true);
    }

    private void logPointerDebugAt(
        final int screenX,
        final int screenY,
        final String source,
        final boolean force
    ) {
        final long now = System.currentTimeMillis();
        final CellCoord cell = screenToCell(screenX, screenY);
        final int cellX = cell == null ? -1 : cell.x;
        final int cellY = cell == null ? -1 : cell.y;
        final boolean cellChanged = cellX != lastPointerDebugCellX || cellY != lastPointerDebugCellY;
        if (!force && !cellChanged && now - lastPointerDebugLogMs < POINTER_DEBUG_LOG_INTERVAL_MS) {
            return;
        }
        lastPointerDebugLogMs = now;
        lastPointerDebugCellX = cellX;
        lastPointerDebugCellY = cellY;

        final float tilePx = tilePixelSize();
        final float baseY = gridBaseY();
        final float topY = gridTopY();
        final float rowOffset = topY - screenY;
        final int rawCellX = (int) Math.floor((screenX - cameraX) / tilePx);
        final int rawCellY = (int) Math.floor(rowOffset / tilePx);

        final StringBuilder line = new StringBuilder();
        line.append(source);
        line.append(" screen=(").append(screenX).append(',').append(screenY).append(')');
        line.append(" inputRaw=(").append(screenX).append(',').append(lastPointerRawY).append(')');
        line.append(" touch=(").append(lastPointerX).append(',').append(lastPointerY).append(')');
        line.append(" cell=").append(formatCell(cellX, cellY));
        line.append(" rawCell=(").append(rawCellX).append(',').append(rawCellY).append(')');

        if (cellX >= 0 && cellY >= 0) {
            final float highlightLeft = cameraX + cellX * tilePx;
            final float highlightBottom = cellBottomY(cellY);
            final float highlightRight = highlightLeft + tilePx;
            final float highlightTop = highlightBottom + tilePx;
            final float centerX = (highlightLeft + highlightRight) * 0.5f;
            final float centerY = (highlightBottom + highlightTop) * 0.5f;
            line.append(" highlight=[")
                .append(Math.round(highlightLeft))
                .append(',')
                .append(Math.round(highlightBottom))
                .append(" .. ")
                .append(Math.round(highlightRight))
                .append(',')
                .append(Math.round(highlightTop))
                .append(']');
            line.append(" center=(")
                .append(Math.round(centerX))
                .append(',')
                .append(Math.round(centerY))
                .append(')');
            line.append(" delta=(")
                .append(screenX - Math.round(centerX))
                .append(',')
                .append(screenY - Math.round(centerY))
                .append(')');
        } else {
            line.append(" highlight=none");
        }

        line.append(" gridY=[base=").append(Math.round(baseY)).append(",top=").append(Math.round(topY)).append(']');
        line.append(" rowOffset=").append(Math.round(rowOffset));
        line.append(" camera=(").append(Math.round(cameraX)).append(',').append(Math.round(cameraY)).append(')');
        line.append(" tilePx=").append(tilePx);
        line.append(" viewport=")
            .append(Gdx.graphics.getWidth())
            .append('x')
            .append(Gdx.graphics.getHeight());
        line.append(" backbuffer=")
            .append(Gdx.graphics.getBackBufferWidth())
            .append('x')
            .append(Gdx.graphics.getBackBufferHeight());

        Gdx.app.log("map-editor-pointer", line.toString());
    }

    private static String formatCell(final int cellX, final int cellY) {
        if (cellX < 0 || cellY < 0) {
            return "—";
        }
        return "(" + cellX + "," + cellY + ")";
    }

    private void saveMap() {
        try {
            MapFileIO.save(currentMapPath, grid);
            statusMessage = "Saved map";
        } catch (final IOException e) {
            statusMessage = "Save failed: " + e.getMessage();
            Gdx.app.error("map-editor", statusMessage, e);
        }
    }

    private void loadMap() {
        try {
            final MapGrid loaded = MapFileIO.load(currentMapPath);
            copyGrid(loaded);
            statusMessage = "Loaded map";
            centerCamera();
        } catch (final IOException e) {
            statusMessage = "Load failed: " + e.getMessage();
            Gdx.app.error("map-editor", statusMessage, e);
        }
    }

    private void copyGrid(final MapGrid source) {
        grid.resize(source.width(), source.height(), source.getDefaultTerrainId());
        grid.setDefaultTerrainId(source.getDefaultTerrainId());
        for (int y = 0; y < source.height(); y++) {
            for (int x = 0; x < source.width(); x++) {
                final MapCell cell = source.get(x, y);
                grid.setTerrain(x, y, cell.getTerrainId());
                grid.setFurniture(x, y, cell.getFurnitureId());
            }
        }
    }

    private void replaceGrid(final MapGrid source) {
        copyGrid(source);
    }

    private void openMapgenPicker() {
        if (mapgenPicker.isOpen() || loadingMapgenCatalog) {
            return;
        }
        if (!mapgenPreviewService.hasDataRoots()) {
            statusMessage = "No data roots (set -Dcdda.data.roots=...)";
            return;
        }
        if (mapgenPreviewService.isLoaded()) {
            showMapgenPicker();
            return;
        }
        loadingMapgenCatalog = true;
        statusMessage = "Loading mapgen catalog…";
        new Thread(() -> {
            try {
                mapgenPreviewService.ensureLoaded(MapgenScanOptions.defaults());
                Gdx.app.postRunnable(() -> {
                    loadingMapgenCatalog = false;
                    logMapgenLoadWarnings();
                    showMapgenPicker();
                });
            } catch (final IOException e) {
                Gdx.app.postRunnable(() -> {
                    loadingMapgenCatalog = false;
                    statusMessage = "Mapgen load failed: " + e.getMessage();
                    Gdx.app.error("mapgen", statusMessage, e);
                });
            }
        }, "mapgen-catalog-load").start();
    }

    private void showMapgenPicker() {
        if (mapgenPreviewService.getCatalog().runnableOnly().isEmpty()) {
            statusMessage = "No json mapgens found";
            return;
        }
        mapgenPicker.open(mapgenPreviewService.getCatalog());
    }

    private void pollMapgenPickerSelection() {
        final Optional<JsonMapgenDefinition> selection = mapgenPicker.takeSelection();
        selection.ifPresent(this::applyMapgenSelection);
    }

    private void applyMapgenSelection(final JsonMapgenDefinition definition) {
        if (!definition.isJsonPreviewSupported()) {
            statusMessage = "Builtin mapgen not supported";
            return;
        }
        try {
            statusMessage = "Generating " + definition.displayName() + "…";
            final MapgenPreviewService.MapgenPreviewResult result = mapgenPreviewService.generate(
                definition,
                gameData,
                new JsonMapgenRunOptions()
            );
            replaceGrid(result.getGrid());
            centerCamera();
            statusMessage = "Mapgen: " + definition.displayName()
                + " (" + grid.width() + "x" + grid.height() + ")";
            for (final String warning : result.getRunWarnings()) {
                Gdx.app.log("mapgen", warning);
            }
            appendValidationSummary(runGameDataValidation(tileset));
        } catch (final RuntimeException e) {
            statusMessage = "Mapgen failed: " + e.getMessage();
            Gdx.app.error("mapgen", statusMessage, e);
        }
    }

    private void logMapgenLoadWarnings() {
        for (final String warning : mapgenPreviewService.getLoadWarnings()) {
            Gdx.app.log("mapgen", warning);
        }
    }

    private void drawMapgenCatalogLoadingOverlay() {
        loadingOverlay.update(Gdx.graphics.getDeltaTime());
        loadingOverlay.draw(
            batch,
            font,
            whitePixel,
            0,
            0,
            viewportPixelWidth(),
            viewportPixelHeight(),
            "Loading mapgen catalog",
            "Scanning palettes and mapgen JSON"
        );
    }


    private void reloadTileset() {
        if (tilesetIds.isEmpty()) {
            loadDefaultTileset();
            return;
        }
        loadTilesetAtIndex(tilesetIndex);
    }

    private void previousTileset() {
        if (tilesetIds.size() <= 1) {
            return;
        }
        loadTilesetAtIndex((tilesetIndex - 1 + tilesetIds.size()) % tilesetIds.size());
    }

    private void nextTileset() {
        if (tilesetIds.size() <= 1) {
            return;
        }
        loadTilesetAtIndex((tilesetIndex + 1) % tilesetIds.size());
    }

    private void loadTilesetAtIndex(final int index) {
        if (tilesetRegistry == null || tilesetIds.isEmpty()) {
            return;
        }
        tilesetIndex = index;
        loadingTilesetId = tilesetIds.get(tilesetIndex);
        loadingTileset = true;
        statusMessage = "Loading " + loadingTilesetId + "…";
        if (tileset != null) {
            tileset.dispose();
            tileset = null;
        }
        palette.setTileset(null);
        cancelLoadSession();
        loadSession = TilesetLoadSession.start(
            tilesetRegistry,
            loadingTilesetId,
            TilesetLoadOptions.defaults(),
            modTilesetRegistry
        );
    }

    private void cycleGridPreset() {
        gridPresetIndex = (gridPresetIndex + 1) % GRID_PRESETS.length;
        final int width = GRID_PRESETS[gridPresetIndex][0];
        final int height = GRID_PRESETS[gridPresetIndex][1];
        grid.resize(width, height, grid.getDefaultTerrainId());
        centerCamera();
        statusMessage = "Grid resized to " + width + "x" + height;
    }

    private void loadDefaultTileset() {
        cancelLoadSession();
        if (tileset != null) {
            tileset.dispose();
            tileset = null;
        }
        try {
            tilesetRegistry = TilesetDiscovery.build();
            modTilesetRegistry = ModTilesetDiscovery.build();
            tilesetIds = buildTilesetIds(tilesetRegistry);
            if (tilesetIds.isEmpty()) {
                loadingTileset = false;
                statusMessage = "No tileset found (set -Dcdda.gfx.roots=...)";
                palette.setTileset(null);
                return;
            }
            int index = indexOfPreferredTileset(tilesetRegistry);
            if (index < 0) {
                index = 0;
            }
            loadTilesetAtIndex(index);
        } catch (final Exception e) {
            loadingTileset = false;
            statusMessage = "Tileset load failed: " + e.getMessage();
            Gdx.app.error("map-editor", statusMessage, e);
        }
    }

    private static List<String> buildTilesetIds(final TilesetRegistry registry) {
        final List<String> ids = new ArrayList<>();
        for (final TilesetOption option : registry.getOptions()) {
            if (registry.contains(option.getId())) {
                ids.add(option.getId());
            }
        }
        return ids;
    }

    private int indexOfPreferredTileset(final TilesetRegistry registry) {
        for (final String preferred : PREFERRED_TILESET_IDS) {
            final int index = tilesetIds.indexOf(preferred);
            if (index >= 0 && registry.contains(preferred)) {
                return index;
            }
        }
        return tilesetIds.isEmpty() ? -1 : 0;
    }

    private MapGrid createCheckerboardGrid() {
        final MapGrid checker = new MapGrid(20, 20, "t_dirt");
        for (int y = 0; y < checker.height(); y++) {
            for (int x = 0; x < checker.width(); x++) {
                checker.setTerrain(x, y, ((x + y) % 2 == 0) ? "t_dirt" : "t_grass");
            }
        }
        return checker;
    }

    private void updateAnimationFrame() {
        animationTick = System.currentTimeMillis() / ANIMATION_TICK_MS;
    }

    private void centerCamera() {
        final float tilePx = tilePixelSize();
        final float gridW = grid.width() * tilePx;
        final float gridH = grid.height() * tilePx;
        final float areaBottom = MapEditorToolbar.HEIGHT + 16;
        final float areaTop = viewportPixelHeight() - hudHeight() - 8;
        final float baseY = (areaBottom + areaTop - gridH) / 2f;
        cameraY = baseY - hudHeight();
        cameraX = (canvasWidth() - gridW) / 2f;
    }

    private int canvasWidth() {
        return Math.max(1, viewportPixelWidth() - MapPalettePanel.WIDTH);
    }

    private float tilePixelSize() {
        if (tileset == null) {
            return Math.max(16f, viewZoom * 16f);
        }
        final int pixelScale = Math.max(1, Math.round(tileset.getTileInfo().getPixelScale()));
        return Math.max(1f, viewZoom * pixelScale * tileset.getTileInfo().getWidth());
    }

    private void adjustViewZoom(final float zoomDelta, final int anchorX, final int anchorY) {
        final float oldTilePx = tilePixelSize();
        final float mapX = (anchorX - cameraX) / oldTilePx;
        final float gridTopOld = cameraY + hudHeight() + grid.height() * oldTilePx;
        final float mapYFromTop = (gridTopOld - anchorY) / oldTilePx;

        viewZoom = MathUtils.clamp(viewZoom - zoomDelta, MIN_VIEW_ZOOM, MAX_VIEW_ZOOM);

        final float newTilePx = tilePixelSize();
        cameraX = anchorX - mapX * newTilePx;
        cameraY = anchorY - hudHeight() - grid.height() * newTilePx + mapYFromTop * newTilePx;
    }

    private int gridZoomAnchorX() {
        return canvasWidth() / 2;
    }

    private int gridZoomAnchorY() {
        return Math.round((gridBaseY() + gridTopY()) * 0.5f);
    }

    private String formatViewZoom() {
        if (Math.abs(viewZoom - Math.round(viewZoom)) < 0.05f) {
            return String.format(Locale.ROOT, "%.0fx", viewZoom);
        }
        return String.format(Locale.ROOT, "%.1fx", viewZoom);
    }

    private void applyProjection() {
        batch.getProjectionMatrix().setToOrtho2D(
            0f,
            0f,
            viewportPixelWidth(),
            viewportPixelHeight()
        );
    }

    private static int viewportPixelWidth() {
        return Gdx.graphics.getBackBufferWidth();
    }

    private static int viewportPixelHeight() {
        return Gdx.graphics.getBackBufferHeight();
    }

    private static int hudHeight() {
        return HUD_MARGIN + HUD_LINES * 16 + 8;
    }

    private static final class CellCoord {
        private final int x;
        private final int y;

        private CellCoord(final int x, final int y) {
            this.x = x;
            this.y = y;
        }
    }
}
