package dev.lumora;

import net.fabricmc.api.ClientModInitializer;

public final class LumoraClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Lumora.init();
	}
}
