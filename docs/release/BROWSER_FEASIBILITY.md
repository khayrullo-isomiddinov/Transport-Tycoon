# Browser build (TeaVM / gdx-teavm): feasibility

Assessed on 2026-10-25 against the code at this point. No code has been ported yet.

## Verdict

**Feasible, but not now.** Ship the Windows build first. Then run a **time-boxed proof of
concept (half a day)**. If the PoC shows the menu and a generated map in the browser, the
full port is roughly 1–3 days.

## Compatibility

| Area | Status |
|---|---|
| libGDX version | Project uses 1.14.0; gdx-teavm 0.16.0 targets 1.14.2 (bump to 1.14.2 for all modules; patch release) |
| Java version | Java 17 bytecode; supported by TeaVM 1.6.x |
| Build system | gdx-teavm artifacts are on Maven Central, and its `TeaBuilder` API is plain Java, so a `web` **Maven** module can run it via `exec-maven-plugin`. **No Gradle migration needed.** |
| `core` module | Pure Java (collections, `Random`, `Optional`); no threads, reflection or I/O. Low risk. |
| Desktop-only code | Only `DesktopLauncher` imports LWJGL3. All game/view classes use the portable `Gdx.*` API. |
| Threads / reflection | None used. |

## Concrete blockers and risks

1. **Module wiring.** The game classes live in `desktop`, which depends on `gdx-backend-lwjgl3`.
   A `web` module would depend on `desktop` with the LWJGL/natives dependencies excluded, and add
   a `TeaVMLauncher` + builder class. No restructuring of existing modules.
2. **Asset loading** (`ResourceFileHandles`). Uses `java.io.File` and `System.getProperty("user.dir")`
   for the IDE fallback. On web this must be skipped (`Gdx.app.getType() == WebGL`), leaving
   `Gdx.files.internal` + `asset-manifest.txt`, which is now correct and test-enforced.
   About 5 lines of change.
3. **`Pixmap` from file** (`TextureWhiteToTransparent`, `DecorationMetadata`): white-to-alpha keying
   reads pixels on the CPU. gdx-teavm supports Pixmap, but this is the most likely place for a
   surprise. **Verify first in the PoC.**
4. **`String.format("%,d")`** in HUD/menu: TeaVM's classlib supports `String.format`. Verify
   grouping output; otherwise a tiny helper.
5. **Browser input.** Ctrl+scroll zooms the *page* and Esc/arrow keys can be captured by the
   browser. gdx-teavm can prevent default handling; needs checking.
6. **Performance.** 512×512 and 1024×1024 maps may be too slow in JS/WebGL. Likely cap web map
   sizes at 256×256.
7. **Unverified:** whether `Gdx.files.internal("resources").list()` works with the web preloader.
   Not required, because the manifest fallback covers it.

## Smallest path to a playable browser build

1. Bump `libgdx.version` to 1.14.2; confirm the desktop build still passes.
2. Add `web/` Maven module: depends on `desktop` (excluding LWJGL3 backend + natives), adds
   `gdx-teavm` backend, `TeaVMLauncher` (starts `TransportTycoonGame`), and a builder `main`
   that copies `resources/` as assets and emits `web/target/webapp/`.
3. Guard the `java.io.File` fallback in `ResourceFileHandles` for WebGL.
4. Build, serve `web/target/webapp` locally, fix what breaks (expect Pixmap / input issues).
5. Zip the webapp folder and upload to the same itch.io project as an HTML5 build
   ("This file will be played in the browser"), viewport 1280×720.

## Go / no-go

- **Now:** no-go. It would delay the desktop release, and the desktop build already gives
  recruiters a working download.
- **Next:** go for a half-day PoC (steps 1–4). Abandon if Pixmap/asset loading needs deep changes.
  A browser-playable link greatly raises the chance a recruiter actually plays it, so the
  payoff justifies a small, bounded experiment.
