# Mini Transport Tycoon — Software Planning Document

**Team 404** | 4-person team | ELTE Faculty of Informatics 2025/2026 Spring  
**Methodology:** Scrum  
**Target complexity:** 5 units (base 1.5 + sub-tasks 3.5)

---

## 1. Project Overview

### 1.1 Vision
A simplified transportation-economic simulation game where players organize road freight and passenger transport between cities and industrial facilities to maximize profit.

### 1.2 Scope Summary
| Component | Complexity | Description |
|-----------|------------|-------------|
| **Base game** | 1.5 units | Map, roads, goods, vehicles, stops, routes, economy, time, display |
| **Sub-tasks** | 3.5 units | Selected from optional features (see §4) |
| **Total** | 5 units | Required for full grade |

### 1.3 Suggested Sub-task Selection (3.5 units)
*Adjust after consultation with lab instructor.*

| Sub-task | Complexity | Rationale |
|----------|------------|-----------|
| Minimap | 0.5 | Core UX, enables large maps |
| Persistence | 0.5 | Essential for playability |
| Continuous movement | 0.5 | Improves polish |
| Forests | 0.5 | Adds map variety |
| Rivers and Lakes | 0.5 | Adds map variety, bridges |
| Map Generation | 0.5 | Procedural maps instead of fixed |
| City Growth | 0.5 | Dynamic gameplay |
| **Total** | **3.5** | |

*Alternatives:* Terrain (1), Traffic Lights (1), Garage (0.5), Overtaking (0.5), 2.5D Graphics (0.5)

---

## 2. Scrum Framework

### 2.1 Roles (4-person team)

| Role | Responsibility | Suggested owner |
|------|----------------|-----------------|
| **Product Owner** | Backlog, priorities, acceptance criteria | Member 1 |
| **Scrum Master** | Ceremonies, impediments, process | Member 2 |
| **Developer** | Implementation | Members 3 & 4 |
| **Developer** | Implementation | All 4 (shared) |

*Note: In a 4-person team, everyone codes; PO and SM are part-time.*

### 2.2 Ceremonies

| Ceremony | Frequency | Duration | Purpose |
|----------|-----------|----------|---------|
| **Sprint Planning** | Start of sprint | 1–2 h | Select backlog items, define sprint goal |
| **Daily Standup** | Daily | 15 min | Progress, blockers, next steps |
| **Sprint Review** | End of sprint | 1 h | Demo, feedback |
| **Sprint Retrospective** | End of sprint | 30–45 min | Process improvements |
| **Backlog Refinement** | Mid-sprint | 30 min | Clarify and estimate upcoming items |

### 2.3 Sprint Cadence
- **Sprint length:** 2 weeks
- **Semester:** ~14 weeks → ~7 sprints
- **Buffer:** 1 sprint for integration, polish, exams

---

## 3. Product Backlog

### 3.1 Epic Structure

```
Epic 1: Core Infrastructure
Epic 2: Map & World
Epic 3: Economy & Simulation
Epic 4: Vehicles & Transport
Epic 5: UI & Display
Epic 6: Sub-task Features
```

### 3.2 Product Backlog Items (PBIs)

#### Epic 1: Core Infrastructure
| ID | PBI | Priority | Estimate | Sprint |
|----|-----|----------|----------|--------|
| PBI-101 | Project setup, tech stack, repo structure | P0 | 3 | 1 |
| PBI-102 | Game loop, time management (pause/normal/fast/very fast) | P0 | 5 | 1 |
| PBI-103 | Event system / message bus | P0 | 3 | 1 |
| PBI-104 | Unit test framework setup | P1 | 2 | 1 |

#### Epic 2: Map & World
| ID | PBI | Priority | Estimate | Sprint |
|----|-----|----------|----------|--------|
| PBI-201 | Grid-based map data structure | P0 | 5 | 1 |
| PBI-202 | Cities (3×3 min), industrial facilities (2×2 min) | P0 | 5 | 2 |
| PBI-203 | Predefined road network, city internal roads | P0 | 5 | 2 |
| PBI-204 | Road construction on empty tiles | P0 | 5 | 2 |
| PBI-205 | Stops placement along roads | P0 | 3 | 2 |
| PBI-206 | [Sub] Map generation (Perlin/Simplex/WFC) | P1 | 5 | 3 |
| PBI-207 | [Sub] Forests (trees, growth, clearing cost) | P1 | 3 | 4 |
| PBI-208 | [Sub] Rivers and lakes, 3 bridge types | P1 | 5 | 4 |

