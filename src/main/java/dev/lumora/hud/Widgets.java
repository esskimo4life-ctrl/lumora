package dev.lumora.hud;

import dev.lumora.Lumora;
import dev.lumora.core.*;
import dev.lumora.core.Module;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** All draggable HUD widgets. */
public final class Widgets {
	private Widgets() {
	}

	private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

	private static String roman(int n) {
		return n >= 0 && n < ROMAN.length ? ROMAN[n] : Integer.toString(n);
	}

	/** Small accent square used as a section icon. */
	private static void icon(GuiGraphicsExtractor g, int x, int y) {
		Gfx.round(g, x, y, 9, 9, 3, Gfx.withAlpha(Theme.accent, 0.35f));
		Gfx.round(g, x + 2, y + 2, 5, 5, 2, Theme.accent);
	}

	// =====================================================================
	public static final class InfoBar extends HudModule {
		private final Setting.Bool sFps = bool("fps", "Show FPS", true);
		private final Setting.Bool sPing = bool("ping", "Show ping", true);
		private final Setting.Bool sTime = bool("time", "Show time", true);
		private final Setting.Bool sServer = bool("server", "Show server", false);
		private final String[] lab = new String[4], val = new String[4];
		private int n;

		public InfoBar() {
			super("infobar", "Info Bar", "Pill with FPS, ping, clock and server", 0f, 0f);
		}

		@Override
		protected void measure(Ctx c) {
			n = 0;
			if (sFps.value) { lab[n] = "FPS"; val[n++] = Lumora.fpsStr; }
			if (sPing.value) { lab[n] = "Ping"; val[n++] = Lumora.pingStr; }
			if (sTime.value) { lab[n] = null; val[n++] = Lumora.clockStr; }
			if (sServer.value) { lab[n] = null; val[n++] = Lumora.serverStr; }
			Font f = c.font;
			int w = 4 + 11 + 6;
			for (int i = 0; i < n; i++) {
				if (i > 0) w += 9;
				if (lab[i] != null) w += f.width(lab[i]) + 3;
				w += f.width(val[i]);
			}
			lw = w + 7;
			lh = 17;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Font f = c.font;
			Gfx.panel(g, x, y, lw, lh);
			Gfx.round(g, x + 4, y + 3, 11, 11, 3, Theme.accent);
			Gfx.textNoShadow(g, f, "L", x + 8, y + 4, 0xFFFFFFFF);
			int cx = x + 21;
			for (int i = 0; i < n; i++) {
				if (i > 0) {
					Gfx.fill(g, cx + 3, y + 4, cx + 4, y + 13, 0x30FFFFFF);
					cx += 9;
				}
				if (lab[i] != null) {
					Gfx.text(g, f, lab[i], cx, y + 5, Gfx.DIM);
					cx += f.width(lab[i]) + 3;
				}
				Gfx.text(g, f, val[i], cx, y + 5, Gfx.WHITE);
				cx += f.width(val[i]);
			}
		}
	}

	// =====================================================================
	public static final class Coordinates extends HudModule {
		private final Setting.Bool sBps = bool("bps", "Show speed (BPS)", true);
		private final Setting.Bool sDir = bool("dir", "Show direction", true);
		private static final String[] DIRS = {"South +Z", "West -X", "North -Z", "East +X"};
		private String line1 = "", line2 = "";
		private int kx = Integer.MIN_VALUE, ky, kz, kb, kd;

		public Coordinates() {
			super("coords", "Coordinates", "Position, speed and facing", 0f, 0.075f);
		}

		@Override
		protected void measure(Ctx c) {
			var p = c.p;
			int ix = (int) Math.floor(p.getX()), iy = (int) Math.floor(p.getY()), iz = (int) Math.floor(p.getZ());
			int b = (int) Math.round(Lumora.bps * 10);
			int d = ((int) Math.floor(p.getYRot() / 90f + 0.5f)) & 3;
			if (ix != kx || iy != ky || iz != kz || b != kb || d != kd) {
				kx = ix; ky = iy; kz = iz; kb = b; kd = d;
				line1 = ix + "  " + iy + "  " + iz;
				StringBuilder sb = new StringBuilder();
				if (sBps.value) sb.append(b / 10).append('.').append(b % 10).append(" b/s");
				if (sDir.value) {
					if (sb.length() > 0) sb.append("   ");
					sb.append(DIRS[d]);
				}
				line2 = sb.toString();
			}
			Font f = c.font;
			int w = 22 + Math.max(f.width(line1), line2.isEmpty() ? 0 : f.width(line2)) + 8;
			lw = Math.max(w, 80);
			lh = line2.isEmpty() ? 17 : 28;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Font f = c.font;
			Gfx.panel(g, x, y, lw, lh);
			icon(g, x + 5, y + 4);
			Gfx.text(g, f, line1, x + 19, y + 5, Gfx.WHITE);
			if (!line2.isEmpty()) Gfx.text(g, f, line2, x + 19, y + 17, Gfx.DIM);
		}
	}

