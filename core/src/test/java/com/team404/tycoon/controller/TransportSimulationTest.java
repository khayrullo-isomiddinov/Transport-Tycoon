package com.team404.tycoon.controller;

import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.Route;
import com.team404.tycoon.model.RouteStop;
import com.team404.tycoon.model.Town;
import com.team404.tycoon.model.TileType;
import com.team404.tycoon.model.TransportContentType;
import com.team404.tycoon.model.TransportDemand;
import com.team404.tycoon.model.Vehicle;
import com.team404.tycoon.model.VehicleType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportSimulationTest {

    @Test
    void starterTransportBootstrapCreatesLoopAndVehicles() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 1, 1));
        state.addTown(new Town("B", 10, 1));
        state.addTown(new Town("C", 20, 1));

        state.bootstrapStarterTransport();

        assertEquals(1, state.getRoutes().size(), "Starter bootstrap creates one route");
        assertEquals(2, state.getVehicles().size(), "Starter bootstrap creates default bus and truck");
        assertEquals(3, state.getRoutes().get(0).getStops().size(), "Starter route should include all towns");
    }

    @Test
    void deliveryLoadsMovesUnloadsAndGeneratesIncome() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("Alpha", 4, 4));
        state.addTown(new Town("Beta", 20, 4));
        paintHorizontalRoad(state, 4, 20, 4);
        state.addRoute(new Route("r-1", List.of(new RouteStop(0), new RouteStop(1))));

        VehicleType mixedBus = new VehicleType(
                "Mixed Bus",
                10,
                10f,
                EnumSet.of(TransportContentType.PASSENGERS, TransportContentType.GOODS));
        Vehicle vehicle = new Vehicle("v-1", "r-1", mixedBus, 0);
        state.addVehicle(vehicle);

        state.addTransportDemand(new TransportDemand(0, 1, TransportContentType.PASSENGERS, 4, 20));
        state.addTransportDemand(new TransportDemand(0, 1, TransportContentType.GOODS, 3, 30));

        new GameController(state).update(4.0f);

        assertTrue(state.getTransportDemand().isEmpty(), "Demand should be fully loaded and delivered");
        assertTrue(vehicle.getLoadedShipments().isEmpty(), "Vehicle should unload at destination");
        assertEquals(170L, state.getBalance(), "Income should equal delivered quantity * unit revenue");
        assertEquals(170L, state.getLifetimeIncome(), "Lifetime income should track successful deliveries");
    }

    @Test
    void vehicleOnlyLoadsCompatibleContentAndValidDestinations() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 1, 1));
        state.addTown(new Town("B", 10, 1));
        state.addTown(new Town("C", 20, 1));
        paintHorizontalRoad(state, 1, 10, 1);
        state.addRoute(new Route("r-2", List.of(new RouteStop(0), new RouteStop(1))));

        VehicleType passengerOnly = new VehicleType(
                "Passenger Van",
                8,
                100f,
                EnumSet.of(TransportContentType.PASSENGERS));
        state.addVehicle(new Vehicle("v-2", "r-2", passengerOnly, 0));

        state.addTransportDemand(new TransportDemand(0, 1, TransportContentType.PASSENGERS, 5, 10));
        state.addTransportDemand(new TransportDemand(0, 1, TransportContentType.GOODS, 5, 10));
        state.addTransportDemand(new TransportDemand(0, 2, TransportContentType.PASSENGERS, 5, 10));

        new GameController(state).update(1.0f);

        assertEquals(50L, state.getBalance(), "Only valid, compatible passenger demand should be delivered");
        assertEquals(2, state.getTransportDemand().size(), "Invalid destination and incompatible goods remain pending");
    }

    @Test
    void vehicleStopsOnRedTrafficLight() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 5));
        state.addTown(new Town("B", 18, 5));
        state.addRoute(new Route("r-3", List.of(new RouteStop(0), new RouteStop(1))));
        state.setTrafficLightDurations(1f, 10f);

        VehicleType bus = new VehicleType("Bus", 10, 8f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-3", "r-3", bus, 0);
        state.addVehicle(vehicle);
        state.addDecoration(new PlacedDecoration(10, 5, "resources/trafficlights.png", 1, 1));

        new GameController(state).update(1.5f);

        assertEquals(0f, vehicle.getLegProgressTiles(), 0.0001f, "Vehicle should remain stopped at red signal");
    }

    @Test
    void vehicleStopsWhenLegCrossesWater() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 2));
        state.addTown(new Town("B", 10, 2));
        state.addRoute(new Route("r-water", List.of(new RouteStop(0), new RouteStop(1))));
        state.getMap().getTile(6, 2).setType(TileType.WATER);

        VehicleType bus = new VehicleType("Bus", 10, 10f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-water", "r-water", bus, 0);
        state.addVehicle(vehicle);

        new GameController(state).update(2.0f);

        assertEquals(0f, vehicle.getLegProgressTiles(), 0.0001f, "Vehicle should stop before water-crossing leg");
    }

    @Test
    void vehicleStopsWithoutRoadConnectionEvenOnLand() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 2));
        state.addTown(new Town("B", 10, 2));
        state.addRoute(new Route("r-land", List.of(new RouteStop(0), new RouteStop(1))));

        VehicleType bus = new VehicleType("Bus", 10, 10f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-land", "r-land", bus, 0);
        state.addVehicle(vehicle);

        new GameController(state).update(2.0f);

        assertEquals(0f, vehicle.getLegProgressTiles(), 0.0001f, "Vehicle should stop when no drivable road path exists");
    }

    @Test
    void highwayIntersectionAndTrafficLightsDecorationsCountAsRoad() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 2));
        state.addTown(new Town("B", 6, 2));
        state.addRoute(new Route("r-decor-road", List.of(new RouteStop(0), new RouteStop(1))));
        state.addDecoration(new PlacedDecoration(2, 2, "resources/highway-straight.png", 1, 1));
        state.addDecoration(new PlacedDecoration(3, 2, "resources/intersection.png", 1, 1));
        state.addDecoration(new PlacedDecoration(4, 2, "resources/trafficlights.png", 1, 1));
        state.addDecoration(new PlacedDecoration(5, 2, "resources/highway-top-left.png", 1, 1));
        state.addDecoration(new PlacedDecoration(6, 2, "resources/highway-straight.png", 1, 1));

        VehicleType bus = new VehicleType("Bus", 10, 10f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-decor-road", "r-decor-road", bus, 0);
        state.addVehicle(vehicle);

        new GameController(state).update(1.0f);

        assertTrue(vehicle.getLegProgressTiles() > 0f, "Vehicle should move on recognized road decorations");
    }

    @Test
    void highwayAndTrafficGenericPngNamesCountAsRoad() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 2));
        state.addTown(new Town("B", 5, 2));
        state.addRoute(new Route("r-generic-road", List.of(new RouteStop(0), new RouteStop(1))));
        state.addDecoration(new PlacedDecoration(2, 2, "resources/highway.png", 1, 1));
        state.addDecoration(new PlacedDecoration(3, 2, "resources/intersection.png", 1, 1));
        state.addDecoration(new PlacedDecoration(4, 2, "resources/traffic.png", 1, 1));
        state.addDecoration(new PlacedDecoration(5, 2, "resources/highway.png", 1, 1));

        VehicleType bus = new VehicleType("Bus", 10, 10f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-generic-road", "r-generic-road", bus, 0);
        state.addVehicle(vehicle);

        new GameController(state).update(1.0f);

        assertTrue(vehicle.getLegProgressTiles() > 0f, "Vehicle should move on highway.png/intersection/traffic.png");
    }

    @Test
    void vehicleDoesNotDriveThroughNonRoadDecorationOnRoadTile() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 2));
        state.addTown(new Town("B", 10, 2));
        paintHorizontalRoad(state, 2, 10, 2);
        state.addRoute(new Route("r-block", List.of(new RouteStop(0), new RouteStop(1))));

        // Put a blocking sprite directly on the road line.
        state.addDecoration(new PlacedDecoration(5, 2, "resources/tree.png", 1, 1));

        VehicleType bus = new VehicleType("Bus", 10, 10f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-block", "r-block", bus, 0);
        state.addVehicle(vehicle);

        new GameController(state).update(1.0f);

        assertEquals(0f, vehicle.getLegProgressTiles(), 0.0001f, "Vehicle should not move onto a blocked road tile");
    }

    @Test
    void purchaseAndSellVehicleViaGarageControllerApis() {
        GameState state = new GameState(32, 32);
        state.setBalance(500L);
        state.addTown(new Town("A", 3, 3));
        state.addTown(new Town("B", 10, 10));
        state.addRoute(new Route("r-4", List.of(new RouteStop(0), new RouteStop(1))));
        state.getMap().getTile(6, 5).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(5, 5, "resources/garage.png", 1, 1));
        GameController controller = new GameController(state);

        boolean purchased = controller.purchaseVehicleAtGarage(5, 5, TransportContentType.GOODS);

        assertTrue(purchased);
        assertEquals(1, state.getVehicles().size());
        assertEquals(250L, state.getBalance(), "Purchase should spend fixed vehicle price");

        String vehicleId = state.getVehicles().get(0).getId();
        boolean sold = controller.sellVehicle(vehicleId);
        assertTrue(sold);
        assertEquals(0, state.getVehicles().size());
        assertEquals(375L, state.getBalance(), "Selling should add resale income");

        assertFalse(controller.sellVehicle("missing-id"));
    }

    private static void paintHorizontalRoad(GameState state, int fromX, int toX, int y) {
        int minX = Math.min(fromX, toX);
        int maxX = Math.max(fromX, toX);
        for (int x = minX; x <= maxX; x++) {
            state.getMap().getTile(x, y).setType(TileType.ROAD);
        }
    }
}
