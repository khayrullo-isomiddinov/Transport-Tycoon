package com.team404.tycoon.model;

/**
 * Footprint heuristics for PNG decorations (tile grid, axis-aligned).
 */
public final class DecorationRules {

    private DecorationRules() {
    }

    public static int[] footprintForPath(String resourcePath) {
        String n = resourcePath.toLowerCase();
        if (n.contains("highway-straight")) {
            // Keep it horizontal by art orientation, but occupy only one grid cell.
            return new int[]{1, 1};
        }
        if (n.contains("bigaphasit")) {
            return new int[]{2, 2};
        }
        // Keep decoration placement bound to exactly one tile so click-to-place
        // always matches a single map cell.
        return new int[]{1, 1};
    }
}
