package com.team404.tycoon.desktop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.desktop.assets.AssetPaletteState;

/**
 * Handles clicks and wheel on the top asset strip before the map sees input.
 */
public class AssetPaletteInputProcessor implements InputProcessor {

    private static final float PAD_LEFT = 8f;
    private static final float THUMB = 52f;
    private static final float GAP = 6f;
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
        return THUMB + GAP;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!UiChrome.isInAssetBar(screenX, screenY)) {
            return false;
        }
        dragStartX = screenX;
        dragStartScroll = palette.getScrollX();
        float localX = screenX + palette.getScrollX() - PAD_LEFT;
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
        palette.setScrollX(dragStartScroll - dx);
        return UiChrome.isInAssetBar(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float mx = com.badlogic.gdx.Gdx.input.getX();
        float my = com.badlogic.gdx.Gdx.input.getY();
        if (UiChrome.isInAssetBar(mx, my)) {
            // Negate vertical wheel / trackpad delta so the strip moves the intuitive way for a horizontal menu.
            palette.addScroll(-amountY * SCROLL_STEP);
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
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
