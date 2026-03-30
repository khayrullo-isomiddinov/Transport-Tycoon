package com.team404.tycoon.controller;

import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.TransportContentType;
import com.team404.tycoon.model.Vehicle;
import com.team404.tycoon.model.VehicleType;

import java.util.Collections;

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
        TransportSimulation.update(gameState, deltaSeconds);
    }

    public boolean purchaseVehicleAtGarage(int garageTileX, int garageTileY, TransportContentType contentType) {
        if (!gameState.isGarageConnectedToRoad(garageTileX, garageTileY)) {
            return false;
        }
        if (gameState.getRoutes().isEmpty()) {
            return false;
        }
        long purchaseCost = 250L;
        if (!gameState.spendMoney(purchaseCost)) {
            return false;
        }
        String routeId = gameState.getRoutes().get(0).getId();
        VehicleType type = new VehicleType(
                contentType == TransportContentType.PASSENGERS ? "Purchased Bus" : "Purchased Truck",
                contentType == TransportContentType.PASSENGERS ? 16 : 20,
                contentType == TransportContentType.PASSENGERS ? 8.5f : 7.2f,
                Collections.singleton(contentType));
        String vehicleId = "veh-" + System.nanoTime();
        gameState.addVehicle(new Vehicle(vehicleId, routeId, type, 0));
        return true;
    }

    public boolean sellVehicle(String vehicleId) {
        boolean removed = gameState.removeVehicleById(vehicleId);
        if (removed) {
            gameState.addIncome(125L);
        }
        return removed;
    }
}

