package com.team404.tycoon.model;

import java.util.Objects;

/**
 * Pending payload waiting at an origin town for a specific destination.
 */
public final class TransportDemand {
    private final int originTownIndex;
    private final int destinationTownIndex;
    private final TransportContentType contentType;
    private int quantity;
    private final int unitRevenue;

    public TransportDemand(int originTownIndex,
                           int destinationTownIndex,
                           TransportContentType contentType,
                           int quantity,
                           int unitRevenue) {
        if (originTownIndex < 0 || destinationTownIndex < 0) {
            throw new IllegalArgumentException("Town indexes must be >= 0");
        }
        if (originTownIndex == destinationTownIndex) {
            throw new IllegalArgumentException("Origin and destination must differ");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }
        if (unitRevenue < 0) {
            throw new IllegalArgumentException("unitRevenue must be >= 0");
        }
        this.originTownIndex = originTownIndex;
        this.destinationTownIndex = destinationTownIndex;
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        this.quantity = quantity;
        this.unitRevenue = unitRevenue;
    }

    public int getOriginTownIndex() {
        return originTownIndex;
    }

    public int getDestinationTownIndex() {
        return destinationTownIndex;
    }

    public TransportContentType getContentType() {
        return contentType;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getUnitRevenue() {
        return unitRevenue;
    }

    public int removeQuantity(int requested) {
        if (requested <= 0) {
            throw new IllegalArgumentException("requested must be > 0");
        }
        int taken = Math.min(requested, quantity);
        quantity -= taken;
        return taken;
    }

    public boolean isDepleted() {
        return quantity <= 0;
    }
}
