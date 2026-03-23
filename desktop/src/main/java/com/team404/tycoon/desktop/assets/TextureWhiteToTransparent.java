package com.team404.tycoon.desktop.assets;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

/**
 * Keyes out flat light backgrounds (white / light gray mats) so PNGs composite on the map and HUD.
 */
public final class TextureWhiteToTransparent {

    private static final float WHITE_CUTOFF = 0.94f;
    /** Max RGB spread for "achromatic" pixels we treat as background when also light enough. */
    private static final float GRAY_BG_MAX_SAT = 0.12f;
    /** Background mats are light on all channels; real shading usually drops min lower than this. */
    private static final float GRAY_BG_MIN_LOW = 0.58f;
    private static final float GRAY_BG_MIN_HIGH = 0.68f;

    private TextureWhiteToTransparent() {
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
        // Typical exporter "plaque": uniform light gray behind the sprite
        if (sat <= GRAY_BG_MAX_SAT && min >= GRAY_BG_MIN_LOW && max >= GRAY_BG_MIN_HIGH) {
            return true;
        }
        return false;
    }

    public static Texture load(FileHandle file) {
        Pixmap src = new Pixmap(file);
        Pixmap dst = new Pixmap(src.getWidth(), src.getHeight(), Pixmap.Format.RGBA8888);
        Color c = new Color();
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                Color.rgba8888ToColor(c, src.getPixel(x, y));
                if (isBackground(c)) {
                    dst.drawPixel(x, y, 0);
                } else {
                    dst.drawPixel(x, y, Color.rgba8888(c));
                }
            }
        }
        src.dispose();
        Texture tex = new Texture(dst);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        dst.dispose();
        return tex;
    }
}
