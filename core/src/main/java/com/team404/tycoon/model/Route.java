package com.team404.tycoon.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Circular route used by vehicles.
 */
public final class Route {
    private final String id;
    private final List<RouteStop> stops;

    public Route(String id, List<RouteStop> stops) {
        this.id = Objects.requireNonNull(id, "id");
        Objects.requireNonNull(stops, "stops");
        if (stops.size() < 2) {
            throw new IllegalArgumentException("Route requires at least 2 stops");
        }
        this.stops = Collections.unmodifiableList(new ArrayList<>(stops));
    }

    public String getId() {
        return id;
    }

    public List<RouteStop> getStops() {
        return stops;
    }

    public boolean containsTownIndex(int townIndex) {
        for (RouteStop stop : stops) {
            if (stop.getTownIndex() == townIndex) {
                return true;
            }
        }
        return false;
    }

    public int getStopCount() {
        return stops.size();
    }
}
