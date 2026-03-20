package com.team404.tycoon.model;

/**
 * Aggregate root for the simulation.
 * For now it only owns the map; economy, cities, vehicles etc. will be added gradually.
 */
public class GameState {

    private final GameMap map;

    public GameState(int mapWidth, int mapHeight) {
        this.map = new GameMap(mapWidth, mapHeight);
    }

    public GameMap getMap() {
        return map;
    }
}

