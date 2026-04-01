package com.team404.tycoon.controller;

import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.RoadPathfinder;
import com.team404.tycoon.model.Route;
import com.team404.tycoon.model.RouteStop;
import com.team404.tycoon.model.Shipment;
import com.team404.tycoon.model.Town;
import com.team404.tycoon.model.TransportDemand;
import com.team404.tycoon.model.Vehicle;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Advances vehicles over routes and processes loading/unloading.
 */
public final class TransportSimulation {
    static final float MIN_TRAVEL_DISTANCE = 1f;
    static final float MAINTENANCE_INTERVAL_SECONDS = 120f;
    private static final float GLOBAL_VEHICLE_SPEED_MULTIPLIER = 0.5f;

    private TransportSimulation() {
    }

    public static void update(GameState state, float deltaSeconds) {
        if (deltaSeconds <= 0f) {
            return;
        }
        state.advanceSimulationTime(deltaSeconds);
        for (Vehicle vehicle : state.getVehiclesMutable()) {
            vehicle.addAgeSeconds(deltaSeconds);
            Route route = state.findRouteById(vehicle.getRouteId()).orElse(null);
            if (route == null || route.getStopCount() < 2) {
                continue;
            }
            if (vehicle.getCurrentStopIndex() >= route.getStopCount()) {
                vehicle.setCurrentStopIndex(0);
            }
            processArrival(state, route, vehicle, vehicle.getCurrentStopIndex());
            float movedTiles = vehicle.getType().getSpeedTilesPerSecond()
                    * GLOBAL_VEHICLE_SPEED_MULTIPLIER
                    * deltaSeconds;
            if (mustStopForMissingRoadConnection(state, route, vehicle) || mustStopForRedLight(state, route, vehicle)) {
                movedTiles = 0f;
            }
            processMovement(state, route, vehicle, movedTiles);
        }
    }

    private static void processMovement(GameState state, Route route, Vehicle vehicle, float movedTiles) {
        float progress = vehicle.getLegProgressTiles() + movedTiles;
        int currentStop = vehicle.getCurrentStopIndex();
        while (true) {
            int nextStop = (currentStop + 1) % route.getStopCount();
            float distance = legDistance(state, route.getStops().get(currentStop), route.getStops().get(nextStop));
            if (progress < distance) {
                break;
            }
            progress -= distance;
            currentStop = nextStop;
            processArrival(state, route, vehicle, currentStop);
        }
        vehicle.setCurrentStopIndex(currentStop);
        vehicle.setLegProgressTiles(progress);
    }

    private static float legDistance(GameState state, RouteStop from, RouteStop to) {
        Town fromTown = state.getTown(from.getTownIndex()).orElse(null);
        Town toTown = state.getTown(to.getTownIndex()).orElse(null);
        if (fromTown == null || toTown == null) {
            return MIN_TRAVEL_DISTANCE;
        }
        List<int[]> path = RoadPathfinder.findRoadPath(state, fromTown, toTown, 6);
        if (path.size() < 2) {
            return MIN_TRAVEL_DISTANCE;
        }
        return Math.max(MIN_TRAVEL_DISTANCE, path.size() - 1);
    }

    private static void processArrival(GameState state, Route route, Vehicle vehicle, int arrivedStopIndex) {
        int townIndex = route.getStops().get(arrivedStopIndex).getTownIndex();
        unloadAtTown(state, vehicle, townIndex);
        loadAtTown(state, route, vehicle, townIndex);
        processMaintenance(state, townIndex, vehicle);
    }

    private static void unloadAtTown(GameState state, Vehicle vehicle, int townIndex) {
        List<Shipment> retained = new ArrayList<>();
        long revenue = 0L;
        for (Shipment shipment : vehicle.getLoadedShipments()) {
            if (shipment.getDestinationTownIndex() == townIndex) {
                revenue += (long) shipment.getQuantity() * shipment.getUnitRevenue();
            } else {
                retained.add(shipment);
            }
        }
        vehicle.clearLoadedShipments();
        for (Shipment shipment : retained) {
            vehicle.addShipment(shipment);
        }
        if (revenue > 0L) {
            state.addIncome(revenue);
        }
    }

    private static void loadAtTown(GameState state, Route route, Vehicle vehicle, int townIndex) {
        int freeCapacity = vehicle.getFreeCapacity();
        if (freeCapacity <= 0) {
            return;
        }
        Iterator<TransportDemand> iterator = state.getTransportDemandMutable().iterator();
        while (iterator.hasNext() && freeCapacity > 0) {
            TransportDemand demand = iterator.next();
            if (demand.getOriginTownIndex() != townIndex) {
                continue;
            }
            if (!vehicle.getType().supports(demand.getContentType())) {
                continue;
            }
            if (!route.containsTownIndex(demand.getDestinationTownIndex())) {
                continue;
            }
            int loaded = demand.removeQuantity(freeCapacity);
            if (loaded > 0) {
                vehicle.addShipment(new Shipment(
                        demand.getContentType(),
                        demand.getDestinationTownIndex(),
                        loaded,
                        demand.getUnitRevenue()));
                freeCapacity -= loaded;
            }
            if (demand.isDepleted()) {
                iterator.remove();
            }
        }
    }

