package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;

import io.gdx.cdda.bn.nextgen.DefaultContent;
import io.gdx.cdda.bn.nextgen.gamedata.DataPaths;
import io.gdx.cdda.bn.nextgen.gamedata.ModDiscovery;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModInfo;
import io.gdx.cdda.bn.nextgen.gamedata.model.ModRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.mod.ModConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/** Toggle enabled BN mods; persists via {@link ModConfiguration}. */
public final class ModConfigScreen {

    private static final int MARGIN = 16;
    private static final int ROW_HEIGHT = 22;
    private static final int BUTTON_HEIGHT = 28;
    private static final int HEADER_LINES = 4;

    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final TextureRegion whitePixel;
    private final Runnable onClose;

    private final List<ModRow> allRows = new ArrayList<>();
    private List<ModRow> visibleRows = Collections.emptyList();
    private final LinkedHashSet<String> enabledIds = new LinkedHashSet<>();
    private String filterQuery = "";
    private boolean filterEditing;
    private int selectedIndex;
    private int scrollOffset;
    private String statusMessage = "";
    private String sourceLabel = "";
    private boolean dirty;

    public ModConfigScreen(final SpriteBatch batch, final Runnable onClose) {
        this.batch = batch;
        this.font = new BitmapFont();
        this.whitePixel = createWhitePixel();
        this.onClose = onClose;
        reload();
    }

    public void render() {
        ScreenUtils.clear(0.10f, 0.11f, 0.15f, 1f);
        batch.getProjectionMatrix().setToOrtho2D(0f, 0f, viewportWidth(), viewportHeight());
        batch.begin();
        drawScreen();
        batch.end();
    }

