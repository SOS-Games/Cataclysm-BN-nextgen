package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.mapgen.MapgenPickerIndex;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.building.CityBuildingRegistry;
import io.gdx.cdda.bn.nextgen.mapgen.json.JsonMapgenDefinition;
import io.gdx.cdda.bn.nextgen.mapgen.json.MapgenCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Modal text picker for json mapgen preview (P3). */
public final class MapgenPickerDialog {

    private static final int PANEL_WIDTH = 660;
    private static final int PANEL_HEIGHT = 420;
    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 26;
    private static final long DOUBLE_CLICK_MS = 400L;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    private MapgenCatalog catalog;
    private CityBuildingRegistry buildingRegistry = CityBuildingRegistry.empty();
    private MapgenPickerIndex pickerIndex = MapgenPickerIndex.build(null, CityBuildingRegistry.empty());
    private List<JsonMapgenDefinition> visibleEntries = Collections.emptyList();
    private String filterQuery = "";
    private boolean filterEditing;
    private int selectedIndex;
    private int scrollOffset;
    private boolean open;
    private JsonMapgenDefinition pendingSelection;
    private CityBuildingDefinition pendingBuilding;
    private long lastRowClickMs;
    private int lastRowClickIndex = -1;

    public boolean isOpen() {
        return open;
    }

    public void open(
        final MapgenCatalog catalog,
        final CityBuildingRegistry buildingRegistry,
        final MapgenPickerIndex pickerIndex
    ) {
        this.catalog = catalog;
        this.buildingRegistry = buildingRegistry == null ? CityBuildingRegistry.empty() : buildingRegistry;
        this.pickerIndex = pickerIndex == null
            ? MapgenPickerIndex.build(catalog, this.buildingRegistry)
            : pickerIndex;
        filterQuery = "";
        filterEditing = true;
        selectedIndex = 0;
        scrollOffset = 0;
        pendingSelection = null;
        pendingBuilding = null;
        lastRowClickIndex = -1;
        rebuildVisibleEntries();
        open = true;
    }

    public void cancel() {
        open = false;
        pendingSelection = null;
        pendingBuilding = null;
        filterEditing = false;
    }

    public Optional<JsonMapgenDefinition> takeSelection() {
        final JsonMapgenDefinition selection = pendingSelection;
        pendingSelection = null;
        pendingBuilding = null;
        return Optional.ofNullable(selection);
    }

    public Optional<CityBuildingDefinition> takeBuildingImport() {
        final CityBuildingDefinition building = pendingBuilding;
        if (building == null) {
            return Optional.empty();
        }
        pendingBuilding = null;
        pendingSelection = null;
        return Optional.of(building);
    }

