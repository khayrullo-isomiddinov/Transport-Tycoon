package com.team404.tycoon.controller;

import com.team404.tycoon.model.EconomyConfig;
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
import com.team404.tycoon.controller.InputController;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
        // Build a driveable road between the two towns so the vehicle would normally move.
        // The traffic light at (10,5) is the only reason it should stop.
        for (int x = 2; x <= 18; x++) {
            if (x == 10) {
                state.addDecoration(new PlacedDecoration(x, 5, "resources/trafficlights.png", 1, 1));
            } else {
                state.addDecoration(new PlacedDecoration(x, 5, "resources/highway-straight.png", 1, 1));
            }
        }
        state.addRoute(new Route("r-3", List.of(new RouteStop(0), new RouteStop(1))));
        // Horizontal green = 1s, so at simulation time ~1.5s the horizontal direction is red.
        state.setTrafficLightDurations(1f, 10f);

        VehicleType bus = new VehicleType("Bus", 10, 8f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-3", "r-3", bus, 0);
        state.addVehicle(vehicle);

        new GameController(state).update(1.5f);

        assertEquals(0f, vehicle.getLegProgressTiles(), 0.0001f, "Vehicle should remain stopped at red signal");
    }

    @Test
    void vehicleMovesOnGreenTrafficLight() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 5));
        state.addTown(new Town("B", 18, 5));
        // Same road layout as the red-light test.
        for (int x = 2; x <= 18; x++) {
            if (x == 10) {
                state.addDecoration(new PlacedDecoration(x, 5, "resources/trafficlights.png", 1, 1));
            } else {
                state.addDecoration(new PlacedDecoration(x, 5, "resources/highway-straight.png", 1, 1));
            }
        }
        state.addRoute(new Route("r-green", List.of(new RouteStop(0), new RouteStop(1))));
        // Horizontal green = 10s. With the phase offset for tile (10,5), the horizontal
        // direction is green from the very start of the simulation, so a short update moves
        // the vehicle rather than blocking it.
        state.setTrafficLightDurations(10f, 1f);

        VehicleType bus = new VehicleType("Bus", 10, 8f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-green", "r-green", bus, 0);
        state.addVehicle(vehicle);

        new GameController(state).update(0.1f);

        assertTrue(vehicle.getLegProgressTiles() > 0f, "Vehicle should move when traffic light is green for its direction");
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
        state.addDecoration(new PlacedDecoration(3, 2, "resources/right-and-down.png", 1, 1));
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
        state.addDecoration(new PlacedDecoration(3, 2, "resources/up-and-right.png", 1, 1));
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
        // Start with just enough to buy a truck and have resale value left over.
        long initialBalance = EconomyConfig.TRUCK_PURCHASE_COST + EconomyConfig.TRUCK_RESALE_VALUE;
        state.setBalance(initialBalance);
        state.addTown(new Town("A", 3, 3));
        state.addTown(new Town("B", 10, 10));
        state.addRoute(new Route("r-4", List.of(new RouteStop(0), new RouteStop(1))));
        state.getMap().getTile(6, 5).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(5, 5, "resources/garage.png", 1, 1));
        GameController controller = new GameController(state);

        boolean purchased = controller.purchaseVehicleAtGarage(5, 5, TransportContentType.GOODS);

        assertTrue(purchased);
        assertEquals(1, state.getVehicles().size());
        assertEquals(EconomyConfig.TRUCK_RESALE_VALUE, state.getBalance(), "Purchase should spend truck cost");

        String vehicleId = state.getVehicles().get(0).getId();
        boolean sold = controller.sellVehicle(vehicleId);
        assertTrue(sold);
        assertEquals(0, state.getVehicles().size());
        assertEquals(EconomyConfig.TRUCK_PURCHASE_COST, state.getBalance(), "Selling should refund resale value");

        assertFalse(controller.sellVehicle("missing-id"));
    }

    @Test
    void purchaseBlockedWhenInsufficientFunds() {
        GameState state = new GameState(32, 32);
        state.setBalance(EconomyConfig.TRUCK_PURCHASE_COST - 1L);
        state.addTown(new Town("A", 3, 3));
        state.addTown(new Town("B", 10, 10));
        state.addRoute(new Route("r-5", List.of(new RouteStop(0), new RouteStop(1))));
        state.getMap().getTile(6, 5).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(5, 5, "resources/garage.png", 1, 1));
        GameController controller = new GameController(state);

        boolean purchased = controller.purchaseVehicleAtGarage(5, 5, TransportContentType.GOODS);

        assertFalse(purchased, "Purchase must be refused when player cannot afford it");
        assertEquals(0, state.getVehicles().size());
        assertEquals(EconomyConfig.TRUCK_PURCHASE_COST - 1L, state.getBalance(), "Balance must not change on failed purchase");
    }

    @Test
    void chargeRunningCostTriggersBankruptcy() {
        GameState state = new GameState(32, 32);
        state.setBalance(EconomyConfig.VEHICLE_MAINTENANCE_COST - 1L);

        assertFalse(state.isBankrupt(), "Not bankrupt before the charge");
        state.chargeRunningCost(EconomyConfig.VEHICLE_MAINTENANCE_COST);

        assertTrue(state.isBankrupt(), "Negative balance after mandatory cost must trigger bankruptcy");
        assertTrue(state.getBalance() < 0, "Balance must actually go negative");
    }

    @Test
    void bankruptcyFreezesSimulationAndBlocksPurchases() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 2));
        state.addTown(new Town("B", 10, 2));
        paintHorizontalRoad(state, 2, 10, 2);
        state.addRoute(new Route("r-bankrupt", List.of(new RouteStop(0), new RouteStop(1))));
        VehicleType bus = new VehicleType("Bus", 10, 10f, EnumSet.of(TransportContentType.PASSENGERS));
        Vehicle vehicle = new Vehicle("v-bankrupt", "r-bankrupt", bus, 0);
        state.addVehicle(vehicle);
        state.setBalance(0L);
        state.chargeRunningCost(1L); // force bankruptcy

        assertTrue(state.isBankrupt());

        GameController controller = new GameController(state);
        controller.update(5.0f); // should be a no-op due to bankruptcy

        assertEquals(0f, vehicle.getLegProgressTiles(), 0.0001f, "Bankrupt simulation must not advance vehicles");

        // Purchase must also be blocked regardless of whether funds exist.
        state.getMap().getTile(3, 3).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(3, 3, "resources/garage.png", 1, 1));
        boolean purchased = controller.purchaseVehicleAtGarage(3, 3, TransportContentType.GOODS);
        assertFalse(purchased, "Bankrupt company must not be able to buy vehicles");
    }

    // ── Height-restriction tests ──────────────────────────────────────────────

    @Test
    void roadPlacementRejectedWhenHeightDiffTooLarge() {
        GameState state = new GameState(20, 20);
        state.setBalance(EconomyConfig.INITIAL_CAPITAL);

        // Lay a flat road at (4, 5) – height defaults to 1.
        state.getMap().getTile(4, 5).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(4, 5, "resources/highway-straight.png", 1, 1));

        // The tile to the right is a steep peak (height 3).
        state.getMap().getTile(5, 5).setHeight(3);

        InputController input = new InputController(new GameController(state));
        input.setSelectedAssetPath("resources/highway-straight.png");
        input.onPrimaryClick(5, 5);

        assertTrue(input.isLastPlacementRejected(), "Placement on height-3 tile next to height-1 road must be rejected");
        assertEquals(1, state.getDecorations().size(), "No new decoration should be placed on illegal steep terrain");
    }

    @Test
    void roadPlacementAllowedWhenHeightDiffIsOne() {
        GameState state = new GameState(20, 20);
        state.setBalance(EconomyConfig.INITIAL_CAPITAL);

        // Flat road at (4, 5) – height 1 (default).
        state.getMap().getTile(4, 5).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(4, 5, "resources/highway-straight.png", 1, 1));

        // Adjacent tile is a gentle slope (height 2 – difference is exactly 1).
        state.getMap().getTile(5, 5).setHeight(2);

        InputController input = new InputController(new GameController(state));
        input.setSelectedAssetPath("resources/highway-straight.png");
        input.onPrimaryClick(5, 5);

        assertFalse(input.isLastPlacementRejected(), "Height difference of 1 must be accepted");
        assertEquals(2, state.getDecorations().size(), "Road decoration must be placed on slope-compatible terrain");
    }

    @Test
    void roadPlacementAllowedOnFlatTerrain() {
        GameState state = new GameState(20, 20);
        state.setBalance(EconomyConfig.INITIAL_CAPITAL);

        // All tiles at default height 1.
        state.getMap().getTile(4, 5).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(4, 5, "resources/highway-straight.png", 1, 1));

        InputController input = new InputController(new GameController(state));
        input.setSelectedAssetPath("resources/highway-straight.png");
        input.onPrimaryClick(5, 5);

        assertFalse(input.isLastPlacementRejected(), "Flat terrain (height diff 0) must always be accepted");
        assertEquals(2, state.getDecorations().size(), "Road decoration must be placed on flat terrain");
    }

    @Test
    void roadPlacementAllowedOnHighTileWithNoAdjacentRoads() {
        GameState state = new GameState(20, 20);
        state.setBalance(EconomyConfig.INITIAL_CAPITAL);

        // Place a road start on a peak – no neighbours yet, so no height check applies.
        state.getMap().getTile(5, 5).setHeight(3);

        InputController input = new InputController(new GameController(state));
        input.setSelectedAssetPath("resources/highway-straight.png");
        input.onPrimaryClick(5, 5);

        assertFalse(input.isLastPlacementRejected(), "Isolated road start must be allowed regardless of tile height");
        assertEquals(1, state.getDecorations().size(), "Road decoration must be placed when no adjacent roads exist");
    }

    @Test
    void dragBuildRejectsIndividualTilesWithSteepHeight() {
        GameState state = new GameState(20, 20);
        state.setBalance(EconomyConfig.INITIAL_CAPITAL);

        // Road network starting tile at height 1.
        state.getMap().getTile(3, 5).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(3, 5, "resources/highway-straight.png", 1, 1));

        // Simulate drag: tiles 4 and 5. Tile 4 is accessible (height 2, diff=1),
        // tile 5 is a cliff (height 4, diff vs tile 4 = 2 → rejected).
        state.getMap().getTile(4, 5).setHeight(2);
        state.getMap().getTile(5, 5).setHeight(4);

        InputController input = new InputController(new GameController(state));
        input.setSelectedAssetPath("resources/highway-straight.png");

        // Simulate what MapInputProcessor does on drag release.
        input.onPrimaryClick(4, 5); // legal: height 2 next to height 1
        boolean tile4Accepted = !input.isLastPlacementRejected();

        input.onPrimaryClick(5, 5); // illegal: height 4 next to height 2 (diff = 2)
        boolean tile5Rejected = input.isLastPlacementRejected();

        assertTrue(tile4Accepted, "Tile at height 2 adjacent to height-1 road must be accepted");
        assertTrue(tile5Rejected, "Tile at height 4 adjacent to height-2 road must be rejected");
    }

    private static void paintHorizontalRoad(GameState state, int fromX, int toX, int y) {
        int minX = Math.min(fromX, toX);
        int maxX = Math.max(fromX, toX);
        for (int x = minX; x <= maxX; x++) {
            state.getMap().getTile(x, y).setType(TileType.ROAD);
            state.addDecoration(new PlacedDecoration(x, y, "resources/highway-straight.png", 1, 1));
        }
    }
}
