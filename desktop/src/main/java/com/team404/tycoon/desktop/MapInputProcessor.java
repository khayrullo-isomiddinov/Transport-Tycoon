package com.team404.tycoon.desktop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.desktop.assets.AssetPaletteState;

public class MapInputProcessor implements InputProcessor {

    private final OrthographicCamera camera;
    private final InputController inputController;
    private final Renderer2D renderer;
    private final AssetPaletteState assetPaletteState;
    private final Vector3 tmp = new Vector3();

    private int dragButton = -1;
    private int lastDragTileX = Integer.MIN_VALUE;
    private int lastDragTileY = Integer.MIN_VALUE;
    private enum RoadDragMode { NONE, HORIZONTAL, VERTICAL }
    private RoadDragMode roadDragMode = RoadDragMode.NONE;
    private boolean straightRoadDragActive;
    private int straightRoadStartTileX = Integer.MIN_VALUE;
    private int straightRoadStartTileY = Integer.MIN_VALUE;

    public MapInputProcessor(
            OrthographicCamera camera,
            InputController inputController,
            Renderer2D renderer,
            AssetPaletteState assetPaletteState) {
        this.camera = camera;
        this.inputController = inputController;
        this.renderer = renderer;
        this.assetPaletteState = assetPaletteState;
    }

    /**
     * Lwjgl3 / {@link com.badlogic.gdx.InputProcessor} use top-left screen origin (y down).
     * {@link com.badlogic.gdx.graphics.Camera#unproject} already converts with {@code height - y};
     * do not flip y here or picking is wrong (isometric math mixes x and y).
     */
    private int[] unprojectToTile(int screenX, int screenY) {
        tmp.set(screenX, screenY, 0f);
        camera.unproject(tmp);
        return Renderer2D.toTile(tmp.x, tmp.y);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (UiChrome.isInTopChrome(screenX, screenY)) {
            return false;
        }
        int[] tile = unprojectToTile(screenX, screenY);
        dragButton = button;
        lastDragTileX = tile[0];
        lastDragTileY = tile[1];

        if (button == Input.Buttons.LEFT && isLinearRoadSelected()) {
            straightRoadDragActive = true;
            straightRoadStartTileX = tile[0];
            straightRoadStartTileY = tile[1];
            if (isHorizontalRoadSelected()) {
                roadDragMode = RoadDragMode.HORIZONTAL;
                renderer.setStraightRoadPreviewHorizontal(straightRoadStartTileX, straightRoadStartTileY, tile[0]);
            } else {
                roadDragMode = RoadDragMode.VERTICAL;
                renderer.setStraightRoadPreviewVertical(straightRoadStartTileX, straightRoadStartTileY, tile[1]);
            }
            return true;
        }

        applyAction(tile[0], tile[1], button);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (UiChrome.isInTopChrome(screenX, screenY)) {
            return false;
        }
        int[] tile = unprojectToTile(screenX, screenY);
        renderer.setHoverTile(tile[0], tile[1]);

        if (straightRoadDragActive) {
            if (roadDragMode == RoadDragMode.HORIZONTAL) {
                renderer.setStraightRoadPreviewHorizontal(straightRoadStartTileX, straightRoadStartTileY, tile[0]);
            } else if (roadDragMode == RoadDragMode.VERTICAL) {
                renderer.setStraightRoadPreviewVertical(straightRoadStartTileX, straightRoadStartTileY, tile[1]);
            }
            return true;
        }

        if (tile[0] != lastDragTileX || tile[1] != lastDragTileY) {
            lastDragTileX = tile[0];
            lastDragTileY = tile[1];
            applyAction(tile[0], tile[1], dragButton);
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (straightRoadDragActive && button == Input.Buttons.LEFT) {
            int[] tile = unprojectToTile(screenX, screenY);
            if (roadDragMode == RoadDragMode.HORIZONTAL) {
                int minX = Math.min(straightRoadStartTileX, tile[0]);
                int maxX = Math.max(straightRoadStartTileX, tile[0]);
                for (int x = minX; x <= maxX; x++) {
                    inputController.onPrimaryClick(x, straightRoadStartTileY);
                }
            } else if (roadDragMode == RoadDragMode.VERTICAL) {
                int minY = Math.min(straightRoadStartTileY, tile[1]);
                int maxY = Math.max(straightRoadStartTileY, tile[1]);
                for (int y = minY; y <= maxY; y++) {
                    inputController.onPrimaryClick(straightRoadStartTileX, y);
                }
            }
            straightRoadDragActive = false;
            roadDragMode = RoadDragMode.NONE;
            renderer.clearStraightRoadPreview();
            dragButton = -1;
            lastDragTileX = Integer.MIN_VALUE;
            lastDragTileY = Integer.MIN_VALUE;
            return true;
        }
        dragButton = -1;
        lastDragTileX = Integer.MIN_VALUE;
        lastDragTileY = Integer.MIN_VALUE;
        return false;
    }

    private boolean isHorizontalRoadSelected() {
        String selected = inputController.getSelectedAssetPath();
        return selected != null && selected.toLowerCase().contains("highway-straight");
    }

    private boolean isVerticalRoadSelected() {
        String selected = inputController.getSelectedAssetPath();
        return selected != null && selected.toLowerCase().contains("highway-top-left");
    }

    private boolean isLinearRoadSelected() {
        return isHorizontalRoadSelected() || isVerticalRoadSelected();
    }

    private void applyAction(int tileX, int tileY, int button) {
        if (button == Input.Buttons.LEFT) {
            inputController.onPrimaryClick(tileX, tileY);
        } else if (button == Input.Buttons.RIGHT) {
            inputController.onSecondaryClick(tileX, tileY);
        }
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (UiChrome.isInTopChrome(screenX, screenY)) {
            return false;
        }
        int[] tile = unprojectToTile(screenX, screenY);
        renderer.setHoverTile(tile[0], tile[1]);
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float mx = com.badlogic.gdx.Gdx.input.getX();
        float my = com.badlogic.gdx.Gdx.input.getY();
        if (UiChrome.isInTopChrome(mx, my)) {
            return false;
        }
        if (Input.Buttons.LEFT == dragButton || Input.Buttons.RIGHT == dragButton) {
            return false;
        }

        if (com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)
                || com.badlogic.gdx.Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
            renderer.zoom(amountY);
        } else {
            renderer.pan(amountX, amountY);
        }
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
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
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        dragButton = -1;
        straightRoadDragActive = false;
        roadDragMode = RoadDragMode.NONE;
        renderer.clearStraightRoadPreview();
        return false;
    }
}
