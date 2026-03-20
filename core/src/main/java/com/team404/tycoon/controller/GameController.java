package com.team404.tycoon.controller;

import com.team404.tycoon.model.GameState;

/**
 * High-level controller that advances the simulation and exposes read-only access to the model.
 */
public class GameController {

    private final GameState gameState;

    public GameController(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }

    /**
     * Advance the simulation in seconds.
     * For now, there is no time-dependent logic; this will grow with vehicles, economy, etc.
     */
    public void update(float deltaSeconds) {
        // Placeholder for future simulation updates.
    }
}

