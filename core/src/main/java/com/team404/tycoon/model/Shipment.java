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

    public Shipment(TransportContentType contentType, int destinationTownIndex, int quantity, int unitRevenue) {
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
}
