package com.team404.tycoon.controller;

import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.EconomyConfig;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

import java.util.Locale;
import java.util.Optional;

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
        return canBuildRoad(map, state, x, y, null);
    }

    public boolean canBuildRoad(GameMap map, GameState state, int x, int y, String candidateRoadResourcePath) {
        Tile tile = map.getTile(x, y);
        if (tile.getType() == TileType.WATER) {
            return false;
        }
        if (tile.getType() == TileType.CITY || tile.getType() == TileType.INDUSTRIAL_FACILITY) {
            return false;
        }
        Optional<PlacedDecoration> existing = state.findDecorationAt(x, y);
        if (existing.isPresent() && !EconomyConfig.isRoadDecoration(existing.get().getResourcePath())) {
            return false;
        }
        if (hasHeightConflictWithAdjacentRoad(map, state, x, y)) {
            return false;
        }
        if (candidateRoadResourcePath != null && hasRoadTypeConflict(map, state, x, y, candidateRoadResourcePath)) {
            return false;
        }
        return true;
    }

    public boolean hasHeightConflictWithAdjacentRoad(GameMap map, GameState state, int x, int y) {
        if (!map.isInBounds(x, y)) {
            return true;
        }
        int tileHeight = map.getTile(x, y).getHeight();
        int[][] dirs = {
                {1, 0},
                {-1, 0},
                {0, 1},
                {0, -1}
        };
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
                    return true;
                }
            }
        }
        return false;
    }

    public boolean hasRoadTypeConflict(GameMap map, GameState state, int x, int y, String candidateRoadResourcePath) {
        if (!map.isInBounds(x, y)) {
            return true;
        }
        int candidateMask = roadConnectionMask(candidateRoadResourcePath);
        if (candidateMask == 0) {
            return false;
        }
        int[][] dirs = {
                {1, 0, SIDE_RIGHT, SIDE_LEFT},
                {-1, 0, SIDE_LEFT, SIDE_RIGHT},
                {0, 1, SIDE_UP, SIDE_DOWN},
                {0, -1, SIDE_DOWN, SIDE_UP}
        };
        for (int[] dir : dirs) {
            int nx = x + dir[0];
            int ny = y + dir[1];
            if (!map.isInBounds(nx, ny)) {
                continue;
            }
            boolean isAdjacentRoad = state.isDriveableRoadTile(nx, ny)
                    || map.getTile(nx, ny).getType() == TileType.ROAD;
            if (isAdjacentRoad) {
                int neighborMask = roadConnectionMaskAt(state, nx, ny);
                if (neighborMask == 0) {
                    continue;
                }
                boolean candidateConnects = hasSide(candidateMask, dir[2]);
                boolean neighborConnects = hasSide(neighborMask, dir[3]);
                // Adjacent road tiles must connect through the touching edge on both sides.
                if (!candidateConnects || !neighborConnects) {
                    return true;
                }
            }
        }
        return false;
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

    /**
     * Returns true if the tile at (tileX, tileY) can have its height raised by 1.
     * Rejected for: out-of-bounds, water, city, industrial, road tiles, and tiles already at max height (3).
     */
    public boolean canRaiseTile(GameMap map, int tileX, int tileY) {
        if (!map.isInBounds(tileX, tileY)) {
            return false;
        }
        Tile tile = map.getTile(tileX, tileY);
        TileType type = tile.getType();
        if (type == TileType.WATER
                || type == TileType.CITY
                || type == TileType.INDUSTRIAL_FACILITY
                || type == TileType.ROAD) {
            return false;
        }
        return tile.getHeight() < 3;
    }

    /**
     * Returns true if the tile at (tileX, tileY) can have its height lowered by 1.
     * Rejected for: out-of-bounds, water, city, industrial, road tiles, and tiles already at min height (1).
     * Height 0 is reserved for water tiles; non-water terrain stays at height 1 or above.
     */
    public boolean canLowerTile(GameMap map, int tileX, int tileY) {
        if (!map.isInBounds(tileX, tileY)) {
            return false;
        }
        Tile tile = map.getTile(tileX, tileY);
        TileType type = tile.getType();
        if (type == TileType.WATER
                || type == TileType.CITY
                || type == TileType.INDUSTRIAL_FACILITY
                || type == TileType.ROAD) {
            return false;
        }
        return tile.getHeight() > 1;
    }

    private static final int SIDE_UP = 1;
    private static final int SIDE_RIGHT = 2;
    private static final int SIDE_DOWN = 4;
    private static final int SIDE_LEFT = 8;

    private static boolean hasSide(int mask, int side) {
        return (mask & side) != 0;
    }

    private static int roadConnectionMaskAt(GameState state, int tileX, int tileY) {
        Optional<PlacedDecoration> dec = state.findDecorationAt(tileX, tileY);
        if (dec.isEmpty()) {
            return 0;
        }
        return roadConnectionMask(dec.get().getResourcePath());
    }

    private static int roadConnectionMask(String resourcePath) {
        if (resourcePath == null) {
            return 0;
        }
        String n = resourcePath.toLowerCase(Locale.ROOT);

        if (n.contains("trafficlights") || n.contains("traffic lights") || n.endsWith("/+.png")) {
            return SIDE_UP | SIDE_RIGHT | SIDE_DOWN | SIDE_LEFT;
        }
        if (n.contains("highway-straight")) {
            return SIDE_LEFT | SIDE_RIGHT;
        }
        if (n.contains("highway-top-left")) {
            return SIDE_UP | SIDE_DOWN;
        }
        if (n.contains("up-and-right")) {
            // Naming follows sprite orientation, not logical grid directions.
            return SIDE_RIGHT | SIDE_DOWN;
        }
        if (n.contains("right-and-down")) {
            return SIDE_DOWN | SIDE_LEFT;
        }
        if (n.contains("down-and-left")) {
            return SIDE_LEFT | SIDE_UP;
        }
        if (n.contains("left-and-up")) {
            return SIDE_UP | SIDE_RIGHT;
        }
        if (n.contains("to-right")) {
            return SIDE_UP | SIDE_DOWN | SIDE_RIGHT;
        }
        if (n.contains("to-left")) {
            return SIDE_UP | SIDE_DOWN | SIDE_LEFT;
        }
        if (n.contains("to-up")) {
            return SIDE_LEFT | SIDE_RIGHT | SIDE_UP;
        }
        if (n.contains("to-down")) {
            return SIDE_LEFT | SIDE_RIGHT | SIDE_DOWN;
        }
        return 0;
    }
}
