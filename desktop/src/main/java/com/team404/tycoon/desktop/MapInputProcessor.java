package com.team404.tycoon.desktop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.desktop.assets.AssetPaletteState;
import com.team404.tycoon.model.TransportContentType;

public class MapInputProcessor implements InputProcessor {

    private final OrthographicCamera camera;
    private final InputController inputController;
    private final Renderer2D renderer;
    private final AssetPaletteState assetPaletteState;
    private final Vector3 tmp = new Vector3();

    private int dragButton = -1;
    private int lastDragTileX = Integer.MIN_VALUE;
    private int lastDragTileY = Integer.MIN_VALUE;
    private Renderer2D.BuildPreviewMode buildDragMode = null;
    private int buildDragStartTileX = Integer.MIN_VALUE;
    private int buildDragStartTileY = Integer.MIN_VALUE;

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

        if (button == Input.Buttons.LEFT && inputController.getSelectedAssetPath() != null) {
            buildDragStartTileX = tile[0];
            buildDragStartTileY = tile[1];
            buildDragMode = pickBuildPreviewMode();
            int endX = (buildDragMode == Renderer2D.BuildPreviewMode.VERTICAL_LINE) ? buildDragStartTileX : tile[0];
            int endY = (buildDragMode == Renderer2D.BuildPreviewMode.HORIZONTAL_LINE) ? buildDragStartTileY : tile[1];
            renderer.setBuildPreview(buildDragStartTileX, buildDragStartTileY, endX, endY, buildDragMode);
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

        if (buildDragMode != null) {
            int endX = (buildDragMode == Renderer2D.BuildPreviewMode.VERTICAL_LINE) ? buildDragStartTileX : tile[0];
            int endY = (buildDragMode == Renderer2D.BuildPreviewMode.HORIZONTAL_LINE) ? buildDragStartTileY : tile[1];
            renderer.setBuildPreview(buildDragStartTileX, buildDragStartTileY, endX, endY, buildDragMode);
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
        if (buildDragMode != null && button == Input.Buttons.LEFT) {
            int[] tile = unprojectToTile(screenX, screenY);
            if (buildDragMode == Renderer2D.BuildPreviewMode.HORIZONTAL_LINE) {
                int minX = Math.min(buildDragStartTileX, tile[0]);
                int maxX = Math.max(buildDragStartTileX, tile[0]);
                for (int x = minX; x <= maxX; x++) {
                    inputController.onPrimaryClick(x, buildDragStartTileY);
                }
            } else if (buildDragMode == Renderer2D.BuildPreviewMode.VERTICAL_LINE) {
                int minY = Math.min(buildDragStartTileY, tile[1]);
                int maxY = Math.max(buildDragStartTileY, tile[1]);
                for (int y = minY; y <= maxY; y++) {
                    inputController.onPrimaryClick(buildDragStartTileX, y);
                }
            } else {
                int minX = Math.min(buildDragStartTileX, tile[0]);
                int maxX = Math.max(buildDragStartTileX, tile[0]);
                int minY = Math.min(buildDragStartTileY, tile[1]);
                int maxY = Math.max(buildDragStartTileY, tile[1]);
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        inputController.onPrimaryClick(x, y);
                    }
                }
            }
            buildDragMode = null;
            renderer.clearBuildPreview();
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

    private Renderer2D.BuildPreviewMode pickBuildPreviewMode() {
        if (isHorizontalRoadSelected()) {
            return Renderer2D.BuildPreviewMode.HORIZONTAL_LINE;
        }
        if (isVerticalRoadSelected()) {
            return Renderer2D.BuildPreviewMode.VERTICAL_LINE;
        }
        return Renderer2D.BuildPreviewMode.AREA;
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
        if (keycode == Input.Keys.NUM_1) {
            inputController.setGaragePurchaseContentType(TransportContentType.PASSENGERS);
            return true;
        }
        if (keycode == Input.Keys.NUM_2) {
            inputController.setGaragePurchaseContentType(TransportContentType.GOODS);
            return true;
        }
        if (keycode == Input.Keys.Z) {
            inputController.adjustTrafficLightHorizontalGreen(-0.5f);
            return true;
        }
        if (keycode == Input.Keys.X) {
            inputController.adjustTrafficLightHorizontalGreen(0.5f);
            return true;
        }
        if (keycode == Input.Keys.C) {
            inputController.adjustTrafficLightVerticalGreen(-0.5f);
            return true;
        }
        if (keycode == Input.Keys.V) {
            inputController.adjustTrafficLightVerticalGreen(0.5f);
            return true;
        }
        if (keycode == Input.Keys.R) {
            inputController.resetTrafficLights();
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
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        dragButton = -1;
        buildDragMode = null;
        renderer.clearBuildPreview();
        return false;
    }
}
