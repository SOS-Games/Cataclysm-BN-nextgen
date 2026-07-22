package io.gdx.cdda.bn.nextgen.worldgen.explore;

/** Pure math for seamless walkaround camera / OMT focus (unit-tested). */
public final class LocalExploreMath {

    private LocalExploreMath() {}

    /**
     * Camera X delta so a fixed world OMT stays at the same screen X after the patch origin shifts.
     * {@code cameraX +=} this value after replacing the grid.
     */
    public static float cameraDeltaX(
        final int oldPatchMinOmtX,
        final int newPatchMinOmtX,
        final int stride,
        final float tilePx
    ) {
        return (newPatchMinOmtX - oldPatchMinOmtX) * stride * tilePx;
    }

    /**
     * Camera Y delta for the editor's y-up grid where row 0 is at the top.
     * Accounts for patch origin shift and optional canvas height change near overmap edges.
     */
    public static float cameraDeltaY(
        final int oldPatchMinOmtY,
        final int newPatchMinOmtY,
        final int oldGridHeight,
        final int newGridHeight,
        final int stride,
        final float tilePx
    ) {
        return (oldPatchMinOmtY - newPatchMinOmtY) * stride * tilePx
            + (oldGridHeight - newGridHeight) * tilePx;
    }

    /** Grid cell under the viewport center (matches {@code centerCameraOnCell} inverse). */
    public static int focusCellX(final float cameraX, final float viewportCenterX, final float tilePx) {
        if (tilePx <= 0f) {
            return 0;
        }
        return Math.round((viewportCenterX - cameraX) / tilePx - 0.5f);
    }

    /**
     * Grid cell Y under the vertical midpoint between grid base and top
     * (same center used by {@code centerCameraOnCell}).
     */
    public static int focusCellY(
        final float cameraY,
        final float hudHeight,
        final int gridHeight,
        final float viewportMidY,
        final float tilePx
    ) {
        if (tilePx <= 0f || gridHeight <= 0) {
            return 0;
        }
        // centerCameraOnCell: cameraY = midY - hud - gridH*px + (cellY+0.5)*px
        return Math.round((cameraY + hudHeight + gridHeight * tilePx - viewportMidY) / tilePx - 0.5f);
    }

    public static int cellToOmt(final int cell, final int patchMinOmt, final int stride) {
        if (stride <= 0) {
            return patchMinOmt;
        }
        final int local = Math.floorDiv(cell, stride);
        return patchMinOmt + local;
    }

    public static int clamp(final int value, final int min, final int maxInclusive) {
        if (value < min) {
            return min;
        }
        if (value > maxInclusive) {
            return maxInclusive;
        }
        return value;
    }
}
