package com.team404.tycoon.model;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Vehicle specification used by one or more runtime vehicles.
 */
public final class VehicleType {
    private final String name;
    private final int capacity;
    private final float speedTilesPerSecond;
    private final Set<TransportContentType> supportedContentTypes;

    public VehicleType(String name,
                       int capacity,
                       float speedTilesPerSecond,
                       Set<TransportContentType> supportedContentTypes) {
        this.name = Objects.requireNonNull(name, "name");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        if (speedTilesPerSecond <= 0f) {
            throw new IllegalArgumentException("speedTilesPerSecond must be > 0");
        }
        Objects.requireNonNull(supportedContentTypes, "supportedContentTypes");
        if (supportedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("supportedContentTypes must not be empty");
        }
        this.capacity = capacity;
        this.speedTilesPerSecond = speedTilesPerSecond;
        this.supportedContentTypes = Collections.unmodifiableSet(EnumSet.copyOf(supportedContentTypes));
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public float getSpeedTilesPerSecond() {
        return speedTilesPerSecond;
    }

    public Set<TransportContentType> getSupportedContentTypes() {
        return supportedContentTypes;
    }

    public boolean supports(TransportContentType contentType) {
        return supportedContentTypes.contains(contentType);
    }
}
