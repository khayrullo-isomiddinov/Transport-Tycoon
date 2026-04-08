package com.team404.tycoon.model;

public enum BuildMode {
    ROAD("Road", TileType.ROAD),
    WATER("Water", TileType.WATER),
    FOREST("Forest", TileType.FOREST),
    DEMOLISH("Demolish", TileType.EMPTY),
    /** Raise a tile's height by 1 (costs money, cannot exceed height 3). */
    RAISE_TERRAIN("Raise", null),
    /** Lower a tile's height by 1 (costs money, cannot go below height 1). */
    LOWER_TERRAIN("Lower", null);

    private final String label;
    /** The tile type to paint on click. Null for modes that don't change tile type (e.g. terraform). */
    private final TileType targetType;

    BuildMode(String label, TileType targetType) {
        this.label = label;
        this.targetType = targetType;
    }

    public String getLabel() {
        return label;
    }

    /** May return null for modes that do not repaint the tile type (RAISE_TERRAIN, LOWER_TERRAIN). */
    public TileType getTargetType() {
        return targetType;
    }
}
