package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.team404.tycoon.controller.GameController;
import com.team404.tycoon.desktop.assets.DecorationMetadata;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

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
    private final DecorationTextureCache textureCache;

    private int hoverTileX = -1;
    private int hoverTileY = -1;

    public Renderer2D(DecorationTextureCache textureCache) {
        this.camera = new OrthographicCamera();
        this.shape = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.textureCache = textureCache;
    }

    public void setHoverTile(int tileX, int tileY) {
        this.hoverTileX = tileX;
        this.hoverTileY = tileY;
    }

    @Override
    public void render(GameController controller, float delta) {
        handleCameraInput(delta);

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
        drawHoverHighlight(map);
    }

    private void drawTileSurface(GameMap map) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int y = map.getHeight() - 1; y >= 0; y--) {
            for (int x = 0; x < map.getWidth(); x++) {
                Tile tile = map.getTile(x, y);
                float sx = toScreenX(x, y);
                float sy = toScreenY(x, y);
                drawDiamond(sx, sy, colorFor(tile.getType()));
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
            float aspect = (float) tex.getHeight() / (float) tex.getWidth();
            float vScale = DecorationMetadata.verticalOverlapScale(
                    d.getResourcePath(), fw, fh);
            float drawH = drawW * aspect * vScale;

            int ax = d.getAnchorTileX();
            int ay = d.getAnchorTileY();
            float sx = toScreenX(ax, ay);
            float sy = toScreenY(ax, ay);
            float by = sy + TILE_H / 2f;
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
    }

    private static Color colorFor(TileType type) {
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
                return new Color(0.22f, 0.45f, 0.78f, 1f);
            case FOREST:
                return new Color(0.12f, 0.40f, 0.14f, 1f);
            default:
                return Color.MAGENTA;
        }
    }

    public void pan(float amountX, float amountY) {
        float panSpeed = 32f * camera.zoom;
        camera.translate(amountX * panSpeed, -amountY * panSpeed);
    }
}
