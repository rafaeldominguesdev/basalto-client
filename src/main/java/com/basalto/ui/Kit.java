package com.basalto.ui;

import org.lwjgl.glfw.GLFW;

import com.basalto.util.Keys;

import net.minecraft.client.Minecraft;

/**
 * Componentes reutilizáveis (botão, toggle, slider, segmentado, dropdown,
 * campo de texto, captura de tecla, badges e cards). Todos seguem os tokens
 * do {@link Theme} e têm hover, pressionado, desativado e foco de teclado.
 */
public final class Kit {
	public enum Btn { PRIMARY, SECONDARY, GHOST, DANGER }

	public static final int KEEP = -2;

	private final Ui ui;
	/** Campo de captura de tecla ativo. */
	public String capturing;
	/** Dropdown aberto. */
	public String openDropdown;
	private Runnable clickSound = () -> {};

	public Kit(Ui ui) {
		this.ui = ui;
	}

	public void setClickSound(Runnable r) {
		clickSound = r;
	}

	private Theme t() {
		return ui.t;
	}

	public void click() {
		clickSound.run();
	}

	// ================================================================ botões

	public float buttonWidth(String label, String icon, boolean small) {
		float pad = small ? 8 : 10;
		float w = pad * 2 + (label == null ? 0 : ui.width(Type.BODY_SEMI, label));
		if (icon != null) w += (label == null || label.isEmpty() ? 0 : 5) + (small ? 8 : 9);
		return w;
	}

	public boolean button(String id, String label, String icon, Btn kind, float x, float y, float w) {
		return button(id, label, icon, kind, x, y, w, Theme.CONTROL_H, true, false);
	}

	public boolean button(String id, String label, String icon, Btn kind, float x, float y, float w, float h, boolean enabled, boolean loading) {
		Theme t = t();
		boolean small = h < Theme.CONTROL_H;
		if (w <= 0) w = buttonWidth(label, icon, small);
		boolean hovered = ui.hover(id, x, y, w, h) && enabled && !loading;
		boolean focused = ui.focusable(id);
		boolean pressing = ui.isPressing(id, hovered);
		if (hovered) ui.hand();
		float hv = ui.anim("btn:" + id, hovered ? 1 : 0, Theme.FAST);

		int bg, fg, border = 0;
		switch (kind) {
			case PRIMARY -> {
				bg = Colors.mix(t.accent, t.dark ? 0xFFFFFFFF : 0xFF000000, 0.10f * hv);
				if (pressing) bg = t.accent2;
				fg = t.onAccent;
			}
			case SECONDARY -> {
				bg = Colors.mix(t.dark ? 0x0DFFFFFF : 0x080B2B26, t.dark ? 0x1AFFFFFF : 0x120B2B26, hv);
				if (pressing) bg = t.pressed;
				border = Colors.mix(t.border, t.borderStrong, hv);
				fg = t.text;
			}
			case DANGER -> {
				bg = Colors.mix(t.dangerFill, Colors.withAlpha(t.danger, 0.24f), hv);
				if (pressing) bg = Colors.withAlpha(t.danger, 0.32f);
				border = Colors.withAlpha(t.danger, 0.30f);
				fg = t.danger;
			}
			default -> {
				bg = Colors.fade(t.hover, hv * 1.4f);
				if (pressing) bg = t.pressed;
				fg = Colors.mix(t.text2, t.text, hv);
			}
		}
		if (!enabled) ui.pushAlpha(0.42f);
		float r = Theme.R_MD;
		if (kind == Btn.PRIMARY && enabled) ui.glow(x, y, w, h, 6, 0.10f + 0.10f * hv);
		ui.round(x, y, w, h, r, bg);
		if (border != 0) ui.ring(x, y, w, h, r, border);
		float is = small ? 8 : 9;
		if (loading) {
			drawDots(x + w / 2, y + h / 2, fg);
		} else {
			float cw = (label == null ? 0 : ui.width(Type.BODY_SEMI, label)) + (icon != null ? is + (label == null || label.isEmpty() ? 0 : 5) : 0);
			float cx = x + (w - cw) / 2;
			if (icon != null) {
				ui.icon(icon, cx, y + (h - is) / 2, is, fg);
				cx += is + 5;
			}
			if (label != null) ui.textMid(Type.BODY_SEMI, label, cx, y, h, fg);
		}
		if (!enabled) ui.popAlpha();
		if (focused && ui.keyboardNav) ui.focusRing(x, y, w, h, r);
		boolean go = enabled && !loading && (ui.clicked(id, hovered) || ui.keyActivated(id));
		if (go) click();
		return go;
	}

