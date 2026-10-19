package dev.lumora.core;

public abstract class Setting {
	public final String key;
	public final String label;

	protected Setting(String key, String label) {
		this.key = key;
		this.label = label;
	}

	public abstract String serialize();

	public abstract void deserialize(String s);

	public static final class Bool extends Setting {
		public boolean value;
		public float anim;

		public Bool(String key, String label, boolean def) {
			super(key, label);
			this.value = def;
			this.anim = def ? 1f : 0f;
		}

		public String serialize() {
			return Boolean.toString(value);
		}

		public void deserialize(String s) {
			value = Boolean.parseBoolean(s);
			anim = value ? 1f : 0f;
		}
	}

	public static final class Slider extends Setting {
		public final double min, max, step;
		public double value;
		public final String suffix;

		public Slider(String key, String label, double min, double max, double step, double def, String suffix) {
			super(key, label);
			this.min = min;
			this.max = max;
			this.step = step;
			this.value = def;
			this.suffix = suffix;
		}

		public float f() {
			return (float) value;
		}

		public int i() {
			return (int) Math.round(value);
		}

		public void setFromT(double t) {
			t = Math.max(0, Math.min(1, t));
			double v = min + t * (max - min);
			if (step > 0) v = Math.round((v - min) / step) * step + min;
			value = Math.max(min, Math.min(max, v));
		}

		public double t() {
			return (value - min) / (max - min);
		}

		public String text() {
			if (step >= 1) return Integer.toString((int) Math.round(value)) + suffix;
			if (step >= 0.1) return (Math.round(value * 10) / 10.0) + suffix;
			return (Math.round(value * 100) / 100.0) + suffix;
		}

		public String serialize() {
			return Double.toString(value);
		}

		public void deserialize(String s) {
			double v = Double.parseDouble(s);
			value = Math.max(min, Math.min(max, v));
		}
	}

	public static final class Mode extends Setting {
		public final String[] options;
		public int index;

		public Mode(String key, String label, int def, String... options) {
			super(key, label);
			this.options = options;
			this.index = def;
		}

		public String get() {
			return options[index];
		}

		public void next() {
			index = (index + 1) % options.length;
		}

		public String serialize() {
			return Integer.toString(index);
		}

		public void deserialize(String s) {
			int v = Integer.parseInt(s);
			if (v >= 0 && v < options.length) index = v;
		}
	}

	public static final class Action extends Setting {
		public final String button;
		public final Runnable run;

		public Action(String key, String label, String button, Runnable run) {
			super(key, label);
			this.button = button;
			this.run = run;
		}

		public String serialize() {
			return "";
		}

		public void deserialize(String s) {
		}
	}
}
