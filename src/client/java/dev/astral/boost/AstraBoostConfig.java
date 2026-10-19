package dev.astral.boost;

import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public final class AstraBoostConfig {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("astraboost.properties");
    private static final Properties PROPERTIES = new Properties();
    private static Profile profile = Profile.ULTRA_FPS;
    private static boolean autoApply = true;
    private static boolean showChatNotice = true;
    private AstraBoostConfig() {}

    public enum Profile {
        ULTRA_FPS("Максимальный FPS"), COMPETITIVE("Соревновательный"), BALANCED("Баланс");
        private final String displayName;
        Profile(String displayName) { this.displayName = displayName; }
        public String displayName() { return displayName; }
    }

    public static synchronized void load() {
        PROPERTIES.clear();
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE)) try (Reader reader = Files.newBufferedReader(FILE)) { PROPERTIES.load(reader); }
        } catch (IOException e) {
            System.err.println("[AstraBoost] Could not load config: " + e.getMessage());
        }
        profile = parseProfile(PROPERTIES.getProperty("profile", "ULTRA_FPS"));
        autoApply = Boolean.parseBoolean(PROPERTIES.getProperty("auto_apply", "true"));
        showChatNotice = Boolean.parseBoolean(PROPERTIES.getProperty("show_chat_notice", "true"));
        save();
    }

    public static synchronized void selectProfile(Profile selected) {
        profile = selected == null ? Profile.ULTRA_FPS : selected;
        PROPERTIES.setProperty("profile", profile.name());
        save();
    }
    public static synchronized Profile profile() { return profile; }
    public static synchronized boolean autoApply() { return autoApply; }
    public static synchronized boolean showChatNotice() { return showChatNotice; }
    public static synchronized void toggleAutoApply() {
        autoApply = !autoApply;
        PROPERTIES.setProperty("auto_apply", Boolean.toString(autoApply));
        save();
    }

    public static synchronized Map<String, Object> settingsForActiveProfile() {
        Map<String, Object> values = profileDefaults(profile);
        putOverride(values, "render_distance", "renderDistance", Integer::parseInt);
        putOverride(values, "simulation_distance", "simulationDistance", Integer::parseInt);
        putOverride(values, "entity_distance", "entityDistanceScaling", Double::parseDouble);
        putOverride(values, "max_fps", "maxFps", Integer::parseInt);
        putOverride(values, "mipmap_levels", "mipmapLevels", Integer::parseInt);
        putOverride(values, "biome_blend_radius", "biomeBlendRadius", Integer::parseInt);
        return values;
    }

    private static Map<String, Object> profileDefaults(Profile profile) {
        Map<String, Object> v = new LinkedHashMap<>();
        switch (profile) {
            case ULTRA_FPS -> {
                v.put("renderDistance", 6); v.put("simulationDistance", 4); v.put("entityDistanceScaling", 0.50D);
                v.put("particles", "MINIMAL"); v.put("graphicsMode", "FAST"); v.put("clouds", "OFF");
                v.put("entityShadows", false); v.put("enableVsync", false); v.put("maxFps", 260);
                v.put("mipmapLevels", 0); v.put("biomeBlendRadius", 0); v.put("bobView", false); v.put("ambientOcclusion", false);
            }
            case COMPETITIVE -> {
                v.put("renderDistance", 8); v.put("simulationDistance", 5); v.put("entityDistanceScaling", 0.65D);
                v.put("particles", "MINIMAL"); v.put("graphicsMode", "FAST"); v.put("clouds", "OFF");
                v.put("entityShadows", false); v.put("enableVsync", false); v.put("maxFps", 260);
                v.put("mipmapLevels", 1); v.put("biomeBlendRadius", 0); v.put("bobView", false); v.put("ambientOcclusion", false);
            }
            case BALANCED -> {
                v.put("renderDistance", 10); v.put("simulationDistance", 6); v.put("entityDistanceScaling", 0.80D);
                v.put("particles", "DECREASED"); v.put("graphicsMode", "FAST"); v.put("clouds", "FAST");
                v.put("entityShadows", false); v.put("enableVsync", false); v.put("maxFps", 165);
                v.put("mipmapLevels", 2); v.put("biomeBlendRadius", 1); v.put("bobView", true); v.put("ambientOcclusion", false);
            }
        }
        return v;
    }

    private interface Parser<T> { T parse(String value); }
    private static <T> void putOverride(Map<String, Object> values, String property, String option, Parser<T> parser) {
        String text = PROPERTIES.getProperty("override_" + property);
        if (text == null || text.isBlank()) return;
        try { values.put(option, parser.parse(text.trim())); }
        catch (RuntimeException ignored) { System.err.println("[AstraBoost] Ignoring invalid override_" + property + " value: " + text); }
    }
    private static Profile parseProfile(String name) {
        try { return Profile.valueOf(name.trim().toUpperCase(Locale.ROOT)); }
        catch (RuntimeException ignored) { return Profile.ULTRA_FPS; }
    }

    public static synchronized void save() {
        PROPERTIES.setProperty("profile", profile.name());
        PROPERTIES.setProperty("auto_apply", Boolean.toString(autoApply));
        PROPERTIES.setProperty("show_chat_notice", Boolean.toString(showChatNotice));
        PROPERTIES.putIfAbsent("override_render_distance", "");
        PROPERTIES.putIfAbsent("override_simulation_distance", "");
        PROPERTIES.putIfAbsent("override_entity_distance", "");
        PROPERTIES.putIfAbsent("override_max_fps", "");
        PROPERTIES.putIfAbsent("override_mipmap_levels", "");
        PROPERTIES.putIfAbsent("override_biome_blend_radius", "");
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) { PROPERTIES.store(writer, "AstraBoost client optimization settings"); }
        } catch (IOException e) { System.err.println("[AstraBoost] Could not save config: " + e.getMessage()); }
    }
}
