package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.team404.tycoon.desktop.assets.AssetPaletteState;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.model.BuildMode;
import com.team404.tycoon.model.EconomyConfig;
import com.team404.tycoon.model.GameMap;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.Tile;
import com.team404.tycoon.model.TileType;

public class HudRenderer {

    // ── menu button ───────────────────────────────────────────────────────────
    private static final Color MENU_BTN_BG     = new Color(0.12f, 0.18f, 0.28f, 0.95f);
    private static final Color MENU_BTN_BORDER = new Color(0.35f, 0.50f, 0.75f, 1f);

    // ── capital health colours ────────────────────────────────────────────────
    private static final Color CAPITAL_HEALTHY = new Color(0.35f, 1f,    0.45f, 1f);
    private static final Color CAPITAL_WARNING = new Color(1f,    0.82f, 0.15f, 1f);
    private static final Color CAPITAL_DANGER  = new Color(1f,    0.28f, 0.18f, 1f);

    // ── stat-chip palette ─────────────────────────────────────────────────────
    private static final Color CHIP_LABEL    = new Color(0.55f, 0.58f, 0.65f, 1f);
    private static final Color CHIP_VEHICLES = new Color(0.55f, 0.85f, 1f,    1f);
    private static final Color CHIP_DEMAND   = new Color(1f,    0.85f, 0.35f, 1f);
    private static final Color CHIP_EARNED   = new Color(0.40f, 0.92f, 0.50f, 1f);
    private static final Color CHIP_SPENT    = new Color(1f,    0.45f, 0.35f, 1f);
    private static final Color CHIP_MAINT_OK = new Color(0.55f, 0.85f, 1f,    1f); // same as vehicles when none due
    private static final Color CHIP_MAINT_DUE= new Color(1f,    0.55f, 0.15f, 1f); // orange when vehicles overdue
    private static final Color DIVIDER_COL   = new Color(1f,    1f,    1f,    0.08f);

    private final OrthographicCamera hudCamera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout;
    private final ShapeRenderer shape;
    private final MinimapOverlay minimapOverlay;
    private final ObjectInspectorOverlay objectInspectorOverlay;

    public HudRenderer() {
        this.hudCamera   = new OrthographicCamera();
        this.batch       = new SpriteBatch();
        this.font        = new BitmapFont();
        this.glyphLayout = new GlyphLayout();
        this.shape       = new ShapeRenderer();
        this.minimapOverlay = new MinimapOverlay();
        this.objectInspectorOverlay = new ObjectInspectorOverlay();
        // Linear filter removes pixelation when the font is scaled up
        font.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        font.setUseIntegerPositions(false);
        font.setColor(Color.WHITE);
    }

    public void render(
            AssetPaletteState palette,
            DecorationTextureCache cache,
            GameState state,
            BuildMode currentMode,
            int speedIndex,
            OrthographicCamera worldCamera,
            int hoverTileX,
            int hoverTileY,
            boolean terrainTooSteep,
            boolean terraformRejected) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float visible  = UiChrome.assetContentWidth(w);
        float maxScroll = palette.maxScrollX(visible, UiChrome.THUMB, UiChrome.ASSET_GAP);
        palette.setScrollX(palette.getScrollX(), maxScroll);

        hudCamera.setToOrtho(false, w, h);
        hudCamera.update();

        shape.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        drawChromeBackground(w, h);
        drawToolButtons(w, h, currentMode, palette);
        drawCategoryDropdownButton(h, palette);
        drawAssetArrows(w, h, palette, maxScroll);

        batch.begin();
        drawAssetStrip(w, h, palette, cache);
        drawStatsBar(w, h, state);
        drawControlsHint(w, state, palette);
        batch.end();
        drawMenuButton(w, h);
        drawSpeedButtons(w, h, speedIndex);
        minimapOverlay.draw(shape, state.getMap(), worldCamera, w, h);
        objectInspectorOverlay.draw(shape, batch, font, glyphLayout, cache, state, hoverTileX, hoverTileY, w, h);
        drawTerrainHoverInfo(w, h, state, hoverTileX, hoverTileY);

