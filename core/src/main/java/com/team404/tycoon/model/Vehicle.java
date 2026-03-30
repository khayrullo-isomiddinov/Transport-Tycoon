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

    public boolean isMaintenanceDue(float maintenanceIntervalSeconds) {
        return ageSeconds - lastMaintenanceAgeSeconds >= maintenanceIntervalSeconds;
    }

    public void performMaintenance() {
        this.lastMaintenanceAgeSeconds = ageSeconds;
    }
}