    public boolean onKeyDown(final int keycode) {
        if (filterEditing) {
            if (keycode == Keys.ESCAPE) {
                filterEditing = false;
                return true;
            }
            if (keycode == Keys.BACKSPACE) {
                if (!filterQuery.isEmpty()) {
                    filterQuery = filterQuery.substring(0, filterQuery.length() - 1);
                    rebuildVisibleRows();
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
            closeWithoutSaving();
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
            moveSelection(-visibleRowCount(layout()));
            return true;
        }
        if (keycode == Keys.PAGE_DOWN) {
            moveSelection(visibleRowCount(layout()));
            return true;
        }
        if (keycode == Keys.F) {
            filterEditing = true;
            return true;
        }
        if (keycode == Keys.SPACE || keycode == Keys.ENTER) {
            toggleSelected();
            return true;
        }
        if (keycode == Keys.R) {
            resetToBnDefaults();
            return true;
        }
        if (keycode == Keys.S) {
            saveAndClose();
            return true;
        }
        return false;
    }

    public boolean onKeyTyped(final char character) {
        if (Character.isISOControl(character)) {
            return false;
        }
        if (!filterEditing) {
            filterEditing = true;
        }
        filterQuery += character;
        rebuildVisibleRows();
        return true;
    }

    public boolean onScroll(final float amountY) {
        if (visibleRows.isEmpty() || amountY == 0f) {
            return false;
        }
        scrollOffset += (int) amountY;
        clampScrollOffset();
        return true;
    }

    public boolean onTouchDown(final int screenX, final int screenY) {
        final int y = ScreenInput.fromInputY(screenY);
        final Layout layout = layout();
        if (hitButton(screenX, y, layout.saveX, layout.buttonY, layout.saveW)) {
            saveAndClose();
            return true;
        }
        if (hitButton(screenX, y, layout.resetX, layout.buttonY, layout.resetW)) {
            resetToBnDefaults();
            return true;
        }
        if (hitButton(screenX, y, layout.cancelX, layout.buttonY, layout.cancelW)) {
            closeWithoutSaving();
            return true;
        }
        if (hitFilter(screenX, y, layout)) {
            filterEditing = true;
            return true;
        }
        final int row = rowAt(screenX, y, layout);
        if (row >= 0) {
            selectedIndex = scrollOffset + row;
            clampSelection();
            toggleSelected();
            return true;
        }
        return false;
    }

    public void dispose() {
        font.dispose();
        whitePixel.getTexture().dispose();
    }

    private void reload() {
        allRows.clear();
        enabledIds.clear();
        enabledIds.addAll(ModConfiguration.activeModIds());
        sourceLabel = ModConfiguration.hasUserSelection()
            ? "Using saved mod selection"
            : "Using BN mods/default.json";

        try {
            final ModRegistry registry = ModDiscovery.discover(DataPaths.gameDataRoots());
            for (final String warning : registry.getDiscoveryWarnings()) {
                Gdx.app.log("mods", warning);
            }
            final List<String> ids = new ArrayList<>(registry.allIds());
            ids.sort(Comparator.comparing(String::toLowerCase));
            for (final String modId : ids) {
                final ModInfo info = registry.find(modId).orElse(null);
                if (info == null) {
                    continue;
                }
                allRows.add(new ModRow(modId, displayName(info), info.isCore()));
            }
            if (allRows.isEmpty()) {
                statusMessage = "No mods found (set -Dcdda.data.roots=...)";
            } else {
                statusMessage = allRows.size() + " mods discovered, " + enabledIds.size() + " enabled";
            }
        } catch (final IOException e) {
            statusMessage = "Mod discovery failed: " + e.getMessage();
            Gdx.app.error("mods", statusMessage, e);
        }

        ensureCoreEnabled();
        dirty = false;
        filterQuery = "";
        filterEditing = false;
        selectedIndex = 0;
        scrollOffset = 0;
        rebuildVisibleRows();
    }

    private static String displayName(final ModInfo info) {
        final String name = info.getName();
        if (name == null || name.isEmpty()) {
            return info.getId();
        }
        return name;
    }

    private void drawScreen() {
        final Layout layout = layout();
        final Color old = font.getColor().cpy();
        font.setColor(0.92f, 0.94f, 0.98f, 1f);
        font.draw(batch, "Configure Mods", layout.panelX + MARGIN, layout.panelY + layout.panelH - MARGIN);
        font.setColor(0.72f, 0.78f, 0.88f, 1f);
        font.draw(batch, sourceLabel, layout.panelX + MARGIN, layout.panelY + layout.panelH - MARGIN - 18);
        font.draw(batch, statusMessage, layout.panelX + MARGIN, layout.panelY + layout.panelH - MARGIN - 36);
        font.setColor(0.92f, 0.94f, 0.98f, 1f);

        drawFilterRow(layout);
        drawList(layout);
        drawButtons(layout);

        final String hint = "[Space] toggle  [F] filter  [S] save  [R] reset to BN defaults  [Esc] cancel";
        font.setColor(0.65f, 0.70f, 0.78f, 1f);
        font.draw(batch, fitText(hint, (int) layout.panelW - MARGIN * 2), layout.panelX + MARGIN, layout.panelY + 8);
        font.setColor(old);
    }

    private void drawFilterRow(final Layout layout) {
        final float filterY = layout.listTop + 8;
        font.draw(batch, "Filter:", layout.panelX + MARGIN, filterY + ROW_HEIGHT - 4);
        final float boxX = layout.panelX + MARGIN + 48;
        final float boxW = layout.panelW - MARGIN * 2 - 48;
        batch.setColor(0.10f, 0.11f, 0.14f, 1f);
        batch.draw(whitePixel, boxX, filterY, boxW, ROW_HEIGHT);
        batch.setColor(Color.WHITE);
        final String shown = filterQuery.isEmpty()
            ? (filterEditing ? "|" : "type to filter…")
            : filterQuery + (filterEditing ? "|" : "");
        font.draw(batch, fitText(shown, (int) boxW - 8), boxX + 4, filterY + ROW_HEIGHT - 5);
    }

    private void drawList(final Layout layout) {
        final int rows = visibleRowCount(layout);
        for (int row = 0; row < rows; row++) {
            final int index = scrollOffset + row;
            if (index >= visibleRows.size()) {
                break;
            }
            final ModRow modRow = visibleRows.get(index);
            final float y = layout.listTop - (row + 1) * ROW_HEIGHT;
            if (index == selectedIndex) {
                batch.setColor(0.24f, 0.28f, 0.38f, 1f);
                batch.draw(whitePixel, layout.panelX + MARGIN, y, layout.panelW - MARGIN * 2, ROW_HEIGHT);
                batch.setColor(Color.WHITE);
            }
            final boolean enabled = enabledIds.contains(modRow.id);
            if (modRow.core) {
                font.setColor(0.75f, 0.85f, 1f, 1f);
            } else if (enabled) {
                font.setColor(0.88f, 0.95f, 0.88f, 1f);
            } else {
                font.setColor(0.55f, 0.58f, 0.64f, 1f);
            }
            final String line = formatRow(modRow, enabled);
            font.draw(
                batch,
                fitText(line, (int) layout.panelW - MARGIN * 2 - 8),
                layout.panelX + MARGIN + 4,
                y + ROW_HEIGHT - 5
            );
        }
        font.setColor(0.92f, 0.94f, 0.98f, 1f);
    }

    private void drawButtons(final Layout layout) {
        drawButton("Save", layout.saveX, layout.buttonY, layout.saveW);
        drawButton("BN defaults", layout.resetX, layout.buttonY, layout.resetW);
        drawButton("Cancel", layout.cancelX, layout.buttonY, layout.cancelW);
    }

    private void drawButton(final String label, final float x, final float y, final float width) {
        batch.setColor(0.22f, 0.24f, 0.30f, 1f);
        batch.draw(whitePixel, x, y, width, BUTTON_HEIGHT);
        batch.setColor(Color.WHITE);
        glyphLayout.setText(font, label);
        font.draw(batch, label, x + (width - glyphLayout.width) / 2f, y + BUTTON_HEIGHT - 8);
    }

    private static String formatRow(final ModRow modRow, final boolean enabled) {
        final String mark = enabled ? "[x]" : "[ ]";
        if (modRow.core) {
            return mark + " " + modRow.id + " — " + modRow.name + "  (core, required)";
        }
        return mark + " " + modRow.id + " — " + modRow.name;
    }

    private void toggleSelected() {
        if (visibleRows.isEmpty()) {
            return;
        }
        final ModRow row = visibleRows.get(selectedIndex);
        if (row.core) {
            return;
        }
        if (enabledIds.contains(row.id)) {
            enabledIds.remove(row.id);
        } else {
            enabledIds.add(row.id);
        }
        dirty = true;
        statusMessage = enabledIds.size() + " mods enabled";
    }

    private void ensureCoreEnabled() {
        for (final ModRow row : allRows) {
            if (row.core) {
                enabledIds.add(row.id);
            }
        }
        if (enabledIds.isEmpty()) {
            enabledIds.add("bn");
        }
    }

    private void resetToBnDefaults() {
        ModConfiguration.clearUserSelection();
        enabledIds.clear();
        enabledIds.addAll(DefaultContent.defaultModIds());
        ensureCoreEnabled();
        dirty = true;
        sourceLabel = "Using BN mods/default.json";
        statusMessage = "Reset to BN defaults (" + enabledIds.size() + " mods)";
    }

    private void saveAndClose() {
        ensureCoreEnabled();
        ModConfiguration.saveEnabledModIds(new ArrayList<>(enabledIds));
        dirty = false;
        onClose.run();
    }

    private void closeWithoutSaving() {
        if (dirty) {
            reload();
        }
        onClose.run();
    }

    private void rebuildVisibleRows() {
        if (filterQuery == null || filterQuery.trim().isEmpty()) {
            visibleRows = new ArrayList<>(allRows);
        } else {
            final String needle = filterQuery.trim().toLowerCase(Locale.ROOT);
            final List<ModRow> filtered = new ArrayList<>();
            for (final ModRow row : allRows) {
                if (row.id.toLowerCase(Locale.ROOT).contains(needle)
                    || row.name.toLowerCase(Locale.ROOT).contains(needle)) {
                    filtered.add(row);
                }
            }
            visibleRows = filtered;
        }
        selectedIndex = Math.min(selectedIndex, Math.max(0, visibleRows.size() - 1));
        clampScrollOffset();
        ensureSelectionVisible();
    }

    private void moveSelection(final int delta) {
        if (visibleRows.isEmpty()) {
            return;
        }
        selectedIndex = Math.max(0, Math.min(visibleRows.size() - 1, selectedIndex + delta));
        ensureSelectionVisible();
    }

    private void ensureSelectionVisible() {
        if (visibleRows.isEmpty()) {
            scrollOffset = 0;
            selectedIndex = 0;
            return;
        }
        clampSelection();
        final int rows = visibleRowCount(layout());
        if (selectedIndex < scrollOffset) {
            scrollOffset = selectedIndex;
        } else if (selectedIndex >= scrollOffset + rows) {
            scrollOffset = selectedIndex - rows + 1;
        }
    }

    private void clampScrollOffset() {
        final int maxOffset = Math.max(0, visibleRows.size() - visibleRowCount(layout()));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }

    private void clampSelection() {
        if (visibleRows.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        selectedIndex = Math.max(0, Math.min(visibleRows.size() - 1, selectedIndex));
    }

    private int visibleRowCount(final Layout layout) {
        final int listHeight = (int) (layout.listTop - layout.buttonY - BUTTON_HEIGHT - 24);
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private String fitText(final String text, final int maxWidth) {
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

    private Layout layout() {
        final float panelW = Math.min(720, viewportWidth() - MARGIN * 2);
        final float panelH = viewportHeight() - MARGIN * 2;
        final float panelX = (viewportWidth() - panelW) / 2f;
        final float panelY = MARGIN;
        final float buttonY = panelY + MARGIN;
        final float listTop = panelY + panelH - MARGIN - HEADER_LINES * 18 - 8;
        final float saveW = 72f;
        final float resetW = 108f;
        final float cancelW = 72f;
        final float saveX = panelX + panelW - MARGIN - saveW;
        final float cancelX = saveX - 8f - cancelW;
        final float resetX = cancelX - 8f - resetW;
        return new Layout(panelX, panelY, panelW, panelH, listTop, buttonY, saveX, saveW, resetX, resetW, cancelX, cancelW);
    }

    private boolean hitButton(final int x, final int y, final float bx, final float by, final float bw) {
        return x >= bx && x <= bx + bw && y >= by && y <= by + BUTTON_HEIGHT;
    }

    private boolean hitFilter(final int x, final int y, final Layout layout) {
        final float filterY = layout.listTop + 8;
        final float boxX = layout.panelX + MARGIN + 48;
        final float boxW = layout.panelW - MARGIN * 2 - 48;
        return x >= boxX && x <= boxX + boxW && y >= filterY && y <= filterY + ROW_HEIGHT;
    }

    private int rowAt(final int x, final int y, final Layout layout) {
        if (x < layout.panelX + MARGIN || x > layout.panelX + layout.panelW - MARGIN) {
            return -1;
        }
        final float firstRowBottom = layout.listTop - ROW_HEIGHT;
        final int rows = visibleRowCount(layout);
        if (y > layout.listTop || y < firstRowBottom - (rows - 1) * ROW_HEIGHT) {
            return -1;
        }
        final int row = (int) ((layout.listTop - y - 1) / ROW_HEIGHT);
        if (row < 0 || row >= rows) {
            return -1;
        }
        return row;
    }

    private static TextureRegion createWhitePixel() {
        final Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        final Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegion(texture);
    }

    private static int viewportWidth() {
        return ScreenInput.viewportWidth();
    }

    private static int viewportHeight() {
        return ScreenInput.viewportHeight();
    }

    private static final class ModRow {
        private final String id;
        private final String name;
        private final boolean core;

        private ModRow(final String id, final String name, final boolean core) {
            this.id = id;
            this.name = name;
            this.core = core;
        }
    }

    private static final class Layout {
        private final float panelX;
        private final float panelY;
        private final float panelW;
        private final float panelH;
        private final float listTop;
        private final float buttonY;
        private final float saveX;
        private final float saveW;
        private final float resetX;
        private final float resetW;
        private final float cancelX;
        private final float cancelW;

        private Layout(
            final float panelX,
            final float panelY,
            final float panelW,
            final float panelH,
            final float listTop,
            final float buttonY,
            final float saveX,
            final float saveW,
            final float resetX,
            final float resetW,
            final float cancelX,
            final float cancelW
        ) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelW = panelW;
            this.panelH = panelH;
            this.listTop = listTop;
            this.buttonY = buttonY;
            this.saveX = saveX;
            this.saveW = saveW;
            this.resetX = resetX;
            this.resetW = resetW;
            this.cancelX = cancelX;
            this.cancelW = cancelW;
        }
    }
}
