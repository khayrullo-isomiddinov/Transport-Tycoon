package com.team404.tycoon.desktop;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.team404.tycoon.model.Difficulty;
import com.team404.tycoon.model.GameState;

/**
 * Full-screen menu: shows the title screen before play and a stats/game-over
 * screen when the player's company goes bankrupt.
 *
 * Features
 *  1. Animated title with pulsing glow
 *  2. Company name text input with blinking cursor
 *  3. Difficulty selector (Easy / Normal / Hard) showing starting capital
 *  4. PLAY button
 *  5. HOW TO PLAY button opening a controls overlay
 *  6. QUIT button
 *  7. Scrolling tips bar along the bottom
 *  8. Animated background vehicles crossing road lanes
 *  9. Game-over stats screen (company, difficulty, year, income, costs, peak balance)
 * 10. PLAY AGAIN button on the game-over screen
 * 11. Company name & year displayed in HUD during play (via GameState)
 * 12. Difficulty badge in HUD (via GameState)
 */
public class MenuScreen extends InputAdapter {

    // ── modes ──────────────────────────────────────────────────────────────────
    private enum Mode { MAIN_MENU, PAUSED, GAME_OVER }
    private Mode mode = Mode.MAIN_MENU;

    // ── intent signals ────────────────────────────────────────────────────────
    private boolean playRequested   = false;
    private boolean quitRequested   = false;
    private boolean resumeRequested = false;

    // ── player choices ────────────────────────────────────────────────────────
    private String      companyName        = "My Company";
    private Difficulty  selectedDifficulty = Difficulty.NORMAL;
    private int         selectedMapSize    = 128;

    // ── text-field state ──────────────────────────────────────────────────────
    private boolean nameFieldActive = false;
    private float   cursorBlink     = 0f;

    // ── help overlay ──────────────────────────────────────────────────────────
    private boolean showHelp = false;

    // ── animation ────────────────────────────────────────────────────────────
    private float elapsed   = 0f;
    private float tipOffset = 0f;

    // ── background vehicles ───────────────────────────────────────────────────
    // Each entry: { x, y, speed, colorR, colorG, colorB }
    private final float[][] bgVehicles = {
        {   0f, 0.20f, 120f, 0.90f, 0.30f, 0.30f },
        { 300f, 0.35f,  80f, 0.30f, 0.70f, 0.90f },
        { 600f, 0.50f, 100f, 0.90f, 0.80f, 0.20f },
        { 200f, 0.65f,  60f, 0.40f, 0.90f, 0.40f },
        { 800f, 0.78f,  90f, 0.80f, 0.40f, 0.90f },
        { 100f, 0.42f, 110f, 0.90f, 0.55f, 0.25f },
        { 500f, 0.58f,  70f, 0.35f, 0.85f, 0.75f },
        { 900f, 0.30f,  95f, 0.70f, 0.70f, 0.30f },
    };

    // ── game-over stats (filled when entering GAME_OVER mode) ─────────────────
    private String goCompany;
    private String goDifficulty;
    private int    goYear;
    private long   goIncome;
    private long   goExpenses;
    private long   goPeak;
    private int    goVehicles;

    // ── rendering ─────────────────────────────────────────────────────────────
    private final OrthographicCamera camera;
    private final SpriteBatch        batch;
    private final BitmapFont         font;
    private final GlyphLayout        layout;
    private final ShapeRenderer      shape;

    // ── button rects: { x, y, w, h } in LibGDX bottom-left coords ────────────
    private float[] rectPlay      = new float[4];
    private float[] rectHelp      = new float[4];
    private float[] rectQuit      = new float[4];
    private float[] rectPlayAgain = new float[4];
    private float[] rectGoQuit    = new float[4];
    private float[] rectHelpClose = new float[4];
    private float[] rectEasy      = new float[4];
    private float[] rectNormal    = new float[4];
    private float[] rectHard      = new float[4];
    private float[] rectNameField = new float[4];
    // map size buttons
    private static final int[] MAP_SIZES = {64, 128, 256, 512, 1024};
    private final float[][] rectSizes = new float[MAP_SIZES.length][4];
    // pause menu
    private float[] rectResume    = new float[4];
    private float[] rectNewGame   = new float[4];
    private float[] rectPauseHelp = new float[4];
    private float[] rectPauseQuit = new float[4];
    private int     mouseX, mouseY; // GLFW coords (y-down)

