package dev.lumora.hud;

import dev.lumora.core.*;
import dev.lumora.core.Module;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Visual effects: overlays, hit marker, brightness, zoom, toasts. */
public final class Effects {
	private Effects() {
	}

	// =====================================================================
	public static final class Particles extends Module {
		private static final int MAX = 160;
		private final Setting.Mode style = mode("style", "Style", 0, "Snow", "Stars", "Embers", "Sakura");
		private final Setting.Slider count = slider("count", "Amount", 10, MAX, 5, 60);
		private final Setting.Slider speed = slider("speed", "Speed", 0.3, 2.5, 0.1, 1.0, "x");
		private final float[] px = new float[MAX], py = new float[MAX], vx = new float[MAX], vy = new float[MAX], ph = new float[MAX];
		private final int[] sz = new int[MAX];
		private final Random rnd = new Random();
		private boolean ready;
		private int lastStyle = -1, lastW, lastH;

		public Particles() {
			super("particles", "Screen Particles", "Snow, stars, embers or petals drifting over the screen", Category.VISUALS);
			layer = 0;
		}

		private void spawn(int i, int sw, int sh, boolean anywhere) {
			int s = style.index;
			px[i] = rnd.nextFloat() * sw;
			ph[i] = rnd.nextFloat() * 6.283f;
			switch (s) {
				case 0 -> { // snow
					py[i] = anywhere ? rnd.nextFloat() * sh : -4;
					vx[i] = -6 + rnd.nextFloat() * 12;
					vy[i] = 22 + rnd.nextFloat() * 34;
					sz[i] = rnd.nextInt(4) == 0 ? 2 : 1;
				}
				case 1 -> { // stars
					py[i] = rnd.nextFloat() * sh;
					vx[i] = -2 + rnd.nextFloat() * 4;
					vy[i] = -2 - rnd.nextFloat() * 5;
					sz[i] = rnd.nextInt(5) == 0 ? 3 : 1;
				}
				case 2 -> { // embers
					py[i] = anywhere ? rnd.nextFloat() * sh : sh + 4;
					vx[i] = -8 + rnd.nextFloat() * 16;
					vy[i] = -(18 + rnd.nextFloat() * 38);
					sz[i] = rnd.nextInt(3) == 0 ? 2 : 1;
				}
				default -> { // sakura
					py[i] = anywhere ? rnd.nextFloat() * sh : -4;
					vx[i] = 10 + rnd.nextFloat() * 22;
					vy[i] = 16 + rnd.nextFloat() * 22;
					sz[i] = 2;
				}
			}
		}

		@Override
		public void render(GuiGraphicsExtractor g, Ctx c) {
			if (!ready || lastStyle != style.index || lastW != c.sw || lastH != c.sh) {
				for (int i = 0; i < MAX; i++) spawn(i, c.sw, c.sh, true);
				ready = true;
				lastStyle = style.index;
				lastW = c.sw;
				lastH = c.sh;
			}
			int n = count.i();
			float k = speed.f();
			float t = (c.now % 100000L) / 1000f;
			for (int i = 0; i < n; i++) {
				float sway = (float) Math.sin(t * 1.4f + ph[i]);
				px[i] += (vx[i] * k + sway * 8f) * c.dt;
				py[i] += vy[i] * k * c.dt;
				if (py[i] > c.sh + 6 || py[i] < -8 || px[i] > c.sw + 6 || px[i] < -6) {
					if (style.index == 1) {
						px[i] = rnd.nextFloat() * c.sw;
						py[i] = c.sh + 2;
					} else {
						spawn(i, c.sw, c.sh, false);
					}
				}
				int x = (int) px[i], y = (int) py[i], s = sz[i];
				switch (style.index) {
					case 0 -> Gfx.fill(g, x, y, x + s, y + s, Gfx.rgba(0xFFFFFF, 140 + (int) (90 * sway)));
					case 1 -> {
						float tw = 0.5f + 0.5f * (float) Math.sin(t * 2.2f + ph[i] * 3f);
						int col = (i & 1) == 0 ? Theme.accent2 : 0xFFFFFFFF;
						int a = (int) (60 + 190 * tw);
						Gfx.fill(g, x, y, x + s, y + s, Gfx.rgba(col, a));
						if (s == 3) {
							Gfx.fill(g, x + 1, y - 1, x + 2, y, Gfx.rgba(col, a / 2));
							Gfx.fill(g, x + 1, y + 3, x + 2, y + 4, Gfx.rgba(col, a / 2));
						}
					}
					case 2 -> {
						float fl = 0.6f + 0.4f * (float) Math.sin(t * 9f + ph[i] * 5f);
						int col = (i % 3 == 0) ? 0xFFD54A : 0xFF7A1C;
						Gfx.fill(g, x, y, x + s, y + s, Gfx.rgba(col, (int) (230 * fl)));
					}
					default -> {
						int col = (i & 1) == 0 ? 0xFFB7D5 : 0xFF8FBF;
						Gfx.fill(g, x, y, x + 2, y + 1, Gfx.rgba(col, 220));
						Gfx.fill(g, x + 1, y + 1, x + 3, y + 2, Gfx.rgba(col, 180));
					}
				}
			}
		}
	}

