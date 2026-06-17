package io.gdx.cdda.bn.nextgen.gamedata.validate;

import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;

/** Which G4 checks run and how failures are handled. */
public final class ValidationOptions {

    private final LoadedTileset tileset;
    private final boolean checkGfx;
    private final boolean failOnError;

    public ValidationOptions(
        final LoadedTileset tileset,
        final boolean checkGfx,
        final boolean failOnError
    ) {
        this.tileset = tileset;
        this.checkGfx = checkGfx;
        this.failOnError = failOnError;
    }

    public static ValidationOptions defaults() {
        return new ValidationOptions(null, false, false);
    }

    public static ValidationOptions withTileset(final LoadedTileset tileset) {
        return new ValidationOptions(tileset, true, false);
    }

    public ValidationOptions withFailOnError(final boolean failOnError) {
        return new ValidationOptions(tileset, checkGfx, failOnError);
    }

    public LoadedTileset getTileset() {
        return tileset;
    }

    public boolean isCheckGfx() {
        return checkGfx;
    }

    public boolean isFailOnError() {
        return failOnError;
    }
}