	private void drawDots(float cx, float cy, int color) {
		long now = System.currentTimeMillis();
		for (int i = 0; i < 3; i++) {
			float ph = ((now + i * 160) % 900) / 900f;
			float a = 0.35f + 0.65f * (float) Math.max(0, Math.sin(ph * Math.PI));
			ui.round(cx - 7 + i * 5, cy - 1.5f, 3, 3, 1.5f, Colors.fade(color, a));
		}
	}

	/** Botão só com ícone. active = estado ligado (ex.: painel aberto). */
	public boolean iconButton(String id, String icon, float x, float y, float size, String tooltip, boolean active) {
		Theme t = t();
		boolean hovered = ui.hover(id, x, y, size, size);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float hv = ui.anim("ib:" + id, hovered || active ? 1 : 0, Theme.FAST);
		boolean pressing = ui.isPressing(id, hovered);
		ui.round(x, y, size, size, Theme.R_MD, pressing ? t.pressed : Colors.fade(t.hover, hv * 1.5f));
		float is = Math.round(size * 0.56f);
		ui.icon(icon, x + (size - is) / 2, y + (size - is) / 2, is, active ? t.accentText : Colors.mix(t.text2, t.text, hv));
		if (focused && ui.keyboardNav) ui.focusRing(x, y, size, size, Theme.R_MD);
		if (tooltip != null) ui.tooltip(id, hovered, tooltip);
		boolean go = ui.clicked(id, hovered) || ui.keyActivated(id);
		if (go) click();
		return go;
	}

	// ================================================================ toggle, checkbox

	public static final float TOGGLE_W = 22, TOGGLE_H = 12;

	/** Toggle arredondado. Devolve true quando clicado (quem chama inverte o valor). */
	public boolean toggle(String id, boolean on, float x, float y) {
		Theme t = t();
		boolean hovered = ui.hover(id, x - 2, y - 2, TOGGLE_W + 4, TOGGLE_H + 4);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float k = ui.anim("tg:" + id, on ? 1 : 0, Theme.BASE);
		float hv = ui.anim("tgh:" + id, hovered ? 1 : 0, Theme.FAST);
		int off = t.dark ? Colors.mix(0xFF2A3638, 0xFF334245, hv) : Colors.mix(0xFFCBD6D5, 0xFFBCC9C8, hv);
		int track = Colors.mix(off, t.accent, k);
		if (k > 0.01f) ui.glow(x, y, TOGGLE_W, TOGGLE_H, 5, 0.16f * k);
		ui.round(x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2, track);
		float ks = TOGGLE_H - 4;
		float kx = x + 2 + (TOGGLE_W - 4 - ks) * k;
		ui.shadow(kx, y + 2, ks, ks, 2, 0.5f, 0.25f);
		ui.round(kx, y + 2, ks, ks, ks / 2, Colors.mix(t.dark ? 0xFFB7C4C3 : 0xFFFFFFFF, t.dark ? 0xFF04201A : 0xFFFFFFFF, k));
		if (focused && ui.keyboardNav) ui.focusRing(x, y, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2);
		boolean go = ui.clicked(id, hovered) || ui.keyActivated(id);
		if (go) click();
		return go;
	}

	public boolean checkbox(String id, boolean on, float x, float y) {
		Theme t = t();
		float s = 10;
		boolean hovered = ui.hover(id, x - 2, y - 2, s + 4, s + 4);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float k = ui.anim("cb:" + id, on ? 1 : 0, Theme.FAST);
		ui.round(x, y, s, s, 3, Colors.mix(t.field, t.accent, k));
		ui.ring(x, y, s, s, 3, Colors.mix(hovered ? t.borderStrong : t.border, 0x00000000, k));
		if (k > 0.05f) ui.icon("check", x + 1.5f, y + 1.5f, s - 3, Colors.fade(t.onAccent, k));
		if (focused && ui.keyboardNav) ui.focusRing(x, y, s, s, 3);
		boolean go = ui.clicked(id, hovered) || ui.keyActivated(id);
		if (go) click();
		return go;
	}

