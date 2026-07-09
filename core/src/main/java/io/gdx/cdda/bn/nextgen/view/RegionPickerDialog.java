package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Modal picker for active {@code region_settings} id (Tier B). */
public final class RegionPickerDialog {

    private static final int PANEL_WIDTH = 480;
    private static final int PANEL_HEIGHT = 360;
    private static final int MARGIN = 12;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 26;
    private static final long DOUBLE_CLICK_MS = 400L;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    private List<String> allRegionIds = Collections.emptyList();
    private List<String> visibleRegionIds = Collections.emptyList();
    private Map<String, String> regionSummaries = Collections.emptyMap();
    private String filterQuery = "";
    private boolean filterEditing;
    private int selectedIndex;
    private int scrollOffset;
    private boolean open;
    private String pendingSelection;
    private long lastRowClickMs;
    private int lastRowClickIndex = -1;

    public boolean isOpen() {
        return open;
    }

    public boolean isFilterEditing() {
        return filterEditing;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void open(final List<String> regionIds, final String selectedRegionId) {
        open(regionIds, selectedRegionId, Collections.emptyMap());
    }

    public void open(
        final List<String> regionIds,
        final String selectedRegionId,
        final Map<String, String> summariesByRegionId
    ) {
        allRegionIds = regionIds == null || regionIds.isEmpty()
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(regionIds));
        if (summariesByRegionId == null || summariesByRegionId.isEmpty()) {
            regionSummaries = Collections.emptyMap();
        } else {
            regionSummaries = Collections.unmodifiableMap(new LinkedHashMap<>(summariesByRegionId));
        }
        pendingSelection = null;
        filterQuery = "";
        filterEditing = false;
        lastRowClickIndex = -1;
        rebuildVisibleEntries();
        selectedIndex = 0;
        if (selectedRegionId != null && !selectedRegionId.isEmpty()) {
            final int index = visibleRegionIds.indexOf(selectedRegionId);
            if (index >= 0) {
                selectedIndex = index;
            }
        }
        if (selectedIndex == 0 && visibleRegionIds.size() > 1) {
            final int defaultIndex = visibleRegionIds.indexOf("default");
            if (defaultIndex >= 0) {
                selectedIndex = defaultIndex;
            }
        }
        ensureSelectionVisible();
        open = true;
    }

    public void cancel() {
        open = false;
        pendingSelection = null;
        filterEditing = false;
    }

