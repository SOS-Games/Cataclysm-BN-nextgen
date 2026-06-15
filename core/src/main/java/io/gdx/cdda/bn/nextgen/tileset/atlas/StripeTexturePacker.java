package io.gdx.cdda.bn.nextgen.tileset.atlas;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Stripe row packer for dynamic atlas pages (A1 / BN {@code stripe_texture_packer}). */
final class StripeTexturePacker {

    static final class Rect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        Rect(final int x, final int y, final int width, final int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        int getWidth() {
            return width;
        }

        int getHeight() {
            return height;
        }
    }

    private static final class Stripe {
        private final int height;
        private final int yOffset;
        private int xRemainder;

        Stripe(final int height, final int yOffset, final int xRemainder) {
            this.height = height;
            this.yOffset = yOffset;
            this.xRemainder = xRemainder;
        }
    }

    private final int boundsWidth;
    private final int boundsHeight;
    private final int minSize;
    private final List<Stripe> stripes = new ArrayList<>();
    private int yRemainder;

    StripeTexturePacker(final int boundsWidth, final int boundsHeight, final int minSize) {
        this.boundsWidth = boundsWidth;
        this.boundsHeight = boundsHeight;
        this.minSize = minSize;
        this.yRemainder = boundsHeight;
    }

    Optional<Rect> pack(final int width, final int height) {
        if (width > boundsWidth || height > boundsHeight) {
            return Optional.empty();
        }
        final int roundedHeight = roundUp(height, minSize);
        Stripe stripe = findStripe(width, roundedHeight);
        if (stripe == null) {
            if (roundedHeight > yRemainder || yRemainder < minSize) {
                return Optional.empty();
            }
            final int yOffset = boundsHeight - yRemainder;
            stripe = new Stripe(roundedHeight, yOffset, boundsWidth);
            stripes.add(stripe);
            yRemainder -= roundedHeight;
        }
        final int xOffset = boundsWidth - stripe.xRemainder;
        final Rect rect = new Rect(xOffset, stripe.yOffset, width, height);
        stripe.xRemainder -= width;
        if (stripe.xRemainder < minSize) {
            stripe.xRemainder = 0;
        }
        return Optional.of(rect);
    }

    private Stripe findStripe(final int width, final int roundedHeight) {
        for (final Stripe stripe : stripes) {
            if (stripe.xRemainder >= width && stripe.height == roundedHeight) {
                return stripe;
            }
        }
        return null;
    }

    private static int roundUp(final int value, final int multiple) {
        if (multiple <= 0) {
            return value;
        }
        return ((value + multiple - 1) / multiple) * multiple;
    }
}
