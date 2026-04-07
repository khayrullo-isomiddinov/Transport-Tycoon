package com.team404.tycoon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Aggregate root for the simulation.
 * For now it only owns the map; economy, cities, vehicles etc. will be added gradually.
 */
public class GameState {

    private final GameMap map;
    private final List<PlacedDecoration> decorations = new ArrayList<>();
    private final List<Town> towns = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final List<TransportDemand> transportDemand = new ArrayList<>();
    private long balance;
    private long lifetimeIncome;
    private long lifetimeExpenses;
    private boolean bankrupt;
    private float simulationTimeSeconds;
    private float trafficLightHorizontalGreenSeconds = 4.0f;
    private float trafficLightVerticalGreenSeconds = 4.0f;

    public GameState(int mapWidth, int mapHeight) {
        this.map = new GameMap(mapWidth, mapHeight);
    }

    public GameMap getMap() {
        return map;
    }

    public List<PlacedDecoration> getDecorations() {
        return Collections.unmodifiableList(decorations);
    }

    public void addDecoration(PlacedDecoration decoration) {
        decorations.add(decoration);
    }

    public List<Town> getTowns() {
        return Collections.unmodifiableList(towns);
    }

    public void clearTowns() {
        towns.clear();
    }

    public void addTown(Town town) {
        towns.add(town);
    }

    public Optional<Town> getTown(int townIndex) {
        if (townIndex < 0 || townIndex >= towns.size()) {
            return Optional.empty();
        }
        return Optional.of(towns.get(townIndex));
    }

    public List<Route> getRoutes() {
        return Collections.unmodifiableList(routes);
    }

    public List<Vehicle> getVehicles() {
        return Collections.unmodifiableList(vehicles);
    }

    public List<TransportDemand> getTransportDemand() {
        return Collections.unmodifiableList(transportDemand);
    }

    public long getBalance() {
        return balance;
    }

    public long getLifetimeIncome() {
        return lifetimeIncome;
    }

    public long getLifetimeExpenses() {
        return lifetimeExpenses;
    }

    public float getSimulationTimeSeconds() {
        return simulationTimeSeconds;
    }

    public float getTrafficLightHorizontalGreenSeconds() {
        return trafficLightHorizontalGreenSeconds;
    }

    public float getTrafficLightVerticalGreenSeconds() {
        return trafficLightVerticalGreenSeconds;
    }

    public boolean isBankrupt() {
        return bankrupt;
    }

    public boolean canAfford(long amount) {
        return !bankrupt && balance >= amount;
    }

    /** Sets the balance directly (test / scenario setup). Clears bankruptcy if balance is non-negative. */
    public void setBalance(long balance) {
        this.balance = balance;
        if (balance >= 0) {
            this.bankrupt = false;
        }
    }

    public void addIncome(long income) {
        if (income < 0) {
            throw new IllegalArgumentException("income must be >= 0");
        }
        this.balance += income;
        this.lifetimeIncome += income;
    }

    /**
     * Deducts a player-initiated expense. Returns false (and does nothing) if the player
     * cannot afford it or the company is bankrupt.
     */
    public boolean spendMoney(long expense) {
        if (expense < 0) {
            throw new IllegalArgumentException("expense must be >= 0");
        }
        if (!canAfford(expense)) {
            return false;
        }
        balance -= expense;
        lifetimeExpenses += expense;
        return true;
    }

