# Aurea Visuals 1.0.0

A client-side visual mod for Minecraft Java Edition 26.1.2 on Fabric. The UI is a restrained graphite interface with slate surfaces and configurable accents; it does not add keystrokes.

## Included

- A custom categorized menu with Russian module descriptions and persisted settings.
- FPS, coordinates, direction, world time, held-item durability, armor durability, target health, movement speed, active effects, session timer, biome and server ping HUD entries.
- Optional FPS history graph and an accent crosshair frame.
- Maximum vanilla gamma, camera bobbing toggle and a temporary low-render-distance mode.
- Menu key registered in Minecraft Controls so it can be rebound there.
- Lightweight rendering: HUD is drawn from a single Fabric HUD element; the FPS history samples four times per second.

## Build

Use Java 25 and Gradle 9.4.0, then run gradle clean build. The build output is in build/libs/.

The GitHub Actions workflow also builds the mod and packages the JAR together with this readme as Aurea-Visuals-26.1.2.zip.

## Install

1. Use a Minecraft 26.1.2 instance with Fabric Loader 0.19.5 or a compatible newer loader, plus Fabric API 0.155.0+26.1.2.
2. Put the Aurea Visuals JAR inside that instance's mods folder.
3. Start the game and press Right Shift. The menu binding can be changed under Options → Controls → Key Binds → Aurea Visuals.
4. The options are saved in config/aurea-visuals.properties.

## Mobile launchers

The mod has no native libraries and is built for Java 25, so it is architecture-neutral at the code level. Mobile compatibility still depends on the launcher/device actually supporting Minecraft 26.1.2, Fabric Loader 0.19.5+, Java 25 and Fabric API. This cannot be guaranteed for every Pojav-based launcher or device.

## Notes

Aurea Visuals is a client-side presentation mod. It does not change server rules or send gameplay packets. The low render-distance and camera-bobbing modules temporarily change local video options and restore the previous values when toggled off.
