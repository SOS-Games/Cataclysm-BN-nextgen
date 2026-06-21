package io.gdx.cdda.bn.nextgen.view;

/** Active palette brush target for map editor painting (M5). */
public enum PaletteBrushLayer {
    TERRAIN,
    FURNITURE;

    public PaletteBrushLayer next() {
        return this == TERRAIN ? FURNITURE : TERRAIN;
    }

    public String hudLabel() {
        return name().toLowerCase();
    }
}
