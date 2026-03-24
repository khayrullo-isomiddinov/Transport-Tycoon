package com.team404.tycoon.desktop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.desktop.assets.AssetPaletteState;

/**
 * Handles clicks and wheel on the top asset strip before the map sees input.
 */
public class AssetPaletteInputProcessor implements InputProcessor {

    private static final float SCROLL_STEP = 48f;

    private final AssetPaletteState palette;
    private final InputController inputController;
    private float dragStartX;
    private float dragStartScroll;

    public AssetPaletteInputProcessor(AssetPaletteState palette, InputController inputController) {
        this.palette = palette;
        this.inputController = inputController;
    }

    private float cellStep() {
        return UiChrome.THUMB + UiChrome.ASSET_GAP;
    }

    private float maxScrollX() {
        float visible = UiChrome.assetContentWidth(com.badlogic.gdx.Gdx.graphics.getWidth());
        return palette.maxScrollX(visible, UiChrome.THUMB, UiChrome.ASSET_GAP);
    }

    private void updateHover(int screenX, int screenY) {
        if (!UiChrome.isInAssetBar(screenX, screenY)
                || UiChrome.isInAssetLeftArrow(screenX, screenY)
                || UiChrome.isInAssetRightArrow(screenX, screenY)) {
            palette.setHoveredPath(null);
            return;
        }
        float localX = screenX + palette.getScrollX() - UiChrome.assetContentLeftX();
        int idx = (int) Math.floor(localX / cellStep());
        if (idx >= 0 && idx < palette.getResourcePaths().size()) {
            palette.setHoveredPath(palette.getResourcePaths().get(idx));
        } else {
            palette.setHoveredPath(null);
        }
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!UiChrome.isInAssetBar(screenX, screenY)) {
            return false;
        }
        if (UiChrome.isInAssetLeftArrow(screenX, screenY)) {
            palette.addScroll(-SCROLL_STEP * 2f, maxScrollX());
            palette.setHoveredPath(null);
            return true;
        }
        if (UiChrome.isInAssetRightArrow(screenX, screenY)) {
            palette.addScroll(SCROLL_STEP * 2f, maxScrollX());
            palette.setHoveredPath(null);
            return true;
        }
        dragStartX = screenX;
        dragStartScroll = palette.getScrollX();
        updateHover(screenX, screenY);
        float localX = screenX + palette.getScrollX() - UiChrome.assetContentLeftX();
        int idx = (int) Math.floor(localX / cellStep());
        if (idx >= 0 && idx < palette.getResourcePaths().size()) {
            String path = palette.getResourcePaths().get(idx);
            palette.setSelectedPath(path);
            inputController.setSelectedAssetPath(path);
            return true;
        }
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!UiChrome.isInTopChrome(screenX, screenY)) {
            return false;
        }
        float dx = screenX - dragStartX;
        palette.setScrollX(dragStartScroll - dx, maxScrollX());
        updateHover(screenX, screenY);
        return UiChrome.isInAssetBar(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float mx = com.badlogic.gdx.Gdx.input.getX();
        float my = com.badlogic.gdx.Gdx.input.getY();
        if (UiChrome.isInAssetBar(mx, my)) {
            // Negate vertical wheel / trackpad delta so the strip moves the intuitive way for a horizontal menu.
            palette.addScroll(-amountY * SCROLL_STEP, maxScrollX());
            return true;
        }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            palette.clearSelection();
            inputController.clearSelectedAssetPath();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        updateHover(screenX, screenY);
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        updateHover(screenX, screenY);
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        palette.setHoveredPath(null);
        return false;
    }
}