    /**
     * Deducts a mandatory running cost (e.g. vehicle maintenance). Unlike
     * {@link #spendMoney}, this always applies even when funds are low, and will
     * trigger bankruptcy if it pushes the balance below zero.
     */
    public void chargeRunningCost(long cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("cost must be >= 0");
        }
        balance -= cost;
        lifetimeExpenses += cost;
        if (balance < 0 && !bankrupt) {
            bankrupt = true;
        }
    }

    public void setTrafficLightDurations(float horizontalGreenSeconds, float verticalGreenSeconds) {
        if (horizontalGreenSeconds <= 0f || verticalGreenSeconds <= 0f) {
            throw new IllegalArgumentException("Traffic light durations must be > 0");
        }
        this.trafficLightHorizontalGreenSeconds = horizontalGreenSeconds;
        this.trafficLightVerticalGreenSeconds = verticalGreenSeconds;
    }

    public void advanceSimulationTime(float deltaSeconds) {
        if (deltaSeconds > 0f) {
            this.simulationTimeSeconds += deltaSeconds;
        }
    }

    public void addRoute(Route route) {
        if (findRouteById(route.getId()).isPresent()) {
            throw new IllegalArgumentException("Route id already exists: " + route.getId());
        }
        validateRoute(route);
        routes.add(route);
    }

    public Optional<Route> findRouteById(String routeId) {
        for (Route route : routes) {
            if (route.getId().equals(routeId)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    public void addVehicle(Vehicle vehicle) {
        if (findRouteById(vehicle.getRouteId()).isEmpty()) {
            throw new IllegalArgumentException("Vehicle route does not exist: " + vehicle.getRouteId());
        }
        vehicles.add(vehicle);
    }

    public void addTransportDemand(TransportDemand demand) {
        validateDemandIndexes(demand);
        transportDemand.add(demand);
    }

    public void seedRandomDemand(int perTownPairs, int maxUnitsPerPair, Random rng) {
        if (towns.size() < 2) {
            return;
        }
        int pairs = Math.max(1, perTownPairs);
        int maxUnits = Math.max(1, maxUnitsPerPair);
        for (int origin = 0; origin < towns.size(); origin++) {
            for (int i = 0; i < pairs; i++) {
                int destination = rng.nextInt(towns.size() - 1);
                if (destination >= origin) {
                    destination++;
                }
                int passengerUnits = 1 + rng.nextInt(maxUnits);
                int goodsUnits = 1 + rng.nextInt(maxUnits);
                addTransportDemand(new TransportDemand(
                        origin,
                        destination,
                        TransportContentType.PASSENGERS,
                        passengerUnits,
                        EconomyConfig.PASSENGER_UNIT_REVENUE));
                addTransportDemand(new TransportDemand(
                        origin,
                        destination,
                        TransportContentType.GOODS,
                        goodsUnits,
                        EconomyConfig.GOODS_UNIT_REVENUE));
            }
        }
    }

    public void clearTransportState() {
        routes.clear();
        vehicles.clear();
        transportDemand.clear();
        simulationTimeSeconds = 0f;
    }

    public void bootstrapStarterTransport() {
        if (towns.size() < 2) {
            return;
        }
        if (!routes.isEmpty() || !vehicles.isEmpty()) {
            return;
        }

        List<RouteStop> stops = new ArrayList<>();
        for (int i = 0; i < towns.size(); i++) {
            stops.add(new RouteStop(i));
        }
        addRoute(new Route("starter-loop", stops));

        VehicleType bus = new VehicleType(
                "Starter Bus",
                14,
                4.0f,
                Collections.singleton(TransportContentType.PASSENGERS));
        VehicleType truck = new VehicleType(
                "Starter Truck",
                18,
                3.6f,
                Collections.singleton(TransportContentType.GOODS));

        addVehicle(new Vehicle("starter-bus-1", "starter-loop", bus, 0));
        addVehicle(new Vehicle("starter-truck-1", "starter-loop", truck, Math.min(1, stops.size() - 1)));
    }

    /**
     * Removes the top-most decoration whose footprint contains the given tile (last placed wins).
     */
    public boolean removeDecorationAtTile(int tileX, int tileY) {
        for (int i = decorations.size() - 1; i >= 0; i--) {
            if (decorations.get(i).occupiesTile(tileX, tileY)) {
                decorations.remove(i);
                return true;
            }
        }
        return false;
    }

    public Optional<PlacedDecoration> findDecorationAt(int tileX, int tileY) {
        for (int i = decorations.size() - 1; i >= 0; i--) {
            PlacedDecoration d = decorations.get(i);
            if (d.occupiesTile(tileX, tileY)) {
                return Optional.of(d);
            }
        }
        return Optional.empty();
    }

    /**
     * True if every footprint tile is in bounds and does not overlap another decoration.
     * Terrain type (roads, water, forest, etc.) does not block decals.
     */
    public boolean canPlaceDecoration(GameMap map, PlacedDecoration candidate) {
        for (int dy = 0; dy < candidate.getFootprintTilesH(); dy++) {
            for (int dx = 0; dx < candidate.getFootprintTilesW(); dx++) {
                int tx = candidate.getAnchorTileX() + dx;
                int ty = candidate.getAnchorTileY() + dy;
                if (!map.isInBounds(tx, ty)) {
                    return false;
                }
                if (map.getTile(tx, ty).getType() == TileType.WATER) {
                    return false;
                }
            }
        }
        for (PlacedDecoration existing : decorations) {
            if (footprintsOverlap(existing, candidate)) {
                return false;
            }
        }
        return true;
    }

    private static boolean footprintsOverlap(PlacedDecoration a, PlacedDecoration b) {
        int ax1 = a.getAnchorTileX();
        int ay1 = a.getAnchorTileY();
        int ax2 = ax1 + a.getFootprintTilesW();
        int ay2 = ay1 + a.getFootprintTilesH();
        int bx1 = b.getAnchorTileX();
        int by1 = b.getAnchorTileY();
        int bx2 = bx1 + b.getFootprintTilesW();
        int by2 = by1 + b.getFootprintTilesH();
        return ax1 < bx2 && ax2 > bx1 && ay1 < by2 && ay2 > by1;
    }

    private void validateRoute(Route route) {
        for (RouteStop stop : route.getStops()) {
            if (stop.getTownIndex() < 0 || stop.getTownIndex() >= towns.size()) {
                throw new IllegalArgumentException("Route stop references missing town: " + stop.getTownIndex());
            }
        }
    }

    private void validateDemandIndexes(TransportDemand demand) {
        if (demand.getOriginTownIndex() >= towns.size()) {
            throw new IllegalArgumentException("Origin town index out of bounds");
        }
        if (demand.getDestinationTownIndex() >= towns.size()) {
            throw new IllegalArgumentException("Destination town index out of bounds");
        }
    }

    public List<Vehicle> getVehiclesMutable() {
        return vehicles;
    }

    public List<TransportDemand> getTransportDemandMutable() {
        return transportDemand;
    }

    public boolean removeVehicleById(String vehicleId) {
        for (int i = 0; i < vehicles.size(); i++) {
            if (vehicles.get(i).getId().equals(vehicleId)) {
                vehicles.remove(i);
                return true;
            }
        }
        return false;
    }

    public List<int[]> getTrafficLightTiles() {
        List<int[]> tiles = new ArrayList<>();
        for (PlacedDecoration d : decorations) {
            String n = d.getResourcePath().toLowerCase();
            if (n.contains("trafficlights") || n.contains("traffic lights")) {
                tiles.add(new int[]{d.getAnchorTileX(), d.getAnchorTileY()});
            }
        }
        return tiles;
    }

    public List<int[]> getGarageTiles() {
        List<int[]> tiles = new ArrayList<>();
        for (PlacedDecoration d : decorations) {
            String n = d.getResourcePath().toLowerCase();
            if (n.contains("garage")) {
                tiles.add(new int[]{d.getAnchorTileX(), d.getAnchorTileY()});
            }
        }
        return tiles;
    }

    public boolean isGarageConnectedToRoad(int garageTileX, int garageTileY) {
        return isRoadTile(garageTileX + 1, garageTileY)
                || isRoadTile(garageTileX - 1, garageTileY)
                || isRoadTile(garageTileX, garageTileY + 1)
                || isRoadTile(garageTileX, garageTileY - 1);
    }

    public boolean hasConnectedGarageNearTown(Town town, int radiusTiles) {
        for (int[] garage : getGarageTiles()) {
            if (!isGarageConnectedToRoad(garage[0], garage[1])) {
                continue;
            }
            int dx = Math.abs(garage[0] - town.getCenterX());
            int dy = Math.abs(garage[1] - town.getCenterY());
            if (dx + dy <= radiusTiles) {
                return true;
            }
        }
        return false;
    }

    public boolean isDriveableRoadTile(int x, int y) {
        if (!map.isInBounds(x, y)) {
            return false;
        }

        boolean hasRoadDecoration = false;

        for (PlacedDecoration d : decorations) {
            if (!d.occupiesTile(x, y)) {
                continue;
            }
            String n = d.getResourcePath().toLowerCase();
            if (isRoadDecorationResourcePath(n)) {
                hasRoadDecoration = true;
                continue;
            }
            // Any other sprite sitting on top of a tile blocks vehicles from driving through it.
            return false;
        }

        return hasRoadDecoration;
    }

    private static boolean isRoadDecorationResourcePath(String lowerResourcePath) {
        return lowerResourcePath.contains("highway")
                || lowerResourcePath.contains("-and-")
                || lowerResourcePath.contains("to-")
                || lowerResourcePath.contains("trafficlights")
                || lowerResourcePath.endsWith("/+.png")
                || lowerResourcePath.contains("traffic lights")
                || lowerResourcePath.contains("traffic")
                || lowerResourcePath.contains("garage");
    }

    private boolean isRoadTile(int x, int y) {
        return map.isInBounds(x, y) && map.getTile(x, y).getType() == TileType.ROAD;
    }
}

