# Week 1 — Foundation: Detailed Specification

**Team 404** | Mini Transport Tycoon  
This document clarifies each Foundation task with acceptance criteria, technical details, and implementation notes.

---

## 1. Ramez — Maven & Model (Economy)

### 1.1 Maven Project Setup

**Goal:** Create a multi-module Maven project with `core` and `desktop` modules.

**Structure:**
```
team-404/
├── pom.xml                    # Parent POM (packaging: pom)
├── core/
│   ├── pom.xml                # Module: core (jar)
│   └── src/
│       ├── main/java/.../     # Package: hu.elte.team404 (or your choice)
│       └── test/java/...
├── desktop/
│   ├── pom.xml                # Module: desktop (jar, depends on core)
│   └── src/
│       └── main/java/.../
└── assets/                    # Optional: for desktop to load
```

**Parent `pom.xml` requirements:**
- `packaging: pom`
- `modules: core, desktop`
- Java version: 17 or 21
- `sourceEncoding: UTF-8`

**Acceptance criteria:**
- [ ] `mvn clean install` succeeds from project root
- [ ] `core` module compiles without LibGDX (only in `desktop`)
- [ ] `desktop` module depends on `core`

---

### 1.2 LibGDX Dependencies in `pom.xml`

**Goal:** Add LibGDX to the `desktop` module only (core stays LibGDX-free).

**Dependencies to add in `desktop/pom.xml`:**
```xml
<!-- LibGDX core (only needed in desktop for ApplicationAdapter) -->
<dependency>
    <groupId>com.badlogicgames.gdx</groupId>
    <artifactId>gdx</artifactId>
    <version>1.12.1</version>
</dependency>
<dependency>
    <groupId>com.badlogicgames.gdx</groupId>
    <artifactId>gdx-backend-lwjgl3</artifactId>
    <version>1.12.1</version>
</dependency>
<dependency>
    <groupId>com.badlogicgames.gdx</groupId>
    <artifactId>gdx-platform</artifactId>
    <version>1.12.1</version>
    <classifier>natives-desktop</classifier>
</dependency>
```

**Acceptance criteria:**
- [ ] `desktop` can import `com.badlogic.gdx.*`
- [ ] `core` has no `com.badlogic.gdx` imports

---

### 1.3 `GameState` Class

**Goal:** Single source of truth for all game data. Aggregates map, economy, time state, pause flag.

**Package:** `core` → `model` (e.g. `hu.elte.team404.model`)

**Required fields (minimal for Week 1):**
```java
public class GameState {
    private final GameMap gameMap;
    private final Economy economy;
    private boolean paused;
    private float gameTime;  // in-game time (seconds)

    // Constructor, getters, setters
    public void setPaused(boolean paused) { ... }
    public void advanceTime(float delta) { ... }  // only if !paused
}
```

**Rules:**
- No LibGDX imports
- Immutable or carefully encapsulated; avoid exposing mutable internals

**Acceptance criteria:**
- [ ] `GameState` holds `GameMap` and `Economy`
- [ ] `paused` and `gameTime` can be read/updated
- [ ] Unit test: `GameState` advances time when not paused

---

### 1.4 `Economy` Model

**Goal:** Track player capital and bankruptcy.

**Package:** `core` → `model.economy`

**Required:**
```java
public class Economy {
    private long capital;  // in cents or smallest unit to avoid float

    public Economy(long initialCapital) { ... }
    public long getCapital() { ... }
    public void addIncome(long amount) { ... }
    public void deductCost(long amount) { ... }
    public boolean isBankrupt() { return capital < 0; }
}
```

**Acceptance criteria:**
- [ ] `isBankrupt()` returns true when `capital < 0`
- [ ] Unit test: add income, deduct cost, check bankruptcy

---

## 2. Harry — Launcher, Renderer, GameClock

### 2.1 `DesktopLauncher`

