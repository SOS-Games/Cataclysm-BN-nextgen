package io.gdx.cdda.bn.nextgen.gamedata.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Post-load validation findings (G4). */
public final class ValidationReport {

    private final List<String> errors;
    private final List<String> warnings;
    private final List<String> infos;

    public ValidationReport(
        final List<String> errors,
        final List<String> warnings,
        final List<String> infos
    ) {
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.warnings = Collections.unmodifiableList(new ArrayList<>(warnings));
        this.infos = Collections.unmodifiableList(new ArrayList<>(infos));
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public List<String> getInfos() {
        return infos;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public int totalIssueCount() {
        return errors.size() + warnings.size() + infos.size();
    }
}