	// =====================================================================
	public static final class Keystrokes extends HudModule {
		private final Setting.Bool sMouse = bool("mouse", "Mouse buttons + CPS", true);
		private final Setting.Bool sExtra = bool("extra", "Space and Shift", true);
		private final float[] a = new float[9];
		private final long[] atk = new long[48], use = new long[48];
		private int ai, ui;
		private boolean pa, pu;
		private static final int K = 20, G = 3;

		public Keystrokes() {
			super("keystrokes", "Keystrokes", "WASD, mouse buttons with CPS, space", 1f, 0.62f);
		}

		private static int cps(long[] arr, long now) {
			int n = 0;
			for (long t : arr) if (t != 0 && now - t < 1000) n++;
			return n;
		}

		@Override
		protected void measure(Ctx c) {
			var o = c.mc.options;
			boolean da = o.keyAttack.isDown(), du = o.keyUse.isDown();
			if (da && !pa) { atk[ai] = c.now; ai = (ai + 1) % atk.length; }
			if (du && !pu) { use[ui] = c.now; ui = (ui + 1) % use.length; }
			pa = da;
			pu = du;
			lw = K * 3 + G * 2;
			lh = K * 2 + G;
			if (sMouse.value) lh += G + 22;
			if (sExtra.value) lh += G + 12;
		}

		private void key(GuiGraphicsExtractor g, Ctx c, int x, int y, int w, int h, String label, int idx, boolean down, String sub) {
			a[idx] = Anim.approach(a[idx], down ? 1f : 0f, 18f, c.dt);
			int r = Math.max(2, Theme.radius() - 1);
			int bg = Gfx.lerp(Theme.panelBg(), (Theme.accent & 0xFFFFFF) | 0xC8000000, a[idx]);
			Gfx.round(g, x, y, w, h, r, bg);
			int tc = Gfx.lerp(Gfx.DIM, Gfx.WHITE, a[idx]);
			if (sub == null) {
				Gfx.textC(g, c.font, label, x + w / 2, y + (h - 8) / 2, tc);
			} else {
				Gfx.textC(g, c.font, label, x + w / 2, y + 2, tc);
				Gfx.textC(g, c.font, sub, x + w / 2, y + 12, Gfx.lerp(Gfx.MUTED, Gfx.WHITE, a[idx]));
			}
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			var o = c.mc.options;
			int cx = x + K + G;
			key(g, c, cx, y, K, K, "W", 0, o.keyUp.isDown(), null);
			int ry = y + K + G;
			key(g, c, x, ry, K, K, "A", 1, o.keyLeft.isDown(), null);
			key(g, c, x + K + G, ry, K, K, "S", 2, o.keyDown.isDown(), null);
			key(g, c, x + 2 * (K + G), ry, K, K, "D", 3, o.keyRight.isDown(), null);
			int yy = ry + K + G;
			if (sMouse.value) {
				int hw = (lw - G) / 2;
				key(g, c, x, yy, hw, 22, "LMB", 4, o.keyAttack.isDown(), cps(atk, c.now) + " CPS");
				key(g, c, x + hw + G, yy, lw - hw - G, 22, "RMB", 5, o.keyUse.isDown(), cps(use, c.now) + " CPS");
				yy += 22 + G;
			}
			if (sExtra.value) {
				int hw = (lw - G) / 2;
				key(g, c, x, yy, hw, 12, "SPACE", 6, o.keyJump.isDown(), null);
				key(g, c, x + hw + G, yy, lw - hw - G, 12, "SHIFT", 7, o.keyShift.isDown(), null);
			}
		}
	}

	// =====================================================================
	public static final class Potions extends HudModule {
		private final Setting.Bool sTimer = bool("timer", "Show timers", true);
		private int count;

		public Potions() {
			super("potions", "Potions", "Active effects with timers", 0f, 0.3f);
		}

		@Override
		protected boolean live(Ctx c) {
			count = c.p.getActiveEffects().size();
			return count > 0;
		}

		private static String time(MobEffectInstance e) {
			if (e.isInfiniteDuration()) return "\u221E";
			int s = e.getDuration() / 20;
			int m = s / 60;
			s %= 60;
			return m + ":" + (s < 10 ? "0" : "") + s;
		}

