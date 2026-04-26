package com.team404.tycoon.controller;

import com.team404.tycoon.model.BuildMode;
import com.team404.tycoon.model.DecorationRules;
import com.team404.tycoon.model.EconomyConfig;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TransportContentType;
import com.team404.tycoon.model.TileType;

import java.util.Optional;

public class InputController {

    private final GameController gameController;
    private final PlacementValidator placementValidator;
    private BuildMode currentMode = BuildMode.ROAD;
    /** When set, LMB places this PNG decoration instead of painting terrain. */
    private String selectedAssetPath;
    private TransportContentType garagePurchaseContentType = TransportContentType.GOODS;
    /**
     * Set to true when the most recent primary-click road placement was rejected due to
     * a terrain height difference that is too steep.  Cleared at the start of each click.
     * The renderer or HUD can read this to show feedback to the player.
     */
    private boolean lastPlacementRejected;
    private boolean lastRoadTypeRejected;

    /**
     * Set to true when the most recent terraform click was rejected (tile invalid or can't afford).
     * Cleared at the start of each click.
     */
    private boolean lastTerraformRejected;

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

    public boolean isLastPlacementRejected() {
        return lastPlacementRejected;
    }

    public boolean isLastRoadTypeRejected() {
        return lastRoadTypeRejected;
    }

    public boolean isLastTerraformRejected() {
        return lastTerraformRejected;
    }

