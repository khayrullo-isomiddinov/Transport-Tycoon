package com.team404.tycoon.controller;

import com.team404.tycoon.model.EconomyConfig;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.Route;
import com.team404.tycoon.model.Town;
import com.team404.tycoon.model.TransportContentType;
import com.team404.tycoon.model.TransportDemand;
import com.team404.tycoon.model.Vehicle;
import com.team404.tycoon.model.VehicleType;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * High-level controller that advances the simulation and exposes read-only access to the model.
 */
public class GameController {
    private static final float MIN_GREEN_SECONDS = 0.25f;
    private static final float DEFAULT_HORIZONTAL_GREEN_SECONDS = 4.0f;
    private static final float DEFAULT_VERTICAL_GREEN_SECONDS = 4.0f;

    /** Available game speed multipliers: paused, normal, fast, very fast. */
    public static final float[] SPEED_MULTIPLIERS = {0f, 1f, 2f, 4f};

    /** Interval (real seconds) between demand regeneration pulses. */
    private static final float DEMAND_REGEN_INTERVAL = 25f;

    /** Interval (real seconds) between town growth checks. */
    private static final float TOWN_GROWTH_INTERVAL = 45f;

    /** Maximum town population cap. */
    private static final int MAX_TOWN_POPULATION = 50_000;

    private final GameState gameState;
    private final Random rng = new Random();

    private int speedIndex = 1; // default 1×
    private float demandRegenTimer = 0f;
    private float townGrowthTimer = 0f;

    public GameController(GameState gameState) {
        this.gameState = gameState;
    }

    public GameState getGameState() {
        return gameState;
    }

    // ── Speed control ─────────────────────────────────────────────────────────

    public void setSpeedIndex(int idx) {
        this.speedIndex = Math.max(0, Math.min(SPEED_MULTIPLIERS.length - 1, idx));
    }

    public int getSpeedIndex() {
        return speedIndex;
    }

    public float getSpeedMultiplier() {
        return SPEED_MULTIPLIERS[speedIndex];
    }

    // ── Main update ───────────────────────────────────────────────────────────

    /**
     * Advance the simulation by one frame. No-ops when the company is bankrupt so that
     * the loss state is stable and the HUD can display it without further state changes.
     */
    public void update(float deltaSeconds) {
        if (gameState.isBankrupt()) {
            return;
        }

        float scaled = deltaSeconds * getSpeedMultiplier();
        if (scaled > 0f) {
            TransportSimulation.update(gameState, scaled);
        }

        // Demand regeneration uses real time so it happens even when paused is off.
        demandRegenTimer += deltaSeconds;
        if (demandRegenTimer >= DEMAND_REGEN_INTERVAL) {
            demandRegenTimer = 0f;
            regenerateDemand();
        }

        // Town growth uses real time too.
        townGrowthTimer += deltaSeconds;
        if (townGrowthTimer >= TOWN_GROWTH_INTERVAL) {
            townGrowthTimer = 0f;
            growTowns();
        }
    }

    // ── Demand regeneration ────────────────────────────────────────────────────

    /**
     * Generates new transport demand for every town based on its population.
     * Called periodically so demand never permanently runs out.
     */
    private void regenerateDemand() {
        List<Town> towns = gameState.getTowns();
        if (towns.size() < 2) {
            return;
        }
        for (int origin = 0; origin < towns.size(); origin++) {
            Town town = towns.get(origin);
            int pop = town.getPopulation();
            // Each town spawns demand to 1-2 random destinations.
            int numDests = 1 + rng.nextInt(2);
            for (int d = 0; d < numDests; d++) {
                int dest = rng.nextInt(towns.size() - 1);
                if (dest >= origin) {
                    dest++;
                }
                int passengers = Math.max(1, (int) (pop * 0.03f * rng.nextFloat()));
                int goods = Math.max(1, (int) (pop * 0.01f * rng.nextFloat()));
                try {
                    gameState.addTransportDemand(new TransportDemand(
                            origin, dest,
                            TransportContentType.PASSENGERS,
                            passengers,
                            EconomyConfig.PASSENGER_UNIT_REVENUE));
                    gameState.addTransportDemand(new TransportDemand(
                            origin, dest,
                            TransportContentType.GOODS,
                            goods,
                            EconomyConfig.GOODS_UNIT_REVENUE));
                } catch (IllegalArgumentException ignored) {
                    // Should not happen with valid indices, but guard defensively.
                }
            }
        }
    }

    // ── Town growth ────────────────────────────────────────────────────────────

    /**
     * Grows towns that have been served by transport recently.
     * Towns that haven't been visited in a while stagnate.
     */
    private void growTowns() {
        float simTime = gameState.getSimulationTimeSeconds();
        for (Town town : gameState.getTowns()) {
            float timeSinceServiced = simTime - town.getLastServicedTime();
            // Growth window: must have been visited within the last 3 growth intervals.
            if (timeSinceServiced <= TOWN_GROWTH_INTERVAL * 3f) {
                int newPop = Math.min(MAX_TOWN_POPULATION, (int) (town.getPopulation() * 1.03f) + 5);
                town.setPopulation(newPop);
            }
        }
    }

    // ── Traffic light controls ─────────────────────────────────────────────────

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

    // ── Vehicle purchasing ─────────────────────────────────────────────────────

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
