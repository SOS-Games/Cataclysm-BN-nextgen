package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed {@code type: mapgen} entry for catalog and preview run (P2). */
public final class JsonMapgenDefinition {

    private final List<String> omTerrain;
    private final String method;
    private final int weight;
    private final boolean disabled;
    private final Path sourceFile;
    private final int indexInFile;
    private final JsonValue objectRoot;

    public JsonMapgenDefinition(
        final List<String> omTerrain,
        final String method,
        final int weight,
        final boolean disabled,
        final Path sourceFile,
        final int indexInFile,
        final JsonValue objectRoot
    ) {
        this.omTerrain = Collections.unmodifiableList(new ArrayList<>(omTerrain));
        this.method = method;
        this.weight = weight;
        this.disabled = disabled;
        this.sourceFile = sourceFile;
        this.indexInFile = indexInFile;
        this.objectRoot = objectRoot;
    }

    public List<String> getOmTerrain() {
        return omTerrain;
    }

    public String getMethod() {
        return method;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public int getIndexInFile() {
        return indexInFile;
    }

    public JsonValue getObjectRoot() {
        return objectRoot;
    }

    public boolean isJsonPreviewSupported() {
        return "json".equals(method)
            && !disabled
            && objectRoot != null
            && objectRoot.isObject()
            && objectRoot.has("rows");
    }

    public String displayName() {
        if (!omTerrain.isEmpty()) {
            return omTerrain.get(0);
        }
        return sourceFile.getFileName().toString() + "#" + indexInFile;
    }
}