	// =====================================================================
	public static final class DamageVignette extends Module {
		private final Setting.Slider thr = slider("thr", "Low health threshold", 4, 20, 1, 8, " HP");
		private final Setting.Slider intensity = slider("int", "Intensity", 0.2, 1.0, 0.05, 0.7);
		private final Setting.Bool hurt = bool("hurt", "Flash when hit", true);
		private final Setting.Bool pulse = bool("pulse", "Heartbeat pulse", true);
		private float v;

		public DamageVignette() {
			super("vignette", "Damage Vignette", "Red screen edges on low health and when you take damage", Category.VISUALS);
			layer = 0;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Ctx c) {
			float hp = c.p.getHealth();
			float t = thr.f();
			float low = hp < t ? (t - hp) / t : 0f;
			if (pulse.value) low *= 0.7f + 0.3f * (float) Math.sin((c.now % 100000L) / 1000f * 6f);
			float h = hurt.value ? Math.min(1f, c.p.hurtTime / 10f) * 0.9f : 0f;
			float target = Math.max(low, h) * intensity.f();
			v = Anim.approachRaw(v, target, 12f, c.dt);
			if (v > 0.01f) Gfx.edge(g, c.sw, c.sh, 0xFF1030, Math.min(0.9f, v), Math.max(24, c.sh / 5));
		}
	}

	// =====================================================================
	public static final class EdgeGlow extends Module {
		private final Setting.Slider strength = slider("strength", "Strength", 0.1, 1.0, 0.05, 0.4);
		private final Setting.Slider width = slider("width", "Width", 20, 120, 5, 56, "px");
		private final Setting.Bool breathe = bool("breathe", "Breathing", true);

		public EdgeGlow() {
			super("glow", "Accent Glow", "Soft glow in your accent color around the screen", Category.VISUALS);
			layer = 0;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Ctx c) {
			float a = strength.f() * 0.5f;
			if (breathe.value) a *= 0.65f + 0.35f * (float) Math.sin((c.now % 100000L) / 1000f * 1.6f);
			Gfx.edge(g, c.sw, c.sh, Theme.accent, a, width.i());
		}
	}

	// =====================================================================
	public static final class HitMarker extends Module {
		private final Setting.Mode style = mode("style", "Style", 0, "Cross", "Plus", "Burst");
		private final Setting.Mode color = mode("color", "Color", 0, "Accent", "White", "Red");
		private final Setting.Slider size = slider("size", "Size", 4, 14, 1, 7, "px");
		private final Setting.Slider time = slider("time", "Duration", 0.15, 0.8, 0.05, 0.35, "s");
		private final Setting.Bool combo = bool("combo", "Combo counter", true);
		private int lastId = -1, lastHt, comboN;
		private float age = 99f;
		private long lastHit;

