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
    private static final float MIN_GREEN_SECONDS = 0.25f;
    private static final float DEFAULT_HORIZONTAL_GREEN_SECONDS = 4.0f;
    private static final float DEFAULT_VERTICAL_GREEN_SECONDS = 4.0f;

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

    public void adjustTrafficLightHorizontalGreenSeconds(float deltaSeconds) {
        float current = gameState.getTrafficLightHorizontalGreenSeconds();
        float updated = Math.max(MIN_GREEN_SECONDS, current + deltaSeconds);
        gameState.setTrafficLightDurations(updated, gameState.getTrafficLightVerticalGreenSeconds());
    }

    public void adjustTrafficLightVerticalGreenSeconds(float deltaSeconds) {
        float current = gameState.getTrafficLightVerticalGreenSeconds();
        float updated = Math.max(MIN_GREEN_SECONDS, current + deltaSeconds);
        gameState.setTrafficLightDurations(gameState.getTrafficLightHorizontalGreenSeconds(), updated);
    }

    public void resetTrafficLightGreenSeconds() {
        gameState.setTrafficLightDurations(
                DEFAULT_HORIZONTAL_GREEN_SECONDS,
                DEFAULT_VERTICAL_GREEN_SECONDS);
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
                contentType == TransportContentType.PASSENGERS ? 4.2f : 3.6f,
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

    /**
     * Simplified gameplay API: selling removes the oldest vehicle that is due for maintenance
     * (based on the same interval as the maintenance system).
     */
    public boolean sellOldestVehicleAtGarage(int garageTileX, int garageTileY) {
        if (!gameState.isGarageConnectedToRoad(garageTileX, garageTileY)) {
            return false;
        }
        Vehicle oldest = null;
        float oldestAge = -1f;
        for (Vehicle v : gameState.getVehiclesMutable()) {
            if (v.getAgeSeconds() > oldestAge) {
                oldestAge = v.getAgeSeconds();
                oldest = v;
            }
        }
        if (oldest == null) {
            return false;
        }
        if (oldest.getAgeSeconds() < TransportSimulation.MAINTENANCE_INTERVAL_SECONDS) {
            return false;
        }
        return sellVehicle(oldest.getId());
    }
}

