package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Modal popup for failed mapgen / building imports. Copies full error text to the clipboard. */
public final class ImportFailedDialog {

    private static final int PANEL_WIDTH = 560;
    private static final int PANEL_MIN_HEIGHT = 200;
    private static final int PANEL_MAX_HEIGHT = 420;
    private static final int MARGIN = 14;
    private static final int LINE_HEIGHT = 18;
    private static final int BUTTON_HEIGHT = 28;
    private static final int MAX_BODY_LINES = 12;

    private final GlyphLayout glyphLayout = new GlyphLayout();

    private boolean open;
    private String title = "Import failed";
    private String headline = "";
    private List<String> bodyLines = Collections.emptyList();
    private String clipboardText = "";

    public boolean isOpen() {
        return open;
    }

    public void show(final String title, final String headline, final Throwable error) {
        this.title = title == null || title.isEmpty() ? "Import failed" : title;
        this.headline = headline == null ? "" : headline;
        this.clipboardText = formatClipboardText(this.title, this.headline, error);
        Gdx.app.getClipboard().setContents(clipboardText);
        this.bodyLines = wrapBodyLines(buildBodyText(error));
        open = true;
    }

    public void dismiss() {
        open = false;
    }

    public boolean onKeyDown(final int keycode) {
        if (!open) {
            return false;
        }
        if (keycode == Keys.ENTER || keycode == Keys.ESCAPE || keycode == Keys.SPACE) {
            dismiss();
            return true;
        }
        return true;
    }

    public boolean onTouchDown(final int screenX, final int screenY, final int viewportWidth, final int viewportHeight) {
        if (!open) {
            return false;
        }
        final PanelLayout layout = layout(viewportWidth, viewportHeight);
        if (hitOk(screenX, screenY, layout)) {
            dismiss();
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

        batch.setColor(0f, 0f, 0f, 0.6f);
        batch.draw(whitePixel, 0f, 0f, viewportWidth, viewportHeight);
        batch.setColor(Color.WHITE);

        final PanelLayout layout = layout(viewportWidth, viewportHeight);
        batch.setColor(0.18f, 0.12f, 0.14f, 0.98f);
        batch.draw(whitePixel, layout.panelX, layout.panelY, PANEL_WIDTH, layout.panelHeight);
        batch.setColor(Color.WHITE);

        final Color old = font.getColor().cpy();
        float textY = layout.panelY + layout.panelHeight - MARGIN;

        font.setColor(1f, 0.55f, 0.55f, 1f);
        font.draw(batch, title, layout.panelX + MARGIN, textY);
        textY -= LINE_HEIGHT + 4;

        font.setColor(0.95f, 0.95f, 0.98f, 1f);
        if (!headline.isEmpty()) {
            font.draw(batch, fitText(font, headline, layout.textWidth), layout.panelX + MARGIN, textY);
            textY -= LINE_HEIGHT;
        }

        font.setColor(0.88f, 0.90f, 0.94f, 1f);
        for (final String line : bodyLines) {
            font.draw(batch, line, layout.panelX + MARGIN, textY);
            textY -= LINE_HEIGHT;
        }

        font.setColor(0.65f, 0.75f, 0.88f, 1f);
        font.draw(batch, "Full error copied to clipboard.", layout.panelX + MARGIN, layout.buttonY + BUTTON_HEIGHT + 8);
        font.setColor(old);

        drawButton(batch, font, whitePixel, "OK", layout.okX, layout.buttonY, layout.okW);
    }

    private static String buildBodyText(final Throwable error) {
        if (error == null) {
            return "Unknown error.";
        }
        final String message = error.getMessage();
        if (message == null || message.isEmpty()) {
            return error.getClass().getSimpleName();
        }
        return error.getClass().getSimpleName() + ": " + message;
    }

    private static String formatClipboardText(
        final String title,
        final String headline,
        final Throwable error
    ) {
        final StringWriter writer = new StringWriter();
        final PrintWriter out = new PrintWriter(writer);
        out.println(title);
        if (headline != null && !headline.isEmpty()) {
            out.println(headline);
        }
        out.println();
        if (error == null) {
            out.println("Unknown error.");
        } else {
            error.printStackTrace(out);
        }
        out.flush();
        return writer.toString();
    }

    private List<String> wrapBodyLines(final String text) {
        final List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return lines;
        }
        final int maxChars = 72;
        for (final String paragraph : text.split("\\R")) {
            String remaining = paragraph.trim();
            if (remaining.isEmpty()) {
                continue;
            }
            while (!remaining.isEmpty() && lines.size() < MAX_BODY_LINES) {
                if (remaining.length() <= maxChars) {
                    lines.add(remaining);
                    break;
                }
                int breakAt = remaining.lastIndexOf(' ', maxChars);
                if (breakAt < maxChars / 2) {
                    breakAt = maxChars;
                }
                lines.add(remaining.substring(0, breakAt).trim());
                remaining = remaining.substring(Math.min(remaining.length(), breakAt)).trim();
            }
        }
        if (lines.size() >= MAX_BODY_LINES) {
            lines.set(MAX_BODY_LINES - 1, lines.get(MAX_BODY_LINES - 1) + " …");
        }
        return lines;
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
        batch.setColor(0.28f, 0.22f, 0.24f, 1f);
        batch.draw(whitePixel, x, y, width, BUTTON_HEIGHT);
        batch.setColor(Color.WHITE);
        glyphLayout.setText(font, label);
        font.draw(batch, label, x + (width - glyphLayout.width) / 2f, y + BUTTON_HEIGHT - 8);
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

    private PanelLayout layout(final int viewportWidth, final int viewportHeight) {
        final int bodyLineCount = Math.max(1, bodyLines.size());
        final int contentHeight = MARGIN * 2
            + LINE_HEIGHT * 2
            + bodyLineCount * LINE_HEIGHT
            + LINE_HEIGHT
            + BUTTON_HEIGHT
            + 10;
        final int panelHeight = Math.max(PANEL_MIN_HEIGHT, Math.min(PANEL_MAX_HEIGHT, contentHeight));
        final float panelX = (viewportWidth - PANEL_WIDTH) / 2f;
        final float panelY = (viewportHeight - panelHeight) / 2f;
        final float buttonY = panelY + MARGIN;
        final float okW = 90f;
        final float okX = panelX + (PANEL_WIDTH - okW) / 2f;
        return new PanelLayout(panelX, panelY, panelHeight, buttonY, okX, okW, PANEL_WIDTH - MARGIN * 2);
    }

    private static boolean hitOk(final int x, final int y, final PanelLayout layout) {
        return x >= layout.okX
            && x <= layout.okX + layout.okW
            && y >= layout.buttonY
            && y <= layout.buttonY + BUTTON_HEIGHT;
    }

    private static final class PanelLayout {
        private final float panelX;
        private final float panelY;
        private final int panelHeight;
        private final float buttonY;
        private final float okX;
        private final float okW;
        private final int textWidth;

        private PanelLayout(
            final float panelX,
            final float panelY,
            final int panelHeight,
            final float buttonY,
            final float okX,
            final float okW,
            final int textWidth
        ) {
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelHeight = panelHeight;
            this.buttonY = buttonY;
            this.okX = okX;
            this.okW = okW;
            this.textWidth = textWidth;
        }
    }
}
