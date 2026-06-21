package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parsed {@code subtype: mutable} overmap special (W6). */
public final class MutableSpecialDefinition {

    private final String id;
    private final Path sourceFile;
    private final String rootPieceId;
    private final Map<String, MutableOvermapNode> nodes;
    private final List<MutableSpecialPhase> phases;
    private final Map<String, String> joinOpposites;

    public MutableSpecialDefinition(
        final String id,
        final String rootPieceId,
        final Map<String, MutableOvermapNode> nodes,
        final List<MutableSpecialPhase> phases,
        final Map<String, String> joinOpposites
    ) {
        this(id, null, rootPieceId, nodes, phases, joinOpposites);
    }

    public MutableSpecialDefinition(
        final String id,
        final Path sourceFile,
        final String rootPieceId,
        final Map<String, MutableOvermapNode> nodes,
        final List<MutableSpecialPhase> phases,
        final Map<String, String> joinOpposites
    ) {
        this.id = id;
        this.sourceFile = sourceFile;
        this.rootPieceId = rootPieceId;
        this.nodes = nodes == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        this.phases = phases == null ? Collections.emptyList() : phases;
        this.joinOpposites = joinOpposites == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(new LinkedHashMap<>(joinOpposites));
    }

    public String getId() {
        return id;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    public String getRootPieceId() {
        return rootPieceId;
    }

    public Map<String, MutableOvermapNode> getNodes() {
        return nodes;
    }

    public MutableOvermapNode getNode(final String pieceId) {
        return nodes.get(pieceId);
    }

    public List<MutableSpecialPhase> getPhases() {
        return phases;
    }

    public Map<String, String> getJoinOpposites() {
        return joinOpposites;
    }
}
