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

    public static final float ASSET_BAR_HEIGHT = 96f;
    public static final float BUILD_BAR_HEIGHT = 50f;
    public static final float ASSET_PAD = 10f;
    public static final float THUMB = 68f;
    public static final float ASSET_GAP = 8f;
    public static final float ARROW_SIZE = 30f;
    public static final float ARROW_GAP = 8f;

    /** Category dropdown button at the far left of the asset bar. */
    public static final float DROPDOWN_BTN_W  = 82f;
    public static final float DROPDOWN_BTN_H  = 38f;
    public static final float DROPDOWN_ITEM_H = 30f;
    public static final float DROPDOWN_GAP    = 8f;

    /** Terrain tool buttons (Water / Forest / Demolish / Raise / Lower) at the left of the asset bar, after the dropdown. */
    public static final float TOOL_BTN_SIZE = 44f;
    public static final float TOOL_BTN_GAP = 5f;
    public static final int TOOL_BTN_COUNT = 5;

    /** True if the screen position is over the category dropdown button. GLFW coords (y down). */
    public static boolean isInDropdownButton(float screenX, float screenY) {
        float top    = (ASSET_BAR_HEIGHT - DROPDOWN_BTN_H) / 2f;
        float bottom = top + DROPDOWN_BTN_H;
        return screenX >= ASSET_PAD && screenX <= ASSET_PAD + DROPDOWN_BTN_W
                && screenY >= top && screenY <= bottom;
    }

    /**
     * Returns which dropdown item (0 = first) the cursor is over when the panel is open,
     * or -1 if not over the panel. GLFW coords (y down).
     */
    public static int dropdownItemIndexAt(float screenX, float screenY, int itemCount) {
        if (screenX < ASSET_PAD || screenX > ASSET_PAD + DROPDOWN_BTN_W) {
            return -1;
        }
        float top    = ASSET_BAR_HEIGHT;
        float bottom = top + itemCount * DROPDOWN_ITEM_H;
        if (screenY < top || screenY > bottom) {
            return -1;
        }
        return (int) ((screenY - top) / DROPDOWN_ITEM_H);
    }

    private UiChrome() {
    }

    /** Screen-X where the terrain tool buttons begin (after the dropdown). */
    public static float toolButtonsStartX() {
        return ASSET_PAD + DROPDOWN_BTN_W + DROPDOWN_GAP;
    }

    /** Total pixel width used by the terrain tool buttons. */
    public static float toolAreaWidth() {
        return TOOL_BTN_COUNT * TOOL_BTN_SIZE + (TOOL_BTN_COUNT - 1) * TOOL_BTN_GAP;
    }

    /** Screen-X of the left scroll arrow (after dropdown + tool buttons). */
    public static float leftArrowX() {
        return toolButtonsStartX() + toolAreaWidth() + TOOL_BTN_GAP;
    }

    /**
     * Returns the index (0-based) of the tool button at the given screen position,
     * or -1 if the position is not over any tool button.
     */
    public static int toolButtonIndexAt(float screenX, float screenY) {
        if (!isInAssetBar(screenX, screenY)) {
            return -1;
        }
        float topPad = (ASSET_BAR_HEIGHT - TOOL_BTN_SIZE) / 2f;
        if (screenY < topPad || screenY > topPad + TOOL_BTN_SIZE) {
            return -1;
        }
        float localX = screenX - toolButtonsStartX();
        int idx = (int) (localX / (TOOL_BTN_SIZE + TOOL_BTN_GAP));
        float xInCell = localX - idx * (TOOL_BTN_SIZE + TOOL_BTN_GAP);
        if (idx < 0 || idx >= TOOL_BTN_COUNT || xInCell > TOOL_BTN_SIZE) {
            return -1;
        }
        return idx;
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
        return leftArrowX() + ARROW_SIZE + ARROW_GAP;
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
        float lx = leftArrowX();
        return screenX >= lx
                && screenX <= lx + ARROW_SIZE
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
