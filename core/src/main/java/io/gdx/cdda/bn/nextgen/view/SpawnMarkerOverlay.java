package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import io.gdx.cdda.bn.nextgen.mapgen.json.SpawnMarker;

import java.util.List;

/** Debug draw for mapgen entity spawn markers (M6). */
public final class SpawnMarkerOverlay {

    private static final GlyphLayout GLYPH = new GlyphLayout();

    private SpawnMarkerOverlay() {}

    public static void draw(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final List<SpawnMarker> markers,
        final float cameraX,
        final float tilePx,
        final int gridWidth,
        final int gridHeight,
        final int firstCol,
        final int lastCol,
        final int firstRow,
        final int lastRow,
        final CellBottomY cellBottomY
    ) {
        if (markers == null || markers.isEmpty() || whitePixel == null) {
            return;
        }
        final float markerSize = Math.max(2f, tilePx * 0.35f);
        final Color old = batch.getColor().cpy();
        final Color oldFont = font.getColor().cpy();

        for (final SpawnMarker marker : markers) {
            if (marker.x < 0 || marker.y < 0 || marker.x >= gridWidth || marker.y >= gridHeight) {
                continue;
            }
            if (marker.x < firstCol || marker.x >= lastCol || marker.y < firstRow || marker.y >= lastRow) {
                continue;
            }
            final float worldX = cameraX + marker.x * tilePx;
            final float worldY = cellBottomY.bottomY(marker.y);
            final float inset = (tilePx - markerSize) / 2f;
            if (marker.kind == SpawnMarker.Kind.ITEM_GROUP) {
                batch.setColor(1f, 0.92f, 0.2f, 0.9f);
            } else {
                batch.setColor(0.95f, 0.25f, 0.2f, 0.9f);
            }
            batch.draw(whitePixel, worldX + inset, worldY + inset, markerSize, markerSize);

            final String label = marker.label();
            if (label == null || label.isEmpty() || tilePx < 12f) {
                continue;
            }
            GLYPH.setText(font, label);
            final float labelX = worldX + (tilePx - GLYPH.width) / 2f;
            final float squareBottom = worldY + inset;
            final float labelGap = Math.max(1f, tilePx * 0.05f);
            final float labelBaseline = Math.max(
                worldY + 2f,
                squareBottom - labelGap - GLYPH.height
            );
            final float pad = 2f;
            batch.setColor(0.05f, 0.06f, 0.1f, 0.75f);
            batch.draw(
                whitePixel,
                labelX - pad,
                labelBaseline - GLYPH.height - pad,
                GLYPH.width + pad * 2f,
                GLYPH.height + pad * 2f
            );
            font.setColor(1f, 1f, 1f, 0.95f);
            font.draw(batch, label, labelX, labelBaseline);
        }

        batch.setColor(old);
        font.setColor(oldFont);
    }

    @FunctionalInterface
    public interface CellBottomY {
        float bottomY(int cellY);
    }
}
