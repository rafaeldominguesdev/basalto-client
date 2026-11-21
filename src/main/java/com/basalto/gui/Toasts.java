package com.basalto.gui;

import java.util.ArrayList;
import java.util.List;

import com.basalto.ui.Anim;
import com.basalto.ui.Colors;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;
import com.basalto.ui.Ui;

/** Avisos temporários dentro do menu (salvou, exportou, deu erro). */
public final class Toasts {
	public enum Kind { SUCCESS, INFO, WARNING, ERROR }

	private record Toast(long id, long time, Kind kind, String title, String detail) {}

	private static final long LIFE = 3400;
	private static final List<Toast> LIST = new ArrayList<>();
	private static long seq;

	private Toasts() {}

	public static void show(Kind kind, String title, String detail) {
		LIST.add(new Toast(seq++, System.currentTimeMillis(), kind, title, detail));
		while (LIST.size() > 4) LIST.remove(0);
	}

	public static void success(String title, String detail) {
		show(Kind.SUCCESS, title, detail);
	}

	public static void error(String title, String detail) {
		show(Kind.ERROR, title, detail);
	}

	/** Pilha no canto inferior direito da área (right, bottom). */
	public static void draw(Ui ui, float right, float bottom) {
		Theme t = ui.t;
		long now = System.currentTimeMillis();
		LIST.removeIf(x -> now - x.time > LIFE + 400);
		float y = bottom;
		float w = 200;
		for (int i = LIST.size() - 1; i >= 0; i--) {
			Toast x = LIST.get(i);
			String key = "toast:" + x.id;
			boolean leaving = now - x.time > LIFE;
			float a = Anim.from(key, 0, leaving ? 0 : 1, Theme.BASE);
			float h = x.detail == null ? 24 : 34;
			String hoverId = key + ":h";
			boolean hovered = ui.hover(hoverId, right - w, y - h, w, h);
			if (hovered && !leaving) LIST.set(i, new Toast(x.id, Math.max(x.time, now - LIFE + 1200), x.kind, x.title, x.detail));
			float slide = (1 - a) * 16 * Anim.move();
			float tx = right - w + slide;
			float ty = y - h;
			int color = switch (x.kind) {
				case SUCCESS -> t.accentText;
				case INFO -> t.info;
				case WARNING -> t.warning;
				case ERROR -> t.danger;
			};
			String icon = switch (x.kind) {
				case SUCCESS -> "circle-check";
				case INFO -> "info";
				case WARNING -> "triangle-alert";
				case ERROR -> "circle-alert";
			};
			ui.pushAlpha(a);
			ui.shadow(tx, ty, w, h, 12, 4, t.dark ? 0.55f : 0.16f);
			ui.round(tx, ty, w, h, Theme.R_LG, t.elevated);
			ui.ring(tx, ty, w, h, Theme.R_LG, t.borderStrong);
			ui.round(tx + 8, ty + 6, 12, 12, 6, Colors.withAlpha(color, 0.14f));
			ui.icon(icon, tx + 10, ty + 8, 8, color);
			ui.text(Type.BODY_SEMI, ui.fit(Type.BODY_SEMI, x.title, w - 46), tx + 26, ty + 6.5f, t.text);
			if (x.detail != null) ui.text(Type.LABEL, ui.fit(Type.LABEL, x.detail, w - 34), tx + 26, ty + 18, t.text2);
			if (ui.clicked(hoverId, hovered)) LIST.remove(i);
			ui.popAlpha();
			y -= (h + 6) * a;
		}
	}
}
