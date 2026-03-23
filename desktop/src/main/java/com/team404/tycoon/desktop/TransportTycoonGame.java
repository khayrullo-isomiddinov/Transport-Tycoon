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
import com.team404.tycoon.model.GameState;
import com.team404.tycoon.model.RandomMapGenerator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TransportTycoonGame extends ApplicationAdapter {

    private GameController gameController;
    private InputController inputController;
    private GameRenderer renderer;
    private HudRenderer hudRenderer;
    private DecorationTextureCache decorationTextureCache;
    private AssetPaletteState assetPaletteState;

    @Override
    public void create() {
        GameState gameState = new GameState(64, 64);
        RandomMapGenerator.generate(gameState, System.nanoTime());
        this.gameController = new GameController(gameState);
        this.inputController = new InputController(gameController);

        this.decorationTextureCache = new DecorationTextureCache();
        List<String> pngPaths = listPngResourcePaths();
        this.assetPaletteState = new AssetPaletteState(pngPaths);

        Renderer2D renderer2D = new Renderer2D(decorationTextureCache);
        this.renderer = renderer2D;
        this.hudRenderer = new HudRenderer();

        AssetPaletteInputProcessor paletteInput = new AssetPaletteInputProcessor(assetPaletteState, inputController);
        MapInputProcessor mapInput = new MapInputProcessor(
                renderer2D.getCamera(), inputController, renderer2D, assetPaletteState);
        Gdx.input.setInputProcessor(new InputMultiplexer(paletteInput, mapInput));
    }

    private static List<String> listPngResourcePaths() {
        List<String> fromInternal = listPngFromInternalDirectory();
        if (!fromInternal.isEmpty()) {
            return fromInternal;
        }
        List<String> fromDisk = ResourceFileHandles.listPngPathsFromDisk();
        if (!fromDisk.isEmpty()) {
            return fromDisk;
        }
        List<String> fromManifest = ResourceFileHandles.listPngPathsFromManifest();
        if (!fromManifest.isEmpty()) {
            return fromManifest;
        }
        return Collections.emptyList();
    }

    private static List<String> listPngFromInternalDirectory() {
        FileHandle dir = Gdx.files.internal("resources");
        if (!dir.exists() || !dir.isDirectory()) {
            return Collections.emptyList();
        }
        FileHandle[] files = dir.list();
        if (files == null) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<>();
        for (FileHandle fh : files) {
            if (fh.isDirectory()) {
                continue;
            }
            String name = fh.name();
            if (name.toLowerCase(Locale.ROOT).endsWith(".png")) {
                out.add("resources/" + name);
            }
        }
        Collections.sort(out);
        return out;
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        gameController.update(delta);
        renderer.render(gameController, delta);
        hudRenderer.render(inputController.getCurrentMode(), assetPaletteState, decorationTextureCache);
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        hudRenderer.dispose();
        decorationTextureCache.dispose();
    }
}
