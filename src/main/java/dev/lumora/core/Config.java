package dev.lumora.core;

import dev.lumora.Lumora;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/** Plain .properties persistence: config/lumora.properties */
public final class Config {
	private Config() {
	}

	private static Path file;

	public static void init() {
		file = FabricLoader.getInstance().getConfigDir().resolve("lumora.properties");
	}

	public static void load() {
		if (file == null || !Files.exists(file)) return;
		Properties p = new Properties();
		try (InputStream in = Files.newInputStream(file)) {
			p.load(in);
		} catch (IOException e) {
			return;
		}
		for (Module m : Lumora.MODULES) {
			String en = p.getProperty(m.id + ".enabled");
			if (en != null && m.togglable) m.enabled = Boolean.parseBoolean(en);
			for (Setting s : m.settings) {
				String v = p.getProperty(m.id + "." + s.key);
				if (v == null) continue;
				try {
					s.deserialize(v);
				} catch (RuntimeException ignored) {
				}
			}
			if (m instanceof HudModule h) {
				try {
					String x = p.getProperty(m.id + ".x");
					String y = p.getProperty(m.id + ".y");
					if (x != null) h.fx = Anim.clamp01(Float.parseFloat(x));
					if (y != null) h.fy = Anim.clamp01(Float.parseFloat(y));
				} catch (RuntimeException ignored) {
				}
			}
		}
	}

	public static void save() {
		if (file == null) return;
		Properties p = new Properties();
		for (Module m : Lumora.MODULES) {
			if (m.togglable) p.setProperty(m.id + ".enabled", Boolean.toString(m.enabled));
			for (Setting s : m.settings) {
				if (s instanceof Setting.Action) continue;
				p.setProperty(m.id + "." + s.key, s.serialize());
			}
			if (m instanceof HudModule h) {
				p.setProperty(m.id + ".x", Float.toString(h.fx));
				p.setProperty(m.id + ".y", Float.toString(h.fy));
			}
		}
		try (OutputStream out = Files.newOutputStream(file)) {
			p.store(out, "Lumora Visuals");
		} catch (IOException ignored) {
		}
	}
}
