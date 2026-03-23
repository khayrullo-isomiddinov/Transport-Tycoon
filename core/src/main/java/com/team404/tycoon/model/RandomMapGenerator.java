package com.team404.tycoon.model;

import java.util.Random;

/**
 * Simple one-shot terrain generator for startup maps.
 */
public final class RandomMapGenerator {

    private RandomMapGenerator() {
    }

    public static void generate(GameState state, long seed) {
        Random rng = new Random(seed);
        GameMap map = state.getMap();
        map.fill(TileType.EMPTY);

        paintWater(map, rng);
        paintForests(map, rng);
        paintCityZones(map, rng);
        paintIndustryZones(map, rng);
        paintRoadNetwork(map, rng);
        paintSpriteDecorations(state, map, rng);
    }

    private static void paintWater(GameMap map, Random rng) {
        int blobs = Math.max(4, (map.getWidth() * map.getHeight()) / 700);
        for (int i = 0; i < blobs; i++) {
            int cx = rng.nextInt(map.getWidth());
            int cy = rng.nextInt(map.getHeight());
            int rx = 2 + rng.nextInt(6);
            int ry = 2 + rng.nextInt(6);
            for (int y = cy - ry; y <= cy + ry; y++) {
                for (int x = cx - rx; x <= cx + rx; x++) {
                    if (!map.isInBounds(x, y)) {
                        continue;
                    }
                    float nx = (x - cx) / (float) rx;
                    float ny = (y - cy) / (float) ry;
                    float d = nx * nx + ny * ny;
                    if (d <= 1.0f + rng.nextFloat() * 0.20f) {
                        map.getTile(x, y).setType(TileType.WATER);
                    }
                }
            }
        }
    }

    private static void paintForests(GameMap map, Random rng) {
        int blobs = Math.max(8, (map.getWidth() * map.getHeight()) / 400);
        for (int i = 0; i < blobs; i++) {
            int cx = rng.nextInt(map.getWidth());
            int cy = rng.nextInt(map.getHeight());
            int radius = 2 + rng.nextInt(5);
            paintNoisyCircle(map, rng, cx, cy, radius, TileType.FOREST);
        }
    }

    private static void paintCityZones(GameMap map, Random rng) {
        int blobs = Math.max(5, (map.getWidth() * map.getHeight()) / 550);
        for (int i = 0; i < blobs; i++) {
            int cx = rng.nextInt(map.getWidth());
            int cy = rng.nextInt(map.getHeight());
            int radius = 2 + rng.nextInt(4);
            paintNoisyCircle(map, rng, cx, cy, radius, TileType.CITY);
        }
    }

    private static void paintIndustryZones(GameMap map, Random rng) {
        int blobs = Math.max(3, (map.getWidth() * map.getHeight()) / 900);
        for (int i = 0; i < blobs; i++) {
            int cx = rng.nextInt(map.getWidth());
            int cy = rng.nextInt(map.getHeight());
            int radius = 1 + rng.nextInt(3);
            paintNoisyCircle(map, rng, cx, cy, radius, TileType.INDUSTRIAL_FACILITY);
        }
    }

