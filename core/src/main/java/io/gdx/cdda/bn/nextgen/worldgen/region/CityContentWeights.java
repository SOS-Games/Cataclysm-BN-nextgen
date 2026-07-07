package io.gdx.cdda.bn.nextgen.worldgen.region;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Weighted OMT id tables from region {@code city} block (W17a). */
public final class CityContentWeights {

    private final Map<String, Integer> houses;
    private final Map<String, Integer> shops;
    private final Map<String, Integer> parks;
    private final Map<String, Integer> finales;

    public CityContentWeights(
        final Map<String, Integer> houses,
        final Map<String, Integer> shops,
        final Map<String, Integer> parks,
        final Map<String, Integer> finales
    ) {
        this.houses = copyWeights(houses);
        this.shops = copyWeights(shops);
        this.parks = copyWeights(parks);
        this.finales = copyWeights(finales);
    }

    public static CityContentWeights empty() {
        return new CityContentWeights(
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyMap()
        );
    }

    public static CityContentWeights housesOnly(final Map<String, Integer> houses) {
        return new CityContentWeights(houses, Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
    }

    public Map<String, Integer> getHouses() {
        return houses;
    }

    public Map<String, Integer> getShops() {
        return shops;
    }

    public Map<String, Integer> getParks() {
        return parks;
    }

    public Map<String, Integer> getFinales() {
        return finales;
    }

    public boolean hasHouseWeights() {
        return !houses.isEmpty();
    }

    public boolean hasUrbanOmtTables() {
        return !houses.isEmpty() || !shops.isEmpty() || !parks.isEmpty() || !finales.isEmpty();
    }

    private static Map<String, Integer> copyWeights(final Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