        drawAssetSelectionOutline(w, h, palette);
        drawAssetHoverTooltip(w, h, palette);
        drawStatsDividers(w, h);

        if (palette.isDropdownOpen()) {
            drawDropdownPanel(h, palette);
        }

        if (terrainTooSteep) {
            drawTerrainTooSteepWarning(w, h);
        }
        if (terraformRejected) {
            drawTerraformRejectedWarning(w, h);
        }
    }

    private void drawTerrainHoverInfo(float w, float h, GameState state, int hoverTileX, int hoverTileY) {
        GameMap map = state.getMap();
        if (!map.isInBounds(hoverTileX, hoverTileY)) {
            return;
        }
        Tile tile = map.getTile(hoverTileX, hoverTileY);
        String terrainLabel = terrainLabel(tile.getType());
        String info = String.format("Tile (%d,%d)  %s  H:%d", hoverTileX, hoverTileY, terrainLabel, tile.getHeight());

        font.getData().setScale(0.92f);
        glyphLayout.setText(font, info);
        float padX = 9f;
        float padY = 5f;
        float boxW = glyphLayout.width + padX * 2f;
        float boxH = glyphLayout.height + padY * 2f;
        float boxX = w - boxW - 12f;
        float boxY = 12f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.08f, 0.10f, 0.15f, 0.84f);
        shape.rect(boxX, boxY, boxW, boxH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.35f, 0.45f, 0.62f, 0.95f);
        shape.rect(boxX, boxY, boxW, boxH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(0.84f, 0.92f, 1f, 1f);
        font.draw(batch, info, boxX + padX, boxY + padY + glyphLayout.height);
        font.setColor(Color.WHITE);
        font.getData().setScale(1f);
        batch.end();
    }

    private static String terrainLabel(TileType type) {
        switch (type) {
            case EMPTY:
                return "Grass";
            case WATER:
                return "Water";
            case FOREST:
                return "Forest";
            case ROAD:
                return "Road";
            case CITY:
                return "City";
            case INDUSTRIAL_FACILITY:
                return "Industry";
            default:
                return type.name();
        }
    }

    // ── chrome background ─────────────────────────────────────────────────────

    private void drawChromeBackground(float w, float h) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);

        // asset strip (slightly darker)
        shape.setColor(0.06f, 0.07f, 0.10f, 0.94f);
        shape.rect(0, h - UiChrome.ASSET_BAR_HEIGHT, w, UiChrome.ASSET_BAR_HEIGHT);

        // stats bar (slightly lighter so the two bars are visually distinct)
        shape.setColor(0.10f, 0.12f, 0.16f, 0.96f);
        shape.rect(0, h - UiChrome.totalTopHeight(), w, UiChrome.BUILD_BAR_HEIGHT);

        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── stat dividers (drawn after batch so they sit on top of text) ──────────

    private void drawStatsDividers(float w, float h) {
        float barBottom = h - UiChrome.totalTopHeight();
        float barH      = UiChrome.BUILD_BAR_HEIGHT;
        int   chips     = 6; // must match the number of stat chips in drawStatsBar
        float chipW     = (w * 0.75f) / chips;
        float padV      = 8f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(DIVIDER_COL);
        for (int i = 1; i < chips; i++) {
            shape.rect(i * chipW - 0.5f, barBottom + padV, 1f, barH - padV * 2f);
        }
        // thin accent line along the top edge of the stats bar
        shape.setColor(0.25f, 0.30f, 0.40f, 0.70f);
        shape.rect(0, h - UiChrome.totalTopHeight() + barH - 1f, w, 1f);
        // thin separator between asset strip and stats bar
        shape.setColor(0.18f, 0.22f, 0.30f, 0.85f);
        shape.rect(0, h - UiChrome.ASSET_BAR_HEIGHT - 1f, w, 1f);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── stats bar ─────────────────────────────────────────────────────────────

    private void drawStatsBar(float w, float h, GameState state) {
        float barBottom = h - UiChrome.totalTopHeight();
        float barH      = UiChrome.BUILD_BAR_HEIGHT;

        long overdueCount = state.getVehicles().stream()
                .filter(v -> v.isMaintenanceDue() || v.getMaintenanceOverdueFactor() < 1f)
                .count();
        String[] labels = {"Capital", "Vehicles", "Demand", "Earned", "Spent", "Maint"};
        String[] values = {
            "$" + formatMoney(state.getBalance()),
            String.valueOf(state.getVehicles().size()),
            String.valueOf(state.getTransportDemand().size()),
            "$" + formatMoney(state.getLifetimeIncome()),
            "$" + formatMoney(state.getLifetimeExpenses()),
            overdueCount == 0 ? "OK" : overdueCount + " due",
        };
        Color[] valueColors = {
            capitalColor(state.getBalance()),
            CHIP_VEHICLES,
            CHIP_DEMAND,
            CHIP_EARNED,
            CHIP_SPENT,
            overdueCount == 0 ? CHIP_MAINT_OK : CHIP_MAINT_DUE,
        };

        // Five stat chips take up 75 % of the bar width; identity info on the right.
        float chipAreaW = w * 0.75f;
        float chipW     = chipAreaW / labels.length;
        float padX      = 14f;
        float labelY    = barBottom + barH - 8f;
        float valueY    = barBottom + 18f;

        for (int i = 0; i < labels.length; i++) {
            float x = i * chipW + padX;

            font.getData().setScale(0.85f);
            font.setColor(CHIP_LABEL);
            font.draw(batch, labels[i], x, labelY);

            font.getData().setScale(1.25f);
            font.setColor(valueColors[i]);
            font.draw(batch, values[i], x, valueY);
        }

        // Company name, year and difficulty on the right side of the stats bar.
        float infoX = chipAreaW + 16f;
        font.getData().setScale(1.05f);
        font.setColor(1f, 0.92f, 0.55f, 1f);
        font.draw(batch, state.getCompanyName(), infoX, labelY);

        font.getData().setScale(0.88f);
        font.setColor(0.60f, 0.68f, 0.82f, 1f);
        font.draw(batch, "Year " + state.getGameYear(), infoX, valueY + 4f);

        String diffLabel = state.getDifficulty().getLabel();
        Color diffColor = difficultyColor(state.getDifficulty());
        font.getData().setScale(0.82f);
        font.setColor(diffColor);
        glyphLayout.setText(font, diffLabel);
        font.draw(batch, diffLabel, w - glyphLayout.width - 14f, valueY + 4f);

        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
    }

    private static Color difficultyColor(com.team404.tycoon.model.Difficulty d) {
        switch (d) {
            case EASY:   return new Color(0.30f, 0.88f, 0.35f, 1f);
            case HARD:   return new Color(0.95f, 0.30f, 0.25f, 1f);
            default:     return new Color(0.40f, 0.70f, 1.00f, 1f);
        }
    }

    // ── controls hint (tiny, bottom of screen) ────────────────────────────────

    private void drawControlsHint(float w, GameState state, AssetPaletteState palette) {
        String sel = palette.getSelectedPath() == null
                ? "(pick a tile above)"
                : palette.getSelectedPath().replace("resources/", "");
        String lightHint = String.format(
                "Light H:%.1fs V:%.1fs (Z/X C/V R)",
                state.getTrafficLightHorizontalGreenSeconds(),
                state.getTrafficLightVerticalGreenSeconds());
        font.getData().setScale(0.88f);
        font.setColor(0.50f, 0.52f, 0.58f, 1f);
        font.draw(batch,
                "WASD/scroll: pan  |  Ctrl+scroll: zoom  |  LMB: place  |  RMB: remove  |  "
                + "Garage: 1=bus 2=truck  |  " + lightHint + "  |  Selected: " + sel,
                10f, 16f);
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
    }

    // ── terrain too steep toast ───────────────────────────────────────────────

    private static final Color STEEP_BG   = new Color(0.75f, 0.10f, 0.05f, 0.90f);
    private static final Color STEEP_TEXT = new Color(1f,    0.92f, 0.85f, 1f);

    private void drawTerrainTooSteepWarning(float w, float h) {
        String msg = "Terrain too steep — road cannot be built here!";
        float panelH = 38f;
        float panelY = UiChrome.totalTopHeight() + 6f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(STEEP_BG);
        shape.rect(0, h - panelY - panelH, w, panelH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(1.1f);
        glyphLayout.setText(font, msg);
        font.setColor(STEEP_TEXT);
        font.draw(batch, msg, w / 2f - glyphLayout.width / 2f, h - panelY - panelH + panelH * 0.65f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── terraform rejected toast ──────────────────────────────────────────────

    private static final Color TERRAFORM_REJECT_BG   = new Color(0.45f, 0.25f, 0.05f, 0.90f);
    private static final Color TERRAFORM_REJECT_TEXT = new Color(1f,    0.92f, 0.70f, 1f);

    private void drawTerraformRejectedWarning(float w, float h) {
        String msg = "Cannot terraform here! (invalid tile or insufficient funds)";
        float panelH = 38f;
        float panelY = UiChrome.totalTopHeight() + 6f + 44f; // below the steep warning

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(TERRAFORM_REJECT_BG);
        shape.rect(0, h - panelY - panelH, w, panelH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(1.1f);
        glyphLayout.setText(font, msg);
        font.setColor(TERRAFORM_REJECT_TEXT);
        font.draw(batch, msg, w / 2f - glyphLayout.width / 2f, h - panelY - panelH + panelH * 0.65f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── asset strip ───────────────────────────────────────────────────────────

    private void drawAssetStrip(float w, float h, AssetPaletteState palette, DecorationTextureCache cache) {
        float yBottom    = h - UiChrome.ASSET_BAR_HEIGHT + (UiChrome.ASSET_BAR_HEIGHT - UiChrome.THUMB) / 2f;
        float x          = UiChrome.assetContentLeftX() - palette.getScrollX();
        float leftBound  = UiChrome.assetContentLeftX();
        float rightBound = UiChrome.assetContentRightX(w);
        for (String path : palette.getVisiblePaths()) {
            if (x + UiChrome.THUMB > leftBound && x < rightBound) {
                boolean sel = path.equals(palette.getSelectedPath());
                Texture tex = cache.get(path);
                float tw    = tex.getWidth();
                float th    = tex.getHeight();
                float scale = UiChrome.THUMB / Math.max(tw, th);
                float dw    = tw * scale;
                float dh    = th * scale;
                float drawY = yBottom + (UiChrome.THUMB - dh) / 2f;
                batch.setColor(sel ? Color.YELLOW : Color.WHITE);
                batch.draw(tex, x + (UiChrome.THUMB - dw) / 2f, drawY, dw, dh);
                batch.setColor(Color.WHITE);
            }
            x += UiChrome.THUMB + UiChrome.ASSET_GAP;
        }
    }

    // ── asset selection outline ───────────────────────────────────────────────

    private void drawAssetSelectionOutline(float w, float h, AssetPaletteState palette) {
        String sel = palette.getSelectedPath();
        if (sel == null) {
            return;
        }
        int idx = palette.getVisiblePaths().indexOf(sel);
        if (idx < 0) {
            return;
        }
        float cell  = UiChrome.THUMB + UiChrome.ASSET_GAP;
        float x     = UiChrome.assetContentLeftX() - palette.getScrollX() + idx * cell;
        float rightBound = UiChrome.assetContentRightX(w);
        if (x + UiChrome.THUMB < UiChrome.assetContentLeftX() || x > rightBound) {
            return;
        }
        float yBottom = h - UiChrome.ASSET_BAR_HEIGHT + (UiChrome.ASSET_BAR_HEIGHT - UiChrome.THUMB) / 2f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.2f, 0.95f, 1f, 1f);
        shape.rect(x, yBottom, UiChrome.THUMB, UiChrome.THUMB);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── hover tooltip ─────────────────────────────────────────────────────────

    private void drawAssetHoverTooltip(float w, float h, AssetPaletteState palette) {
        String hovered = palette.getHoveredPath();
        if (hovered == null) {
            return;
        }
        int idx = palette.getVisiblePaths().indexOf(hovered);
        if (idx < 0) {
            return;
        }
        float cell       = UiChrome.THUMB + UiChrome.ASSET_GAP;
        float x          = UiChrome.assetContentLeftX() - palette.getScrollX() + idx * cell;
        float yBottom    = h - UiChrome.ASSET_BAR_HEIGHT + (UiChrome.ASSET_BAR_HEIGHT - UiChrome.THUMB) / 2f;
        float leftBound  = UiChrome.assetContentLeftX();
        float rightBound = UiChrome.assetContentRightX(w);
        if (x + UiChrome.THUMB < leftBound || x > rightBound) {
            return;
        }

        String label = palette.displayNameFor(hovered);
        if (EconomyConfig.isRoadDecoration(hovered)) {
            label = label + " [$" + EconomyConfig.ROAD_DECORATION_COST + "]";
        }
        glyphLayout.setText(font, label);
        float padX = 8f;
        float padY = 5f;
        float boxW = glyphLayout.width  + padX * 2f;
        float boxH = glyphLayout.height + padY * 2f;
        float boxX = x + UiChrome.THUMB * 0.5f - boxW * 0.5f;
        float minX = UiChrome.ASSET_PAD + UiChrome.ARROW_SIZE + 2f;
        float maxX = w - UiChrome.ASSET_PAD - UiChrome.ARROW_SIZE - boxW - 2f;
        boxX = Math.max(minX, Math.min(maxX, boxX));
        float boxY = yBottom - boxH - 6f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.92f, 0.92f, 0.62f, 0.97f);
        shape.rect(boxX, boxY, boxW, boxH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.18f, 0.18f, 0.06f, 1f);
        shape.rect(boxX, boxY, boxW, boxH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.setColor(0.12f, 0.12f, 0.08f, 1f);
        font.draw(batch, label, boxX + padX, boxY + padY + glyphLayout.height);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── category dropdown ─────────────────────────────────────────────────────

    private void drawCategoryDropdownButton(float h, AssetPaletteState palette) {
        float btnX   = UiChrome.ASSET_PAD;
        float btnW   = UiChrome.DROPDOWN_BTN_W;
        float btnH   = UiChrome.DROPDOWN_BTN_H;
        float btnY   = h - UiChrome.ASSET_BAR_HEIGHT + (UiChrome.ASSET_BAR_HEIGHT - btnH) / 2f;

        String label = categoryLabel(palette.getSelectedCategory()) + "  v";

        boolean open = palette.isDropdownOpen();
        Color bg     = open
                ? new Color(0.22f, 0.28f, 0.38f, 1f)
                : new Color(0.12f, 0.15f, 0.22f, 0.97f);
        Color border = open
                ? new Color(0.45f, 0.70f, 1f, 1f)
                : new Color(0.28f, 0.42f, 0.65f, 1f);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(bg);
        shape.rect(btnX, btnY, btnW, btnH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(border);
        shape.rect(btnX, btnY, btnW, btnH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(0.90f);
        font.setColor(open ? new Color(0.65f, 0.88f, 1f, 1f) : new Color(0.80f, 0.88f, 1f, 1f));
        glyphLayout.setText(font, label);
        font.draw(batch, label,
                btnX + (btnW - glyphLayout.width) / 2f,
                btnY + (btnH + glyphLayout.height) / 2f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private void drawDropdownPanel(float h, AssetPaletteState palette) {
        float panelX    = UiChrome.ASSET_PAD;
        float panelW    = UiChrome.DROPDOWN_BTN_W;
        float itemH     = UiChrome.DROPDOWN_ITEM_H;
        int   count     = AssetPaletteState.CATEGORIES.length;
        float panelH    = count * itemH;
        float panelTop  = h - UiChrome.ASSET_BAR_HEIGHT;         // LibGDX top of panel
        float panelBot  = panelTop - panelH;                      // LibGDX bottom of panel

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // background + border
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.09f, 0.12f, 0.18f, 0.98f);
        shape.rect(panelX, panelBot, panelW, panelH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.28f, 0.42f, 0.65f, 1f);
        shape.rect(panelX, panelBot, panelW, panelH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // item text
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(0.92f);
        String active = palette.getSelectedCategory();
        for (int i = 0; i < count; i++) {
            float itemBot = panelTop - (i + 1) * itemH;
            boolean sel = AssetPaletteState.CATEGORIES[i].equals(active);
            font.setColor(sel
                    ? new Color(0.35f, 0.92f, 1f, 1f)
                    : new Color(0.72f, 0.78f, 0.90f, 1f));
            glyphLayout.setText(font, AssetPaletteState.CATEGORY_LABELS[i]);
            font.draw(batch, AssetPaletteState.CATEGORY_LABELS[i],
                    panelX + (panelW - glyphLayout.width) / 2f,
                    itemBot + (itemH + glyphLayout.height) / 2f);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();

        // divider lines between items
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.25f, 0.32f, 0.45f, 0.6f);
        for (int i = 1; i < count; i++) {
            float lineY = panelTop - i * itemH;
            shape.rect(panelX + 6f, lineY, panelW - 12f, 1f);
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private static String categoryLabel(String category) {
        if (category == null) {
            return "All";
        }
        for (int i = 0; i < AssetPaletteState.CATEGORIES.length; i++) {
            if (AssetPaletteState.CATEGORIES[i].equals(category)) {
                return AssetPaletteState.CATEGORY_LABELS[i];
            }
        }
        return "All";
    }

    // ── scroll arrows ─────────────────────────────────────────────────────────

    private void drawAssetArrows(float w, float h, AssetPaletteState palette, float maxScroll) {
        float barBottom = h - UiChrome.ASSET_BAR_HEIGHT;
        float y         = barBottom + (UiChrome.ASSET_BAR_HEIGHT - UiChrome.ARROW_SIZE) * 0.5f;
        float leftX     = UiChrome.leftArrowX();
        // Right arrow sits to the left of the menu button, matching the hit-test in UiChrome.
        float rightX    = w - UiChrome.ASSET_PAD - UiChrome.MENU_BTN_W - UiChrome.ASSET_PAD - UiChrome.ARROW_SIZE;

        boolean canScrollLeft  = palette.getScrollX() > 0.5f;
        boolean canScrollRight = palette.getScrollX() < maxScroll - 0.5f;

        drawArrowButton(leftX,  y, true,  canScrollLeft);
        drawArrowButton(rightX, y, false, canScrollRight);
    }

    private void drawArrowButton(float x, float y, boolean left, boolean enabled) {
        Color bg = enabled ? new Color(0.15f, 0.18f, 0.22f, 0.95f) : new Color(0.10f, 0.10f, 0.10f, 0.45f);
        Color fg = enabled ? new Color(0.90f, 0.95f, 1f,    1f)    : new Color(0.45f, 0.45f, 0.45f, 0.9f);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(bg);
        shape.rect(x, y, UiChrome.ARROW_SIZE, UiChrome.ARROW_SIZE);
        shape.setColor(fg);
        float cx   = x + UiChrome.ARROW_SIZE * 0.5f;
        float cy   = y + UiChrome.ARROW_SIZE * 0.5f;
        float triW = UiChrome.ARROW_SIZE * 0.28f;
        float triH = UiChrome.ARROW_SIZE * 0.30f;
        if (left) {
            shape.triangle(cx + triW * 0.5f, cy + triH, cx - triW, cy, cx + triW * 0.5f, cy - triH);
        } else {
            shape.triangle(cx - triW * 0.5f, cy + triH, cx + triW, cy, cx - triW * 0.5f, cy - triH);
        }
        shape.end();
    }

    // ── terrain tool buttons ─────────────────────────────────────────────────

    private static final BuildMode[] TERRAIN_MODES = {
        BuildMode.WATER, BuildMode.FOREST, BuildMode.DEMOLISH,
        BuildMode.RAISE_TERRAIN, BuildMode.LOWER_TERRAIN
    };

    private void drawToolButtons(float w, float h, BuildMode activeMode, AssetPaletteState palette) {
        float barBottom = h - UiChrome.ASSET_BAR_HEIGHT;
        float btnY = barBottom + (UiChrome.ASSET_BAR_HEIGHT - UiChrome.TOOL_BTN_SIZE) / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < TERRAIN_MODES.length; i++) {
            float btnX = UiChrome.toolButtonsStartX() + i * (UiChrome.TOOL_BTN_SIZE + UiChrome.TOOL_BTN_GAP);
            shape.setColor(toolBgColor(TERRAIN_MODES[i], TERRAIN_MODES[i] == activeMode));
            shape.rect(btnX, btnY, UiChrome.TOOL_BTN_SIZE, UiChrome.TOOL_BTN_SIZE);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < TERRAIN_MODES.length; i++) {
            if (TERRAIN_MODES[i] == activeMode) {
                float btnX = UiChrome.toolButtonsStartX() + i * (UiChrome.TOOL_BTN_SIZE + UiChrome.TOOL_BTN_GAP);
                shape.setColor(Color.YELLOW);
                shape.rect(btnX, btnY, UiChrome.TOOL_BTN_SIZE, UiChrome.TOOL_BTN_SIZE);
            }
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(0.88f);
        for (int i = 0; i < TERRAIN_MODES.length; i++) {
            float btnX = UiChrome.toolButtonsStartX() + i * (UiChrome.TOOL_BTN_SIZE + UiChrome.TOOL_BTN_GAP);
            String label = TERRAIN_MODES[i].getLabel();
            glyphLayout.setText(font, label);
            float tx = btnX + (UiChrome.TOOL_BTN_SIZE - glyphLayout.width) / 2f;
            float ty = btnY + (UiChrome.TOOL_BTN_SIZE + glyphLayout.height) / 2f;
            font.setColor(Color.WHITE);
            font.draw(batch, label, tx, ty);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    private static Color toolBgColor(BuildMode mode, boolean active) {
        switch (mode) {
            case WATER:         return active ? new Color(0.10f, 0.35f, 0.80f, 1f) : new Color(0.06f, 0.22f, 0.50f, 1f);
            case FOREST:        return active ? new Color(0.12f, 0.55f, 0.18f, 1f) : new Color(0.08f, 0.32f, 0.10f, 1f);
            case DEMOLISH:      return active ? new Color(0.75f, 0.15f, 0.10f, 1f) : new Color(0.45f, 0.08f, 0.06f, 1f);
            case RAISE_TERRAIN: return active ? new Color(0.75f, 0.55f, 0.10f, 1f) : new Color(0.45f, 0.32f, 0.06f, 1f);
            case LOWER_TERRAIN: return active ? new Color(0.30f, 0.20f, 0.55f, 1f) : new Color(0.18f, 0.12f, 0.35f, 1f);
            default:            return Color.DARK_GRAY;
        }
    }

    // ── in-game menu button ───────────────────────────────────────────────────

    private void drawMenuButton(float w, float h) {
        float bx = UiChrome.menuButtonX(w);
        float by = UiChrome.menuButtonLibGdxY(h);   // correct LibGDX y-up coordinate
        float bw = UiChrome.MENU_BTN_W;
        float bh = UiChrome.MENU_BTN_H;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Filled background with a warm accent so the button stands out from the dark strip.
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(MENU_BTN_BG);
        shape.rect(bx, by, bw, bh);
        shape.end();

        // Bright border
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(MENU_BTN_BORDER);
        shape.rect(bx, by, bw, bh);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(1.0f);
        font.setColor(0.95f, 0.95f, 1f, 1f);
        String label = "\u2630 Menu";
        glyphLayout.setText(font, label);
        font.draw(batch, label, bx + (bw - glyphLayout.width) / 2f, by + (bh + glyphLayout.height) / 2f);
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── speed control buttons ─────────────────────────────────────────────────

    private static final String[] SPEED_LABELS = {"||", "1\u00d7", "2\u00d7", "4\u00d7"};
    private static final Color SPEED_ACTIVE_BG   = new Color(0.20f, 0.40f, 0.20f, 1f);
    private static final Color SPEED_INACTIVE_BG = new Color(0.10f, 0.12f, 0.18f, 0.95f);
    private static final Color SPEED_BORDER      = new Color(0.30f, 0.45f, 0.30f, 1f);

    private void drawSpeedButtons(float w, float h, int activeSpeedIndex) {
        float barBottom = h - UiChrome.totalTopHeight();
        float btnH = UiChrome.SPEED_BTN_H;
        float btnY = barBottom + (UiChrome.BUILD_BAR_HEIGHT - btnH) / 2f;
        float startX = UiChrome.speedButtonsStartX(w);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < UiChrome.SPEED_BTN_COUNT; i++) {
            float bx = startX + i * (UiChrome.SPEED_BTN_W + UiChrome.SPEED_BTN_GAP);
            shape.setColor(i == activeSpeedIndex ? SPEED_ACTIVE_BG : SPEED_INACTIVE_BG);
            shape.rect(bx, btnY, UiChrome.SPEED_BTN_W, btnH);
        }
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < UiChrome.SPEED_BTN_COUNT; i++) {
            float bx = startX + i * (UiChrome.SPEED_BTN_W + UiChrome.SPEED_BTN_GAP);
            shape.setColor(i == activeSpeedIndex ? new Color(0.40f, 1f, 0.40f, 1f) : SPEED_BORDER);
            shape.rect(bx, btnY, UiChrome.SPEED_BTN_W, btnH);
        }
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        font.getData().setScale(0.88f);
        for (int i = 0; i < UiChrome.SPEED_BTN_COUNT; i++) {
            float bx = startX + i * (UiChrome.SPEED_BTN_W + UiChrome.SPEED_BTN_GAP);
            font.setColor(i == activeSpeedIndex ? new Color(0.50f, 1f, 0.50f, 1f) : new Color(0.70f, 0.75f, 0.85f, 1f));
            glyphLayout.setText(font, SPEED_LABELS[i]);
            font.draw(batch, SPEED_LABELS[i],
                    bx + (UiChrome.SPEED_BTN_W - glyphLayout.width) / 2f,
                    btnY + (btnH + glyphLayout.height) / 2f);
        }
        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Color capitalColor(long balance) {
        long warn   = EconomyConfig.INITIAL_CAPITAL / 4;
        long danger = EconomyConfig.INITIAL_CAPITAL / 10;
        if (balance >= warn) {
            return CAPITAL_HEALTHY;
        }
        if (balance >= danger) {
            return CAPITAL_WARNING;
        }
        return CAPITAL_DANGER;
    }

    private static String formatMoney(long amount) {
        return String.format("%,d", amount);
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
        shape.dispose();
    }

    public MinimapOverlay getMinimapOverlay() {
        return minimapOverlay;
    }

    public ObjectInspectorOverlay getObjectInspectorOverlay() {
        return objectInspectorOverlay;
    }
}