    public boolean isFilterEditing() {
        return filterEditing;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void clearFilter() {
        filterQuery = "";
        filterEditing = true;
        rebuildVisibleEntries();
    }

    public boolean onKeyDown(final int keycode) {
        if (!open) {
            return false;
        }
        if (filterEditing) {
            if (keycode == Keys.ESCAPE) {
                filterEditing = false;
                return true;
            }
            if (keycode == Keys.BACKSPACE) {
                if (!filterQuery.isEmpty()) {
                    filterQuery = filterQuery.substring(0, filterQuery.length() - 1);
                    rebuildVisibleEntries();
                }
                return true;
            }
            if (keycode == Keys.ENTER) {
                filterEditing = false;
                return true;
            }
            return false;
        }

        if (keycode == Keys.ESCAPE) {
            cancel();
            return true;
        }
        if (keycode == Keys.UP) {
            moveSelection(-1);
            return true;
        }
        if (keycode == Keys.DOWN) {
            moveSelection(1);
            return true;
        }
        if (keycode == Keys.PAGE_UP) {
            moveSelection(-visibleRowCount());
            return true;
        }
        if (keycode == Keys.PAGE_DOWN) {
            moveSelection(visibleRowCount());
            return true;
        }
        if (keycode == Keys.ENTER) {
            if (selectedBuilding().isPresent()) {
                confirmBuildingImport();
            } else {
                confirmSelection();
            }
            return true;
        }
        return false;
    }

    public boolean onKeyTyped(final char character) {
        if (!open) {
            return false;
        }
        if (Character.isISOControl(character)) {
            return false;
        }
        if (!filterEditing) {
            filterEditing = true;
        }
        filterQuery += character;
        rebuildVisibleEntries();
        return true;
    }

    public boolean onScroll(final float amountY) {
        if (!open || visibleEntries.isEmpty() || amountY == 0f) {
            return open;
        }
        scrollOffset += (int) amountY;
        clampScrollOffset();
        return true;
    }

    public boolean onTouchDown(final int screenX, final int screenY, final int viewportWidth, final int viewportHeight) {
        if (!open) {
            return false;
        }
        final PanelLayout layout = layout(viewportWidth, viewportHeight);
        if (hitBuilding(screenX, screenY, layout)) {
            confirmBuildingImport();
            return true;
        }
        if (hitGenerate(screenX, screenY, layout)) {
            confirmSelection();
            return true;
        }
        if (hitCancel(screenX, screenY, layout)) {
            cancel();
            return true;
        }
        if (hitFilter(screenX, screenY, layout)) {
            filterEditing = true;
            return true;
        }
        final int row = rowAt(screenX, screenY, layout);
        if (row >= 0) {
            selectedIndex = scrollOffset + row;
            clampSelection();
            final long now = System.currentTimeMillis();
            if (row == lastRowClickIndex && now - lastRowClickMs <= DOUBLE_CLICK_MS) {
                if (selectedBuilding().isPresent()) {
                    confirmBuildingImport();
                } else {
                    confirmSelection();
                }
            }
            lastRowClickIndex = row;
            lastRowClickMs = now;
            return true;
        }
        return true;
    }

    public void render(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final int viewportWidth,
        final int viewportHeight
    ) {
        if (!open) {
            return;
        }

        batch.setColor(0f, 0f, 0f, 0.55f);
        batch.draw(whitePixel, 0f, 0f, viewportWidth, viewportHeight);
        batch.setColor(Color.WHITE);

        final PanelLayout layout = layout(viewportWidth, viewportHeight);
        batch.setColor(0.16f, 0.17f, 0.22f, 0.98f);
        batch.draw(whitePixel, layout.panelX, layout.panelY, PANEL_WIDTH, PANEL_HEIGHT);
        batch.setColor(Color.WHITE);

        final Color old = font.getColor().cpy();
        font.setColor(0.92f, 0.94f, 0.98f, 1f);
        font.draw(batch, "Import mapgen", layout.panelX + MARGIN, layout.panelY + PANEL_HEIGHT - MARGIN);

        drawFilterRow(batch, font, whitePixel, layout);
        drawList(batch, font, whitePixel, layout);
        drawButtons(batch, font, whitePixel, layout);
        drawBuildingHint(batch, font, layout);
        font.setColor(old);
    }

    private void drawBuildingHint(final SpriteBatch batch, final BitmapFont font, final PanelLayout layout) {
        final Optional<CityBuildingDefinition> building = selectedBuilding();
        if (!building.isPresent()) {
            return;
        }
        final CityBuildingDefinition def = building.get();
        font.setColor(0.72f, 0.78f, 0.88f, 1f);
        final StringBuilder hint = new StringBuilder("Building: ");
        hint.append(def.getId());
        final String summary = def.buildingSummaryLabel();
        if (!summary.isEmpty()) {
            hint.append(" (").append(summary).append(')');
        }
        font.draw(batch, hint.toString(), layout.panelX + MARGIN, layout.buttonY + BUTTON_HEIGHT + 10);
        font.setColor(0.92f, 0.94f, 0.98f, 1f);
    }

    private void drawFilterRow(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final PanelLayout layout
    ) {
        final float filterY = layout.listTop + 8;
        font.draw(batch, "Filter:", layout.panelX + MARGIN, filterY + ROW_HEIGHT - 4);
        final float boxX = layout.panelX + MARGIN + 48;
        final float boxW = PANEL_WIDTH - MARGIN * 2 - 48;
        batch.setColor(0.10f, 0.11f, 0.14f, 1f);
        batch.draw(whitePixel, boxX, filterY, boxW, ROW_HEIGHT);
        batch.setColor(Color.WHITE);
        final String shown = filterQuery.isEmpty() ? (filterEditing ? "|" : "type to filter…") : filterQuery + (filterEditing ? "|" : "");
        font.setColor(filterEditing ? 1f : 0.75f, filterEditing ? 0.95f : 0.78f, filterEditing ? 0.55f : 0.78f, 1f);
        font.draw(batch, fitText(font, shown, (int) boxW - 8), boxX + 4, filterY + ROW_HEIGHT - 5);
        font.setColor(0.92f, 0.94f, 0.98f, 1f);
    }

    private void drawList(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final PanelLayout layout
    ) {
        final int rows = visibleRowCount();
        for (int row = 0; row < rows; row++) {
            final int index = scrollOffset + row;
            if (index >= visibleEntries.size()) {
                break;
            }
            final JsonMapgenDefinition definition = visibleEntries.get(index);
            final float y = layout.listTop - (row + 1) * ROW_HEIGHT;
            if (index == selectedIndex) {
                batch.setColor(0.24f, 0.28f, 0.38f, 1f);
                batch.draw(whitePixel, layout.panelX + MARGIN, y, PANEL_WIDTH - MARGIN * 2, ROW_HEIGHT);
                batch.setColor(Color.WHITE);
            }
            final boolean runnable = definition.isJsonPreviewSupported();
            if (runnable) {
                font.setColor(index == selectedIndex ? 0.95f : 0.88f, 0.9f, 0.94f, 1f);
            } else {
                font.setColor(0.45f, 0.47f, 0.52f, 1f);
            }
            font.draw(
                batch,
                fitText(font, formatRow(definition), PANEL_WIDTH - MARGIN * 2 - 8),
                layout.panelX + MARGIN + 4,
                y + ROW_HEIGHT - 5
            );
        }
        font.setColor(0.92f, 0.94f, 0.98f, 1f);
    }

    private void drawButtons(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final PanelLayout layout
    ) {
        final boolean buildingEnabled = selectedBuilding().isPresent();
        if (buildingEnabled) {
            drawButton(batch, font, whitePixel, "Import building", layout.buildingX, layout.buttonY, layout.buildingW);
        } else {
            drawButton(batch, font, whitePixel, "Generate", layout.generateX, layout.buttonY, layout.generateW);
        }
        drawButton(batch, font, whitePixel, "Cancel", layout.cancelX, layout.buttonY, layout.cancelW);
    }

    private void drawButton(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final String label,
        final float x,
        final float y,
        final float width
    ) {
        batch.setColor(0.22f, 0.24f, 0.30f, 1f);
        batch.draw(whitePixel, x, y, width, BUTTON_HEIGHT);
        batch.setColor(Color.WHITE);
        glyphLayout.setText(font, label);
        font.draw(batch, label, x + (width - glyphLayout.width) / 2f, y + BUTTON_HEIGHT - 7);
    }

    private void confirmBuildingImport() {
        final Optional<CityBuildingDefinition> building = selectedBuilding();
        if (!building.isPresent()) {
            return;
        }
        pendingBuilding = building.get();
        open = false;
        filterEditing = false;
    }

    private Optional<CityBuildingDefinition> selectedBuilding() {
        if (catalog == null || buildingRegistry == null || visibleEntries.isEmpty()) {
            return Optional.empty();
        }
        final JsonMapgenDefinition definition = visibleEntries.get(selectedIndex);
        return findBuildingForDefinition(definition).filter(CityBuildingDefinition::isBundledBuilding);
    }

    private void confirmSelection() {
        if (visibleEntries.isEmpty()) {
            return;
        }
        final JsonMapgenDefinition definition = visibleEntries.get(selectedIndex);
        if (!definition.isJsonPreviewSupported()) {
            return;
        }
        pendingSelection = definition;
        open = false;
        filterEditing = false;
    }

    private void moveSelection(final int delta) {
        if (visibleEntries.isEmpty()) {
            return;
        }
        selectedIndex = Math.max(0, Math.min(visibleEntries.size() - 1, selectedIndex + delta));
        ensureSelectionVisible();
    }

    private void rebuildVisibleEntries() {
        if (catalog == null || pickerIndex == null) {
            visibleEntries = Collections.emptyList();
            selectedIndex = 0;
            scrollOffset = 0;
            return;
        }
        visibleEntries = new ArrayList<>(pickerIndex.filter(filterQuery, catalog));
        selectedIndex = Math.min(selectedIndex, Math.max(0, visibleEntries.size() - 1));
        clampScrollOffset();
        ensureSelectionVisible();
    }

    private Optional<CityBuildingDefinition> findBuildingForDefinition(final JsonMapgenDefinition definition) {
        return MapgenPickerIndex.findBuildingForDefinition(definition, buildingRegistry);
    }

    private void ensureSelectionVisible() {
        if (visibleEntries.isEmpty()) {
            scrollOffset = 0;
            selectedIndex = 0;
            return;
        }
        clampSelection();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + visibleRowCount()) {
            scrollOffset = selectedIndex - visibleRowCount() + 1;
        }
    }

