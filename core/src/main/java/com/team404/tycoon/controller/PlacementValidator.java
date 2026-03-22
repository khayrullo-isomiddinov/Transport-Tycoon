package com.team404.tycoon.controller;

import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

public class PlacementValidator {

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