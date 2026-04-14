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
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.controller.PlacementValidator;
import com.team404.tycoon.desktop.assets.DecorationMetadata;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.model.BuildMode;
import com.team404.tycoon.model.EconomyConfig;
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
    private static final int FLAT_HEIGHT = 1;
    private static final float EDGE_DEPTH = 16f;
    /** Screen-pixel lift per height unit above flat (H:1). H:2 = +16 px, H:3 = +32 px. */
    private static final float HEIGHT_STEP = TILE_H * 0.5f;
    private static final Color EDGE_LEFT  = new Color(0.15f, 0.30f, 0.10f, 1f);
    private static final Color EDGE_RIGHT = new Color(0.10f, 0.22f, 0.08f, 1f);
    private static final Color GRID_COLOR = new Color(0f, 0f, 0f, 0.12f);
    private static final Color HIGHLIGHT_COLOR = new Color(1f, 1f, 0.3f, 0.85f);

    private static final float CAMERA_SPEED = 400f;
    private static final float ZOOM_SPEED = 0.1f;
    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 4.0f;

    private static final Color HOVER_VALID   = new Color(0.30f, 1f,    0.30f, 0.90f);
    private static final Color HOVER_INVALID = new Color(1f,    0.18f, 0.10f, 0.95f);

    private final OrthographicCamera camera;
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout;
    private final DecorationTextureCache textureCache;
    private final InputController inputController;
    private final PlacementValidator placementValidator;
    private float waterAnimTime;

    // ── birds ─────────────────────────────────────────────────────────────────
    private static final int   BIRD_COUNT      = 18;
    private static final float BIRD_ALTITUDE   = 90f;   // screen px above tile
    private static final float BIRD_WING       = 6f;    // half-wingspan px
    private static final float BIRD_FLAP_SPEED = 2.8f;
    private static final float BIRD_FLAP_AMP   = 4f;
    private final float[] birdTx   = new float[BIRD_COUNT]; // tile-space x
    private final float[] birdTy   = new float[BIRD_COUNT]; // tile-space y
    private final float[] birdVx   = new float[BIRD_COUNT]; // tiles/sec
    private final float[] birdVy   = new float[BIRD_COUNT]; // tiles/sec
    private final float[] birdPhase = new float[BIRD_COUNT];

    private int hoverTileX = -1;
    private int hoverTileY = -1;
    private boolean buildPreviewActive;
    private BuildPreviewMode buildPreviewMode = BuildPreviewMode.AREA;
    private int buildPreviewStartX;
    private int buildPreviewStartY;
    private int buildPreviewEndX;
    private int buildPreviewEndY;

    public Renderer2D(DecorationTextureCache textureCache, InputController inputController) {
        this.camera = new OrthographicCamera();
        this.shape = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.glyphLayout = new GlyphLayout();
        this.textureCache = textureCache;
        this.inputController = inputController;
        this.placementValidator = new PlacementValidator();
        java.util.Random bRng = new java.util.Random(7919L);
        for (int i = 0; i < BIRD_COUNT; i++) {
            birdTx[i]    = bRng.nextFloat() * 64f;
            birdTy[i]    = bRng.nextFloat() * 64f;
            birdPhase[i] = bRng.nextFloat() * 6.28f;
            // General drift: mostly right/down-right with individual wobble
            float baseAngle = 0.4f + (bRng.nextFloat() - 0.5f) * 0.8f;
            float speed     = 1.2f + bRng.nextFloat() * 1.0f;
            birdVx[i] = (float) Math.cos(baseAngle) * speed;
            birdVy[i] = (float) Math.sin(baseAngle) * speed;
        }
    }

    /** Returns how many screen pixels a tile at the given height should be lifted above the baseline. */
    private static float heightOffset(int height) {
        return Math.max(0, height - FLAT_HEIGHT) * HEIGHT_STEP;
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
        drawGroundTextures(map);
        drawTileSurface(map);
        drawGridLines(map);
        drawDecorations(state);
        drawTrafficLightOverlays(state);
        drawVehicles(state);
        drawTownNames(state);
        drawBuildPreview(map);
        drawHoverHighlight(state);
        drawBirds(delta, map.getWidth(), map.getHeight());
    }

    private static final String CAR_RIGHT_PATH      = "resources/car-right.png";
    private static final String CAR_LEFT_PATH       = "resources/car-left.png";
    private static final String CAR_DOWN_RIGHT_PATH = "resources/car-down-right.png";
    private static final String CAR_DOWN_LEFT_PATH  = "resources/car-down-left.png";
    private static final float VEHICLE_DRAW_SIZE = 32f;

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

        GameMap decMap = state.getMap();
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
            float hOff = decMap.isInBounds(ax, ay) ? heightOffset(decMap.getTile(ax, ay).getHeight()) : 0f;
            float sx = toScreenX(ax, ay);
            float sy = toScreenY(ax, ay) + hOff;
            float by = sy + TILE_H / 2f + DecorationMetadata.groundYOffset(d.getResourcePath(), drawH);
            batch.draw(tex, sx - drawW / 2f, by, drawW, drawH);
        }

        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private static final Color TL_GREEN      = new Color(0.10f, 0.90f, 0.20f, 0.95f);
    private static final Color TL_RED        = new Color(0.95f, 0.12f, 0.10f, 0.95f);
    private static final Color TL_DIM_GREEN  = new Color(0.04f, 0.22f, 0.06f, 0.80f);
    private static final Color TL_DIM_RED    = new Color(0.22f, 0.03f, 0.03f, 0.80f);
    private static final Color TL_HOUSING    = new Color(0.08f, 0.08f, 0.08f, 0.92f);

    /**
     * Draws a small traffic-light housing (dark box) with a red bulb on top and a green
     * bulb on the bottom above each placed traffic-light tile.  Only the active bulb is
     * lit; the other is dark, exactly like a real traffic light.
     * The horizontal direction is used as the reference signal for the overlay.
     */
    private void drawTrafficLightOverlays(GameState state) {
        java.util.List<int[]> lights = state.getTrafficLightTiles();
        if (lights.isEmpty()) {
            return;
        }
        GameMap tlMap = state.getMap();
        final float DOT_R    = 4.0f;
        final float DOT_GAP  = 3.0f;   // vertical gap between the two bulbs
        final float DOT_RISE = 18f;     // pixels above the tile top point
        final float PAD      = 2.5f;    // housing padding around bulbs

        float housingW = (DOT_R + PAD) * 2f;
        float housingH = DOT_R * 2f + DOT_GAP + DOT_R * 2f + PAD * 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int[] light : lights) {
            int lx = light[0];
            int ly = light[1];
            float hOff = tlMap.isInBounds(lx, ly)
                    ? heightOffset(tlMap.getTile(lx, ly).getHeight()) : 0f;
            float cx = toScreenX(lx, ly);
            float baseY = toScreenY(lx, ly) + hOff + TILE_H + DOT_RISE;

            boolean hGreen = state.isTrafficLightGreenForHorizontal(lx, ly);

            // Dark housing box
            shape.setColor(TL_HOUSING);
            shape.rect(cx - housingW / 2f, baseY, housingW, housingH);

            // Green bulb (bottom) — lit when horizontal has green
            float greenCY = baseY + PAD + DOT_R;
            shape.setColor(hGreen ? TL_GREEN : TL_DIM_GREEN);
            shape.circle(cx, greenCY, DOT_R, 12);

            // Red bulb (top) — lit when horizontal has red
            float redCY = greenCY + DOT_R + DOT_GAP + DOT_R;
            shape.setColor(hGreen ? TL_DIM_RED : TL_RED);
            shape.circle(cx, redCY, DOT_R, 12);
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawVehicles(GameState state) {
        if (state.getVehicles().isEmpty()) {
            return;
        }
        Texture carRightTex     = textureCache.get(CAR_RIGHT_PATH);
        Texture carLeftTex      = textureCache.get(CAR_LEFT_PATH);
        Texture carDownRightTex = textureCache.get(CAR_DOWN_RIGHT_PATH);
        Texture carDownLeftTex  = textureCache.get(CAR_DOWN_LEFT_PATH);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.setColor(Color.WHITE);
        batch.begin();
        for (Vehicle vehicle : state.getVehicles()) {
            float[] pos = getVehicleScreenPos(state, vehicle);
            if (pos == null) {
                continue;
            }
            int dx = (int) pos[2];
            int dy = (int) pos[3];
            Texture carTex;
            if (dx > 0) {
                carTex = carRightTex;
            } else if (dx < 0) {
                carTex = carDownLeftTex;
            } else if (dy > 0) {
                carTex = carLeftTex;
            } else {
                carTex = carDownRightTex;
            }
            // Tint vehicle sprite based on maintenance state so the player can see at a glance.
            float overdueFactor = vehicle.getMaintenanceOverdueFactor();
            if (overdueFactor <= 0.5f) {
                batch.setColor(1f, 0.30f, 0.20f, 1f); // bright red  – critically overdue, 50% speed
            } else if (overdueFactor < 1f) {
                batch.setColor(1f, 0.75f, 0.10f, 1f); // orange      – overdue, 75% speed
            } else if (vehicle.isMaintenanceDue()) {
                batch.setColor(1f, 0.90f, 0.30f, 1f); // yellow      – due now, will be serviced at next garage
            } else if (vehicle.isMaintenanceDueSoon()) {
                batch.setColor(1f, 1f, 0.60f, 1f);    // pale yellow – coming up soon (>75% of interval used)
            } else {
                batch.setColor(Color.WHITE);            // white       – healthy
            }
            float half = VEHICLE_DRAW_SIZE / 2f;
            batch.draw(carTex, pos[0] - half, pos[1] - half, VEHICLE_DRAW_SIZE, VEHICLE_DRAW_SIZE);
            batch.setColor(Color.WHITE); // always reset so other sprites are unaffected
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
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
        int dirDx = 1;
        int dirDy = 0;
        if (roadPath.isEmpty()) {
            int[] nearest = RoadPathfinder.findNearestRoadTile(
                    state, fromTown.getCenterX(), fromTown.getCenterY(), 6);
            if (nearest == null) {
                return null;
            }
            tilePos = new float[]{nearest[0], nearest[1]};
        } else {
            tilePos = interpolateOnRoadPath(roadPath, vehicle.getLegProgressTiles());
            float progress = Math.max(0f, Math.min(vehicle.getLegProgressTiles(), roadPath.size() - 1f));
            int seg = Math.min((int) Math.floor(progress), roadPath.size() - 2);
            int[] segFrom = roadPath.get(seg);
            int[] segTo   = roadPath.get(seg + 1);
            dirDx = segTo[0] - segFrom[0];
            dirDy = segTo[1] - segFrom[1];
        }
        float sx = toScreenX(tilePos[0], tilePos[1]);
        float sy = toScreenY(tilePos[0], tilePos[1]) + TILE_H;
        return new float[]{sx, sy, dirDx, dirDy};
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

        GameMap nameMap = state.getMap();
        for (int i = 0; i < state.getTowns().size(); i++) {
            Town t = state.getTowns().get(i);
            float hOff = nameMap.isInBounds(t.getCenterX(), t.getCenterY())
                    ? heightOffset(nameMap.getTile(t.getCenterX(), t.getCenterY()).getHeight()) : 0f;
            float sx = toScreenX(t.getCenterX(), t.getCenterY());
            float sy = toScreenY(t.getCenterX(), t.getCenterY()) + hOff;
            String name = t.getName();
            glyphLayout.setText(font, name);
            float x = sx - glyphLayout.width * 0.5f;
            float y = sy + TILE_H * 1.8f;
            font.setColor(0f, 0f, 0f, 0.8f);
            font.draw(batch, name, x + 1f, y - 1f);
            font.setColor(0.95f, 0.95f, 0.95f, 1f);
            font.draw(batch, name, x, y);

            // Population sub-label
            font.getData().setScale(0.80f);
            String popLabel = String.format("%,d", t.getPopulation());
            glyphLayout.setText(font, popLabel);
            float px = sx - glyphLayout.width * 0.5f;
            float py = y - 12f;
            font.setColor(0f, 0f, 0f, 0.65f);
            font.draw(batch, popLabel, px + 1f, py - 1f);
            font.setColor(0.65f, 0.90f, 1.00f, 0.95f);
            font.draw(batch, popLabel, px, py);
            font.getData().setScale(1f);

            String demandLabel = buildDemandLabel(state, i);
            if (!demandLabel.isEmpty()) {
                glyphLayout.setText(font, demandLabel);
                float dx = sx - glyphLayout.width * 0.5f;
                float dy = y - 26f; // pushed down to make room for population label
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

    private static final Color GROUND_FILL = new Color(0.32f, 0.52f, 0.20f, 1f);
    private static final Color WATER_FILL  = new Color(0.22f, 0.52f, 0.80f, 1f);
    private static final Color FOREST_FILL = new Color(0.12f, 0.38f, 0.10f, 1f);
    private static final Color CITY_FILL   = new Color(0.55f, 0.55f, 0.55f, 1f);

    /** Draws PNG textures for ground and water tiles (back-to-front, before cliffs). */
    private void drawGroundTextures(GameMap map) {
        // Pass 1 — solid diamonds via ShapeRenderer so there are zero pixel gaps.
        // Any crack that slips through the texture will show the matching solid colour, not black.
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = map.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(x, y);
                float sx = toScreenX(x, y);
                float sy = toScreenY(x, y);
                if (tile.getType() == TileType.EMPTY && tile.getHeight() == 1) {
                    drawDiamond(sx, sy, GROUND_FILL);
                } else if (tile.getType() == TileType.WATER) {
                    drawDiamond(sx, sy, WATER_FILL);
                } else if (tile.getType() == TileType.FOREST) {
                    drawDiamond(sx, sy, FOREST_FILL);
                } else if (tile.getType() == TileType.CITY) {
                    drawDiamond(sx, sy, CITY_FILL);
                }
            }
        }
        shape.end();

        // Pass 2 — texture on top for visual detail.
        Texture groundTex  = textureCache.get("resources/simpleground.png");
        Texture waterTex   = textureCache.get("resources/waterr.png");
        Texture forestTex  = textureCache.get("resources/forest.png");
        Texture cityTex    = textureCache.get("resources/groundgrey.png");
        float gW = TILE_W, gH = gW * ((float) groundTex.getHeight() / groundTex.getWidth());
        float wW = TILE_W, wH = wW * ((float) waterTex.getHeight()  / waterTex.getWidth());
        float fW = TILE_W, fH = fW * ((float) forestTex.getHeight() / forestTex.getWidth());
        float cW = TILE_W, cH = cW * ((float) cityTex.getHeight()   / cityTex.getWidth());
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (int y = map.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(x, y);
                float sx = toScreenX(x, y);
                float sy = toScreenY(x, y);
                if (tile.getType() == TileType.EMPTY && tile.getHeight() == 1) {
                    batch.draw(groundTex, sx - gW / 2f, sy, gW, gH);
                } else if (tile.getType() == TileType.WATER) {
                    batch.draw(waterTex,  sx - wW / 2f, sy, wW, wH);
                } else if (tile.getType() == TileType.FOREST) {
                    batch.draw(forestTex, sx - fW / 2f, sy, fW, fH);
                } else if (tile.getType() == TileType.CITY) {
                    batch.draw(cityTex,   sx - cW / 2f, sy, cW, cH);
                }
            }
        }
        batch.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawTileSurface(GameMap map) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = map.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(x, y);
                float sx = toScreenX(x, y);
                float sy = toScreenY(x, y);
                float hOff = heightOffset(tile.getHeight());

                // Left cliff — when this tile is higher than its (x-1, y) neighbour.
                float leftOff = map.isInBounds(x - 1, y)
                        ? heightOffset(map.getTile(x - 1, y).getHeight()) : 0f;
                if (hOff > leftOff) {
                    drawLeftCliff(sx, sy + hOff, hOff - leftOff,
                            cliffLeftColor(tile.getType(), tile.getHeight()));
                }

                // Right cliff — when this tile is higher than its (x, y-1) neighbour.
                float rightOff = map.isInBounds(x, y - 1)
                        ? heightOffset(map.getTile(x, y - 1).getHeight()) : 0f;
                if (hOff > rightOff) {
                    drawRightCliff(sx, sy + hOff, hOff - rightOff,
                            cliffRightColor(tile.getType(), tile.getHeight()));
                }

                // Skip diamond for tiles drawn as textures in drawGroundTextures.
                if ((tile.getType() == TileType.EMPTY && tile.getHeight() == 1)
                        || tile.getType() == TileType.WATER
                        || tile.getType() == TileType.FOREST
                        || tile.getType() == TileType.CITY) {
                    continue;
                }
                drawDiamond(sx, sy + hOff, colorFor(tile.getType(), tile.getHeight(), x, y));
            }
        }
        shape.end();
    }

    /** Left (south-west-facing) cliff face between a raised tile and its lower-left neighbour. */
    private void drawLeftCliff(float sx, float topSy, float drop, Color color) {
        float hw = TILE_W / 2f;
        float hh = TILE_H / 2f;
        shape.setColor(color);
        // parallelogram: left edge of surface down to left edge of lower tile
        shape.triangle(sx - hw, topSy + hh, sx, topSy, sx, topSy - drop);
        shape.triangle(sx - hw, topSy + hh, sx, topSy - drop, sx - hw, topSy + hh - drop);
    }

    /** Right (south-east-facing) cliff face between a raised tile and its lower-right neighbour. */
    private void drawRightCliff(float sx, float topSy, float drop, Color color) {
        float hw = TILE_W / 2f;
        float hh = TILE_H / 2f;
        shape.setColor(color);
        // parallelogram: right edge of surface down to right edge of lower tile
        shape.triangle(sx, topSy, sx + hw, topSy + hh, sx + hw, topSy + hh - drop);
        shape.triangle(sx, topSy, sx + hw, topSy + hh - drop, sx, topSy - drop);
    }

    private static Color cliffLeftColor(TileType type, int height) {
        if (height >= 3) {
            return new Color(0.40f, 0.32f, 0.20f, 1f); // rocky, lit
        }
        if (height == 2) {
            return new Color(0.18f, 0.32f, 0.10f, 1f); // hillside, lit
        }
        return EDGE_LEFT;
    }

    private static Color cliffRightColor(TileType type, int height) {
        if (height >= 3) {
            return new Color(0.28f, 0.22f, 0.12f, 1f); // rocky, shadow
        }
        if (height == 2) {
            return new Color(0.12f, 0.22f, 0.07f, 1f); // hillside, shadow
        }
        return EDGE_RIGHT;
    }

    private void drawDiamond(float sx, float sy, Color color) {
        float hw = TILE_W / 2f;
        float hh = TILE_H / 2f;
        float by = sy + hh;
        shape.setColor(color);
        shape.triangle(sx, by + TILE_H, sx - hw, by + hh, sx, by);
        shape.triangle(sx, by + TILE_H, sx + hw, by + hh, sx, by);
    }

    private void drawHoverHighlight(GameState state) {
        GameMap map = state.getMap();
        if (!map.isInBounds(hoverTileX, hoverTileY)) {
            return;
        }

        // Show green/red outline depending on whether the current action is valid on the hovered tile.
        Color outlineColor = HIGHLIGHT_COLOR;
        String selectedPath = inputController.getSelectedAssetPath();
        BuildMode mode = inputController.getCurrentMode();
        if (selectedPath != null && EconomyConfig.isRoadDecoration(selectedPath)) {
            boolean valid = placementValidator.canBuildRoad(map, state, hoverTileX, hoverTileY);
            outlineColor = valid ? HOVER_VALID : HOVER_INVALID;
        } else if (mode == BuildMode.RAISE_TERRAIN) {
            boolean valid = placementValidator.canRaiseTile(map, hoverTileX, hoverTileY);
            outlineColor = valid ? HOVER_VALID : HOVER_INVALID;
        } else if (mode == BuildMode.LOWER_TERRAIN) {
            boolean valid = placementValidator.canLowerTile(map, hoverTileX, hoverTileY);
            outlineColor = valid ? HOVER_VALID : HOVER_INVALID;
        }

        float hOff = heightOffset(map.getTile(hoverTileX, hoverTileY).getHeight());
        float sx = toScreenX(hoverTileX, hoverTileY);
        float sy = toScreenY(hoverTileX, hoverTileY) + hOff;
        float hw = TILE_W / 2f;
        float hh = TILE_H / 2f;
        float by = sy + hh;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        Gdx.gl.glLineWidth(2f);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(outlineColor);
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
                float hOff = heightOffset(map.getTile(x, y).getHeight());
                float sx = toScreenX(x, y);
                float sy = toScreenY(x, y) + hOff;
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
            float eHOff = map.isInBounds(x, 0) ? heightOffset(map.getTile(x, 0).getHeight()) : 0f;
            float sx = toScreenX(x, 0);
            float by = toScreenY(x, 0) + eHOff + hh;
            shape.triangle(sx, by, sx - hw, by + hh, sx - hw, by + hh - EDGE_DEPTH);
            shape.triangle(sx, by, sx - hw, by + hh - EDGE_DEPTH, sx, by - EDGE_DEPTH);
        }

        shape.setColor(EDGE_RIGHT);
        for (int y = 0; y < h; y++) {
            float eHOff = map.isInBounds(0, y) ? heightOffset(map.getTile(0, y).getHeight()) : 0f;
            float sx = toScreenX(0, y);
            float by = toScreenY(0, y) + eHOff + hh;
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
    }

    /**
     * Sets the camera to a comfortable starting position for a new game: zoom=1,
     * centred on the map's isometric midpoint. Call once after the map is generated.
     */
    public void initCameraForMap(int mapW, int mapH) {
        camera.zoom = 1.0f;
        camera.position.set(toScreenX(mapW / 2, mapH / 2), toScreenY(mapW / 2, mapH / 2), 0);
        camera.update();
    }

    /**
     * Centers and zooms the camera so the full map is visible with a small margin.
     * Call this once after the game map has been created.
     */
    public void fitCamera(int mapW, int mapH) {
        int vw = Gdx.graphics.getWidth();
        int vh = Gdx.graphics.getHeight();

        // World-space extent of the isometric diamond
        float worldW = (mapW + mapH) * (TILE_W / 2f);
        float worldH = (mapW + mapH) * (TILE_H / 2f) + 64f; // +64 headroom for tall tiles/sprites

        // The HUD chrome eats some screen pixels at the top; account for it
        float availH = Math.max(1f, vh - UiChrome.totalTopHeight());

        float fitZoomW = worldW / Math.max(1f, vw);
        float fitZoomH = worldH / availH;
        float zoom = Math.max(fitZoomW, fitZoomH) * 1.08f; // 8 % margin so edges aren't clipped
        camera.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

        // Center on the map's isometric midpoint
        int midX = mapW / 2;
        int midY = mapH / 2;
        camera.position.set(toScreenX(midX, midY), toScreenY(midX, midY), 0);
        camera.update();
    }

    private void drawBirds(float delta, int mapW, int mapH) {
        // Update positions
        for (int i = 0; i < BIRD_COUNT; i++) {
            birdTx[i] += birdVx[i] * delta;
            birdTy[i] += birdVy[i] * delta;
            if (birdTx[i] > mapW) birdTx[i] -= mapW;
            if (birdTx[i] < 0)    birdTx[i] += mapW;
            if (birdTy[i] > mapH) birdTy[i] -= mapH;
            if (birdTy[i] < 0)    birdTy[i] += mapH;
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glLineWidth(1.5f);
        shape.setProjectionMatrix(camera.combined);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.12f, 0.10f, 0.08f, 0.80f);
        for (int i = 0; i < BIRD_COUNT; i++) {
            float sx = toScreenX(birdTx[i], birdTy[i]);
            float sy = toScreenY(birdTx[i], birdTy[i]) + BIRD_ALTITUDE;
            float flapY = (float) Math.sin(waterAnimTime * BIRD_FLAP_SPEED + birdPhase[i]) * BIRD_FLAP_AMP;
            shape.line(sx - BIRD_WING, sy + flapY, sx, sy);
            shape.line(sx, sy, sx + BIRD_WING, sy + flapY);
        }
        shape.end();
        Gdx.gl.glLineWidth(1f);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    @Override
    public void dispose() {
        shape.dispose();
        batch.dispose();
        font.dispose();
    }

    private Color colorFor(TileType type, int height, int tileX, int tileY) {
        switch (type) {
            case EMPTY:
                if (height >= 3) {
                    return new Color(0.62f, 0.52f, 0.38f, 1f); // rocky peak
                }
                if (height == 2) {
                    return new Color(0.30f, 0.46f, 0.18f, 1f); // olive foothill
                }
                return new Color(0.28f, 0.52f, 0.22f, 1f);     // normal grass
            case CITY:
                return new Color(0.65f, 0.65f, 0.68f, 1f);
            case INDUSTRIAL_FACILITY:
                return new Color(0.82f, 0.58f, 0.25f, 1f);
            case ROAD:
                return new Color(0.45f, 0.44f, 0.42f, 1f);
            case WATER:
                return animatedWaterColor(tileX, tileY);
            case FOREST:
                if (height >= 3) {
                    return new Color(0.06f, 0.26f, 0.07f, 1f); // dark mountain forest
                }
                if (height == 2) {
                    return new Color(0.09f, 0.34f, 0.11f, 1f); // slightly darker foothill
                }
                return new Color(0.12f, 0.40f, 0.14f, 1f);     // normal forest
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
