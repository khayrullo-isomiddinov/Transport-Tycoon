package com.team404.tycoon.model;

import java.util.Objects;

/**
 * Generated town metadata shown in the world renderer.
 */
public final class Town {
    private final String name;
    private final int centerX;
    private final int centerY;

    public Town(String name, int centerX, int centerY) {
        this.name = Objects.requireNonNull(name, "name");
        this.centerX = centerX;
        this.centerY = centerY;
    }

    public String getName() {
        return name;
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }
}
