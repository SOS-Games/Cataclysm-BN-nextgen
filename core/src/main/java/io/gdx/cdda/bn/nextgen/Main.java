package io.gdx.cdda.bn.nextgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.gdx.cdda.bn.nextgen.tileset.TilesetDiscovery;
import io.gdx.cdda.bn.nextgen.view.TileDisplayScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private SpriteBatch batch;
    private TileDisplayScreen tileDisplay;

    @Override
    public void create() {
        batch = new SpriteBatch();
        tileDisplay = new TileDisplayScreen(batch);
        tileDisplay.loadFromRegistry(TilesetDiscovery.build());
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(final int keycode) {
                return tileDisplay.onKeyDown(keycode);
            }

            @Override
            public boolean scrolled(final float amountX, final float amountY) {
                return tileDisplay.onScroll(amountY);
            }
        });
    }

    @Override
    public void render() {
        tileDisplay.render();
    }

    @Override
    public void resize(final int width, final int height) {
        tileDisplay.resize(width, height);
    }

    @Override
    public void dispose() {
        tileDisplay.dispose();
        batch.dispose();
    }
}
