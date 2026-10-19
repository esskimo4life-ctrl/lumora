package dev.lumora.core;

public final class Theme {
	private Theme() {
	}

	public static final Module MODULE = new Module("theme", "Theme", "Look and feel of every Lumora element", Category.THEME) {
	};

	public static final Setting.Mode ACCENT = MODULE.mode("accent", "Accent color", 0,
		"Violet", "Cyan", "Rose", "Emerald", "Amber", "Azure", "Rainbow");
	public static final Setting.Slider RADIUS = MODULE.slider("radius", "Corner radius", 2, 8, 1, 5, "px");
	public static final Setting.Slider OPACITY = MODULE.slider("opacity", "Panel opacity", 35, 95, 1, 70, "%");
	public static final Setting.Bool SHADOW = MODULE.bool("shadow", "Panel shadow", true);
	public static final Setting.Bool TEXT_SHADOW = MODULE.bool("textshadow", "Text shadow", true);
	public static final Setting.Slider ANIM = MODULE.slider("anim", "Animation speed", 0.5, 2.0, 0.1, 1.0, "x");
	public static final Setting.Action RESET = MODULE.action("reset", "HUD positions", "Reset", Theme::resetPositions);

	private static final int[] MAIN = {0x8B5CF6, 0x22D3EE, 0xF43F5E, 0x10B981, 0xF59E0B, 0x3B82F6};
	private static final int[] SOFT = {0xC4B5FD, 0xA5F3FC, 0xFDA4AF, 0x6EE7B7, 0xFDE68A, 0x93C5FD};

	/** Opaque ARGB accent colors, refreshed once per frame. */
	public static int accent = 0xFF8B5CF6;
	public static int accent2 = 0xFFC4B5FD;

	static {
		MODULE.togglable = false;
		MODULE.enabled = true;
	}

	public static void update(long now) {
		int i = ACCENT.index;
		if (i >= MAIN.length) {
			float h = (now % 9000L) / 9000f;
			accent = 0xFF000000 | hsv(h, 0.62f, 1f);
			accent2 = 0xFF000000 | hsv((h + 0.12f) % 1f, 0.35f, 1f);
		} else {
			accent = 0xFF000000 | MAIN[i];
			accent2 = 0xFF000000 | SOFT[i];
		}
	}

	public static int radius() {
		return RADIUS.i();
	}

	public static float animSpeed() {
		return ANIM.f();
	}

	public static int panelBg() {
		int a = (int) (OPACITY.value / 100.0 * 255.0);
		return (a << 24) | 0x0B0B13;
	}

	public static void resetPositions() {
		for (Module m : dev.lumora.Lumora.MODULES) {
			if (m instanceof HudModule h) {
				h.fx = h.defX;
				h.fy = h.defY;
			}
		}
	}

	public static int hsv(float h, float s, float v) {
		float r = 0, g = 0, b = 0;
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = v * (1f - s), q = v * (1f - f * s), t = v * (1f - (1f - f) * s);
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}
}
