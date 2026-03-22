package com.team404.tycoon.controller;

import com.team404.tycoon.model.BuildMode;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

public class InputController {

    private final GameController gameController;
    private final PlacementValidator placementValidator;
    private BuildMode currentMode = BuildMode.ROAD;

    public InputController(GameController gameController) {
        this.gameController = gameController;
        this.placementValidator = new PlacementValidator();
    }

    public BuildMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(BuildMode mode) {
        this.currentMode = mode;
    }

    public void cycleModeForward() {
        BuildMode[] modes = BuildMode.values();
        int next = (currentMode.ordinal() + 1) % modes.length;
        currentMode = modes[next];
    }

    public void onPrimaryClick(int tileX, int tileY) {
        GameMap map = gameController.getGameState().getMap();

        if (currentMode == BuildMode.DEMOLISH) {
            if (placementValidator.canDemolish(map, tileX, tileY)) {
                map.getTile(tileX, tileY).setType(TileType.EMPTY);
            }
            return;
        }

        if (placementValidator.canBuild(map, tileX, tileY)) {
            Tile tile = map.getTile(tileX, tileY);
            tile.setType(currentMode.getTargetType());
        }
    }

    public void onSecondaryClick(int tileX, int tileY) {
        GameMap map = gameController.getGameState().getMap();

        if (placementValidator.canDemolish(map, tileX, tileY)) {
            map.getTile(tileX, tileY).setType(TileType.EMPTY);
        }
    }
}