package com.team404.tycoon.desktop;

import com.team404.tycoon.controller.GameController;

/**
 * View interface for rendering the current game state.
 * Implementations are responsible for drawing using a specific technology (LibGDX here).
 */
public interface GameRenderer {

    void render(GameController controller, float deltaSeconds);

    void resize(int width, int height);

    void dispose();
}

