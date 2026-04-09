package com.team404.tycoon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Runtime vehicle assigned to one route.
 */
public final class Vehicle {
    private final String id;
    private final String routeId;
    private final VehicleType type;
    private final List<Shipment> loadedShipments = new ArrayList<>();
    private int currentStopIndex;
    private float legProgressTiles;
    private float ageSeconds;
    private float lastMaintenanceAgeSeconds;

    public Vehicle(String id, String routeId, VehicleType type, int currentStopIndex) {
        this.id = Objects.requireNonNull(id, "id");
        this.routeId = Objects.requireNonNull(routeId, "routeId");
        this.type = Objects.requireNonNull(type, "type");
        if (currentStopIndex < 0) {
            throw new IllegalArgumentException("currentStopIndex must be >= 0");
        }
        this.currentStopIndex = currentStopIndex;
        this.legProgressTiles = 0f;
        this.ageSeconds = 0f;
        this.lastMaintenanceAgeSeconds = 0f;
    }

    public String getId() {
        return id;
    }

    public String getRouteId() {
        return routeId;
    }

    public VehicleType getType() {
        return type;
    }

    public int getCurrentStopIndex() {
        return currentStopIndex;
    }

    public void setCurrentStopIndex(int currentStopIndex) {
        if (currentStopIndex < 0) {
            throw new IllegalArgumentException("currentStopIndex must be >= 0");
        }
        this.currentStopIndex = currentStopIndex;
    }

    public float getLegProgressTiles() {
        return legProgressTiles;
    }

    public void setLegProgressTiles(float legProgressTiles) {
        if (legProgressTiles < 0f) {
            throw new IllegalArgumentException("legProgressTiles must be >= 0");
        }
        this.legProgressTiles = legProgressTiles;
    }

    public List<Shipment> getLoadedShipments() {
        return Collections.unmodifiableList(loadedShipments);
    }

    public void clearLoadedShipments() {
        loadedShipments.clear();
    }

    public void addShipment(Shipment shipment) {
        loadedShipments.add(Objects.requireNonNull(shipment, "shipment"));
    }

    public int getUsedCapacity() {
        int sum = 0;
        for (Shipment shipment : loadedShipments) {
            sum += shipment.getQuantity();
        }
        return sum;
    }

    public int getFreeCapacity() {
        return Math.max(0, type.getCapacity() - getUsedCapacity());
    }

    public float getAgeSeconds() {
        return ageSeconds;
    }

    public void addAgeSeconds(float deltaSeconds) {
        if (deltaSeconds > 0f) {
            this.ageSeconds += deltaSeconds;
        }
    }

    /**
     * Returns the current service interval in simulation seconds, which decreases
     * linearly as the vehicle ages.  A fresh vehicle is serviced every
     * {@link EconomyConfig#MAINTENANCE_BASE_INTERVAL_SECONDS}; once old enough the
     * interval floors at {@link EconomyConfig#MAINTENANCE_MIN_INTERVAL_SECONDS}.
     */
    public float getMaintenanceIntervalSeconds() {
        float reduced = EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS
                - ageSeconds * EconomyConfig.MAINTENANCE_AGE_REDUCTION_RATE;
        return Math.max(EconomyConfig.MAINTENANCE_MIN_INTERVAL_SECONDS, reduced);
    }

    /**
     * Returns true when the vehicle has gone longer than its current age-adjusted
     * service interval since its last maintenance.
     */
    public boolean isMaintenanceDue() {
        return ageSeconds - lastMaintenanceAgeSeconds >= getMaintenanceIntervalSeconds();
    }

    /**
     * Returns true when the vehicle has consumed at least 75 % of its current
     * service interval — i.e. maintenance is coming up soon even though it is
     * not overdue yet.  Used by the renderer to show a "due soon" amber tint.
     */
    public boolean isMaintenanceDueSoon() {
        return ageSeconds - lastMaintenanceAgeSeconds >= getMaintenanceIntervalSeconds() * 0.75f;
    }

    /**
     * Legacy overload kept for backward-compatibility with existing tests.
     *
     * @deprecated Prefer {@link #isMaintenanceDue()} which uses age-based scheduling.
     */
    @Deprecated
    public boolean isMaintenanceDue(float maintenanceIntervalSeconds) {
        return ageSeconds - lastMaintenanceAgeSeconds >= maintenanceIntervalSeconds;
    }

    /**
     * Returns the maintenance cost for this vehicle in its current age.
     * Scales linearly from {@link EconomyConfig#VEHICLE_MAINTENANCE_COST} at age 0
     * up to {@link EconomyConfig#MAINTENANCE_MAX_COST_MULTIPLIER}× that value.
     */
    public long getMaintenanceCost() {
        float multiplier = 1f + ageSeconds / EconomyConfig.MAINTENANCE_COST_AGE_SCALE;
        multiplier = Math.min(multiplier, EconomyConfig.MAINTENANCE_MAX_COST_MULTIPLIER);
        return (long) (EconomyConfig.VEHICLE_MAINTENANCE_COST * multiplier);
    }

    /**
     * Returns a [0.0, 1.0] speed factor applied when maintenance is overdue.
     * <ul>
     *   <li>1.0 – maintenance is current, no penalty</li>
     *   <li>0.75 – overdue by up to one additional interval (vehicle is "struggling")</li>
     *   <li>0.50 – overdue by two or more intervals (vehicle is "breaking down")</li>
     * </ul>
     * The penalty resets to 1.0 after the vehicle is serviced.
     */
    public float getMaintenanceOverdueFactor() {
        float sinceLastService = ageSeconds - lastMaintenanceAgeSeconds;
        float interval = getMaintenanceIntervalSeconds();
        if (sinceLastService < interval) {
            return 1.0f;
        }
        float overdueIntervals = (sinceLastService - interval) / interval;
        if (overdueIntervals < 1f) {
            return 0.75f;
        }
        return 0.50f;
    }

    public void performMaintenance() {
        this.lastMaintenanceAgeSeconds = ageSeconds;
    }
}
