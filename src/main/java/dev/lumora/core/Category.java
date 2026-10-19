package dev.lumora.core;

public enum Category {
	VISUALS("Visuals"), HUD("HUD"), THEME("Theme");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}
