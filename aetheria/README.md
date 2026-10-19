# Aetheria Visuals — Minecraft Fabric 26.1.2

A clean, client-side visual suite with a responsive settings screen. This project is written independently; it does not depend on Lumora's rendering or module code.

## Included
- A custom responsive configuration screen with Visuals, HUD, Utility and Style tabs with a compact two-column toggle layout inspired by the clean, restrained Pulse Visuals interface.
- FPS/ping/session bar, coordinates + movement speed, armor/held-item status, active effect timers, target card, compass and frame-time graph. Keystrokes are intentionally excluded.
- Ambient particles, hit feedback, accent reticle, low-health tint, optional full brightness, smooth hold-to-zoom and a low-effects mode.
- Adjustable accent palette, particle amount, panel opacity, corner radius and zoom FOV.
- Persistent settings in the config/aetheria.properties file; no external runtime libraries beyond Fabric API.
- HUD effects are rendered through Minecraft's GUI extraction path rather than raw OpenGL.

## Install
1. Use Minecraft 26.1.2, Fabric Loader and Fabric API for 26.1.2, with Java 25.
2. Put Aetheria-Visuals-26.1.2.jar in the instance's mods folder.
3. Start the Fabric client. Press Right Shift to open the menu. Both menu and zoom bindings can be changed in Controls.
4. Settings are saved automatically to config/aetheria.properties.

## PojavLauncher / Android
This is a Minecraft Java client mod, not a Bedrock/mobile mod. It can work in a PojavLauncher instance when that instance successfully runs the same Minecraft 26.1.2 + Fabric Loader + Fabric API + Java 25 combination. Rendering performance depends on the device, GPU driver and renderer. On phones, start with Ambient Particles disabled or use Low-effects mode, and bind the menu key to an accessible key in Controls.

## Build
Requires JDK 25 and Gradle 9.5.1. Run gradle build --no-daemon from this folder.
Output: build/libs/aetheria-visuals-1.0.0.jar
