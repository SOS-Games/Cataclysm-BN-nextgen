package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** Startup menu: choose sprite viewer or map editor. */
public final class MainMenuScreen {

    private static final String TITLE = "Cataclysm BN Nextgen";
    private static final String[] ITEM_LABELS = {
        "Sprite Viewer",
        "Map Editor"
    };
    private static final String[] ITEM_HINTS = {
        "Browse loaded tileset sprites",
        "Paint terrain on a grid (M2 preview)"
    };

    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Runnable onSpriteViewer;
    private final Runnable onMapEditor;

    private int selectedIndex;

    public MainMenuScreen(
        final SpriteBatch batch,
        final Runnable onSpriteViewer,
        final Runnable onMapEditor
    ) {
        this.batch = batch;
        this.font = new BitmapFont();
        this.onSpriteViewer = onSpriteViewer;
        this.onMapEditor = onMapEditor;
    }

    public void render() {
        ScreenUtils.clear(0.10f, 0.11f, 0.15f, 1f);
        batch.getProjectionMatrix().setToOrtho2D(
            0f,
            0f,
            viewportWidth(),
            viewportHeight()
        );

        batch.begin();
        drawMenu();
        batch.end();
    }

    public boolean onKeyDown(final int keycode) {
        if (keycode == Keys.UP || keycode == Keys.W) {
            selectedIndex = (selectedIndex - 1 + ITEM_LABELS.length) % ITEM_LABELS.length;
            return true;
        }
        if (keycode == Keys.DOWN || keycode == Keys.S) {
            selectedIndex = (selectedIndex + 1) % ITEM_LABELS.length;
            return true;
        }
        if (keycode == Keys.ENTER || keycode == Keys.SPACE) {
            activateSelection();
            return true;
        }
        if (keycode == Keys.NUM_1) {
            selectedIndex = 0;
            activateSelection();
            return true;
        }
        if (keycode == Keys.NUM_2) {
            selectedIndex = 1;
            activateSelection();
            return true;
        }
        return false;
    }

    public boolean onTouchDown(final int screenX, final int screenY) {
        final int y = ScreenInput.fromInputY(screenY);
        final int hit = hitTest(screenX, y);
        if (hit < 0) {
            return false;
        }
        selectedIndex = hit;
        activateSelection();
        return true;
    }

    public boolean onMouseMoved(final int screenX, final int screenY) {
        final int y = ScreenInput.fromInputY(screenY);
        final int hit = hitTest(screenX, y);
        if (hit >= 0) {
            selectedIndex = hit;
        }
        return false;
    }

    public void dispose() {
        font.dispose();
    }

    private void drawMenu() {
        final int centerX = viewportWidth() / 2;
        final int centerY = viewportHeight() / 2;

        glyphLayout.setText(font, TITLE);
        font.draw(batch, TITLE, centerX - glyphLayout.width / 2f, centerY + 96);

        final int itemStartY = centerY + 24;
        final int itemStep = 56;
        for (int i = 0; i < ITEM_LABELS.length; i++) {
            final int itemY = itemStartY - i * itemStep;
            final boolean selected = i == selectedIndex;
            final String prefix = selected ? "> " : "  ";
            final String line = prefix + (i + 1) + ". " + ITEM_LABELS[i];
            glyphLayout.setText(font, line);
            font.draw(batch, line, centerX - glyphLayout.width / 2f, itemY);
            glyphLayout.setText(font, ITEM_HINTS[i]);
            font.draw(
                batch,
                ITEM_HINTS[i],
                centerX - glyphLayout.width / 2f,
                itemY - 18
            );
        }

        final String controls = "[Up/Down] select  [Enter] open  [1/2] shortcut  [Click] item";
        glyphLayout.setText(font, controls);
        font.draw(batch, controls, centerX - glyphLayout.width / 2f, 48);
    }

    private void activateSelection() {
        if (selectedIndex == 0) {
            onSpriteViewer.run();
        } else {
            onMapEditor.run();
        }
    }

    private int hitTest(final int screenX, final int screenY) {
        final int centerX = viewportWidth() / 2;
        final int centerY = viewportHeight() / 2;
        final int itemStartY = centerY + 24;
        final int itemStep = 56;
        final int halfWidth = 220;
        final int halfHeight = 28;

        for (int i = 0; i < ITEM_LABELS.length; i++) {
            final int itemY = itemStartY - i * itemStep;
            final int left = centerX - halfWidth;
            final int right = centerX + halfWidth;
            final int top = itemY + halfHeight;
            final int bottom = itemY - halfHeight - 18;
            if (screenX >= left && screenX <= right && screenY >= bottom && screenY <= top) {
                return i;
            }
        }
        return -1;
    }

    private static int viewportWidth() {
        return ScreenInput.viewportWidth();
    }

    private static int viewportHeight() {
        return ScreenInput.viewportHeight();
    }
}
