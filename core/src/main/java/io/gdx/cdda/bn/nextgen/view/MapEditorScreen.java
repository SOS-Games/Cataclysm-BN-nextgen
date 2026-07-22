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
import io.gdx.cdda.bn.nextgen.gamedata.cache.LoadTiming;
import io.gdx.cdda.bn.nextgen.gamedata.load.GameDataLoadOptions;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.validate.GameDataValidationException;
import io.gdx.cdda.bn.nextgen.gamedata.validate.GameDataValidator;
import io.gdx.cdda.bn.nextgen.gamedata.validate.ValidationOptions;
import io.gdx.cdda.bn.nextgen.gamedata.validate.ValidationReport;
import io.gdx.cdda.bn.nextgen.map.MapCell;
import io.gdx.cdda.bn.nextgen.map.MapFileIO;
import io.gdx.cdda.bn.nextgen.map.MapGrid;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenPreviewService;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingPiece;
import io.gdx.cdda.bn.nextgen.mapgen.compose.BuildingPieceDebugFormatter;
import io.gdx.cdda.bn.nextgen.mapgen.compose.MapVolume;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtPieceRect;
import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenRunOptions;
import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;
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

import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGrid;
import io.gdx.cdda.bn.nextgen.worldgen.generate.OvermapGenerateResult;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridExporter;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapGridFactory;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoadResult;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainLoader;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainRegistry;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenPreviewService;
import io.gdx.cdda.bn.nextgen.worldgen.WorldgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.explore.LocalExploreMath;
import io.gdx.cdda.bn.nextgen.worldgen.explore.LocalExploreState;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionProfileSummary;
import io.gdx.cdda.bn.nextgen.worldgen.region.RegionSettingsDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.submap.OmtNeighborhoodStitcher;
import io.gdx.cdda.bn.nextgen.worldgen.submap.VisitResult;
import io.gdx.cdda.bn.nextgen.worldgen.placement.PlacedBuildingRecord;
import io.gdx.cdda.bn.nextgen.worldgen.visit.ZLevelResolver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Map editor with terrain palette and paint gestures (M3). */
public final class MapEditorScreen {

    private static final long ANIMATION_TICK_MS = 17L;
    private static final int HUD_MARGIN = 8;
    private static final int HUD_LINES = 7;
    private static final Path DEFAULT_MAP_PATH = Paths.get("maps/autosave.json");
    private static final Path DEFAULT_OVERMAP_EXPORT_PATH = Paths.get("maps/overmap_export.json");
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
    private static final float MIN_VIEW_ZOOM = 0.125f;
    private static final float MAX_VIEW_ZOOM = 8f;
    /** Per wheel notch; fractional {@code amountY} from trackpads scales smoothly. */
    private static final float ZOOM_SCROLL_SENSITIVITY = 0.1f;
    private static final float ZOOM_KEY_SENSITIVITY = 0.15f;

    private static final float OMT_BASE_CELL_PX = 24f;
    private static final float OVERMAP_SYMBOL_MIN_CELL_PX = 10f;
    private static final int[] OVERMAP_SIZE_PRESETS = { 8, 16, 32, 64, 128, 180, 256 };
    private static final int DEFAULT_OVERMAP_SIZE = 128;
    private static final int LARGE_OVERMAP_CONFIRM_SIZE = 180;

    private final SpriteBatch batch;
    private final BitmapFont font;
    private MapGrid grid;
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
    private long tilesetLoadStartNs;
    private int gridPresetIndex = 1;
    private Path currentMapPath = DEFAULT_MAP_PATH;
    private String statusMessage;
    private float viewZoom = 2f;
    private float cameraX;
    private float cameraY;
    private boolean animationPlayback = true;
    private long animationTick;
    private boolean showFurnitureLayer = true;
    private boolean showZCutaway;
    private boolean showSpawnOverlay;
    private boolean showOmtPieceBorders = true;
    private Map<Integer, List<SpawnMarker>> spawnMarkersByZ = Collections.emptyMap();

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

    private final WorldgenPreviewService worldgenPreviewService;
    private final MapgenPickerDialog mapgenPicker = new MapgenPickerDialog();
    private final RegionPickerDialog regionPicker = new RegionPickerDialog();
    private final ImportFailedDialog importFailedDialog = new ImportFailedDialog();
    private final KeybindHelpDialog keybindHelp = new KeybindHelpDialog();
    private MapVolume mapVolume;
    private CityBuildingDefinition activeBuilding;
    private boolean loadingMapgenCatalog;
    private boolean generatingOvermap;
    private int pendingLargeOvermapSize = -1;

    private EditorMode editorMode = EditorMode.SUBMAP;
    private OvermapGrid overmapGrid;
    private OvermapTerrainRegistry overmapTerrainRegistry = new OvermapTerrainRegistry();
    private boolean overmapTerrainLoaded;
    private int selectedOmtX = -1;
    private int selectedOmtY = -1;
    private long overmapSeed = 12345L;
    private String overmapRegionId = "default";
    private int overmapSize = DEFAULT_OVERMAP_SIZE;
    private boolean overmapSmokeLayoutPending = true;
    private OvermapGenerateResult lastOvermapGeneration;
    private final LocalExploreState localExplore = new LocalExploreState();
    private boolean exploreRestitchPending;

    public MapEditorScreen(final SpriteBatch batch) {
        this(batch, new WorldgenPreviewService());
    }