    private static void paintNoisyCircle(GameMap map, Random rng, int cx, int cy, int radius, TileType type) {
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                if (!map.isInBounds(x, y)) {
                    continue;
                }
                if (map.getTile(x, y).getType() == TileType.WATER) {
                    continue;
                }
                int dx = x - cx;
                int dy = y - cy;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d <= radius + (rng.nextFloat() - 0.5f) * 1.0f) {
                    map.getTile(x, y).setType(type);
                }
            }
        }
    }

    private static void paintRoadNetwork(GameMap map, Random rng) {
        int anchors = Math.max(6, (map.getWidth() + map.getHeight()) / 20);
        int[][] points = new int[anchors][2];
        for (int i = 0; i < anchors; i++) {
            points[i][0] = rng.nextInt(map.getWidth());
            points[i][1] = rng.nextInt(map.getHeight());
        }

        // Connect each anchor to the next in a Manhattan path, forming a simple network.
        for (int i = 0; i < anchors - 1; i++) {
            carveRoad(map, points[i][0], points[i][1], points[i + 1][0], points[i + 1][1]);
        }
    }

    private static void carveRoad(GameMap map, int x1, int y1, int x2, int y2) {
        int x = x1;
        int y = y1;
        while (x != x2) {
            paintRoadTile(map, x, y);
            x += Integer.compare(x2, x);
        }
        while (y != y2) {
            paintRoadTile(map, x, y);
            y += Integer.compare(y2, y);
        }
        paintRoadTile(map, x, y);
    }

    private static void paintRoadTile(GameMap map, int x, int y) {
        if (!map.isInBounds(x, y)) {
            return;
        }
        if (map.getTile(x, y).getType() == TileType.WATER) {
            return;
        }
        map.getTile(x, y).setType(TileType.ROAD);
    }

    private static void paintSpriteDecorations(GameState state, GameMap map, Random rng) {
        // Use a handful of user sprites as startup world dressing.
        placeAlongRoads(state, map, rng, "resources/highway-straight.png", 180, 0.68f);
        placeAlongRoads(state, map, rng, "resources/highway-top-left.png", 120, 0.52f);
        placeAlongRoads(state, map, rng, "resources/intersection.png", 70, 0.38f);

        // Be generous with buildings in city-like regions.
        TileType[] urban = new TileType[]{TileType.CITY, TileType.INDUSTRIAL_FACILITY, TileType.ROAD};
        placeOnTypes(state, map, rng, urban, "resources/building1.png", 150, 0.50f);
        placeOnTypes(state, map, rng, urban, "resources/building2.png", 140, 0.46f);
        placeOnTypes(state, map, rng, urban, "resources/building3.png", 130, 0.42f);
        placeOnTypes(state, map, rng, urban, "resources/building4.png", 120, 0.40f);
        placeOnTypes(state, map, rng, urban, "resources/building5.png", 115, 0.38f);
        placeOnTypes(state, map, rng, urban, "resources/building6.png", 105, 0.35f);
        placeOnTypes(state, map, rng, urban, "resources/building7.png", 95, 0.33f);
        placeOnTypes(state, map, rng, urban, "resources/building8.png", 90, 0.31f);
        placeOnTypes(state, map, rng, urban, "resources/garage.png", 85, 0.30f);
        placeOnTypes(state, map, rng, urban, "resources/garage1.png", 80, 0.28f);
        placeOnTypes(state, map, rng, urban, "resources/villagehouse.png", 120, 0.34f);
        placeOnTypes(state, map, rng, urban, "resources/villagehousewithgrass.png", 100, 0.30f);
        placeOnTypes(state, map, rng, urban, "resources/villagehousewithgrass2.png", 95, 0.28f);
        placeOnTypes(state, map, rng, urban, "resources/somegreybuilding.png", 95, 0.30f);

        placeOnTypes(state, map, rng, new TileType[]{TileType.FOREST, TileType.EMPTY}, "resources/tree.png", 140, 0.12f);
    }

    private static void placeAlongRoads(
            GameState state, GameMap map, Random rng, String path, int attempts, float probability) {
        for (int i = 0; i < attempts; i++) {
            int x = rng.nextInt(map.getWidth());
            int y = rng.nextInt(map.getHeight());
            if (map.getTile(x, y).getType() != TileType.ROAD || rng.nextFloat() > probability) {
                continue;
            }
            tryPlaceDecoration(state, map, x, y, path);
        }
    }

    private static void placeOnTypes(
            GameState state,
            GameMap map,
            Random rng,
            TileType[] allowed,
            String path,
            int attempts,
            float probability) {
        for (int i = 0; i < attempts; i++) {
            int x = rng.nextInt(map.getWidth());
            int y = rng.nextInt(map.getHeight());
            TileType t = map.getTile(x, y).getType();
            if (!contains(allowed, t) || rng.nextFloat() > probability) {
                continue;
            }
            tryPlaceDecoration(state, map, x, y, path);
        }
    }

    private static void tryPlaceDecoration(GameState state, GameMap map, int x, int y, String path) {
        int[] fp = DecorationRules.footprintForPath(path);
        PlacedDecoration d = new PlacedDecoration(x, y, path, fp[0], fp[1]);
        if (state.canPlaceDecoration(map, d)) {
            state.addDecoration(d);
        }
    }

    private static boolean contains(TileType[] arr, TileType t) {
        for (TileType value : arr) {
            if (value == t) {
                return true;
            }
        }
        return false;
    }
}
