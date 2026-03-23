package com.team404.tycoon.controller;

import com.team404.tycoon.model.BuildMode;
import com.team404.tycoon.model.DecorationRules;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

public class InputController {

    private final GameController gameController;
    private final PlacementValidator placementValidator;
    private BuildMode currentMode = BuildMode.ROAD;
    /** When set, LMB places this PNG decoration instead of painting terrain. */
    private String selectedAssetPath;

    public InputController(GameController gameController) {
        this.gameController = gameController;
        this.placementValidator = new PlacementValidator();
    }

    public void setSelectedAssetPath(String selectedAssetPath) {
        this.selectedAssetPath = selectedAssetPath;
    }

    public String getSelectedAssetPath() {
        return selectedAssetPath;
    }

    public void clearSelectedAssetPath() {
        this.selectedAssetPath = null;
    }

    public BuildMode getCurrentMode() {
        return currentMode;
    }

    public void setCurrentMode(BuildMode mode) {
        this.currentMode = mode;
        clearSelectedAssetPath();
    }

    public void cycleModeForward() {
        BuildMode[] modes = BuildMode.values();
        int next = (currentMode.ordinal() + 1) % modes.length;
        currentMode = modes[next];
        clearSelectedAssetPath();
    }

    public void onPrimaryClick(int tileX, int tileY) {
        GameState state = gameController.getGameState();
        GameMap map = state.getMap();

        if (selectedAssetPath != null) {
            int[] fp = DecorationRules.footprintForPath(selectedAssetPath);
            PlacedDecoration dec = new PlacedDecoration(
                    tileX, tileY, selectedAssetPath, fp[0], fp[1]);
            if (state.canPlaceDecoration(map, dec)) {
                state.addDecoration(dec);
            }
            return;
        }

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
        GameState state = gameController.getGameState();
        GameMap map = state.getMap();

        if (state.removeDecorationAtTile(tileX, tileY)) {
            return;
        }

        if (placementValidator.canDemolish(map, tileX, tileY)) {
            map.getTile(tileX, tileY).setType(TileType.EMPTY);
        }
    }
}