    private void clampScrollOffset() {
        final int maxOffset = Math.max(0, visibleEntries.size() - visibleRowCount());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }

    private void clampSelection() {
        if (visibleEntries.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = Math.max(0, Math.min(visibleEntries.size() - 1, selectedIndex));
    }

    private int visibleRowCount() {
        final int listHeight = PANEL_HEIGHT - 120;
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private String formatRow(final JsonMapgenDefinition definition) {
        return MapgenPickerIndex.formatRow(definition, buildingRegistry);
    }

    private String fitText(final BitmapFont font, final String text, final int maxWidth) {
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

    private PanelLayout layout(final int viewportWidth, final int viewportHeight) {
        final float panelX = (viewportWidth - PANEL_WIDTH) / 2f;
        final float panelY = (viewportHeight - PANEL_HEIGHT) / 2f;
        final float listTop = panelY + PANEL_HEIGHT - 52;
        final float buttonY = panelY + MARGIN;
        final float generateW = 110f;
        final float cancelW = 90f;
        final float buildingW = 124f;
        final float generateX = panelX + PANEL_WIDTH - MARGIN - generateW;
        final float cancelX = generateX - 8f - cancelW;
        final float buildingX = cancelX - 8f - buildingW;
        return new PanelLayout(panelX, panelY, listTop, buttonY, buildingX, buildingW, generateX, generateW, cancelX, cancelW);
    }

    private boolean hitBuilding(final int x, final int y, final PanelLayout layout) {
        if (!selectedBuilding().isPresent()) {
            return false;
        }
        return x >= layout.buildingX
            && x <= layout.buildingX + layout.buildingW
            && y >= layout.buttonY
            && y <= layout.buttonY + BUTTON_HEIGHT;
    }

    private boolean hitGenerate(final int x, final int y, final PanelLayout layout) {
        if (selectedBuilding().isPresent()) {
            return false;
        }
        return x >= layout.generateX
            && x <= layout.generateX + layout.generateW
            && y >= layout.buttonY
            && y <= layout.buttonY + BUTTON_HEIGHT;
    }

    private boolean hitCancel(final int x, final int y, final PanelLayout layout) {
        return x >= layout.cancelX
            && x <= layout.cancelX + layout.cancelW
            && y >= layout.buttonY
            && y <= layout.buttonY + BUTTON_HEIGHT;
    }

    private boolean hitFilter(final int x, final int y, final PanelLayout layout) {
        final float filterY = layout.listTop + 8;
        final float boxX = layout.panelX + MARGIN + 48;
        final float boxW = PANEL_WIDTH - MARGIN * 2 - 48;
        return x >= boxX && x <= boxX + boxW && y >= filterY && y <= filterY + ROW_HEIGHT;
    }

    private int rowAt(final int x, final int y, final PanelLayout layout) {
        if (x < layout.panelX + MARGIN || x > layout.panelX + PANEL_WIDTH - MARGIN) {
            return -1;
        }
        final float firstRowBottom = layout.listTop - ROW_HEIGHT;
        if (y > layout.listTop || y < firstRowBottom - (visibleRowCount() - 1) * ROW_HEIGHT) {
            return -1;
        }
        final int row = (int) ((layout.listTop - y - 1) / ROW_HEIGHT);
        if (row < 0 || row >= visibleRowCount()) {
            return -1;
        }
        return row;
    }

    private static final class PanelLayout {
        private final float panelX;
        private final float panelY;
        private final float listTop;
        private final float buttonY;
        private final float buildingX;
        private final float buildingW;
        private final float generateX;
        private final float generateW;
        private final float cancelX;
        private final float cancelW;

        private PanelLayout(
            final float panelX,
            final float panelY,
            final float listTop,
            final float buttonY,
            final float buildingX,
            final float buildingW,
            final float generateX,
            final float generateW,
            final float cancelX,
            final float cancelW
        ) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.listTop = listTop;
            this.buttonY = buttonY;
            this.buildingX = buildingX;
            this.buildingW = buildingW;
            this.generateX = generateX;
            this.generateW = generateW;
            this.cancelX = cancelX;
            this.cancelW = cancelW;
        }
    }
}
