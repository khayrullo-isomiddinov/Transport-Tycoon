package com.team404.tycoon.model;

import java.util.Arrays;

/**
 * Grid-based map model.
 * This belongs firmly in the Model layer and is UI-agnostic.
 */
public class GameMap {

    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public GameMap(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.tiles = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x] = new Tile(x, y, TileType.EMPTY);
            }
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Tile getTile(int x, int y) {
        if (!isInBounds(x, y)) {
            throw new IndexOutOfBoundsException("Out of bounds: (" + x + "," + y + ")");
        }
        return tiles[y][x];
    }

    public boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public void fill(TileType type) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tiles[y][x].setType(type);
            }
        }
    }

    public Tile[][] snapshot() {
        Tile[][] copy = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Tile original = tiles[y][x];
                Tile copied = new Tile(original.getX(), original.getY(), original.getType());
                copied.setHeight(original.getHeight());
                copy[y][x] = copied;
            }
        }
        return copy;
    }

    @Override
    public String toString() {
        return "GameMap{" +
                "width=" + width +
                ", height=" + height +
                ", tiles=" + Arrays.deepToString(tiles) +
                '}';
    }
}

