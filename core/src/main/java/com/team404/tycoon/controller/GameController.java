package com.team404.tycoon.controller;

import com.team404.tycoon.model.EconomyConfig;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.Route;
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
     * Advance the simulation by one frame. No-ops when the company is bankrupt so that
     * the loss state is stable and the HUD can display it without further state changes.
     */
    public void update(float deltaSeconds) {
        if (gameState.isBankrupt()) {
            return;
        }
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
        if (gameState.isBankrupt()) {
            return false;
        }
        if (!gameState.isGarageConnectedToRoad(garageTileX, garageTileY)) {
            return false;
        }
        if (gameState.getRoutes().isEmpty()) {
            return false;
        }
        boolean isBus = contentType == TransportContentType.PASSENGERS;
        long purchaseCost = isBus ? EconomyConfig.BUS_PURCHASE_COST : EconomyConfig.TRUCK_PURCHASE_COST;
        if (!gameState.spendMoney(purchaseCost)) {
            return false;
        }
        Route route = gameState.getRoutes().get(0);
        String routeId = route.getId();
        int stopCount = route.getStops().size();
        int startStop = stopCount > 1 ? (int) Math.floorMod(System.nanoTime(), stopCount) : 0;
        VehicleType type = new VehicleType(
                isBus ? "Purchased Bus" : "Purchased Truck",
                isBus ? 16 : 20,
                isBus ? 4.2f : 3.6f,
                Collections.singleton(contentType));
        String vehicleId = "veh-" + System.nanoTime();
        gameState.addVehicle(new Vehicle(vehicleId, routeId, type, startStop));
        return true;
    }

    public boolean sellVehicle(String vehicleId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        boolean removed = gameState.removeVehicleById(vehicleId);
        if (removed) {
            long resale = resaleValueFor(vehicle);
            gameState.addIncome(resale);
        }
        return removed;
    }

    /**
     * Selling removes the oldest vehicle that is due for maintenance.
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

    private Vehicle findVehicleById(String vehicleId) {
        for (Vehicle v : gameState.getVehiclesMutable()) {
            if (v.getId().equals(vehicleId)) {
                return v;
            }
        }
        return null;
    }

    private static long resaleValueFor(Vehicle vehicle) {
        if (vehicle == null) {
            return 0L;
        }
        boolean isBus = vehicle.getType().supports(TransportContentType.PASSENGERS)
                && !vehicle.getType().supports(TransportContentType.GOODS);
        return isBus ? EconomyConfig.BUS_RESALE_VALUE : EconomyConfig.TRUCK_RESALE_VALUE;
    }
}
