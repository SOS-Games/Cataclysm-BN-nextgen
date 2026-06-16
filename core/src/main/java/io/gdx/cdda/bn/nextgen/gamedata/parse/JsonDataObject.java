package io.gdx.cdda.bn.nextgen.gamedata.parse;

import com.badlogic.gdx.utils.JsonValue;

import java.nio.file.Path;

/** One JSON object with a {@code type} field from a game data file. */
public final class JsonDataObject {

    private final String type;
    private final Path sourceFile;
    private final JsonValue root;

    public JsonDataObject(final String type, final Path sourceFile, final JsonValue root) {
        this.type = type;
        this.sourceFile = sourceFile;
        this.root = root;
    }

    public String getType() {
        return type;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public JsonValue getRoot() {
        return root;
    }
}
