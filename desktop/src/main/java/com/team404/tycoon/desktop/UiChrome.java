package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;

/**
 * Screen-space layout for pointer hit tests on the top HUD.
 * <p>
 * Lwjgl3 reports mouse {@code screenY} in the same convention as {@link Gdx#input} (GLFW):
 * origin at the <strong>top-left</strong> of the window client area, y increases downward.
 * This matches {@link com.badlogic.gdx.InputProcessor} coordinates on desktop.
 * <p>
 * Order from the top of the window down: asset strip (PNGs) only.
 */
public final class UiChrome {

    public static final float ASSET_BAR_HEIGHT = 76f;
    public static final float BUILD_BAR_HEIGHT = 0f;
    public static final float ASSET_PAD = 8f;
    public static final float THUMB = 52f;
    public static final float ASSET_GAP = 6f;
    public static final float ARROW_SIZE = 26f;
    public static final float ARROW_GAP = 8f;

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

    /** Legacy strip under the asset bar (disabled in sprite-only mode). */
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

    public static float assetContentLeftX() {
        return ASSET_PAD + ARROW_SIZE + ARROW_GAP;
    }

    public static float assetContentRightX(float screenWidth) {
        return screenWidth - ASSET_PAD - ARROW_SIZE - ARROW_GAP;
    }

    public static float assetContentWidth(float screenWidth) {
        return Math.max(0f, assetContentRightX(screenWidth) - assetContentLeftX());
    }

    public static boolean isInAssetLeftArrow(float screenX, float screenY) {
        if (!isInAssetBar(screenX, screenY)) {
            return false;
        }
        float top = (ASSET_BAR_HEIGHT - ARROW_SIZE) * 0.5f;
        float bottom = top + ARROW_SIZE;
        return screenX >= ASSET_PAD
                && screenX <= ASSET_PAD + ARROW_SIZE
                && screenY >= top
                && screenY <= bottom;
    }

    public static boolean isInAssetRightArrow(float screenX, float screenY) {
        if (!isInAssetBar(screenX, screenY)) {
            return false;
        }
        float w = Gdx.graphics.getWidth();
        float left = w - ASSET_PAD - ARROW_SIZE;
        float top = (ASSET_BAR_HEIGHT - ARROW_SIZE) * 0.5f;
        float bottom = top + ARROW_SIZE;
        return screenX >= left
                && screenX <= left + ARROW_SIZE
                && screenY >= top
                && screenY <= bottom;
    }
}
