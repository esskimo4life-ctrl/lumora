package dev.lumora.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.lumora.Lumora;
import dev.lumora.core.*;
import dev.lumora.core.Module;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** The Lumora menu: tabs, module cards with switches, per-module settings, draggable HUD editing. */
public final class LumoraScreen extends Screen {
	private static final int T_TAB = 0, T_CARD = 1, T_GEAR = 2, T_BOOL = 3, T_SLIDER = 4, T_MODE = 5, T_ACTION = 6, T_BACK = 7;
	private static final String[] TABS = {"Visuals", "HUD", "Theme"};

	private static final class Hit {
		int x, y, w, h, type, aux;
		Object ref;
		boolean clip;
	}

	private final List<Hit> hits = new ArrayList<>();
	private int hitN;

	private int tab;
	private Module open;
	private float scroll, scrollT, openT;
	private final float[] tabA = new float[3];
	private final int[] tabX = new int[3], tabW = new int[3];
	private float ulX = -1, ulW;
	private long lastNano;

	private int wx, wy, ww, wh;
	private int clipY1, clipY2, clipX1, clipX2;

	private Setting.Slider dragSlider;
	private int dragTrackX, dragTrackW;
	private HudModule dragHud;

	public LumoraScreen() {
		super(Component.literal("Lumora"));
	}

	private void hit(int x, int y, int w, int h, int type, Object ref, int aux, boolean clip) {
		Hit k;
		if (hitN < hits.size()) {
			k = hits.get(hitN);
		} else {
			k = new Hit();
			hits.add(k);
		}
		hitN++;
		k.x = x;
		k.y = y;
		k.w = w;
		k.h = h;
		k.type = type;
		k.ref = ref;
		k.aux = aux;
		k.clip = clip;
	}

