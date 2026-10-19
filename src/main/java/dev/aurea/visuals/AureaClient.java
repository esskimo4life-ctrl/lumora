package dev.aurea.visuals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.lwjgl.glfw.GLFW;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import dev.aurea.visuals.gui.AureaScreen;
import dev.aurea.visuals.hud.AureaHud;

public final class AureaClient implements ClientModInitializer {
    public static final String MOD_ID = "aurea-visuals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Aurea Visuals");
    private static KeyMapping openMenuKey;
    private static long sessionStartedAt = System.currentTimeMillis();
    private static boolean wasInWorld = false;

    @Override
    public void onInitializeClient() {
        AureaConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.aurea-visuals.open_menu",
                com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && !wasInWorld) {
                sessionStartedAt = System.currentTimeMillis();
                wasInWorld = true;
            } else if (client.player == null) {
                wasInWorld = false;
            }

            while (openMenuKey.consumeClick()) {
                if (client.screen instanceof AureaScreen) {
                    client.setScreen(null);
                } else if (client.screen == null) {
                    client.setScreen(new AureaScreen());
                }
            }
        });

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                AureaHud::render
        );

        AureaConfig.applyStartup(Minecraft.getInstance());
        LOGGER.info("Aurea Visuals загружен. Меню настраивается в Управление → Клавиши.");
    }

    public static long sessionStartedAt() {
        return sessionStartedAt;
    }
}
