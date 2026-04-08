package com.team404.tycoon.controller;

import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

public class PlacementValidator {

    /**
     * Returns false if placing a road at (x, y) would create a connection to an adjacent
     * road tile whose height differs by more than 1.  A height difference of 0 or 1 is
     * always legal; steep drops/rises of 2+ are rejected.
     *
     * <p>Tiles with no adjacent road neighbours pass unconditionally — isolated starts
     * are always allowed regardless of elevation.
     */
    public boolean canBuildRoad(GameMap map, GameState state, int x, int y) {
        if (!map.isInBounds(x, y)) {
            return false;
        }
        int tileHeight = map.getTile(x, y).getHeight();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (!map.isInBounds(nx, ny)) {
                continue;
            }
            boolean isAdjacentRoad = state.isDriveableRoadTile(nx, ny)
                    || map.getTile(nx, ny).getType() == TileType.ROAD;
            if (isAdjacentRoad) {
                int neighborHeight = map.getTile(nx, ny).getHeight();
                if (Math.abs(tileHeight - neighborHeight) > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean canBuild(GameMap map, int tileX, int tileY) {
        if (!map.isInBounds(tileX, tileY)) {
            return false;
        }

        Tile tile = map.getTile(tileX, tileY);
        return tile.getType() == TileType.EMPTY;
    }

    public boolean canDemolish(GameMap map, int tileX, int tileY) {
        if (!map.isInBounds(tileX, tileY)) {
            return false;
        }

        Tile tile = map.getTile(tileX, tileY);
        TileType type = tile.getType();

        if (type == TileType.CITY || type == TileType.INDUSTRIAL_FACILITY) {
            return false;
        }

        return true;
    }
}