**Goal:** Create window and run the LibGDX game loop.

**Package:** `desktop` → `hu.elte.team404.desktop`

**Implementation:**
```java
public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Mini Transport Tycoon");
        config.setWindowedMode(1280, 720);
        new Lwjgl3Application(new GameApplication(), config);
    }
}
```

`GameApplication` extends `ApplicationAdapter` and implements:
- `create()` — create `GameState`, `GameRenderer`, `GameController`, `InputController`, `GameClock`
- `render()` — call input → controller update → renderer

**Acceptance criteria:**
- [ ] Window opens with title "Mini Transport Tycoon"
- [ ] No crash on run; window closes cleanly

---

### 2.2 `GameRenderer` Interface

**Goal:** Abstraction for rendering; allows 2D/3D swap later.

**Package:** `core` → `view` (core can have interface; implementation in desktop or core with LibGDX in desktop)

**Note:** Interface lives in `core` but must not depend on LibGDX. Implementation (`Renderer2D`) will need LibGDX — so either:
- **Option A:** Interface in `core`, implementation in `desktop` (desktop imports core + LibGDX)
- **Option B:** Both in `core`; `core` depends on `gdx` only for `view` package (breaks "core has no LibGDX" rule)

**Recommended:** Interface in `core`, implementation in `desktop`. The `core` view package can have the interface; the concrete `Renderer2D` is in `desktop` if it needs LibGDX. Actually, `Renderer2D` will need `SpriteBatch`, `OrthographicCamera` — those are LibGDX. So:
- `GameRenderer` interface in `core` (no LibGDX)
- `Renderer2D` in `desktop` (implements interface, uses LibGDX)

**Interface:**
```java
public interface GameRenderer {
    void render(GameState state, float delta);
    void resize(int width, int height);  // optional, for camera
}
```

**Acceptance criteria:**
- [ ] Interface in `core`, no LibGDX dependencies
- [ ] Single method `render(GameState, float)` at minimum

---

### 2.3 `Renderer2D` Skeleton

**Goal:** Implement `GameRenderer`; for Week 1, just clear screen (e.g. dark gray).

**Package:** `desktop` → `view` (or `core` if core has gdx dependency — but we said core should not)

**Implementation (in desktop):**
```java
public class Renderer2D implements GameRenderer {
    private OrthographicCamera camera;
    private SpriteBatch batch;

    @Override
    public void create() { ... }  // init camera, batch
    @Override
    public void render(GameState state, float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        // Later: draw map, etc.
    }
}
```

**Acceptance criteria:**
- [ ] Screen clears to a solid color (no flicker)
- [ ] No errors when `GameState` is passed in

---

### 2.4 `GameClock`

**Goal:** Manage game time and speed (pause, normal, fast, very fast).