		public HitMarker() {
			super("hitmarker", "Hit Marker", "Animated marker and combo counter when you hit something", Category.VISUALS);
			layer = 2;
		}

		@Override
		public void render(GuiGraphicsExtractor g, Ctx c) {
			if (!c.editing) {
				Entity e = c.mc.crosshairPickEntity;
				if (e instanceof LivingEntity le && le != c.p) {
					int ht = le.hurtTime;
					if (le.getId() == lastId && ht > lastHt) {
						age = 0f;
						if (c.now - lastHit > 3000) comboN = 0;
						comboN++;
						lastHit = c.now;
					}
					lastId = le.getId();
					lastHt = ht;
				} else {
					lastId = -1;
					lastHt = 0;
				}
				if (c.p.hurtTime > 0) comboN = 0;
			}
			age += c.dt;
			Font f = c.font;
			int cx = c.sw / 2, cy = c.sh / 2;
			float dur = time.f();
			boolean preview = c.editing;
			float t = preview ? 0.25f : age / dur;
			if (t < 1f) {
				float a = 1f - t;
				int col = switch (color.index) {
					case 1 -> 0xFFFFFFFF;
					case 2 -> 0xFFFF4D5E;
					default -> Theme.accent;
				};
				int sz = size.i();
				int gap = Math.round(sz * 0.45f + t * sz * 0.7f);
				int len = Math.max(2, Math.round(sz * (0.75f - 0.3f * t)));
				int cc = Gfx.withAlpha(col, a);
				for (int k = 0; k < len; k++) {
					int d = gap + k;
					if (style.index == 1) {
						Gfx.fill(g, cx + d, cy, cx + d + 1, cy + 1, cc);
						Gfx.fill(g, cx - d - 1, cy, cx - d, cy + 1, cc);
						Gfx.fill(g, cx, cy + d, cx + 1, cy + d + 1, cc);
						Gfx.fill(g, cx, cy - d - 1, cx + 1, cy - d, cc);
					} else {
						Gfx.fill(g, cx + d, cy + d, cx + d + 1, cy + d + 1, cc);
						Gfx.fill(g, cx - d - 1, cy + d, cx - d, cy + d + 1, cc);
						Gfx.fill(g, cx + d, cy - d - 1, cx + d + 1, cy - d, cc);
						Gfx.fill(g, cx - d - 1, cy - d - 1, cx - d, cy - d, cc);
					}
				}
				if (style.index == 2) {
					int r = gap + len + 3;
					int dc = Gfx.withAlpha(0xFFFFFFFF, a * 0.8f);
					Gfx.fill(g, cx + r, cy, cx + r + 1, cy + 1, dc);
					Gfx.fill(g, cx - r - 1, cy, cx - r, cy + 1, dc);
					Gfx.fill(g, cx, cy + r, cx + 1, cy + r + 1, dc);
					Gfx.fill(g, cx, cy - r - 1, cx + 1, cy - r, dc);
				}
			}
			if (combo.value && (preview || (comboN > 1 && c.now - lastHit < 3000))) {
				int n = preview ? 3 : comboN;
				float fade = preview ? 1f : Anim.clamp01((3000 - (c.now - lastHit)) / 600f);
				float pop = Anim.clamp01(1f - (c.now - lastHit) / 180f);
				String s = "x" + n + " Combo";
				int y = cy + size.i() + 18 - Math.round(pop * 2);
				Gfx.textC(g, f, s, cx, y, Gfx.withAlpha(Gfx.lerp(Theme.accent2, 0xFFFFFFFF, pop), fade));
			}
		}
	}

	// =====================================================================
	public static final class Brightness extends Module {
		private final Setting.Slider level = slider("level", "Level", 0.5, 1.0, 0.05, 1.0);
		private Double orig;

