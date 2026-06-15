package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/** Simple rotating arc spinner drawn over the sprite batch. */
final class LoadingSpinner {

    private final ShapeRenderer shapes = new ShapeRenderer();
    private float angle;

    void update(final float deltaSeconds) {
        angle = (angle + deltaSeconds * 280f) % 360f;
    }

    void draw(final SpriteBatch batch, final int centerX, final int centerY) {
        batch.flush();
        shapes.setProjectionMatrix(batch.getProjectionMatrix());
        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.45f, 0.68f, 1f, 1f);
        shapes.arc(centerX, centerY, 28f, angle, 300f);
        shapes.end();
        batch.setColor(1f, 1f, 1f, 1f);
    }

    void dispose() {
        shapes.dispose();
    }
}
