package com.team404.tycoon.model;

import java.util.Locale;

/**
 * Central economy configuration: all costs, revenues, and starting values in one place.
 *
 * <p>Adjust values here to rebalance the economy without hunting through multiple files.
 * Future features (loans, interest, difficulty multipliers, running costs per km) should
 * extend this class rather than scatter new magic numbers elsewhere.
 *
 * <p>All amounts are in integer currency units (long for balance fields, int for unit revenues).
 */
public final class EconomyConfig {

    /** Capital given to the player at the start of a new game. */
    public static final long INITIAL_CAPITAL = 100_000L;

    /**
     * Cost to place one road-infrastructure decoration tile (highway, intersection,
     * or traffic light). Charged per placement by the player; does not apply to
     * terrain painted by the map generator.
     */
    public static final long ROAD_DECORATION_COST = 500L;

    /** Purchase price for a passenger bus. */
    public static final long BUS_PURCHASE_COST = 25_000L;

    /** Purchase price for a goods truck. */
    public static final long TRUCK_PURCHASE_COST = 22_000L;

    /** Resale value of a passenger bus (50 % of purchase price). */
    public static final long BUS_RESALE_VALUE = 12_500L;

    /** Resale value of a goods truck (50 % of purchase price). */
    public static final long TRUCK_RESALE_VALUE = 11_000L;

    /**
     * Base mandatory maintenance charge for a brand-new vehicle per service.
     * Actual cost scales up with vehicle age via {@link #MAINTENANCE_COST_AGE_SCALE}.
     * Always deducted even when balance is low; can push the balance
     * negative and trigger bankruptcy.
     */
    public static final long VEHICLE_MAINTENANCE_COST = 500L;

    /**
     * Service interval for a brand-new vehicle (age = 0), in simulation seconds.
     * The effective interval shrinks linearly with vehicle age at a rate of
     * {@link #MAINTENANCE_AGE_REDUCTION_RATE} seconds per second of vehicle age,
     * down to a minimum of {@link #MAINTENANCE_MIN_INTERVAL_SECONDS}.
     */
    public static final float MAINTENANCE_BASE_INTERVAL_SECONDS = 120f;

    /**
     * Minimum service interval regardless of how old the vehicle is, in simulation seconds.
     * A vehicle that reaches this age will need servicing very frequently.
     */
    public static final float MAINTENANCE_MIN_INTERVAL_SECONDS = 30f;

    /**
     * How many seconds the service interval shrinks for every second of vehicle age.
     * Default 0.06: after 1500 s of vehicle age the interval hits the minimum of 30 s.
     */
    public static final float MAINTENANCE_AGE_REDUCTION_RATE = 0.06f;

    /**
     * Every this many seconds of vehicle age, the maintenance cost increases by 100 % of base.
     * Example: age 600 s → cost = 500 × (1 + 600/600) = $1 000.
     */
    public static final float MAINTENANCE_COST_AGE_SCALE = 600f;

    /**
     * Maximum multiplier applied to {@link #VEHICLE_MAINTENANCE_COST} regardless of age.
     * Caps the per-service charge at 3× the base cost.
     */
    public static final float MAINTENANCE_MAX_COST_MULTIPLIER = 3f;

    /**
     * Cost to raise or lower a single tile's height by one level.
     * Charged per click; rejected if the player cannot afford it.
     */
    public static final long TERRAFORM_COST = 2_000L;

    /** Revenue per passenger unit successfully delivered to its destination. */
    public static final int PASSENGER_UNIT_REVENUE = 50;

    /** Revenue per goods unit successfully delivered to its destination. */
    public static final int GOODS_UNIT_REVENUE = 80;

    /**
     * Returns true if the resource path represents a road-infrastructure decoration
     * that the player should be charged for placing (highway, intersection, traffic light).
     * Garages, buildings, and nature decorations are excluded.
     */
    public static boolean isRoadDecoration(String resourcePath) {
        String n = resourcePath.toLowerCase(Locale.ROOT);
        return n.contains("highway")
                || n.contains("-and-")
                || n.contains("to-")
                || n.contains("trafficlights")
                || n.contains("traffic lights")
                || n.endsWith("/+.png");
    }

    private EconomyConfig() {
    }
}
