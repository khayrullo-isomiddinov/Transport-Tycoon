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
import com.team404.tycoon.desktop.assets.AssetPaletteState;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.model.GameState;

public class HudRenderer {

    private final OrthographicCamera hudCamera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout;
    private final ShapeRenderer shape;

    public HudRenderer() {
        this.hudCamera = new OrthographicCamera();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.glyphLayout = new GlyphLayout();
        this.shape = new ShapeRenderer();
        font.setColor(Color.WHITE);
    }

    public void render(AssetPaletteState palette, DecorationTextureCache cache, GameState state) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float visible = UiChrome.assetContentWidth(w);
        float maxScroll = palette.maxScrollX(visible, UiChrome.THUMB, UiChrome.ASSET_GAP);
        palette.setScrollX(palette.getScrollX(), maxScroll);

        hudCamera.setToOrtho(false, w, h);
        hudCamera.update();

        shape.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        drawChromeBackground(w, h);
        drawAssetArrows(w, h, palette, maxScroll);

        batch.begin();
        drawAssetStrip(w, h, palette, cache);
        drawControlsText(w, h, palette, state);
        batch.end();

        drawAssetSelectionOutline(w, h, palette);
        drawAssetHoverTooltip(w, h, palette);
    }

    private void drawChromeBackground(float w, float h) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.82f);
        shape.rect(0, h - UiChrome.ASSET_BAR_HEIGHT, w, UiChrome.ASSET_BAR_HEIGHT);
        shape.setColor(0f, 0f, 0f, 0.72f);
        shape.rect(0, h - UiChrome.totalTopHeight(), w, UiChrome.BUILD_BAR_HEIGHT);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    /** Matches {@link AssetPaletteInputProcessor} cell layout for hit testing. */
    private void drawAssetSelectionOutline(float w, float h, AssetPaletteState palette) {
        String sel = palette.getSelectedPath();
        if (sel == null) {
            return;
        }
        int idx = palette.getResourcePaths().indexOf(sel);
        if (idx < 0) {
            return;
        }
        float cell = UiChrome.THUMB + UiChrome.ASSET_GAP;
        float x = UiChrome.assetContentLeftX() - palette.getScrollX() + idx * cell;
        float rightBound = UiChrome.assetContentRightX(w);
        if (x + UiChrome.THUMB < UiChrome.assetContentLeftX() || x > rightBound) {
            return;
        }
        float yBottom = h - UiChrome.ASSET_BAR_HEIGHT + UiChrome.ASSET_PAD;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.2f, 0.95f, 1f, 1f);
        shape.rect(x, yBottom, UiChrome.THUMB, UiChrome.THUMB);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawAssetStrip(float w, float h, AssetPaletteState palette, DecorationTextureCache cache) {
        float yBottom = h - UiChrome.ASSET_BAR_HEIGHT + UiChrome.ASSET_PAD;
        float x = UiChrome.assetContentLeftX() - palette.getScrollX();
        float leftBound = UiChrome.assetContentLeftX();
        float rightBound = UiChrome.assetContentRightX(w);
        for (String path : palette.getResourcePaths()) {
            if (x + UiChrome.THUMB > leftBound && x < rightBound) {
                boolean sel = path.equals(palette.getSelectedPath());
                Texture tex = cache.get(path);
                float tw = tex.getWidth();
                float th = tex.getHeight();
                float scale = UiChrome.THUMB / Math.max(tw, th);
                float dw = tw * scale;
                float dh = th * scale;
                float drawY = yBottom + (UiChrome.THUMB - dh) / 2f;
                batch.setColor(sel ? Color.YELLOW : Color.WHITE);
                batch.draw(tex, x + (UiChrome.THUMB - dw) / 2f, drawY, dw, dh);
                batch.setColor(Color.WHITE);
            }
            x += UiChrome.THUMB + UiChrome.ASSET_GAP;
        }
    }

    private void drawAssetHoverTooltip(float w, float h, AssetPaletteState palette) {
        String hovered = palette.getHoveredPath();
        if (hovered == null) {
            return;
        }
        int idx = palette.getResourcePaths().indexOf(hovered);
        if (idx < 0) {
            return;
        }
        float cell = UiChrome.THUMB + UiChrome.ASSET_GAP;
        float x = UiChrome.assetContentLeftX() - palette.getScrollX() + idx * cell;
        float yBottom = h - UiChrome.ASSET_BAR_HEIGHT + UiChrome.ASSET_PAD;
        float leftBound = UiChrome.assetContentLeftX();
        float rightBound = UiChrome.assetContentRightX(w);
        if (x + UiChrome.THUMB < leftBound || x > rightBound) {
            return;
        }

        String label = palette.displayNameFor(hovered);
        glyphLayout.setText(font, label);
        float padX = 8f;
        float padY = 5f;
        float boxW = glyphLayout.width + padX * 2f;
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

    private void drawAssetArrows(float w, float h, AssetPaletteState palette, float maxScroll) {
        float barBottom = h - UiChrome.ASSET_BAR_HEIGHT;
        float y = barBottom + (UiChrome.ASSET_BAR_HEIGHT - UiChrome.ARROW_SIZE) * 0.5f;
        float leftX = UiChrome.ASSET_PAD;
        float rightX = w - UiChrome.ASSET_PAD - UiChrome.ARROW_SIZE;

        boolean canScrollLeft = palette.getScrollX() > 0.5f;
        boolean canScrollRight = palette.getScrollX() < maxScroll - 0.5f;

        drawArrowButton(leftX, y, true, canScrollLeft);
        drawArrowButton(rightX, y, false, canScrollRight);
    }

    private void drawArrowButton(float x, float y, boolean left, boolean enabled) {
        Color bg = enabled ? new Color(0.15f, 0.18f, 0.22f, 0.95f) : new Color(0.10f, 0.10f, 0.10f, 0.45f);
        Color fg = enabled ? new Color(0.90f, 0.95f, 1f, 1f) : new Color(0.45f, 0.45f, 0.45f, 0.9f);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(bg);
        shape.rect(x, y, UiChrome.ARROW_SIZE, UiChrome.ARROW_SIZE);
        shape.setColor(fg);
        float cx = x + UiChrome.ARROW_SIZE * 0.5f;
        float cy = y + UiChrome.ARROW_SIZE * 0.5f;
        float triW = UiChrome.ARROW_SIZE * 0.28f;
        float triH = UiChrome.ARROW_SIZE * 0.30f;
        if (left) {
            shape.triangle(cx + triW * 0.5f, cy + triH, cx - triW, cy, cx + triW * 0.5f, cy - triH);
        } else {
            shape.triangle(cx - triW * 0.5f, cy + triH, cx + triW, cy, cx - triW * 0.5f, cy - triH);
        }
        shape.end();
    }

    private void drawControlsText(float w, float h, AssetPaletteState palette, GameState state) {
        font.setColor(new Color(1f, 1f, 1f, 0.55f));
        String sel = palette.getSelectedPath() == null
                ? "(pick PNG above)"
                : palette.getSelectedPath().replace("resources/", "");
        font.draw(batch,
                "WASD: pan | Ctrl+scroll: zoom | scroll: pan | pick sprite above then LMB place / RMB remove | PNG: "
                        + sel,
                10f, 18f);
        String transportLine = "Vehicles: " + state.getVehicles().size()
                + " | Pending demand: " + state.getTransportDemand().size()
                + " | Balance: " + state.getBalance()
                + " | Income: " + state.getLifetimeIncome()
                + " | Expenses: " + state.getLifetimeExpenses();
        font.draw(batch, transportLine, 10f, 38f);
        font.setColor(Color.WHITE);
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
        shape.dispose();
    }
}
