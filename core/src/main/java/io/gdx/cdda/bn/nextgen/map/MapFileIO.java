package io.gdx.cdda.bn.nextgen.map;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** JSON persistence for nextgen map editor grid files. */
public final class MapFileIO {

    public static final String FORMAT = "cdda-bn-nextgen-map";
    public static final int VERSION = 1;

    private MapFileIO() {}

    public static void save(final Path path, final MapGrid grid) throws IOException {
        final StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"format\": \"").append(FORMAT).append("\",\n");
        json.append("  \"version\": ").append(VERSION).append(",\n");
        json.append("  \"width\": ").append(grid.width()).append(",\n");
        json.append("  \"height\": ").append(grid.height()).append(",\n");
        json.append("  \"default_terrain\": \"").append(escapeJson(grid.getDefaultTerrainId())).append("\",\n");
        json.append("  \"terrain\": [\n");

        final int size = grid.width() * grid.height();
        int index = 0;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String value = grid.get(x, y).getTerrainId();
                json.append("    \"").append(escapeJson(value)).append("\"");
                index++;
                if (index < size) {
                    json.append(",");
                }
                json.append("\n");
            }
        }
        json.append("  ],\n");

        boolean hasFurniture = false;
        for (int y = 0; y < grid.height(); y++) {
            for (int x = 0; x < grid.width(); x++) {
                final String furnitureId = grid.get(x, y).getFurnitureId();
                if (furnitureId != null && !furnitureId.isEmpty()) {
                    hasFurniture = true;
                    break;
                }
            }
            if (hasFurniture) {
                break;
            }
        }

        if (hasFurniture) {
            json.append("  \"furniture\": [\n");
            index = 0;
            for (int y = 0; y < grid.height(); y++) {
                for (int x = 0; x < grid.width(); x++) {
                    final String furnitureId = grid.get(x, y).getFurnitureId();
                    if (furnitureId == null || furnitureId.isEmpty()) {
                        json.append("    null");
                    } else {
                        json.append("    \"").append(escapeJson(furnitureId)).append("\"");
                    }
                    index++;
                    if (index < size) {
                        json.append(",");
                    }
                    json.append("\n");
                }
            }
            json.append("  ]\n");
        } else {
            json.append("  \"furniture\": null\n");
        }
        json.append("}\n");

        final Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(path, json.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static MapGrid load(final Path path) throws IOException {
        final String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        final JsonValue root = new JsonReader().parse(text);

        final String format = root.getString("format", null);
        if (!FORMAT.equals(format)) {
            throw new IOException("Invalid map format: " + format);
        }

        final int version = root.getInt("version", -1);
        if (version != VERSION) {
            throw new IOException("Unsupported map version: " + version);
        }

        final int width = root.getInt("width", -1);
        final int height = root.getInt("height", -1);
        if (width <= 0 || height <= 0) {
            throw new IOException("Invalid map dimensions: " + width + "x" + height);
        }

        final String defaultTerrain = root.getString("default_terrain", null);
        if (defaultTerrain == null || defaultTerrain.trim().isEmpty()) {
            throw new IOException("Missing default_terrain");
        }

        final JsonValue terrainArray = root.get("terrain");
        if (terrainArray == null || !terrainArray.isArray()) {
            throw new IOException("terrain must be an array");
        }

        final int expected = width * height;
        if (terrainArray.size != expected) {
            throw new IOException(
                "terrain length mismatch: expected " + expected + " but got " + terrainArray.size
            );
        }

        final MapGrid grid = new MapGrid(width, height, defaultTerrain);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int index = y * width + x;
                final JsonValue value = terrainArray.get(index);
                if (value == null) {
                    throw new IOException("terrain[" + index + "] is null");
                }
                final String terrainId = value.asString();
                if (terrainId == null || terrainId.trim().isEmpty()) {
                    throw new IOException("terrain[" + index + "] is empty");
                }
                grid.setTerrain(x, y, terrainId);
            }
        }

        final JsonValue furnitureArray = root.get("furniture");
        if (furnitureArray != null && !furnitureArray.isNull()) {
            if (!furnitureArray.isArray()) {
                throw new IOException("furniture must be an array or null");
            }
            if (furnitureArray.size != expected) {
                throw new IOException(
                    "furniture length mismatch: expected " + expected + " but got " + furnitureArray.size
                );
            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    final int index = y * width + x;
                    final JsonValue value = furnitureArray.get(index);
                    if (value == null || value.isNull()) {
                        continue;
                    }
                    final String furnitureId = value.asString();
                    if (furnitureId != null && !furnitureId.trim().isEmpty()) {
                        grid.setFurniture(x, y, furnitureId);
                    }
                }
            }
        }

        return grid;
    }

    private static String escapeJson(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
