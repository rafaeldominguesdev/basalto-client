package com.basalto.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Valores animados por chave. Cada chamada aproxima o valor do alvo com
 * suavização exponencial, então dá pra pedir o alvo novo a cada frame.
 */
public final class Anim {
	public enum Motion { FULL, REDUCED, OFF }

	private static final Map<String, float[]> VALUES = new HashMap<>();
	private static long lastFrame = System.nanoTime();
	private static float dtMs = 16;
	private static int frame;
	public static Motion motion = Motion.FULL;

	private Anim() {}

	public static void frame() {
		long now = System.nanoTime();
		dtMs = Math.min(100, (now - lastFrame) / 1_000_000f);
		lastFrame = now;
		frame++;
		if (frame % 600 == 0) VALUES.entrySet().removeIf(e -> e.getValue()[1] < frame - 600);
	}

	/** Anima até o alvo em mais ou menos durationMs. Sem animação, vai direto. */
	public static float to(String key, float target, float durationMs) {
		float[] v = VALUES.get(key);
		if (v == null) {
			VALUES.put(key, new float[] {target, frame});
			return target;
		}
		v[1] = frame;
		if (motion == Motion.OFF || durationMs <= 0) {
			v[0] = target;
			return target;
		}
		float tau = durationMs / 3.5f;
		v[0] += (target - v[0]) * (1 - (float) Math.exp(-dtMs / tau));
		if (Math.abs(target - v[0]) < 0.001f) v[0] = target;
		return v[0];
	}

	/** Igual ao to(), mas começa de "from" quando a chave é nova (para entradas animadas). */
	public static float from(String key, float from, float target, float durationMs) {
		if (!VALUES.containsKey(key)) VALUES.put(key, new float[] {motion == Motion.OFF ? target : from, frame});
		return to(key, target, durationMs);
	}

	public static void reset(String key) {
		VALUES.remove(key);
	}

	/** Fator de deslocamento: movimentos somem com "reduzir movimento". */
	public static float move() {
		return motion == Motion.FULL ? 1f : 0f;
	}

	public static float dt() {
		return dtMs;
	}
}
