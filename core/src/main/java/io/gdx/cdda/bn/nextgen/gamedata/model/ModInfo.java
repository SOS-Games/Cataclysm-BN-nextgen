package io.gdx.cdda.bn.nextgen.gamedata.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parsed {@code MOD_INFO} manifest (G5 / unit 03). */
public final class ModInfo {

    private final String id;
    private final String name;
    private final String description;
    private final Path modinfoPath;
    private final Path modDirectory;
    private final Path resolvedContentPath;
    private final boolean core;
    private final List<String> dependencies;
    private final List<String> conflicts;

    public ModInfo(
        final String id,
        final String name,
        final String description,
        final Path modinfoPath,
        final Path modDirectory,
        final Path resolvedContentPath,
        final boolean core,
        final List<String> dependencies,
        final List<String> conflicts
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.modinfoPath = modinfoPath;
        this.modDirectory = modDirectory;
        this.resolvedContentPath = resolvedContentPath;
        this.core = core;
        this.dependencies = Collections.unmodifiableList(new ArrayList<>(dependencies));
        this.conflicts = Collections.unmodifiableList(new ArrayList<>(conflicts));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Path getModinfoPath() {
        return modinfoPath;
    }

    public Path getModDirectory() {
        return modDirectory;
    }

    public Path getResolvedContentPath() {
        return resolvedContentPath;
    }

    public boolean isCore() {
        return core;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public List<String> getConflicts() {
        return conflicts;
    }
}