	// ================================================================ slider

	/** Slider arredondado. Devolve a nova fração (0 a 1). steps > 0 arredonda para passos. */
	public float slider(String id, float frac, float x, float y, float w, int steps) {
		Theme t = t();
		float h = 12;
		boolean hovered = ui.hover(id, x - 4, y, w + 8, h);
		boolean focused = ui.focusable(id);
		if (hovered || ui.holding(id)) ui.hand();
		ui.clicked(id, hovered);
		float out = frac;
		if (ui.holding(id)) out = Ui.clamp((ui.mx - x) / w, 0, 1);
		if (focused) {
			float step = steps > 0 ? 1f / steps : 0.05f;
			if (ui.consumeKey(GLFW.GLFW_KEY_RIGHT) || ui.consumeKey(GLFW.GLFW_KEY_UP)) out = Math.min(1, out + step);
			if (ui.consumeKey(GLFW.GLFW_KEY_LEFT) || ui.consumeKey(GLFW.GLFW_KEY_DOWN)) out = Math.max(0, out - step);
		}
		boolean touched = out != frac;
		if (steps > 0 && touched) out = Math.round(out * steps) / (float) steps;
		float shown = ui.anim("sl:" + id, out, ui.holding(id) ? 0 : Theme.FAST);
		float ty = y + h / 2 - 2;
		ui.round(x, ty, w, 4, 2, t.dark ? 0xFF243032 : 0xFFD5DEDD);
		ui.round(x, ty, Math.max(4, w * shown), 4, 2, t.accent);
		float hv = ui.anim("slh:" + id, hovered || ui.holding(id) ? 1 : 0, Theme.FAST);
		float ks = 9 + hv;
		float kx = x + w * shown - ks / 2;
		float ky = y + (h - ks) / 2;
		if (hv > 0) ui.glow(kx, ky, ks, ks, 4, 0.22f * hv);
		ui.shadow(kx, ky, ks, ks, 3, 0.5f, 0.3f);
		ui.round(kx, ky, ks, ks, ks / 2, t.dark ? 0xFFF2F7F6 : 0xFFFFFFFF);
		ui.round(kx + ks / 2 - 1.5f, ky + ks / 2 - 1.5f, 3, 3, 1.5f, t.accent2);
		if (focused && ui.keyboardNav) ui.focusRing(kx, ky, ks, ks, ks / 2);
		if (ui.released[0] && id.equals(ui.active)) click();
		return out;
	}

	// ================================================================ segmentado

	/** Botões segmentados de larguras iguais. Devolve o índice escolhido. */
	public int segmented(String id, String[] opts, int sel, float x, float y, float w) {
		Theme t = t();
		float h = Theme.CONTROL_H;
		ui.round(x, y, w, h, Theme.R_MD, t.dark ? 0xFF0E1517 : 0xFFE6EDEC);
		ui.ring(x, y, w, h, Theme.R_MD, t.border);
		float cw = (w - 4) / opts.length;
		float pos = ui.anim("seg:" + id, sel, Theme.BASE);
		float px = x + 2 + cw * pos;
		ui.shadow(px, y + 2, cw, h - 4, 3, 1, t.dark ? 0.35f : 0.10f);
		ui.round(px, y + 2, cw, h - 4, Theme.R_SM, t.dark ? t.elevated : 0xFFFFFFFF);
		ui.ring(px, y + 2, cw, h - 4, Theme.R_SM, t.dark ? t.borderStrong : t.border);
		int out = sel;
		for (int i = 0; i < opts.length; i++) {
			String oid = id + ":" + i;
			float ox = x + 2 + cw * i;
			boolean hovered = ui.hover(oid, ox, y, cw, h);
			boolean focused = ui.focusable(oid);
			if (hovered && i != sel) ui.hand();
			int fg = i == sel ? t.text : hovered ? Colors.mix(t.text2, t.text, 0.5f) : t.text2;
			String label = ui.fit(Type.BODY_MEDIUM, opts[i], cw - 6);
			ui.textMid(Type.BODY_MEDIUM, label, ox + (cw - ui.width(Type.BODY_MEDIUM, label)) / 2, y, h, fg);
			if (focused && ui.keyboardNav) ui.focusRing(ox, y + 2, cw, h - 4, Theme.R_SM);
			if ((ui.clicked(oid, hovered) || ui.keyActivated(oid)) && i != sel) {
				out = i;
				click();
			}
			if (focused) {
				if (ui.consumeKey(GLFW.GLFW_KEY_RIGHT)) out = Math.min(opts.length - 1, sel + 1);
				if (ui.consumeKey(GLFW.GLFW_KEY_LEFT)) out = Math.max(0, sel - 1);
			}
		}
		return out;
	}

