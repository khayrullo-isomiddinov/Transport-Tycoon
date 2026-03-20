package com.team404.tycoon.model;

public enum BuildMode {
    ROAD("Road", TileType.ROAD),
    WATER("Water", TileType.WATER),
    FOREST("Forest", TileType.FOREST),
    DEMOLISH("Demolish", TileType.EMPTY);

    private final String label;
    private final TileType targetType;

    BuildMode(String label, TileType targetType) {
        this.label = label;
        this.targetType = targetType;
    }

    public String getLabel() {
        return label;
    }

    public TileType getTargetType() {
        return targetType;
    }
}
