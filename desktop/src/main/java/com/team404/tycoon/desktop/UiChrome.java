package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;

/**
 * Screen-space layout for pointer hit tests on the top HUD.
 * <p>
 * Lwjgl3 reports mouse {@code screenY} in the same convention as {@link Gdx#input} (GLFW):
 * origin at the <strong>top-left</strong> of the window client area, y increases downward.
 * This matches {@link com.badlogic.gdx.InputProcessor} coordinates on desktop.
 * <p>
 * Order from the top of the window down: asset strip (PNGs), then build-mode strip.
 */
public final class UiChrome {

    public static final float ASSET_BAR_HEIGHT = 76f;
    public static final float BUILD_BAR_HEIGHT = 40f;

    private UiChrome() {
    }

    public static float totalTopHeight() {
        return ASSET_BAR_HEIGHT + BUILD_BAR_HEIGHT;
    }

    /** Highest strip: PNG picker. */
    public static boolean isInAssetBar(float screenX, float screenY) {
        float w = Gdx.graphics.getWidth();
        return screenX >= 0 && screenX <= w
                && screenY >= 0 && screenY <= ASSET_BAR_HEIGHT;
    }

    /** Strip under the asset bar: 1–4 tools. */
    public static boolean isInBuildBar(float screenX, float screenY) {
        float w = Gdx.graphics.getWidth();
        return screenX >= 0 && screenX <= w
                && screenY > ASSET_BAR_HEIGHT
                && screenY <= totalTopHeight();
    }

    public static boolean isInTopChrome(float screenX, float screenY) {
        float w = Gdx.graphics.getWidth();
        return screenX >= 0 && screenX <= w && screenY >= 0 && screenY <= totalTopHeight();
    }
}
