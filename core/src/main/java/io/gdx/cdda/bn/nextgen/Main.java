package io.gdx.cdda.bn.nextgen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import io.gdx.cdda.bn.nextgen.tileset.TilesetDiscovery;
import io.gdx.cdda.bn.nextgen.view.MainMenuScreen;
import io.gdx.cdda.bn.nextgen.view.MapEditorScreen;
import io.gdx.cdda.bn.nextgen.view.TileDisplayScreen;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {

    private enum Mode {
        MENU,
        VIEWER,
        EDITOR
    }

    private SpriteBatch batch;
    private MainMenuScreen menu;
    private TileDisplayScreen tileDisplay;
    private MapEditorScreen mapEditor;
    private Mode mode = Mode.MENU;

    @Override
    public void create() {
        batch = new SpriteBatch();
        menu = new MainMenuScreen(batch, this::openSpriteViewer, this::openMapEditor);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(final int keycode) {
                if (keycode == Keys.ESCAPE && mode != Mode.MENU) {
                    if (mode == Mode.EDITOR && mapEditor.onEscape()) {
                        return true;
                    }
                    returnToMenu();
                    return true;
                }
                if (mode == Mode.VIEWER && keycode == Keys.E) {
                    openMapEditor();
                    return true;
                }
                if (mode == Mode.MENU) {
                    return menu.onKeyDown(keycode);
                }
                if (mode == Mode.EDITOR) {
                    return mapEditor.onKeyDown(keycode);
                }
                return tileDisplay.onKeyDown(keycode);
            }

            @Override
            public boolean keyTyped(final char character) {
                if (mode == Mode.EDITOR) {
                    return mapEditor.onKeyTyped(character);
                }
                return false;
            }

            @Override
            public boolean scrolled(final float amountX, final float amountY) {
                if (mode == Mode.EDITOR) {
                    return mapEditor.onScroll(amountY);
                }
                if (mode == Mode.VIEWER) {
                    return tileDisplay.onScroll(amountY);
                }
                return false;
            }

            @Override
            public boolean touchDown(final int screenX, final int screenY, final int pointer, final int button) {
                if (mode == Mode.MENU) {
                    return menu.onTouchDown(screenX, screenY);
                }
                if (mode == Mode.EDITOR) {
                    return mapEditor.onTouchDown(screenX, screenY, button);
                }
                return false;
            }

            @Override
            public boolean touchDragged(final int screenX, final int screenY, final int pointer) {
                if (mode == Mode.EDITOR) {
                    return mapEditor.onTouchDragged(screenX, screenY);
                }
                return false;
            }

            @Override
            public boolean touchUp(final int screenX, final int screenY, final int pointer, final int button) {
                if (mode == Mode.EDITOR) {
                    return mapEditor.onTouchUp(screenX, screenY, button);
                }
                return false;
            }

            @Override
            public boolean mouseMoved(final int screenX, final int screenY) {
                if (mode == Mode.MENU) {
                    return menu.onMouseMoved(screenX, screenY);
                }
                if (mode == Mode.EDITOR) {
                    return mapEditor.onMouseMoved(screenX, screenY);
                }
                return false;
            }
        });
    }

    @Override
    public void render() {
        switch (mode) {
            case MENU:
                menu.render();
                break;
            case EDITOR:
                mapEditor.render();
                break;
            case VIEWER:
                tileDisplay.render();
                break;
            default:
                break;
        }
    }

    @Override
    public void resize(final int width, final int height) {
        if (mode == Mode.VIEWER) {
            tileDisplay.resize(width, height);
        } else if (mode == Mode.EDITOR) {
            mapEditor.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        disposeChildScreens();
        if (menu != null) {
            menu.dispose();
        }
        batch.dispose();
    }

    private void openSpriteViewer() {
        disposeChildScreens();
        tileDisplay = new TileDisplayScreen(batch);
        tileDisplay.loadFromRegistry(TilesetDiscovery.build());
        mode = Mode.VIEWER;
    }

    private void openMapEditor() {
        disposeChildScreens();
        mapEditor = new MapEditorScreen(batch);
        mode = Mode.EDITOR;
    }

    private void returnToMenu() {
        disposeChildScreens();
        mode = Mode.MENU;
    }

    private void disposeChildScreens() {
        if (mapEditor != null) {
            mapEditor.dispose();
            mapEditor = null;
        }
        if (tileDisplay != null) {
            tileDisplay.dispose();
            tileDisplay = null;
        }
    }
}
