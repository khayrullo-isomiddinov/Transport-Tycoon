package com.team404.tycoon.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
        state.setBalance(EconomyConfig.INITIAL_CAPITAL);

        paintWater(map, rng);
        paintForests(map, rng);
        generateHeights(map, rng);
        generateTowns(state, map, rng);
        // Keep starter simulation alive by ensuring towns have a connected backbone.
        connectTownRoadNetwork(state, map);
        decorateAllRoadTiles(state, map, rng);
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

    /**
     * Assigns terrain heights to all map tiles.
     * Water stays at height 0. A handful of mountain blobs raise tiles to
     * height 2 (foothills) and height 3 (peaks). Everything else is height 1 (flat).
     * Town areas are later flattened back to 1 so they are always reachable.
     */
    private static void generateHeights(GameMap map, Random rng) {
        // Water tiles are valleys (height 0).
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.getTile(x, y).getType() == TileType.WATER) {
                    map.getTile(x, y).setHeight(0);
                }
            }
        }

        // Only 1-2 small mountain blobs, placed near map edges so they don't
        // interfere with roads or towns in the playable center area.
        int mountains = 1 + rng.nextInt(2);
        int edgeMargin = Math.max(4, Math.min(map.getWidth(), map.getHeight()) / 5);
        for (int i = 0; i < mountains; i++) {
            int cx, cy;
            switch (rng.nextInt(4)) {
                case 0:  cx = rng.nextInt(map.getWidth());                       cy = rng.nextInt(edgeMargin); break;
                case 1:  cx = rng.nextInt(map.getWidth());                       cy = map.getHeight() - 1 - rng.nextInt(edgeMargin); break;
                case 2:  cx = rng.nextInt(edgeMargin);                           cy = rng.nextInt(map.getHeight()); break;
                default: cx = map.getWidth() - 1 - rng.nextInt(edgeMargin);     cy = rng.nextInt(map.getHeight()); break;
            }
            int outerRadius = 2 + rng.nextInt(3);
            int innerRadius = Math.max(1, outerRadius / 2);
            for (int ty = cy - outerRadius; ty <= cy + outerRadius; ty++) {
                for (int tx = cx - outerRadius; tx <= cx + outerRadius; tx++) {
                    if (!map.isInBounds(tx, ty)) {
                        continue;
                    }
                    if (map.getTile(tx, ty).getType() == TileType.WATER) {
                        continue;
                    }
                    int dx = tx - cx;
                    int dy = ty - cy;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d <= innerRadius) {
                        map.getTile(tx, ty).setHeight(3);
                    } else if (d <= outerRadius) {
                        // Only raise to foothills if not already a peak.
                        if (map.getTile(tx, ty).getHeight() < 2) {
                            map.getTile(tx, ty).setHeight(2);
                        }
                    }
                }
            }
        }
    }

    /** Sets every tile in a rectangular area to the given height (if in bounds). */
    private static void flattenAreaToHeight(GameMap map, int cx, int cy, int radius, int height) {
        for (int y = cy - radius; y <= cy + radius; y++) {
            for (int x = cx - radius; x <= cx + radius; x++) {
                if (map.isInBounds(x, y)) {
                    map.getTile(x, y).setHeight(height);
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
        // Scale town count with map size: small maps 4-6, large maps up to 16-18.
        int baseTowns = Math.min(16, 3 + map.getWidth() / 50);
        int townCount = baseTowns + rng.nextInt(3);
        for (int i = 0; i < townCount; i++) {
            int cx = 10 + rng.nextInt(Math.max(1, map.getWidth() - 20));
            int cy = 10 + rng.nextInt(Math.max(1, map.getHeight() - 20));
            int[] relocated = relocateOffWater(map, cx, cy, 8);
            cx = relocated[0];
            cy = relocated[1];
            int half = 3 + rng.nextInt(3); // smaller towns: roughly 7..11
            int block = 2; // cleaner, denser roads
            layTownRoadGrid(map, rng, cx, cy, half, block);
            // Flatten the town footprint so players can always reach it from flat terrain.
            flattenAreaToHeight(map, cx, cy, half + 2, 1);
            // Ensure the "town center" is always on land and driveable.
            paintRoadTile(map, cx, cy);
            placeTownBuildings(state, map, rng, cx, cy, half);
            // Population scales with town size; half is 3–5, giving ~400–900 starting pop.
            int initialPop = 200 + half * 80 + rng.nextInt(200);
            state.addTown(new Town(generateTownName(rng), cx, cy, initialPop));
        }
    }

    private static void connectTownRoadNetwork(GameState state, GameMap map) {
        List<Town> towns = state.getTowns();
        if (towns.size() < 2) {
            return;
        }
        for (int i = 0; i < towns.size() - 1; i++) {
            Town a = towns.get(i);
            Town b = towns.get(i + 1);
            connectTownsWithHighway(map, a.getCenterX(), a.getCenterY(), b.getCenterX(), b.getCenterY());
        }
        if (towns.size() > 2) {
            Town first = towns.get(0);
            Town last  = towns.get(towns.size() - 1);
            connectTownsWithHighway(map, first.getCenterX(), first.getCenterY(), last.getCenterX(), last.getCenterY());
        }
    }

    /** BFS road between two towns, avoiding water. Only sets TileType.ROAD — decoration happens later. */
    private static void connectTownsWithHighway(GameMap map, int sx, int sy, int ex, int ey) {
        if (sx == ex && sy == ey) {
            return;
        }
        List<int[]> path = findPathAvoidingWater(map, sx, sy, ex, ey);
        if (path.isEmpty()) {
            // Fallback: carve a direct L-shaped corridor when water blocks BFS connectivity.
            path = buildDirectPath(sx, sy, ex, ey);
        }
        for (int[] tile : path) {
            forceRoadTile(map, tile[0], tile[1]);
        }
    }

    /** BFS from (sx, sy) to (ex, ey) that never steps on water. Returns the tile path, or empty if unreachable. */
    private static List<int[]> findPathAvoidingWater(GameMap map, int sx, int sy, int ex, int ey) {
        int w = map.getWidth();
        int h = map.getHeight();
        int[][] prevX = new int[h][w];
        int[][] prevY = new int[h][w];
        boolean[][] visited = new boolean[h][w];
        for (int[] row : prevX) {
            java.util.Arrays.fill(row, -1);
        }
        visited[sy][sx] = true;
        prevX[sy][sx] = sx;
        prevY[sy][sx] = sy;
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sx, sy});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean found = false;
        outer:
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            for (int[] d : dirs) {
                int nx = cur[0] + d[0];
                int ny = cur[1] + d[1];
                if (!map.isInBounds(nx, ny) || visited[ny][nx]) {
                    continue;
                }
                if (map.getTile(nx, ny).getType() == TileType.WATER) {
                    continue;
                }
                visited[ny][nx] = true;
                prevX[ny][nx] = cur[0];
                prevY[ny][nx] = cur[1];
                if (nx == ex && ny == ey) {
                    found = true;
                    break outer;
                }
                queue.add(new int[]{nx, ny});
            }
        }
        if (!found) {
            return Collections.emptyList();
        }
        List<int[]> path = new ArrayList<>();
        int cx = ex;
        int cy = ey;
        while (cx != sx || cy != sy) {
            path.add(new int[]{cx, cy});
            int px = prevX[cy][cx];
            int py = prevY[cy][cx];
            cx = px;
            cy = py;
        }
        path.add(new int[]{sx, sy});
        Collections.reverse(path);
        return path;
    }

    private static List<int[]> buildDirectPath(int sx, int sy, int ex, int ey) {
        List<int[]> path = new ArrayList<>();
        int x = sx;
        int y = sy;
        path.add(new int[]{x, y});
        while (x != ex) {
            x += Integer.compare(ex, x);
            path.add(new int[]{x, y});
        }
        while (y != ey) {
            y += Integer.compare(ey, y);
            path.add(new int[]{x, y});
        }
        return path;
    }

    private static void forceRoadTile(GameMap map, int x, int y) {
        if (!map.isInBounds(x, y)) {
            return;
        }
        map.getTile(x, y).setType(TileType.ROAD);
        if (map.getTile(x, y).getHeight() == 0) {
            map.getTile(x, y).setHeight(1);
        }
    }

    private static int[] relocateOffWater(GameMap map, int cx, int cy, int maxRadius) {
        if (map.isInBounds(cx, cy) && map.getTile(cx, cy).getType() != TileType.WATER) {
            return new int[]{cx, cy};
        }
        for (int radius = 1; radius <= maxRadius; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) > radius) {
                        continue;
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    if (!map.isInBounds(x, y)) {
                        continue;
                    }
                    if (map.getTile(x, y).getType() != TileType.WATER) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        // Fallback: clamp inside bounds; later town logic will avoid water where possible.
        int x = Math.max(0, Math.min(map.getWidth() - 1, cx));
        int y = Math.max(0, Math.min(map.getHeight() - 1, cy));
        return new int[]{x, y};
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

    /** Iteratively removes road tiles with 0 or 1 road neighbour until the network is clean. */
    private static void pruneDeadEndRoads(GameMap map) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int y = 0; y < map.getHeight(); y++) {
                for (int x = 0; x < map.getWidth(); x++) {
                    if (map.getTile(x, y).getType() != TileType.ROAD) {
                        continue;
                    }
                    int neighbors = (isRoad(map, x, y + 1) ? 1 : 0)
                            + (isRoad(map, x, y - 1) ? 1 : 0)
                            + (isRoad(map, x + 1, y) ? 1 : 0)
                            + (isRoad(map, x - 1, y) ? 1 : 0);
                    if (neighbors <= 1) {
                        map.getTile(x, y).setType(TileType.EMPTY);
                        changed = true;
                    }
                }
            }
        }
    }

    /** Single global decoration pass — runs after the full road network (towns + inter-town) is laid. */
    private static void decorateAllRoadTiles(GameState state, GameMap map, Random rng) {
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.getTile(x, y).getType() != TileType.ROAD) {
                    continue;
                }
                boolean up    = isRoad(map, x, y + 1);
                boolean down  = isRoad(map, x, y - 1);
                boolean left  = isRoad(map, x - 1, y);
                boolean right = isRoad(map, x + 1, y);
                int neighbors = (up ? 1 : 0) + (down ? 1 : 0) + (left ? 1 : 0) + (right ? 1 : 0);

                String path;
                if (neighbors >= 4) {
                    path = rng.nextBoolean() ? "resources/trafficlights.png" : "resources/+.png";
                } else if (neighbors == 3 && up && down && right) {
                    path = "resources/to-right.png";
                } else if (neighbors == 3 && up && down && left) {
                    path = "resources/to-left.png";
                } else if (neighbors == 3 && left && right && up) {
                    path = "resources/to-up.png";
                } else if (neighbors == 3 && left && right && down) {
                    path = "resources/to-down.png";
                } else if (neighbors == 3) {
                    path = "resources/trafficlights.png";
                } else if (right && down) {
                    path = "resources/up-and-right.png";
                } else if (down && left) {
                    path = "resources/right-and-down.png";
                } else if (left && up) {
                    path = "resources/down-and-left.png";
                } else if (up && right) {
                    path = "resources/left-and-up.png";
                } else if (left || right) {
                    path = "resources/highway-straight.png";
                } else {
                    path = "resources/highway-top-left.png";
                }
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
        placeOnTypes(state, map, rng, new TileType[]{TileType.FOREST}, "resources/tree.png", 600, 0.55f);
    }

    private static boolean nearRoad(GameMap map, int x, int y) {
        return isRoad(map, x + 1, y)
                || isRoad(map, x - 1, y)
                || isRoad(map, x, y + 1)
                || isRoad(map, x, y - 1);
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