**Package:** `core` → `infrastructure` (or `desktop` if it uses LibGDX — it shouldn't)

**Behavior:**
- `pause` → speed = 0
- `normal` → speed = 1
- `fast` → speed = 2
- `very fast` → speed = 4

**Interface:**
```java
public class GameClock {
    public enum Speed { PAUSE, NORMAL, FAST, VERY_FAST }
    private Speed currentSpeed = Speed.NORMAL;
    private float gameTime;  // accumulated game seconds

    public void setSpeed(Speed speed) { ... }
    public void tick(float realDeltaSeconds) {
        if (currentSpeed != Speed.PAUSE) {
            gameTime += realDeltaSeconds * getMultiplier();
        }
    }
    public float getGameTime() { return gameTime; }
    public boolean isPaused() { return currentSpeed == Speed.PAUSE; }
    private float getMultiplier() { ... }  // 1, 2, or 4
}
```

**Acceptance criteria:**
- [ ] When paused, `gameTime` does not advance
- [ ] When normal, 1 real second → 1 game second
- [ ] When fast, 1 real second → 2 game seconds
- [ ] Unit test: tick with different speeds

---

## 3. Komron — Map Model

### 3.1 `GameMap` — Grid-Based Map

**Goal:** 2D grid of tiles. Access by (x, y).

**Package:** `core` → `model.map`

**Structure:**
```java
public class GameMap {
    private final int width;
    private final int height;
    private final Tile[][] tiles;

    public GameMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        // Initialize all tiles as EMPTY
    }
    public Tile getTile(int x, int y) { ... }
    public void setTile(int x, int y, Tile tile) { ... }
    public boolean isInBounds(int x, int y) { ... }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
```

**Acceptance criteria:**
- [ ] `getTile(x, y)` returns tile at position
- [ ] Out-of-bounds access throws or returns null (document which)
- [ ] Unit test: create 10×10 map, set and get tile

---

### 3.2 `Tile` and `TileType`

**Goal:** Each tile has a type. Extensible for future (road, water, forest).

**Package:** `core` → `model.map`

```java
public enum TileType {
    EMPTY,
    ROAD,      // Week 2
    WATER,     // Sub-task
    FOREST     // Sub-task
}

public class Tile {
    private final TileType type;
    // Optional: x, y for convenience

    public Tile(TileType type) { ... }
    public TileType getType() { ... }
}
```

**Acceptance criteria:**
- [ ] `Tile` can be created with a type
- [ ] `GameMap` initializes all tiles as `EMPTY`

---

### 3.3 `City` Model

**Goal:** City occupies at least 3×3 tiles. Has position and size.

**Package:** `core` → `model.map`

```java
public class City {
    private final int x;       // top-left corner
    private final int y;
    private final int width;  // >= 3
    private final int height; // >= 3
    private final String name;

    public City(int x, int y, int width, int height, String name) {
        if (width < 3 || height < 3) throw new IllegalArgumentException(...);
        ...
    }
    public boolean contains(int tileX, int tileY) { ... }
    public int getX() { ... }
    public int getY() { ... }
    // getters
}
```

**Acceptance criteria:**
- [ ] City occupies at least 3×3
- [ ] `contains(tileX, tileY)` returns true for tiles inside city
- [ ] Unit test: `City(0,0,3,3,"A").contains(1,1)` is true

---

### 3.4 `IndustrialFacility` Model

**Goal:** Facility occupies at least 2×2 tiles. Has type (mine, farm, factory).

**Package:** `core` → `model.map`

```java
public enum FacilityType {
    MINE, FARM, FACTORY, STEEL_MILL  // examples
}

public class IndustrialFacility {
    private final int x;
    private final int y;
    private final int width;   // >= 2
    private final int height;  // >= 2
    private final FacilityType type;

    public IndustrialFacility(int x, int y, int width, int height, FacilityType type) {
        if (width < 2 || height < 2) throw new IllegalArgumentException(...);
        ...
    }
    public boolean contains(int tileX, int tileY) { ... }
    // getters
}
```

**Acceptance criteria:**
- [ ] Facility occupies at least 2×2
- [ ] `contains(tileX, tileY)` works correctly
- [ ] Unit test: facility at (5,5) 2×2 contains (5,5) and (6,6)

---

## 4. Uluk — Game Loop & Controller Wiring

### 4.1 `GameController`

**Goal:** Orchestrate game update. Called each frame when not paused.

**Package:** `core` → `controller`

```java
public class GameController {
    private final GameState gameState;
    private final GameClock gameClock;

    public GameController(GameState gameState, GameClock gameClock) {
        this.gameState = gameState;
        this.gameClock = gameClock;
    }

    public void update(float realDeltaSeconds) {
        if (gameState.isPaused()) return;
        gameClock.tick(realDeltaSeconds);
        gameState.advanceTime(gameClock.getLastTickDelta());  // or similar
        // Later: vehicle movement, economy tick, etc.
    }
}
```

**Acceptance criteria:**
- [ ] When paused, `update` does nothing
- [ ] When not paused, time advances
- [ ] No LibGDX in `GameController`

---

### 4.2 `InputController`

**Goal:** Read input and forward to game state (e.g. pause toggle).

**Package:** `core` → `controller` — but input needs LibGDX (`Gdx.input`). So either:
- `InputController` in `desktop` (has LibGDX)
- Or `InputController` interface in `core`, implementation in `desktop`

**Recommended:** Interface in `core`:
```java
public interface InputHandler {
    void handleInput();  // Called each frame
}
```

Implementation in `desktop`:
```java
public class InputController implements InputHandler {
    private final GameState gameState;
    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            gameState.setPaused(!gameState.isPaused());
        }
    }
}
```

**Acceptance criteria:**
- [ ] SPACE toggles pause
- [ ] Game responds (pause when SPACE pressed)

---

### 4.3 Wire MVC in Launcher

**Goal:** In `GameApplication.create()` and `render()`, wire everything.

**Flow:**
1. Create `GameMap`, `Economy`, `GameState`
2. Create `GameClock`, `GameController`, `InputController` (desktop impl)
3. Create `GameRenderer` (Renderer2D)
4. In `render()`: `inputController.handleInput()` → `gameController.update(delta)` → `gameRenderer.render(gameState, delta)`

**Acceptance criteria:**
- [ ] Game runs without crash
- [ ] Pause toggles with SPACE
- [ ] Screen clears (Renderer2D)
- [ ] Time advances when not paused (visual proof in Week 2 with UI)

---

### 4.4 JUnit + Mockito Setup

**Goal:** Add test dependencies to `core`, write first tests.

**In `core/pom.xml`:**
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.8.0</version>
    <scope>test</scope>
</dependency>
```

**First tests (examples):**
- `EconomyTest`: `addIncome`, `deductCost`, `isBankrupt`
- `GameMapTest`: `getTile`, `setTile`, `isInBounds`
- `CityTest`: `contains`
- `GameClockTest`: `tick` when paused vs not paused

**Acceptance criteria:**
- [ ] `mvn test` runs and passes
- [ ] At least 3 unit tests for Model classes

---

## 5. Week 1 Demo Checklist

| Item | Owner | Status |
|------|-------|--------|
| Window opens | Harry | |
| Time controls (pause/normal/fast/very fast) | Harry | |
| SPACE toggles pause | Uluk | |
| Empty map grid exists in memory | Komron | |
| GameState, Economy, GameMap in place | Ramez | |
| Unit tests pass | Uluk | |

---

## 6. Dependency Order (Who Blocks Whom)

```
Ramez (Maven, GameState, Economy) ──┐
                                   ├──► Uluk (wire MVC) ──► Harry (render in GameApplication)
Komron (GameMap, Tile, City, Facility) ───┘
```

**Sync point:** After Ramez and Komron have Model and GameMap, Uluk can wire. Harry can work on DesktopLauncher, GameRenderer, GameClock in parallel from Day 1.

---

## 7. Suggested File Layout After Week 1

```
core/
├── pom.xml
└── src/main/java/hu/elte/team404/
    ├── model/
    │   ├── GameState.java
    │   ├── economy/
    │   │   └── Economy.java
    │   └── map/
    │       ├── GameMap.java
    │       ├── Tile.java
    │       ├── TileType.java
    │       ├── City.java
    │       └── IndustrialFacility.java
    ├── controller/
    │   └── GameController.java
    ├── view/
    │   └── GameRenderer.java
    └── infrastructure/
        └── GameClock.java

desktop/
├── pom.xml
└── src/main/java/hu/elte/team404/desktop/
    ├── DesktopLauncher.java
    ├── GameApplication.java
    ├── view/
    │   └── Renderer2D.java
    └── controller/
        └── InputController.java
```

---

*Document version: 1.0*  
*Team 404 — Mini Transport Tycoon*
