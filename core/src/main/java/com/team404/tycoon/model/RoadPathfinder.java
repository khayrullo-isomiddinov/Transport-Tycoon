package com.team404.tycoon.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Finds driveable road paths between towns.
 */
public final class RoadPathfinder {

    private RoadPathfinder() {
    }

    public static List<int[]> findRoadPath(GameState state, Town fromTown, Town toTown, int searchRadius) {
        int[] start = findNearestRoadTile(state, fromTown.getCenterX(), fromTown.getCenterY(), searchRadius);
        int[] goal = findNearestRoadTile(state, toTown.getCenterX(), toTown.getCenterY(), searchRadius);
        if (start == null || goal == null) {
            return Collections.emptyList();
        }
        int width = state.getMap().getWidth();
        int height = state.getMap().getHeight();
        boolean[][] visited = new boolean[height][width];
        int[][] prevX = new int[height][width];
        int[][] prevY = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                prevX[y][x] = -1;
                prevY[y][x] = -1;
            }
        }

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(start);
        visited[start[1]][start[0]] = true;
        int[][] dirs = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] tile = queue.removeFirst();
            if (tile[0] == goal[0] && tile[1] == goal[1]) {
                return rebuildPath(prevX, prevY, start, goal);
            }
            for (int[] d : dirs) {
                int nx = tile[0] + d[0];
                int ny = tile[1] + d[1];
                if (!state.getMap().isInBounds(nx, ny) || visited[ny][nx]) {
                    continue;
                }
                if (!state.isDriveableRoadTile(nx, ny)) {
                    continue;
                }
                visited[ny][nx] = true;
                prevX[ny][nx] = tile[0];
                prevY[ny][nx] = tile[1];
                queue.addLast(new int[]{nx, ny});
            }
        }
        return Collections.emptyList();
    }

    public static int[] findNearestRoadTile(GameState state, int cx, int cy, int maxRadius) {
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) > radius) {
                        continue;
                    }
                    int x = cx + dx;
                    int y = cy + dy;
                    if (!state.getMap().isInBounds(x, y)) {
                        continue;
                    }
                    if (state.isDriveableRoadTile(x, y)) {
                        return new int[]{x, y};
                    }
                }
            }
        }
        return null;
    }

    private static List<int[]> rebuildPath(int[][] prevX, int[][] prevY, int[] start, int[] goal) {
        List<int[]> path = new ArrayList<>();
        int cx = goal[0];
        int cy = goal[1];
        while (cx != -1 && cy != -1) {
            path.add(new int[]{cx, cy});
            if (cx == start[0] && cy == start[1]) {
                break;
            }
            int nx = prevX[cy][cx];
            int ny = prevY[cy][cx];
            cx = nx;
            cy = ny;
        }
        Collections.reverse(path);
        if (path.isEmpty()) {
            return Collections.emptyList();
        }
        int[] first = path.get(0);
        if (first[0] != start[0] || first[1] != start[1]) {
            return Collections.emptyList();
        }
        return path;
    }
}