	private static boolean in(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	// ------------------------------------------------------------------ render
	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
		Minecraft mc = Minecraft.getInstance();
		Font f = mc.font;
		long nano = System.nanoTime();
		float dt = lastNano == 0 ? 0.016f : Math.min(0.05f, (nano - lastNano) / 1.0e9f);
		lastNano = nano;
		Theme.update(System.currentTimeMillis());

		openT = Anim.approachRaw(openT, 1f, 9f, dt);
		float ease = Anim.easeOut(openT);
		Gfx.ga = ease;
		hitN = 0;

		Gfx.vgrad(g, 0, 0, width, height, 0x38000008, 0x8C050510);

		ww = Math.min(width - 12, Math.max(320, Math.min(560, (int) (width * 0.62f))));
		wh = Math.min(height - 12, Math.max(200, Math.min(340, (int) (height * 0.68f))));
		wx = (width - ww) / 2;
		wy = (height - wh) / 2 + Math.round((1f - ease) * 12f);

		int r = Theme.radius() + 3;
		Gfx.round(g, wx - 3, wy - 3, ww + 6, wh + 6, r + 3, 0x14000000);
		Gfx.round(g, wx - 1, wy - 1, ww + 2, wh + 2, r + 1, Gfx.rgba(Theme.accent, 0x44));
		Gfx.round(g, wx, wy, ww, wh, r, 0xF40E0E16);
		Gfx.vgrad(g, wx + r, wy + 1, wx + ww - r, wy + 40, Gfx.rgba(Theme.accent, 0x26), Gfx.rgba(Theme.accent, 0));

		// ---- tabs
		int ty = wy + 14;
		int tx = wx + 18;
		for (int i = 0; i < TABS.length; i++) {
			tabW[i] = f.width(TABS[i]);
			tabX[i] = tx;
			tx += tabW[i] + 16;
		}
		for (int i = 0; i < TABS.length; i++) {
			boolean hov = in(mouseX, mouseY, tabX[i] - 4, ty - 4, tabW[i] + 8, 17);
			float target = tab == i ? 1f : (hov ? 0.55f : 0f);
			tabA[i] = Anim.approachRaw(tabA[i], target, 14f, dt);
			Gfx.textNoShadow(g, f, TABS[i], tabX[i], ty, Gfx.lerp(0xFF565C72, Gfx.WHITE, tabA[i]));
			if (i < TABS.length - 1) Gfx.textNoShadow(g, f, "/", tabX[i] + tabW[i] + 5, ty, 0xFF343848);
			hit(tabX[i] - 4, ty - 4, tabW[i] + 8, 17, T_TAB, null, i, false);
		}
		if (ulX < 0) {
			ulX = tabX[tab];
			ulW = tabW[tab];
		}
		ulX = Anim.approachRaw(ulX, tabX[tab], 16f, dt);
		ulW = Anim.approachRaw(ulW, tabW[tab], 16f, dt);
		Gfx.round(g, Math.round(ulX), ty + 12, Math.round(ulW), 2, 1, Theme.accent);

		String ver = "v" + Lumora.VERSION;
		Gfx.textR(g, f, ver, wx + ww - 16, ty, Gfx.MUTED);
		Gfx.textR(g, f, "LUMORA", wx + ww - 16 - f.width(ver) - 6, ty, Theme.accent2);
		Gfx.fill(g, wx + 14, wy + 32, wx + ww - 14, wy + 33, 0x16FFFFFF);

		// ---- content
		int cx1 = wx + 14, cy1 = wy + 40, cx2 = wx + ww - 14, cy2 = wy + wh - 22;
		if (tab == 2) {
			drawSettings(g, f, Theme.MODULE.settings, cx1, cy1, cx2, cy2, mouseX, mouseY, dt);
		} else if (open != null) {
			boolean hov = in(mouseX, mouseY, cx1, cy1, 46, 16);
			Gfx.round(g, cx1, cy1, 46, 16, 6, hov ? 0xFF22222F : 0xFF171722);
			Gfx.textC(g, f, "< Back", cx1 + 23, cy1 + 4, Gfx.DIM);
			hit(cx1, cy1, 46, 16, T_BACK, null, 0, false);
			Gfx.text(g, f, open.name, cx1 + 54, cy1 + 4, Gfx.WHITE);
			Gfx.textR(g, f, Gfx.fit(f, open.desc, Math.max(40, cx2 - cx1 - 54 - f.width(open.name) - 14)), cx2, cy1 + 4, Gfx.MUTED);
			drawSettings(g, f, open.settings, cx1, cy1 + 24, cx2, cy2, mouseX, mouseY, dt);
		} else {
			drawCards(g, f, cx1, cy1, cx2, cy2, mouseX, mouseY, dt);
		}

		String hint = tab == 1 ? "Drag HUD elements anywhere outside this window to move them" : "Click a card to toggle  -  dots open its settings";
		Gfx.textNoShadow(g, f, Gfx.fit(f, hint, ww - 40), wx + 18, wy + wh - 14, 0xFF4A5068);
		Gfx.ga = 1f;
	}

