package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.team404.tycoon.controller.GameController;
import com.team404.tycoon.desktop.assets.DecorationMetadata;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.RoadPathfinder;
import com.team404.tycoon.model.Route;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;
import com.team404.tycoon.model.Town;
import com.team404.tycoon.model.Shipment;
import com.team404.tycoon.model.TransportContentType;
import com.team404.tycoon.model.TransportDemand;
import com.team404.tycoon.model.Vehicle;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Renderer2D implements GameRenderer {

    public static final float TILE_W = 64f;
    public static final float TILE_H = 32f;
    private static final float EDGE_DEPTH = 16f;
    private static final Color EDGE_LEFT = new Color(0.15f, 0.30f, 0.10f, 1f);
    private static final Color EDGE_RIGHT = new Color(0.10f, 0.22f, 0.08f, 1f);
    private static final Color GRID_COLOR = new Color(0f, 0f, 0f, 0.12f);
    private static final Color HIGHLIGHT_COLOR = new Color(1f, 1f, 0.3f, 0.85f);

    private static final float CAMERA_SPEED = 400f;
    private static final float ZOOM_SPEED = 0.1f;
    private static final float MIN_ZOOM = 0.3f;
    private static final float MAX_ZOOM = 2f;

    private final OrthographicCamera camera;
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout;
    private final DecorationTextureCache textureCache;
    private float waterAnimTime;

    private int hoverTileX = -1;
    private int hoverTileY = -1;
    private boolean buildPreviewActive;
    private BuildPreviewMode buildPreviewMode = BuildPreviewMode.AREA;
    private int buildPreviewStartX;
    private int buildPreviewStartY;
    private int buildPreviewEndX;
    private int buildPreviewEndY;

    public Renderer2D(DecorationTextureCache textureCache) {
        this.camera = new OrthographicCamera();
        this.shape = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.glyphLayout = new GlyphLayout();
        this.textureCache = textureCache;
    }

    public void setHoverTile(int tileX, int tileY) {
        this.hoverTileX = tileX;
        this.hoverTileY = tileY;
    }

    public void setBuildPreview(
            int startTileX,
            int startTileY,
            int endTileX,
            int endTileY,
            BuildPreviewMode mode) {
        this.buildPreviewActive = true;
        this.buildPreviewMode = mode;
        this.buildPreviewStartX = startTileX;
        this.buildPreviewStartY = startTileY;
        this.buildPreviewEndX = endTileX;
        this.buildPreviewEndY = endTileY;
    }

    public void clearBuildPreview() {
        this.buildPreviewActive = false;
    }

    @Override
    public void render(GameController controller, float delta) {
        handleCameraInput(delta);
        waterAnimTime += delta;

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shape.setProjectionMatrix(camera.combined);

        GameState state = controller.getGameState();
        GameMap map = state.getMap();
        drawEdgeWalls(map);
        drawTileSurface(map);
        drawGridLines(map);
        drawDecorations(state);
        drawVehicles(state);
        drawTownNames(state);
        drawBuildPreview(map);
        drawHoverHighlight(map);
    }

    private void drawVehicles(GameState state) {
        if (state.getVehicles().isEmpty()) {
            return;
        }
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (Vehicle vehicle : state.getVehicles()) {
            float[] pos = getVehicleScreenPos(state, vehicle);
            if (pos == null) {
                continue;
            }
            shape.setColor(vehicleColor(vehicle));
            shape.circle(pos[0], pos[1], 10f);
        }
        shape.end();
    }

    private float[] getVehicleScreenPos(GameState state, Vehicle vehicle) {
        Route route = state.findRouteById(vehicle.getRouteId()).orElse(null);
        if (route == null || route.getStops().size() < 2) {
            return null;
        }
        int stopCount = route.getStops().size();
        int fromIndex = Math.floorMod(vehicle.getCurrentStopIndex(), stopCount);
        int toIndex = (fromIndex + 1) % stopCount;
        Town fromTown = state.getTown(route.getStops().get(fromIndex).getTownIndex()).orElse(null);
        Town toTown = state.getTown(route.getStops().get(toIndex).getTownIndex()).orElse(null);
        if (fromTown == null || toTown == null) {
            return null;
        }
        List<int[]> roadPath = RoadPathfinder.findRoadPath(state, fromTown, toTown, 6);
        float[] tilePos;
        if (roadPath.isEmpty()) {
            int[] nearest = RoadPathfinder.findNearestRoadTile(
                    state, fromTown.getCenterX(), fromTown.getCenterY(), 6);
            if (nearest == null) {
                return null;
            }
            tilePos = new float[]{nearest[0], nearest[1]};
        } else {
            tilePos = interpolateOnRoadPath(roadPath, vehicle.getLegProgressTiles());
        }
        float sx = toScreenX(tilePos[0], tilePos[1]);
        float sy = toScreenY(tilePos[0], tilePos[1]) + TILE_H * 1.35f;
        return new float[]{sx, sy};
    }

    private static float[] interpolateOnRoadPath(List<int[]> roadPath, float legProgressTiles) {
        if (roadPath.size() == 1) {
            return new float[]{roadPath.get(0)[0], roadPath.get(0)[1]};
        }
        float clamped = Math.max(0f, Math.min(legProgressTiles, roadPath.size() - 1f));
        int segment = Math.min((int) Math.floor(clamped), roadPath.size() - 2);
        float localT = clamped - segment;
        int[] from = roadPath.get(segment);
        int[] to = roadPath.get(segment + 1);
        float x = from[0] + (to[0] - from[0]) * localT;
        float y = from[1] + (to[1] - from[1]) * localT;
        return new float[]{x, y};
    }

    private void drawTownNames(GameState state) {
        if (state.getTowns().isEmpty() && state.getVehicles().isEmpty()) {
            return;
        }
        batch.begin();
        batch.setProjectionMatrix(camera.combined);

        for (int i = 0; i < state.getTowns().size(); i++) {
            Town t = state.getTowns().get(i);
            float sx = toScreenX(t.getCenterX(), t.getCenterY());
            float sy = toScreenY(t.getCenterX(), t.getCenterY());
            String name = t.getName();
            glyphLayout.setText(font, name);
            float x = sx - glyphLayout.width * 0.5f;
            float y = sy + TILE_H * 1.8f;
            font.setColor(0f, 0f, 0f, 0.8f);
            font.draw(batch, name, x + 1f, y - 1f);
            font.setColor(0.95f, 0.95f, 0.95f, 1f);
            font.draw(batch, name, x, y);

            String demandLabel = buildDemandLabel(state, i);
            if (!demandLabel.isEmpty()) {
                glyphLayout.setText(font, demandLabel);
                float dx = sx - glyphLayout.width * 0.5f;
                float dy = y - 14f;
                font.setColor(0f, 0f, 0f, 0.7f);
                font.draw(batch, demandLabel, dx + 1f, dy - 1f);
                font.setColor(1f, 0.85f, 0.25f, 1f);
                font.draw(batch, demandLabel, dx, dy);
            }
        }

        for (Vehicle vehicle : state.getVehicles()) {
            float[] pos = getVehicleScreenPos(state, vehicle);
            if (pos == null) {
                continue;
            }
            String cargoLabel = buildCargoLabel(vehicle);
            if (cargoLabel.isEmpty()) {
                continue;
            }
            glyphLayout.setText(font, cargoLabel);
            float lx = pos[0] - glyphLayout.width * 0.5f;
            float ly = pos[1] + 14f;
            font.setColor(0f, 0f, 0f, 0.75f);
            font.draw(batch, cargoLabel, lx + 1f, ly - 1f);
            font.setColor(vehicleTextColor(vehicle));
            font.draw(batch, cargoLabel, lx, ly);
        }

        font.setColor(Color.WHITE);
        batch.end();
    }

    private static String buildDemandLabel(GameState state, int townIndex) {
        int passengers = 0;
        int goods = 0;
        for (TransportDemand demand : state.getTransportDemand()) {
            if (demand.getOriginTownIndex() == townIndex) {
                if (demand.getContentType() == TransportContentType.PASSENGERS) {
                    passengers += demand.getQuantity();
                } else {
                    goods += demand.getQuantity();
                }
            }
        }
        if (passengers == 0 && goods == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (passengers > 0) {
            sb.append(passengers).append("p");
        }
        if (goods > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(goods).append("g");
        }
        return sb.toString();
    }

    private static String buildCargoLabel(Vehicle vehicle) {
        int passengers = 0;
        int goods = 0;
        for (Shipment shipment : vehicle.getLoadedShipments()) {
            if (shipment.getContentType() == TransportContentType.PASSENGERS) {
                passengers += shipment.getQuantity();
            } else {
                goods += shipment.getQuantity();
            }
        }
        if (passengers == 0 && goods == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (passengers > 0) {
            sb.append(passengers).append("p");
        }
        if (goods > 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(goods).append("g");
        }
        return sb.toString();
    }

    private Color vehicleTextColor(Vehicle vehicle) {
        boolean p = vehicle.getType().supports(TransportContentType.PASSENGERS);
        boolean g = vehicle.getType().supports(TransportContentType.GOODS);
        if (p && g) {
            return new Color(1f, 0.9f, 0.2f, 1f);
        }
        if (p) {
            return new Color(0.4f, 0.95f, 1f, 1f);
        }
        return new Color(1f, 0.7f, 0.3f, 1f);
    }

    private void drawTileSurface(GameMap map) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = map.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(x, y);
                float sx = toScreenX(x, y);
                float sy = toScreenY(x, y);
                drawDiamond(sx, sy, colorFor(tile.getType(), x, y));
            }
        }
        shape.end();
    }

    private void drawDiamond(float sx, float sy, Color color) {
        float hw = TILE_W / 2f;
        float hh = TILE_H / 2f;
        float by = sy + hh;
        shape.setColor(color);
        shape.triangle(sx, by + TILE_H, sx - hw, by + hh, sx, by);
        shape.triangle(sx, by + TILE_H, sx + hw, by + hh, sx, by);
    }

    private void drawHoverHighlight(GameMap map) {
        if (!map.isInBounds(hoverTileX, hoverTileY)) {
            return;
        }

        float sx = toScreenX(hoverTileX, hoverTileY);
        float sy = toScreenY(hoverTileX, hoverTileY);
        float hw = TILE_W / 2f;
        float hh = TILE_H / 2f;
        float by = sy + hh;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        Gdx.gl.glLineWidth(2f);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(HIGHLIGHT_COLOR);
        shape.line(sx, by + TILE_H, sx - hw, by + hh);
        shape.line(sx - hw, by + hh, sx, by);
        shape.line(sx, by, sx + hw, by + hh);
        shape.line(sx + hw, by + hh, sx, by + TILE_H);
        shape.end();
        Gdx.gl.glLineWidth(1f);

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBuildPreview(GameMap map) {
        if (!buildPreviewActive) {
            return;
        }
        int minX = Math.min(buildPreviewStartX, buildPreviewEndX);
        int maxX = Math.max(buildPreviewStartX, buildPreviewEndX);
        int minY = Math.min(buildPreviewStartY, buildPreviewEndY);
        int maxY = Math.max(buildPreviewStartY, buildPreviewEndY);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(new Color(1f, 1f, 1f, 0.95f));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (buildPreviewMode == BuildPreviewMode.HORIZONTAL_LINE && y != buildPreviewStartY) {
                    continue;
                }
                if (buildPreviewMode == BuildPreviewMode.VERTICAL_LINE && x != buildPreviewStartX) {
                    continue;
                }
                if (!map.isInBounds(x, y)) {
                    continue;
                }
                float sx = toScreenX(x, y);
                float sy = toScreenY(x, y);
                float hw = TILE_W / 2f;
                float hh = TILE_H / 2f;
                float by = sy + hh;
                shape.line(sx, by + TILE_H, sx - hw, by + hh);
                shape.line(sx - hw, by + hh, sx, by);
                shape.line(sx, by, sx + hw, by + hh);
                shape.line(sx + hw, by + hh, sx, by + TILE_H);
            }
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (buildPreviewMode == BuildPreviewMode.HORIZONTAL_LINE && y != buildPreviewStartY) {
                    continue;
                }
                if (buildPreviewMode == BuildPreviewMode.VERTICAL_LINE && x != buildPreviewStartX) {
                    continue;
                }
                if (map.isInBounds(x, y)) {
                    count++;
                }
            }
        }
        if (count <= 0) {
            return;
        }
        String label = "Length: " + count;
        float sx = toScreenX(buildPreviewEndX, buildPreviewEndY);
        float sy = toScreenY(buildPreviewEndX, buildPreviewEndY);
        float labelX = sx + TILE_W * 0.4f;
        float labelY = sy + TILE_H * 1.15f;

        batch.begin();
        batch.setProjectionMatrix(camera.combined);
        font.setColor(Color.WHITE);
        glyphLayout.setText(font, label);
        font.draw(batch, label, labelX, labelY);
        batch.end();
    }

    public enum BuildPreviewMode {
        HORIZONTAL_LINE,
        VERTICAL_LINE,
        AREA
    }

    private void drawEdgeWalls(GameMap map) {
        int w = map.getWidth();
        int h = map.getHeight();

        shape.begin(ShapeRenderer.ShapeType.Filled);

        float hh = TILE_H / 2f;
        float hw = TILE_W / 2f;

        shape.setColor(EDGE_LEFT);
        for (int x = 0; x < w; x++) {
            float sx = toScreenX(x, 0);
            float by = toScreenY(x, 0) + hh;
            shape.triangle(sx, by, sx - hw, by + hh, sx - hw, by + hh - EDGE_DEPTH);
            shape.triangle(sx, by, sx - hw, by + hh - EDGE_DEPTH, sx, by - EDGE_DEPTH);
        }

        shape.setColor(EDGE_RIGHT);
        for (int y = 0; y < h; y++) {
            float sx = toScreenX(0, y);
            float by = toScreenY(0, y) + hh;
            shape.triangle(sx, by, sx + hw, by + hh, sx + hw, by + hh - EDGE_DEPTH);
            shape.triangle(sx, by, sx + hw, by + hh - EDGE_DEPTH, sx, by - EDGE_DEPTH);
        }

        float cx = toScreenX(0, 0);
        float cby = toScreenY(0, 0) + hh;
        shape.setColor(EDGE_RIGHT);
        shape.triangle(cx, cby, cx, cby - EDGE_DEPTH, cx - hw, cby + hh - EDGE_DEPTH);
        shape.triangle(cx, cby, cx + hw, cby + hh - EDGE_DEPTH, cx, cby - EDGE_DEPTH);

        shape.end();
    }

    private void drawGridLines(GameMap map) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(GRID_COLOR);

        int w = map.getWidth();
        int h = map.getHeight();

        for (int x = 0; x <= w; x++) {
            float x1 = toScreenX(x, 0);
            float y1 = toScreenY(x, 0) + TILE_H / 2f;
            float x2 = toScreenX(x, h);
            float y2 = toScreenY(x, h) + TILE_H / 2f;
            shape.line(x1, y1, x2, y2);
        }

        for (int y = 0; y <= h; y++) {
            float x1 = toScreenX(0, y);
            float y1 = toScreenY(0, y) + TILE_H / 2f;
            float x2 = toScreenX(w, y);
            float y2 = toScreenY(w, y) + TILE_H / 2f;
            shape.line(x1, y1, x2, y2);
        }

        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawDecorations(GameState state) {
        List<PlacedDecoration> list = new ArrayList<>(state.getDecorations());
        // Draw larger (x+y) first (back / top of screen), smaller last (front / bottom) so nearer sprites win.
        list.sort(Comparator
                .comparingInt(PlacedDecoration::depthSortKey).reversed()
                .thenComparingInt(PlacedDecoration::getAnchorTileX)
                .thenComparingInt(PlacedDecoration::getAnchorTileY));

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        batch.setProjectionMatrix(camera.combined);
        batch.setColor(Color.WHITE);

        for (PlacedDecoration d : list) {
            Texture tex = textureCache.get(d.getResourcePath());
            int fw = d.getFootprintTilesW();
            int fh = d.getFootprintTilesH();
            float drawW = TILE_W * (0.5f * (fw + fh));
            drawW *= DecorationMetadata.widthScale(d.getResourcePath());
            float aspect = (float) tex.getHeight() / (float) tex.getWidth();
            float vScale = DecorationMetadata.verticalOverlapScale(
                    d.getResourcePath(), fw, fh);
            float drawH = drawW * aspect * vScale;

            int ax = d.getAnchorTileX();
            int ay = d.getAnchorTileY();
            float sx = toScreenX(ax, ay);
            float sy = toScreenY(ax, ay);
            float by = sy + TILE_H / 2f + DecorationMetadata.groundYOffset(d.getResourcePath(), drawH);
            batch.draw(tex, sx - drawW / 2f, by, drawW, drawH);
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public static float toScreenX(int tileX, int tileY) {
        return (tileX - tileY) * (TILE_W / 2f);
    }

    public static float toScreenY(int tileX, int tileY) {
        return (tileX + tileY) * (TILE_H / 2f);
    }

    private static float toScreenX(float tileX, float tileY) {
        return (tileX - tileY) * (TILE_W / 2f);
    }

    private static float toScreenY(float tileX, float tileY) {
        return (tileX + tileY) * (TILE_H / 2f);
    }

    /**
     * Inverse of {@link #toScreenX} / {@link #toScreenY} at the tile origin (same space as unproject).
     */
    public static int[] toTile(float worldX, float worldY) {
        float halfW = TILE_W / 2f;
        float halfH = TILE_H / 2f;
        float a = worldX / halfW;
        float b = worldY / halfH;
        float fx = (a + b) * 0.5f;
        float fy = (b - a) * 0.5f;
        return new int[]{(int) Math.floor(fx), (int) Math.floor(fy)};
    }

    private void handleCameraInput(float delta) {
        float move = CAMERA_SPEED * delta * camera.zoom;

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.translate(-move, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            camera.translate(move, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            camera.translate(0, move);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            camera.translate(0, -move);
        }
    }

    public void zoom(float amount) {
        camera.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, camera.zoom + amount * ZOOM_SPEED));
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        int midTile = 32;
        camera.position.set(
                toScreenX(midTile, midTile),
                toScreenY(midTile, midTile),
                0
        );
    }

    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
        font.dispose();
    }

    private Color colorFor(TileType type, int tileX, int tileY) {
        switch (type) {
            case EMPTY:
                return new Color(0.28f, 0.52f, 0.22f, 1f);
            case CITY:
                return new Color(0.65f, 0.65f, 0.68f, 1f);
            case INDUSTRIAL_FACILITY:
                return new Color(0.82f, 0.58f, 0.25f, 1f);
            case ROAD:
                return new Color(0.45f, 0.44f, 0.42f, 1f);
            case WATER:
                return animatedWaterColor(tileX, tileY);
            case FOREST:
                return new Color(0.12f, 0.40f, 0.14f, 1f);
            default:
                return Color.MAGENTA;
        }
    }

    private Color animatedWaterColor(int tileX, int tileY) {
        // Cheap "moving water" look: combine two low-frequency waves
        // so neighboring tiles drift subtly in brightness over time.
        float waveA = (float) Math.sin(waterAnimTime * 1.7f + tileX * 0.42f + tileY * 0.31f);
        float waveB = (float) Math.sin(waterAnimTime * 2.4f - tileX * 0.27f + tileY * 0.48f);
        float wave = (waveA * 0.6f + waveB * 0.4f) * 0.5f + 0.5f; // [0..1]

        float r = 0.16f + wave * 0.10f;
        float g = 0.38f + wave * 0.14f;
        float b = 0.70f + wave * 0.18f;
        return new Color(r, g, b, 1f);
    }

    public void pan(float amountX, float amountY) {
        float panSpeed = 32f * camera.zoom;
        camera.translate(amountX * panSpeed, -amountY * panSpeed);
    }

    private Color vehicleColor(Vehicle vehicle) {
        boolean supportsPassengers = vehicle.getType().supports(TransportContentType.PASSENGERS);
        boolean supportsGoods = vehicle.getType().supports(TransportContentType.GOODS);
        if (supportsPassengers && supportsGoods) {
            return new Color(0.92f, 0.72f, 0.25f, 1f);
        }
        if (supportsPassengers) {
            return new Color(0.20f, 0.82f, 1f, 1f);
        }
        if (supportsGoods) {
            return new Color(1f, 0.55f, 0.20f, 1f);
        }
        return new Color(0.95f, 0.95f, 0.95f, 1f);
    }
}
