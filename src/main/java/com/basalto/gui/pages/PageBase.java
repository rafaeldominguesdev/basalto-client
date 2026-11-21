package com.basalto.gui.pages;

import com.basalto.config.ClientSettings;
import com.basalto.config.ConfigManager;
import com.basalto.gui.App;
import com.basalto.ui.Colors;
import com.basalto.ui.Kit;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;
import com.basalto.ui.Ui;

/** Base das páginas: atalhos para o App e peças repetidas (cabeçalho, seção, linha de opção). */
public abstract class PageBase {
	protected final App app;
	protected final Ui ui;
	protected final Kit kit;
	public static final float PAD = 20;

	protected PageBase(App app) {
		this.app = app;
		this.ui = app.ui;
		this.kit = app.kit;
	}

	public abstract void draw(float x, float y, float w, float h);

	protected Theme t() {
		return ui.t;
	}

	protected ClientSettings cs() {
		return app.cs();
	}

	protected ConfigManager cfg() {
		return app.cfg();
	}

	/** Título da página e texto de apoio. Devolve o y logo abaixo. */
	protected float header(String title, String subtitle, float x, float y, float w) {
		Theme t = t();
		ui.text(Type.HEADING, title, x, y, t.text);
		if (subtitle != null) ui.text(Type.BODY, ui.fit(Type.BODY, subtitle, w), x, y + 17, t.text2);
		return y + (subtitle != null ? 36 : 22);
	}

	/** Título de seção pequeno, com texto de apoio opcional. Devolve o y abaixo. */
	protected float section(String title, String subtitle, float x, float y, float w) {
		Theme t = t();
		ui.text(Type.TITLE, title, x, y, t.text);
		if (subtitle == null) return y + 18;
		ui.text(Type.LABEL, ui.fit(Type.LABEL, subtitle, w), x, y + 13, t.text2);
		return y + 30;
	}

	/** Título de card com ícone. */
	protected void cardTitle(String icon, String title, float x, float y) {
		Theme t = t();
		if (icon != null) {
			ui.icon(icon, x, y + 1.5f, 9, t.text2);
			x += 14;
		}
		ui.text(Type.BODY_SEMI, title, x, y, t.text);
	}

	/**
	 * Linha de configuração: título, descrição e espaço para o controle à direita.
	 * Devolve a altura usada. O controle é desenhado por quem chama em (x + w - controlW, y + mid).
	 */
	protected float settingRow(String title, String desc, float x, float y, float w, float controlW, boolean showReset, Runnable reset, String id) {
		Theme t = t();
		float textW = w - controlW - 40;
		java.util.List<String> lines = desc == null ? java.util.List.of() : ui.wrap(Type.LABEL, desc, textW, 2);
		float h = Math.max(34, 12 + Type.BODY_SEMI.lineHeight + lines.size() * Type.LABEL.lineHeight + 4);
		ui.text(Type.BODY_SEMI, title, x, y + 8, t.text);
		float ly = y + 8 + Type.BODY_SEMI.lineHeight + 1;
		for (String l : lines) {
			ui.text(Type.LABEL, l, x, ly, t.text2);
			ly += Type.LABEL.lineHeight;
		}
		if (showReset && reset != null) {
			float rx = x + w - controlW - 24;
			if (kit.iconButton("reset:" + id, "rotate-ccw", rx, y + (h - 16) / 2, 16, "Restaurar padrão", false)) reset.run();
		}
		return h;
	}

	protected void divider(float x, float y, float w) {
		ui.hline(x, y, w, t().divider);
	}

	/** Pequeno rótulo + valor (linhas de "Sessão", "Sobre"). */
	protected void keyValue(String key, String value, float x, float y, float w, int valueColor) {
		Theme t = t();
		ui.text(Type.BODY, key, x, y, t.text2);
		ui.textRight(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, value, w * 0.6f), x + w, y, valueColor == 0 ? t.text : valueColor);
	}

	protected static int alphaOf(int color, float a) {
		return Colors.withAlpha(color, a);
	}
}
