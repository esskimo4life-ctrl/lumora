package dev.lumora.core;

public final class Anim {
	private Anim() {
	}

	public static float approach(float cur, float target, float speed, float dt) {
		float k = 1f - (float) Math.exp(-speed * dt * Theme.animSpeed());
		return cur + (target - cur) * k;
	}

	public static float approachRaw(float cur, float target, float speed, float dt) {
		float k = 1f - (float) Math.exp(-speed * dt);
		return cur + (target - cur) * k;
	}

	public static float easeOut(float t) {
		t = Math.max(0f, Math.min(1f, t));
		float u = 1f - t;
		return 1f - u * u * u;
	}

	public static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}
}
