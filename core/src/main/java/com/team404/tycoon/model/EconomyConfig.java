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
     * Mandatory maintenance charge per vehicle per service interval.
     * Always deducted even when balance is low; can push the balance
     * negative and trigger bankruptcy.
     *
     * @see com.team404.tycoon.controller.TransportSimulation#MAINTENANCE_INTERVAL_SECONDS
     */
    public static final long VEHICLE_MAINTENANCE_COST = 500L;

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
