package io.gdx.cdda.bn.nextgen.mapgen.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Options for {@link JsonMapgenRunner} (P2). */
public final class JsonMapgenRunOptions {

    private String defaultFillTer = "t_dirt";
    private final List<String> warnings = new ArrayList<>();

    public JsonMapgenRunOptions() {}

    public String getDefaultFillTer() {
        return defaultFillTer;
    }

    public JsonMapgenRunOptions withDefaultFillTer(final String defaultFillTer) {
        this.defaultFillTer = defaultFillTer;
        return this;
    }

    public List<String> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }

    void addWarning(final String warning) {
        warnings.add(warning);
    }
}
