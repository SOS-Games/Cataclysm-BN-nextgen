package io.gdx.cdda.bn.nextgen.worldgen.overmap;

/** One mapgen method entry from BN {@code overmap_terrain} JSON (W1). */
public final class MapgenRef {

    private final String method;
    private final String omTerrain;
    private final int weight;

    public MapgenRef(final String method, final String omTerrain, final int weight) {
        this.method = method == null ? "" : method;
        this.omTerrain = omTerrain == null ? "" : omTerrain;
        this.weight = weight;
    }

    public String getMethod() {
        return method;
    }

    public String getOmTerrain() {
        return omTerrain;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isJsonMethod() {
        return "json".equals(method);
    }
}
