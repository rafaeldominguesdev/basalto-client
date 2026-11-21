package com.basalto.ui;

/** Operações em cores ARGB. */
public final class Colors {
	private Colors() {}

	public static int alpha(int argb) {
		return argb >>> 24;
	}

	public static int withAlpha(int argb, float a) {
		int ai = Math.round(Math.max(0, Math.min(1, a)) * 255);
		return (ai << 24) | (argb & 0xFFFFFF);
	}

	/** Multiplica o alfa atual da cor. */
	public static int fade(int argb, float k) {
		if (k >= 1f) return argb;
		int a = Math.round(alpha(argb) * Math.max(0, k));
		return (a << 24) | (argb & 0xFFFFFF);
	}

	/** Mistura linear de a para b (t = 0 a 1), incluindo o alfa. */
	public static int mix(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int aa = a >>> 24, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
		int ba = b >>> 24, br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
			| (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}
}