	// ================================================================ dropdown

	/** Seletor compacto com lista suspensa. Devolve o índice escolhido. */
	public int dropdown(String id, String[] opts, int sel, float x, float y, float w) {
		Theme t = t();
		float h = Theme.CONTROL_H;
		boolean open = id.equals(openDropdown);
		boolean hovered = ui.hover(id, x, y, w, h);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float hv = ui.anim("dd:" + id, hovered || open ? 1 : 0, Theme.FAST);
		ui.round(x, y, w, h, Theme.R_MD, t.field);
		ui.ring(x, y, w, h, Theme.R_MD, open ? t.accentBorder : Colors.mix(t.border, t.borderStrong, hv));
		ui.textMid(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, opts[sel], w - 24), x + 7, y, h, t.text);
		float rot = ui.anim("ddr:" + id, open ? 1 : 0, Theme.FAST);
		ui.icon(rot > 0.5f ? "chevron-up" : "chevron-down", x + w - 14, y + (h - 8) / 2, 8, t.text2);
		if (focused && ui.keyboardNav) ui.focusRing(x, y, w, h, Theme.R_MD);
		if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
			openDropdown = open ? null : id;
			click();
		}
		final int[] result = {sel};
		if (id.equals(openDropdown)) {
			float rowH = 15;
			float lh = opts.length * rowH + 4;
			float ly = y + h + 3;
			if (ly + lh > ui.H - 6) ly = y - lh - 3;
			final float fly = ly;
			final float fw = Math.max(w, 80);
			if (focused) {
				if (ui.consumeKey(GLFW.GLFW_KEY_DOWN)) result[0] = Math.min(opts.length - 1, sel + 1);
				if (ui.consumeKey(GLFW.GLFW_KEY_UP)) result[0] = Math.max(0, sel - 1);
				if (ui.consumeKey(GLFW.GLFW_KEY_ESCAPE)) openDropdown = null;
			}
			ui.overlay(() -> {
				float a = ui.animFrom("ddo:" + id, 0, 1, Theme.FAST);
				ui.pushAlpha(a);
				float oy = fly - 3 * (1 - a) * Anim.move();
				ui.block(id + ":list", x, oy, fw, lh);
				ui.shadow(x, oy, fw, lh, 10, 3, t.dark ? 0.55f : 0.16f);
				ui.round(x, oy, fw, lh, Theme.R_MD, t.elevated);
				ui.ring(x, oy, fw, lh, Theme.R_MD, t.borderStrong);
				for (int i = 0; i < opts.length; i++) {
					String oid = id + ":o" + i;
					float ry = oy + 2 + i * rowH;
					boolean hv2 = ui.hover(oid, x + 2, ry, fw - 4, rowH);
					if (hv2) ui.hand();
					if (hv2) ui.round(x + 2, ry, fw - 4, rowH, Theme.R_SM, t.hover);
					ui.textMid(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, opts[i], fw - 26), x + 8, ry, rowH, i == sel ? t.text : t.text2);
					if (i == sel) ui.icon("check", x + fw - 16, ry + (rowH - 8) / 2, 8, t.accentText);
					if (ui.clicked(oid, hv2)) {
						result[0] = i;
						openDropdown = null;
						click();
					}
				}
				if (ui.pressedOutside(x, oy, fw, lh) && !ui.mouseIn(x, y, w, h)) openDropdown = null;
				ui.popAlpha();
			});
		}
		return result[0];
	}

	// ================================================================ campo de texto

	public enum FieldResult { NONE, CHANGED, SUBMITTED }

	/** Campo de texto de uma linha. Digita quando tem foco; Enter envia. */
	public FieldResult textField(String id, StringBuilder text, String placeholder, String icon, float x, float y, float w, int maxLen) {
		Theme t = t();
		float h = Theme.CONTROL_H + 2;
		boolean hovered = ui.hover(id, x, y, w, h);
		if (hovered) ui.ibeam();
		if (ui.clicked(id, hovered) || (hovered && ui.pressed[0])) ui.focus = id;
		boolean focused = ui.focusable(id);
		float f = ui.anim("tf:" + id, focused ? 1 : 0, Theme.FAST);
		ui.round(x, y, w, h, Theme.R_MD, t.field);
		ui.ring(x, y, w, h, Theme.R_MD, Colors.mix(hovered ? t.borderStrong : t.border, t.accentBorder, f));
		if (f > 0.01f) ui.glow(x, y, w, h, 4, 0.10f * f);
		float tx = x + 7;
		if (icon != null) {
			ui.icon(icon, tx, y + (h - 9) / 2, 9, focused ? t.accentText : t.text3);
			tx += 14;
		}
		FieldResult res = FieldResult.NONE;
		if (focused) {
			String typed = ui.takeChars();
			if (ui.consumeShortcut(GLFW.GLFW_KEY_V)) typed += Minecraft.getInstance().keyboardHandler.getClipboard().replaceAll("[\\r\\n]", "");
			if (ui.consumeShortcut(GLFW.GLFW_KEY_A) || ui.consumeShortcut(GLFW.GLFW_KEY_BACKSPACE)) {
				text.setLength(0);
				res = FieldResult.CHANGED;
			}
			if (!typed.isEmpty()) {
				for (int i = 0; i < typed.length() && text.length() < maxLen; i++) {
					char c = typed.charAt(i);
					if (c >= ' ') text.append(c);
				}
				res = FieldResult.CHANGED;
			}
			if (ui.consumeKey(GLFW.GLFW_KEY_BACKSPACE) && !text.isEmpty()) {
				text.setLength(text.length() - 1);
				res = FieldResult.CHANGED;
			}
			if (ui.consumeKey(GLFW.GLFW_KEY_ENTER) || ui.consumeKey(GLFW.GLFW_KEY_KP_ENTER)) res = FieldResult.SUBMITTED;
		}
		float avail = x + w - tx - 8;
		ui.pushClip(tx, y, avail + 2, h);
		if (text.isEmpty()) {
			ui.textMid(Type.BODY, placeholder, tx, y, h, t.text3);
		} else {
			String s = text.toString();
			float sw = ui.width(Type.BODY, s);
			float off = Math.max(0, sw - avail + 2);
			ui.textMid(Type.BODY, s, tx - off, y, h, t.text);
			tx = tx - off + sw;
		}
		if (focused && (System.currentTimeMillis() / 530) % 2 == 0)
			ui.rect(text.isEmpty() ? tx : tx + 0.5f, y + 4, 1f / ui.p * 1.5f, h - 8, t.accentText);
		ui.popClip();
		return res;
	}

	// ================================================================ captura de tecla

	/** Mostra a tecla e entra em captura ao clicar. Devolve a nova tecla, -1 para nenhuma ou KEEP. */
	public int keybind(String id, int key, float x, float y, float w) {
		Theme t = t();
		float h = Theme.CONTROL_H;
		boolean cap = id.equals(capturing);
		boolean hovered = ui.hover(id, x, y, w, h);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float hv = ui.anim("kb:" + id, hovered || cap ? 1 : 0, Theme.FAST);
		ui.round(x, y, w, h, Theme.R_MD, cap ? t.accentFill : t.field);
		ui.ring(x, y, w, h, Theme.R_MD, cap ? t.accentBorder : Colors.mix(t.border, t.borderStrong, hv));
		ui.icon("keyboard", x + 6, y + (h - 9) / 2, 9, cap ? t.accentText : t.text3);
		String label = cap ? "Aperte uma tecla…" : key < 0 ? "Sem tecla" : Keys.name(key);
		ui.textMid(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, label, w - 24), x + 19, y, h, cap ? t.accentText : key < 0 ? t.text3 : t.text);
		if (focused && ui.keyboardNav) ui.focusRing(x, y, w, h, Theme.R_MD);
		if (cap) {
			int k = ui.takeAnyKey();
			if (k >= 0) {
				capturing = null;
				click();
				if (k == GLFW.GLFW_KEY_ESCAPE) return KEEP;
				if (k == GLFW.GLFW_KEY_BACKSPACE || k == GLFW.GLFW_KEY_DELETE) return -1;
				return k;
			}
			if (ui.pressedOutside(x, y, w, h)) capturing = null;
			return KEEP;
		}
		if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
			capturing = id;
			click();
		}
		return KEEP;
	}

	// ================================================================ pequenos

	public float badgeWidth(String text) {
		return ui.width(Type.CAPTION, text) + 10;
	}

	/** Etiqueta em pílula. */
	public float badge(String text, int fg, int bg, float x, float y) {
		float w = badgeWidth(text);
		ui.round(x, y, w, 10, 5, bg);
		ui.textMid(Type.CAPTION, text, x + 5, y, 10, fg);
		return w;
	}

	/** Badge com ponto de status na frente. */
	public float statusBadge(String text, int color, float x, float y) {
		float w = ui.width(Type.LABEL, text) + 20;
		ui.round(x, y, w, 14, 7, Colors.withAlpha(color, t().dark ? 0.12f : 0.14f));
		ui.ring(x, y, w, 14, 7, Colors.withAlpha(color, 0.25f));
		ui.round(x + 6, y + 5, 4, 4, 2, color);
		ui.textMid(Type.LABEL, text, x + 13, y, 14, t().dark ? Colors.mix(color, 0xFFFFFFFF, 0.25f) : Colors.mix(color, 0xFF000000, 0.35f));
		return w;
	}

	/** Superfície de card: sombra, fundo e borda; destaque opcional. */
	public void card(float x, float y, float w, float h, float hover, float activeK) {
		Theme t = t();
		ui.shadow(x, y, w, h, 8, 2 + hover, t.dark ? 0.28f + 0.12f * hover : 0.06f + 0.04f * hover);
		if (activeK > 0.01f) ui.glow(x, y, w, h, 8, 0.07f * activeK);
		ui.round(x, y, w, h, Theme.R_LG, Colors.mix(t.surface, t.surface2, hover * 0.6f));
		int border = Colors.mix(t.border, t.borderStrong, hover);
		ui.ring(x, y, w, h, Theme.R_LG, Colors.mix(border, t.accentBorder, activeK));
	}

	/** Bolinhas de cor (cor de destaque). Devolve o índice escolhido. */
	public int swatches(String id, int[] colors, String[] names, int selected, float x, float y) {
		Theme t = t();
		int out = selected;
		float s = 16, gap = 6;
		for (int i = 0; i < colors.length; i++) {
			String sid = id + ":" + i;
			float sx = x + i * (s + gap);
			boolean hovered = ui.hover(sid, sx - 2, y - 2, s + 4, s + 4);
			boolean focused = ui.focusable(sid);
			if (hovered) ui.hand();
			boolean on = i == selected;
			float k = ui.anim("sw:" + sid, on ? 1 : hovered ? 0.5f : 0, Theme.FAST);
			if (k > 0) ui.ring(sx - 2.5f, y - 2.5f, s + 5, s + 5, (s + 5) / 2, Colors.fade(t.text, k * 0.85f));
			ui.round(sx, y, s, s, s / 2, colors[i]);
			if (on) ui.icon("check", sx + 4, y + 4, s - 8, 0xFF04201A);
			ui.tooltip(sid, hovered, names[i]);
			if (focused && ui.keyboardNav) ui.focusRing(sx, y, s, s, s / 2);
			if ((ui.clicked(sid, hovered) || ui.keyActivated(sid)) && !on) {
				out = i;
				click();
			}
		}
		return out;
	}

	/** Estado vazio: ícone num círculo, título e texto de apoio, centralizados. */
	public void emptyState(String icon, String title, String body, float cx, float y, float maxW) {
		Theme t = t();
		ui.round(cx - 16, y, 32, 32, 16, t.surface2);
		ui.ring(cx - 16, y, 32, 32, 16, t.border);
		ui.icon(icon, cx - 7, y + 9, 14, t.text2);
		ui.textCenter(Type.TITLE, title, cx, y + 40, t.text);
		float ly = y + 54;
		for (String line : ui.wrap(Type.BODY, body, maxW, 3)) {
			ui.textCenter(Type.BODY, line, cx, ly, t.text2);
			ly += Type.BODY.lineHeight;
		}
	}
}
