package com.team404.tycoon.model;

/**
 * A stop inside a route, represented by a town index in {@link GameState#getTowns()}.
 */
public final class RouteStop {
    private final int townIndex;

    public RouteStop(int townIndex) {
        if (townIndex < 0) {
            throw new IllegalArgumentException("townIndex must be >= 0");
        }
        this.townIndex = townIndex;
    }

    public int getTownIndex() {
        return townIndex;
    }
}