    private static final String[] TIPS = {
        "TIP: Connect towns with roads, then buy vehicles at garages.",
        "TIP: Trucks carry goods, buses carry passengers.",
        "TIP: Traffic lights slow vehicles — use Z/X and C/V to tune timing.",
        "TIP: Vehicles stop for red lights and for each other — plan your routes.",
        "TIP: Garages must be next to a road tile for vehicles to be purchased.",
        "TIP: Selling old vehicles returns cash — useful in a tight spot.",
        "TIP: Maintenance costs are charged automatically every 2 minutes.",
        "TIP: Use Ctrl+Scroll to zoom, WASD/arrows to pan the map.",
        "TIP: Hard mode starts you with half the capital — every decision counts!",
        "TIP: The year advances every 60 seconds. How long can you last?",
    };
    private int   tipIndex  = 0;
    private float tipDelay  = 0f;

    private static final String[] HELP_LINES = {
        "WASD / Arrow keys     Pan the map",
        "Ctrl + Scroll         Zoom in / out",
        "Left click            Place selected tile",
        "Right click           Remove tile / sell vehicle",
        "Click garage (1)      Buy a bus",
        "Click garage (2)      Buy a truck",
        "Z / X                 Horizontal light −/+ 0.5s",
        "C / V                 Vertical light −/+ 0.5s",
        "R                     Reset light timings",
        "",
        "Goal: haul passengers and goods between towns.",
        "Don't let running costs push you into bankruptcy!",
    };

    public MenuScreen() {
        camera = new OrthographicCamera();
        batch  = new SpriteBatch();
        font   = new BitmapFont();
        layout = new GlyphLayout();
        shape  = new ShapeRenderer();
        font.getRegion().getTexture().setFilter(TextureFilter.Linear, TextureFilter.Linear);
        font.setUseIntegerPositions(false);
    }

    // ── public API ────────────────────────────────────────────────────────────

    public void setGameOver(GameState state) {
        goCompany    = state.getCompanyName();
        goDifficulty = state.getDifficulty().getLabel();
        goYear       = state.getGameYear();
        goIncome     = state.getLifetimeIncome();
        goExpenses   = state.getLifetimeExpenses();
        goPeak       = state.getPeakBalance();
        goVehicles   = state.getVehicles().size();
        mode         = Mode.GAME_OVER;
        showHelp     = false;
    }

    public void resetToMainMenu() {
        mode          = Mode.MAIN_MENU;
        playRequested = false;
        showHelp      = false;
    }

    public void setPaused() {
        mode          = Mode.PAUSED;
        resumeRequested = false;
        showHelp      = false;
    }

    public boolean isPlayRequested()    { return playRequested;   }
    public boolean isQuitRequested()    { return quitRequested;   }
    public boolean isResumeRequested()  { return resumeRequested; }
    public void    clearPlayRequest()   { playRequested   = false; }
    public void    clearResumeRequest() { resumeRequested = false; }

    public String     getCompanyName()  { return companyName;         }
    public Difficulty getDifficulty()   { return selectedDifficulty;  }
    public int        getMapSize()      { return selectedMapSize;      }

    // ── render ────────────────────────────────────────────────────────────────

