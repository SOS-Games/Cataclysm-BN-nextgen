package io.gdx.cdda.bn.nextgen.view;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Dim panel, spinner, and wrapped progress text for incremental loads. */
final class LoadingOverlay {

    private final LoadingSpinner spinner = new LoadingSpinner();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    void update(final float deltaSeconds) {
        spinner.update(deltaSeconds);
    }

    void draw(
        final SpriteBatch batch,
        final BitmapFont font,
        final TextureRegion whitePixel,
        final int x,
        final int y,
        final int width,
        final int height,
        final String title,
        final String detail
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        batch.setColor(0.06f, 0.07f, 0.10f, 0.72f);
        batch.draw(whitePixel, x, y, width, height);
        batch.setColor(Color.WHITE);

        final int centerX = x + width / 2;
        final int centerY = y + height / 2;
        spinner.draw(batch, centerX, centerY);

        batch.setColor(Color.WHITE);
        final float maxTextWidth = Math.max(120f, width - 48f);
        final List<String> lines = buildLines(font, title, detail, maxTextWidth);
        if (lines.isEmpty()) {
            return;
        }

        final Color old = font.getColor().cpy();
        final float lineHeight = font.getLineHeight() + 2f;
        float textY = centerY - LoadingSpinner.RADIUS - 22f - lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            glyphLayout.setText(font, line);
            if (i == 0) {
                font.setColor(0.95f, 0.96f, 0.98f, 1f);
            } else {
                font.setColor(0.72f, 0.76f, 0.84f, 1f);
            }
            font.draw(
                batch,
                line,
                Math.round(centerX - glyphLayout.width * 0.5f),
                Math.round(textY)
            );
            textY -= lineHeight;
        }
        font.setColor(old);
    }

    void dispose() {
        spinner.dispose();
    }

    private List<String> buildLines(
        final BitmapFont font,
        final String title,
        final String detail,
        final float maxWidth
    ) {
        final List<String> lines = new ArrayList<>();
        if (title != null && !title.isEmpty()) {
            lines.addAll(wrapLine(font, title, maxWidth));
        }
        if (detail != null && !detail.isEmpty() && !detail.equals(title)) {
            lines.addAll(wrapLine(font, detail, maxWidth));
        }
        return lines.isEmpty() ? Collections.singletonList("Loading…") : lines;
    }

    private List<String> wrapLine(final BitmapFont font, final String text, final float maxWidth) {
        glyphLayout.setText(font, text);
        if (glyphLayout.width <= maxWidth) {
            return Collections.singletonList(text);
        }

        final List<String> lines = new ArrayList<>();
        final String[] words = text.split("\\s+");
        final StringBuilder current = new StringBuilder();
        for (final String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            final String candidate = current.length() == 0 ? word : current + " " + word;
            glyphLayout.setText(font, candidate);
            if (glyphLayout.width > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                if (current.length() > 0) {
                    current.append(' ');
                }
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }
}
