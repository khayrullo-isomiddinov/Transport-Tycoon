package com.team404.tycoon.desktop;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.team404.tycoon.controller.GameController;
import com.team404.tycoon.controller.InputController;
import com.team404.tycoon.model.GameState;

public class TransportTycoonGame extends ApplicationAdapter {

    private GameController gameController;
    private InputController inputController;
    private GameRenderer renderer;
    private HudRenderer hudRenderer;
    private InputProcessor inputProcessor;

    @Override
    public void create() {
        GameState gameState = new GameState(64, 64);
        this.gameController = new GameController(gameState);
        this.inputController = new InputController(gameController);

        Renderer2D renderer2D = new Renderer2D();
        this.renderer = renderer2D;
        this.hudRenderer = new HudRenderer();

        this.inputProcessor = new MapInputProcessor(renderer2D.getCamera(), inputController, renderer2D);
        Gdx.input.setInputProcessor(inputProcessor);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        gameController.update(delta);
        renderer.render(gameController, delta);
        hudRenderer.render(inputController.getCurrentMode());
    }

    @Override
    public void resize(int width, int height) {
        renderer.resize(width, height);
    }

    @Override
    public void dispose() {
        renderer.dispose();
        hudRenderer.dispose();
    }
}
