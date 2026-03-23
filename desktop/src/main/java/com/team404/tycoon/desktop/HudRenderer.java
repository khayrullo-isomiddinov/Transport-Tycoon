package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.team404.tycoon.desktop.assets.AssetPaletteState;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.model.BuildMode;

public class HudRenderer {

    private static final float PAD = 8f;
    private static final float THUMB = 52f;
    private static final float GAP = 6f;

    private final OrthographicCamera hudCamera;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final ShapeRenderer shape;

    public HudRenderer() {
        this.hudCamera = new OrthographicCamera();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.shape = new ShapeRenderer();
        font.setColor(Color.WHITE);
    }

    public void render(BuildMode currentMode, AssetPaletteState palette, DecorationTextureCache cache) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        hudCamera.setToOrtho(false, w, h);
        hudCamera.update();

        shape.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        drawChromeBackground(w, h);

        batch.begin();
        drawAssetStrip(w, h, palette, cache);
        drawBuildToolbarText(w, h, currentMode);
        drawControlsText(w, h, palette);
        batch.end();

        drawAssetSelectionOutline(w, h, palette);
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
        float cell = THUMB + GAP;
        float x = PAD - palette.getScrollX() + idx * cell;
        if (x + THUMB < 0 || x > w) {
            return;
        }
        float yBottom = h - UiChrome.ASSET_BAR_HEIGHT + PAD;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.2f, 0.95f, 1f, 1f);
        shape.rect(x, yBottom, THUMB, THUMB);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawAssetStrip(float w, float h, AssetPaletteState palette, DecorationTextureCache cache) {
        float yBottom = h - UiChrome.ASSET_BAR_HEIGHT + PAD;
        float x = PAD - palette.getScrollX();
        for (String path : palette.getResourcePaths()) {
            if (x + THUMB > 0 && x < w) {
                boolean sel = path.equals(palette.getSelectedPath());
                Texture tex = cache.get(path);
                float tw = tex.getWidth();
                float th = tex.getHeight();
                float scale = THUMB / Math.max(tw, th);
                float dw = tw * scale;
                float dh = th * scale;
                float drawY = yBottom + (THUMB - dh) / 2f;
                batch.setColor(sel ? Color.YELLOW : Color.WHITE);
                batch.draw(tex, x + (THUMB - dw) / 2f, drawY, dw, dh);
                batch.setColor(Color.WHITE);
            }
            x += THUMB + GAP;
        }
    }

    private void drawBuildToolbarText(float w, float h, BuildMode currentMode) {
        float barY = h - UiChrome.totalTopHeight();

        BuildMode[] modes = BuildMode.values();
        float xOffset = 16f;

        for (int i = 0; i < modes.length; i++) {
            BuildMode mode = modes[i];
            String label = "[" + (i + 1) + "] " + mode.getLabel();
            font.setColor(mode == currentMode ? Color.YELLOW : Color.LIGHT_GRAY);
            font.draw(batch, label, xOffset, barY + 26f);
            xOffset += 120f;
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "Tool: " + currentMode.getLabel(), w - 200f, barY + 26f);
    }

    private void drawControlsText(float w, float h, AssetPaletteState palette) {
        font.setColor(new Color(1f, 1f, 1f, 0.55f));
        String sel = palette.getSelectedPath() == null
                ? "(pick PNG above)"
                : palette.getSelectedPath().replace("resources/", "");
        font.draw(batch,
                "WASD: pan | Ctrl+scroll: zoom | scroll: pan | LMB: place | RMB: remove decal / demolish | PNG: "
                        + sel,
                10f, 18f);
        font.setColor(Color.WHITE);
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
        shape.dispose();
    }
}