	private void drawCards(GuiGraphicsExtractor g, Font f, int x1, int y1, int x2, int y2, int mx, int my, float dt) {
		Category want = tab == 0 ? Category.VISUALS : Category.HUD;
		List<Module> list = new ArrayList<>();
		for (Module m : Lumora.MODULES) if (m.cat == want) list.add(m);

		int cw = x2 - x1;
		int cols = cw >= 330 ? 2 : 1;
		int gap = 6, ch = 30;
		int rows = (list.size() + cols - 1) / cols;
		int total = rows * (ch + gap);
		int view = y2 - y1;
		int maxScroll = Math.max(0, total - view);
		boolean bar = maxScroll > 0;
		scrollT = Math.max(0, Math.min(maxScroll, scrollT));
		scroll = Anim.approachRaw(scroll, scrollT, 16f, dt);
		int cardW = (cw - gap * (cols - 1) - (bar ? 7 : 0)) / cols;

		clipX1 = x1;
		clipX2 = x2;
		clipY1 = y1;
		clipY2 = y2;
		g.enableScissor(x1, y1, x2, y2);
		for (int i = 0; i < list.size(); i++) {
			Module m = list.get(i);
			int x = x1 + (i % cols) * (cardW + gap);
			int y = y1 + (i / cols) * (ch + gap) - Math.round(scroll);
			if (y + ch < y1 || y > y2) continue;
			boolean hov = in(mx, my, x, y, cardW, ch) && in(mx, my, x1, y1, cw, view);
			m.hover = Anim.approachRaw(m.hover, hov ? 1f : 0f, 14f, dt);
			m.anim = Anim.approachRaw(m.anim, m.enabled ? 1f : 0f, 14f, dt);
			int base = Gfx.lerp(0xFF13131D, 0xFF1A1A27, m.hover);
			base = Gfx.lerp(base, Theme.accent, 0.16f * m.anim);
			Gfx.round(g, x, y, cardW, ch, 7, base);
			if (m.anim > 0.02f) Gfx.round(g, x, y + 7, 2, ch - 14, 1, Gfx.withAlpha(Theme.accent, m.anim));
			Gfx.textNoShadow(g, f, Gfx.fit(f, m.name, cardW - 74), x + 11, y + 6, Gfx.lerp(0xFFB4BACB, Gfx.WHITE, m.anim));
			Gfx.textNoShadow(g, f, Gfx.fit(f, m.desc, cardW - 66), x + 11, y + 17, 0xFF565C72);
			Gfx.toggle(g, x + cardW - 34, y + 9, m.anim);
			hit(x, y, cardW, ch, T_CARD, m, 0, true);
			if (!m.settings.isEmpty()) {
				int gx = x + cardW - 52, gy = y + ch / 2 - 5;
				boolean gh = in(mx, my, gx - 4, y, 18, ch) && hov;
				int dc = gh ? Gfx.WHITE : 0xFF7B8197;
				for (int d = 0; d < 3; d++) Gfx.round(g, gx + 3, gy + d * 4, 3, 3, 1, dc);
				hit(gx - 4, y, 18, ch, T_GEAR, m, 0, true);
			}
		}
		g.disableScissor();
		scrollbar(g, x2, y1, y2, maxScroll, total, view);
	}

	private void drawSettings(GuiGraphicsExtractor g, Font f, List<Setting> list, int x1, int y1, int x2, int y2, int mx, int my, float dt) {
		int rowH = 22, gap = 4;
		int total = list.size() * (rowH + gap);
		int view = y2 - y1;
		int maxScroll = Math.max(0, total - view);
		boolean bar = maxScroll > 0;
		scrollT = Math.max(0, Math.min(maxScroll, scrollT));
		scroll = Anim.approachRaw(scroll, scrollT, 16f, dt);
		int w = x2 - x1 - (bar ? 7 : 0);

		clipX1 = x1;
		clipX2 = x2;
		clipY1 = y1;
		clipY2 = y2;
		g.enableScissor(x1, y1, x2, y2);
		int y = y1 - Math.round(scroll);
		for (Setting s : list) {
			if (y + rowH >= y1 && y <= y2) {
				boolean hov = in(mx, my, x1, y, w, rowH) && in(mx, my, x1, y1, x2 - x1, view);
				Gfx.round(g, x1, y, w, rowH, 7, hov ? 0xFF1A1A27 : 0xFF13131D);
				Gfx.textNoShadow(g, f, s.label, x1 + 10, y + 7, 0xFFC2C8D8);
				if (s instanceof Setting.Bool b) {
					b.anim = Anim.approachRaw(b.anim, b.value ? 1f : 0f, 16f, dt);
					Gfx.toggle(g, x1 + w - 34, y + 5, b.anim);
					hit(x1, y, w, rowH, T_BOOL, b, 0, true);
				} else if (s instanceof Setting.Slider sl) {
					int tw = Math.min(110, w / 3);
					int tx = x1 + w - 12 - tw;
					int ty = y + 9;
					Gfx.round(g, tx, ty, tw, 4, 2, 0x30FFFFFF);
					int fw = Math.max(3, Math.round(tw * (float) sl.t()));
					Gfx.round(g, tx, ty, fw, 4, 2, Theme.accent);
					Gfx.round(g, tx + fw - 4, ty - 2, 8, 8, 4, Gfx.WHITE);
					Gfx.textR(g, f, sl.text(), tx - 8, y + 7, Gfx.DIM);
					// hit box starts 4px left of the track; aux keeps the track width for dragging
					hit(tx - 4, y, tw + 8, rowH, T_SLIDER, sl, tw, true);
				} else if (s instanceof Setting.Mode m) {
					String t = m.get();
					int pw = f.width(t) + 16;
					int px = x1 + w - 10 - pw;
					Gfx.round(g, px, y + 3, pw, 16, 7, Gfx.rgba(Theme.accent, 0x3A));
					Gfx.textNoShadow(g, f, t, px + 8, y + 7, Theme.accent2);
					hit(x1, y, w, rowH, T_MODE, m, 0, true);
				} else if (s instanceof Setting.Action a) {
					int pw = f.width(a.button) + 16;
					int px = x1 + w - 10 - pw;
					Gfx.round(g, px, y + 3, pw, 16, 7, Gfx.rgba(Theme.accent, 0x3A));
					Gfx.textNoShadow(g, f, a.button, px + 8, y + 7, Theme.accent2);
					hit(px, y + 3, pw, 16, T_ACTION, a, 0, true);
				}
			}
			y += rowH + gap;
		}
		g.disableScissor();
		scrollbar(g, x2, y1, y2, maxScroll, total, view);
	}

