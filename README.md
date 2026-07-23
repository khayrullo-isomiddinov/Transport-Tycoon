## Requirements

Install the following before running the game:

* Java Development Kit 17 or newer
* Apache Maven

Check your installations:

```bash
java -version
mvn -version
```

The Java version should be 17 or newer.

---

## Run the game

Clone the repository:

```bash
git clone https://github.com/khayrullo-isomiddinov/Transport-Tycoon.git
cd Transport-Tycoon
```

Build the project:

```bash
mvn clean install
```

Run the desktop game:

```bash
mvn -pl desktop exec:exec
```

You can also build and run it in one command:

```bash
mvn clean install && mvn -pl desktop exec:exec
```

### macOS

Some macOS setups require the JVM to start on the first thread.

Use:

```bash
mvn clean install
mvn -pl desktop exec:exec \
  -Dexec.args="-XstartOnFirstThread -classpath %classpath com.team404.tycoon.desktop.DesktopLauncher"
```

---

## Controls

### Mouse

| Action                           | Control               |
| -------------------------------- | --------------------- |
| Place or use selected item       | Left click            |
| Remove an item or sell a vehicle | Right click           |
| Place items across several tiles | Left-click and drag   |
| Move around the map              | Scroll                |
| Zoom                             | Ctrl + scroll         |
| Jump using the minimap           | Left click on minimap |

### Keyboard

| Key   | Action                               |
| ----- | ------------------------------------ |
| `1`   | Select passenger bus purchases       |
| `2`   | Select goods truck purchases         |
| `L`   | Show details for the hovered object  |
| `Z`   | Reduce horizontal green-light time   |
| `X`   | Increase horizontal green-light time |
| `C`   | Reduce vertical green-light time     |
| `V`   | Increase vertical green-light time   |
| `R`   | Reset traffic-light timing           |
| `Esc` | Open the pause menu                  |

Game speed can also be changed using the buttons in the interface.

---

## How a basic game works

1. Start a new company.
2. Choose the map size and difficulty.
3. Look for towns and useful road connections.
4. Build or extend roads.
5. Place and connect a garage.
6. Choose a bus or truck.
7. Buy the vehicle through the garage.
8. Let it carry passengers or goods.
9. Earn delivery income.
10. Keep enough money for construction and maintenance.

For example, a bus costs money upfront and continues to create maintenance costs as it gets older. It needs a usable route to earn that money back.

So buying more vehicles is not always the best move.

---

## Economy values

The current balance settings are kept in one Java configuration class.

Some current values are:

| Item                       |  Value |
| -------------------------- | -----: |
| One road tile              |    500 |
| Passenger bus              | 25,000 |
| Goods truck                | 22,000 |
| Bus resale value           | 12,500 |
| Truck resale value         | 11,000 |
| Terraforming one tile      |  2,000 |
| Base vehicle maintenance   |    500 |
| Revenue per passenger unit |     50 |
| Revenue per goods unit     |     80 |

These values may change as the game is balanced further.

---

## Build details

The desktop application starts from:

```text
com.team404.tycoon.desktop.DesktopLauncher
```

The default window settings are:

* 1280 × 720
* vertical sync enabled
* foreground frame rate limited to 60 FPS

---

## Current project state

The game has a working simulation and several connected systems, but it is still a student project under development.

Some parts may need more balancing, testing, UI cleanup, and documentation.

There is currently no packaged installer or downloadable release. The project needs to be built from source with Maven.

---

## Possible next steps

Some useful future improvements would be:

* save and load support
* a clearer route editor
* more vehicle types
* rail and water transport
* sound and music
* better building categories
* more automated tests
* packaged releases for Windows, macOS, and Linux
* gameplay screenshots and a short demo GIF

---

## Contributing

Contributions are welcome.

Before making a large change, open an issue and describe what you plan to work on.

A basic contribution flow looks like this:

```bash
git checkout -b feature/my-change
mvn test
git add .
git commit -m "Add my change"
git push origin feature/my-change
```

