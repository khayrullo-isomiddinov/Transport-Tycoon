## Tech & Architecture Stack

- **Language & runtime**
  - **Java 17+** as the primary language.

- **Build & project layout**
  - **Maven multi-module project**:
    - Root `pom.xml` with modules:
      - `core`: pure Java domain model and game logic (GameState, GameMap, City, IndustrialFacility, Economy, Controllers).
      - `desktop`: desktop launcher, rendering, input, audio.

- **Game / UI framework**
  - **libGDX (desktop / LWJGL3 backend)**:
    - Game loop (`render()` at ~60 FPS).
    - 2D tile-based rendering (orthogonal first, later isometric-style).
    - Input handling (mouse, keyboard), camera, zoom/pan.
    - Sprite batching, textures, basic audio.

- **Architecture style**
  - **MVC-ish separation**:
    - **Model** in `core` (`GameState`, map, economy, vehicles, routes, facilities).
    - **Controllers** in `core` (`GameController`, `BuildController`, `VehicleController`, `RouteController`, `EconomyController`, `InputController`).
    - **View** in `desktop` (LibGDX screens and renderers: `GameRenderer`, `Renderer2D`, `MapView`, `VehicleView`, HUD).
  - `desktop` depends on `core`, but `core` has **no dependency** on LibGDX.

- **Persistence**
  - **JSON-based save files** for `GameState` (using Jackson or similar) in `core`, with simple save/load UI in `desktop`.

- **Testing**
  - **JUnit 5** for unit tests on the `core` module (economy, map, vehicles, routes, pathfinding).

- **Target platform**
  - **Desktop (macOS, Windows, Linux)** using the LibGDX LWJGL3 backend (similar feel to OpenTTD’s SDL/OpenGL desktop stack, but in Java).

