package io.gdx.cdda.bn.nextgen.gamedata.validate;

import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.FurnitureRegistry;
import io.gdx.cdda.bn.nextgen.gamedata.model.LoadedGameData;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainDefinition;
import io.gdx.cdda.bn.nextgen.gamedata.model.TerrainRegistry;
import io.gdx.cdda.bn.nextgen.tileset.model.LoadedTileset;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Post-load game data checks (G4 / unit 10). */
public final class GameDataValidator {

    private static final String CHECK_V1 = "V1";
    private static final String CHECK_V3 = "V3";
    private static final String CHECK_V5 = "V5";
    private static final String CHECK_V6 = "V6";

    private static final String UNKNOWN_TILE_ID = "unknown";
    private static final int V6_MANY_MISSING_MIN = 5;

    private static final Logger LOG = Logger.getLogger(GameDataValidator.class.getName());

    private GameDataValidator() {}

    public static ValidationReport validate(final LoadedGameData data, final ValidationOptions options)
        throws GameDataValidationException {
        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        final List<String> infos = new ArrayList<>();

        validateNonEmptyTerrain(data.getTerrain(), errors);
        validateLooksLike(data, warnings);

        if (options.isCheckGfx() && options.getTileset() != null) {
            validateGfx(data.getTerrain(), options.getTileset(), warnings, infos);
        }

        final ValidationReport report = new ValidationReport(errors, warnings, infos);
        if (options.isFailOnError() && report.hasErrors()) {
            throw new GameDataValidationException(report);
        }
        return report;
    }

    private static void validateNonEmptyTerrain(final TerrainRegistry terrain, final List<String> errors) {
        if (terrain.size() == 0) {
            final String message = "[" + CHECK_V1 + "] terrain registry is empty after load";
            errors.add(message);
            LOG.log(Level.SEVERE, message);
        }
    }

    private static void validateLooksLike(final LoadedGameData data, final List<String> warnings) {
        final TerrainRegistry terrain = data.getTerrain();
        final FurnitureRegistry furniture = data.getFurniture();

        for (final String id : terrain.allIds()) {
            final TerrainDefinition def = terrain.find(id).orElseThrow();
            checkLooksLikeTarget(CHECK_V3, "terrain", id, def.getLooksLike(), terrain, furniture, warnings);
        }

        for (final String id : furniture.allIds()) {
            final FurnitureDefinition def = furniture.find(id).orElseThrow();
            checkLooksLikeTarget(CHECK_V3, "furniture", id, def.getLooksLike(), terrain, furniture, warnings);
        }
    }

    private static void checkLooksLikeTarget(
        final String checkId,
        final String kind,
        final String id,
        final String looksLike,
        final TerrainRegistry terrain,
        final FurnitureRegistry furniture,
        final List<String> warnings
    ) {
        if (looksLike == null || looksLike.isEmpty()) {
            return;
        }
        if (terrain.contains(looksLike) || furniture.contains(looksLike)) {
            return;
        }
        final String message = "[" + checkId + "] " + kind + " " + id
            + ": looks_like target '" + looksLike + "' not found";
        warnings.add(message);
        LOG.log(Level.WARNING, message);
    }

    private static void validateGfx(
        final TerrainRegistry terrain,
        final LoadedTileset tileset,
        final List<String> warnings,
        final List<String> infos
    ) {
        int missingGfx = 0;
        for (final String id : terrain.allIds()) {
            if (tileset.findTile(id).isEmpty()) {
                missingGfx++;
                final String message = "[" + CHECK_V5 + "] terrain " + id + ": no gfx tile";
                warnings.add(message);
            }
        }

        if (missingGfx >= V6_MANY_MISSING_MIN && tileset.findTile(UNKNOWN_TILE_ID).isPresent()) {
            final String message = "[" + CHECK_V6 + "] " + missingGfx + " terrain ids lack gfx tiles;"
                + " tileset provides '" + UNKNOWN_TILE_ID + "' fallback";
            infos.add(message);
            LOG.log(Level.INFO, message);
        }
    }
}
