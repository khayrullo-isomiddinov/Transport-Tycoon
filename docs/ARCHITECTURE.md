# Mini Transport Tycoon — MVC Architecture (Harry)

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

### 2.1 Classes (`core/src/main/java/com/team404/tycoon/model/`)

| Class | Description |
|-------|-------------|
| `GameState` | **Aggregate root**: map, towns, routes, vehicles, demand, decorations, balance, bankruptcy, simulation time, traffic-light timing |
| `GameMap`, `Tile`, `TileType` | Grid, terrain types and tile heights |
| `Town`, `TransportDemand`, `Shipment` | Towns and the passenger/goods demand between them |
| `Route`, `RouteStop`, `RoadPathfinder` | Routes over the road network and pathfinding |
| `Vehicle`, `VehicleType`, `TransportContentType` | Buses/trucks, capacity, ageing maintenance; passengers vs goods |
| `PlacedDecoration`, `DecorationRules`, `BuildMode` | Placed buildings/roads/garages and what may be placed where |
| `EconomyConfig`, `Difficulty` | **All balance values** (costs, revenues) and starting capital |
| `RandomMapGenerator` | Terrain, water, forests, towns, industry and starter roads |

### 2.2 Key Rules

- Model classes have **no** imports from `com.badlogic.gdx` or any view/controller package
- Model is **testable** in isolation (plain JUnit)
- `GameState` is the single source of truth for game data

---

## 3. View

**Responsibility:** Render the Model. No game logic.

### 3.1 Components (`desktop/src/main/java/com/team404/tycoon/desktop/`)

| Component | Responsibility |
|-----------|----------------|
| `GameRenderer` (interface) | `render(GameController controller, float deltaSeconds)`, `resize`, `dispose`. Abstraction for 2D/3D |
| `Renderer2D` | Isometric map, roads, buildings and vehicles (SpriteBatch, OrthographicCamera) |
| `HudRenderer` | HUD: balance, speed buttons, build palette, hover info |
| `MinimapOverlay`, `ObjectInspectorOverlay` | Clickable minimap; `L` object details |
| `MenuScreen` | Main menu (company name, map size, difficulty), pause and game-over screens |
| `UiChrome` | Shared UI drawing helpers |
| `assets/*` | Texture loading and caching, white-to-transparent keying, build-palette state, asset manifest |

### 3.2 Key Rules

- View reads state through `GameController.getGameState()`
- View **never** modifies the Model directly; player actions go through `InputController`
- Input processors (`MapInputProcessor`, `AssetPaletteInputProcessor`) translate LibGDX events into `InputController` calls

---

## 4. Controller

**Responsibility:** Handle input, update Model, orchestrate game loop.

### 4.1 Components (`core/src/main/java/com/team404/tycoon/controller/`)

| Controller | Responsibility |
|------------|----------------|
| `GameController` | `update(delta)`: ticks simulation, speed multiplier, demand regeneration, vehicle purchase/sale at garages, traffic-light timing |
| `InputController` | Current build mode, placement/removal/terraform actions, game speed, menu requests |
| `PlacementValidator` | Whether a road/build/demolish/terraform is allowed (terrain, height difference, adjacent road types) |
| `TransportSimulation` | Vehicle movement, loading/unloading, delivery income, maintenance |

Save/load is not implemented yet.

### 4.2 Key Rules

- Controller **updates** Model
- Controller **does not** render; the game loop calls the View after `update`

---

## 5. Game Loop (MVC in action)

`TransportTycoonGame` (a LibGDX `ApplicationAdapter`) switches between `MENU` and `PLAYING`:

```java
// TransportTycoonGame.render(), PLAYING state (simplified)
if (inputController.isMenuRequested())          { /* show pause menu */ }
if (gameController.getGameState().isBankrupt()) { /* game-over screen */ }
gameController.update(delta);               // Controller -> Model
renderer.render(gameController, delta);     // View reads Model
hudRenderer.render(...);                    // HUD on top
```

Input arrives asynchronously through an `InputMultiplexer` (palette first, then map).

---

## 6. Module Structure (Maven)

```
core/                         pure Java, no LibGDX
└── com/team404/tycoon/
    ├── model/                (classes in 2.1)
    └── controller/           (classes in 4.1)

desktop/                      LibGDX + LWJGL3
└── com/team404/tycoon/desktop/
    ├── DesktopLauncher       main(); only LWJGL3-specific class
    ├── TransportTycoonGame   ApplicationAdapter, wires MVC
    ├── (view classes in 3.1, input processors)
    └── assets/

resources/                    PNG sprites, packaged into the desktop jar under resources/
```

---

## 7. Dependency Rules

```
View       → Controller, Model (read)
Controller → Model
Model    → nothing (no outgoing dependencies to view/controller)
```

---

## 8. 3D Extension

When adding 3D (sub-task):

1. Create `Renderer3D implements GameRenderer`
2. Implement `render(GameController controller, float deltaSeconds)` using 3D APIs
3. Swap `Renderer2D` for `Renderer3D` in `TransportTycoonGame`
4. **Model and Controller unchanged**

---

*Document version: 1.1 (updated to match the implemented code)*

*Team 404 — Mini Transport Tycoon*
