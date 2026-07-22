package io.gdx.cdda.bn.nextgen.gamedata.cache;

/**
 * Console timings for startup / catalog load. Look for {@code [load-timing]} in the run log.
 */
public final class LoadTiming {

    private static final String PREFIX = "[load-timing]";

    private LoadTiming() {}

    public static long start() {
        return System.nanoTime();
    }

    public static long msSince(final long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000L;
    }

    public static void log(final String scope, final String message) {
        System.out.println(PREFIX + " " + scope + ": " + message);
    }

    public static void logMs(final String scope, final String phase, final long startNs) {
        log(scope, phase + " " + msSince(startNs) + " ms");
    }

    /** Mutable multi-phase timer for one load pass. */
    public static final class Session {
        private final String scope;
        private final long startNs;
        private long phaseNs;

        public Session(final String scope) {
            this.scope = scope == null ? "load" : scope;
            this.startNs = System.nanoTime();
            this.phaseNs = this.startNs;
            LoadTiming.log(this.scope, "begin");
        }

        public void phase(final String label) {
            final long now = System.nanoTime();
            LoadTiming.log(scope, label + " " + ((now - phaseNs) / 1_000_000L) + " ms");
            phaseNs = now;
        }

        public void done() {
            LoadTiming.log(scope, "total " + ((System.nanoTime() - startNs) / 1_000_000L) + " ms");
        }

        public long elapsedMs() {
            return (System.nanoTime() - startNs) / 1_000_000L;
        }
    }
}
