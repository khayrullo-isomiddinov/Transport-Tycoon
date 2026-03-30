package com.team404.tycoon.model;

import java.util.ArrayList;
import java.util.List;
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
        state.clearTowns();
        state.clearTransportState();

        paintWater(map, rng);
        paintForests(map, rng);
        generateTowns(state, map, rng);
        sprinkleNatureDecor(state, map, rng);
        state.bootstrapStarterTransport();
        state.seedRandomDemand(2, 6, rng);
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

    private static void paintRoadTile(GameMap map, int x, int y) {
        if (!map.isInBounds(x, y)) {
            return;
        }
        if (map.getTile(x, y).getType() == TileType.WATER) {
            return;
        }
        map.getTile(x, y).setType(TileType.ROAD);
    }

    private static void generateTowns(GameState state, GameMap map, Random rng) {
        int townCount = 3 + rng.nextInt(3); // 3..5 towns
        for (int i = 0; i < townCount; i++) {
            int cx = 10 + rng.nextInt(Math.max(1, map.getWidth() - 20));
            int cy = 10 + rng.nextInt(Math.max(1, map.getHeight() - 20));
            int half = 3 + rng.nextInt(3); // smaller towns: roughly 7..11
            int block = 2; // cleaner, denser roads
            layTownRoadGrid(map, rng, cx, cy, half, block);
            placeTownBuildings(state, map, rng, cx, cy, half);
            decorateTownRoadTiles(state, map, rng, cx, cy, half);
            state.addTown(new Town(generateTownName(rng), cx, cy));
        }
    }

    private static void layTownRoadGrid(GameMap map, Random rng, int cx, int cy, int half, int blockSize) {
        int minX = cx - half;
        int maxX = cx + half;
        int minY = cy - half;
        int maxY = cy + half;
        float radialScaleX = 0.98f + rng.nextFloat() * 0.08f;
        float radialScaleY = 0.98f + rng.nextFloat() * 0.08f;
        int xOffset = 0;
        int yOffset = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!map.isInBounds(x, y)) {
                    continue;
                }
                if (map.getTile(x, y).getType() == TileType.WATER) {
                    continue;
                }
                float nx = (x - cx) / (half * radialScaleX);
                float ny = (y - cy) / (half * radialScaleY);
                float radial = nx * nx + ny * ny;
                if (radial > 1.12f + (rng.nextFloat() - 0.5f) * 0.20f) {
                    continue;
                }

                int dx = Math.abs(x - cx + xOffset);
                int dy = Math.abs(y - cy + yOffset);
                boolean majorRoad = dx % blockSize == 0 || dy % blockSize == 0;
                if (majorRoad) {
                    paintRoadTile(map, x, y);
                } else {
                    map.getTile(x, y).setType(TileType.CITY);
                }
            }
        }

        // Keep this generator clean and compact: no wobbly connectors.
    }

    private static void placeTownBuildings(GameState state, GameMap map, Random rng, int cx, int cy, int half) {
        String[] buildingPool = new String[]{
                "resources/building1.png",
                "resources/building2.png",
                "resources/building3.png",
                "resources/building4.png",
                "resources/building5.png",
                "resources/building6.png",
                "resources/building7.png",
                "resources/building8.png",
                "resources/garage.png",
                "resources/garage1.png",
                "resources/villagehouse.png",
                "resources/villagehousewithgrass.png",
                "resources/villagehousewithgrass2.png",
                "resources/somegreybuilding.png"
        };
        int minX = cx - half;
        int maxX = cx + half;
        int minY = cy - half;
        int maxY = cy + half;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!map.isInBounds(x, y)) {
                    continue;
                }
                if (map.getTile(x, y).getType() != TileType.CITY) {
                    continue;
                }
                float centerDist = (float) Math.hypot(x - cx, y - cy) / Math.max(1f, half);
                float nearRoadBoost = nearRoad(map, x, y) ? 0.24f : 0f;
                float density = 0.90f - centerDist * 0.16f + nearRoadBoost;
                if (rng.nextFloat() > density) {
                    continue;
                }
                String path = buildingPool[rng.nextInt(buildingPool.length)];
                tryPlaceDecoration(state, map, x, y, path);
            }
        }
    }

    private static void decorateTownRoadTiles(GameState state, GameMap map, Random rng, int cx, int cy, int half) {
        int minX = cx - half;
        int maxX = cx + half;
        int minY = cy - half;
        int maxY = cy + half;
        List<int[]> roadCells = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (!map.isInBounds(x, y)) {
                    continue;
                }
                if (map.getTile(x, y).getType() == TileType.ROAD) {
                    roadCells.add(new int[]{x, y});
                }
            }
        }
        for (int[] c : roadCells) {
            int x = c[0];
            int y = c[1];
            boolean up = isRoad(map, x, y + 1);
            boolean down = isRoad(map, x, y - 1);
            boolean left = isRoad(map, x - 1, y);
            boolean right = isRoad(map, x + 1, y);

            int neighbors = (up ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0) + (right ? 1 : 0);
            String path = null;

            // 4-way node => traffic lights.
            if (neighbors >= 4) {
                path = "resources/trafficlights.png";
            } else if (neighbors == 2 && !((up && down) || (left && right))) {
                // 90-degree corner => intersection marker.
                path = "resources/intersection.png";
            } else if (left && right && !up && !down) {
                path = "resources/highway-straight.png";
            } else if (up && down && !left && !right) {
                path = "resources/highway-top-left.png";
            } else if (neighbors == 3) {
                // 3-way highways can also use traffic lights (not always).
                boolean hasStraightMain = (left && right) || (up && down);
                float chance = hasStraightMain ? 0.35f : 0.22f;
                if (rng.nextFloat() < chance) {
                    path = "resources/trafficlights.png";
                }
            }

            if (path != null) {
                tryPlaceDecoration(state, map, x, y, path);
            }
        }
    }

    private static boolean isRoad(GameMap map, int x, int y) {
        return map.isInBounds(x, y) && map.getTile(x, y).getType() == TileType.ROAD;
    }

    private static void sprinkleNatureDecor(GameState state, GameMap map, Random rng) {
        // Expand forest coverage outside dense city roads, then place many tree sprites.
        int extraForestBlobs = Math.max(10, (map.getWidth() * map.getHeight()) / 260);
        for (int i = 0; i < extraForestBlobs; i++) {
            int cx = rng.nextInt(map.getWidth());
            int cy = rng.nextInt(map.getHeight());
            if (map.getTile(cx, cy).getType() == TileType.ROAD || map.getTile(cx, cy).getType() == TileType.CITY) {
                continue;
            }
            int radius = 2 + rng.nextInt(6);
            paintNoisyCircle(map, rng, cx, cy, radius, TileType.FOREST);
        }
        placeOnTypes(state, map, rng, new TileType[]{TileType.FOREST, TileType.EMPTY}, "resources/tree.png", 210, 0.16f);
    }

    private static boolean nearRoad(GameMap map, int x, int y) {
        return isRoad(map, x + 1, y)
                || isRoad(map, x - 1, y)
                || isRoad(map, x, y + 1)
                || isRoad(map, x, y - 1);
    }

    private static void carveWobblyRoad(GameMap map, Random rng, int sx, int sy, int ex, int ey, int wiggleStep) {
        int x = sx;
        int y = sy;
        int counter = 0;
        while (x != ex || y != ey) {
            paintRoadTile(map, x, y);
            if (counter++ % wiggleStep == 0 && rng.nextFloat() < 0.45f) {
                if (rng.nextBoolean() && x != ex) {
                    y += rng.nextBoolean() ? 1 : -1;
                } else if (y != ey) {
                    x += rng.nextBoolean() ? 1 : -1;
                }
            } else {
                if (Math.abs(ex - x) >= Math.abs(ey - y)) {
                    x += Integer.compare(ex, x);
                } else {
                    y += Integer.compare(ey, y);
                }
            }
            if (!map.isInBounds(x, y)) {
                break;
            }
        }
        paintRoadTile(map, ex, ey);
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

    private static String generateTownName(Random rng) {
        String[] prefix = {"Fran", "North", "South", "East", "West", "Oak", "River", "Stone", "Green", "Bright"};
        String[] suffix = {"field", "ford", "ton", "ville", "bridge", "brook", "crest", "port", "view", "dale"};
        String n = prefix[rng.nextInt(prefix.length)] + suffix[rng.nextInt(suffix.length)];
        if (rng.nextFloat() < 0.15f) {
            n += " " + (1 + rng.nextInt(9));
        }
        return n;
    }
}
