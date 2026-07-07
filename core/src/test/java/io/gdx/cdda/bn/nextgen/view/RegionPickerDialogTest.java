package io.gdx.cdda.bn.nextgen.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionPickerDialogTest {

    private static final int VIEWPORT_WIDTH = 800;
    private static final int VIEWPORT_HEIGHT = 600;

    private RegionPickerDialog dialog;

    @BeforeEach
    void setUp() {
        dialog = new RegionPickerDialog();
        dialog.open(Arrays.asList("default", "forest_trails", "urban_heavy"), "default");
    }

    @Test
    void rowHitTestAlignsWithFirstVisibleEntry() {
        clickListRow(0);
        clickApply();
        assertEquals("default", dialog.takeSelection().orElse(""));
    }

    @Test
    void secondRowHitTestSelectsForestTrails() {
        clickListRow(1);
        clickApply();
        assertEquals("forest_trails", dialog.takeSelection().orElse(""));
    }

    private void clickListRow(final int row) {
        dialog.onTouchDown(listClickX(), listClickY(row), VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
    }

    private void clickApply() {
        final float panelX = (VIEWPORT_WIDTH - 480) / 2f;
        final float panelY = (VIEWPORT_HEIGHT - 360) / 2f;
        final float buttonY = panelY + 12;
        final float applyX = panelX + 480 - 12 - 96 * 2 - 12;
        dialog.onTouchDown((int) (applyX + 10), (int) (buttonY + 10), VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
    }

    private static int listClickX() {
        final float panelX = (VIEWPORT_WIDTH - 480) / 2f;
        return (int) (panelX + 20);
    }

    private static int listClickY(final int row) {
        final float panelY = (VIEWPORT_HEIGHT - 360) / 2f;
        final float listTop = panelY + 360 - 72;
        final int rowHeight = 20;
        return (int) (listTop - row * rowHeight - rowHeight / 2);
    }
}