Then open a pull request.

Please keep game logic in the `core` module when possible. Desktop rendering and input code belong in the `desktop` module.

---

## Team

Built by **Team 404** for the Software Technology course at ELTE.

Repository maintained by [Khayrullo Isomiddinov](https://github.com/khayrullo-isomiddinov).

---

## License

No license file is currently included.

That means the source is publicly visible, but standard open-source reuse permissions have not been granted yet.

Add a `LICENSE` file before describing the project as open source.


# Mini Transport Tycoon

A small isometric transport management game built with Java and LibGDX.

Build roads. Connect towns. Buy buses and trucks. Move passengers and goods. And try not to run your company into bankruptcy.

This project was created by Team 404 as part of the Software Technology course at ELTE.

---

## About the game

Mini Transport Tycoon is a desktop transport simulation game.

You start with a generated map containing towns, industrial areas, forests, water, and different terrain heights. Your job is to build a working road network and run a transport company on top of it.

Passenger buses move people between towns. Trucks deliver goods. Successful deliveries earn money, but roads, vehicles, terraforming, and maintenance all cost money too.

The game ends when your company can no longer stay financially alive.

## Main features

### Build a road network

Place roads across the map and connect important locations.

Road tiles automatically change into corners, straight sections, T-junctions, and intersections based on nearby roads.

Road placement also checks terrain height and nearby road connections. So roads cannot simply be placed anywhere without limits.

### Transport passengers and goods

The game supports two transport types:

* passenger buses
* goods trucks

Vehicles follow routes between towns and transport demand is handled by the simulation.

Passenger and goods deliveries have different income values.

### Buy vehicles through garages

Garages are part of the transport system, not just map decorations.

Connect a garage to a valid road network and use it to buy vehicles for nearby usable routes.

You can choose between:

* `1` — passenger bus
* `2` — goods truck

Vehicles can also be sold back for part of their original price.

### Manage the company economy

Every decision affects your balance.

Current costs include:

* road construction
* buses and trucks
* vehicle maintenance
* raising or lowering terrain

Income comes from completed passenger and goods deliveries.

Vehicles become more expensive to maintain as they age. Poor planning can push the company into bankruptcy.

### Shape the terrain

Raise and lower individual map tiles to prepare land for roads and buildings.

Terraforming costs money, and road construction is limited when neighbouring terrain is too steep.

### Working traffic lights

Traffic lights switch between horizontal and vertical directions.

Vehicles detect them and wait when their direction has a red light.

The timing can also be changed while playing.

### Random maps

Each game starts on a generated map.

The generator places terrain, water, forests, towns, industrial facilities, and starter road areas.

You can also choose the map size before starting.

### Difficulty settings

The selected difficulty changes the amount of starting money available to the company.

This means the same map can require a different strategy depending on the chosen setting.

### Clickable minimap

The minimap shows the main terrain types and the area currently visible on screen.

Click anywhere on it to move the camera directly to that part of the map.

### Object and terrain information

Hover over the map to see information about the selected tile.

Press `L` while hovering over an object to open more detailed information.

### Adjustable simulation speed

The game supports four speed states:

* paused
* normal
* fast
* very fast

This makes it easier to stop and plan or speed up vehicle movement and income.

---

## Tech stack

* **Java 17**
* **LibGDX 1.14**
* **LWJGL3**
* **Maven**
* **JUnit 5**
* **JaCoCo**

The project uses a multi-module Maven structure:

```text
Transport-Tycoon/
├── core/       Game rules, models, controllers, and simulation logic
├── desktop/    LibGDX rendering, desktop input, HUD, and launcher
├── resources/  Game sprites and other PNG assets
└── pom.xml     Parent Maven configuration
```

The core and desktop parts are kept separate.

The `core` module contains logic that does not depend on the desktop window. The `desktop` module contains rendering, input handling, UI code, and the LWJGL3 launcher.

---

