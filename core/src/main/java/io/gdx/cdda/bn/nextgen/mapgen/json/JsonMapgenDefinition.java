package io.gdx.cdda.bn.nextgen.mapgen.json;

import com.badlogic.gdx.utils.JsonValue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** Parsed {@code type: mapgen} entry for catalog and preview run (P2). */
public final class JsonMapgenDefinition {

    private final List<String> omTerrain;
    private final OmTerrainGrid omTerrainGrid;
    private final String nestedMapgenId;
    private final String updateMapgenId;
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
        this(omTerrain, null, null, null, method, weight, disabled, sourceFile, indexInFile, objectRoot);
    }

    public JsonMapgenDefinition(
        final List<String> omTerrain,
        final OmTerrainGrid omTerrainGrid,
        final String method,
        final int weight,
        final boolean disabled,
        final Path sourceFile,
        final int indexInFile,
        final JsonValue objectRoot
    ) {
        this(omTerrain, omTerrainGrid, null, null, method, weight, disabled, sourceFile, indexInFile, objectRoot);
    }

    public JsonMapgenDefinition(
        final List<String> omTerrain,
        final OmTerrainGrid omTerrainGrid,
        final String nestedMapgenId,
        final String updateMapgenId,
        final String method,
        final int weight,
        final boolean disabled,
        final Path sourceFile,
        final int indexInFile,
        final JsonValue objectRoot
    ) {
        this.omTerrain = Collections.unmodifiableList(new ArrayList<>(omTerrain));
        this.omTerrainGrid = omTerrainGrid;
        this.nestedMapgenId = nestedMapgenId;
        this.updateMapgenId = updateMapgenId;
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

    public Optional<OmTerrainGrid> getOmTerrainGrid() {
        return Optional.ofNullable(omTerrainGrid);
    }

    public Optional<String> getNestedMapgenId() {
        return Optional.ofNullable(nestedMapgenId);
    }

    public Optional<String> getUpdateMapgenId() {
        return Optional.ofNullable(updateMapgenId);
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
        if (!"json".equals(method) || disabled || objectRoot == null || !objectRoot.isObject()) {
            return false;
        }
        if (updateMapgenId != null && !updateMapgenId.isEmpty()) {
            return true;
        }
        return objectRoot.has("rows");
    }

    /** True when the definition maps to an OMT tile; false for nested/update-only fragments. */
    public boolean isStandalonePickerEntry() {
        return !omTerrain.isEmpty();
    }

    public String displayName() {
        if (nestedMapgenId != null && !nestedMapgenId.isEmpty()) {
            return nestedMapgenId;
        }
        if (updateMapgenId != null && !updateMapgenId.isEmpty()) {
            return updateMapgenId;
        }
        if (!omTerrain.isEmpty()) {
            return omTerrain.get(0);
        }
        return sourceFile.getFileName().toString() + "#" + indexInFile;
    }
}
