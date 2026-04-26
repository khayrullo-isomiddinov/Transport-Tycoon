package com.team404.tycoon.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.team404.tycoon.controller.GameController;
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.desktop.assets.AssetPaletteState;
import com.team404.tycoon.desktop.assets.DecorationTextureCache;
import com.team404.tycoon.desktop.assets.ResourceFileHandles;
import com.team404.tycoon.model.Difficulty;
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.RandomMapGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TransportTycoonGame extends ApplicationAdapter {

    private enum AppScreen { MENU, PLAYING }
    private AppScreen appScreen = AppScreen.MENU;

    // ── always alive ──────────────────────────────────────────────────────────
    private MenuScreen menuScreen;

    // ── alive only during PLAYING ─────────────────────────────────────────────
    private GameController        gameController;
    private InputController       inputController;
    private GameRenderer          renderer;
    private Renderer2D            renderer2D;
    private HudRenderer           hudRenderer;
    private DecorationTextureCache decorationTextureCache;
    private AssetPaletteState     assetPaletteState;
    private InputMultiplexer      gameInputMultiplexer;

    @Override
    public void create() {
        menuScreen = new MenuScreen();
        Gdx.input.setInputProcessor(menuScreen);
    }

    private void startGame(String companyName, Difficulty difficulty) {
        disposeGameObjects();

        int mapSize = menuScreen.getMapSize();
        GameState gameState = new GameState(mapSize, mapSize);
        RandomMapGenerator.generate(gameState, System.nanoTime());
        // Override the balance set by the generator to match chosen difficulty.
        gameState.setBalance(difficulty.getStartingCapital());
        gameState.setCompanyName(companyName);
        gameState.setDifficulty(difficulty);

        gameController  = new GameController(gameState);
        inputController = new InputController(gameController);

        decorationTextureCache = new DecorationTextureCache();
        List<String> pngPaths  = listPngResourcePaths();
        assetPaletteState      = new AssetPaletteState(pngPaths);

        renderer2D = new Renderer2D(decorationTextureCache, inputController);
        renderer    = renderer2D;
        hudRenderer = new HudRenderer();
        renderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        renderer2D.initCameraForMap(mapSize, mapSize);

        AssetPaletteInputProcessor paletteInput = new AssetPaletteInputProcessor(assetPaletteState, inputController);
        MapInputProcessor mapInput = new MapInputProcessor(
                renderer2D.getCamera(),
                inputController,
                renderer2D,
                gameController,
                hudRenderer.getMinimapOverlay(),
                hudRenderer.getObjectInspectorOverlay());
        gameInputMultiplexer = new InputMultiplexer(paletteInput, mapInput);

        appScreen = AppScreen.PLAYING;
        Gdx.input.setInputProcessor(gameInputMultiplexer);
    }

    private void switchToGameOver() {
        menuScreen.setGameOver(gameController.getGameState());
        appScreen = AppScreen.MENU;
        Gdx.input.setInputProcessor(menuScreen);
    }

    private void disposeGameObjects() {
        if (renderer              != null) { renderer.dispose();              renderer              = null; }
        renderer2D = null;
        if (hudRenderer           != null) { hudRenderer.dispose();           hudRenderer           = null; }
        if (decorationTextureCache != null) { decorationTextureCache.dispose(); decorationTextureCache = null; }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (appScreen == AppScreen.MENU) {
            menuScreen.render(delta);

            if (menuScreen.isQuitRequested()) {
                Gdx.app.exit();
                return;
            }
            if (menuScreen.isResumeRequested()) {
                menuScreen.clearResumeRequest();
                appScreen = AppScreen.PLAYING;
                Gdx.input.setInputProcessor(gameInputMultiplexer);
                return;
            }
            if (menuScreen.isPlayRequested()) {
                String name = menuScreen.getCompanyName();
                Difficulty diff = menuScreen.getDifficulty();
                menuScreen.clearPlayRequest();
                startGame(name, diff);
            }

        } else {
            // Check for menu request (ESC or MENU button) → show pause screen.
            if (inputController.isMenuRequested()) {
                inputController.clearMenuRequest();
                menuScreen.setPaused();
                appScreen = AppScreen.MENU;
                Gdx.input.setInputProcessor(menuScreen);
                return;
            }
            // Check for bankruptcy → hand off to game-over screen.
            if (gameController.getGameState().isBankrupt()) {
                switchToGameOver();
                return;
            }
            gameController.update(delta);
            renderer.render(gameController, delta);
            hudRenderer.render(
                    assetPaletteState,
                    decorationTextureCache,
                    gameController.getGameState(),
                    inputController.getCurrentMode(),
                    inputController.getGameSpeedIndex(),
                    renderer2D.getCamera(),
                    renderer2D != null ? renderer2D.getHoverTileX() : -1,
                    renderer2D != null ? renderer2D.getHoverTileY() : -1,
                    inputController.isLastPlacementRejected(),
                    inputController.isLastRoadTypeRejected(),
                    inputController.isLastTerraformRejected());
        }
    }

    @Override
    public void resize(int width, int height) {
        if (renderer != null) {
            renderer.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        disposeGameObjects();
        if (menuScreen != null) menuScreen.dispose();
    }

    // ── resource helpers ──────────────────────────────────────────────────────

    private static List<String> listPngResourcePaths() {
        List<String> fromInternal = listPngFromInternalDirectory();
        if (!fromInternal.isEmpty()) return fromInternal;
        List<String> fromDisk = ResourceFileHandles.listPngPathsFromDisk();
        if (!fromDisk.isEmpty()) return fromDisk;
        List<String> fromManifest = ResourceFileHandles.listPngPathsFromManifest();
        if (!fromManifest.isEmpty()) return fromManifest;
        return Collections.emptyList();
    }

    private static List<String> listPngFromInternalDirectory() {
        FileHandle dir = Gdx.files.internal("resources");
        if (!dir.exists() || !dir.isDirectory()) return Collections.emptyList();
        FileHandle[] files = dir.list();
        if (files == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (FileHandle fh : files) {
            if (fh.isDirectory()) continue;
            String name = fh.name();
            if (name.toLowerCase(Locale.ROOT).endsWith(".png")) {
                out.add("resources/" + name);
            }
        }
        Collections.sort(out);
        return out;
    }
}