    private static void processMaintenance(GameState state, int townIndex, Vehicle vehicle) {
        if (!vehicle.isMaintenanceDue(MAINTENANCE_INTERVAL_SECONDS)) {
            return;
        }
        Town town = state.getTown(townIndex).orElse(null);
        if (town == null) {
            return;
        }
        if (!state.hasConnectedGarageNearTown(town, 10)) {
            return;
        }
        long maintenanceCost = 15L;
        if (state.spendMoney(maintenanceCost)) {
            vehicle.performMaintenance();
        }
    }

    private static boolean mustStopForRedLight(GameState state, Route route, Vehicle vehicle) {
        int stopCount = route.getStopCount();
        int fromIndex = vehicle.getCurrentStopIndex() % stopCount;
        int toIndex = (fromIndex + 1) % stopCount;
        Town fromTown = state.getTown(route.getStops().get(fromIndex).getTownIndex()).orElse(null);
        Town toTown = state.getTown(route.getStops().get(toIndex).getTownIndex()).orElse(null);
        if (fromTown == null || toTown == null) {
            return false;
        }
        boolean horizontal = Math.abs(toTown.getCenterX() - fromTown.getCenterX())
                >= Math.abs(toTown.getCenterY() - fromTown.getCenterY());

        float ax = fromTown.getCenterX();
        float ay = fromTown.getCenterY();
        float bx = toTown.getCenterX();
        float by = toTown.getCenterY();
        float abLen2 = (bx - ax) * (bx - ax) + (by - ay) * (by - ay);
        float segLen = (float) Math.sqrt(abLen2);
        float vehicleT = (segLen > 0f) ? Math.min(1f, vehicle.getLegProgressTiles() / segLen) : 0f;

        // Find only the nearest traffic light *ahead* of the vehicle on this leg.
        int[] nextLight = null;
        float nextLightT = Float.MAX_VALUE;
        for (int[] light : state.getTrafficLightTiles()) {
            if (!isNearLine(light[0], light[1], fromTown, toTown)) {
                continue;
            }
            float lightT = (abLen2 > 0f)
                    ? Math.max(0f, Math.min(1f,
                            ((light[0] - ax) * (bx - ax) + (light[1] - ay) * (by - ay)) / abLen2))
                    : 0f;
            if (lightT >= vehicleT && lightT < nextLightT) {
                nextLightT = lightT;
                nextLight = light;
            }
        }

        return nextLight != null && !isLightGreenForDirection(state, nextLight[0], nextLight[1], horizontal);
    }

    private static boolean mustStopForMissingRoadConnection(GameState state, Route route, Vehicle vehicle) {
        int stopCount = route.getStopCount();
        int fromIndex = vehicle.getCurrentStopIndex() % stopCount;
        int toIndex = (fromIndex + 1) % stopCount;
        Town fromTown = state.getTown(route.getStops().get(fromIndex).getTownIndex()).orElse(null);
        Town toTown = state.getTown(route.getStops().get(toIndex).getTownIndex()).orElse(null);
        if (fromTown == null || toTown == null) {
            return false;
        }
        List<int[]> path = RoadPathfinder.findRoadPath(state, fromTown, toTown, 6);
        return path.size() < 2;
    }

    /**
     * Each traffic light gets a deterministic phase offset derived from its tile position,
     * so intersections cycle independently rather than all switching at the same instant.
     */
    private static boolean isLightGreenForDirection(GameState state, int lx, int ly, boolean horizontal) {
        float h = state.getTrafficLightHorizontalGreenSeconds();
        float v = state.getTrafficLightVerticalGreenSeconds();
        float cycle = h + v;
        float phaseOffset = ((lx * 7 + ly * 13) & 0xFF) / 256f * cycle;
        float t = (state.getSimulationTimeSeconds() + phaseOffset) % cycle;
        return horizontal == (t < h);
    }

    private static boolean isNearLine(int lx, int ly, Town fromTown, Town toTown) {
        float ax = fromTown.getCenterX();
        float ay = fromTown.getCenterY();
        float bx = toTown.getCenterX();
        float by = toTown.getCenterY();
        float px = lx;
        float py = ly;

        float abx = bx - ax;
        float aby = by - ay;
        float apx = px - ax;
        float apy = py - ay;
        float abLen2 = abx * abx + aby * aby;
        if (abLen2 <= 0.0001f) {
            return false;
        }
        float t = Math.max(0f, Math.min(1f, (apx * abx + apy * aby) / abLen2));
        float cx = ax + t * abx;
        float cy = ay + t * aby;
        float dx = px - cx;
        float dy = py - cy;
        return dx * dx + dy * dy <= 4.0f;
    }

}