		@Override
		protected void measure(Ctx c) {
			Font f = c.font;
			int w = f.width("Potions") + 28;
			Collection<MobEffectInstance> list = c.p.getActiveEffects();
			for (MobEffectInstance e : list) {
				int rw = 18 + f.width(name(e)) + 14 + (sTimer.value ? f.width(time(e)) : 0) + 8;
				if (rw > w) w = rw;
			}
			lw = Math.max(w, 104);
			lh = 17 + Math.max(1, count) * 12 + 2;
		}

		private static String name(MobEffectInstance e) {
			String n = e.getEffect().value().getDisplayName().getString();
			int amp = e.getAmplifier();
			return amp > 0 ? n + " " + roman(amp + 1) : n;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Font f = c.font;
			Gfx.panel(g, x, y, lw, lh);
			icon(g, x + 5, y + 4);
			Gfx.text(g, f, "Potions", x + 19, y + 5, Gfx.WHITE);
			int yy = y + 18;
			if (count == 0) {
				Gfx.text(g, f, "No active effects", x + 8, yy, Gfx.MUTED);
				return;
			}
			for (MobEffectInstance e : c.p.getActiveEffects()) {
				int col = 0xFF000000 | e.getEffect().value().getColor();
				Gfx.round(g, x + 7, yy + 2, 5, 5, 2, col);
				Gfx.text(g, f, name(e), x + 16, yy, Gfx.DIM);
				if (sTimer.value) Gfx.textR(g, f, time(e), x + lw - 7, yy, Gfx.WHITE);
				yy += 12;
			}
		}
	}

	// =====================================================================
	public static final class Armor extends HudModule {
		private final Setting.Bool sHand = bool("hand", "Include held item", true);
		private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		private int cells;

		public Armor() {
			super("armor", "Armor Status", "Armor and held item durability", 0f, 0.8f);
		}

		@Override
		protected void measure(Ctx c) {
			cells = SLOTS.length + (sHand.value ? 1 : 0);
			lw = cells * 20 + 6;
			lh = 28;
		}

		private void cell(GuiGraphicsExtractor g, int x, int y, ItemStack s) {
			if (s.isEmpty()) {
				Gfx.round(g, x + 4, y + 7, 12, 12, 3, 0x14FFFFFF);
				return;
			}
			g.item(s, x + 2, y + 3);
			if (s.isDamageableItem()) {
				int max = s.getMaxDamage();
				float r = max <= 0 ? 1f : 1f - (float) s.getDamageValue() / max;
				Gfx.fill(g, x + 2, y + 21, x + 18, y + 23, 0x55000000);
				int w = Math.max(1, Math.round(16 * Anim.clamp01(r)));
				Gfx.fill(g, x + 2, y + 21, x + 2 + w, y + 23, Gfx.lerp(Gfx.BAD, Gfx.GOOD, r));
			}
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Gfx.panel(g, x, y, lw, lh);
			int cx = x + 3;
			for (EquipmentSlot slot : SLOTS) {
				cell(g, cx, y, c.p.getItemBySlot(slot));
				cx += 20;
			}
			if (sHand.value) {
				Gfx.fill(g, cx - 1, y + 5, cx, y + lh - 5, 0x24FFFFFF);
				cell(g, cx, y, c.p.getMainHandItem());
			}
		}
	}

	// =====================================================================
	public static final class ItemCounter extends HudModule {
		private final Setting.Bool sHide = bool("hide", "Hide empty", false);
		private ItemStack[] icons;
		private final int[] counts = new int[5];
		private int shown;
		private final int[] order = new int[5];

		public ItemCounter() {
			super("itemcounter", "Item Counter", "Totems, pearls, apples, crystals, XP", 0.5f, 0.8f);
		}

		private void ensure() {
			if (icons == null) {
				icons = new ItemStack[]{
					new ItemStack(Items.TOTEM_OF_UNDYING), new ItemStack(Items.ENDER_PEARL),
					new ItemStack(Items.GOLDEN_APPLE), new ItemStack(Items.END_CRYSTAL),
					new ItemStack(Items.EXPERIENCE_BOTTLE)};
			}
		}

		@Override
		public void tick(Minecraft mc) {
			if (!enabled || mc.player == null) return;
			ensure();
			for (int i = 0; i < counts.length; i++) counts[i] = 0;
			var inv = mc.player.getInventory();
			int size = inv.getContainerSize();
			for (int i = 0; i < size; i++) {
				ItemStack s = inv.getItem(i);
				if (s.isEmpty()) continue;
				for (int k = 0; k < icons.length; k++) {
					if (s.is(icons[k].getItem())) {
						counts[k] += s.getCount();
						break;
					}
				}
			}
		}

