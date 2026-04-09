package com.team404.tycoon.model;

import java.util.Objects;

/**
 * Generated town metadata shown in the world renderer.
 */
public final class Town {
    private final String name;
    private final int centerX;
    private final int centerY;
    private int population;
    /** Simulation time (seconds) when a vehicle last visited this town. */
    private float lastServicedTime = -9999f;

    public Town(String name, int centerX, int centerY) {
        this(name, centerX, centerY, 500);
    }

    public Town(String name, int centerX, int centerY, int population) {
        this.name = Objects.requireNonNull(name, "name");
        this.centerX = centerX;
        this.centerY = centerY;
        this.population = Math.max(100, population);
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

    public int getPopulation() {
        return population;
    }

    public void setPopulation(int population) {
        this.population = Math.max(100, population);
    }

    public float getLastServicedTime() {
        return lastServicedTime;
    }

    public void setLastServicedTime(float t) {
        this.lastServicedTime = t;
    }
}
