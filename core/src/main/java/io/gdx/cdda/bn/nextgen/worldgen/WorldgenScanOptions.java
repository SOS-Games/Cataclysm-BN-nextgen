package io.gdx.cdda.bn.nextgen.worldgen;

import io.gdx.cdda.bn.nextgen.gamedata.mod.ModConfiguration;
import io.gdx.cdda.bn.nextgen.mapgen.MapgenScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.connection.OvermapConnectionScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.mutable.MutableSpecialScanOptions;
import io.gdx.cdda.bn.nextgen.worldgen.overmap.OvermapTerrainScanOptions;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Scan options for worldgen preview (W3). */
public final class WorldgenScanOptions {

    private final MapgenScanOptions mapgenScanOptions;
    private final OvermapTerrainScanOptions overmapTerrainScanOptions;
    private final OvermapConnectionScanOptions overmapConnectionScanOptions;
    private final MutableSpecialScanOptions mutableSpecialScanOptions;

    public WorldgenScanOptions(
        final MapgenScanOptions mapgenScanOptions,
        final OvermapTerrainScanOptions overmapTerrainScanOptions,
        final OvermapConnectionScanOptions overmapConnectionScanOptions,
        final MutableSpecialScanOptions mutableSpecialScanOptions
    ) {
        this.mapgenScanOptions = mapgenScanOptions;
        this.overmapTerrainScanOptions = overmapTerrainScanOptions;
        this.overmapConnectionScanOptions = overmapConnectionScanOptions == null
            ? OvermapConnectionScanOptions.defaults()
            : overmapConnectionScanOptions;
        this.mutableSpecialScanOptions = mutableSpecialScanOptions == null
            ? MutableSpecialScanOptions.defaults()
            : mutableSpecialScanOptions;
    }

    public WorldgenScanOptions(
        final MapgenScanOptions mapgenScanOptions,
        final OvermapTerrainScanOptions overmapTerrainScanOptions,
        final OvermapConnectionScanOptions overmapConnectionScanOptions
    ) {
        this(
            mapgenScanOptions,
            overmapTerrainScanOptions,
            overmapConnectionScanOptions,
            matchingMutableOptions(overmapTerrainScanOptions)
        );
    }

    public WorldgenScanOptions(
        final MapgenScanOptions mapgenScanOptions,
        final OvermapTerrainScanOptions overmapTerrainScanOptions
    ) {
        this(
            mapgenScanOptions,
            overmapTerrainScanOptions,
            matchingConnectionOptions(overmapTerrainScanOptions),
            matchingMutableOptions(overmapTerrainScanOptions)
        );
    }

    private static OvermapConnectionScanOptions matchingConnectionOptions(
        final OvermapTerrainScanOptions overmapTerrainScanOptions
    ) {
        if (overmapTerrainScanOptions.getDataRoots().size() == 1
            && overmapTerrainScanOptions.getModIds().isEmpty()) {
            return OvermapConnectionScanOptions.fromDataRoot(
                overmapTerrainScanOptions.getDataRoots().get(0)
            );
        }
        return new OvermapConnectionScanOptions(
            overmapTerrainScanOptions.getDataRoots(),
            overmapTerrainScanOptions.getModIds()
        );
    }

    private static MutableSpecialScanOptions matchingMutableOptions(
        final OvermapTerrainScanOptions overmapTerrainScanOptions
    ) {
        if (overmapTerrainScanOptions.getDataRoots().size() == 1
            && overmapTerrainScanOptions.getModIds().isEmpty()) {
            return MutableSpecialScanOptions.fromDataRoot(
                overmapTerrainScanOptions.getDataRoots().get(0)
            );
        }
        return new MutableSpecialScanOptions(
            overmapTerrainScanOptions.getDataRoots(),
            overmapTerrainScanOptions.getModIds()
        );
    }

    public static WorldgenScanOptions defaults() {
        return new WorldgenScanOptions(
            MapgenScanOptions.defaults(),
            OvermapTerrainScanOptions.defaults()
        );
    }

    public static WorldgenScanOptions fromDataRoot(final Path dataRoot) {
        return new WorldgenScanOptions(
            MapgenScanOptions.fromDataRoot(dataRoot),
            OvermapTerrainScanOptions.fromDataRoot(dataRoot)
        );
    }

    public MapgenScanOptions getMapgenScanOptions() {
        return mapgenScanOptions;
    }

    public OvermapTerrainScanOptions getOvermapTerrainScanOptions() {
        return overmapTerrainScanOptions;
    }

    public OvermapConnectionScanOptions getOvermapConnectionScanOptions() {
        return overmapConnectionScanOptions;
    }

    public MutableSpecialScanOptions getMutableSpecialScanOptions() {
        return mutableSpecialScanOptions;
    }

    public List<Path> getDataRoots() {
        return mapgenScanOptions.getDataRoots();
    }

    public List<String> getModIds() {
        final List<String> modIds = new ArrayList<>(mapgenScanOptions.getModIds());
        for (final String modId : overmapTerrainScanOptions.getModIds()) {
            if (!modIds.contains(modId)) {
                modIds.add(modId);
            }
        }
        if (modIds.isEmpty()) {
            return ModConfiguration.activeModIdsForRoots(getDataRoots());
        }
        return Collections.unmodifiableList(modIds);
    }
}