    public void onPrimaryClick(int tileX, int tileY) {
        lastPlacementRejected = false;
        lastRoadTypeRejected = false;
        lastTerraformRejected = false;
        GameState state = gameController.getGameState();
        if (state.isBankrupt()) {
            return;
        }
        // Functional buildings should keep their click behavior even when a road tool is selected.
        if (selectedAssetPath != null
                && EconomyConfig.isRoadDecoration(selectedAssetPath)
                && state.findGarageAt(tileX, tileY).isPresent()) {
            gameController.purchaseVehicleAtGarage(tileX, tileY, garagePurchaseContentType);
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
        if (EconomyConfig.isRoadDecoration(selectedAssetPath) && isAutoRoadPath(selectedAssetPath)) {
            placeAutoRoad(state, tileX, tileY, selectedAssetPath);
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
                && !placementValidator.canBuildRoad(map, state, tileX, tileY, selectedAssetPath)) {
            if (placementValidator.hasHeightConflictWithAdjacentRoad(map, state, tileX, tileY)) {
                lastPlacementRejected = true;
            } else {
                lastRoadTypeRejected = true;
            }
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

    private void placeAutoRoad(GameState state, int tileX, int tileY, String selectedRoadPath) {
        GameMap map = state.getMap();
        if (!placementValidator.canBuildRoad(map, state, tileX, tileY, null)) {
            if (placementValidator.hasHeightConflictWithAdjacentRoad(map, state, tileX, tileY)) {
                lastPlacementRejected = true;
            } else {
                lastRoadTypeRejected = true;
            }
            return;
        }

        if (!state.isDriveableRoadTile(tileX, tileY)) {
            if (!state.spendMoney(EconomyConfig.ROAD_DECORATION_COST)) {
                return;
            }
        }

        map.getTile(tileX, tileY).setType(TileType.ROAD);
        applyRoadDecorationAt(state, tileX, tileY, selectedRoadPath);

        // Recompute neighbors so turns / T-junctions / intersections update automatically.
        applyRoadDecorationAt(state, tileX + 1, tileY, selectedRoadPath);
        applyRoadDecorationAt(state, tileX - 1, tileY, selectedRoadPath);
        applyRoadDecorationAt(state, tileX, tileY + 1, selectedRoadPath);
        applyRoadDecorationAt(state, tileX, tileY - 1, selectedRoadPath);
    }

    private void applyRoadDecorationAt(GameState state, int tileX, int tileY, String selectedRoadPath) {
        GameMap map = state.getMap();
        if (!map.isInBounds(tileX, tileY)) {
            return;
        }
        if (!state.isDriveableRoadTile(tileX, tileY) && map.getTile(tileX, tileY).getType() != TileType.ROAD) {
            return;
        }
        String roadPath = chooseRoadPathForTile(state, tileX, tileY, selectedRoadPath);
        if (roadPath == null) {
            return;
        }
        Optional<PlacedDecoration> existing = state.findDecorationAt(tileX, tileY);
        if (existing.isPresent() && EconomyConfig.isRoadDecoration(existing.get().getResourcePath())) {
            if (existing.get().getResourcePath().equals(roadPath)) {
                map.getTile(tileX, tileY).setType(TileType.ROAD);
                return;
            }
            state.removeDecorationAtTile(tileX, tileY);
        }
        PlacedDecoration replacement = new PlacedDecoration(tileX, tileY, roadPath, 1, 1);
        if (state.canPlaceDecoration(map, replacement)) {
            state.addDecoration(replacement);
        }
        map.getTile(tileX, tileY).setType(TileType.ROAD);
    }

    private String chooseRoadPathForTile(GameState state, int tileX, int tileY, String selectedRoadPath) {
        boolean up = state.isDriveableRoadTile(tileX, tileY + 1) || isRoadType(state, tileX, tileY + 1);
        boolean down = state.isDriveableRoadTile(tileX, tileY - 1) || isRoadType(state, tileX, tileY - 1);
        boolean left = state.isDriveableRoadTile(tileX - 1, tileY) || isRoadType(state, tileX - 1, tileY);
        boolean right = state.isDriveableRoadTile(tileX + 1, tileY) || isRoadType(state, tileX + 1, tileY);
        int neighbors = (up ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0) + (right ? 1 : 0);

        if (neighbors >= 4) {
            return "resources/+.png";
        }
        if (neighbors == 3 && up && down && right) {
            return "resources/to-right.png";
        }
        if (neighbors == 3 && up && down && left) {
            return "resources/to-left.png";
        }
        if (neighbors == 3 && left && right && up) {
            return "resources/to-up.png";
        }
        if (neighbors == 3 && left && right && down) {
            return "resources/to-down.png";
        }
        if (right && down) {
            return "resources/up-and-right.png";
        }
        if (down && left) {
            return "resources/right-and-down.png";
        }
        if (left && up) {
            return "resources/down-and-left.png";
        }
        if (up && right) {
            return "resources/left-and-up.png";
        }
        if (left || right) {
            return "resources/highway-straight.png";
        }
        if (up || down) {
            return "resources/highway-top-left.png";
        }
        // Isolated first piece: respect selected straight orientation.
        if (selectedRoadPath != null && selectedRoadPath.toLowerCase().contains("highway-top-left")) {
            return "resources/highway-top-left.png";
        }
        return "resources/highway-straight.png";
    }

    private boolean isRoadType(GameState state, int tileX, int tileY) {
        if (!state.getMap().isInBounds(tileX, tileY)) {
            return false;
        }
        return state.getMap().getTile(tileX, tileY).getType() == TileType.ROAD;
    }

    private static boolean isAutoRoadPath(String resourcePath) {
        if (resourcePath == null) {
            return false;
        }
        String n = resourcePath.toLowerCase();
        return n.contains("highway-straight") || n.contains("highway-top-left");
    }

    public void onSecondaryClick(int tileX, int tileY) {
        GameState state = gameController.getGameState();
        if (selectedAssetPath != null
                && EconomyConfig.isRoadDecoration(selectedAssetPath)
                && state.findGarageAt(tileX, tileY).isPresent()) {
            gameController.sellOldestVehicleAtGarage(tileX, tileY);
            return;
        }
        if (selectedAssetPath == null) {
            gameController.sellOldestVehicleAtGarage(tileX, tileY);
            return;
        }
        state.removeDecorationAtTile(tileX, tileY);
    }

    public void setGaragePurchaseContentType(TransportContentType garagePurchaseContentType) {
        this.garagePurchaseContentType = garagePurchaseContentType;
    }

    public TransportContentType getGaragePurchaseContentType() {
        return garagePurchaseContentType;
    }

    private boolean menuRequested = false;

    public void requestMenu() {
        menuRequested = true;
    }

    public boolean isMenuRequested() {
        return menuRequested;
    }

    public void clearMenuRequest() {
        menuRequested = false;
    }

    public void setGameSpeedIndex(int idx) {
        gameController.setSpeedIndex(idx);
    }

    public int getGameSpeedIndex() {
        return gameController.getSpeedIndex();
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
        if (currentMode == BuildMode.RAISE_TERRAIN) {
            if (!placementValidator.canRaiseTile(map, tileX, tileY)) {
                lastTerraformRejected = true;
                return;
            }
            if (!state.spendMoney(EconomyConfig.TERRAFORM_COST)) {
                lastTerraformRejected = true;
                return;
            }
            Tile tile = map.getTile(tileX, tileY);
            tile.setHeight(tile.getHeight() + 1);
        } else if (currentMode == BuildMode.LOWER_TERRAIN) {
            if (!placementValidator.canLowerTile(map, tileX, tileY)) {
                lastTerraformRejected = true;
                return;
            }
            if (!state.spendMoney(EconomyConfig.TERRAFORM_COST)) {
                lastTerraformRejected = true;
                return;
            }
            Tile tile = map.getTile(tileX, tileY);
            tile.setHeight(tile.getHeight() - 1);
        } else if (currentMode == BuildMode.DEMOLISH) {
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
