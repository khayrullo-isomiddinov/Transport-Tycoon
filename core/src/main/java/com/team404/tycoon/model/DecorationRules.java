package com.team404.tycoon.model;

/**
 * Footprint heuristics for PNG decorations (tile grid, axis-aligned).
 */
public final class DecorationRules {

    private DecorationRules() {
    }

    public static int[] footprintForPath(String resourcePath) {
        String n = resourcePath.toLowerCase();
        if (n.contains("building")
                || n.contains("village")
                || n.contains("garage")
                || n.contains("teplitsa")
                || n.contains("playground")
                || n.contains("somegreybuilding")) {
            return new int[]{2, 2};
        }
        if (n.contains("tree")) {
            return new int[]{1, 2};
        }
        return new int[]{1, 1};
    }
}
