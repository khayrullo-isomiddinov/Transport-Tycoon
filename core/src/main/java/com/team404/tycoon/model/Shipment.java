package com.team404.tycoon.model;

import java.util.Objects;

/**
 * Payload currently loaded into a vehicle.
 */
public final class Shipment {
    private final TransportContentType contentType;
    private final int destinationTownIndex;
    private final int quantity;
    private final int unitRevenue;
    /** Approximate tile distance between origin and destination town centres. Used for distance bonus. */
    private final int distanceTiles;

    public Shipment(TransportContentType contentType, int destinationTownIndex, int quantity, int unitRevenue, int distanceTiles) {
        if (destinationTownIndex < 0) {
            throw new IllegalArgumentException("destinationTownIndex must be >= 0");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (unitRevenue < 0) {
            throw new IllegalArgumentException("unitRevenue must be >= 0");
        }
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        this.destinationTownIndex = destinationTownIndex;
        this.quantity = quantity;
        this.unitRevenue = unitRevenue;
        this.distanceTiles = Math.max(0, distanceTiles);
    }

    /** Convenience constructor with no distance bonus (backwards-compatible). */
    public Shipment(TransportContentType contentType, int destinationTownIndex, int quantity, int unitRevenue) {
        this(contentType, destinationTownIndex, quantity, unitRevenue, 0);
    }

    public TransportContentType getContentType() {
        return contentType;
    }

    public int getDestinationTownIndex() {
        return destinationTownIndex;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitRevenue() {
        return unitRevenue;
    }

    public int getDistanceTiles() {
        return distanceTiles;
    }
}
