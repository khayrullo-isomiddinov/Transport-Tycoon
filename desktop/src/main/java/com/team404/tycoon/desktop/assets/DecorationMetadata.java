package com.team404.tycoon.desktop.assets;

/**
 * Visual tuning for isometric sprites (rendering only; placement footprint lives in {@code DecorationRules}).
 */
public final class DecorationMetadata {

    private DecorationMetadata() {
    }

    /** Extra vertical draw scale so sprites overlap the row behind (pseudo-3D). */
    public static float verticalOverlapScale(String resourcePath, int footprintW, int footprintH) {
        String n = resourcePath.toLowerCase();
        if (n.contains("building") || n.contains("village") || footprintW + footprintH >= 3) {
            return 1.35f;
        }
        if (n.contains("highway") || n.contains("bighigh")) {
            return 1.15f;
        }
        return 1.25f;
    }
}
