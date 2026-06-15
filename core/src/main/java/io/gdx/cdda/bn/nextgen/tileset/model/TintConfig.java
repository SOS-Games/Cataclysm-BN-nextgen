package io.gdx.cdda.bn.nextgen.tileset.model;

/** Entity tint for dynamic-atlas composite keys (A1 / BN {@code tint_config}). */
public final class TintConfig {

    public static final TintConfig NONE = new TintConfig(0);

    private final int colorRgba;

    public TintConfig(final int colorRgba) {
        this.colorRgba = colorRgba;
    }

    public int getColorRgba() {
        return colorRgba;
    }

    public boolean hasTint() {
        return colorRgba != 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TintConfig)) {
            return false;
        }
        final TintConfig other = (TintConfig) obj;
        return colorRgba == other.colorRgba;
    }

    @Override
    public int hashCode() {
        return colorRgba;
    }
}
