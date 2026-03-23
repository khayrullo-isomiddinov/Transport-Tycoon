package com.team404.tycoon.desktop.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;

import java.util.HashMap;
import java.util.Map;

/**
 * Visual tuning for isometric sprites (rendering only; placement footprint lives in {@code DecorationRules}).
 */
public final class DecorationMetadata {

    private static final float WHITE_CUTOFF = 0.94f;
    private static final float GRAY_BG_MAX_SAT = 0.12f;
    private static final float GRAY_BG_MIN_LOW = 0.58f;
    private static final float GRAY_BG_MIN_HIGH = 0.68f;
    private static final Map<String, SpriteMetrics> METRICS_CACHE = new HashMap<>();

    private DecorationMetadata() {
    }

    /** Extra vertical draw scale so sprites overlap the row behind (pseudo-3D). */
    public static float verticalOverlapScale(String resourcePath, int footprintW, int footprintH) {
        // Keep single-cell assets visually tighter to the anchor tile.
        return 1.0f;
    }

    /**
     * Per-asset width scale. For highway road decals, scales by measured non-background width
     * so the visible pixels match the intended tile footprint.
     */
    public static float widthScale(String resourcePath) {
        if (usesMeasuredHighwayFit(resourcePath)) {
            SpriteMetrics m = metricsFor(resourcePath);
            return 1f / m.opaqueWidthRatio;
        }
        return 1f;
    }

    /** Per-asset Y offset to remove visual gaps with terrain plane. */
    public static float groundYOffset(String resourcePath, float drawHeight) {
        if (usesMeasuredHighwayFit(resourcePath)) {
            SpriteMetrics m = metricsFor(resourcePath);
            return -drawHeight * m.bottomPaddingRatio;
        }
        return 0f;
    }

    private static boolean usesMeasuredHighwayFit(String resourcePath) {
        if (resourcePath == null) {
            return false;
        }
        String n = resourcePath.toLowerCase();
        return n.contains("highway-straight")
                || n.contains("highway-top-left")
                || n.contains("intersection");
    }

    private static SpriteMetrics metricsFor(String resourcePath) {
        return METRICS_CACHE.computeIfAbsent(resourcePath, DecorationMetadata::measureOpaqueBounds);
    }

    private static SpriteMetrics measureOpaqueBounds(String resourcePath) {
        FileHandle fh = ResourceFileHandles.resolve(resourcePath);
        if (!fh.exists()) {
            return SpriteMetrics.DEFAULT;
        }

        Pixmap pm = new Pixmap(fh);
        try {
            int w = pm.getWidth();
            int h = pm.getHeight();
            int minX = w;
            int minY = h;
            int maxX = -1;
            int maxY = -1;
            Color c = new Color();

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color.rgba8888ToColor(c, pm.getPixel(x, y));
                    if (isBackground(c)) {
                        continue;
                    }
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                }
            }

            if (maxX < minX || maxY < minY) {
                return SpriteMetrics.DEFAULT;
            }

            float opaqueW = (maxX - minX + 1f);
            float bottomPad = (h - 1f - maxY);
            float opaqueWidthRatio = clampRatio(opaqueW / w);
            float bottomPaddingRatio = clampRatio(bottomPad / h);
            return new SpriteMetrics(opaqueWidthRatio, bottomPaddingRatio);
        } finally {
            pm.dispose();
        }
    }

    private static boolean isBackground(Color c) {
        if (c.a < 0.02f) {
            return true;
        }
        if (c.r >= WHITE_CUTOFF && c.g >= WHITE_CUTOFF && c.b >= WHITE_CUTOFF) {
            return true;
        }
        float max = Math.max(c.r, Math.max(c.g, c.b));
        float min = Math.min(c.r, Math.min(c.g, c.b));
        float sat = max - min;
        return sat <= GRAY_BG_MAX_SAT && min >= GRAY_BG_MIN_LOW && max >= GRAY_BG_MIN_HIGH;
    }

    private static float clampRatio(float v) {
        return Math.max(0.001f, Math.min(1f, v));
    }

    private static final class SpriteMetrics {
        private static final SpriteMetrics DEFAULT = new SpriteMetrics(1f, 0f);

        private final float opaqueWidthRatio;
        private final float bottomPaddingRatio;

        private SpriteMetrics(float opaqueWidthRatio, float bottomPaddingRatio) {
            this.opaqueWidthRatio = opaqueWidthRatio;
            this.bottomPaddingRatio = bottomPaddingRatio;
        }
    }
}
