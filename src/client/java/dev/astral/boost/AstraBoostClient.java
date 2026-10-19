package dev.astral.boost;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AstraBoostClient implements ClientModInitializer {
    private static KeyMapping openMenuKey;
    private static boolean initialProfileApplied;

    @Override
    public void onInitializeClient() {
        AstraBoostConfig.load();
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("astraboost", "controls")
        );
        openMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.astraboost.open_menu", InputConstants.Type.KEYSYM,
                InputConstants.KEY_F8, category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!initialProfileApplied) {
                initialProfileApplied = true;
                if (AstraBoostConfig.autoApply()) {
                    PerformanceTuner.ApplyResult result = PerformanceTuner.apply(
                            client, AstraBoostConfig.settingsForActiveProfile()
                    );
                    System.out.println("[AstraBoost] " + result.summary());
}
            }
            while (openMenuKey.consumeClick()) client.setScreen(new AstraBoostScreen());
        });
    }
}