		@Override
		protected void measure(Ctx c) {
			ensure();
			shown = 0;
			for (int i = 0; i < icons.length; i++) {
				if (sHide.value && counts[i] == 0) continue;
				order[shown++] = i;
			}
			int w = 6;
			for (int k = 0; k < shown; k++) w += 20 + c.font.width(Integer.toString(counts[order[k]])) + 6;
			lw = Math.max(w, 30);
			lh = 22;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Gfx.panel(g, x, y, lw, lh);
			int cx = x + 4;
			for (int k = 0; k < shown; k++) {
				int i = order[k];
				g.item(icons[i], cx, y + 3);
				String t = Integer.toString(counts[i]);
				Gfx.text(g, c.font, t, cx + 18, y + 7, counts[i] == 0 ? Gfx.MUTED : Gfx.WHITE);
				cx += 20 + c.font.width(t) + 6;
			}
		}
	}

	// =====================================================================
	public static final class TargetHud extends HudModule {
		private final Setting.Slider range = slider("range", "Max distance", 3, 24, 1, 8, " m");
		private final Setting.Bool sDist = bool("dist", "Show distance", true);
		private float vis, hpFast, hpSlow;
		private String name = "Player";
		private float hp = 20, max = 20, absorb, dist;

		public TargetHud() {
			super("targethud", "Target HUD", "Health and name of the entity you look at", 0.5f, 0.6f);
		}

		@Override
		protected boolean live(Ctx c) {
			Entity e = c.mc.crosshairPickEntity;
			boolean has = false;
			if (e instanceof LivingEntity le && le != c.p) {
				float d = c.p.distanceTo(le);
				if (d <= range.f()) {
					has = true;
					name = le.getName().getString();
					hp = le.getHealth();
					max = Math.max(1f, le.getMaxHealth());
					absorb = le.getAbsorptionAmount();
					dist = d;
				}
			}
			vis = Anim.approach(vis, has ? 1f : 0f, 10f, c.dt);
			float frac = Anim.clamp01(hp / max);
			hpFast = Anim.approach(hpFast, frac, 14f, c.dt);
			hpSlow = Anim.approach(hpSlow, frac, 3.5f, c.dt);
			return vis > 0.02f;
		}

		@Override
		protected void measure(Ctx c) {
			lw = 136;
			lh = 34;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Font f = c.font;
			float old = Gfx.ga;
			Gfx.ga = vis > 0.02f ? vis : 0.55f;
			Gfx.panel(g, x, y, lw, lh);
			Gfx.round(g, x + 5, y + 5, 24, 24, 5, Gfx.withAlpha(Theme.accent, 0.30f));
			String ini = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
			Gfx.textC(g, f, ini, x + 17, y + 13, Theme.accent2);
			Gfx.text(g, f, Gfx.fit(f, name, lw - 40), x + 35, y + 5, Gfx.WHITE);
			String hs = "HP / " + (Math.round((hp + absorb) * 10) / 10.0);
			if (sDist.value) hs += "  " + (Math.round(dist * 10) / 10.0) + "m";
			Gfx.text(g, f, hs, x + 35, y + 15, Gfx.DIM);
			int bx = x + 35, bw = lw - 42, by = y + 26;
			Gfx.round(g, bx, by, bw, 4, 2, 0x40FFFFFF);
			Gfx.round(g, bx, by, Math.max(2, Math.round(bw * hpSlow)), 4, 2, 0x88FFFFFF);
			int col = hpFast < 0.3f ? Gfx.BAD : Theme.accent;
			Gfx.round(g, bx, by, Math.max(2, Math.round(bw * hpFast)), 4, 2, col);
			Gfx.ga = old;
		}
	}

	// =====================================================================
	public static final class Compass extends HudModule {
		private static final String[] CARD = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};

		public Compass() {
			super("compass", "Compass", "Heading strip at the top", 0.5f, 0.0f);
		}

