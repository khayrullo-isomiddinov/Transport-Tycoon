package com.team404.tycoon.desktop;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.PlacedDecoration;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;
import com.team404.tycoon.model.Town;
import com.team404.tycoon.model.TransportContentType;
import com.team404.tycoon.model.TransportDemand;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Left-side hover inspector with clickable center details modal.
 */
public class ObjectInspectorOverlay {

    private static final float PANEL_PAD = 16f;
    // Match minimap footprint for visual balance.
    private static final float PANEL_W = 210f;
    private static final float PANEL_H = 150f;

    private static final Color PANEL_BG = new Color(0.06f, 0.09f, 0.15f, 0.88f);
    private static final Color PANEL_BORDER = new Color(0.30f, 0.45f, 0.68f, 1f);
    private static final Color MODAL_BG = new Color(0.05f, 0.08f, 0.12f, 0.96f);
    private static final Color MODAL_BORDER = new Color(0.45f, 0.65f, 0.95f, 1f);
    private static final Color MODAL_BACKDROP = new Color(0f, 0f, 0f, 0.45f);

    private boolean modalOpen;

    private float panelX;
    private float panelY;
    private float panelW;
    private float panelH;

    private float modalX;
    private float modalY;
    private float modalW;
    private float modalH;

    public void draw(
            ShapeRenderer shape,
            SpriteBatch batch,
            BitmapFont font,
            GlyphLayout glyph,
            DecorationTextureCache textureCache,
            GameState state,
            int hoverTileX,
            int hoverTileY,
            float screenWidth,
            float screenHeight
    ) {
        InspectorData data = resolve(state, hoverTileX, hoverTileY);
        updateLayout(screenWidth, screenHeight);

        drawPanel(shape, batch, font, glyph, textureCache, data, hoverTileX, hoverTileY);
        if (modalOpen) {
            drawModal(shape, batch, font, glyph, data, hoverTileX, hoverTileY, screenWidth, screenHeight);
        }
    }

    public boolean isModalOpen() {
        return modalOpen;
    }

    public void closeModal() {
        modalOpen = false;
    }

    public void toggleDetails(GameState state, int hoverTileX, int hoverTileY) {
        if (modalOpen) {
            modalOpen = false;
            return;
        }
        InspectorData data = resolve(state, hoverTileX, hoverTileY);
        if (!"None".equals(data.type)) {
            modalOpen = true;
        }
    }

    private void updateLayout(float screenWidth, float screenHeight) {
        float mapViewHeight = Math.max(1f, screenHeight - UiChrome.totalTopHeight());

        panelW = PANEL_W;
        panelH = PANEL_H;
        panelX = PANEL_PAD;
        panelY = PANEL_PAD;

        modalW = Math.min(620f, screenWidth * 0.52f);
        modalH = Math.min(380f, mapViewHeight * 0.62f);
        modalX = (screenWidth - modalW) * 0.5f;
        modalY = (screenHeight - modalH) * 0.5f;
    }

    private void drawPanel(
            ShapeRenderer shape,
            SpriteBatch batch,
            BitmapFont font,
            GlyphLayout glyph,
            DecorationTextureCache textureCache,
            InspectorData data,
            int hoverTileX,
            int hoverTileY
    ) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(PANEL_BG);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(PANEL_BORDER);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();

        float iconSize = 36f;
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(data.iconColor);
        shape.rect(panelX + 12f, panelY + panelH - iconSize - 12f, iconSize, iconSize);
        shape.end();

        float iconX = panelX + 12f;
        float iconY = panelY + panelH - iconSize - 12f;

        batch.begin();
        if (data.iconResourcePath != null) {
            Texture icon = textureCache.get(data.iconResourcePath);
            float tw = icon.getWidth();
            float th = icon.getHeight();
            float scale = iconSize / Math.max(tw, th);
            float dw = tw * scale;
            float dh = th * scale;
            float drawX = iconX + (iconSize - dw) * 0.5f;
            float drawY = iconY + (iconSize - dh) * 0.5f;
            batch.setColor(Color.WHITE);
            batch.draw(icon, drawX, drawY, dw, dh);
        }

        font.getData().setScale(1.0f);
        font.setColor(new Color(0.80f, 0.90f, 1f, 1f));
        font.draw(batch, "Object Inspector", panelX + 58f, panelY + panelH - 14f);

