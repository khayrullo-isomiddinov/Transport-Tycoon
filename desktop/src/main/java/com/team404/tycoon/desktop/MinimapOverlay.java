package com.team404.tycoon.desktop;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.TileType;

/**
 * Draws and handles interaction for the HUD minimap.
 */
public class MinimapOverlay {

    private static final float PADDING = 16f;
    private static final float MINIMAP_WIDTH = 210f;
    private static final float MINIMAP_HEIGHT = 150f;

    private static final Color BACKGROUND = new Color(0f, 0f, 0f, 0.55f);
    private static final Color BORDER = new Color(1f, 1f, 1f, 0.4f);
    private static final Color VIEWPORT_COLOR = new Color(1f, 1f, 0.2f, 0.9f);

    private float x;
    private float y;
    private float width;
    private float height;

    private final Vector3 screenTmp = new Vector3();

    public void updateLayout(float screenWidth, float screenHeight) {
        this.width = MINIMAP_WIDTH;
        this.height = MINIMAP_HEIGHT;
        this.x = screenWidth - width - PADDING;
        this.y = PADDING;
    }

    public void draw(ShapeRenderer shape, GameMap map, OrthographicCamera worldCamera, float screenWidth, float screenHeight) {
        updateLayout(screenWidth, screenHeight);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(BACKGROUND);
        shape.rect(x, y, width, height);

        float cellW = width / map.getWidth();
        float cellH = height / map.getHeight();
        for (int ty = 0; ty < map.getHeight(); ty++) {
            for (int tx = 0; tx < map.getWidth(); tx++) {
                shape.setColor(colorForMinimap(map.getTile(tx, ty).getType()));
                shape.rect(x + tx * cellW, y + ty * cellH, cellW + 0.2f, cellH + 0.2f);
            }
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(BORDER);
        shape.rect(x, y, width, height);

        ViewportBounds viewport = computeVisibleTileBounds(worldCamera, screenWidth, screenHeight, map);
        float vx = x + viewport.minTileX * cellW;
        float vy = y + viewport.minTileY * cellH;
        float vw = Math.max(1f, (viewport.maxTileX - viewport.minTileX + 1) * cellW);
        float vh = Math.max(1f, (viewport.maxTileY - viewport.minTileY + 1) * cellH);

        shape.setColor(VIEWPORT_COLOR);
        shape.rect(vx, vy, Math.min(vw, width), Math.min(vh, height));
        shape.end();
    }

    public boolean containsScreenPoint(int screenX, int screenY, int screenHeight) {
        updateLayout(com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight());
        float hudY = screenHeight - screenY;
        return screenX >= x && screenX <= (x + width) && hudY >= y && hudY <= (y + height);
    }

    public int[] minimapPointToTile(int screenX, int screenY, int screenHeight, GameMap map) {
        updateLayout(com.badlogic.gdx.Gdx.graphics.getWidth(), com.badlogic.gdx.Gdx.graphics.getHeight());
        float hudY = screenHeight - screenY;
        float localX = MathUtils.clamp(screenX - x, 0f, width);
        float localY = MathUtils.clamp(hudY - y, 0f, height);

        float normX = width <= 0f ? 0f : localX / width;
        float normY = height <= 0f ? 0f : localY / height;

        int tileX = MathUtils.clamp((int) (normX * map.getWidth()), 0, map.getWidth() - 1);
        int tileY = MathUtils.clamp((int) (normY * map.getHeight()), 0, map.getHeight() - 1);
        return new int[]{tileX, tileY};
    }

    private ViewportBounds computeVisibleTileBounds(
            OrthographicCamera worldCamera,
            float screenWidth,
            float screenHeight,
            GameMap map
    ) {
        screenTmp.set(0f, 0f, 0f);
        worldCamera.unproject(screenTmp, 0f, 0f, screenWidth, screenHeight);
        float minWorldX = screenTmp.x;
        float minWorldY = screenTmp.y;

        screenTmp.set(screenWidth, screenHeight, 0f);
        worldCamera.unproject(screenTmp, 0f, 0f, screenWidth, screenHeight);
        float maxWorldX = screenTmp.x;
        float maxWorldY = screenTmp.y;

        int[] a = com.team404.tycoon.desktop.Renderer2D.toTile(minWorldX, minWorldY);
        int[] b = com.team404.tycoon.desktop.Renderer2D.toTile(maxWorldX, maxWorldY);
        int[] c = com.team404.tycoon.desktop.Renderer2D.toTile(minWorldX, maxWorldY);
        int[] d = com.team404.tycoon.desktop.Renderer2D.toTile(maxWorldX, minWorldY);

        int minTileX = Math.min(Math.min(a[0], b[0]), Math.min(c[0], d[0]));
        int maxTileX = Math.max(Math.max(a[0], b[0]), Math.max(c[0], d[0]));
        int minTileY = Math.min(Math.min(a[1], b[1]), Math.min(c[1], d[1]));
        int maxTileY = Math.max(Math.max(a[1], b[1]), Math.max(c[1], d[1]));

        minTileX = MathUtils.clamp(minTileX, 0, map.getWidth() - 1);
        maxTileX = MathUtils.clamp(maxTileX, 0, map.getWidth() - 1);
        minTileY = MathUtils.clamp(minTileY, 0, map.getHeight() - 1);
        maxTileY = MathUtils.clamp(maxTileY, 0, map.getHeight() - 1);

        return new ViewportBounds(minTileX, minTileY, maxTileX, maxTileY);
    }

    private Color colorForMinimap(TileType type) {
        switch (type) {
            case EMPTY:
                return new Color(0.22f, 0.52f, 0.20f, 1f);
            case CITY:
                return new Color(0.72f, 0.72f, 0.75f, 1f);
            case INDUSTRIAL_FACILITY:
                return new Color(0.80f, 0.55f, 0.20f, 1f);
            case ROAD:
                return new Color(0.33f, 0.33f, 0.33f, 1f);
            case WATER:
                return new Color(0.20f, 0.56f, 0.95f, 1f);
            case FOREST:
                return new Color(0.06f, 0.33f, 0.07f, 1f);
            default:
                return Color.MAGENTA;
        }
    }

    private static final class ViewportBounds {
        private final int minTileX;
        private final int minTileY;
        private final int maxTileX;
        private final int maxTileY;

        private ViewportBounds(int minTileX, int minTileY, int maxTileX, int maxTileY) {
            this.minTileX = minTileX;
            this.minTileY = minTileY;
            this.maxTileX = maxTileX;
            this.maxTileY = maxTileY;
        }
    }
}