    public MapEditorScreen(final SpriteBatch batch, final WorldgenPreviewService worldgenPreviewService) {
        this.batch = batch;
        this.worldgenPreviewService = worldgenPreviewService == null
            ? new WorldgenPreviewService()
            : worldgenPreviewService;
        this.font = new BitmapFont();
        this.font.setUseIntegerPositions(true);
        this.whitePixel = createWhitePixel();
        this.grid = createCheckerboardGrid();
        this.statusMessage = "Map editor";
        loadGameData();
        loadOvermapTerrain();
        initOvermapGrid();
        loadDefaultTileset();
        palette.setSelectedTerrainId("t_grass");
        centerCamera();
        if (this.worldgenPreviewService.isLoaded()) {
            overmapTerrainRegistry = this.worldgenPreviewService.getOvermapTerrainRegistry();
            overmapTerrainLoaded = overmapTerrainRegistry.size() > 0;
            syncOvermapRegionFromRegistry();
            LoadTiming.log("session", "MapEditorScreen attached to already-loaded worldgen");
        }
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
            if (editorMode == EditorMode.OVERMAP) {
                drawOvermapGrid();
                drawOvermapSelection();
            } else {
                drawGrid();
                drawSpawnOverlay();
                drawOmtPieceBorders();
                drawHoverCell();
            }
        }
        drawPalette();
        drawToolbar();
        drawHud();
        if (loadingTileset) {
            drawTilesetLoadingOverlay();
        } else if (loadingMapgenCatalog || generatingOvermap) {
            // Prefer tileset overlay when both are busy — gfx load dominates cold start.
            drawBusyLoadingOverlay();
        }
        mapgenPicker.render(batch, font, whitePixel, viewportPixelWidth(), viewportPixelHeight());
        regionPicker.render(batch, font, whitePixel, viewportPixelWidth(), viewportPixelHeight());
        importFailedDialog.render(batch, font, whitePixel, viewportPixelWidth(), viewportPixelHeight());
        keybindHelp.render(batch, font, whitePixel, viewportPixelWidth(), viewportPixelHeight());
        pollMapgenPickerSelection();
        pollRegionPickerSelection();
        batch.end();
    }

    public boolean onEscape() {
        if (keybindHelp.isOpen()) {
            keybindHelp.close();
            return true;
        }
        if (importFailedDialog.isOpen()) {
            importFailedDialog.dismiss();
            return true;
        }
        if (mapgenPicker.isOpen()) {
            if (mapgenPicker.isFilterEditing() && !mapgenPicker.getFilterQuery().isEmpty()) {
                mapgenPicker.clearFilter();
                return true;
            }
            mapgenPicker.cancel();
            return true;
        }
        if (regionPicker.isOpen()) {
            if (regionPicker.isFilterEditing() && !regionPicker.getFilterQuery().isEmpty()) {
                regionPicker.clearFilter();
                return true;
            }
            regionPicker.cancel();
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
        if (editorMode == EditorMode.OVERMAP && selectedOmtX >= 0) {
            selectedOmtX = -1;
            selectedOmtY = -1;
            statusMessage = "Overmap selection cleared";
            return true;
        }
        return false;
    }

    public boolean onKeyDown(final int keycode) {
        if (importFailedDialog.isOpen()) {
            return importFailedDialog.onKeyDown(keycode);
        }
        if (keybindHelp.isOpen()) {
            return keybindHelp.onKeyDown(keycode);
        }
        if (mapgenPicker.isOpen()) {
            if (mapgenPicker.onKeyDown(keycode)) {
                return true;
            }
            return !mapgenPicker.isFilterEditing();
        }
        if (regionPicker.isOpen()) {
            if (regionPicker.onKeyDown(keycode)) {
                return true;
            }
            return !regionPicker.isFilterEditing();
        }
        if (palette.isFilterEditing() && palette.onKeyDown(keycode)) {
            return true;
        }
        if (keycode == Keys.F1 || keycode == Keys.H) {
            toggleKeybindHelp();
            return true;
        }
        if (keycode == Keys.M && overmapTerrainLoaded && overmapGrid != null) {
            toggleEditorMode();
            return true;
        }
        if (editorMode == EditorMode.OVERMAP
            && (keycode == Keys.ENTER || keycode == Keys.NUMPAD_ENTER)) {
            visitSelectedOmt();
            return true;
        }
        if (editorMode == EditorMode.OVERMAP) {
            if (keycode == Keys.LEFT_BRACKET) {
                cycleOvermapSize(-1);
                return true;
            }
            if (keycode == Keys.RIGHT_BRACKET) {
                cycleOvermapSize(1);
                return true;
            }
            if (keycode == Keys.R) {
                requestOvermapRegeneration();
                return true;
            }
            if (keycode == Keys.G) {
                openRegionPicker();
                return true;
            }
        }
        if (mapVolume != null) {
            if (keycode == Keys.PAGE_UP || keycode == Keys.LEFT_BRACKET) {
                switchBuildingFloor(true);
                return true;
            }
            if (keycode == Keys.PAGE_DOWN || keycode == Keys.RIGHT_BRACKET) {
                switchBuildingFloor(false);
                return true;
            }
            if (keycode == Keys.T && mapVolume.floorCount() > 1) {
                showZCutaway = !showZCutaway;
                statusMessage = "Upper-floor cutaway: " + (showZCutaway ? "on" : "off");
                return true;
            }
        }
        if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT)) {
            final boolean shift = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)
                || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
            if (shift && keycode == Keys.C) {
                if (editorMode == EditorMode.OVERMAP && overmapGrid != null) {
                    copyOvermapDebug();
                    return true;
                }
                if (mapVolume != null) {
                    copyBuildingPieceDebug();
                    return true;
                }
            }
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
            return false;
        }

        final float panStep = tilePixelSize() * 2f;
        if (keycode == Keys.LEFT) {
            cameraX -= panStep;
            maybeRestitchExploreFromCamera();
            return true;
        }
        if (keycode == Keys.RIGHT) {
            cameraX += panStep;
            maybeRestitchExploreFromCamera();
            return true;
        }
        if (keycode == Keys.UP) {
            cameraY += panStep;
            maybeRestitchExploreFromCamera();
            return true;
        }
        if (keycode == Keys.DOWN) {
            cameraY -= panStep;
            maybeRestitchExploreFromCamera();
            return true;
        }
        if (keycode == Keys.EQUALS || keycode == Keys.PLUS) {
            adjustViewZoomByKey(true, gridZoomAnchorX(), gridZoomAnchorY());
            return true;
        }
        if (keycode == Keys.MINUS) {
            adjustViewZoomByKey(false, gridZoomAnchorX(), gridZoomAnchorY());
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
        if (keycode == Keys.F) {
            showFurnitureLayer = !showFurnitureLayer;
            statusMessage = "Furniture layer: " + (showFurnitureLayer ? "on" : "off");
            return true;
        }
        if (keycode == Keys.L) {
            final PaletteBrushLayer layer = palette.cycleBrushLayer();
            statusMessage = "Brush layer: " + layer.hudLabel();
            return true;
        }
        if (keycode == Keys.O) {
            toggleSpawnOverlay();
            return true;
        }
        if (keycode == Keys.B) {
            toggleOmtPieceBorders();
            return true;
        }
        if (keycode == Keys.F5) {
            reloadTileset();
            return true;
        }
        if (keycode == Keys.G) {
            cycleGridPreset();
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
        if (keybindHelp.isOpen()) {
            return true;
        }
        if (mapgenPicker.isOpen() && mapgenPicker.onKeyTyped(character)) {
            return true;
        }
        if (regionPicker.isOpen() && regionPicker.onKeyTyped(character)) {
            return true;
        }
        if (palette.isFilterEditing()) {
            return palette.onKeyTyped(character);
        }
        return false;
    }

    public boolean onScroll(final float amountY) {
        if (importFailedDialog.isOpen()) {
            return true;
        }
        if (keybindHelp.isOpen()) {
            return keybindHelp.onScroll(amountY);
        }
        if (mapgenPicker.isOpen()) {
            return mapgenPicker.onScroll(amountY);
        }
        if (regionPicker.isOpen()) {
            return true;
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
            adjustViewZoomByScroll(amountY, mouseX, mouseY);
        }
        return true;
    }

    public boolean onTouchDown(final int screenX, final int screenY, final int button) {
        if (importFailedDialog.isOpen()) {
            return importFailedDialog.onTouchDown(
                screenX,
                ScreenInput.fromInputY(screenY),
                viewportPixelWidth(),
                viewportPixelHeight()
            );
        }
        if (keybindHelp.isOpen()) {
            return keybindHelp.onTouchDown(
                screenX,
                ScreenInput.fromInputY(screenY),
                viewportPixelWidth(),
                viewportPixelHeight()
            );
        }
        if (mapgenPicker.isOpen()) {
            return mapgenPicker.onTouchDown(
                screenX,
                ScreenInput.fromInputY(screenY),
                viewportPixelWidth(),
                viewportPixelHeight()
            );
        }
        if (regionPicker.isOpen()) {
            return regionPicker.onTouchDown(
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
        palette.cancelFilterEdit();
        if (isToolbarPoint(screenX, y)) {
            handleToolbarHit(toolbar.hitTest(screenX, y, canvasWidth()));
            return true;
        }
        if (loadingTileset) {
            return true;
        }
        if (editorMode == EditorMode.OVERMAP) {
            if (button == Buttons.LEFT) {
                selectOmtAt(screenX, y);
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
        if (mapgenPicker.isOpen() || regionPicker.isOpen() || keybindHelp.isOpen() || loadingMapgenCatalog || generatingOvermap) {
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
            maybeRestitchExploreFromCamera();
            return true;
        }
        if (painting) {
            paintAt(screenX, y);
            return true;
        }
        return false;
    }

    public boolean onTouchUp(final int screenX, final int screenY, final int button) {
        if (mapgenPicker.isOpen() || regionPicker.isOpen() || keybindHelp.isOpen() || loadingMapgenCatalog || generatingOvermap) {
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
                adjustViewZoomByKey(true, gridZoomAnchorX(), gridZoomAnchorY());
                break;
            case ZOOM_OUT:
                adjustViewZoomByKey(false, gridZoomAnchorX(), gridZoomAnchorY());
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
            case CYCLE_EDITOR_MODE:
                if (overmapTerrainLoaded && overmapGrid != null) {
                    toggleEditorMode();
                } else {
                    statusMessage = "Overmap mode unavailable — no overmap terrain loaded";
                }
                break;
            case TOGGLE_BRUSH_LAYER:
                palette.cycleBrushLayer();
                statusMessage = "Brush layer: " + palette.getBrushLayer().hudLabel();
                break;
            case TOGGLE_SPAWN_OVERLAY:
                toggleSpawnOverlay();
                break;
            case TOGGLE_OMT_PIECE_BORDERS:
                toggleOmtPieceBorders();
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
        if (localExplore.isActive() && editorMode == EditorMode.SUBMAP) {
            return true;
        }
        return toolbar.getActiveTool() == MapEditorToolbar.Tool.PAN || Gdx.input.isKeyPressed(Keys.SPACE);
    }

    private boolean isToolbarPoint(final int screenX, final int screenY) {
        return screenX >= 0
            && screenX < canvasWidth()
            && screenY >= 8
            && screenY <= 8 + MapEditorToolbar.HEIGHT + 4;
    }

    private void drawToolbar() {
        toolbar.setFurnitureBrushLayer(palette.getBrushLayer() == PaletteBrushLayer.FURNITURE);
        toolbar.setSpawnOverlayOn(showSpawnOverlay);
        toolbar.setOmtPieceBordersOn(showOmtPieceBorders);
        toolbar.render(batch, font, whitePixel, canvasWidth());
    }

    private void drawSpawnOverlay() {
        if (!showSpawnOverlay) {
            return;
        }
        final List<SpawnMarker> markers = activeSpawnMarkers();
        if (markers.isEmpty()) {
            return;
        }
        final float tilePx = tilePixelSize();
        final int viewWidth = canvasWidth();
        final int viewH = viewportPixelHeight();
        final float baseY = gridBaseY();
        final float topY = gridTopY();
        final int firstCol = Math.max(0, (int) Math.floor((-cameraX) / tilePx) - 1);
        final int lastCol = Math.min(grid.width(), (int) Math.ceil((viewWidth - cameraX) / tilePx) + 1);
        final int firstRow = Math.max(0, (int) Math.floor((topY - viewH) / tilePx));
        final int lastRow = Math.min(grid.height(), (int) Math.ceil((topY - baseY) / tilePx));
        SpawnMarkerOverlay.draw(
            batch,
            font,
            whitePixel,
            markers,
            cameraX,
            tilePx,
            grid.width(),
            grid.height(),
            firstCol,
            lastCol,
            firstRow,
            lastRow,
            this::cellBottomY
        );
    }

    private void toggleSpawnOverlay() {
        showSpawnOverlay = !showSpawnOverlay;
        statusMessage = "Spawn overlay: " + (showSpawnOverlay ? "on" : "off");
    }

    private void toggleOmtPieceBorders() {
        showOmtPieceBorders = !showOmtPieceBorders;
        statusMessage = "Chunk borders: " + (showOmtPieceBorders ? "on" : "off");
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

    private void drawOmtPieceBorders() {
        if (!showOmtPieceBorders || mapVolume == null) {
            return;
        }
        final List<OmtPieceRect> layouts = mapVolume.getActivePieceLayouts();
        if (layouts.size() < 2) {
            return;
        }
        final float tilePx = tilePixelSize();
        final float lineW = Math.max(1f, tilePx * 0.1f);
        batch.setColor(0.95f, 0.55f, 0.15f, 0.85f);
        for (final OmtPieceRect piece : layouts) {
            drawRectOutline(
                cameraX + piece.getOriginX() * tilePx,
                gridTopY() - piece.getOriginY() * tilePx,
                piece.getWidth() * tilePx,
                piece.getHeight() * tilePx,
                lineW
            );
        }
        batch.setColor(Color.WHITE);
    }

    private void drawRectOutline(
        final float left,
        final float top,
        final float width,
        final float height,
        final float lineW
    ) {
        final float bottom = top - height;
        batch.draw(whitePixel, left, top - lineW, width, lineW);
        batch.draw(whitePixel, left, bottom, width, lineW);
        batch.draw(whitePixel, left, bottom, lineW, height);
        batch.draw(whitePixel, left + width - lineW, bottom, lineW, height);
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
        final LoadTiming.Session session = new LoadTiming.Session("startup/game-data");
        try {
            gameData = GameDataLoader.loadCore(GameDataLoadOptions.defaults());
            session.phase("loadCore terrain=" + gameData.getTerrain().size()
                + " furniture=" + gameData.getFurniture().size());
            palette.setTerrainRegistry(gameData.getTerrain());
            palette.setFurnitureRegistry(gameData.getFurniture());
            statusMessage = "Loaded " + gameData.getTerrain().size() + " terrain ids";
            appendValidationSummary(runGameDataValidation(null));
            session.phase("validation");
        } catch (final IOException e) {
            statusMessage = "Game data load failed: " + e.getMessage();
            Gdx.app.error("map-editor", statusMessage, e);
        } finally {
            session.done();
        }
    }

    private void loadOvermapTerrain() {
        if (worldgenPreviewService.isLoaded()) {
            return;
        }
        final LoadTiming.Session session = new LoadTiming.Session("startup/overmap-terrain");
        try {
            final OvermapTerrainLoadResult result = OvermapTerrainLoader.load(OvermapTerrainScanOptions.defaults());
            overmapTerrainRegistry = result.getRegistry();
            overmapTerrainLoaded = overmapTerrainRegistry.size() > 0;
            logWarningSummary("overmap", result.getWarnings());
            if (overmapTerrainLoaded) {
                Gdx.app.log("overmap", "Loaded " + overmapTerrainRegistry.size() + " overmap terrain ids");
                worldgenPreviewService.seedOvermapTerrainRegistry(overmapTerrainRegistry, result.getWarnings());
            }
            session.phase("ids=" + overmapTerrainRegistry.size());
        } catch (final IOException e) {
            overmapTerrainLoaded = false;
            Gdx.app.error("map-editor", "Overmap terrain load failed: " + e.getMessage(), e);
        } finally {
            session.done();
        }
    }

    private MapgenPreviewService mapgenService() {
        return worldgenPreviewService.getMapgenPreviewService();
    }

    private void initOvermapGrid() {
        try {
            final URL fixtureUrl = MapEditorScreen.class.getResource("/worldgen-fixtures/overmaps/test_8x8.json");
            if (fixtureUrl != null) {
                overmapGrid = OvermapGridFactory.fromJsonFile(Paths.get(fixtureUrl.toURI()));
                overmapSize = overmapGrid.width();
                overmapSmokeLayoutPending = false;
                return;
            }
        } catch (final URISyntaxException | IOException e) {
            Gdx.app.log("map-editor", "Demo overmap fixture unavailable: " + e.getMessage());
        }
        overmapGrid = OvermapGridFactory.empty(overmapSize, overmapSize, defaultOvermapFillId());
        overmapSmokeLayoutPending = true;
    }

    private String defaultOvermapFillId() {
        if (overmapTerrainRegistry.contains("field")) {
            return "field";
        }
        if (overmapTerrainRegistry.contains("forest")) {
            return "forest";
        }
        return overmapTerrainRegistry.contains("open_air") ? "open_air" : "field";
    }

    private void refreshOvermapSmokeLayout() {
        if (!overmapSmokeLayoutPending || !worldgenPreviewService.isLoaded()) {
            return;
        }
        regenerateOvermapLayout();
    }

    private void requestOvermapRegeneration() {
        overmapSeed = System.nanoTime() ^ 0xC0DA011L;
        ensureWorldgenLoaded(this::regenerateOvermapLayout);
    }

    private void regenerateOvermapLayout() {
        worldgenPreviewService.setWorldSeed(overmapSeed);
        worldgenPreviewService.setRegionId(overmapRegionId);
        applyOvermapCacheSizing();
        if (overmapSize > 32) {
            startAsyncOvermapGeneration();
            return;
        }
        applyOvermapGeneration(worldgenPreviewService.generateOvermap(overmapSize, overmapSize));
    }

    private void startAsyncOvermapGeneration() {
        if (generatingOvermap) {
            return;
        }
        generatingOvermap = true;
        statusMessage = "Generating overmap " + overmapSize + "×" + overmapSize + "…";
        final int size = overmapSize;
        final long seed = overmapSeed;
        new Thread(() -> {
            try {
                worldgenPreviewService.setWorldSeed(seed);
                worldgenPreviewService.setRegionId(overmapRegionId);
                final OvermapGenerateResult generated = worldgenPreviewService.generateOvermap(size, size);
                Gdx.app.postRunnable(() -> {
                    generatingOvermap = false;
                    applyOvermapGeneration(generated);
                });
            } catch (final OutOfMemoryError error) {
                Gdx.app.postRunnable(() -> {
                    generatingOvermap = false;
                    statusMessage = "Out of memory generating " + size + "×" + size
                        + " — try a smaller overmap";
                    Gdx.app.error("overmap", "OOM during generate", error);
                });
            } catch (final RuntimeException error) {
                Gdx.app.postRunnable(() -> {
                    generatingOvermap = false;
                    showImportFailed(
                        "Overmap generation failed",
                        "Could not generate " + size + "×" + size + " overmap.",
                        error
                    );
                });
            }
        }, "overmap-generate").start();
    }

    private void applyOvermapGeneration(final OvermapGenerateResult generated) {
        overmapGrid = generated.getGrid();
        lastOvermapGeneration = generated;
        overmapSmokeLayoutPending = false;
        selectedOmtX = -1;
        selectedOmtY = -1;
        localExplore.clear();
        centerCamera();
        statusMessage = "Generated overmap " + overmapSize + "×" + overmapSize + " — "
            + generated.getCityBuildingsPlaced() + " buildings, "
            + generated.getUrbanOmtsPlaced() + " urban, "
            + generated.getStaticSpecialsPlaced() + " specials, "
            + generated.getRiverCellsCarved() + " river, "
            + generated.getRoadCellsPlaced() + " road, "
            + generated.getForestTrailCellsPlaced() + " trail, "
            + generated.getUndergroundCellsPlaced() + " underground"
            + " — region=" + overmapRegionId
            + ", seed=" + overmapSeed;
    }

    private void applyOvermapCacheSizing() {
        // Walkaround uses a 12×12 OMT window (144 cells); keep headroom for sliding.
        final int walkaroundMin = OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_WIDTH
            * OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_HEIGHT
            * 2;
        if (overmapSize >= 256) {
            worldgenPreviewService.setSubmapCacheCapacity(Math.max(512, walkaroundMin));
        } else if (overmapSize >= 180) {
            worldgenPreviewService.setSubmapCacheCapacity(Math.max(256, walkaroundMin));
        } else if (overmapSize >= 64) {
            worldgenPreviewService.setSubmapCacheCapacity(Math.max(256, walkaroundMin));
        } else {
            worldgenPreviewService.setSubmapCacheCapacity(Math.max(128, walkaroundMin));
        }
        worldgenPreviewService.setVolumeCacheCapacity(overmapSize >= 128 ? 24 : 16);
    }

    private void cycleOvermapSize(final int direction) {
        if (generatingOvermap) {
            return;
        }
        final int presetIndex = overmapSizePresetIndex(overmapSize);
        final int nextIndex = MathUtils.clamp(presetIndex + direction, 0, OVERMAP_SIZE_PRESETS.length - 1);
        final int nextSize = OVERMAP_SIZE_PRESETS[nextIndex];
        if (nextSize == overmapSize) {
            statusMessage = "Overmap size already at " + overmapSize + "×" + overmapSize;
            return;
        }
        if (nextSize >= LARGE_OVERMAP_CONFIRM_SIZE && pendingLargeOvermapSize != nextSize) {
            pendingLargeOvermapSize = nextSize;
            statusMessage = nextSize + "×" + nextSize + " is slow — press "
                + (direction > 0 ? "]" : "[")
                + " again to confirm";
            return;
        }
        pendingLargeOvermapSize = -1;
        overmapSize = nextSize;
        selectedOmtX = -1;
        selectedOmtY = -1;
        if (worldgenPreviewService.isLoaded()) {
            regenerateOvermapLayout();
            return;
        }
        overmapGrid = OvermapGridFactory.empty(overmapSize, overmapSize, defaultOvermapFillId());
        overmapSmokeLayoutPending = true;
        centerCamera();
        statusMessage = "Overmap size set to " + overmapSize + "×" + overmapSize + " — worldgen will fill on load";
    }

    private static int overmapSizePresetIndex(final int size) {
        for (int i = 0; i < OVERMAP_SIZE_PRESETS.length; i++) {
            if (OVERMAP_SIZE_PRESETS[i] == size) {
                return i;
            }
        }
        return 1;
    }

    /** Main-menu shortcut: jump straight into overmap worldgen preview. */
    public void openWorldgenMode() {
        localExplore.clear();
        editorMode = EditorMode.OVERMAP;
        statusMessage = "Loading worldgen…";
        ensureWorldgenLoaded(() -> {
            if (!overmapTerrainLoaded || overmapGrid == null) {
                editorMode = EditorMode.SUBMAP;
                statusMessage = "Overmap mode unavailable — no overmap terrain loaded";
                return;
            }
            enterOvermapMode();
        });
    }

    private void toggleEditorMode() {
        if (!overmapTerrainLoaded || overmapGrid == null) {
            statusMessage = "Overmap mode unavailable — no overmap terrain loaded";
            return;
        }
        editorMode = editorMode == EditorMode.SUBMAP ? EditorMode.OVERMAP : EditorMode.SUBMAP;
        if (editorMode == EditorMode.OVERMAP) {
            localExplore.clear();
        }
        if (editorMode == EditorMode.OVERMAP && overmapSmokeLayoutPending) {
            ensureWorldgenLoaded(this::enterOvermapMode);
            return;
        }
        if (editorMode == EditorMode.OVERMAP) {
            enterOvermapMode();
            return;
        }
        centerCamera();
        statusMessage = "Mode: SUBMAP";
    }

    private void enterOvermapMode() {
        refreshOvermapSmokeLayout();
        centerCamera();
        if (statusMessage == null || !statusMessage.startsWith("Generated overmap")) {
            statusMessage = "Mode: OVERMAP — click a cell with mapgens≥1, Enter to generate submap";
        }
    }

    private void selectOmtAt(final int screenX, final int screenY) {
        final CellCoord cell = screenToCell(screenX, screenY);
        if (cell == null) {
            return;
        }
        selectedOmtX = cell.x;
        selectedOmtY = cell.y;
        final String omtId = overmapGrid.getOmtId(selectedOmtX, selectedOmtY);
        final boolean visitable = mapgenService().isLoaded()
            && OvermapGridFactory.isVisitableOmt(omtId, mapgenService().getCatalog());
        statusMessage = "Selected OMT (" + selectedOmtX + "," + selectedOmtY + ") " + omtId
            + (visitable ? " — Enter to walkaround (neighborhood)" : " — no json submap (background tile)");
    }

    private void visitSelectedOmt() {
        if (selectedOmtX < 0 || selectedOmtY < 0 || overmapGrid == null) {
            statusMessage = "Select an OMT cell first";
            return;
        }
        ensureWorldgenLoaded(this::visitSelectedOmtLoaded);
    }

    private void visitSelectedOmtLoaded() {
        worldgenPreviewService.setGameData(gameData);
        worldgenPreviewService.setWorldSeed(overmapSeed);
        final String selectedOmtId = overmapGrid.getOmtId(selectedOmtX, selectedOmtY);
        final int visitZ = ZLevelResolver.visitZForOmt(selectedOmtId);
        final VisitResult result = worldgenPreviewService.visitNeighborhoodSize(
            overmapGrid,
            selectedOmtX,
            selectedOmtY,
            visitZ,
            OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_WIDTH,
            OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_HEIGHT
        );
        final String omtId = result.getOmtId();
        if (!result.hasGrid()) {
            if ("open_air".equals(omtId) || "field".equals(omtId) || "forest".equals(omtId)) {
                statusMessage = omtId + " is a background OMT — select a building/structure cell (mapgens≥1)";
            } else {
                statusMessage = "No json mapgen for " + omtId + " — try Ctrl+G mapgen picker for direct import";
            }
            logWarningSummary("worldgen", result.getWarnings());
            localExplore.clear();
            return;
        }
        replaceGrid(result.getGrid());
        setSpawnMarkers(result.getSpawnMarkers());
        mapVolume = null;
        activeBuilding = null;
        editorMode = EditorMode.SUBMAP;
        localExplore.activateFromPatch(
            result,
            OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_WIDTH,
            OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_HEIGHT,
            visitZ
        );
        centerCameraOnVisit(result);
        final String cacheNote = result.isFromCache() ? " (cached)" : "";
        statusMessage = "Explore " + omtId + " @ (" + selectedOmtX + "," + selectedOmtY + ",z=" + visitZ + ")"
            + " → " + grid.width() + "x" + grid.height() + " neighborhood"
            + cacheNote + " — left-drag to walk";
        logWarningSummary("worldgen", result.getWarnings());
        appendValidationSummary(runGameDataValidation(tileset));
    }

    private void maybeRestitchExploreFromCamera() {
        if (!localExplore.isActive()
            || editorMode != EditorMode.SUBMAP
            || overmapGrid == null
            || exploreRestitchPending) {
            return;
        }
        final float tilePx = tilePixelSize();
        final int stride = localExplore.omtStride();
        final int cellX = LocalExploreMath.focusCellX(cameraX, canvasWidth() / 2f, tilePx);
        final float areaBottom = MapEditorToolbar.HEIGHT + 16;
        final float areaTop = viewportPixelHeight() - hudHeight() - 8;
        final float viewMidY = (areaBottom + areaTop) / 2f;
        final int cellY = LocalExploreMath.focusCellY(
            cameraY,
            hudHeight(),
            grid.height(),
            viewMidY,
            tilePx
        );
        int focusX = LocalExploreMath.cellToOmt(cellX, localExplore.getPatchMinOmtX(), stride);
        int focusY = LocalExploreMath.cellToOmt(cellY, localExplore.getPatchMinOmtY(), stride);
        focusX = LocalExploreMath.clamp(focusX, 0, overmapGrid.width() - 1);
        focusY = LocalExploreMath.clamp(focusY, 0, overmapGrid.height() - 1);
        if (focusX == localExplore.getFocusOmtX() && focusY == localExplore.getFocusOmtY()) {
            return;
        }
        restitchExploreNeighborhood(focusX, focusY);
    }

    private void restitchExploreNeighborhood(final int focusOmtX, final int focusOmtY) {
        if (overmapGrid == null || !worldgenPreviewService.isLoaded()) {
            return;
        }
        exploreRestitchPending = true;
        try {
            final int oldMinX = localExplore.getPatchMinOmtX();
            final int oldMinY = localExplore.getPatchMinOmtY();
            final int oldHeight = grid.height();
            final float tilePx = tilePixelSize();
            final int stride = localExplore.omtStride();

            worldgenPreviewService.setGameData(gameData);
            worldgenPreviewService.setWorldSeed(overmapSeed);
            final VisitResult result = worldgenPreviewService.visitNeighborhoodSize(
                overmapGrid,
                focusOmtX,
                focusOmtY,
                localExplore.getVisitZ(),
                localExplore.getWindowWidth(),
                localExplore.getWindowHeight()
            );
            if (!result.hasGrid() || !result.isPatchVisit()) {
                return;
            }

            replaceGridKeepingExplore(result.getGrid());
            setSpawnMarkers(result.getSpawnMarkers());
            mapVolume = null;
            activeBuilding = null;

            cameraX += LocalExploreMath.cameraDeltaX(
                oldMinX,
                result.getPatchMinOmtX(),
                stride,
                tilePx
            );
            cameraY += LocalExploreMath.cameraDeltaY(
                oldMinY,
                result.getPatchMinOmtY(),
                oldHeight,
                grid.height(),
                stride,
                tilePx
            );
            // Re-anchor pan mid-drag so further drag deltas don't jump.
            if (panning) {
                panAnchorCameraX = cameraX;
                panAnchorCameraY = cameraY;
                panAnchorScreenX = lastPointerX;
                panAnchorScreenY = lastPointerY;
            }

            localExplore.updateAfterRestitch(result, focusOmtX, focusOmtY);
            selectedOmtX = focusOmtX;
            selectedOmtY = focusOmtY;
            final String cacheNote = result.isFromCache() ? " (cached)" : "";
            statusMessage = "Explore (" + focusOmtX + "," + focusOmtY + ",z=" + localExplore.getVisitZ() + ") "
                + result.getOmtId() + " → " + grid.width() + "x" + grid.height() + cacheNote;
        } finally {
            exploreRestitchPending = false;
        }
    }

    private void replaceGridKeepingExplore(final MapGrid source) {
        if (source == null) {
            return;
        }
        mapVolume = null;
        activeBuilding = null;
        grid = new MapGrid(source.width(), source.height(), source.getDefaultTerrainId());
        copyGrid(source);
    }

    private void ensureWorldgenLoaded(final Runnable onReady) {
        if (worldgenPreviewService.isLoaded()) {
            overmapTerrainRegistry = worldgenPreviewService.getOvermapTerrainRegistry();
            overmapTerrainLoaded = overmapTerrainRegistry.size() > 0;
            syncOvermapRegionFromRegistry();
            onReady.run();
            return;
        }
        if (loadingMapgenCatalog) {
            return;
        }
        if (!worldgenPreviewService.hasDataRoots()) {
            statusMessage = "No data roots (set -Dcdda.data.roots=...)";
            return;
        }
        loadingMapgenCatalog = true;
        statusMessage = "Loading worldgen data…";
        new Thread(() -> {
            final long wallNs = LoadTiming.start();
            try {
                worldgenPreviewService.ensureLoaded(WorldgenScanOptions.defaults());
                final long ms = LoadTiming.msSince(wallNs);
                LoadTiming.log("ui", "worldgen ensureLoaded wall " + ms + " ms");
                Gdx.app.postRunnable(() -> {
                    loadingMapgenCatalog = false;
                    overmapTerrainRegistry = worldgenPreviewService.getOvermapTerrainRegistry();
                    overmapTerrainLoaded = overmapTerrainRegistry.size() > 0;
                    syncOvermapRegionFromRegistry();
                    logMapgenLoadWarnings();
                    logWarningSummary("overmap", worldgenPreviewService.getOvermapLoadWarnings());
                    statusMessage = "Worldgen loaded in " + ms + " ms — see [load-timing] in console";
                    onReady.run();
                });
            } catch (final IOException e) {
                Gdx.app.postRunnable(() -> {
                    loadingMapgenCatalog = false;
                    showImportFailed("Worldgen load failed", "Could not load mapgen/overmap data.", e);
                });
            }
        }, "worldgen-load").start();
    }

    private void ensureMapgenCatalogLoaded(final Runnable onReady) {
        ensureWorldgenLoaded(onReady);
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
        logWarningSummary("game-data", report.getWarnings());
        logWarningSummary("game-data", report.getInfos());
    }

    private void appendValidationSummary(final ValidationReport report) {
        final int issues = report.getErrors().size() + report.getWarnings().size();
        if (issues > 0) {
            statusMessage += " | " + issues + " validation issue(s)";
        }
    }

    private static void logWarningSummary(final String tag, final List<String> warnings) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        if (warnings.size() == 1) {
            Gdx.app.log(tag, warnings.get(0));
            return;
        }
        Gdx.app.log(tag, warnings.size() + " messages (suppressed detail); e.g. " + warnings.get(0));
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

        for (final GridDrawPass pass : GridDrawPass.values()) {
            if (pass.isFurniture() && !showFurnitureLayer) {
                continue;
            }
            for (int y = firstRow; y < lastRow; y++) {
                for (int x = firstCol; x < lastCol; x++) {
                    final float worldX = cameraX + x * tilePx;
                    final float worldY = cellBottomY(y);
                    final MapCell cell = grid.get(x, y);
                    if (pass.isTerrain()) {
                        drawCellTerrain(x, y, cell.getTerrainId(), worldX, worldY, tilePx, pass.drawBackground());
                    } else {
                        drawCellFurniture(cell.getFurnitureId(), worldX, worldY, tilePx, pass.drawBackground());
                    }
                }
            }
        }

        if (ZCutawayPolicy.isCutawayActive(mapVolume, showZCutaway)) {
            drawUpperFloorCutaway(firstCol, lastCol, firstRow, lastRow, tilePx);
        }
    }

    private void drawOvermapGrid() {
        if (overmapGrid == null) {
            return;
        }
        final RegionSettingsDefinition region = worldgenPreviewService.getRegionSettingsRegistry()
            .find(overmapRegionId)
            .orElse(null);
        final float cellPx = activeCellPixelSize();
        final VisibleCellRange visible = visibleOmtCellRange(cellPx);
        final boolean drawSymbols = cellPx >= OVERMAP_SYMBOL_MIN_CELL_PX;

        for (int y = visible.firstRow; y < visible.lastRow; y++) {
            for (int x = visible.firstCol; x < visible.lastCol; x++) {
                final String omtId = overmapGrid.getOmtId(x, y);
                final String displayOmtId = resolveDisplayOmtId(omtId, region);
                final float worldX = cameraX + x * cellPx;
                final float worldY = cellBottomY(y);
                if (drawOvermapOmtSprite(displayOmtId, worldX, worldY, cellPx)) {
                    continue;
                }
                drawOvermapOmtFallback(displayOmtId, worldX, worldY, cellPx, drawSymbols);
            }
        }
    }

    private boolean drawOvermapOmtSprite(
        final String omtId,
        final float worldX,
        final float worldY,
        final float cellPx
    ) {
        if (tileset == null) {
            return false;
        }
        final OvermapOmtSpriteResolver.Resolve resolved =
            OvermapOmtSpriteResolver.resolve(tileset, omtId).orElse(null);
        if (resolved == null) {
            return false;
        }
        final TileDefinition tile = tileset.findTile(resolved.getTileId()).orElse(null);
        if (tile == null) {
            return false;
        }
        drawCellTile(tile, worldX, worldY, cellPx, false, resolved.getRotationIndex(), 1f);
        return true;
    }

    private void drawOvermapOmtFallback(
        final String omtId,
        final float worldX,
        final float worldY,
        final float cellPx,
        final boolean drawSymbols
    ) {
        final OvermapTerrainDefinition definition = overmapTerrainRegistry.find(omtId).orElse(null);
        final Color fill = definition == null
            ? OvermapTerrainColors.hashColor(omtId)
            : OvermapTerrainColors.resolve(definition.getColor(), omtId);
        batch.setColor(fill);
        batch.draw(whitePixel, worldX, worldY, cellPx, cellPx);
        batch.setColor(Color.WHITE);
        if (!drawSymbols) {
            return;
        }
        if (definition != null && definition.hasSymbol()) {
            final String label = definition.getSymbol();
            glyphLayout.setText(font, label);
            font.draw(
                batch,
                label,
                worldX + (cellPx - glyphLayout.width) / 2f,
                worldY + (cellPx + glyphLayout.height) / 2f
            );
        } else if (definition == null) {
            glyphLayout.setText(font, "?");
            font.draw(
                batch,
                "?",
                worldX + (cellPx - glyphLayout.width) / 2f,
                worldY + (cellPx + glyphLayout.height) / 2f
            );
        }
    }

    private VisibleCellRange visibleOmtCellRange(final float cellPx) {
        final int firstCol = Math.max(0, (int) Math.floor((-cameraX) / cellPx) - 1);
        final int lastCol = Math.min(
            overmapGrid.width(),
            (int) Math.ceil((canvasWidth() - cameraX) / cellPx) + 1
        );
        final int firstRow = Math.max(0, (int) Math.floor((gridTopY() - viewportPixelHeight()) / cellPx));
        final int lastRow = Math.min(
            overmapGrid.height(),
            (int) Math.ceil((gridTopY() - gridBaseY()) / cellPx)
        );
        return new VisibleCellRange(firstCol, lastCol, firstRow, lastRow);
    }

    private static final class VisibleCellRange {
        private final int firstCol;
        private final int lastCol;
        private final int firstRow;
        private final int lastRow;

        private VisibleCellRange(final int firstCol, final int lastCol, final int firstRow, final int lastRow) {
            this.firstCol = firstCol;
            this.lastCol = lastCol;
            this.firstRow = firstRow;
            this.lastRow = lastRow;
        }
    }

    private void drawOvermapSelection() {
        if (overmapGrid == null || selectedOmtX < 0 || selectedOmtY < 0) {
            return;
        }
        final float cellPx = activeCellPixelSize();
        final float worldX = cameraX + selectedOmtX * cellPx;
        final float worldY = cellBottomY(selectedOmtY);
        batch.setColor(1f, 0.92f, 0.35f, 1f);
        final float border = Math.max(1f, cellPx * 0.08f);
        batch.draw(whitePixel, worldX, worldY + cellPx - border, cellPx, border);
        batch.draw(whitePixel, worldX, worldY, cellPx, border);
        batch.draw(whitePixel, worldX, worldY, border, cellPx);
        batch.draw(whitePixel, worldX + cellPx - border, worldY, border, cellPx);
        batch.setColor(Color.WHITE);
    }

    private void drawUpperFloorCutaway(
        final int firstCol,
        final int lastCol,
        final int firstRow,
        final int lastRow,
        final float tilePx
    ) {
        final TerrainRegistry terrains = gameData == null ? null : gameData.getTerrain();
        for (final int zLevel : ZCutawayPolicy.zLevelsAboveActive(mapVolume)) {
            final MapGrid upperGrid = mapVolume.getGridAtZ(zLevel);
            if (upperGrid == null) {
                continue;
            }
            final int upperLastCol = Math.min(lastCol, upperGrid.width());
            final int upperLastRow = Math.min(lastRow, upperGrid.height());
            for (final GridDrawPass pass : GridDrawPass.values()) {
                if (!pass.isTerrain()) {
                    continue;
                }
                for (int y = firstRow; y < upperLastRow; y++) {
                    for (int x = firstCol; x < upperLastCol; x++) {
                        final String terrainId = upperGrid.get(x, y).getTerrainId();
                        if (ZCutawayPolicy.skipUpperFloorOverlay(terrainId, terrains)) {
                            continue;
                        }
                        final float worldX = cameraX + x * tilePx;
                        final float worldY = cellBottomY(y);
                        drawCellTerrain(
                            x,
                            y,
                            terrainId,
                            worldX,
                            worldY,
                            tilePx,
                            pass.drawBackground(),
                            upperGrid,
                            ZCutawayPolicy.UPPER_FLOOR_OVERLAY_ALPHA
                        );
                    }
                }
            }
        }
    }

    private enum GridDrawPass {
        TERRAIN_BACKGROUND(true, false, true),
        TERRAIN_FOREGROUND(true, false, false),
        FURNITURE_BACKGROUND(false, true, true),
        FURNITURE_FOREGROUND(false, true, false);

        private final boolean terrain;
        private final boolean furniture;
        private final boolean background;

        GridDrawPass(final boolean terrain, final boolean furniture, final boolean background) {
            this.terrain = terrain;
            this.furniture = furniture;
            this.background = background;
        }

        boolean isTerrain() {
            return terrain;
        }

        boolean isFurniture() {
            return furniture;
        }

        boolean drawBackground() {
            return background;
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
            LoadTiming.logMs("startup/tileset", loadingTilesetId + " complete", tilesetLoadStartNs);
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

    private void drawCellTerrain(
        final int cellX,
        final int cellY,
        final String terrainId,
        final float drawX,
        final float drawY,
        final float tilePx,
        final boolean backgroundOnly
    ) {
        drawCellTerrain(cellX, cellY, terrainId, drawX, drawY, tilePx, backgroundOnly, grid, 1f);
    }

    private void drawCellTerrain(
        final int cellX,
        final int cellY,
        final String terrainId,
        final float drawX,
        final float drawY,
        final float tilePx,
        final boolean backgroundOnly,
        final MapGrid sourceGrid,
        final float alpha
    ) {
        if (tileset == null) {
            return;
        }

        final MultitileConnectResolver.TerrainDrawResolve resolved = MultitileConnectResolver.resolveTerrainDraw(
            tileset,
            sourceGrid,
            cellX,
            cellY,
            terrainId,
            gameData == null ? null : gameData.getTerrain()
        );
        final String drawableId = TileLooksLikeResolver.resolveTerrainDrawId(
            resolved.getTileId(),
            tileset,
            gameData == null ? null : gameData.getTerrain()
        );
        final TileDefinition tile = findTileWithFallback(drawableId).orElse(null);
        if (tile == null) {
            return;
        }
        drawCellTile(tile, drawX, drawY, tilePx, backgroundOnly, resolved.getRotation(), alpha);
    }

    private void drawCellFurniture(
        final String furnitureId,
        final float drawX,
        final float drawY,
        final float tilePx,
        final boolean backgroundOnly
    ) {
        if (tileset == null || furnitureId == null || furnitureId.isEmpty()) {
            return;
        }
        final String drawableId = TileLooksLikeResolver.resolveFurnitureDrawId(
            furnitureId,
            tileset,
            gameData == null ? null : gameData.getFurniture()
        );
        if (!TileSpriteResolver.hasDrawableArt(tileset, drawableId)) {
            return;
        }
        tileset.findTile(drawableId).ifPresent(tile -> drawCellTile(tile, drawX, drawY, tilePx, backgroundOnly, 0, 1f));
    }

    private void drawCellTile(
        final TileDefinition tile,
        final float drawX,
        final float drawY,
        final float tilePx,
        final boolean backgroundOnly
    ) {
        drawCellTile(tile, drawX, drawY, tilePx, backgroundOnly, 0, 1f);
    }

    private void drawCellTile(
        final TileDefinition tile,
        final float drawX,
        final float drawY,
        final float tilePx,
        final boolean backgroundOnly,
        final int rotationIndex
    ) {
        drawCellTile(tile, drawX, drawY, tilePx, backgroundOnly, rotationIndex, 1f);
    }

    private void drawCellTile(
        final TileDefinition tile,
        final float drawX,
        final float drawY,
        final float tilePx,
        final boolean backgroundOnly,
        final int rotationIndex,
        final float alpha
    ) {
        final int pick = TileSpriteResolver.animationPickIndex(tile, animationTick, animationPlayback);
        final TextureRegion bg = TileSpriteResolver.resolveBackground(
            tileset,
            tile,
            pick,
            rotationIndex,
            TilesetFxType.NONE
        );
        final TextureRegion fg = TileSpriteResolver.resolveForeground(
            tileset,
            tile,
            pick,
            rotationIndex,
            TilesetFxType.NONE
        );

        if (backgroundOnly) {
            if (bg != null) {
                drawLayer(tile, bg, drawX, drawY, tilePx, alpha);
            }
            return;
        }
        if (fg != null) {
            drawLayer(tile, fg, drawX, drawY, tilePx, alpha);
        }
    }

    private void drawLayer(
        final TileDefinition tile,
        final TextureRegion region,
        final float drawX,
        final float drawY,
        final float tilePx
    ) {
        drawLayer(tile, region, drawX, drawY, tilePx, 1f);
    }

    private void drawLayer(
        final TileDefinition tile,
        final TextureRegion region,
        final float drawX,
        final float drawY,
        final float tilePx,
        final float alpha
    ) {
        final TileDrawMath.DrawRect rect = TileDrawMath.computeDrawRect(
            tileset,
            tile,
            region,
            drawX,
            drawY,
            tilePx
        );
        if (alpha >= 0.999f) {
            batch.draw(region, rect.x, rect.y, rect.width, rect.height);
            return;
        }
        final Color saved = batch.getColor().cpy();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(region, rect.x, rect.y, rect.width, rect.height);
        batch.setColor(saved);
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
        if (editorMode == EditorMode.OVERMAP) {
            drawOvermapHud();
            return;
        }
        final String brush = palette.getBrushLayer() == PaletteBrushLayer.TERRAIN
            ? palette.getSelectedTerrainId()
            : (palette.isClearFurnitureBrush() ? "(clear)" : palette.getSelectedFurnitureId());
        final String cursor = hoverCellX >= 0
            ? formatCursorCell(hoverCellX, hoverCellY)
            : "—";
        final String pieceLabel = formatHoveredPieceLabel();
        final String tilesetLine = formatTilesetLine();
        final String mapPath = currentMapPath.toAbsolutePath().normalize().toString();
        final String line1 = statusMessage;
        final String line2 = tilesetLine + "  |  map: " + mapPath;
        final String line4 = localExplore.isActive()
            ? ("Explore (" + localExplore.getFocusOmtX() + "," + localExplore.getFocusOmtY()
                + ",z=" + localExplore.getVisitZ() + ")"
                + "  |  patch min (" + localExplore.getPatchMinOmtX() + "," + localExplore.getPatchMinOmtY() + ")"
                + "  |  Grid " + grid.width() + "x" + grid.height()
                + "  |  left-drag walk  |  zoom " + formatViewZoom())
            : ("Grid " + grid.width() + "x" + grid.height()
                + buildingFloorHudSuffix()
                + "  |  layer " + palette.getBrushLayer().hudLabel()
                + "  |  brush " + brush
                + "  |  furniture " + (showFurnitureLayer ? "on" : "off")
                + "  |  spawns " + (showSpawnOverlay ? "on" : "off")
                + "  |  chunks " + (showOmtPieceBorders ? "on" : "off")
                + cutawayHudSuffix()
                + "  |  tool " + toolbar.getActiveTool().name().toLowerCase()
                + "  |  zoom " + formatViewZoom());
        final String line5 = localExplore.isActive()
            ? "Walkaround: left-drag or arrows load nearby OMTs  |  M returns to overmap"
            : "Toolbar: Paint / Pan / Pick / Mapgen — Ctrl+G import json mapgen";
        final String line6 = pointerDebugLogging
            ? "F3 pointer debug ON — delta = mouse minus highlight center  |  Esc menu"
            : formatShortcutLine();

        final int top = viewportPixelHeight() - HUD_MARGIN;
        final float cursorLineY = top - 32;
        font.draw(batch, line1, HUD_MARGIN, top);
        font.draw(batch, line2, HUD_MARGIN, top - 16);
        font.draw(batch, "Cursor: " + cursor, HUD_MARGIN, cursorLineY);
        if (!pieceLabel.isEmpty()) {
            glyphLayout.setText(font, pieceLabel);
            font.draw(batch, pieceLabel, canvasWidth() - HUD_MARGIN - glyphLayout.width, cursorLineY);
        }
        font.draw(batch, line4, HUD_MARGIN, top - 48);
        font.draw(batch, line5, HUD_MARGIN, top - 64);
        font.draw(batch, line6, HUD_MARGIN, top - 80);
    }

    private void drawOvermapHud() {
        final String hover = hoverCellX >= 0
            ? formatOvermapCursorCell(hoverCellX, hoverCellY)
            : "—";
        final String selection = selectedOmtX >= 0
            ? formatOvermapCursorCell(selectedOmtX, selectedOmtY)
            : "none";
        final String line1 = statusMessage;
        final String line2 = "Mode: OVERMAP  "
            + overmapGrid.width() + "×" + overmapGrid.height()
            + "  region=" + overmapRegionId
            + "  seed=" + overmapSeed;
        final String line3 = "Profile: " + describeActiveRegionProfile();
        final String line4 = "Hover: " + hover + "  |  Selected: " + selection + "  |  zoom " + formatViewZoom();
        final String line5 = "Click building cell — Enter walkaround — [ ] size — R new seed — G region — [M] submap";
        final String line6 = overmapTerrainLoaded
            ? "[M] submap mode  |  F1 help  |  Esc clear selection  |  F3 debug"
            : "Overmap terrain not loaded";

        final int top = viewportPixelHeight() - HUD_MARGIN;
        font.draw(batch, line1, HUD_MARGIN, top);
        font.draw(batch, line2, HUD_MARGIN, top - 16);
        font.draw(batch, line3, HUD_MARGIN, top - 32);
        font.draw(batch, line4, HUD_MARGIN, top - 48);
        font.draw(batch, line5, HUD_MARGIN, top - 64);
        font.draw(batch, line6, HUD_MARGIN, top - 80);
    }

    private String describeActiveRegionProfile() {
        final RegionSettingsDefinition region = worldgenPreviewService.getRegionSettingsRegistry()
            .find(overmapRegionId)
            .orElse(null);
        return RegionProfileSummary.describe(region);
    }

    private String formatOvermapCursorCell(final int x, final int y) {
        final String omtId = overmapGrid.getOmtId(x, y);
        final RegionSettingsDefinition region = worldgenPreviewService.getRegionSettingsRegistry()
            .find(overmapRegionId)
            .orElse(null);
        final String displayOmtId = resolveDisplayOmtId(omtId, region);
        final OvermapTerrainDefinition definition = overmapTerrainRegistry.find(omtId).orElse(null);
        final String mapgenCount = definition == null
            ? "mapgens=?"
            : "mapgens=" + definition.jsonMapgenRefCount();
        final String rotatable = definition != null && definition.isRotatable() ? " rotatable" : "";
        final String displaySuffix = omtId.equals(displayOmtId) ? "" : " display=" + displayOmtId;
        return "(" + x + "," + y + ") " + omtId + displaySuffix + "  " + mapgenCount + rotatable;
    }

    private static String resolveDisplayOmtId(final String omtId, final RegionSettingsDefinition region) {
        if (omtId == null || omtId.isEmpty() || region == null || !region.hasDisplayOter()) {
            return omtId;
        }
        if (!omtId.equals(region.getDefaultOter())) {
            return omtId;
        }
        final String display = region.getDisplayOter();
        return display == null || display.isEmpty() ? omtId : display;
    }

    private void copyBuildingPieceDebug() {
        if (mapVolume == null) {
            statusMessage = "No building loaded";
            return;
        }
        final String text = BuildingPieceDebugFormatter.format(mapVolume, activeBuilding);
        if (text.isEmpty()) {
            statusMessage = "Nothing to copy";
            return;
        }
        Gdx.app.getClipboard().setContents(text);
        statusMessage = "Piece layout copied (" + text.split("\n", -1).length + " lines) — paste into chat/issue";
        Gdx.app.log("mapgen", text);
    }

    private void copyOvermapDebug() {
        if (overmapGrid == null) {
            statusMessage = "No overmap loaded";
            return;
        }
        final String regionId = overmapRegionId;
        final String json = OvermapGridExporter.toJson(
            overmapGrid,
            overmapSeed,
            regionId,
            lastOvermapGeneration
        );
        Gdx.app.getClipboard().setContents(json);
        try {
            Files.createDirectories(DEFAULT_OVERMAP_EXPORT_PATH.getParent());
            Files.write(DEFAULT_OVERMAP_EXPORT_PATH, json.getBytes(StandardCharsets.UTF_8));
            statusMessage = "Overmap copied — clipboard + " + DEFAULT_OVERMAP_EXPORT_PATH
                + " (" + overmapGrid.width() + "×" + overmapGrid.height() + ", seed=" + overmapSeed + ")";
        } catch (final IOException e) {
            statusMessage = "Overmap copied to clipboard (file write failed: " + e.getMessage() + ")";
        }
        Gdx.app.log("overmap", json);
    }

    private String buildingFloorHudSuffix() {
        if (mapVolume == null) {
            return "";
        }
        return "  |  building " + mapVolume.getBuildingId()
            + "  floor z=" + mapVolume.getActiveZ()
            + " (" + (mapVolume.activeFloorIndex() + 1) + "/" + mapVolume.floorCount() + ")";
    }

    private String cutawayHudSuffix() {
        if (mapVolume == null || mapVolume.floorCount() <= 1) {
            return "";
        }
        return "  |  cutaway " + (showZCutaway ? "on" : "off");
    }

    private String formatShortcutLine() {
        final String modeHint = overmapTerrainLoaded ? "  [M] overmap" : "";
        final String floorHint = mapVolume != null && mapVolume.floorCount() > 1
            ? "  [PgUp/PgDn] floor  [T] cutaway"
            : "  [[ ]] tileset";
        final String pieceHint = editorMode == EditorMode.OVERMAP && overmapGrid != null
            ? "  [Ctrl+Shift+C] export overmap"
            : (mapVolume != null ? "  [Ctrl+Shift+C] copy piece layout" : "");
        return "[F] furniture  [L] brush layer  [O] spawns  [B] chunks  [P] anim  [G] grid" + modeHint + floorHint + pieceHint + "  |  F1 help  |  F3 debug  |  Esc menu";
    }

    private void toggleKeybindHelp() {
        final boolean multiFloor = mapVolume != null && mapVolume.floorCount() > 1;
        keybindHelp.toggle(MapEditorKeybindHelp.build(
            overmapTerrainLoaded && overmapGrid != null,
            editorMode == EditorMode.OVERMAP,
            multiFloor,
            localExplore.isActive()
        ));
    }

    private String formatCursorCell(final int x, final int y) {
        final MapCell cell = grid.get(x, y);
        final String furnitureId = cell.getFurnitureId();
        if (furnitureId != null && !furnitureId.isEmpty()) {
            return x + "," + y + " = " + cell.getTerrainId() + " + " + furnitureId;
        }
        return x + "," + y + " = " + cell.getTerrainId();
    }

    private String formatHoveredPieceLabel() {
        if (mapVolume == null || hoverCellX < 0 || hoverCellY < 0 || !isBuildingBundleView()) {
            return "";
        }
        return resolveHoveredPieceId(hoverCellX, hoverCellY)
            .map(id -> "Piece: " + id)
            .orElse("");
    }

    private boolean isBuildingBundleView() {
        if (mapVolume == null) {
            return false;
        }
        if (mapVolume.floorCount() > 1) {
            return true;
        }
        return mapVolume.getActivePieceLayouts().size() > 1;
    }

    private Optional<String> resolveHoveredPieceId(final int cellX, final int cellY) {
        for (final OmtPieceRect piece : mapVolume.getActivePieceLayouts()) {
            if (pieceContainsCell(piece, cellX, cellY)) {
                return Optional.of(piece.getOvermapId());
            }
        }
        if (activeBuilding == null) {
            return Optional.empty();
        }
        final List<CityBuildingPiece> pieces = activeBuilding.piecesAtZ(mapVolume.getActiveZ());
        if (pieces.size() == 1
            && cellX >= 0
            && cellY >= 0
            && cellX < grid.width()
            && cellY < grid.height()) {
            return Optional.of(pieces.get(0).getOvermapId());
        }
        return Optional.empty();
    }

    private static boolean pieceContainsCell(final OmtPieceRect piece, final int cellX, final int cellY) {
        return cellX >= piece.getOriginX()
            && cellX < piece.getOriginX() + piece.getWidth()
            && cellY >= piece.getOriginY()
            && cellY < piece.getOriginY() + piece.getHeight();
    }

    private String formatTilesetLine() {
        if (tilesetIds.isEmpty()) {
            return "Tileset: (none)";
        }
        final String id = tilesetIds.get(tilesetIndex);
        return "Tileset: " + id + "  " + (tilesetIndex + 1) + "/" + tilesetIds.size();
    }

    private void paintAt(final int screenX, final int screenY) {
        if (editorMode == EditorMode.OVERMAP) {
            return;
        }
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
        if (palette.getBrushLayer() == PaletteBrushLayer.FURNITURE) {
            if (palette.isClearFurnitureBrush()) {
                grid.clearFurniture(cell.x, cell.y);
            } else {
                final String furnitureId = palette.getSelectedFurnitureId();
                grid.setFurniture(cell.x, cell.y, furnitureId);
                warnUnknownFurnitureId(furnitureId);
            }
        } else {
            grid.setTerrain(cell.x, cell.y, palette.getSelectedTerrainId());
        }
        lastPaintCellX = cell.x;
        lastPaintCellY = cell.y;
    }

    private void eyedropAt(final int screenX, final int screenY) {
        if (editorMode == EditorMode.OVERMAP) {
            return;
        }
        final CellCoord cell = screenToCell(screenX, screenY);
        if (cell == null) {
            return;
        }
        final MapCell mapCell = grid.get(cell.x, cell.y);
        if (palette.getBrushLayer() == PaletteBrushLayer.FURNITURE) {
            final String furnitureId = mapCell.getFurnitureId();
            if (furnitureId == null || furnitureId.isEmpty()) {
                palette.setSelectedFurnitureId(null);
                statusMessage = "Eyedropper: clear furniture";
            } else {
                palette.setSelectedFurnitureId(furnitureId);
                statusMessage = "Eyedropper: " + furnitureId;
            }
            return;
        }
        palette.setSelectedTerrainId(mapCell.getTerrainId());
        statusMessage = "Eyedropper: " + palette.getSelectedTerrainId();
    }

    private void warnUnknownFurnitureId(final String furnitureId) {
        if (furnitureId == null || furnitureId.isEmpty() || gameData == null) {
            return;
        }
        if (!gameData.getFurniture().contains(furnitureId)) {
            statusMessage = "Unknown furniture id: " + furnitureId;
        }
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

        final float cellPx = activeCellPixelSize();
        final int cellX = (int) Math.floor((screenX - cameraX) / cellPx);
        final float rowOffset = gridTopY() - screenY;
        final int cellY = (int) Math.floor(rowOffset / cellPx);
        if (cellX < 0 || cellY < 0 || cellX >= activeGridWidth() || cellY >= activeGridHeight()) {
            return null;
        }
        return new CellCoord(cellX, cellY);
    }

    private int activeGridWidth() {
        if (editorMode == EditorMode.OVERMAP && overmapGrid != null) {
            return overmapGrid.width();
        }
        return grid.width();
    }

    private int activeGridHeight() {
        if (editorMode == EditorMode.OVERMAP && overmapGrid != null) {
            return overmapGrid.height();
        }
        return grid.height();
    }

    private float activeCellPixelSize() {
        return editorMode == EditorMode.OVERMAP ? omtCellPixelSize() : tilePixelSize();
    }

    private float omtCellPixelSize() {
        return Math.max(0.5f, viewZoom * OMT_BASE_CELL_PX);
    }

    /** Bottom edge of the grid in screen space (LibGDX y-up). */
    private float gridBaseY() {
        return cameraY + hudHeight();
    }

    /** Top edge of the grid in screen space; grid row 0 is just below this. */
    private float gridTopY() {
        return gridBaseY() + activeGridHeight() * activeCellPixelSize();
    }

    /** Bottom-left draw position for a cell; row 0 is the top row of the map. */
    private float cellBottomY(final int gridY) {
        return gridTopY() - (gridY + 1) * activeCellPixelSize();
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
            mapVolume = null;
            activeBuilding = null;
            clearSpawnMarkers();
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
        localExplore.clear();
        mapVolume = null;
        activeBuilding = null;
        clearSpawnMarkers();
        grid = new MapGrid(source.width(), source.height(), source.getDefaultTerrainId());
        copyGrid(source);
    }

    private void clearSpawnMarkers() {
        spawnMarkersByZ = Collections.emptyMap();
    }

    private void setSpawnMarkers(final List<SpawnMarker> markers) {
        if (markers == null || markers.isEmpty()) {
            spawnMarkersByZ = Collections.emptyMap();
            return;
        }
        spawnMarkersByZ = Collections.singletonMap(0, List.copyOf(markers));
    }

    private void setSpawnMarkersByZ(final Map<Integer, List<SpawnMarker>> markersByZ) {
        if (markersByZ == null || markersByZ.isEmpty()) {
            spawnMarkersByZ = Collections.emptyMap();
            return;
        }
        spawnMarkersByZ = Collections.unmodifiableMap(new HashMap<>(markersByZ));
    }

    private List<SpawnMarker> activeSpawnMarkers() {
        if (spawnMarkersByZ.isEmpty()) {
            return List.of();
        }
        if (mapVolume != null) {
            final List<SpawnMarker> floorMarkers = spawnMarkersByZ.get(mapVolume.getActiveZ());
            return floorMarkers == null ? List.of() : floorMarkers;
        }
        if (spawnMarkersByZ.size() == 1) {
            return spawnMarkersByZ.values().iterator().next();
        }
        final List<SpawnMarker> ground = spawnMarkersByZ.get(0);
        return ground == null ? List.of() : ground;
    }

    private void setMapVolume(final MapVolume volume) {
        setMapVolume(volume, null);
    }

    private void setMapVolume(final MapVolume volume, final CityBuildingDefinition building) {
        mapVolume = volume;
        activeBuilding = building;
        grid = volume.getActiveGrid();
        centerCamera();
    }

    private void switchBuildingFloor(final boolean higher) {
        if (mapVolume == null) {
            return;
        }
        final Optional<Integer> nextZ = higher ? mapVolume.nextZ() : mapVolume.previousZ();
        if (!nextZ.isPresent()) {
            return;
        }
        mapVolume.setActiveZ(nextZ.get());
        final int oldWidth = grid.width();
        final int oldHeight = grid.height();
        grid = mapVolume.getActiveGrid();
        if (grid.width() != oldWidth || grid.height() != oldHeight) {
            centerCamera();
        }
        statusMessage = "Building " + mapVolume.getBuildingId()
            + " — floor z=" + mapVolume.getActiveZ()
            + " (" + (mapVolume.activeFloorIndex() + 1) + "/" + mapVolume.floorCount() + ")";
    }

    private void openMapgenPicker() {
        if (mapgenPicker.isOpen() || regionPicker.isOpen() || keybindHelp.isOpen() || loadingMapgenCatalog || generatingOvermap) {
            return;
        }
        ensureMapgenCatalogLoaded(this::showMapgenPicker);
    }

    private void openRegionPicker() {
        if (regionPicker.isOpen() || mapgenPicker.isOpen() || keybindHelp.isOpen() || loadingMapgenCatalog || generatingOvermap) {
            return;
        }
        ensureWorldgenLoaded(this::showRegionPicker);
    }

    private void showRegionPicker() {
        syncOvermapRegionFromRegistry();
        final List<String> regionIds = worldgenPreviewService.getRegionSettingsRegistry().regionIdsForPicker();
        if (regionIds.isEmpty()) {
            statusMessage = "No region_settings loaded from data/";
            return;
        }
        final java.util.Map<String, String> summaries = new java.util.LinkedHashMap<>();
        for (final String regionId : regionIds) {
            final RegionSettingsDefinition region = worldgenPreviewService.getRegionSettingsRegistry()
                .find(regionId)
                .orElse(null);
            summaries.put(regionId, RegionProfileSummary.describe(region));
        }
        regionPicker.open(regionIds, overmapRegionId, summaries);
    }

    private void syncOvermapRegionFromRegistry() {
        if (!worldgenPreviewService.getRegionSettingsRegistry().find(overmapRegionId).isPresent()) {
            if (worldgenPreviewService.getRegionSettingsRegistry().find("default").isPresent()) {
                overmapRegionId = "default";
            } else {
                final List<String> ids = worldgenPreviewService.getRegionSettingsRegistry().regionIdsForPicker();
                if (!ids.isEmpty()) {
                    overmapRegionId = ids.get(0);
                }
            }
        }
        worldgenPreviewService.setRegionId(overmapRegionId);
    }

    private void pollRegionPickerSelection() {
        final Optional<String> selection = regionPicker.takeSelection();
        selection.ifPresent(this::applyOvermapRegionSelection);
    }

    private void applyOvermapRegionSelection(final String regionId) {
        if (regionId == null || regionId.isEmpty() || regionId.equals(overmapRegionId)) {
            return;
        }
        overmapRegionId = regionId;
        worldgenPreviewService.setRegionId(regionId);
        if (editorMode == EditorMode.OVERMAP && worldgenPreviewService.isLoaded()) {
            regenerateOvermapLayout();
            return;
        }
        statusMessage = "Region set to " + regionId + " — switch to overmap (M) to regenerate";
    }

    private void showMapgenPicker() {
        if (mapgenService().getCatalog().runnableOnly().isEmpty()) {
            statusMessage = "No json mapgens found";
            return;
        }
        mapgenPicker.open(
            mapgenService().getCatalog(),
            mapgenService().getCityBuildings(),
            mapgenService().getPickerIndex()
        );
    }

    private void pollMapgenPickerSelection() {
        final Optional<CityBuildingDefinition> buildingImport = mapgenPicker.takeBuildingImport();
        if (buildingImport.isPresent()) {
            applyBuildingImport(buildingImport.get());
            return;
        }
        final Optional<JsonMapgenDefinition> selection = mapgenPicker.takeSelection();
        selection.ifPresent(this::applyMapgenSelection);
    }

    private void applyBuildingImport(final CityBuildingDefinition building) {
        try {
            statusMessage = "Generating building " + building.getId() + "…";
            final MapgenPreviewService.MapgenBuildingResult result = mapgenService().generateBuilding(
                building,
                gameData,
                new JsonMapgenRunOptions()
            );
            setMapVolume(result.getVolume(), building);
            setSpawnMarkersByZ(result.getSpawnMarkersByZ());
            statusMessage = "Building: " + building.getId()
                + " — floor z=" + mapVolume.getActiveZ()
                + " (" + mapVolume.floorCount() + " floors loaded)";
            if (!result.getRunWarnings().isEmpty()) {
                statusMessage += " | " + result.getRunWarnings().size() + " warning(s)";
            }
            logWarningSummary("mapgen", result.getRunWarnings());
            logMapgenLoadWarnings();
        } catch (final RuntimeException e) {
            showImportFailed("Building import failed", "Building: " + building.getId(), e);
        }
    }

    private void showImportFailed(final String title, final String headline, final Throwable error) {
        statusMessage = title;
        Gdx.app.error("mapgen", headline, error);
        importFailedDialog.show(title, headline, error);
    }

    private void applyMapgenSelection(final JsonMapgenDefinition definition) {
        if (!definition.isJsonPreviewSupported()) {
            statusMessage = "Builtin mapgen not supported";
            return;
        }
        try {
            statusMessage = "Generating " + definition.displayName() + "…";
            final MapgenPreviewService.MapgenPreviewResult result = mapgenService().generate(
                definition,
                gameData,
                new JsonMapgenRunOptions()
            );
            replaceGrid(result.getGrid());
            setSpawnMarkers(result.getSpawnMarkers());
            centerCamera();
            statusMessage = "Mapgen: " + definition.displayName()
                + " (" + grid.width() + "x" + grid.height() + ")";
            logWarningSummary("mapgen", result.getRunWarnings());
            appendValidationSummary(runGameDataValidation(tileset));
        } catch (final RuntimeException e) {
            showImportFailed("Mapgen import failed", "Mapgen: " + definition.displayName(), e);
        }
    }

    private void logMapgenLoadWarnings() {
        logWarningSummary("mapgen", mapgenService().getLoadWarnings());
    }

    private void drawBusyLoadingOverlay() {
        loadingOverlay.update(Gdx.graphics.getDeltaTime());
        final String title;
        final String detail;
        if (generatingOvermap) {
            title = "Generating overmap";
            detail = statusMessage == null ? "Procedural layout in progress" : statusMessage;
        } else {
            title = "Loading mapgen catalog";
            detail = "Scanning palettes and mapgen JSON";
        }
        loadingOverlay.draw(
            batch,
            font,
            whitePixel,
            0,
            0,
            viewportPixelWidth(),
            viewportPixelHeight(),
            title,
            detail
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
        tilesetLoadStartNs = LoadTiming.start();
        LoadTiming.log("startup/tileset", "begin " + loadingTilesetId);
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
        if (mapVolume != null) {
            statusMessage = "Grid resize disabled while viewing a building bundle";
            return;
        }
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
        final float cellPx = activeCellPixelSize();
        final float gridW = activeGridWidth() * cellPx;
        final float gridH = activeGridHeight() * cellPx;
        final float areaBottom = MapEditorToolbar.HEIGHT + 16;
        final float areaTop = viewportPixelHeight() - hudHeight() - 8;
        final float baseY = (areaBottom + areaTop - gridH) / 2f;
        cameraY = baseY - hudHeight();
        cameraX = (canvasWidth() - gridW) / 2f;
    }

    private void centerCameraOnVisit(final VisitResult result) {
        int focusCellX = -1;
        int focusCellY = -1;
        if (result.isBuildingVisit() && mapVolume != null && result.getVisitOmtX() >= 0) {
            final int[] focus = focusCellForBuildingPiece(result.getVisitOmtX(), result.getVisitOmtY());
            focusCellX = focus[0];
            focusCellY = focus[1];
        } else if (result.isPatchVisit()) {
            final int stride = OmtStitchComposer.DEFAULT_OMT_SIZE;
            focusCellX = (result.getVisitOmtX() - result.getPatchMinOmtX()) * stride + stride / 2;
            focusCellY = (result.getVisitOmtY() - result.getPatchMinOmtY()) * stride + stride / 2;
        }
        if (focusCellX >= 0 && focusCellY >= 0) {
            centerCameraOnCell(focusCellX, focusCellY);
            return;
        }
        centerCamera();
    }

    private int[] focusCellForBuildingPiece(final int omtX, final int omtY) {
        if (mapVolume == null || worldgenPreviewService == null) {
            return new int[] { grid.width() / 2, grid.height() / 2 };
        }
        final PlacedBuildingRecord record = worldgenPreviewService.getPlacementIndex().findAt(omtX, omtY).orElse(null);
        if (record == null) {
            return new int[] { grid.width() / 2, grid.height() / 2 };
        }
        final int pieceOffsetX = omtX - record.getAnchorX();
        final int pieceOffsetY = omtY - record.getAnchorY();
        final int stride = OmtStitchComposer.DEFAULT_OMT_SIZE;
        for (final OmtPieceRect piece : mapVolume.getActivePieceLayouts()) {
            if (piece.getOriginX() / stride == pieceOffsetX && piece.getOriginY() / stride == pieceOffsetY) {
                return new int[] {
                    piece.getOriginX() + piece.getWidth() / 2,
                    piece.getOriginY() + piece.getHeight() / 2
                };
            }
        }
        return new int[] { grid.width() / 2, grid.height() / 2 };
    }

    private void centerCameraOnCell(final int cellX, final int cellY) {
        final float cellPx = activeCellPixelSize();
        final float gridH = activeGridHeight() * cellPx;
        final float areaBottom = MapEditorToolbar.HEIGHT + 16;
        final float areaTop = viewportPixelHeight() - hudHeight() - 8;
        final float viewMidY = (areaBottom + areaTop) / 2f;

        cameraX = canvasWidth() / 2f - (cellX + 0.5f) * cellPx;
        cameraY = viewMidY - hudHeight() - gridH + (cellY + 0.5f) * cellPx;
    }

    private int canvasWidth() {
        return Math.max(1, viewportPixelWidth() - MapPalettePanel.WIDTH);
    }

    private float tilePixelSize() {
        if (tileset == null) {
            return Math.max(16f, viewZoom * 16f);
        }
        final int pixelScale = Math.max(1, Math.round(tileset.getTileInfo().getPixelScale()));
        return Math.max(0.5f, viewZoom * pixelScale * tileset.getTileInfo().getWidth());
    }

    private void adjustViewZoomByScroll(final float amountY, final int anchorX, final int anchorY) {
        if (amountY == 0f) {
            return;
        }
        applyViewZoomScale(scrollZoomScale(amountY), anchorX, anchorY);
    }

    private void adjustViewZoomByKey(final boolean zoomIn, final int anchorX, final int anchorY) {
        final float scale = zoomIn
            ? 1f + ZOOM_KEY_SENSITIVITY
            : 1f / (1f + ZOOM_KEY_SENSITIVITY);
        applyViewZoomScale(scale, anchorX, anchorY);
    }

    private static float scrollZoomScale(final float amountY) {
        return (float) Math.pow(1f + ZOOM_SCROLL_SENSITIVITY, -amountY);
    }

    private void applyViewZoomScale(final float scale, final int anchorX, final int anchorY) {
        if (scale == 1f) {
            return;
        }
        final float oldCellPx = activeCellPixelSize();
        final float mapX = (anchorX - cameraX) / oldCellPx;
        final float gridTopOld = cameraY + hudHeight() + activeGridHeight() * oldCellPx;
        final float mapYFromTop = (gridTopOld - anchorY) / oldCellPx;

        viewZoom = MathUtils.clamp(viewZoom * scale, MIN_VIEW_ZOOM, MAX_VIEW_ZOOM);

        final float newCellPx = activeCellPixelSize();
        cameraX = anchorX - mapX * newCellPx;
        cameraY = anchorY - hudHeight() - activeGridHeight() * newCellPx + mapYFromTop * newCellPx;
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
        if (viewZoom < 1f) {
            return String.format(Locale.ROOT, "%.2fx", viewZoom);
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
