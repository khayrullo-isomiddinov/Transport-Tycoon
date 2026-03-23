package com.team404.tycoon.model;

import java.util.Objects;

/**
 * A PNG decoration anchored on the map grid. Footprint is axis-aligned in tile space (isometric view).
 */
public final class PlacedDecoration {

    private final int anchorTileX;
    private final int anchorTileY;
    /** Classpath-relative path, e.g. {@code resources/building1.png} */
    private final String resourcePath;
    private final int footprintTilesW;
    private final int footprintTilesH;

    public PlacedDecoration(
            int anchorTileX,
            int anchorTileY,
            String resourcePath,
            int footprintTilesW,
            int footprintTilesH
    ) {
        if (footprintTilesW < 1 || footprintTilesH < 1) {
            throw new IllegalArgumentException("footprint must be positive");
        }
        this.anchorTileX = anchorTileX;
        this.anchorTileY = anchorTileY;
        this.resourcePath = Objects.requireNonNull(resourcePath);
        this.footprintTilesW = footprintTilesW;
        this.footprintTilesH = footprintTilesH;
    }

    public int getAnchorTileX() {
        return anchorTileX;
    }

    public int getAnchorTileY() {
        return anchorTileY;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public int getFootprintTilesW() {
        return footprintTilesW;
    }

    public int getFootprintTilesH() {
        return footprintTilesH;
    }

    /**
     * Painter order for this projection: larger (x + y) sits farther toward the top of the screen
     * (“back”); smaller sum is closer to the bottom (“front”). Multi-tile sprites use the back-most
     * footprint cell so wide buildings sort correctly.
     */
    public int depthSortKey() {
        int max = Integer.MIN_VALUE;
        for (int dy = 0; dy < footprintTilesH; dy++) {
            for (int dx = 0; dx < footprintTilesW; dx++) {
                int tx = anchorTileX + dx;
                int ty = anchorTileY + dy;
                max = Math.max(max, tx + ty);
            }
        }
        return max;
    }

    public boolean occupiesTile(int tileX, int tileY) {
        return tileX >= anchorTileX
                && tileX < anchorTileX + footprintTilesW
                && tileY >= anchorTileY
                && tileY < anchorTileY + footprintTilesH;
    }
}
