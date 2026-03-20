package com.team404.tycoon.controller;

import com.team404.tycoon.model.BuildMode;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

public class InputController {

    private final GameController gameController;
    private BuildMode currentMode = BuildMode.ROAD;

    public InputController(GameController gameController) {
        this.gameController = gameController;
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
        Tile tile = getTileIfInBounds(tileX, tileY);
        if (tile == null) {
            return;
        }

        TileType target = currentMode.getTargetType();

        if (currentMode == BuildMode.DEMOLISH) {
            if (tile.getType() != TileType.CITY && tile.getType() != TileType.INDUSTRIAL_FACILITY) {
                tile.setType(TileType.EMPTY);
            }
        } else if (tile.getType() == TileType.EMPTY) {
            tile.setType(target);
        }
    }

    public void onSecondaryClick(int tileX, int tileY) {
        Tile tile = getTileIfInBounds(tileX, tileY);
        if (tile == null) {
            return;
        }
        if (tile.getType() != TileType.CITY && tile.getType() != TileType.INDUSTRIAL_FACILITY) {
            tile.setType(TileType.EMPTY);
        }
    }

    private Tile getTileIfInBounds(int tileX, int tileY) {
        GameMap map = gameController.getGameState().getMap();
        if (!map.isInBounds(tileX, tileY)) {
            return null;
        }
        return map.getTile(tileX, tileY);
    }
}
