# AstraBoost 1.0.0

A client-side performance-profile mod for Minecraft Java 26.1.2 + Fabric, aimed at Android Java launchers such as Fold Craft Launcher. It does not bundle Minecraft, Java, Fabric Loader, Sodium, MobileGlues or Unity Culling.

## What's included

- F8 opens an in-game menu. The key can be rebound under Options → Controls → Key Binds → AstraBoost.
- Maximum FPS: render distance 6, simulation distance 4, entity distance 50%, minimal particles, fast graphics, clouds off, entity shadows off, VSync off, FPS target 260, mipmaps off and biome blend radius 0.
- Competitive: a slightly higher render distance and entity distance, with expensive effects disabled.
- Balanced: longer render distance and a 165 FPS target.
- Automatic profile application on startup, configurable in the menu.
- Reflection-based application skips settings which are absent or renamed instead of intentionally crashing.
- Config file: config/astraboost.properties.
- GitHub Actions builds the distributable JAR with Java 25 and Gradle 9.5.1.

## Install in Fold Craft Launcher

1. Start the exact Minecraft 26.1.2 / Fabric profile once so the instance folder exists.
2. Ensure that profile uses Java 25 and a Fabric Loader version compatible with Minecraft 26.1.2.
3. Put astraboost-1.0.0.jar in that profile's mods folder. Do not put the downloaded artifact ZIP there.
4. Keep only versions of Sodium, MobileGlues and Unity Culling that specifically support that Minecraft version and your rendering path. AstraBoost does not replace those projects or guarantee their compatibility with each other.
5. Start the game and press F8 to open AstraBoost. If F8 is intercepted, rebind it in Controls.

## Build and download

GitHub Actions builds the JAR automatically after a push. Open the repository's Actions tab, select Build AstraBoost and download the artifact AstraBoost-Minecraft-26.1.2 from the completed run. The JAR is inside the downloaded artifact ZIP.

To build locally, use JDK 25 and Gradle 9.5.1, then run gradle build --no-daemon. The distributable JAR appears in build/libs.

## Optional settings

After the first run, edit config/astraboost.properties in the selected instance folder. Available profile names: ULTRA_FPS, COMPETITIVE, BALANCED.

Optional numeric overrides (leave blank to use the profile default):
- override_render_distance=6
- override_simulation_distance=4
- override_entity_distance=0.5
- override_max_fps=260
- override_mipmap_levels=0
- override_biome_blend_radius=0

Use values your device can sustain. If performance gets worse after extended play, lower render distance and FPS cap to reduce heat and thermal throttling.

## Limits

A mod cannot guarantee a fixed FPS increase. If the bottleneck is MobileGlues/OpenGL, GPU fill-rate, CPU-side chunk generation, heat or launcher behavior, option tuning has a ceiling. This version tunes recognized vanilla client options; it does not patch Sodium, MobileGlues, Unity Culling, Java runtime flags or Android power/thermal policy. Benchmark the same world and route after warm-up, not only in an empty void.