		@Override
		protected void measure(Ctx c) {
			lw = Math.min(190, c.sw - 40);
			lh = 19;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Font f = c.font;
			Gfx.panel(g, x, y, lw, lh);
			float bearing = (c.p.getYRot() + 180f) % 360f;
			if (bearing < 0) bearing += 360f;
			float ppd = 1.7f;
			int cx = x + lw / 2;
			float half = lw / 2f - 6f;
			int start = (int) Math.floor((bearing - half / ppd) / 5f) * 5;
			int end = (int) Math.ceil((bearing + half / ppd) / 5f) * 5;
			for (int a = start; a <= end; a += 5) {
				float dx = (a - bearing) * ppd;
				float fade = 1f - (float) Math.pow(Math.abs(dx) / (half + 1f), 2.0);
				if (fade <= 0.02f) continue;
				int px = cx + Math.round(dx);
				int am = ((a % 360) + 360) % 360;
				if (am % 45 == 0) {
					String s = CARD[am / 45];
					int col = am == 0 ? Theme.accent2 : (am % 90 == 0 ? Gfx.WHITE : Gfx.DIM);
					Gfx.textC(g, f, s, px, y + 3, Gfx.withAlpha(col, fade));
				} else if (am % 15 == 0) {
					Gfx.fill(g, px, y + 5, px + 1, y + 10, Gfx.rgba(0xFFFFFF, (int) (70 * fade)));
				} else {
					Gfx.fill(g, px, y + 6, px + 1, y + 9, Gfx.rgba(0xFFFFFF, (int) (40 * fade)));
				}
			}
			for (int i = 0; i < 3; i++) {
				Gfx.fill(g, cx - (2 - i), y + lh - 5 + i, cx + (2 - i) + 1, y + lh - 4 + i, Theme.accent);
			}
		}
	}

	// =====================================================================
	public static final class FrameGraph extends HudModule {
		private static final int N = 50;

		public FrameGraph() {
			super("framegraph", "Frame Graph", "Live frame-time chart", 1f, 0.0f);
		}

		@Override
		protected void measure(Ctx c) {
			lw = N * 2 + 10;
			lh = 44;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Font f = c.font;
			Gfx.panel(g, x, y, lw, lh);
			icon(g, x + 5, y + 3);
			Gfx.text(g, f, "Frametime", x + 19, y + 4, Gfx.WHITE);
			float[] fm = Lumora.frameMs;
			float sum = 0;
			int base = y + lh - 4;
			Gfx.fill(g, x + 5, base - 14, x + lw - 5, base - 13, 0x22FFFFFF);
			for (int i = 0; i < N; i++) {
				int idx = ((Lumora.frameIdx - N + i) % fm.length + fm.length) % fm.length;
				float ms = fm[idx];
				sum += ms;
				int h = Math.max(1, Math.min(22, Math.round(ms * 0.85f)));
				int col = ms <= 16.8f ? Theme.accent : (ms <= 33.4f ? 0xFFFBBF24 : Gfx.BAD);
				int px = x + 5 + i * 2;
				Gfx.fill(g, px, base - h, px + 2, base, col);
			}
			String avg = (Math.round(sum / N * 10) / 10.0) + " ms";
			Gfx.textR(g, f, avg, x + lw - 6, y + 4, Gfx.DIM);
		}
	}

	// =====================================================================
	public static final class Hotkeys extends HudModule {
		public Hotkeys() {
			super("hotkeys", "Hot Keys", "Reminder of the Lumora keys", 1f, 0.3f);
		}

		private static String key(net.minecraft.client.KeyMapping k) {
			return k.getTranslatedKeyMessage().getString();
		}

		@Override
		protected void measure(Ctx c) {
			Font f = c.font;
			int w = Math.max(f.width("Menu"), Math.max(f.width("Zoom"), f.width("Hide HUD"))) + 10;
			int kw = Math.max(f.width(key(Lumora.OPEN)), Math.max(f.width(key(Lumora.ZOOM)), f.width(key(Lumora.HIDE))));
			lw = 22 + w + kw + 14;
			lh = 17 + 3 * 12 + 2;
		}

		@Override
		protected void draw(GuiGraphicsExtractor g, int x, int y, Ctx c) {
			Font f = c.font;
			Gfx.panel(g, x, y, lw, lh);
			icon(g, x + 5, y + 4);
			Gfx.text(g, f, "Hot Keys", x + 19, y + 5, Gfx.WHITE);
			int yy = y + 18;
			row(g, f, x, yy, "Menu", key(Lumora.OPEN));
			row(g, f, x, yy + 12, "Zoom", key(Lumora.ZOOM));
			row(g, f, x, yy + 24, "Hide HUD", key(Lumora.HIDE));
		}

		private void row(GuiGraphicsExtractor g, Font f, int x, int y, String a, String k) {
			Gfx.text(g, f, a, x + 8, y, Gfx.DIM);
			Gfx.textR(g, f, k, x + lw - 8, y, Gfx.WHITE);
		}
	}
}
