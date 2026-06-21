package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scrollable modal listing keyboard shortcuts. */
public final class KeybindHelpDialog {

    private static final int PANEL_WIDTH = 540;
    private static final int PANEL_HEIGHT = 460;
    private static final int MARGIN = 14;
    private static final int ROW_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 26;
    private static final int KEY_COL_WIDTH = 148;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    private boolean open;
    private List<HelpRow> rows = Collections.emptyList();
    private int scrollOffset;

    public boolean isOpen() {
        return open;
    }

    public void toggle(final List<HelpRow> rows) {
        if (open) {
            close();
            return;
        }
        open(rows);
    }

    public void open(final List<HelpRow> rows) {
        this.rows = rows == null ? Collections.emptyList() : new ArrayList<>(rows);
        scrollOffset = 0;
        open = true;
    }

    public void close() {
        open = false;
    }

    public boolean onKeyDown(final int keycode) {
        if (!open) {
            return false;
        }
        if (keycode == Keys.ESCAPE || keycode == Keys.F1 || keycode == Keys.H) {
            close();
            return true;
        }
        if (keycode == Keys.UP) {
            scrollBy(-1);
            return true;
        }
        if (keycode == Keys.DOWN) {
            scrollBy(1);
            return true;
        }
        if (keycode == Keys.PAGE_UP) {
            scrollBy(-visibleRowCount());
            return true;
        }
        if (keycode == Keys.PAGE_DOWN) {
            scrollBy(visibleRowCount());
            return true;
        }
        return true;
    }

    public boolean onScroll(final float amountY) {
        if (!open || amountY == 0f) {
            return open;
        }
        scrollOffset += (int) amountY;
        clampScroll();
        return true;
    }

    public boolean onTouchDown(final int screenX, final int screenY, final int viewportWidth, final int viewportHeight) {
        if (!open) {
            return false;
        }
        final PanelLayout layout = layout(viewportWidth, viewportHeight);
        if (hitClose(screenX, screenY, layout)) {
            close();
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
        batch.setColor(0.14f, 0.16f, 0.22f, 0.98f);
        batch.draw(whitePixel, layout.panelX, layout.panelY, PANEL_WIDTH, PANEL_HEIGHT);
        batch.setColor(Color.WHITE);

        final Color old = font.getColor().cpy();
        font.setColor(0.92f, 0.94f, 0.98f, 1f);
        font.draw(batch, "Keyboard shortcuts", layout.panelX + MARGIN, layout.panelY + PANEL_HEIGHT - MARGIN);

        float rowY = layout.listTop;
        final int end = Math.min(rows.size(), scrollOffset + visibleRowCount());
        for (int i = scrollOffset; i < end; i++) {
            final HelpRow row = rows.get(i);
            if (row.isSection()) {
                font.setColor(0.55f, 0.78f, 0.95f, 1f);
                font.draw(batch, row.getAction(), layout.panelX + MARGIN, rowY);
            } else {
                font.setColor(0.82f, 0.86f, 0.92f, 1f);
                font.draw(batch, row.getKey(), layout.panelX + MARGIN, rowY);
                font.setColor(0.92f, 0.94f, 0.98f, 1f);
                font.draw(
                    batch,
                    fitText(font, row.getAction(), layout.actionWidth),
                    layout.panelX + MARGIN + KEY_COL_WIDTH,
                    rowY
                );
            }
            rowY -= ROW_HEIGHT;
        }

        font.setColor(0.65f, 0.72f, 0.82f, 1f);
        font.draw(
            batch,
            "F1 / H / Esc close  |  wheel / PgUp PgDn scroll",
            layout.panelX + MARGIN,
            layout.buttonY + BUTTON_HEIGHT + 8
        );
        font.setColor(old);

        drawButton(batch, font, whitePixel, "Close", layout.closeX, layout.buttonY, layout.closeW);
    }

    private void scrollBy(final int delta) {
        scrollOffset += delta;
        clampScroll();
    }

    private void clampScroll() {
        final int maxOffset = Math.max(0, rows.size() - visibleRowCount());
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxOffset));
    }

    private int visibleRowCount() {
        final int listHeight = PANEL_HEIGHT - 88;
        return Math.max(1, listHeight / ROW_HEIGHT);
    }

    private String fitText(final BitmapFont font, final String text, final int maxWidth) {
        glyphLayout.setText(font, text);
        if (glyphLayout.width <= maxWidth) {
            return text;
        }
        final String ellipsis = "…";
        for (int length = text.length() - 1; length > 0; length--) {
            final String candidate = text.substring(0, length) + ellipsis;
            glyphLayout.setText(font, candidate);
            if (glyphLayout.width <= maxWidth) {
                return candidate;
            }
        }
        return ellipsis;
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
        batch.setColor(0.24f, 0.28f, 0.36f, 1f);
        batch.draw(whitePixel, x, y, width, BUTTON_HEIGHT);
        batch.setColor(Color.WHITE);
        glyphLayout.setText(font, label);
        font.draw(batch, label, x + (width - glyphLayout.width) / 2f, y + BUTTON_HEIGHT - 7);
    }

    private PanelLayout layout(final int viewportWidth, final int viewportHeight) {
        final float panelX = (viewportWidth - PANEL_WIDTH) / 2f;
        final float panelY = (viewportHeight - PANEL_HEIGHT) / 2f;
        final float listTop = panelY + PANEL_HEIGHT - 40;
        final float buttonY = panelY + MARGIN;
        final float closeW = 90f;
        final float closeX = panelX + PANEL_WIDTH - MARGIN - closeW;
        final int actionWidth = PANEL_WIDTH - MARGIN * 2 - KEY_COL_WIDTH;
        return new PanelLayout(panelX, panelY, listTop, buttonY, closeX, closeW, actionWidth);
    }

    private static boolean hitClose(final int x, final int y, final PanelLayout layout) {
        return x >= layout.closeX
            && x <= layout.closeX + layout.closeW
            && y >= layout.buttonY
            && y <= layout.buttonY + BUTTON_HEIGHT;
    }

    public static final class HelpRow {
        private final String key;
        private final String action;
        private final boolean section;

        private HelpRow(final String key, final String action, final boolean section) {
            this.key = key == null ? "" : key;
            this.action = action == null ? "" : action;
            this.section = section;
        }

        public static HelpRow section(final String title) {
            return new HelpRow("", title, true);
        }

        public static HelpRow bind(final String key, final String action) {
            return new HelpRow(key, action, false);
        }

        public String getKey() {
            return key;
        }

        public String getAction() {
            return action;
        }

        public boolean isSection() {
            return section;
        }
    }

    private static final class PanelLayout {
        private final float panelX;
        private final float panelY;
        private final float listTop;
        private final float buttonY;
        private final float closeX;
        private final float closeW;
        private final int actionWidth;

        private PanelLayout(
            final float panelX,
            final float panelY,
            final float listTop,
            final float buttonY,
            final float closeX,
            final float closeW,
            final int actionWidth
        ) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.listTop = listTop;
            this.buttonY = buttonY;
            this.closeX = closeX;
            this.closeW = closeW;
            this.actionWidth = actionWidth;
        }
    }
}
