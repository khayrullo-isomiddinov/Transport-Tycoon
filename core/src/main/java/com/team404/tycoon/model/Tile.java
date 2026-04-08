package com.team404.tycoon.model;

import java.util.Objects;

/**
 * Immutable coordinates, mutable type and metadata.
 */
public final class Tile {

    private final int x;
    private final int y;
    private TileType type;
    private int height = 1;

    public Tile(int x, int y, TileType type) {
        this.x = x;
        this.y = y;
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TileType getType() {
        return type;
    }

    public void setType(TileType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if (height < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
        this.height = height;
    }
}

