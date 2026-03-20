package com.team404.tycoon.desktop;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.model.BuildMode;

public class MapInputProcessor implements InputProcessor {

    private final OrthographicCamera camera;
    private final InputController inputController;
    private final Renderer2D renderer;
    private final Vector3 tmp = new Vector3();

    private int dragButton = -1;
    private int lastDragTileX = Integer.MIN_VALUE;
    private int lastDragTileY = Integer.MIN_VALUE;

    public MapInputProcessor(OrthographicCamera camera, InputController inputController, Renderer2D renderer) {
        this.camera = camera;
        this.inputController = inputController;
        this.renderer = renderer;
    }

    private int[] unprojectToTile(int screenX, int screenY) {
        tmp.set(screenX, screenY, 0f);
        camera.unproject(tmp);
        return Renderer2D.toTile(tmp.x, tmp.y);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        int[] tile = unprojectToTile(screenX, screenY);
        dragButton = button;
        lastDragTileX = tile[0];
        lastDragTileY = tile[1];

        applyAction(tile[0], tile[1], button);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        int[] tile = unprojectToTile(screenX, screenY);
        renderer.setHoverTile(tile[0], tile[1]);

        if (tile[0] != lastDragTileX || tile[1] != lastDragTileY) {
            lastDragTileX = tile[0];
            lastDragTileY = tile[1];
            applyAction(tile[0], tile[1], dragButton);
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        dragButton = -1;
        lastDragTileX = Integer.MIN_VALUE;
        lastDragTileY = Integer.MIN_VALUE;
        return false;
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
        int[] tile = unprojectToTile(screenX, screenY);
        renderer.setHoverTile(tile[0], tile[1]);
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
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
        switch (keycode) {
            case Input.Keys.NUM_1:
                inputController.setCurrentMode(BuildMode.ROAD);
                return true;
            case Input.Keys.NUM_2:
                inputController.setCurrentMode(BuildMode.WATER);
                return true;
            case Input.Keys.NUM_3:
                inputController.setCurrentMode(BuildMode.FOREST);
                return true;
            case Input.Keys.NUM_4:
                inputController.setCurrentMode(BuildMode.DEMOLISH);
                return true;
            case Input.Keys.TAB:
                inputController.cycleModeForward();
                return true;
            default:
                return false;
        }
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
        return false;
    }
}
