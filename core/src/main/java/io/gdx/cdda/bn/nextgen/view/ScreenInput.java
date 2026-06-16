package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.Gdx;

/** Pointer coordinates in LibGDX y-up screen space (matches SpriteBatch ortho). */
public final class ScreenInput {

    private ScreenInput() {}

    public static int pointerX() {
        return Gdx.input.getX();
    }

    public static int pointerY() {
        return fromInputY(Gdx.input.getY());
    }

    public static int rawPointerY() {
        return Gdx.input.getY();
    }

    /** GLFW reports y-down on some desktops; rendering uses y-up. */
    public static int fromInputY(final int inputY) {
        return viewportHeight() - inputY;
    }

    public static int viewportWidth() {
        return Gdx.graphics.getBackBufferWidth();
    }

    public static int viewportHeight() {
        return Gdx.graphics.getBackBufferHeight();
    }
}
