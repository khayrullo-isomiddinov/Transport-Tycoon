# Mini Transport Tycoon — TODO List (6 Weeks)

**Team 404:** Ramez | Harry | Komron | Uluk  
**Deadline:** 6 weeks

---

## Week 1 — Foundation

> **Detailed specification:** [docs/FOUNDATION.md](docs/FOUNDATION.md) — acceptance criteria, code examples, dependency order.

### Ramez
- [ ] Maven project setup (parent POM, core + desktop modules)
- [ ] LibGDX dependencies in `pom.xml`
- [ ] `GameState` class (aggregate root)
- [ ] `Economy` model (capital, bankruptcy check)

### Harry
- [ ] `DesktopLauncher` — window, LibGDX ApplicationAdapter
- [ ] `GameRenderer` interface
- [ ] `Renderer2D` skeleton (empty render)
- [ ] `GameClock` — time management (pause/normal/fast/very fast)

### Komron
- [ ] `GameMap` — grid-based map data structure
- [ ] `Tile`, `TileType` enum
- [ ] `City` model (3×3 min area)
- [ ] `IndustrialFacility` model (2×2 min area)

### Uluk
- [ ] `GameController` — main game loop, `update(delta)`
- [ ] `InputController` — basic input handling
- [ ] Wire MVC in launcher (Controller → Model → View)
- [ ] JUnit + Mockito setup, first unit tests

**Week 1 demo:** Window opens, time controls work, empty map grid.

---

## Week 2 — Map, Roads & Economy

### Ramez
- [ ] `Road` model, road network logic
- [ ] `Stop` model
- [ ] `GoodsType`, `Demand` models
- [ ] Facility production/consumption logic
- [ ] City demands (passengers, products)

### Harry
- [ ] `MapView` — render tiles, roads, cities, facilities
- [ ] Tile sprites (placeholders or simple shapes)
- [ ] `StopView` — render stops
- [ ] Time speed controls in UI (pause/normal/fast/very fast)

### Komron
- [ ] `BuildController` — build roads on empty tiles
- [ ] `BuildController` — place stops along roads
- [ ] Road build cost, validation
- [ ] Predefined road network, city internal roads

### Uluk
- [ ] `EconomyController` — apply costs, check bankruptcy
- [ ] Road cost, vehicle purchase cost (stub)
- [ ] `UIManager` — HUD (capital display)
- [ ] Build road UI, place stop UI (input → BuildController)

**Week 2 demo:** Place roads and stops, see capital change.

---

## Week 3 — Transport Loop

### Ramez
- [ ] `Vehicle`, `VehicleType` models (≥2 cargo, ≥2 passenger)
- [ ] `Route`, `RouteStop` models
- [ ] `Cargo`, `CargoType` models
- [ ] Demand changes over time logic

### Harry
- [ ] `VehicleView` — render vehicles at positions
- [ ] Route visualization (line/path on map)
- [ ] `UIManager` — economy display (income, costs)
- [ ] Route editor UI (select stops, create route)

### Komron
- [ ] `Pathfinder` — A* on road network
- [ ] `VehicleController` — vehicle movement along route
- [ ] One vehicle per direction per tile rule
- [ ] Loading/unloading at stops logic

### Uluk
- [ ] `RouteController` — create/edit routes, assign vehicles
- [ ] Income from delivered cargo/passengers
- [ ] `EconomyController` — integrate income each tick
- [ ] Map generation (Perlin/Simplex) — *sub-task*

**Week 3 demo:** Full transport loop, vehicles move, profit/loss visible.

---

## Week 4 — Sub-tasks: Polish & Map Features

### Ramez
- [ ] Forests — trees on tiles, growth over time
- [ ] Forests — road build on forest (higher cost)
- [ ] Rivers and lakes — water tiles, bridge placement

### Harry
- [ ] Continuous/smooth vehicle movement animation
- [ ] Scrollable map (camera pan)
- [ ] Minimap — navigable minimap in UI

### Komron
- [ ] Rivers and lakes — 3 bridge types (cost, distance, speed limit)
- [ ] Bridge building logic in `BuildController`

### Uluk
- [ ] `MinimapView` — render minimap
- [ ] Minimap click → jump camera to position
- [ ] Integration testing, bug fixes

**Week 4 demo:** Smooth movement, minimap, forests, rivers & bridges.

---

## Week 5 — Persistence & City Growth

### Ramez
- [ ] City growth — size increases over time
- [ ] City growth — faster with goods/passenger traffic
- [ ] City expansion into adjacent tiles (new buildings, roads)

### Harry
- [ ] Save/load — serialize `GameState` to file
- [ ] Save/load — load game, resume vehicles in motion
- [ ] Multiple save files support
- [ ] Save/load UI (menu or buttons)

### Komron
- [ ] `SaveController` — orchestrate save/load
- [ ] `SaveFileHandler` — file I/O, format (JSON or custom)
- [ ] Garage — build garage, connect to road *(optional sub-task)*

### Uluk
- [ ] Garage — vehicle purchase at garage *(optional)*
- [ ] Garage — maintenance intervals, sell old vehicles *(optional)*
- [ ] Bug fixes, integration

**Week 5 demo:** Save/load works, cities grow.

---

## Week 6 — Integration & Release

### Ramez
- [ ] End-to-end testing
- [ ] Bug fixing (economy, demands, city growth)
- [ ] Code review support

### Harry
- [ ] Final UI polish
- [ ] Performance tuning (rendering, minimap)
- [ ] Demo preparation (script, test scenario)

### Komron
- [ ] Bug fixing (pathfinding, vehicles, roads)
- [ ] Edge cases (bankruptcy, empty routes)

### Uluk
- [ ] README — setup, run, build instructions
- [ ] Documentation (architecture, how to play)
- [ ] Final submission packaging

**All**
- [ ] Final integration run
- [ ] Demo rehearsal
- [ ] Submit

---

## Summary by Person

| Person  | Focus areas |
|---------|-------------|
| **Ramez** | Model (economy, goods, demands), city growth, forests |
| **Harry** | View (rendering, UI), save/load, minimap, polish |
| **Komron** | Map, BuildController, pathfinding, vehicles, bridges |
| **Uluk** | Game loop, controllers, route logic, map generation, docs |

---

## Quick Reference — Who Does What

| Task | Owner |
|------|-------|
| Maven, GameState, Economy | Ramez |
| Rendering, UI, GameClock | Harry |
| Map, BuildController, Pathfinder | Komron |
| GameController, InputController, RouteController | Uluk |
| Vehicles (model: Ramez, logic: Komron, view: Harry) | Shared |
| Save/load | Harry + Komron |
| City growth | Ramez |
| Forests, rivers | Ramez + Komron |
| Minimap | Harry + Uluk |
| **Terrain** (if selected) | See [docs/TERRAIN_SUBTASK.md](docs/TERRAIN_SUBTASK.md) |
| Docs, README | Uluk |

---

## Optional: Terrain Sub-task (1 complexity)

If you select Terrain instead of other sub-tasks, see **[docs/TERRAIN_SUBTASK.md](docs/TERRAIN_SUBTASK.md)** for:
- Model: tile height, slope types, terraform validation
- Road build: height diff ≤ 1 between adjacent tiles
- Vehicle speed: slower on uphill
- Terraform: raise/lower empty tiles by 1, cost, max diff 2
- View: slope sprites, terraform UI

---

*Last updated: February 2025*
