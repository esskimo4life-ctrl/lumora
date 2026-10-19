package dev.lumora;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumora.core.*;
import dev.lumora.core.Module;
import dev.lumora.hud.Effects;
import dev.lumora.hud.Widgets;
import dev.lumora.ui.LumoraScreen;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class Lumora {
	private Lumora() {
	}

	public static final String ID = "lumora";
	public static final String VERSION = "1.0";

	/** UI order. Includes Theme.MODULE for config persistence. */
	public static final List<Module> MODULES = new ArrayList<>();
	/** Draw order (sorted by layer). */
	private static final List<Module> RENDER = new ArrayList<>();
	public static final Ctx ctx = new Ctx();

	public static KeyMapping OPEN, ZOOM, HIDE;
	public static boolean hidden;

	// ---- cheap cached stats -------------------------------------------------
	public static int fps;
	public static String fpsStr = "0";
	public static String pingStr = "0ms";
	public static String serverStr = "Singleplayer";
	public static String clockStr = "00:00";
	public static final float[] frameMs = new float[96];
	public static int frameIdx;
	public static double bps;
	private static double lastX, lastZ;
	private static boolean haveLast;
	private static float fpsTime;
	private static int fpsFrames;
	private static long lastNano;
	private static int tickCount;

	private static void add(Module m) {
		MODULES.add(m);
	}

	public static void init() {
		Config.init();

		KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(ID, "main"));
		OPEN = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lumora.open", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, cat));
		ZOOM = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lumora.zoom", InputConstants.Type.KEYSYM, InputConstants.KEY_C, cat));
		HIDE = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.lumora.hide", InputConstants.Type.KEYSYM, InputConstants.KEY_H, cat));

		// Visuals
		add(new Effects.Particles());
		add(new Effects.DamageVignette());
		add(new Effects.EdgeGlow());
		add(new Effects.HitMarker());
		add(new Effects.Brightness());
		add(new Effects.Zoom());
		add(new Effects.Notifications());
		// HUD
		add(new Widgets.InfoBar());
		add(new Widgets.Coordinates());
		add(new Widgets.Keystrokes());
		add(new Widgets.Potions());
		add(new Widgets.Armor());
		add(new Widgets.ItemCounter());
		add(new Widgets.TargetHud());
		add(new Widgets.Compass());
		add(new Widgets.FrameGraph());
		add(new Widgets.Hotkeys());
		// Theme (config only)
		add(Theme.MODULE);

		// Sensible first-run defaults
		for (Module m : MODULES) {
			String id = m.id;
			m.enabled = m.togglable && (id.equals("infobar") || id.equals("coords") || id.equals("keystrokes") || id.equals("potions")
				|| id.equals("armor") || id.equals("targethud") || id.equals("hitmarker") || id.equals("glow")
				|| id.equals("notifications") || id.equals("vignette") || id.equals("zoom") || id.equals("itemcounter"));
		}
		Config.load();

		for (Module m : MODULES) if (m.layer == 0) RENDER.add(m);
		for (Module m : MODULES) if (m.layer == 1) RENDER.add(m);
		for (Module m : MODULES) if (m.layer == 2) RENDER.add(m);

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(ID, "hud"), (graphics, delta) -> renderHud(graphics));

		ClientTickEvents.END_CLIENT_TICK.register(Lumora::tick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> {
			restoreEffects(mc);
			Config.save();
		});
	}

	private static void restoreEffects(Minecraft mc) {
		for (Module m : MODULES) {
			if (m instanceof Effects.Brightness b) b.restore(mc);
			if (m instanceof Effects.Zoom z) z.restore(mc);
		}
	}

	private static void tick(Minecraft mc) {
		while (OPEN.consumeClick()) {
			if (mc.screen == null && mc.player != null) mc.setScreen(new LumoraScreen());
		}
		while (HIDE.consumeClick()) {
			hidden = !hidden;
			Notifs.push("Lumora HUD", hidden ? "Hidden" : "Visible", !hidden);
		}
		if (mc.player != null) {
			double x = mc.player.getX(), z = mc.player.getZ();
			if (haveLast) {
				double dx = x - lastX, dz = z - lastZ;
				double v = Math.sqrt(dx * dx + dz * dz) * 20.0;
				bps += (v - bps) * 0.4;
			}
			lastX = x;
			lastZ = z;
			haveLast = true;
		} else {
			haveLast = false;
			bps = 0;
		}
		if (tickCount++ % 10 == 0) refreshSlowStats(mc);
		for (Module m : MODULES) m.tick(mc);
	}

	private static void refreshSlowStats(Minecraft mc) {
		int ping = 0;
		if (mc.player != null && mc.getConnection() != null) {
			var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			if (info != null) ping = info.getLatency();
		}
		pingStr = ping + "ms";
		var sd = mc.getCurrentServer();
		serverStr = sd == null ? "Singleplayer" : sd.ip;
		LocalTime t = LocalTime.now();
		int h = t.getHour(), m = t.getMinute();
		clockStr = (h < 10 ? "0" : "") + h + ":" + (m < 10 ? "0" : "") + m;
	}

	private static void renderHud(GuiGraphicsExtractor g) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;

		long nano = System.nanoTime();
		float dt = lastNano == 0 ? 0.016f : (nano - lastNano) / 1.0e9f;
		lastNano = nano;
		dt = Math.min(dt, 0.1f);
		frameMs[frameIdx] = dt * 1000f;
		frameIdx = (frameIdx + 1) % frameMs.length;
		fpsTime += dt;
		fpsFrames++;
		if (fpsTime >= 0.25f) {
			fps = Math.round(fpsFrames / fpsTime);
			fpsStr = Integer.toString(fps);
			fpsTime = 0;
			fpsFrames = 0;
		}

		Ctx c = ctx;
		c.mc = mc;
		c.font = mc.font;
		c.p = mc.player;
		c.sw = mc.getWindow().getGuiScaledWidth();
		c.sh = mc.getWindow().getGuiScaledHeight();
		c.dt = dt;
		c.now = System.currentTimeMillis();
		c.editing = mc.screen instanceof LumoraScreen;
		Theme.update(c.now);

		if (mc.options.hideGui && !c.editing) return;
		if (hidden && !c.editing) return;

		Gfx.ga = 1f;
		for (Module m : RENDER) {
			if (m.enabled) m.render(g, c);
		}
	}
}
