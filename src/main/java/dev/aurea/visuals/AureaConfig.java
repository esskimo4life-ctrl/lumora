package dev.aurea.visuals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

public final class AureaConfig {
    private static final Map<AureaModule, Boolean> ENABLED = new EnumMap<>(AureaModule.class);
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aurea-visuals.properties");
    private static final int[] ACCENTS = {0xFF73A7FF, 0xFFB29AFF, 0xFF80D6B2, 0xFFFFA77E};
    private static int accentIndex = 0;
    private static boolean gammaCaptured = false;
    private static double originalGamma = 1.0;
    private static boolean bobCaptured = false;
    private static boolean originalBobView = true;
    private static boolean distanceCaptured = false;
    private static int originalRenderDistance = 12;

    private AureaConfig() {}

    public static void load() {
        ENABLED.clear();
        for (AureaModule module : AureaModule.values()) {
            ENABLED.put(module, module.defaultEnabled());
        }
        Properties properties = new Properties();
        try {
            if (Files.exists(FILE)) {
                try (InputStream input = Files.newInputStream(FILE)) {
                    properties.load(input);
                }
            }
        } catch (IOException exception) {
            AureaClient.LOGGER.warn("Не удалось прочитать конфигурацию Aurea Visuals", exception);
        }

        for (AureaModule module : AureaModule.values()) {
            String value = properties.getProperty("module." + module.id());
            if (value != null) {
                ENABLED.put(module, Boolean.parseBoolean(value));
            }
        }
        accentIndex = parseInt(properties.getProperty("accent"), 0);
        if (accentIndex < 0 || accentIndex >= ACCENTS.length) {
            accentIndex = 0;
        }

        gammaCaptured = properties.containsKey("original.gamma");
        originalGamma = parseDouble(properties.getProperty("original.gamma"), 1.0);
        bobCaptured = properties.containsKey("original.bob");
        originalBobView = Boolean.parseBoolean(properties.getProperty("original.bob", "true"));
        distanceCaptured = properties.containsKey("original.renderDistance");
        originalRenderDistance = parseInt(properties.getProperty("original.renderDistance"), 12);
    }

    public static boolean isEnabled(AureaModule module) {
        return ENABLED.getOrDefault(module, module.defaultEnabled());
    }

    public static int accentColor() {
        return ACCENTS[accentIndex];
    }

    public static void cycleAccent() {
        accentIndex = (accentIndex + 1) % ACCENTS.length;
        save();
    }

    public static void reset() {
        for (AureaModule module : AureaModule.values()) {
            ENABLED.put(module, module.defaultEnabled());
        }
        accentIndex = 0;
        gammaCaptured = false;
        bobCaptured = false;
        distanceCaptured = false;
        applyStartup(Minecraft.getInstance());
        save();
    }

    public static void setEnabled(AureaModule module, boolean enabled, Minecraft client) {
        boolean old = isEnabled(module);
        if (old == enabled) {
            return;
        }
        ENABLED.put(module, enabled);
        applyModule(module, enabled, client);
        save();
    }

    public static void applyStartup(Minecraft client) {
        for (AureaModule module : AureaModule.values()) {
            if (isEnabled(module)) {
                applyModule(module, true, client);
            }
        }
    }

    private static void applyModule(AureaModule module, boolean enabled, Minecraft client) {
        if (client == null || client.options == null) {
            return;
        }

        switch (module) {
            case FULLBRIGHT -> {
                if (enabled) {
                    if (!gammaCaptured) {
                        originalGamma = client.options.gamma().get();
                        gammaCaptured = true;
                    }
                    client.options.gamma().set(1.0);
                } else {
                    if (gammaCaptured) {
                        client.options.gamma().set(originalGamma);
                    }
                    gammaCaptured = false;
                }
            }
            case NO_BOBBING -> {
                if (enabled) {
                    if (!bobCaptured) {
                        originalBobView = client.options.bobView().get();
                        bobCaptured = true;
                    }
                    client.options.bobView().set(false);
                } else {
                    if (bobCaptured) {
                        client.options.bobView().set(originalBobView);
                    }
                    bobCaptured = false;
                }
            }
            case LOW_RENDER_DISTANCE -> {
                if (enabled) {
                    if (!distanceCaptured) {
                        originalRenderDistance = client.options.renderDistance().get();
                        distanceCaptured = true;
                    }
                    client.options.renderDistance().set(Math.min(originalRenderDistance, 6));
                } else {
                    if (distanceCaptured) {
                        client.options.renderDistance().set(originalRenderDistance);
                    }
                    distanceCaptured = false;
                }
            }
            default -> {
                // HUD-only modules do not mutate game options.
            }
        }
    }

    private static void save() {
        Properties properties = new Properties();
        for (AureaModule module : AureaModule.values()) {
            properties.setProperty("module." + module.id(), Boolean.toString(isEnabled(module)));
        }
        properties.setProperty("accent", Integer.toString(accentIndex));
        if (gammaCaptured) properties.setProperty("original.gamma", Double.toString(originalGamma));
        if (bobCaptured) properties.setProperty("original.bob", Boolean.toString(originalBobView));
        if (distanceCaptured) properties.setProperty("original.renderDistance", Integer.toString(originalRenderDistance));

        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "Aurea Visuals settings");
            }
        } catch (IOException exception) {
            AureaClient.LOGGER.warn("Не удалось сохранить конфигурацию Aurea Visuals", exception);
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return value == null ? fallback : Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
