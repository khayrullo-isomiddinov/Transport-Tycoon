# Publishing on itch.io

Everything needed to put the Windows build on itch.io. Nothing here has been uploaded yet.

---

## 0. Before publishing: permissions (blocking)

This is a group course project with no `LICENSE` file, so copyright is held jointly by its
contributors and nobody has been granted distribution rights. An open-source licence is **not**
required to publish a free game on itch.io, but co-author consent **is**.

- [ ] **Written OK from every contributor** (a message in the team chat is enough):
      Khayrullo Isomiddinov, Ulukbek Tursunov, Komron Asilov, Ramal Hasanli.
      Agree on how the team is credited (suggested: "Team 404", with names on the page).
- [ ] **Sprite provenance.** Confirm every PNG in `resources/` was made by the team, or record
      its source and licence. Third-party art without a licence that allows redistribution
      must be replaced before publishing.
- [ ] **Course rules.** Check that the ELTE Software Technology course allows publishing
      coursework (the repo is already public on GitHub, which suggests it does, but confirm).
- [ ] Optional: if the team wants the code to be open source, agree on a licence (e.g. MIT)
      and add `LICENSE`. Until then, keep the itch page's licence field unset / "All rights reserved".

---

## 1. Build the release zip

Use the CI build for the public upload: it bundles an OpenJDK (Eclipse Temurin) runtime.

**Option A: GitHub Actions (recommended)**
1. Commit and push the release changes to `main`. The **Run workflow** button only appears once
   `.github/workflows/release.yml` is on the default branch.
2. GitHub, then **Actions**, then **Release build**, then **Run workflow**. Or push a tag:
   `git tag v0.1.0 && git push origin v0.1.0` (this also creates a GitHub Release with the zip).
3. Download the artifact `MiniTransportTycoon-0.1.0-windows-x64` (it is a zip containing the zip).

**Option B: local (Windows, JDK 17+ with jpackage)**
```bash
mvn clean install -Prelease
# -> desktop/target/MiniTransportTycoon-0.1.0-windows-x64.zip
```
The runtime is copied from whichever JDK runs Maven. Prefer a Temurin/OpenJDK JDK for builds
you publish.

### Smoke test (do this on the exact zip you upload)
- [ ] Extract it to a new folder outside the repo (e.g. `Downloads`).
- [ ] Run `MiniTransportTycoon\MiniTransportTycoon.exe`: the menu appears.
- [ ] Press PLAY: the map, HUD, build palette and minimap render; a vehicle moves.
- [ ] Speed buttons read `|| 1x 2x 4x`; hovering a traffic light shows the Object Inspector
      rows above "Press [L] for details" with no overlap.
- [ ] Ideally test on a second PC without Java installed.

---

## 2. Create the itch.io page

1. Sign in at https://itch.io, then **Dashboard**, then **Create new project**.
2. Fill in:

| Field | Value |
|---|---|
| Title | Mini Transport Tycoon |
| Project URL | `https://<your-username>.itch.io/mini-transport-tycoon` |
| Short description / tagline | Build roads, run buses and trucks, and keep your transport company out of bankruptcy. |
| Classification | Games |
| Kind of project | Downloadable |
| Release status | In development |
| Pricing | No payments (free); or "$0 or donate" |
| Genre | Simulation |
| Tags | `tycoon`, `management`, `transport`, `isometric`, `city-builder`, `economy`, `pixel-art`, `singleplayer`, `libgdx`, `java` |
| Made with | libGDX |
| Inputs | Mouse, Keyboard |
| Average session | About half an hour |
| Languages | English |
| Visibility | **Draft** first; switch to Public after checking the page |

3. **Uploads**, then **Upload files**: choose `MiniTransportTycoon-0.1.0-windows-x64.zip`,
   tick **Windows** as the platform, and set the display name to `Windows (64-bit)`.
4. Paste the description below, add a cover image and screenshots, then **Save**.
5. Open the page in draft view, download the zip from it, and repeat the smoke test.
6. Set Visibility to **Public**.

**Optional: butler (command-line uploads, versioned updates)**
```bash
butler login
butler push desktop/target/MiniTransportTycoon-0.1.0-windows-x64.zip \
    <your-username>/mini-transport-tycoon:windows --userversion 0.1.0
```

---

## 3. Page description (paste into itch.io)

> **Build roads. Connect towns. Run buses and trucks. Don't go bankrupt.**
>
> Mini Transport Tycoon is a small isometric transport-management game inspired by the
> classic tycoon genre. Every map is randomly generated with hills, water, forests, towns and
> industry, and it's your job to turn it into a profitable transport network.
>
> **Features**
> - Random maps from 64×64 to 1024×1024 with terrain heights
> - Road building with automatic corners, junctions and slope rules
> - Terraforming: raise and lower land, place water and forests
> - Passenger buses and goods trucks, bought and sold at garages
> - Town-to-town demand and per-delivery income
> - Running costs: construction, terraforming and maintenance that rises as vehicles age
> - Traffic lights that vehicles obey, with timing you can tune live
> - Three difficulty levels, four game speeds, a clickable minimap and an object inspector
>
> **How to play**
> 1. Name your company, choose difficulty and map size, press **PLAY**.
> 2. Build roads between towns and connect a garage to them.
> 3. Press **1** (bus) or **2** (truck) and click the garage to buy a vehicle.
> 4. Earn money from deliveries, and keep enough cash for maintenance.
>
> **Controls:** Left click to place · Right click to remove/sell · WASD/arrows or wheel to pan ·
> Ctrl+wheel to zoom · L to inspect · Z/X/C/V to adjust traffic lights · Esc to pause
>
> **Install:** Extract the zip and run `MiniTransportTycoon.exe`. No Java needed.
> Windows may show a SmartScreen warning because the game isn't code-signed:
> click *More info* and then *Run anyway*.
>
> **About:** Built by Team 404 (Khayrullo Isomiddinov, Ulukbek Tursunov, Komron Asilov,
> Ramal Hasanli) for the Software Technology course at ELTE, using Java 17 and libGDX with a
> clean separation between a framework-free simulation core and the rendering layer.
> Source: https://github.com/khayrullo-isomiddinov/Transport-Tycoon
>
> *Early version: no save/load or sound yet.*

---

## 4. Screenshots and video to capture

itch.io recommends 3–5 screenshots at the game's native 1280×720 (16:9). A cover image must be
630×500 (crop from a screenshot).

- [ ] **Cover (630×500):** zoomed-in busy junction with traffic lights and vehicles.
- [ ] **Main menu:** company name, difficulty and map-size selection.
- [ ] **Network overview:** zoomed-out view of several towns joined by roads, minimap visible.
- [ ] **Vehicles at work:** buses/trucks on a route with cargo labels, HUD showing earnings.
- [ ] **Building:** road being dragged across terrain, or a terraformed hill.
- [ ] **Object inspector:** `L` details panel on a vehicle or garage.
- [ ] **Short GIF/video (15–30 s):** build a road, connect a garage, buy a bus, speed up to 4×,
      watch income come in. Record with ShareX (GIF) or OBS (MP4, upload to YouTube and embed).
      Keep the GIF under ~10 MB.

---

## 5. After publishing

- [ ] Add the itch.io link and a screenshot/GIF to the top of `README.md`.
- [ ] Put the itch.io link on your CV/portfolio with a one-line summary, and link the GitHub repo.
- [ ] For updates: bump `release.version` (or tag `vX.Y.Z`), rebuild, and upload or `butler push`
      to the same channel.
