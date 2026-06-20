package io.gdx.cdda.bn.nextgen.worldgen.mutable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Assembles mutable special layouts from phases + joins (W6 v1). */
public final class SpecialPhaseAssembler {

    private SpecialPhaseAssembler() {}

    public static Optional<AssembledSpecialLayout> assemble(
        final MutableSpecialDefinition definition,
        final Random rng,
        final List<String> warnings
    ) {
        if (definition == null) {
            return Optional.empty();
        }
        final MutableOvermapNode rootNode = definition.getNode(definition.getRootPieceId());
        if (rootNode == null) {
            addWarning(warnings, "mutable special " + definition.getId() + " missing root node");
            return Optional.empty();
        }

        final List<PlacedMutablePiece> placed = new ArrayList<>();
        placed.add(new PlacedMutablePiece(
            rootNode.getPieceId(),
            0,
            0,
            rootNode.getOvermapTerrainId()
        ));

        if (definition.getPhases().isEmpty()) {
            return Optional.of(new AssembledSpecialLayout(definition.getId(), placed));
        }

        for (final MutableSpecialPhase phase : definition.getPhases()) {
            assemblePhase(definition, phase, placed, warnings);
        }

        if (placed.size() <= 1) {
            addWarning(warnings, "mutable special " + definition.getId() + " assembled only root piece");
        }
        return Optional.of(new AssembledSpecialLayout(definition.getId(), placed));
    }

    private static void assemblePhase(
        final MutableSpecialDefinition definition,
        final MutableSpecialPhase phase,
        final List<PlacedMutablePiece> placed,
        final List<String> warnings
    ) {
        for (final MutablePhaseEntry entry : phase.getEntries()) {
            final MutableOvermapNode node = definition.getNode(entry.getPieceId());
            if (node == null) {
                addWarning(warnings, "unknown mutable piece '" + entry.getPieceId() + "' on " + definition.getId());
                continue;
            }
            int attached = 0;
            for (int i = 0; i < entry.getMaxCount(); i++) {
                if (isAlreadyPlaced(placed, node.getPieceId())) {
                    break;
                }
                final Optional<int[]> attachment = JoinMatcher.findAttachment(placed, node, definition);
                if (!attachment.isPresent()) {
                    addWarning(
                        warnings,
                        "no join edge for mutable piece '" + node.getPieceId() + "' on " + definition.getId()
                    );
                    break;
                }
                final int[] at = attachment.get();
                placed.add(new PlacedMutablePiece(
                    node.getPieceId(),
                    at[0],
                    at[1],
                    node.getOvermapTerrainId()
                ));
                attached++;
            }
            if (attached == 0 && entry.getMaxCount() > 0) {
                addWarning(warnings, "phase entry '" + entry.getPieceId() + "' did not attach on " + definition.getId());
            }
        }
    }

    private static boolean isAlreadyPlaced(final List<PlacedMutablePiece> placed, final String pieceId) {
        for (final PlacedMutablePiece piece : placed) {
            if (pieceId.equals(piece.getPieceId())) {
                return true;
            }
        }
        return false;
    }

    private static void addWarning(final List<String> warnings, final String message) {
        if (warnings != null) {
            warnings.add(message);
        }
    }
}
