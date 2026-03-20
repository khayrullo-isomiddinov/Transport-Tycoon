package com.team404.tycoon.model;

import java.util.Objects;

/**
 * Immutable coordinates, mutable type and metadata.
 */
public final class Tile {

    private final int x;
    private final int y;
    private TileType type;

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
}

