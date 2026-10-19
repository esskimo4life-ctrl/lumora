package dev.lumora.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public abstract class Module {
	public final String id, name, desc;
	public final Category cat;
	public boolean enabled;
	public boolean togglable = true;
	/** 0 = background overlays, 1 = widgets, 2 = top-most */
	public int layer = 1;
	public float anim, hover;
	public final List<Setting> settings = new ArrayList<>();

	protected Module(String id, String name, String desc, Category cat) {
		this.id = id;
		this.name = name;
		this.desc = desc;
		this.cat = cat;
	}

	public Setting.Bool bool(String key, String label, boolean def) {
		Setting.Bool s = new Setting.Bool(key, label, def);
		settings.add(s);
		return s;
	}

	public Setting.Slider slider(String key, String label, double min, double max, double step, double def) {
		return slider(key, label, min, max, step, def, "");
	}

	public Setting.Slider slider(String key, String label, double min, double max, double step, double def, String suffix) {
		Setting.Slider s = new Setting.Slider(key, label, min, max, step, def, suffix);
		settings.add(s);
		return s;
	}

	public Setting.Mode mode(String key, String label, int def, String... options) {
		Setting.Mode s = new Setting.Mode(key, label, def, options);
		settings.add(s);
		return s;
	}

	public Setting.Action action(String key, String label, String button, Runnable run) {
		Setting.Action s = new Setting.Action(key, label, button, run);
		settings.add(s);
		return s;
	}

	/** Called every client tick for every module (even when disabled) so effects can clean up. */
	public void tick(Minecraft mc) {
	}

	public void render(GuiGraphicsExtractor g, Ctx c) {
	}

	public void toggleByUser() {
		if (!togglable) return;
		enabled = !enabled;
		Notifs.push(name, enabled ? "Enabled" : "Disabled", enabled);
	}
}
