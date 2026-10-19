package dev.lumora.core;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Tiny 2D toolkit on top of GuiGraphicsExtractor: rounded rects, glass panels, glow, fade. Only uses fill / fillGradient / text. */
public final class Gfx {
	private Gfx() {
	}

	public static final int WHITE = 0xFFFFFFFF;
	public static final int DIM = 0xFF9CA3B8;
	public static final int MUTED = 0xFF646B80;
	public static final int GOOD = 0xFF4ADE80;
	public static final int BAD = 0xFFF87171;

	/** Global alpha multiplier (for fade in/out). */
	public static float ga = 1f;

	private static final int[][] INSET = new int[17][];

	static {
		for (int r = 1; r <= 16; r++) {
			int[] a = new int[r];
			for (int i = 0; i < r; i++) {
				double dy = r - i - 0.5;
				a[i] = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
			}
			INSET[r] = a;
		}
	}

	public static int mul(int c) {
		if (ga >= 0.999f) return c;
		int a = (int) ((c >>> 24) * Math.max(0f, ga));
		return (a << 24) | (c & 0xFFFFFF);
	}

	public static int withAlpha(int c, float a) {
		int na = (int) ((c >>> 24) * Anim.clamp01(a));
		return (na << 24) | (c & 0xFFFFFF);
	}

	public static int rgba(int rgb, int a) {
		return (Math.max(0, Math.min(255, a)) << 24) | (rgb & 0xFFFFFF);
	}

	public static int lerp(int a, int b, float t) {
		t = Anim.clamp01(t);
		int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
			| ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
	}

	public static void fill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int c) {
		if (x2 <= x1 || y2 <= y1) return;
		g.fill(x1, y1, x2, y2, mul(c));
	}

	public static void vgrad(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int c1, int c2) {
		if (x2 <= x1 || y2 <= y1) return;
		g.fillGradient(x1, y1, x2, y2, mul(c1), mul(c2));
	}

	public static void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
		fill(g, x, y, x + w, y + 1, c);
		fill(g, x, y + h - 1, x + w, y + h, c);
		fill(g, x, y + 1, x + 1, y + h - 1, c);
		fill(g, x + w - 1, y + 1, x + w, y + h - 1, c);
	}

	public static void round(GuiGraphicsExtractor g, int x, int y, int w, int h, int r, int c) {
		if (w <= 0 || h <= 0) return;
		r = Math.min(r, Math.min(w, h) / 2);
		if (r <= 0) {
			fill(g, x, y, x + w, y + h, c);
			return;
		}
		if (r > 16) r = 16;
		int[] in = INSET[r];
		int col = mul(c);
		for (int i = 0; i < r; i++) {
			int k = in[i];
			g.fill(x + k, y + i, x + w - k, y + i + 1, col);
			g.fill(x + k, y + h - 1 - i, x + w - k, y + h - i, col);
		}
		g.fill(x, y + r, x + w, y + h - r, col);
	}

	/** Glass panel: soft shadow, translucent body, top sheen. */
	public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		int r = Theme.radius();
		if (Theme.SHADOW.value) {
			round(g, x - 3, y - 3, w + 6, h + 6, r + 3, 0x0C000000);
			round(g, x - 2, y - 2, w + 4, h + 4, r + 2, 0x12000000);
			round(g, x - 1, y - 1, w + 2, h + 2, r + 1, 0x1A000000);
		}
		round(g, x, y, w, h, r, Theme.panelBg());
		fill(g, x + r, y, x + w - r, y + 1, 0x26FFFFFF);
	}

	public static void text(GuiGraphicsExtractor g, Font f, String s, int x, int y, int c) {
		g.text(f, s, x, y, mul(c), Theme.TEXT_SHADOW.value);
	}

	public static void textNoShadow(GuiGraphicsExtractor g, Font f, String s, int x, int y, int c) {
		g.text(f, s, x, y, mul(c), false);
	}

	public static void textC(GuiGraphicsExtractor g, Font f, String s, int cx, int y, int c) {
		g.text(f, s, cx - f.width(s) / 2, y, mul(c), Theme.TEXT_SHADOW.value);
	}

	public static void textR(GuiGraphicsExtractor g, Font f, String s, int rx, int y, int c) {
		g.text(f, s, rx - f.width(s), y, mul(c), Theme.TEXT_SHADOW.value);
	}

	public static String fit(Font f, String s, int maxW) {
		if (f.width(s) <= maxW) return s;
		int n = s.length();
		while (n > 1 && f.width(s.substring(0, n) + "..") > maxW) n--;
		return s.substring(0, n) + "..";
	}

	/** Pill toggle switch. t = 0..1 animation. */
	public static void toggle(GuiGraphicsExtractor g, int x, int y, float t) {
		int off = 0xFF2A2A3A;
		int on = Theme.accent;
		round(g, x, y, 24, 12, 6, lerp(off, on, t));
		int kx = x + 2 + Math.round(t * 12);
		round(g, kx, y + 2, 8, 8, 4, 0xFFFFFFFF);
	}

	/** Soft screen-edge glow. */
	public static void edge(GuiGraphicsExtractor g, int sw, int sh, int rgb, float a, int t) {
		a = Anim.clamp01(a);
		if (a < 0.01f || t < 2) return;
		int strong = ((int) (255 * a) << 24) | (rgb & 0xFFFFFF);
		int clear = rgb & 0xFFFFFF;
		vgrad(g, 0, 0, sw, t, strong, clear);
		vgrad(g, 0, sh - t, sw, sh, clear, strong);
		int steps = Math.min(t, 14);
		int step = Math.max(1, t / steps);
		for (int i = 0; i < steps; i++) {
			float k = 1f - (float) i / steps;
			int col = ((int) (255 * a * k * k) << 24) | (rgb & 0xFFFFFF);
			fill(g, i * step, t, (i + 1) * step, sh - t, col);
			fill(g, sw - (i + 1) * step, t, sw - i * step, sh - t, col);
		}
	}
}