#### Epic 3: Economy & Simulation
| ID | PBI | Priority | Estimate | Sprint |
|----|-----|----------|----------|--------|
| PBI-301 | Initial capital, bankruptcy condition | P0 | 2 | 2 |
| PBI-302 | Goods types (≥3) + passengers | P0 | 5 | 2 |
| PBI-303 | Facility production/consumption logic | P0 | 5 | 2 |
| PBI-304 | City demands (passengers, products) | P0 | 3 | 2 |
| PBI-305 | Demand changes over time | P0 | 3 | 3 |
| PBI-306 | Road build cost, vehicle purchase/maintenance costs | P0 | 3 | 2 |
| PBI-307 | Income from delivered cargo/passengers | P0 | 3 | 3 |
| PBI-308 | [Sub] City growth (size, expansion, traffic influence) | P1 | 5 | 5 |

#### Epic 4: Vehicles & Transport
| ID | PBI | Priority | Estimate | Sprint |
|----|-----|----------|----------|--------|
| PBI-401 | Vehicle types (≥2 cargo, ≥2 passenger) | P0 | 5 | 3 |
| PBI-402 | Route definition (circular A→B→C→A) | P0 | 5 | 3 |
| PBI-403 | Vehicle movement, pathfinding | P0 | 8 | 3 |
| PBI-404 | Loading/unloading at stops | P0 | 5 | 3 |
| PBI-405 | One vehicle per direction per tile rule | P0 | 5 | 3 |
| PBI-406 | [Sub] Continuous/smooth movement animation | P1 | 5 | 4 |
| PBI-407 | [Sub] Garage (build, purchase, maintenance, sell) | P1 | 5 | 5 |
| PBI-408 | [Sub] Overtaking logic | P1 | 5 | 6 |

#### Epic 5: UI & Display
| ID | PBI | Priority | Estimate | Sprint |
|----|-----|----------|----------|--------|
| PBI-501 | 2D top-down tile-based renderer | P0 | 5 | 2 |
| PBI-502 | Tile sprites (facilities, cities, roads) | P0 | 5 | 2 |
| PBI-503 | Vehicle, stop, route visualization | P0 | 3 | 3 |
| PBI-504 | Build road UI, place stop UI | P0 | 3 | 3 |
| PBI-505 | Route editor UI | P0 | 5 | 3 |
| PBI-506 | Economy/balance display (capital, income, costs) | P0 | 2 | 3 |
| PBI-507 | Time speed controls | P0 | 2 | 2 |
| PBI-508 | [Sub] Scrollable map, minimap | P1 | 5 | 4 |
| PBI-509 | [Sub] Save/load game, multiple save files | P1 | 5 | 5 |

*Estimate scale: 1 = ~2h, 2 = ~4h, 3 = ~6h, 5 = ~1 day, 8 = ~1.5 days (per person)*

---

## 4. Sprint Plan (7 sprints)

### Sprint 1 — Foundation (Weeks 1–2)
**Goal:** Runnable game shell with time control and map skeleton.

| PBI | Owner | Definition of Done |
|-----|-------|--------------------|
| PBI-101 | All | Repo, build, run, README |
| PBI-102 | Dev 1 | Pause/normal/fast/very fast working |
| PBI-103 | Dev 2 | Event system usable |
| PBI-104 | Dev 3 | Tests run in CI |
| PBI-201 | Dev 4 | Map grid, tile access API |

**Sprint 1 demo:** Empty map with time controls.

---

### Sprint 2 — Map & Roads (Weeks 3–4)
**Goal:** Buildable roads, cities, facilities, basic economy.

| PBI | Owner | Definition of Done |
|-----|-------|--------------------|
| PBI-202 | Dev 1 | Cities and facilities on map |
| PBI-203 | Dev 2 | Road network, city internal roads |
| PBI-204 | Dev 3 | Build roads, cost deducted |
| PBI-205 | Dev 4 | Place stops |
| PBI-301, 306 | Dev 1 | Capital, costs |
| PBI-501, 502 | Dev 2 | Tile renderer, sprites |
| PBI-507 | Dev 3 | Time controls in UI |

**Sprint 2 demo:** Place roads and stops, see capital change.

---

### Sprint 3 — Transport Loop (Weeks 5–6)
**Goal:** Vehicles move, load, deliver; economy runs.

| PBI | Owner | Definition of Done |
|-----|-------|--------------------|
| PBI-302, 303, 304 | Dev 1 | Goods, production, consumption |
| PBI-401, 402 | Dev 2 | Vehicle types, routes |
| PBI-403, 404, 405 | Dev 3 | Movement, loading, tile rules |
| PBI-305, 307 | Dev 4 | Demand changes, income |
| PBI-503, 504, 505, 506 | Dev 1–4 | UI for routes, economy display |
| PBI-206 | Dev 2 | Map generation (if selected) |

**Sprint 3 demo:** Full transport loop, profit/loss visible.

---

### Sprint 4 — Polish & Map Features (Weeks 7–8)
**Goal:** Smooth movement, minimap, forests, rivers.