    public Optional<String> takeSelection() {
        final String selection = pendingSelection;
        pendingSelection = null;
        return Optional.ofNullable(selection);
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
            confirmSelection();
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

    public boolean onTouchDown(final int screenX, final int screenY, final int viewportWidth, final int viewportHeight) {
        if (!open) {
            return false;
        }
        final PanelLayout layout = layout(viewportWidth, viewportHeight);
        if (hitApply(screenX, screenY, layout)) {
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
                confirmSelection();
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
        font.draw(batch, "Region settings", layout.panelX + MARGIN, layout.panelY + PANEL_HEIGHT - MARGIN);
        font.draw(
            batch,
            "Layout demos: preview_* — most BN regions only change submap content",
            layout.panelX + MARGIN,
            layout.panelY + PANEL_HEIGHT - MARGIN - 16
        );

        drawFilterRow(batch, font, whitePixel, layout);
        drawList(batch, font, whitePixel, layout);
        drawSelectedSummary(batch, font, layout);
        drawButtons(batch, font, whitePixel, layout);
        font.setColor(old);
    }

    private void drawSelectedSummary(
        final SpriteBatch batch,
        final BitmapFont font,
        final PanelLayout layout
    ) {
        if (visibleRegionIds.isEmpty()) {
            return;
        }
        final String selectedRegion = visibleRegionIds.get(selectedIndex);
        final String summary = regionSummaries.getOrDefault(selectedRegion, "no profile summary");
        final float y = layout.buttonY + BUTTON_HEIGHT + 16;
        font.setColor(0.82f, 0.86f, 0.92f, 1f);
        font.draw(batch, "Selected: " + selectedRegion, layout.panelX + MARGIN, y + 16);
        font.setColor(0.70f, 0.75f, 0.84f, 1f);
        font.draw(
            batch,
            fitText(font, summary, PANEL_WIDTH - MARGIN * 2 - 8),
            layout.panelX + MARGIN,
            y
        );
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
        if (visibleRegionIds.isEmpty()) {
            font.setColor(0.55f, 0.58f, 0.64f, 1f);
            font.draw(batch, "(no regions loaded)", layout.panelX + MARGIN + 4, layout.listTop - ROW_HEIGHT);
            font.setColor(0.92f, 0.94f, 0.98f, 1f);
            return;
        }
        for (int row = 0; row < rows; row++) {
            final int index = scrollOffset + row;
            if (index >= visibleRegionIds.size()) {
                break;
            }
            final String regionId = visibleRegionIds.get(index);
            final float y = layout.listTop - (row + 1) * ROW_HEIGHT;
            if (index == selectedIndex) {
                batch.setColor(0.24f, 0.28f, 0.38f, 1f);
                batch.draw(whitePixel, layout.panelX + MARGIN, y, PANEL_WIDTH - MARGIN * 2, ROW_HEIGHT);
                batch.setColor(Color.WHITE);
            }
            font.setColor(index == selectedIndex ? 0.95f : 0.88f, 0.9f, 0.94f, 1f);
            font.draw(
                batch,
                fitText(font, regionId, PANEL_WIDTH - MARGIN * 2 - 8),
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
        drawButton(batch, font, whitePixel, "Apply", layout.applyX, layout.buttonY, layout.applyW);
        drawButton(batch, font, whitePixel, "Cancel", layout.cancelX, layout.buttonY, layout.applyW);
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

    private void confirmSelection() {
        if (visibleRegionIds.isEmpty()) {
            return;
        }
        pendingSelection = visibleRegionIds.get(selectedIndex);
        open = false;
        filterEditing = false;
    }

    private void moveSelection(final int delta) {
        if (visibleRegionIds.isEmpty()) {
            return;
        }
        selectedIndex = Math.max(0, Math.min(visibleRegionIds.size() - 1, selectedIndex + delta));
        ensureSelectionVisible();
    }

    private void rebuildVisibleEntries() {
        final String needle = filterQuery.trim().toLowerCase(Locale.ROOT);
        final List<String> next = new ArrayList<>();
        for (final String regionId : allRegionIds) {
            if (needle.isEmpty() || regionId.toLowerCase(Locale.ROOT).contains(needle)) {
                next.add(regionId);
            }
        }
        visibleRegionIds = next;
        clampSelection();
        ensureSelectionVisible();
    }

    private void clampSelection() {
        if (visibleRegionIds.isEmpty()) {
            selectedIndex = 0;
            scrollOffset = 0;
            return;
        }
        selectedIndex = Math.max(0, Math.min(visibleRegionIds.size() - 1, selectedIndex));
    }

    private void ensureSelectionVisible() {
        final int rows = visibleRowCount();
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + rows) {
            scrollOffset = selectedIndex - rows + 1;
        }
        scrollOffset = Math.max(0, scrollOffset);
    }

    private int visibleRowCount() {
        return Math.max(1, (PANEL_HEIGHT - 150) / ROW_HEIGHT);
    }

    private static String fitText(final BitmapFont font, final String text, final int maxWidth) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        final GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        if (layout.width <= maxWidth) {
            return text;
        }
        String trimmed = text;
        while (trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
            layout.setText(font, trimmed + "…");
            if (layout.width <= maxWidth) {
                return trimmed + "…";
            }
        }
        return trimmed;
    }

    private PanelLayout layout(final int viewportWidth, final int viewportHeight) {
        final float panelX = (viewportWidth - PANEL_WIDTH) / 2f;
        final float panelY = (viewportHeight - PANEL_HEIGHT) / 2f;
        final float listTop = panelY + PANEL_HEIGHT - 72;
        final float buttonY = panelY + MARGIN;
        final float buttonW = 96f;
        final float gap = 12f;
        final float applyX = panelX + PANEL_WIDTH - MARGIN - buttonW * 2 - gap;
        final float cancelX = panelX + PANEL_WIDTH - MARGIN - buttonW;
        return new PanelLayout(panelX, panelY, listTop, buttonY, applyX, cancelX, buttonW);
    }

    private int rowAt(final int screenX, final int screenY, final PanelLayout layout) {
        if (screenX < layout.panelX + MARGIN || screenX > layout.panelX + PANEL_WIDTH - MARGIN) {
            return -1;
        }
        final float firstRowBottom = layout.listTop - ROW_HEIGHT;
        if (screenY > layout.listTop || screenY < firstRowBottom - (visibleRowCount() - 1) * ROW_HEIGHT) {
            return -1;
        }
        final int row = (int) ((layout.listTop - screenY - 1) / ROW_HEIGHT);
        if (row < 0 || row >= visibleRowCount()) {
            return -1;
        }
        return row;
    }

    private static boolean hitApply(final int screenX, final int screenY, final PanelLayout layout) {
        return screenX >= layout.applyX && screenX <= layout.applyX + layout.applyW
            && screenY >= layout.buttonY && screenY <= layout.buttonY + BUTTON_HEIGHT;
    }

    private static boolean hitCancel(final int screenX, final int screenY, final PanelLayout layout) {
        return screenX >= layout.cancelX && screenX <= layout.cancelX + layout.applyW
            && screenY >= layout.buttonY && screenY <= layout.buttonY + BUTTON_HEIGHT;
    }

    private static boolean hitFilter(final int screenX, final int screenY, final PanelLayout layout) {
        final float filterY = layout.listTop + 8;
        return screenY >= filterY && screenY <= filterY + ROW_HEIGHT
            && screenX >= layout.panelX + MARGIN
            && screenX <= layout.panelX + PANEL_WIDTH - MARGIN;
    }

    private static final class PanelLayout {
        private final float panelX;
        private final float panelY;
        private final float listTop;
        private final float buttonY;
        private final float applyX;
        private final float cancelX;
        private final float applyW;

        private PanelLayout(
            final float panelX,
            final float panelY,
            final float listTop,
            final float buttonY,
            final float applyX,
            final float cancelX,
            final float applyW
        ) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.listTop = listTop;
            this.buttonY = buttonY;
            this.applyX = applyX;
            this.cancelX = cancelX;
            this.applyW = applyW;
        }
    }
}
