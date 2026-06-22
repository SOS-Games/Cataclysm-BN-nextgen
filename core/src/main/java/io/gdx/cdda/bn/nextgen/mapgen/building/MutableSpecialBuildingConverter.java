package io.gdx.cdda.bn.nextgen.mapgen.building;

import io.gdx.cdda.bn.nextgen.worldgen.mutable.AssembledSpecialLayout;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialDefinition;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.PlacedMutablePiece;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.SpecialPhaseAssembler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Assembles a mutable {@code overmap_special} into a preview {@link CityBuildingDefinition} (unit 11). */
public final class MutableSpecialBuildingConverter {

    private MutableSpecialBuildingConverter() {}

    public static Optional<CityBuildingDefinition> assembleBuilding(
        final MutableSpecialDefinition definition,
        final Random rng,
        final List<String> warnings
    ) {
        if (definition == null) {
            return Optional.empty();
        }
        final Optional<AssembledSpecialLayout> layout = SpecialPhaseAssembler.assemble(
            definition,
            rng == null ? new Random(0L) : rng,
            warnings
        );
        if (!layout.isPresent()) {
            return Optional.empty();
        }
        final AssembledSpecialLayout assembled = layout.get();
        if (assembled.getPieces().size() < 2) {
            return Optional.empty();
        }
        return Optional.of(fromAssembledLayout(definition, assembled));
    }

    public static CityBuildingDefinition fromAssembledLayout(
        final MutableSpecialDefinition definition,
        final AssembledSpecialLayout layout
    ) {
        return toBuilding(definition, layout);
    }

    private static CityBuildingDefinition toBuilding(
        final MutableSpecialDefinition definition,
        final AssembledSpecialLayout layout
    ) {
        final int minX = layout.getMinOffsetX();
        final int minY = layout.getMinOffsetY();
        final List<CityBuildingPiece> pieces = new ArrayList<>();
        for (final PlacedMutablePiece piece : layout.getPieces()) {
            pieces.add(new CityBuildingPiece(
                piece.getOffsetX() - minX,
                piece.getOffsetY() - minY,
                0,
                piece.resolveOvermapTerrainId()
            ));
        }
        pieces.sort(Comparator
            .comparingInt(CityBuildingPiece::getOffsetY)
            .thenComparingInt(CityBuildingPiece::getOffsetX)
            .thenComparing(CityBuildingPiece::getOvermapId));
        return new CityBuildingDefinition(
            definition.getId(),
            definition.getSourceFile(),
            pieces,
            CityBuildingDefinition.LayoutKind.OVERMAP_SPECIAL_WHOLE
        );
    }
}
