package com.team404.tycoon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Aggregate root for the simulation.
 * For now it only owns the map; economy, cities, vehicles etc. will be added gradually.
 */
public class GameState {

    private final GameMap map;
    private final List<PlacedDecoration> decorations = new ArrayList<>();

    public GameState(int mapWidth, int mapHeight) {
        this.map = new GameMap(mapWidth, mapHeight);
    }

    public GameMap getMap() {
        return map;
    }

    public List<PlacedDecoration> getDecorations() {
        return Collections.unmodifiableList(decorations);
    }

    public void addDecoration(PlacedDecoration decoration) {
        decorations.add(decoration);
    }

    /**
     * Removes the top-most decoration whose footprint contains the given tile (last placed wins).
     */
    public boolean removeDecorationAtTile(int tileX, int tileY) {
        for (int i = decorations.size() - 1; i >= 0; i--) {
            if (decorations.get(i).occupiesTile(tileX, tileY)) {
                decorations.remove(i);
                return true;
            }
        }
        return false;
    }

    public Optional<PlacedDecoration> findDecorationAt(int tileX, int tileY) {
        for (int i = decorations.size() - 1; i >= 0; i--) {
            PlacedDecoration d = decorations.get(i);
            if (d.occupiesTile(tileX, tileY)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    /**
     * True if every footprint tile is in bounds and does not overlap another decoration.
     * Terrain type (roads, water, forest, etc.) does not block decals.
     */
    public boolean canPlaceDecoration(GameMap map, PlacedDecoration candidate) {
        for (int dy = 0; dy < candidate.getFootprintTilesH(); dy++) {
            for (int dx = 0; dx < candidate.getFootprintTilesW(); dx++) {
                int tx = candidate.getAnchorTileX() + dx;
                int ty = candidate.getAnchorTileY() + dy;
                if (!map.isInBounds(tx, ty)) {
                    return false;
                }
            }
        }
        for (PlacedDecoration existing : decorations) {
            if (footprintsOverlap(existing, candidate)) {
                return false;
            }
        }
        return true;
    }

    private static boolean footprintsOverlap(PlacedDecoration a, PlacedDecoration b) {
        int ax1 = a.getAnchorTileX();
        int ay1 = a.getAnchorTileY();
        int ax2 = ax1 + a.getFootprintTilesW();
        int ay2 = ay1 + a.getFootprintTilesH();
        int bx1 = b.getAnchorTileX();
        int by1 = b.getAnchorTileY();
        int bx2 = bx1 + b.getFootprintTilesW();
        int by2 = by1 + b.getFootprintTilesH();
        return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1;
    }
}

