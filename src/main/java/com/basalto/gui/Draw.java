package com.basalto.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Pequenos utilitários de desenho para o menu e o HUD. */
public final class Draw {
	public static final int TEXT = 0xFFE5E7EB;
	public static final int TEXT_DIM = 0xFF9CA3AF;
	public static final int TEXT_DARK = 0xFF06140A;
	public static final int HEADER = 0xF0161920;
	public static final int FIELD = 0xFF0B0D11;
	public static final int BORDER = 0xFF2E333D;
	public static final int HOVER = 0x22FFFFFF;

	private Draw() {}

	/** Escurece/clareia uma cor ARGB mantendo o alfa. */
	public static int shade(int argb, float f) {
		int a = argb >>> 24;
		int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
		int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
		int b = Math.min(255, (int) ((argb & 0xFF) * f));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static void border(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
		g.fill(x1, y1, x2, y1 + 1, color);
		g.fill(x1, y2 - 1, x2, y2, color);
		g.fill(x1, y1, x1 + 1, y2, color);
		g.fill(x2 - 1, y1, x2, y2, color);
	}

	/** Preenche um polígono convexo linha por linha. */
	private static void polygon(GuiGraphicsExtractor g, float[] xs, float[] ys, int color) {
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		for (float y : ys) {
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		int n = xs.length;
		for (int row = (int) Math.floor(minY); row < (int) Math.ceil(maxY); row++) {
			float sy = row + 0.5f;
			float left = Float.MAX_VALUE, right = -Float.MAX_VALUE;
			for (int i = 0; i < n; i++) {
				float x1 = xs[i], y1 = ys[i], x2 = xs[(i + 1) % n], y2 = ys[(i + 1) % n];
				if ((sy >= y1 && sy < y2) || (sy >= y2 && sy < y1)) {
					float ix = x1 + (sy - y1) / (y2 - y1) * (x2 - x1);
					left = Math.min(left, ix);
					right = Math.max(right, ix);
				}
			}
			if (left <= right) g.fill(Math.round(left), row, Math.round(right), row + 1, color);
		}
	}

	/** O ícone do Basalto: um cubo isométrico em três tons da cor de destaque. */
	public static void cube(GuiGraphicsExtractor g, int x, int y, int size, int accent) {
		float k = size / 24f;
		polygon(g, new float[] {x + 12 * k, x + 21 * k, x + 12 * k, x + 3 * k}, new float[] {y + 2 * k, y + 7 * k, y + 12 * k, y + 7 * k}, accent);
		polygon(g, new float[] {x + 3 * k, x + 12 * k, x + 12 * k, x + 3 * k}, new float[] {y + 7 * k, y + 12 * k, y + 22 * k, y + 17 * k}, shade(accent, 0.62f));
		polygon(g, new float[] {x + 21 * k, x + 12 * k, x + 12 * k, x + 21 * k}, new float[] {y + 7 * k, y + 12 * k, y + 22 * k, y + 17 * k}, shade(accent, 0.34f));
		polygon(g, new float[] {x + 12 * k, x + 16.4f * k, x + 12 * k, x + 7.6f * k}, new float[] {y + 4.6f * k, y + 7 * k, y + 9.4f * k, y + 7 * k}, 0xFF06140A);
	}
}