    public void render(float delta) {
        elapsed     += delta;
        cursorBlink += delta;
        tipDelay    += delta;
        if (tipDelay > 6f) { tipDelay = 0f; tipIndex = (tipIndex + 1) % TIPS.length; tipOffset = 0f; }

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        camera.setToOrtho(false, w, h);
        camera.update();
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        Gdx.gl.glClearColor(0.04f, 0.06f, 0.10f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        drawBackground(w, h);
        drawBgVehicles(delta, w, h);

        if (showHelp) {
            drawHelpOverlay(w, h);
            return;
        }

        if (mode == Mode.GAME_OVER) {
            drawGameOver(w, h);
        } else if (mode == Mode.PAUSED) {
            drawPauseMenu(w, h);
        } else {
            drawMainMenu(w, h);
        }

        drawScrollingTip(delta, w, h);
    }

    // ── tree positions: { x (0-1 of w), y (0-1 of h), size } ────────────────
    private static final float[][] TREES = {
        {0.02f, 0.28f, 28f}, {0.06f, 0.32f, 36f}, {0.04f, 0.38f, 24f}, {0.09f, 0.25f, 32f},
        {0.12f, 0.35f, 20f}, {0.01f, 0.42f, 30f}, {0.07f, 0.45f, 26f}, {0.14f, 0.29f, 22f},
        {0.88f, 0.30f, 34f}, {0.92f, 0.26f, 28f}, {0.96f, 0.38f, 22f}, {0.85f, 0.42f, 30f},
        {0.90f, 0.46f, 36f}, {0.94f, 0.33f, 24f}, {0.98f, 0.44f, 20f}, {0.82f, 0.35f, 26f},
        {0.03f, 0.52f, 22f}, {0.10f, 0.55f, 28f}, {0.93f, 0.52f, 26f}, {0.87f, 0.56f, 32f},
    };

    // ── background ────────────────────────────────────────────────────────────

    private void drawBackground(float w, float h) {
        // Road y: 28 % from bottom — vehicles drive here
        float roadY  = h * 0.28f;
        float roadH  = 28f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Sky bands (top → down, dark teal to warm horizon)
        shape.setColor(0.04f, 0.14f, 0.22f, 1f);
        shape.rect(0, h * 0.60f, w, h * 0.40f);
        shape.setColor(0.06f, 0.20f, 0.28f, 1f);
        shape.rect(0, h * 0.50f, w, h * 0.10f);
        shape.setColor(0.10f, 0.30f, 0.30f, 1f);
        shape.rect(0, h * 0.42f, w, h * 0.08f);

        // Sun glow (top-right area)
        float sx = w * 0.80f, sy = h * 0.82f;
        shape.setColor(1f, 0.95f, 0.60f, 0.06f);
        shape.circle(sx, sy, 120f, 32);
        shape.setColor(1f, 0.95f, 0.60f, 0.10f);
        shape.circle(sx, sy, 75f, 32);
        shape.setColor(1f, 0.92f, 0.40f, 0.30f);
        shape.circle(sx, sy, 38f, 32);
        shape.setColor(1f, 0.96f, 0.65f, 0.90f);
        shape.circle(sx, sy, 18f, 32);

        // Far hills silhouette
        shape.setColor(0.08f, 0.28f, 0.18f, 1f);
        shape.rect(0, h * 0.38f, w, h * 0.04f);
        // Hill bumps
        for (int i = 0; i < 7; i++) {
            float hx = w * (0.05f + i * 0.14f);
            shape.circle(hx, h * 0.42f, w * 0.08f, 24);
        }

        // Mid fields
        shape.setColor(0.10f, 0.38f, 0.15f, 1f);
        shape.rect(0, h * 0.34f, w, h * 0.08f);
        // Field texture strips (alternating shades)
        for (int i = 0; i < 12; i++) {
            shape.setColor(i % 2 == 0 ? new Color(0.12f,0.42f,0.16f,1f) : new Color(0.09f,0.34f,0.13f,1f));
            shape.rect(w * (i / 12f), h * 0.34f, w / 12f, h * 0.06f);
        }

        // Near grass (between road and bottom)
        shape.setColor(0.14f, 0.52f, 0.18f, 1f);
        shape.rect(0, 0, w, roadY - roadH / 2f);
        // Grass texture strips
        for (int i = 0; i < 16; i++) {
            shape.setColor(i % 2 == 0 ? new Color(0.16f,0.58f,0.20f,1f) : new Color(0.12f,0.46f,0.15f,1f));
            shape.rect(w * (i / 16f), 0, w / 16f, roadY * 0.55f);
        }

        // Road surface
        shape.setColor(0.22f, 0.22f, 0.24f, 1f);
        shape.rect(0, roadY - roadH / 2f, w, roadH);
        // Road edge lines
        shape.setColor(0.60f, 0.58f, 0.30f, 1f);
        shape.rect(0, roadY - roadH / 2f,      w, 2f);
        shape.rect(0, roadY + roadH / 2f - 2f, w, 2f);

        shape.end();

        // Road centre dashes (white dashes)
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.92f, 0.92f, 0.85f, 0.85f);
        float dashW = 22f, dashH = 3f, dashGap = 18f;
        for (float dx = 0; dx < w; dx += dashW + dashGap) {
            shape.rect(dx, roadY - dashH / 2f, dashW, dashH);
        }
        shape.end();

        // Trees
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (float[] t : TREES) {
            float tx = t[0] * w, ty = t[1] * h, ts = t[2];
            // Trunk
            shape.setColor(0.35f, 0.22f, 0.08f, 1f);
            shape.rect(tx - 3f, ty, 6f, ts * 0.45f);
            // Shadow under canopy
            shape.setColor(0.05f, 0.20f, 0.05f, 0.40f);
            shape.triangle(tx - ts, ty + ts * 0.35f, tx + ts, ty + ts * 0.35f, tx, ty + ts * 1.4f);
            // Canopy (two overlapping triangles for depth)
            shape.setColor(0.12f, 0.52f, 0.18f, 1f);
            shape.triangle(tx - ts * 0.85f, ty + ts * 0.4f, tx + ts * 0.85f, ty + ts * 0.4f, tx, ty + ts * 1.45f);
            shape.setColor(0.18f, 0.68f, 0.24f, 1f);
            shape.triangle(tx - ts * 0.65f, ty + ts * 0.65f, tx + ts * 0.65f, ty + ts * 0.65f, tx, ty + ts * 1.55f);
        }
        shape.end();

        // Dark centre vignette so menu text pops
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0.04f, 0.02f, 0.62f);
        shape.rect(0, 0, w, h);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBgVehicles(float delta, float w, float h) {
        float roadY = h * 0.28f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        for (float[] v : bgVehicles) {
            v[0] += v[2] * delta;
            if (v[0] > w + 60f) v[0] = -60f;
            float vx = v[0];
            float vy = roadY - 7f;
            // Shadow
            shape.setColor(0f, 0f, 0f, 0.30f);
            shape.rect(vx + 4f, vy - 4f, 44f, 14f);
            // Body
            shape.setColor(v[3], v[4], v[5], 0.92f);
            shape.rect(vx, vy, 44f, 14f);
            // Roof
            shape.setColor(v[3] * 0.75f, v[4] * 0.75f, v[5] * 0.75f, 0.92f);
            shape.rect(vx + 8f, vy + 14f, 26f, 8f);
            // Windows
            shape.setColor(0.75f, 0.92f, 1f, 0.65f);
            shape.rect(vx + 10f, vy + 5f, 8f, 5f);
            shape.rect(vx + 22f, vy + 5f, 8f, 5f);
            // Wheels
            shape.setColor(0.12f, 0.12f, 0.12f, 1f);
            shape.circle(vx + 9f,  vy - 2f, 5f, 8);
            shape.circle(vx + 35f, vy - 2f, 5f, 8);
        }
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    // ── main menu ─────────────────────────────────────────────────────────────

    private void drawMainMenu(float w, float h) {
        float cx = w / 2f;

        // ── Central panel ──
        float panelW = Math.min(w * 0.55f, 540f);
        float panelH = Math.min(h * 0.88f, 640f);
        float panelX = cx - panelW / 2f;
        float panelY = h / 2f - panelH / 2f;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.03f, 0.10f, 0.05f, 0.88f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.25f, 0.65f, 0.30f, 0.80f);
        shape.rect(panelX, panelY, panelW, panelH);
        // inner highlight
        shape.setColor(0.15f, 0.45f, 0.20f, 0.35f);
        shape.rect(panelX + 3f, panelY + 3f, panelW - 6f, panelH - 6f);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Title ──
        float pulse = 1f + 0.04f * (float) Math.sin(elapsed * 2.2f);
        font.getData().setScale(2.8f * pulse);
        layout.setText(font, "MINI TRANSPORT TYCOON");
        float titleY = panelY + panelH - 28f;
        batch.begin();
        // green shadow glow
        font.setColor(0.20f, 0.90f, 0.35f, 0.22f);
        font.draw(batch, "MINI TRANSPORT TYCOON", cx - layout.width / 2f + 3f, titleY - 3f);
        // golden title
        font.setColor(0.98f, 0.88f, 0.20f, 1f);
        font.draw(batch, "MINI TRANSPORT TYCOON", cx - layout.width / 2f, titleY);
        // subtitle
        font.getData().setScale(1.05f);
        font.setColor(0.40f, 0.85f, 0.48f, 1f);
        layout.setText(font, "Build your transport empire");
        font.draw(batch, "Build your transport empire", cx - layout.width / 2f, titleY - 46f);
        batch.end();

        float sectionY = titleY - 80f;

        // ── Company name label + field ──
        sectionY -= 30f;
        batch.begin();
        font.getData().setScale(1.05f);
        font.setColor(0.70f, 0.75f, 0.85f, 1f);
        font.draw(batch, "Company Name", cx - 200f, sectionY);
        batch.end();

        sectionY -= 38f;
        float fieldW = 400f, fieldH = 36f;
        float fieldX = cx - fieldW / 2f;
        rectNameField[0] = fieldX; rectNameField[1] = sectionY - fieldH;
        rectNameField[2] = fieldW; rectNameField[3] = fieldH;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(nameFieldActive ? new Color(0.14f,0.18f,0.28f,1f) : new Color(0.10f,0.12f,0.20f,1f));
        shape.rect(fieldX, sectionY - fieldH, fieldW, fieldH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(nameFieldActive ? new Color(0.50f,0.75f,1f,1f) : new Color(0.28f,0.38f,0.55f,1f));
        shape.rect(fieldX, sectionY - fieldH, fieldW, fieldH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        String displayName = companyName + (nameFieldActive && cursorBlink % 1f < 0.5f ? "|" : "");
        batch.begin();
        font.getData().setScale(1.1f);
        font.setColor(Color.WHITE);
        font.draw(batch, displayName, fieldX + 10f, sectionY - 8f);
        batch.end();

        // ── Difficulty ──
        sectionY -= 58f;
        batch.begin();
        font.getData().setScale(1.05f);
        font.setColor(0.70f, 0.75f, 0.85f, 1f);
        font.draw(batch, "Difficulty", cx - 200f, sectionY);
        batch.end();

        sectionY -= 38f;
        float btnW = 120f, btnH = 36f, btnGap = 12f;
        float diffStartX = cx - (btnW * 3f + btnGap * 2f) / 2f;
        Difficulty[] diffs   = { Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD };
        float[][]    dRects  = { rectEasy, rectNormal, rectHard };
        String[]     dLabels = { "Easy  $150k", "Normal  $100k", "Hard  $50k" };
        Color[]      dColors = {
            new Color(0.18f, 0.55f, 0.22f, 1f),
            new Color(0.20f, 0.42f, 0.75f, 1f),
            new Color(0.70f, 0.18f, 0.18f, 1f)
        };

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < 3; i++) {
            float bx = diffStartX + i * (btnW + btnGap);
            dRects[i][0] = bx; dRects[i][1] = sectionY - btnH;
            dRects[i][2] = btnW; dRects[i][3] = btnH;
            boolean sel = selectedDifficulty == diffs[i];
            Color base = dColors[i];
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(sel ? base : new Color(base.r*0.45f, base.g*0.45f, base.b*0.45f, 1f));
            shape.rect(bx, sectionY - btnH, btnW, btnH);
            shape.end();
            if (sel) {
                shape.begin(ShapeRenderer.ShapeType.Line);
                shape.setColor(Color.WHITE);
                shape.rect(bx, sectionY - btnH, btnW, btnH);
                shape.end();
            }
            batch.begin();
            font.getData().setScale(0.82f);
            font.setColor(sel ? Color.WHITE : new Color(0.70f,0.70f,0.70f,1f));
            layout.setText(font, dLabels[i]);
            font.draw(batch, dLabels[i], bx + (btnW - layout.width)/2f, sectionY - btnH + btnH*0.65f);
            batch.end();
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── Map size ──
        sectionY -= 58f;
        batch.begin();
        font.getData().setScale(1.05f);
        font.setColor(0.70f, 0.75f, 0.85f, 1f);
        font.draw(batch, "Map Size", cx - 200f, sectionY);
        batch.end();

        sectionY -= 38f;
        float szBtnW = (400f - (MAP_SIZES.length - 1) * 8f) / MAP_SIZES.length; // evenly fill 400px
        float szBtnH = 36f;
        float szStartX = cx - 200f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int i = 0; i < MAP_SIZES.length; i++) {
            float bx = szStartX + i * (szBtnW + 8f);
            rectSizes[i][0] = bx; rectSizes[i][1] = sectionY - szBtnH;
            rectSizes[i][2] = szBtnW; rectSizes[i][3] = szBtnH;
            boolean sel = selectedMapSize == MAP_SIZES[i];
            Color base = new Color(0.20f, 0.35f, 0.60f, 1f);
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(sel ? base : new Color(base.r * 0.45f, base.g * 0.45f, base.b * 0.45f, 1f));
            shape.rect(bx, sectionY - szBtnH, szBtnW, szBtnH);
            shape.end();
            if (sel) {
                shape.begin(ShapeRenderer.ShapeType.Line);
                shape.setColor(Color.WHITE);
                shape.rect(bx, sectionY - szBtnH, szBtnW, szBtnH);
                shape.end();
            }
            batch.begin();
            font.getData().setScale(0.80f);
            font.setColor(sel ? Color.WHITE : new Color(0.70f, 0.70f, 0.70f, 1f));
            String lbl = MAP_SIZES[i] + "x";
            layout.setText(font, lbl);
            font.draw(batch, lbl, bx + (szBtnW - layout.width) / 2f, sectionY - szBtnH + szBtnH * 0.65f);
            batch.end();
        }
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── PLAY / HELP / QUIT buttons ──
        sectionY -= 68f;
        float mbW = 240f, mbH = 48f;
        drawMainButton(cx - mbW/2f, sectionY - mbH, mbW, mbH,
            "PLAY", new Color(0.15f,0.55f,0.20f,1f), new Color(0.22f,0.78f,0.28f,1f), rectPlay);

        sectionY -= 68f;
        drawMainButton(cx - mbW/2f, sectionY - mbH, mbW, mbH,
            "HOW TO PLAY", new Color(0.15f,0.35f,0.65f,1f), new Color(0.22f,0.52f,0.92f,1f), rectHelp);

        sectionY -= 68f;
        drawMainButton(cx - mbW/2f, sectionY - mbH, mbW, mbH,
            "QUIT", new Color(0.50f,0.12f,0.12f,1f), new Color(0.75f,0.18f,0.18f,1f), rectQuit);
    }

    // ── pause menu ────────────────────────────────────────────────────────────

    private void drawPauseMenu(float w, float h) {
        float cx = w / 2f;

        float panelW = Math.min(w * 0.42f, 400f);
        float panelH = Math.min(h * 0.62f, 440f);
        float panelX = cx - panelW / 2f;
        float panelY = h / 2f - panelH / 2f;

        // Panel background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.03f, 0.08f, 0.14f, 0.94f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.28f, 0.52f, 0.85f, 0.90f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.setColor(0.18f, 0.35f, 0.60f, 0.40f);
        shape.rect(panelX + 3f, panelY + 3f, panelW - 6f, panelH - 6f);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Title
        batch.begin();
        font.getData().setScale(2.2f);
        font.setColor(0.85f, 0.90f, 1f, 1f);
        layout.setText(font, "PAUSED");
        font.draw(batch, "PAUSED", cx - layout.width / 2f, panelY + panelH - 22f);
        batch.end();

        // Buttons stacked vertically with even spacing
        float btnW = panelW * 0.75f;
        float btnH = 46f;
        float btnX = cx - btnW / 2f;
        float spacing = 62f;
        float startY = panelY + panelH - 110f;

        drawMainButton(btnX, startY - btnH,              btnW, btnH,
            "RESUME",       new Color(0.12f, 0.50f, 0.18f, 1f), new Color(0.18f, 0.75f, 0.25f, 1f), rectResume);

        drawMainButton(btnX, startY - btnH - spacing,    btnW, btnH,
            "NEW GAME",     new Color(0.15f, 0.28f, 0.60f, 1f), new Color(0.22f, 0.45f, 0.88f, 1f), rectNewGame);

        drawMainButton(btnX, startY - btnH - spacing*2f, btnW, btnH,
            "HOW TO PLAY",  new Color(0.35f, 0.22f, 0.55f, 1f), new Color(0.55f, 0.35f, 0.85f, 1f), rectPauseHelp);

        drawMainButton(btnX, startY - btnH - spacing*3f, btnW, btnH,
            "QUIT",         new Color(0.50f, 0.12f, 0.12f, 1f), new Color(0.78f, 0.18f, 0.18f, 1f), rectPauseQuit);
    }

    // ── game over ─────────────────────────────────────────────────────────────

    private void drawGameOver(float w, float h) {
        float cx = w / 2f;

        // Title
        batch.begin();
        font.getData().setScale(2.8f);
        font.setColor(0.95f, 0.25f, 0.18f, 1f);
        layout.setText(font, "COMPANY DISSOLVED");
        font.draw(batch, "COMPANY DISSOLVED", cx - layout.width / 2f, h - 60f);

        font.getData().setScale(1.6f);
        font.setColor(1f, 0.92f, 0.75f, 1f);
        layout.setText(font, goCompany);
        font.draw(batch, goCompany, cx - layout.width / 2f, h - 120f);
        batch.end();

        // Stats panel
        float panelW = 420f, panelH = 230f;
        float panelX = cx - panelW/2f, panelY = h - 370f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0.10f, 0.12f, 0.18f, 0.95f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.35f, 0.42f, 0.60f, 1f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        String[][] rows = {
            { "Difficulty",     goDifficulty },
            { "Final year",     String.valueOf(goYear) },
            { "Peak balance",   "$" + fmt(goPeak) },
            { "Total income",   "$" + fmt(goIncome) },
            { "Total costs",    "$" + fmt(goExpenses) },
            { "Vehicles fleet", String.valueOf(goVehicles) },
        };

        batch.begin();
        float rowH = (panelH - 16f) / rows.length;
        for (int i = 0; i < rows.length; i++) {
            float rowY = panelY + panelH - 16f - i * rowH;
            font.getData().setScale(1.0f);
            font.setColor(0.60f, 0.65f, 0.78f, 1f);
            font.draw(batch, rows[i][0], panelX + 20f, rowY);
            font.setColor(1f, 1f, 1f, 1f);
            layout.setText(font, rows[i][1]);
            font.draw(batch, rows[i][1], panelX + panelW - layout.width - 20f, rowY);
        }
        batch.end();

        // Buttons
        float mbW = 220f, mbH = 48f, gap = 24f;
        float totalBtnW = mbW * 2f + gap;
        float btnY = panelY - 72f;
        drawMainButton(cx - totalBtnW/2f,              btnY, mbW, mbH,
            "PLAY AGAIN", new Color(0.15f,0.55f,0.20f,1f), new Color(0.22f,0.78f,0.28f,1f), rectPlayAgain);
        drawMainButton(cx - totalBtnW/2f + mbW + gap,  btnY, mbW, mbH,
            "QUIT", new Color(0.50f,0.12f,0.12f,1f), new Color(0.75f,0.18f,0.18f,1f), rectGoQuit);
    }

    // ── help overlay ──────────────────────────────────────────────────────────

    private void drawHelpOverlay(float w, float h) {
        float cx = w / 2f;
        float panelW = 520f;
        float panelH = HELP_LINES.length * 28f + 110f;
        float panelX = cx - panelW / 2f;
        float panelY = h / 2f - panelH / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.70f);
        shape.rect(0, 0, w, h);
        shape.setColor(0.08f, 0.10f, 0.18f, 0.98f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(0.40f, 0.55f, 0.85f, 1f);
        shape.rect(panelX, panelY, panelW, panelH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(1.6f);
        font.setColor(1f, 0.92f, 0.25f, 1f);
        layout.setText(font, "HOW TO PLAY");
        font.draw(batch, "HOW TO PLAY", cx - layout.width / 2f, panelY + panelH - 14f);

        font.getData().setScale(0.92f);
        float lineY = panelY + panelH - 62f;
        for (String line : HELP_LINES) {
            if (line.isEmpty()) { lineY -= 10f; continue; }
            int split = line.indexOf("  ");
            if (split > 0) {
                font.setColor(0.80f, 0.86f, 1f, 1f);
                font.draw(batch, line.substring(0, split), panelX + 24f, lineY);
                font.setColor(0.55f, 0.60f, 0.72f, 1f);
                font.draw(batch, line.substring(split).trim(), panelX + 280f, lineY);
            } else {
                font.setColor(0.70f, 0.80f, 0.65f, 1f);
                font.draw(batch, line, panelX + 24f, lineY);
            }
            lineY -= 28f;
        }
        batch.end();

        float closeW = 140f, closeH = 40f;
        float closeX = cx - closeW / 2f;
        float closeY = panelY + 14f;
        rectHelpClose[0] = closeX; rectHelpClose[1] = closeY;
        rectHelpClose[2] = closeW; rectHelpClose[3] = closeH;
        drawMainButton(closeX, closeY, closeW, closeH,
            "CLOSE", new Color(0.30f,0.15f,0.50f,1f), new Color(0.55f,0.28f,0.85f,1f), rectHelpClose);
    }

    // ── scrolling tips ────────────────────────────────────────────────────────

    private void drawScrollingTip(float delta, float w, float h) {
        tipOffset += delta * 55f;
        String tip = TIPS[tipIndex];

        batch.begin();
        font.getData().setScale(0.88f);
        font.setColor(0.42f, 0.48f, 0.58f, 1f);
        font.draw(batch, tip, w - tipOffset, 20f);
        batch.end();
    }

    // ── shared button helper ──────────────────────────────────────────────────

    private void drawMainButton(float x, float y, float bw, float bh,
                                String label, Color base, Color hover, float[] rectOut) {
        rectOut[0] = x; rectOut[1] = y; rectOut[2] = bw; rectOut[3] = bh;
        boolean hovered = isHovered(rectOut);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(hovered ? hover : base);
        shape.rect(x, y, bw, bh);
        shape.end();
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(hovered ? Color.WHITE : new Color(1f,1f,1f,0.35f));
        shape.rect(x, y, bw, bh);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        font.getData().setScale(1.1f);
        font.setColor(hovered ? Color.WHITE : new Color(0.88f,0.88f,0.88f,1f));
        layout.setText(font, label);
        font.draw(batch, label, x + (bw - layout.width) / 2f, y + (bh + layout.height) / 2f);
        batch.end();

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
    }

    // ── input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseMoved(int sx, int sy) {
        mouseX = sx; mouseY = sy;
        return false;
    }

    @Override
    public boolean touchDown(int sx, int sy, int pointer, int button) {
        mouseX = sx; mouseY = sy;
        if (button != Input.Buttons.LEFT) return false;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        float lx = sx;
        float ly = h - sy; // convert to bottom-left

        if (showHelp) {
            if (inRect(lx, ly, rectHelpClose)) { showHelp = false; return true; }
            return true; // eat all input while overlay is open
        }

        if (mode == Mode.GAME_OVER) {
            if (inRect(lx, ly, rectPlayAgain)) {
                mode = Mode.MAIN_MENU;
                playRequested = true;
            } else if (inRect(lx, ly, rectGoQuit)) {
                quitRequested = true;
            }
            return true;
        }

        if (mode == Mode.PAUSED) {
            if (inRect(lx, ly, rectResume))    { resumeRequested = true;             return true; }
            if (inRect(lx, ly, rectNewGame))   { mode = Mode.MAIN_MENU;              return true; }
            if (inRect(lx, ly, rectPauseHelp)) { showHelp = true;                   return true; }
            if (inRect(lx, ly, rectPauseQuit)) { quitRequested = true;              return true; }
            return true;
        }

        // Main menu
        if (inRect(lx, ly, rectNameField)) {
            nameFieldActive = true;
            return true;
        }
        nameFieldActive = false;

        if (inRect(lx, ly, rectEasy))   { selectedDifficulty = Difficulty.EASY;   return true; }
        if (inRect(lx, ly, rectNormal)) { selectedDifficulty = Difficulty.NORMAL; return true; }
        if (inRect(lx, ly, rectHard))   { selectedDifficulty = Difficulty.HARD;   return true; }
        for (int i = 0; i < MAP_SIZES.length; i++) {
            if (inRect(lx, ly, rectSizes[i])) { selectedMapSize = MAP_SIZES[i]; return true; }
        }
        if (inRect(lx, ly, rectPlay))   { playRequested = true;                   return true; }
        if (inRect(lx, ly, rectHelp))   { showHelp = true;                        return true; }
        if (inRect(lx, ly, rectQuit))   { quitRequested = true;                   return true; }
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.ESCAPE) {
            nameFieldActive = false;
            if (showHelp) { showHelp = false; return true; }
            if (mode == Mode.PAUSED) { resumeRequested = true; return true; }
        }
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        if (!nameFieldActive) return false;
        if (c == '\b') {
            if (companyName.length() > 0) companyName = companyName.substring(0, companyName.length() - 1);
        } else if (c >= 32 && companyName.length() < 24) {
            companyName += c;
        }
        return true;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** rect is { x, y, w, h } in LibGDX bottom-left coords; mouseY is GLFW (y-down). */
    private boolean isHovered(float[] rect) {
        float lx = mouseX;
        float ly = Gdx.graphics.getHeight() - mouseY;
        return inRect(lx, ly, rect);
    }

    private static boolean inRect(float lx, float ly, float[] r) {
        return lx >= r[0] && lx <= r[0] + r[2] && ly >= r[1] && ly <= r[1] + r[3];
    }

    private static String fmt(long v) {
        return String.format("%,d", v);
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
        shape.dispose();
    }
}
