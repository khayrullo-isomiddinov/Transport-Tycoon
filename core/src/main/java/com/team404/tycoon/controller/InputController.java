package com.team404.tycoon.controller;

import com.team404.tycoon.model.BuildMode;
import com.team404.tycoon.model.DecorationRules;
import com.team404.tycoon.model.EconomyConfig;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.TransportContentType;
import com.team404.tycoon.model.TileType;

public class InputController {

    private final GameController gameController;
    private final PlacementValidator placementValidator;
    private BuildMode currentMode = BuildMode.ROAD;
    /** When set, LMB places this PNG decoration instead of painting terrain. */
    private String selectedAssetPath;
    private TransportContentType garagePurchaseContentType = TransportContentType.GOODS;

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
        if (state.isBankrupt()) {
            return;
        }
        if (selectedAssetPath == null) {
            if (currentMode != BuildMode.ROAD) {
                applyTerrainTool(state, tileX, tileY);
            } else {
                gameController.purchaseVehicleAtGarage(tileX, tileY, garagePurchaseContentType);
            }
            return;
        }
        GameMap map = state.getMap();
        int[] fp = DecorationRules.footprintForPath(selectedAssetPath);
        PlacedDecoration dec = new PlacedDecoration(
                tileX, tileY, selectedAssetPath, fp[0], fp[1]);
        if (!state.canPlaceDecoration(map, dec)) {
            return;
        }
        if (EconomyConfig.isRoadDecoration(selectedAssetPath)
                && !state.spendMoney(EconomyConfig.ROAD_DECORATION_COST)) {
            return;
        }
        state.addDecoration(dec);
        if (shouldPaintFoundation(selectedAssetPath)) {
            paintFootprint(map, dec, foundationTypeFor(selectedAssetPath));
        }
    }

    public void onSecondaryClick(int tileX, int tileY) {
        if (selectedAssetPath == null) {
            gameController.sellOldestVehicleAtGarage(tileX, tileY);
            return;
        }
        GameState state = gameController.getGameState();
        state.removeDecorationAtTile(tileX, tileY);
    }

    public void setGaragePurchaseContentType(TransportContentType garagePurchaseContentType) {
        this.garagePurchaseContentType = garagePurchaseContentType;
    }

    public TransportContentType getGaragePurchaseContentType() {
        return garagePurchaseContentType;
    }

    public void adjustTrafficLightHorizontalGreen(float deltaSeconds) {
        gameController.adjustTrafficLightHorizontalGreenSeconds(deltaSeconds);
    }

    public void adjustTrafficLightVerticalGreen(float deltaSeconds) {
        gameController.adjustTrafficLightVerticalGreenSeconds(deltaSeconds);
    }

    public void resetTrafficLights() {
        gameController.resetTrafficLightGreenSeconds();
    }

    private void applyTerrainTool(GameState state, int tileX, int tileY) {
        GameMap map = state.getMap();
        if (!map.isInBounds(tileX, tileY)) {
            return;
        }
        if (currentMode == BuildMode.DEMOLISH) {
            state.removeDecorationAtTile(tileX, tileY);
            map.getTile(tileX, tileY).setType(TileType.EMPTY);
        } else {
            map.getTile(tileX, tileY).setType(currentMode.getTargetType());
        }
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
                && !n.contains("-and-")
                && !n.contains("to-")
                && !n.contains("trafficlights")
                && !n.endsWith("/+.png")
                && !n.contains("traffic lights")
                && !n.contains("tree");
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