		public Brightness() {
			super("brightness", "Full Bright", "Pushes the game brightness to the level you choose", Category.VISUALS);
		}

		public void restore(Minecraft mc) {
			if (orig != null) {
				mc.options.gamma().set(orig);
				orig = null;
			}
		}

		@Override
		public void tick(Minecraft mc) {
			if (mc.options == null) return;
			if (enabled) {
				if (orig == null) orig = mc.options.gamma().get();
				double t = level.value;
				if (Math.abs(mc.options.gamma().get() - t) > 1.0e-3) mc.options.gamma().set(t);
			} else {
				restore(mc);
			}
		}
	}

	// =====================================================================
	public static final class Zoom extends Module {
		private final Setting.Slider fov = slider("fov", "Zoom FOV", 30, 80, 1, 30, "\u00B0");
		private Integer orig;
		private float cur;

		public Zoom() {
			super("zoom", "Smooth Zoom", "Hold the Zoom key for a smooth camera zoom", Category.VISUALS);
		}

		public void restore(Minecraft mc) {
			if (orig != null) {
				mc.options.fov().set(orig);
				orig = null;
			}
		}

		@Override
		public void tick(Minecraft mc) {
			if (mc.options == null) return;
			boolean down = enabled && mc.screen == null && dev.lumora.Lumora.ZOOM.isDown();
			if (down) {
				if (orig == null) {
					orig = mc.options.fov().get();
					cur = orig;
				}
				cur += (fov.f() - cur) * 0.4f;
				mc.options.fov().set(Math.round(cur));
			} else if (orig != null) {
				cur += (orig - cur) * 0.4f;
				if (Math.abs(cur - orig) < 1f) {
					restore(mc);
				} else {
					mc.options.fov().set(Math.round(cur));
				}
			}
		}
	}

	// =====================================================================
	public static final class Notifications extends Module {
		private final Setting.Slider dur = slider("dur", "Duration", 1, 6, 0.5, 2.5, "s");
		private final Setting.Mode pos = mode("pos", "Position", 0, "Top right", "Bottom right");

		public Notifications() {
			super("notifications", "Notifications", "Slide-in toasts when modules are toggled", Category.VISUALS);
			layer = 2;
		}

		@Override
		public void tick(Minecraft mc) {
			Notifs.active = enabled;
			if (!enabled && !Notifs.LIST.isEmpty()) Notifs.LIST.clear();
		}

		@Override
		public void render(GuiGraphicsExtractor g, Ctx c) {
			List<Notifs.Entry> list = Notifs.LIST;
			if (list.isEmpty()) return;
			Font f = c.font;
			long life = (long) (dur.value * 1000);
			boolean top = pos.index == 0;
			int y = top ? 8 : c.sh - 8;
			for (int i = list.size() - 1; i >= 0; i--) {
				Notifs.Entry e = list.get(i);
				long age = c.now - e.t0;
				if (age > life + 400) {
					list.remove(i);
					continue;
				}
				int w = Math.max(f.width(e.title), f.width(e.msg)) + 26;
				int h = 26;
				float in = Anim.easeOut(age / 280f);
				float out = age > life ? Anim.clamp01((age - life) / 400f) : 0f;
				float old = Gfx.ga;
				Gfx.ga = (1f - out) * Math.min(1f, in + 0.2f);
				int x = c.sw - 8 - w + Math.round((1f - in) * (w + 12)) + Math.round(out * 20);
				int yy = top ? y : y - h;
				Gfx.panel(g, x, yy, w, h);
				int bar = e.good ? Gfx.GOOD : Gfx.BAD;
				Gfx.round(g, x + 5, yy + 5, 3, h - 10, 1, bar);
				Gfx.text(g, f, e.title, x + 13, yy + 5, Gfx.WHITE);
				Gfx.text(g, f, e.msg, x + 13, yy + 15, Gfx.DIM);
				Gfx.ga = old;
				y += top ? h + 5 : -(h + 5);
			}
		}
	}
}
