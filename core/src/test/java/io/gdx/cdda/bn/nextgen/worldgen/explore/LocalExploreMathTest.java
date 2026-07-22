package io.gdx.cdda.bn.nextgen.worldgen.explore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalExploreMathTest {

    @Test
    void cameraDeltaXKeepsWorldPointStableWhenPatchShiftsWest() {
        final float tilePx = 16f;
        final int stride = 24;
        // Expanding west: newMin = oldMin - 1
        final float dx = LocalExploreMath.cameraDeltaX(10, 9, stride, tilePx);
        assertEquals(-stride * tilePx, dx, 0.001f);

        final float oldCam = 100f;
        final int oldCellX = (12 - 10) * stride; // world omt 12
        final int newCellX = (12 - 9) * stride;
        final float oldScreen = oldCam + oldCellX * tilePx;
        final float newScreen = (oldCam + dx) + newCellX * tilePx;
        assertEquals(oldScreen, newScreen, 0.001f);
    }

    @Test
    void cameraDeltaYKeepsWorldPointStableWhenPatchShiftsNorth() {
        final float tilePx = 16f;
        final int stride = 24;
        final int oldH = 72;
        final int newH = 72;
        final float dy = LocalExploreMath.cameraDeltaY(10, 9, oldH, newH, stride, tilePx);
        assertEquals(stride * tilePx, dy, 0.001f);

        // Same formula as MapEditorScreen.cellBottomY for equal heights:
        // bottom = cameraY + hud + H*px - (cellY+1)*px
        final float hud = 40f;
        final float oldCam = 200f;
        final int oldCellY = (12 - 10) * stride;
        final int newCellY = (12 - 9) * stride;
        final float oldBottom = oldCam + hud + oldH * tilePx - (oldCellY + 1) * tilePx;
        final float newBottom = (oldCam + dy) + hud + newH * tilePx - (newCellY + 1) * tilePx;
        assertEquals(oldBottom, newBottom, 0.001f);
    }

    @Test
    void focusCellRoundTripsCenterCameraOnCell() {
        final float tilePx = 20f;
        final float canvasCenterX = 400f;
        final float midY = 300f;
        final float hud = 48f;
        final int gridH = 72;
        final int cellX = 30;
        final int cellY = 18;

        final float cameraX = canvasCenterX - (cellX + 0.5f) * tilePx;
        final float cameraY = midY - hud - gridH * tilePx + (cellY + 0.5f) * tilePx;

        assertEquals(cellX, LocalExploreMath.focusCellX(cameraX, canvasCenterX, tilePx));
        assertEquals(cellY, LocalExploreMath.focusCellY(cameraY, hud, gridH, midY, tilePx));
    }

    @Test
    void cellToOmtUsesPatchOrigin() {
        assertEquals(5, LocalExploreMath.cellToOmt(0, 5, 24));
        assertEquals(5, LocalExploreMath.cellToOmt(23, 5, 24));
        assertEquals(6, LocalExploreMath.cellToOmt(24, 5, 24));
        assertEquals(7, LocalExploreMath.cellToOmt(48, 5, 24));
    }
}
