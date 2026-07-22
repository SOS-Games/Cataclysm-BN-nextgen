package io.gdx.cdda.bn.nextgen.worldgen.explore;

import io.gdx.cdda.bn.nextgen.mapgen.compose.OmtStitchComposer;
import io.gdx.cdda.bn.nextgen.worldgen.submap.OmtNeighborhoodStitcher;
import io.gdx.cdda.bn.nextgen.worldgen.submap.VisitResult;

/**
 * In-memory session for seamless local-map walkaround after an overmap neighborhood visit.
 * Tracks the stitched patch origin and focus OMT so panning can restitch without jumping.
 */
public final class LocalExploreState {

    private boolean active;
    private int visitZ;
    private int focusOmtX = -1;
    private int focusOmtY = -1;
    private int patchMinOmtX = -1;
    private int patchMinOmtY = -1;
    private int windowWidth = OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_WIDTH;
    private int windowHeight = OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_HEIGHT;

    public boolean isActive() {
        return active;
    }

    public int getVisitZ() {
        return visitZ;
    }

    public int getFocusOmtX() {
        return focusOmtX;
    }

    public int getFocusOmtY() {
        return focusOmtY;
    }

    public int getPatchMinOmtX() {
        return patchMinOmtX;
    }

    public int getPatchMinOmtY() {
        return patchMinOmtY;
    }

    public int getWindowWidth() {
        return windowWidth;
    }

    public int getWindowHeight() {
        return windowHeight;
    }

    /** Larger axis; prefer {@link #getWindowWidth()} / {@link #getWindowHeight()}. */
    public int getWindowSize() {
        return Math.max(windowWidth, windowHeight);
    }

    /** @deprecated prefer {@link #getWindowWidth()} / {@link #getWindowHeight()}. */
    @Deprecated
    public int getRadius() {
        return Math.max(0, (Math.min(windowWidth, windowHeight) - 1) / 2);
    }

    public void clear() {
        active = false;
        visitZ = 0;
        focusOmtX = -1;
        focusOmtY = -1;
        patchMinOmtX = -1;
        patchMinOmtY = -1;
        windowWidth = OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_WIDTH;
        windowHeight = OmtNeighborhoodStitcher.DEFAULT_WALKAROUND_HEIGHT;
    }

    public void activateFromPatch(final VisitResult result, final int windowSize) {
        activateFromPatch(result, windowSize, windowSize, 0);
    }

    public void activateFromPatch(
        final VisitResult result,
        final int windowWidth,
        final int windowHeight,
        final int visitZ
    ) {
        if (result == null || !result.isPatchVisit()) {
            clear();
            return;
        }
        this.active = true;
        this.windowWidth = Math.max(1, windowWidth);
        this.windowHeight = Math.max(1, windowHeight);
        this.visitZ = visitZ;
        this.focusOmtX = result.getVisitOmtX();
        this.focusOmtY = result.getVisitOmtY();
        this.patchMinOmtX = result.getPatchMinOmtX();
        this.patchMinOmtY = result.getPatchMinOmtY();
    }

    public void updateAfterRestitch(final VisitResult result, final int focusOmtX, final int focusOmtY) {
        if (result == null || !result.isPatchVisit()) {
            return;
        }
        this.focusOmtX = focusOmtX;
        this.focusOmtY = focusOmtY;
        this.patchMinOmtX = result.getPatchMinOmtX();
        this.patchMinOmtY = result.getPatchMinOmtY();
    }

    public int omtStride() {
        return OmtStitchComposer.DEFAULT_OMT_SIZE;
    }
}
