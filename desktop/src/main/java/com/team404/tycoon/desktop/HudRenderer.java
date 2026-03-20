package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.team404.tycoon.model.BuildMode;

public class HudRenderer {

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

    public void render(BuildMode currentMode) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        hudCamera.setToOrtho(false, w, h);
        hudCamera.update();

        shape.setProjectionMatrix(hudCamera.combined);
        batch.setProjectionMatrix(hudCamera.combined);

        drawToolbarBackground(w, h);

        batch.begin();
        drawToolbarText(w, h, currentMode);
        drawControlsText(w, h);
        batch.end();
    }

    private void drawToolbarBackground(float w, float h) {
        float barH = 40f;
        float barY = h - barH;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.7f);
        shape.rect(0, barY, w, barH);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawToolbarText(float w, float h, BuildMode currentMode) {
        float barH = 40f;
        float barY = h - barH;

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
        font.draw(batch, "Tool: " + currentMode.getLabel(), w - 160f, barY + 26f);
    }

    private void drawControlsText(float w, float h) {
        font.setColor(new Color(1f, 1f, 1f, 0.5f));
        font.draw(batch,
                "WASD/Arrows: pan | Two-finger scroll: pan | Ctrl+Scroll: zoom | 1-4: tools | LMB: build | RMB: demolish",
                10f, 20f);
        font.setColor(Color.WHITE);
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
        shape.dispose();
    }
}