package dev.lumora.core;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A draggable HUD element. Position is stored as a fraction of the free screen space (resolution independent). */
public abstract class HudModule extends Module {
	public float fx, fy;
	public final float defX, defY;
	public int lx, ly, lw, lh;
	public boolean visible;
	public boolean grabbed;

	protected HudModule(String id, String name, String desc, float defX, float defY) {
		super(id, name, desc, Category.HUD);
		this.defX = defX;
		this.defY = defY;
		this.fx = defX;
		this.fy = defY;
	}

	/** Must set lw / lh. */
	protected abstract void measure(Ctx c);

	protected abstract void draw(GuiGraphicsExtractor g, int x, int y, Ctx c);

	/** When false the widget is hidden in-game (but still shown as a placeholder while the menu is open). */
	protected boolean live(Ctx c) {
		return true;
	}

	@Override
	public final void render(GuiGraphicsExtractor g, Ctx c) {
		visible = false;
		boolean lv = live(c);
		if (!c.editing && !lv) return;
		measure(c);
		final int m = 6;
		lx = m + Math.round(fx * Math.max(0, c.sw - lw - 2 * m));
		ly = m + Math.round(fy * Math.max(0, c.sh - lh - 2 * m));
		visible = true;
		draw(g, lx, ly, c);
		if (c.editing) {
			int col = grabbed ? 0xCC000000 | (Theme.accent & 0xFFFFFF) : 0x55000000 | (Theme.accent & 0xFFFFFF);
			Gfx.outline(g, lx - 2, ly - 2, lw + 4, lh + 4, col);
		}
	}

	public boolean contains(double mx, double my) {
		return visible && mx >= lx - 3 && mx <= lx + lw + 3 && my >= ly - 3 && my <= ly + lh + 3;
	}
}
