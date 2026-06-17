package io.gdx.cdda.bn.nextgen.gamedata.validate;

/** Thrown when validation reports errors and {@link ValidationOptions#isFailOnError()} is set. */
public final class GameDataValidationException extends Exception {

    private final ValidationReport report;

    public GameDataValidationException(final ValidationReport report) {
        super("Game data validation failed with " + report.getErrors().size() + " error(s)");
        this.report = report;
    }

    public ValidationReport getReport() {
        return report;
    }
}
