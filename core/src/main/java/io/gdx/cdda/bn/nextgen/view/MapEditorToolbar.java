package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/** Bottom toolbar for map editor — mouse-first controls. */
public final class MapEditorToolbar {

    public static final int HEIGHT = 30;
    private static final int MARGIN = 8;
    private static final int BUTTON_PAD_X = 8;
    private static final int BUTTON_PAD_Y = 6;

    public enum Tool {
        PAINT,
        PAN,
        EYEDROPPER
    }

    public enum Action {
        NONE,
        SET_TOOL,
        ZOOM_IN,
        ZOOM_OUT,
        CENTER,
        SAVE,
        LOAD,
        OPEN_MAPGEN,
        CYCLE_GRID,
        TILESET_PREV,
        TILESET_NEXT,
        CYCLE_EDITOR_MODE,
        TOGGLE_BRUSH_LAYER,
        TOGGLE_SPAWN_OVERLAY,
        TOGGLE_OMT_PIECE_BORDERS
    }

    private static final class ButtonDef {
        private final String label;
        private final Action action;
        private final Tool tool;
        private final boolean toggle;

        private ButtonDef(final String label, final Action action, final Tool tool) {
            this(label, action, tool, false);
        }

        private ButtonDef(final String label, final Action action, final Tool tool, final boolean toggle) {
            this.label = label;
            this.action = action;
            this.tool = tool;
            this.toggle = toggle;
        }
    }

    private static final ButtonDef[] BUTTONS = {
        new ButtonDef("Paint", Action.SET_TOOL, Tool.PAINT),
        new ButtonDef("Pan", Action.SET_TOOL, Tool.PAN),
        new ButtonDef("Pick", Action.SET_TOOL, Tool.EYEDROPPER),
        new ButtonDef("Ter", Action.TOGGLE_BRUSH_LAYER, null, true),
        new ButtonDef("Spawns", Action.TOGGLE_SPAWN_OVERLAY, null, true),
        new ButtonDef("Chunks", Action.TOGGLE_OMT_PIECE_BORDERS, null, true),
        new ButtonDef("-", Action.ZOOM_OUT, null),
        new ButtonDef("+", Action.ZOOM_IN, null),
        new ButtonDef("Center", Action.CENTER, null),
        new ButtonDef("Save", Action.SAVE, null),
        new ButtonDef("Load", Action.LOAD, null),
        new ButtonDef("Mapgen", Action.OPEN_MAPGEN, null),
        new ButtonDef("Grid", Action.CYCLE_GRID, null),
        new ButtonDef("OMT", Action.CYCLE_EDITOR_MODE, null),
        new ButtonDef("<", Action.TILESET_PREV, null),
        new ButtonDef(">", Action.TILESET_NEXT, null)
    };

    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final float[] buttonX = new float[BUTTONS.length];
    private final float[] buttonW = new float[BUTTONS.length];

    private Tool activeTool = Tool.PAINT;
    private String brushLayerLabel = "Ter";
    private boolean furnitureBrushLayer;
    private boolean spawnOverlayOn;
    private boolean omtPieceBordersOn = true;

    public void setFurnitureBrushLayer(final boolean furnitureBrushLayer) {
        this.furnitureBrushLayer = furnitureBrushLayer;
        this.brushLayerLabel = furnitureBrushLayer ? "Furn" : "Ter";
    }

    public void setSpawnOverlayOn(final boolean spawnOverlayOn) {
        this.spawnOverlayOn = spawnOverlayOn;
    }

    public void setOmtPieceBordersOn(final boolean omtPieceBordersOn) {
        this.omtPieceBordersOn = omtPieceBordersOn;
    }

    public Tool getActiveTool() {
        return activeTool;
    }

    public void setActiveTool(final Tool tool) {
        activeTool = tool;
    }

    public ToolbarHit hitTest(final int screenX, final int screenY, final int canvasWidth) {
        final float barY = MARGIN;
        if (screenY < barY || screenY > barY + HEIGHT) {
            return ToolbarHit.none();
        }
        if (screenX < MARGIN || screenX >= canvasWidth - MARGIN) {
            return ToolbarHit.none();
        }
        layoutButtons(canvasWidth);
        for (int i = 0; i < BUTTONS.length; i++) {
            final float x = buttonX[i];
            final float w = buttonW[i];
            if (screenX >= x && screenX <= x + w) {
                final ButtonDef def = BUTTONS[i];
                if (def.action == Action.SET_TOOL) {
                    activeTool = def.tool;
                    return new ToolbarHit(Action.SET_TOOL, def.tool);
                }
                return new ToolbarHit(def.action, null);
            }
        }
        return ToolbarHit.none();
    }

    public void render(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final int canvasWidth
    ) {
        layoutButtons(canvasWidth);
        final float barY = MARGIN;

        batch.setColor(0.14f, 0.15f, 0.19f, 0.92f);
        batch.draw(whitePixel, MARGIN, barY, canvasWidth - MARGIN * 2f, HEIGHT);
        batch.setColor(Color.WHITE);

        final Color old = font.getColor().cpy();
        for (int i = 0; i < BUTTONS.length; i++) {
            final ButtonDef def = BUTTONS[i];
            final String label = labelFor(def);
            final boolean selected = def.action == Action.SET_TOOL && def.tool == activeTool
                || def.action == Action.TOGGLE_BRUSH_LAYER && furnitureBrushLayer
                || def.action == Action.TOGGLE_SPAWN_OVERLAY && spawnOverlayOn
                || def.action == Action.TOGGLE_OMT_PIECE_BORDERS && omtPieceBordersOn;
            if (selected) {
                batch.setColor(0.28f, 0.32f, 0.42f, 1f);
                batch.draw(whitePixel, buttonX[i], barY + 2, buttonW[i], HEIGHT - 4);
                batch.setColor(Color.WHITE);
                font.setColor(0.95f, 0.88f, 0.45f, 1f);
            } else {
                font.setColor(0.88f, 0.9f, 0.94f, 1f);
            }
            glyphLayout.setText(font, label);
            font.draw(
                batch,
                label,
                buttonX[i] + (buttonW[i] - glyphLayout.width) / 2f,
                barY + HEIGHT - BUTTON_PAD_Y
            );
        }
        font.setColor(old);
        batch.setColor(Color.WHITE);
    }

    private String labelFor(final ButtonDef def) {
        if (def.action == Action.TOGGLE_BRUSH_LAYER) {
            return brushLayerLabel;
        }
        return def.label;
    }

    private void layoutButtons(final int canvasWidth) {
        float x = MARGIN + 4;
        for (int i = 0; i < BUTTONS.length; i++) {
            buttonX[i] = x;
            buttonW[i] = estimateButtonWidth(labelFor(BUTTONS[i])) + BUTTON_PAD_X * 2f;
            x += buttonW[i] + 4;
        }
    }

    private float estimateButtonWidth(final String label) {
        if (label.length() == 1) {
            return 18f;
        }
        return label.length() * 7.5f;
    }

    public static final class ToolbarHit {
        private final Action action;
        private final Tool tool;

        private ToolbarHit(final Action action, final Tool tool) {
            this.action = action;
            this.tool = tool;
        }

        public static ToolbarHit none() {
            return new ToolbarHit(Action.NONE, null);
        }

        public Action getAction() {
            return action;
        }

        public Tool getTool() {
            return tool;
        }

        public boolean isHit() {
            return action != Action.NONE;
        }
    }
}
