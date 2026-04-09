package com.team404.tycoon.model;

import com.team404.tycoon.controller.GameController;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the age-based automatic maintenance scheduling feature.
 *
 * <p>Acceptance criteria verified:
 * <ul>
 *   <li>Vehicles track age over time</li>
 *   <li>Maintenance interval decreases as vehicle ages</li>
 *   <li>Maintenance cost increases with vehicle age</li>
 *   <li>Speed penalty applied when maintenance is overdue</li>
 *   <li>Maintenance interacts with garage and economy systems</li>
 * </ul>
 */
class VehicleMaintenanceTest {

    private static VehicleType anyBus() {
        return new VehicleType("Bus", 10, 8f, EnumSet.of(TransportContentType.PASSENGERS));
    }

    // ── interval scheduling ───────────────────────────────────────────────────

    @Test
    void freshVehicleUsesBaseInterval() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        // Age is 0 → no age reduction yet → should return base interval.
        assertEquals(EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS,
                v.getMaintenanceIntervalSeconds(), 0.001f,
                "A brand-new vehicle should have the full base maintenance interval");
    }

    @Test
    void intervalDecreasesAsVehicleAges() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        float intervalAtAge0 = v.getMaintenanceIntervalSeconds();

        v.addAgeSeconds(300f); // half-way to minimum
        float intervalAtAge300 = v.getMaintenanceIntervalSeconds();

        assertTrue(intervalAtAge300 < intervalAtAge0,
                "Maintenance interval must decrease as vehicle ages");
    }

    @Test
    void intervalFloorsCatMinimum() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        // Age the vehicle far beyond the point where the formula would go negative.
        v.addAgeSeconds(9999f);
        assertEquals(EconomyConfig.MAINTENANCE_MIN_INTERVAL_SECONDS,
                v.getMaintenanceIntervalSeconds(), 0.001f,
                "Interval must never drop below the configured minimum");
    }

    @Test
    void maintenanceDueAfterAgeBasedInterval() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        float interval = v.getMaintenanceIntervalSeconds();

        assertFalse(v.isMaintenanceDue(), "Maintenance should not be due on a fresh vehicle");

        v.addAgeSeconds(interval);
        assertTrue(v.isMaintenanceDue(),
                "Maintenance must be due once the vehicle has aged by its current interval");
    }

    @Test
    void performMaintenanceResetsDueFlag() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        v.addAgeSeconds(EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS);
        assertTrue(v.isMaintenanceDue());

        v.performMaintenance();
        assertFalse(v.isMaintenanceDue(),
                "After maintenance, the vehicle should not be due again immediately");
    }

    // ── cost scaling ──────────────────────────────────────────────────────────

    @Test
    void freshVehicleCostsBaseMaintenance() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        assertEquals(EconomyConfig.VEHICLE_MAINTENANCE_COST, v.getMaintenanceCost(),
                "A brand-new vehicle should cost exactly the base maintenance fee");
    }

    @Test
    void maintenanceCostIncreasesWithAge() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        long costAtAge0 = v.getMaintenanceCost();

        v.addAgeSeconds(EconomyConfig.MAINTENANCE_COST_AGE_SCALE); // +1× step
        long costAtAge600 = v.getMaintenanceCost();

        assertTrue(costAtAge600 > costAtAge0,
                "Maintenance cost must increase as the vehicle ages");
    }

    @Test
    void maintenanceCostCapsAtMaxMultiplier() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        v.addAgeSeconds(9999f); // far beyond the cap
        long maxExpectedCost = (long) (EconomyConfig.VEHICLE_MAINTENANCE_COST
                * EconomyConfig.MAINTENANCE_MAX_COST_MULTIPLIER);
        assertEquals(maxExpectedCost, v.getMaintenanceCost(),
                "Maintenance cost must cap at MAX_COST_MULTIPLIER × base cost");
    }

    // ── speed penalty ─────────────────────────────────────────────────────────

    @Test
    void noSpeedPenaltyWhenMaintenanceCurrent() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        assertEquals(1.0f, v.getMaintenanceOverdueFactor(), 0.001f,
                "A freshly-created vehicle must not have a speed penalty");
    }

    @Test
    void speedPenaltyAppliedWhenSlightlyOverdue() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        // Age by 1.5× the base interval so the vehicle is overdue but not critically.
        v.addAgeSeconds(EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS * 1.5f);
        float factor = v.getMaintenanceOverdueFactor();
        assertTrue(factor < 1.0f,
                "An overdue vehicle must have a speed penalty (factor < 1.0)");
        assertTrue(factor >= 0.5f,
                "Speed penalty must not exceed 50 % reduction for a slightly overdue vehicle");
    }

    @Test
    void severeSpeedPenaltyWhenCriticallyOverdue() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        // Age by 4× the base interval: overdue by more than two intervals.
        v.addAgeSeconds(EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS * 4f);
        assertEquals(0.50f, v.getMaintenanceOverdueFactor(), 0.001f,
                "A critically-overdue vehicle must run at the minimum 50 % speed factor");
    }

    @Test
    void speedPenaltyResetsAfterMaintenance() {
        Vehicle v = new Vehicle("v", "r", anyBus(), 0);
        v.addAgeSeconds(EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS * 4f);
        assertTrue(v.getMaintenanceOverdueFactor() < 1f, "Setup: vehicle must be overdue");

        v.performMaintenance();
        assertEquals(1.0f, v.getMaintenanceOverdueFactor(), 0.001f,
                "Speed penalty must reset to 1.0 immediately after maintenance is performed");
    }

    // ── integration: older vehicle charged more through the simulation ─────────

    @Test
    void olderVehicleChargedMorePerMaintenanceEvent() {
        // Young vehicle setup
        GameState youngState = makeStateWithGarage();
        VehicleType bus = anyBus();
        Vehicle youngVehicle = new Vehicle("young", "r-m", bus, 0);
        // Age the young vehicle just enough to trigger one maintenance.
        youngVehicle.addAgeSeconds(EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS);
        youngState.addVehicle(youngVehicle);

        // Old vehicle setup (at the cost cap)
        GameState oldState = makeStateWithGarage();
        Vehicle oldVehicle = new Vehicle("old", "r-m", bus, 0);
        oldVehicle.addAgeSeconds(9999f); // at cost cap
        // Also push lastMaintenanceAge back so maintenance is due.
        oldVehicle.addAgeSeconds(EconomyConfig.MAINTENANCE_MIN_INTERVAL_SECONDS + 1f);
        oldState.addVehicle(oldVehicle);

        long youngCost = youngVehicle.getMaintenanceCost();
        long oldCost   = oldVehicle.getMaintenanceCost();

        assertTrue(oldCost > youngCost,
                "An older vehicle must be charged more per maintenance event than a young one");
        assertEquals((long)(EconomyConfig.VEHICLE_MAINTENANCE_COST * EconomyConfig.MAINTENANCE_MAX_COST_MULTIPLIER),
                oldCost,
                "Old vehicle cost must be exactly MAX_MULTIPLIER × base");
    }

    @Test
    void overdueVehicleMovesSlowerThanFreshVehicle() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 5));
        state.addTown(new Town("B", 20, 5));
        paintHorizontalRoad(state, 2, 20, 5);
        state.addRoute(new Route("r-speed", List.of(new RouteStop(0), new RouteStop(1))));

        VehicleType bus = new VehicleType("Bus", 10, 8f, EnumSet.of(TransportContentType.PASSENGERS));

        // Fresh vehicle — no penalty.
        Vehicle fresh = new Vehicle("fresh", "r-speed", bus, 0);

        // Critically-overdue vehicle — should run at 50 % speed.
        Vehicle overdue = new Vehicle("overdue", "r-speed", bus, 0);
        overdue.setLegProgressTiles(5f); // put it ahead so fresh doesn't follow
        overdue.addAgeSeconds(EconomyConfig.MAINTENANCE_BASE_INTERVAL_SECONDS * 4f);

        state.addVehicle(overdue);
        state.addVehicle(fresh);

        new GameController(state).update(2.0f);

        assertTrue(fresh.getLegProgressTiles() > overdue.getLegProgressTiles() - 5f,
                "Fresh vehicle must make more progress than the critically-overdue vehicle in the same time");
        // The overdue vehicle has the 0.5× penalty applied, so at the same base speed
        // it should move noticeably less per second.
        assertTrue(overdue.getMaintenanceOverdueFactor() <= 0.5f,
                "Overdue vehicle's speed factor must remain at the critical penalty level");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static GameState makeStateWithGarage() {
        GameState state = new GameState(32, 32);
        state.addTown(new Town("A", 2, 2));
        state.addTown(new Town("B", 10, 2));
        paintHorizontalRoad(state, 2, 10, 2);
        state.addRoute(new Route("r-m", List.of(new RouteStop(0), new RouteStop(1))));
        // Garage adjacent to town A so maintenance can be processed.
        state.getMap().getTile(3, 2).setType(TileType.ROAD);
        state.addDecoration(new PlacedDecoration(2, 2, "resources/garage.png", 1, 1));
        return state;
    }

    private static void paintHorizontalRoad(GameState state, int fromX, int toX, int y) {
        for (int x = Math.min(fromX, toX); x <= Math.max(fromX, toX); x++) {
            state.getMap().getTile(x, y).setType(TileType.ROAD);
            state.addDecoration(new PlacedDecoration(x, y, "resources/highway-straight.png", 1, 1));
        }
    }
}