	private void scrollbar(GuiGraphicsExtractor g, int x2, int y1, int y2, int maxScroll, int total, int view) {
		if (maxScroll <= 0) return;
		int th = Math.max(14, view * view / Math.max(1, total));
		int ty = y1 + Math.round((view - th) * (scroll / maxScroll));
		Gfx.round(g, x2 - 3, y1, 3, view, 1, 0x14FFFFFF);
		Gfx.round(g, x2 - 3, ty, 3, th, 1, Gfx.rgba(Theme.accent, 0xAA));
	}

	// ------------------------------------------------------------------ input
	private void sliderTo(double mx) {
		if (dragSlider == null) return;
		dragSlider.setFromT((mx - dragTrackX) / Math.max(1, dragTrackW));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
		double mx = event.x(), my = event.y();
		for (int i = hitN - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!in(mx, my, h.x, h.y, h.w, h.h)) continue;
			if (h.clip && !in(mx, my, clipX1, clipY1, clipX2 - clipX1, clipY2 - clipY1)) continue;
			switch (h.type) {
				case T_TAB -> {
					tab = h.aux;
					open = null;
					scroll = 0;
					scrollT = 0;
				}
				case T_CARD -> ((Module) h.ref).toggleByUser();
				case T_GEAR -> {
					open = (Module) h.ref;
					scroll = 0;
					scrollT = 0;
				}
				case T_BACK -> {
					open = null;
					scroll = 0;
					scrollT = 0;
				}
				case T_BOOL -> {
					Setting.Bool b = (Setting.Bool) h.ref;
					b.value = !b.value;
				}
				case T_SLIDER -> {
					dragSlider = (Setting.Slider) h.ref;
					dragTrackX = h.x + 4;
					dragTrackW = h.aux;
					sliderTo(mx);
				}
				case T_MODE -> ((Setting.Mode) h.ref).next();
				case T_ACTION -> ((Setting.Action) h.ref).run.run();
				default -> {
				}
			}
			return true;
		}
		if (!in(mx, my, wx, wy, ww, wh)) {
			for (int i = Lumora.MODULES.size() - 1; i >= 0; i--) {
				if (Lumora.MODULES.get(i) instanceof HudModule hm && hm.enabled && hm.contains(mx, my)) {
					dragHud = hm;
					hm.grabbed = true;
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubled);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragSlider != null) {
			sliderTo(event.x());
			return true;
		}
		if (dragHud != null) {
			Ctx c = Lumora.ctx;
			dragHud.fx = Anim.clamp01(dragHud.fx + (float) (dx / Math.max(1, c.sw - dragHud.lw - 12)));
			dragHud.fy = Anim.clamp01(dragHud.fy + (float) (dy / Math.max(1, c.sh - dragHud.lh - 12)));
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean used = dragSlider != null || dragHud != null;
		dragSlider = null;
		if (dragHud != null) {
			dragHud.grabbed = false;
			dragHud = null;
		}
		if (used) {
			Config.save();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		scrollT -= (float) (sy * 18.0);
		return true;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == InputConstants.KEY_RSHIFT) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		Config.save();
		super.onClose();
	}
}
