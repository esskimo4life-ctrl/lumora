package dev.lumora.core;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;

/** Per-frame shared state, reused to avoid allocations. */
public final class Ctx {
	public Minecraft mc;
	public Font font;
	public LocalPlayer p;
	public int sw, sh;
	public float dt;
	public long now;
	public boolean editing;
}
