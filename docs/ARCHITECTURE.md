# Mini Transport Tycoon — MVC Architecture

**Team 404** | Java + Maven + LibGDX

---

## 1. MVC Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                         CONTROLLER                                 │
│  Receives input → Updates Model → Triggers View refresh           │
│  GameController, BuildController, RouteController, InputController │
└──────────────────────────────────────────────────────────────────┘
         │                                    │
         │ updates                             │ notifies / passes Model
         ▼                                    ▼
┌─────────────────────┐              ┌─────────────────────┐
│        MODEL        │              │        VIEW         │
│  Game state & logic │◄─────────────│  Renders Model      │
│  No UI dependencies │   reads      │  GameRenderer, UI   │
└─────────────────────┘              └─────────────────────┘
```

**Flow:** `Input → Controller → Model` | `Game loop → Controller.update() → Model` | `View.render(Model)`

---

## 2. Model

**Responsibility:** Game data and business rules. No LibGDX, no rendering, no input.

### 2.1 Packages & Classes

| Package | Classes | Description |
|---------|---------|-------------|
| `model.map` | `GameMap`, `Tile`, `TileType` | Grid, tile types (empty, road, water, etc.) |
| `model.map` | `City`, `IndustrialFacility` | Cities (3×3+), facilities (2×2+) |
| `model.map` | `Road`, `Stop` | Roads, stops on roads |
| `model.economy` | `Economy`, `Capital` | Money, income, costs, bankruptcy |
| `model.economy` | `GoodsType`, `Demand` | Goods, passengers, demand over time |
| `model.transport` | `Vehicle`, `VehicleType` | Vehicles, speed, capacity, maintenance |
| `model.transport` | `Route`, `RouteStop` | Ordered stops (A→B→C→A) |
| `model.transport` | `Cargo`, `CargoType` | Cargo/passengers carried |
| `model` | `GameState` | **Single source of truth** — aggregates map, economy, vehicles, routes, time |

### 2.2 Key Rules

- Model classes have **no** imports from `com.badlogic.gdx` or any view/controller package
- Model is **testable** in isolation (plain JUnit)
- `GameState` is the only object the View reads from

---

## 3. View

**Responsibility:** Render the Model. Read-only access to Model. No game logic.

### 3.1 Components

| Component | Responsibility |
|-----------|----------------|
| `GameRenderer` (interface) | `void render(GameState state, float delta)` — abstraction for 2D/3D |
| `Renderer2D` | SpriteBatch, OrthographicCamera, tile sprites |
| `Renderer3D` | ModelBatch, PerspectiveCamera *(add later if 3D sub-task)* |
| `MapView` | Renders map tiles, roads, cities, facilities |
| `VehicleView` | Renders vehicles at positions |
| `StopView` | Renders stops |
| `UIManager` | HUD (capital, time speed), build menu, route editor overlay |
| `MinimapView` | Minimap *(sub-task)* |

### 3.2 Key Rules

- View **only reads** from `GameState` (and sub-objects)
- View **never** modifies Model
- View has **no** input handling logic (that's Controller)

---

## 4. Controller

**Responsibility:** Handle input, update Model, orchestrate game loop.

### 4.1 Components

| Controller | Responsibility |
|------------|----------------|
| `GameController` | Main game loop: `update(delta)` → tick simulation, time, vehicles. Delegates to other controllers. |
| `InputController` | Maps mouse/keyboard to actions (click, drag, key press). Passes to BuildController, RouteController, etc. |
| `BuildController` | Build roads, place stops. Validates placement, deducts cost, updates Model. |
| `RouteController` | Create/edit routes, assign vehicles. Updates Model routes. |
| `VehicleController` | Spawn vehicles, movement logic, loading/unloading. Updates Model vehicles. |
| `EconomyController` | Apply income/costs each tick, check bankruptcy. Updates Model economy. |
| `SaveController` | Save/load `GameState`. *(sub-task)* |

### 4.2 Key Rules

- Controller **updates** Model
- Controller **does not** render; it triggers View refresh via the game loop
- Controller may use **services** (e.g. `Pathfinder`, `GameClock`) as infrastructure

---

## 5. Game Loop (MVC in action)

```java
// DesktopLauncher or main ApplicationAdapter
public void render() {
    float delta = Gdx.graphics.getDeltaTime();
    
    // 1. Controller: process input
    inputController.handleInput();
    
    // 2. Controller: update model (if not paused)
    if (!gameState.isPaused()) {
        gameController.update(delta);
    }
    
    // 3. View: render model
    gameRenderer.render(gameState, delta);
}
```

---

## 6. Package Structure (Maven modules)

```
core/
├── model/
│   ├── GameState.java
│   ├── map/
│   │   ├── GameMap.java
│   │   ├── Tile.java
│   │   ├── City.java
│   │   ├── IndustrialFacility.java
│   │   ├── Road.java
│   │   └── Stop.java
│   ├── economy/
│   │   ├── Economy.java
│   │   ├── GoodsType.java
│   │   └── Demand.java
│   └── transport/
│       ├── Vehicle.java
│       ├── Route.java
│       ├── RouteStop.java
│       └── Cargo.java
│
├── view/
│   ├── GameRenderer.java      (interface)
│   ├── Renderer2D.java
│   ├── MapView.java
│   ├── VehicleView.java
│   ├── StopView.java
│   ├── UIManager.java
│   └── MinimapView.java       (sub-task)
│
├── controller/
│   ├── GameController.java
│   ├── InputController.java
│   ├── BuildController.java
│   ├── RouteController.java
│   ├── VehicleController.java
│   ├── EconomyController.java
│   └── SaveController.java    (sub-task)
│
└── infrastructure/            (used by Controller, not MVC core)
    ├── GameClock.java
    ├── Pathfinder.java
    ├── AssetManager.java
    └── SaveFileHandler.java

desktop/
└── DesktopLauncher.java       (main, wires MVC, starts game loop)
```

---

## 7. Dependency Rules

```
View     → Model only (read)
Controller → Model, Infrastructure
Model    → nothing (no outgoing dependencies to view/controller)
```

---

## 8. 3D Extension

When adding 3D (sub-task):

1. Create `Renderer3D implements GameRenderer`
2. Implement `render(GameState state, float delta)` using 3D APIs
3. Swap `Renderer2D` for `Renderer3D` in launcher
4. **Model and Controller unchanged**

---

*Document version: 1.0*  
*Team 404 — Mini Transport Tycoon*
