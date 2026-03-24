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
        if (selectedAssetPath == null) {
            return;
        }
        GameState state = gameController.getGameState();
        GameMap map = state.getMap();

        int[] fp = DecorationRules.footprintForPath(selectedAssetPath);
        PlacedDecoration dec = new PlacedDecoration(
                tileX, tileY, selectedAssetPath, fp[0], fp[1]);
        if (state.canPlaceDecoration(map, dec)) {
            state.addDecoration(dec);
            if (shouldPaintFoundation(selectedAssetPath)) {
                paintFootprint(map, dec, foundationTypeFor(selectedAssetPath));
            }
        }
    }

    public void onSecondaryClick(int tileX, int tileY) {
        if (selectedAssetPath == null) {
            return;
        }
        GameState state = gameController.getGameState();
        state.removeDecorationAtTile(tileX, tileY);
    }

    private static void paintFootprint(GameMap map, PlacedDecoration decoration, TileType type) {
        for (int dy = 0; dy < decoration.getFootprintTilesH(); dy++) {
            for (int dx = 0; dx < decoration.getFootprintTilesW(); dx++) {
                int tx = decoration.getAnchorTileX() + dx;
                int ty = decoration.getAnchorTileY() + dy;
                if (map.isInBounds(tx, ty)) {
                    map.getTile(tx, ty).setType(type);
                }
            }
        }
    }

    private static boolean shouldPaintFoundation(String resourcePath) {
        if (resourcePath == null) {
            return true;
        }
        String n = resourcePath.toLowerCase();
        return !n.contains("highway-straight")
                && !n.contains("highway-top-left")
                && !n.contains("intersection")
                && !n.contains("trafficlights")
                && !n.contains("traffic lights");
    }

    private static TileType foundationTypeFor(String resourcePath) {
        if (resourcePath == null) {
            return TileType.CITY;
        }
        String n = resourcePath.toLowerCase();
        // Building-like assets look better on darker asphalt-style pads.
        if (n.contains("building")
                || n.contains("garage")
                || n.contains("village")
                || n.contains("teplitsa")
                || n.contains("playground")
                || n.contains("factory")) {
            return TileType.ROAD;
        }
        return TileType.CITY;
    }
}