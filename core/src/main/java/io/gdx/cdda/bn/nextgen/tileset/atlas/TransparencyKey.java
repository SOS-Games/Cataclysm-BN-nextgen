package io.gdx.cdda.bn.nextgen.tileset.atlas;

/** Optional sheet transparency / color-key settings (unit 06a). */
public final class TransparencyKey {

    private final int red;
    private final int green;
    private final int blue;

    public TransparencyKey(final int red, final int green, final int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public static TransparencyKey disabled() {
        return new TransparencyKey(-1, -1, -1);
    }

    public static TransparencyKey fromChannels(final int red, final int green, final int blue) {
        if (red < 0 || green < 0 || blue < 0) {
            return disabled();
        }
        if (red > 255 || green > 255 || blue > 255) {
            return disabled();
        }
        return new TransparencyKey(red, green, blue);
    }

    public boolean isEnabled() {
        return red >= 0 && green >= 0 && blue >= 0;
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }
}