| PBI | Owner | Definition of Done |
|-----|-------|--------------------|
| PBI-406 | Dev 1 | Smooth vehicle animation |
| PBI-508 | Dev 2 | Scrollable map, minimap |
| PBI-207 | Dev 3 | Forests, tree growth |
| PBI-208 | Dev 4 | Rivers, lakes, bridges |

**Sprint 4 demo:** Playable game with sub-task map features.

---

### Sprint 5 — Persistence & Growth (Weeks 9–10)
**Goal:** Save/load, city growth, garage (if selected).

| PBI | Owner | Definition of Done |
|-----|-------|--------------------|
| PBI-509 | Dev 1 | Save/load, multiple files |
| PBI-308 | Dev 2 | City growth |
| PBI-407 | Dev 3 | Garage (if selected) |
| Dev 4 | Bug fixes, integration | |

**Sprint 5 demo:** Save/load, growing cities.

---

### Sprint 6 — Advanced Features (Weeks 11–12)
**Goal:** Overtaking, traffic lights, or other remaining sub-tasks.

| PBI | Owner | Definition of Done |
|-----|-------|--------------------|
| PBI-408 | Dev 1 | Overtaking (if selected) |
| Traffic Lights | Dev 2 | If selected |
| Remaining sub-tasks | Dev 3, 4 | Per selection |
| Integration | All | End-to-end testing |

**Sprint 6 demo:** All selected sub-tasks working.

---

### Sprint 7 — Integration & Release (Weeks 13–14)
**Goal:** Stable release, documentation, demo.

| Task | Owner |
|------|-------|
| Bug fixing | All |
| Performance tuning | Dev 1, 2 |
| Documentation, README | Dev 3 |
| Demo preparation | Dev 4 |
| Final submission | All |

---

## 5. Technical Architecture

### 5.1 Tech Stack
| Component | Choice |
|-----------|--------|
| Language | Java 17 or 21 |
| Build | Maven |
| Framework | LibGDX |
| Testing | JUnit 5, Mockito |
| Architecture | **MVC** |

### 5.2 MVC Structure
See **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** for full details.

| Layer | Responsibility |
|-------|----------------|
| **Model** | Game state, entities, business rules (no LibGDX) |
| **View** | Renders Model (GameRenderer, UI) — read-only |
| **Controller** | Input, game loop, updates Model |

### 5.3 Module Structure
```
core/
├── model/          # GameMap, Economy, Vehicle, Route, GameState
├── view/           # GameRenderer, UIManager
├── controller/     # GameController, BuildController, etc.
└── infrastructure/ # Pathfinder, GameClock, AssetManager

desktop/             # Launcher, wires MVC
```

### 5.4 Key Design Decisions
- **Grid:** Integer coordinates, tile-based.
- **Time:** Central clock, speed multiplier.
- **Pathfinding:** A* or similar for road network.
- **Rendering:** Sprite-based; `GameRenderer` interface allows 2D→3D swap later.

---

## 6. Definition of Done

- [ ] Code compiles and runs
- [ ] Unit tests pass (where applicable)
- [ ] Code reviewed by at least one teammate
- [ ] No known critical bugs
- [ ] Documented (comments/README where needed)
- [ ] Meets acceptance criteria from PBI

---

## 7. Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Pathfinding complexity | High | Start with simple BFS; refine later |
| Sub-task scope creep | Medium | Lock selection after Sprint 1 |
| Merge conflicts | Medium | Small branches, frequent integration |
| Exam period overlap | Medium | Light Sprint 7, buffer in Sprint 6 |

---

## 8. Tools & Conventions

| Area | Tool/Convention |
|------|-----------------|
| Repo | Git, main + feature branches |
| Board | GitHub Projects / Jira / Trello |
| Docs | Markdown in `/docs` |
| Commits | Conventional: `feat:`, `fix:`, `docs:` |
| Branching | `feature/PBI-XXX-description` |

---

## 9. Appendix: Sub-task Reference

| Sub-task | Complexity | Dependencies |
|----------|------------|--------------|
| Forests | 0.5 | Map, road construction |
| Rivers and Lakes | 0.5 | Map, road construction |
| Terrain | 1 | Map, road construction, vehicles |
| Garage | 0.5 | Vehicles, economy |
| Overtaking | 0.5 | Vehicles, pathfinding |
| Traffic Lights | 1 | Roads, intersections, vehicles |
| City Growth | 0.5 | Cities, economy, transport |
| Minimap | 0.5 | Map, UI, scrollable view |
| Persistence | 0.5 | Full game state serializable |
| Continuous movement | 0.5 | Vehicle rendering |
| Map Generation | 0.5 | Map structure |
| 2.5D Graphics | 0.5 | Renderer (exclusive with 3D) |
| 3D Graphics | 1 | Renderer (exclusive with 2.5D) |

---

*Document version: 1.0*  
*Last updated: February 2025*  
*Team 404 — Mini Transport Tycoon*
