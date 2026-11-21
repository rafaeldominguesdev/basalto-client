package com.basalto.gui.pages;

import java.util.Map;

import com.basalto.config.ClientSettings;
import com.basalto.gui.App;
import com.basalto.ui.Colors;
import com.basalto.ui.Kit;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;

/** Liga e desliga cada elemento do HUD. */
public final class HudPage extends PageBase {
	private static final String SCROLL = "page:hud";

	public HudPage(App app) {
		super(app);
	}

	@Override
	public void draw(float x, float y, float w, float h) {
		Theme t = t();
		float off = ui.scroll(SCROLL);
		float px = x + PAD, pw = w - PAD * 2;
		float top = y + 18 - off;
		int visible = 0;
		for (String id : ClientSettings.HUD_LABELS.keySet()) if (cs().hud(id)) visible++;
		float allW = kit.buttonWidth("Mostrar tudo", "eye", false);
		float cy = header("HUD", visible + " de " + ClientSettings.HUD_LABELS.size() + " elementos visíveis durante o jogo. F1 esconde tudo.", px, top, pw - allW - 10);
		boolean all = visible == ClientSettings.HUD_LABELS.size();
		if (kit.button("hud:all", all ? "Esconder tudo" : "Mostrar tudo", all ? "eye-off" : "eye", Kit.Btn.SECONDARY, px + pw - allW, top + 4, allW)) {
			for (String id : ClientSettings.HUD_LABELS.keySet()) cs().hud.put(id, !all);
			cfg().markDirty();
		}

		float gap = 10;
		int cols = Math.max(1, (int) ((pw + gap) / (240 + gap)));
		float cw = (pw - gap * (cols - 1)) / cols;
		float ch = 50;
		int i = 0;
		for (Map.Entry<String, String[]> e : ClientSettings.HUD_LABELS.entrySet()) {
			float gx = px + (i % cols) * (cw + gap);
			float gy = cy + (i / cols) * (ch + gap);
			item(e.getKey(), e.getValue(), gx, gy, cw, ch);
			i++;
		}
		cy += ((i + cols - 1) / cols) * (ch + gap) + 8;

		// Dica
		ui.round(px, cy, pw, 30, Theme.R_LG, t.infoFill);
		ui.icon("info", px + 10, cy + 10, 10, t.info);
		ui.textMid(Type.BODY, ui.fit(Type.BODY, "Para tirar um módulo da lista do HUD, abra as opções dele em Módulos.", pw - 140), px + 26, cy, 30, t.text);
		float bw = kit.buttonWidth("Ir para Módulos", null, true);
		if (kit.button("hud:mods", "Ir para Módulos", null, Kit.Btn.GHOST, px + pw - 8 - bw, cy + 8, bw, 14, true, false)) app.navigate(App.Page.MODULES);
		cy += 50;
		ui.scrollArea(SCROLL, x, y, w, h, cy + off - y);
	}

	private void item(String id, String[] info, float x, float y, float w, float h) {
		Theme t = t();
		String hid = "hud:" + id;
		boolean on = cs().hud(id);
		boolean hovered = ui.hover(hid, x, y, w, h);
		if (hovered) ui.hand();
		float hv = ui.anim(hid + ":h", hovered ? 1 : 0, Theme.FAST);
		float k = ui.anim(hid + ":on", on ? 1 : 0, Theme.BASE);
		kit.card(x, y, w, h, hv, k * 0.6f);
		ui.round(x + 12, y + 13, 24, 24, Theme.R_MD, Colors.mix(t.surface2, t.accentFill, k));
		ui.icon(info[2], x + 18, y + 19, 12, Colors.mix(t.text2, t.accentText, k));
		ui.text(Type.BODY_SEMI, info[0], x + 44, y + 12, t.text);
		ui.text(Type.LABEL, ui.fit(Type.LABEL, info[1], w - 84), x + 44, y + 25, t.text2);
		boolean flip = kit.toggle(hid + ":tg", on, x + w - 12 - Kit.TOGGLE_W, y + (h - Kit.TOGGLE_H) / 2);
		if (flip || ui.clicked(hid, hovered)) {
			if (!flip) kit.click();
			cs().hud.put(id, !on);
			cfg().markDirty();
		}
	}
}