        font.getData().setScale(1.08f);
        font.setColor(Color.WHITE);
        font.draw(batch, data.name, panelX + 12f, panelY + panelH - 56f);

        font.getData().setScale(0.90f);
        font.setColor(new Color(0.65f, 0.76f, 0.95f, 1f));
        font.draw(batch, "Type: " + data.type, panelX + 12f, panelY + panelH - 76f);
        font.draw(batch, "Tile: (" + hoverTileX + ", " + hoverTileY + ")", panelX + 12f, panelY + panelH - 94f);

        font.setColor(new Color(0.76f, 0.84f, 0.95f, 1f));
        float y = panelY + panelH - 116f;
        int maxLines = 3;
        for (int i = 0; i < data.lines.size() && i < maxLines; i++) {
            font.draw(batch, data.lines.get(i), panelX + 12f, y);
            y -= 17f;
        }

        font.setColor(new Color(0.50f, 0.92f, 1f, 1f));
        font.draw(batch, "Press [L] for details", panelX + 12f, panelY + 16f);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawModal(
            ShapeRenderer shape,
            SpriteBatch batch,
            BitmapFont font,
            GlyphLayout glyph,
            InspectorData data,
            int hoverTileX,
            int hoverTileY,
            float screenWidth,
            float screenHeight
    ) {
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(MODAL_BACKDROP);
        shape.rect(0f, 0f, screenWidth, screenHeight);
        shape.setColor(MODAL_BG);
        shape.rect(modalX, modalY, modalW, modalH);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(MODAL_BORDER);
        shape.rect(modalX, modalY, modalW, modalH);
        shape.end();

        batch.begin();
        font.getData().setScale(1.2f);
        font.setColor(Color.WHITE);
        font.draw(batch, data.name, modalX + 18f, modalY + modalH - 18f);

        font.getData().setScale(0.95f);
        font.setColor(new Color(0.70f, 0.82f, 1f, 1f));
        font.draw(batch, "Type: " + data.type, modalX + 18f, modalY + modalH - 44f);
        font.draw(batch, "Tile: (" + hoverTileX + ", " + hoverTileY + ")", modalX + 18f, modalY + modalH - 62f);

        float y = modalY + modalH - 92f;
        font.setColor(new Color(0.88f, 0.93f, 1f, 1f));
        for (String line : data.lines) {
            glyph.setText(font, line);
            if (y < modalY + 28f) {
                break;
            }
            font.draw(batch, line, modalX + 18f, y);
            y -= 20f;
        }

        font.setColor(new Color(0.55f, 0.92f, 1f, 1f));
        font.draw(batch, "Press [L] to close", modalX + 18f, modalY + 18f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private InspectorData resolve(GameState state, int tileX, int tileY) {
        GameMap map = state.getMap();
        if (!map.isInBounds(tileX, tileY)) {
            return new InspectorData("None", "No object", new ArrayList<String>(), new Color(0.30f, 0.30f, 0.30f, 1f), null);
        }

        Optional<PlacedDecoration> decoration = state.findDecorationAt(tileX, tileY);
        if (decoration.isPresent()) {
            return inspectDecoration(state, decoration.get(), tileX, tileY);
        }

        Tile tile = map.getTile(tileX, tileY);
        if (tile.getType() == TileType.CITY) {
            return inspectCityTile(state, tileX, tileY);
        }

        List<String> lines = new ArrayList<>();
        lines.add("Terrain: " + prettyTileType(tile.getType()));
        lines.add("Height: " + tile.getHeight());
        return new InspectorData("Tile", prettyTileType(tile.getType()), lines, colorForTile(tile.getType()), null);
    }

    private InspectorData inspectDecoration(GameState state, PlacedDecoration decoration, int tileX, int tileY) {
        String path = decoration.getResourcePath().toLowerCase(Locale.ROOT);
        String name = prettyResourceName(decoration.getResourcePath());
        List<String> lines = new ArrayList<>();
        lines.add("Anchor: (" + decoration.getAnchorTileX() + ", " + decoration.getAnchorTileY() + ")");
        lines.add("Footprint: " + decoration.getFootprintTilesW() + "x" + decoration.getFootprintTilesH());

        if (path.contains("garage")) {
            int purchased = state.getGaragePurchaseCount(decoration.getAnchorTileX(), decoration.getAnchorTileY());
            boolean connected = state.isGarageConnectedToRoad(tileX, tileY);
            lines.add("Purchased vehicles: " + purchased);
            lines.add("Connected to road: " + (connected ? "Yes" : "No"));
            return new InspectorData("Garage", name, lines, new Color(0.78f, 0.58f, 0.28f, 1f), decoration.getResourcePath());
        }
        if (path.contains("trafficlights") || path.contains("traffic lights")) {
            lines.add(String.format(Locale.US, "Green H/V: %.1fs / %.1fs",
                    state.getTrafficLightHorizontalGreenSeconds(),
                    state.getTrafficLightVerticalGreenSeconds()));
            lines.add("Current horizontal: " + (state.isTrafficLightGreenForHorizontal(tileX, tileY) ? "Green" : "Red"));
            return new InspectorData("Traffic Light", name, lines, new Color(0.86f, 0.28f, 0.24f, 1f), decoration.getResourcePath());
        }
        return new InspectorData("Decoration", name, lines, new Color(0.65f, 0.72f, 0.84f, 1f), decoration.getResourcePath());
    }

    private InspectorData inspectCityTile(GameState state, int tileX, int tileY) {
        Town nearest = null;
        int best = Integer.MAX_VALUE;
        int nearestIndex = -1;
        List<Town> towns = state.getTowns();
        for (int i = 0; i < towns.size(); i++) {
            Town town = towns.get(i);
            int dist = Math.abs(town.getCenterX() - tileX) + Math.abs(town.getCenterY() - tileY);
            if (dist < best) {
                best = dist;
                nearest = town;
                nearestIndex = i;
            }
        }

        if (nearest == null) {
            List<String> lines = new ArrayList<>();
            lines.add("No town metadata available");
            return new InspectorData("City", "Unnamed City", lines, new Color(0.55f, 0.65f, 0.90f, 1f), null);
        }

        int passengersDemand = 0;
        int goodsDemand = 0;
        for (TransportDemand demand : state.getTransportDemand()) {
            if (demand.getOriginTownIndex() != nearestIndex) {
                continue;
            }
            if (demand.getContentType() == TransportContentType.PASSENGERS) {
                passengersDemand += demand.getQuantity();
            } else {
                goodsDemand += demand.getQuantity();
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add("Population: " + nearest.getPopulation());
        lines.add("Demand (P/G): " + passengersDemand + " / " + goodsDemand);
        lines.add("Center: (" + nearest.getCenterX() + ", " + nearest.getCenterY() + ")");
        return new InspectorData("City", nearest.getName(), lines, new Color(0.58f, 0.70f, 0.94f, 1f), null);
    }

    private static String prettyResourceName(String resourcePath) {
        String raw = resourcePath;
        int slash = raw.lastIndexOf('/');
        if (slash >= 0 && slash < raw.length() - 1) {
            raw = raw.substring(slash + 1);
        }
        int dot = raw.lastIndexOf('.');
        if (dot > 0) {
            raw = raw.substring(0, dot);
        }
        return raw.replace('-', ' ').replace('_', ' ').trim();
    }

    private static String prettyTileType(TileType type) {
        switch (type) {
            case INDUSTRIAL_FACILITY:
                return "Industrial Facility";
            default:
                String s = type.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                return Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    private static Color colorForTile(TileType type) {
        switch (type) {
            case ROAD:
                return new Color(0.45f, 0.45f, 0.45f, 1f);
            case WATER:
                return new Color(0.20f, 0.56f, 0.95f, 1f);
            case FOREST:
                return new Color(0.10f, 0.42f, 0.12f, 1f);
            case CITY:
                return new Color(0.58f, 0.70f, 0.94f, 1f);
            case INDUSTRIAL_FACILITY:
                return new Color(0.84f, 0.60f, 0.26f, 1f);
            default:
                return new Color(0.30f, 0.52f, 0.20f, 1f);
        }
    }

    private static final class InspectorData {
        private final String type;
        private final String name;
        private final List<String> lines;
        private final Color iconColor;
        private final String iconResourcePath;

        private InspectorData(String type, String name, List<String> lines, Color iconColor, String iconResourcePath) {
            this.type = type;
            this.name = name;
            this.lines = lines;
            this.iconColor = iconColor;
            this.iconResourcePath = iconResourcePath;
        }
    }
}
