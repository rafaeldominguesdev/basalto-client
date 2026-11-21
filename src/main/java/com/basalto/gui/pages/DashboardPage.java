package com.basalto.gui.pages;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.basalto.Basalto;
import com.basalto.gui.App;
import com.basalto.gui.Env;
import com.basalto.gui.ModuleIcons;
import com.basalto.gui.Toasts;
import com.basalto.hud.Activity;
import com.basalto.module.Module;
import com.basalto.ui.Colors;
import com.basalto.ui.Kit;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;

import net.minecraft.client.Minecraft;

/** Visão geral: ambiente, módulos, desempenho, perfil, atividade, favoritos, sessão e avisos. */
public final class DashboardPage extends PageBase {
	private static final String SCROLL = "page:dashboard";

	public DashboardPage(App app) {
		super(app);
	}

	@Override
	public void draw(float x, float y, float w, float h) {
		Theme t = t();
		float off = ui.scroll(SCROLL);
		float px = x + PAD, pw = w - PAD * 2;
		float top = y + 18 - off;

		// Cabeçalho
		String name = cs().streamerMode ? "" : ", " + Env.playerName();
		String sub = Env.kind() == Env.Kind.NONE ? "Nenhum mundo aberto no momento."
			: (cs().streamerMode ? "Mundo oculto" : Env.worldName()) + " · " + Env.dimension() + " · " + Env.worldClock() + " no jogo";
		float bw = kit.buttonWidth("Ver módulos", "blocks", false);
		float after = header(greeting() + name, sub, px, top, pw - bw - 10);
		if (kit.button("dash:modules", "Ver módulos", "blocks", Kit.Btn.PRIMARY, px + pw - bw, top + 4, bw)) app.navigate(App.Page.MODULES);
		float cy = after + 8;

		// Cards de resumo
		int cols = pw >= 620 ? 4 : 2;
		float gap = 10;
		float cwid = (pw - gap * (cols - 1)) / cols;
		float sh = 62;
		for (int i = 0; i < 4; i++) {
			float sx = px + (i % cols) * (cwid + gap);
			float sy = cy + (i / cols) * (sh + gap);
			statCard(i, sx, sy, cwid, sh);
		}
		cy += (4 / cols) * (sh + gap) + 6;

		// Atividade + Atalhos rápidos
		boolean wide = pw >= 620;
		float leftW = wide ? (pw - gap) * 0.6f : pw;
		float rightW = wide ? pw - leftW - gap : pw;
		List<Module> favs = favorites();
		float favH = 34 + Math.max(1, favs.size()) * 24 + (favs.isEmpty() ? 70 : 6);
		float actH = Math.max(150, favH);
		activityCard(px, cy, leftW, wide ? actH : 150);
		if (wide) favoritesCard(px + leftW + gap, cy, rightW, actH, favs);
		else favoritesCard(px, cy + 150 + gap, rightW, favH, favs);
		cy += (wide ? actH : 150 + gap + favH) + gap;

		// Sessão + Avisos
		List<String[]> notices = notices();
		float sessH = 34 + 6 * 18 + 10;
		float noteH = 34 + notices.size() * 38 + 6;
		if (wide) {
			float half = (pw - gap) / 2;
			float rowH = Math.max(sessH, noteH);
			sessionCard(px, cy, half, rowH);
			noticesCard(px + half + gap, cy, half, rowH, notices);
			cy += rowH;
		} else {
			sessionCard(px, cy, pw, sessH);
			cy += sessH + gap;
			noticesCard(px, cy, pw, noteH, notices);
			cy += noteH;
		}
		cy += 20;
		ui.scrollArea(SCROLL, x, y, w, h, cy + off - y);
	}

	private static String greeting() {
		int hour = LocalTime.now().getHour();
		if (hour < 5) return "Boa noite";
		if (hour < 12) return "Bom dia";
		if (hour < 18) return "Boa tarde";
		return "Boa noite";
	}

	private List<Module> favorites() {
		List<Module> out = new ArrayList<>();
		for (String n : cs().favorites) {
			Module m = Basalto.modules().find(n);
			if (m != null) out.add(m);
		}
		return out;
	}

	// ---------------------------------------------------------------- cards de resumo

	private void statCard(int i, float x, float y, float w, float h) {
		Theme t = t();
		String id = "dash:stat" + i;
		boolean hovered = ui.hover(id, x, y, w, h);
		float hv = ui.anim(id, hovered ? 1 : 0, Theme.FAST);
		kit.card(x, y, w, h, hv, 0);
		String icon, label, value, caption;
		int accent = t.accentText;
		Type valueType = Type.METRIC;
		String unit = null;
		switch (i) {
			case 0 -> {
				Env.Kind k = Env.kind();
				icon = k.icon;
				label = "Ambiente";
				value = k.label;
				valueType = Type.HEADING;
				caption = k == Env.Kind.NONE ? "Abra um mundo para usar os módulos"
					: k == Env.Kind.SERVER ? (cs().streamerMode ? "Endereço oculto" : String.valueOf(Env.address()))
					: cs().streamerMode ? "Mundo oculto" : Env.worldName();
				accent = switch (k) {
					case LOCAL -> t.accentText;
					case LAN -> t.info;
					case SERVER -> t.warning;
					case NONE -> t.danger;
				};
			}
			case 1 -> {
				icon = "blocks";
				label = "Módulos ativos";
				value = String.valueOf(app.activeCount());
				unit = "de " + Basalto.modules().all().size();
				int f = cs().favorites.size();
				caption = f == 0 ? "Nenhum favorito" : f == 1 ? "1 favorito" : f + " favoritos";
			}
			case 2 -> {
				icon = "gauge";
				label = "Desempenho";
				int fps = Env.fps();
				value = String.valueOf(fps);
				unit = "FPS";
				float tps = Env.tps();
				int ping = Env.ping();
				caption = tps >= 0 ? String.format("TPS %.1f · %.1f ms/tick", tps, Env.mspt())
					: ping >= 0 ? "Ping " + ping + " ms" : "—";
				accent = fps < 30 ? t.warning : t.accentText;
			}
			default -> {
				icon = "layers";
				label = "Perfil ativo";
				value = cfg().activeProfile;
				valueType = Type.HEADING;
				boolean changed = cfg().activeProfileChanged(app.frame);
				caption = changed ? "Alterações não salvas" : "Tudo salvo";
				accent = changed ? t.warning : t.accentText;
			}
		}
		ui.round(x + 12, y + 12, 16, 16, Theme.R_MD, Colors.withAlpha(accent, 0.12f));
		ui.icon(icon, x + 15.5f, y + 15.5f, 9, accent);
		ui.text(Type.LABEL, label, x + 34, y + 15, t.text2);
		float vy = y + 32;
		String v = ui.fit(valueType, value, w - 24 - (unit == null ? 0 : ui.width(Type.BODY, unit) + 6));
		ui.text(valueType, v, x + 12, valueType == Type.METRIC ? vy - 3 : vy - 1, t.text);
		if (unit != null) ui.text(Type.BODY, unit, x + 12 + ui.width(valueType, v) + 4, vy + 3, t.text2);
		ui.tooltip(id, hovered, caption);
		String cap = ui.fit(Type.CAPTION, caption, w - 24);
		ui.text(Type.CAPTION, cap, x + 12, y + h - 13, i == 3 && caption.startsWith("Alterações") ? t.warning : t.text3);
		if (i == 3 && ui.clicked(id, hovered)) app.navigate(App.Page.PROFILES);
		if (i == 1 && ui.clicked(id, hovered)) app.navigate(App.Page.MODULES);
		if ((i == 1 || i == 3) && hovered) ui.hand();
	}

	// ---------------------------------------------------------------- atividade

	private void activityCard(float x, float y, float w, float h) {
		Theme t = t();
		kit.card(x, y, w, h, 0, 0);
		cardTitle("activity", "Atividade recente", x + 12, y + 11);
		if (!cs().keepActivity) {
			kit.emptyState("eye-off", "Histórico desligado", "Ative em Configurações › Privacidade para ver o que mudou nesta sessão.", x + w / 2, y + 34, w - 40);
			return;
		}
		int max = (int) ((h - 36) / 22);
		var list = Activity.recent(max);
		if (list.isEmpty()) {
			kit.emptyState("history", "Nada por aqui ainda", "Ligar módulos e trocar de perfil aparece nesta lista.", x + w / 2, y + 36, w - 40);
			return;
		}
		float ry = y + 30;
		for (Activity.Entry e : list) {
			app.activityRow(e, x + 6, ry, w - 12, 22);
			ry += 22;
		}
	}

	// ---------------------------------------------------------------- favoritos

	private void favoritesCard(float x, float y, float w, float h, List<Module> favs) {
		Theme t = t();
		kit.card(x, y, w, h, 0, 0);
		cardTitle("star", "Atalhos rápidos", x + 12, y + 11);
		if (favs.isEmpty()) {
			kit.emptyState("star", "Sem favoritos", "Marque a estrela de um módulo para ligar daqui.", x + w / 2, y + 32, w - 32);
			float bw = kit.buttonWidth("Escolher módulos", null, true);
			if (kit.button("dash:favs", "Escolher módulos", null, Kit.Btn.SECONDARY, x + (w - bw) / 2, y + h - 26, bw, 14, true, false))
				app.navigate(App.Page.MODULES);
			return;
		}
		float ry = y + 30;
		for (Module m : favs) {
			boolean on = m.isEnabled();
			ui.round(x + 10, ry + 4, 16, 16, Theme.R_SM, on ? t.accentFill : t.surface2);
			ui.icon(ModuleIcons.of(m.name), x + 14, ry + 8, 8, on ? t.accentText : t.text2);
			ui.textMid(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, m.name, w - 72), x + 32, ry, 24, t.text);
			if (kit.toggle("dash:fav:" + m.name, on, x + w - 12 - Kit.TOGGLE_W, ry + 6)) m.toggle();
			ry += 24;
		}
	}

	// ---------------------------------------------------------------- sessão

	private void sessionCard(float x, float y, float w, float h) {
		Theme t = t();
		kit.card(x, y, w, h, 0, 0);
		cardTitle("map-pin", "Sessão", x + 12, y + 11);
		var mc = Minecraft.getInstance();
		float ry = y + 32, iw = w - 24;
		boolean hidden = cs().streamerMode;
		keyValue("Mundo", hidden ? "Oculto" : Env.worldName(), x + 12, ry, iw, 0);
		ry += 18;
		keyValue("Dimensão", Env.dimension(), x + 12, ry, iw, 0);
		ry += 18;
		String coords = mc.player == null ? "—" : hidden ? "Ocultas" : String.format("%.0f, %.0f, %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
		keyValue("Coordenadas", coords, x + 12, ry, iw, 0);
		ry += 18;
		keyValue("Hora no jogo", Env.worldClock(), x + 12, ry, iw, 0);
		ry += 18;
		long used = Env.memUsedMb(), max = Env.memMaxMb();
		float frac = max > 0 ? used / (float) max : 0;
		keyValue("Memória", used + " / " + max + " MB", x + 12, ry, iw, frac > 0.85f ? t.warning : 0);
		ry += 13;
		ui.round(x + 12, ry, iw, 3, 1.5f, t.dark ? 0xFF1F2B2D : 0xFFDCE5E4);
		ui.round(x + 12, ry, Math.max(3, iw * frac), 3, 1.5f, frac > 0.85f ? t.warning : t.accent);
		ry += 8;
		keyValue("Versão", "Basalto " + Basalto.VERSION + " · Minecraft 26.1.2", x + 12, ry, iw, 0);
	}

	// ---------------------------------------------------------------- avisos

	/** {nível, título, texto, ação} — nível: warn, info, ok. */
	private List<String[]> notices() {
		List<String[]> out = new ArrayList<>();
		if (Env.kind() == Env.Kind.SERVER)
			out.add(new String[] {"warn", "Servidor externo", "Use o Basalto só onde você tem permissão do dono.", null});
		if (cfg().activeProfileChanged(app.frame))
			out.add(new String[] {"info", "Perfil com alterações", "O estado atual difere de \"" + cfg().activeProfile + "\".", "save"});
		long used = Env.memUsedMb(), max = Env.memMaxMb();
		if (max > 0 && used > max * 0.88f)
			out.add(new String[] {"warn", "Memória quase cheia", "O jogo está perto do limite de RAM configurado.", null});
		if (Env.fps() > 0 && Env.fps() < 30 && cs().blur)
			out.add(new String[] {"warn", "FPS baixo", "Desligar o desfoque em Desempenho pode ajudar.", "perf"});
		if (out.isEmpty()) out.add(new String[] {"ok", "Tudo certo", "Nenhum aviso no momento.", null});
		return out;
	}

	private void noticesCard(float x, float y, float w, float h, List<String[]> notices) {
		Theme t = t();
		kit.card(x, y, w, h, 0, 0);
		cardTitle("bell", "Avisos", x + 12, y + 11);
		float ry = y + 30;
		for (String[] n : notices) {
			int color = switch (n[0]) {
				case "warn" -> t.warning;
				case "info" -> t.info;
				default -> t.accentText;
			};
			String icon = switch (n[0]) {
				case "warn" -> "triangle-alert";
				case "info" -> "info";
				default -> "circle-check";
			};
			ui.round(x + 8, ry, w - 16, 32, Theme.R_MD, Colors.withAlpha(color, t.dark ? 0.07f : 0.08f));
			ui.icon(icon, x + 15, ry + 6, 9, color);
			float actW = 0;
			if (n[3] != null) {
				String label = n[3].equals("save") ? "Salvar" : "Abrir";
				actW = kit.buttonWidth(label, null, true);
				if (kit.button("dash:notice:" + n[3], label, null, Kit.Btn.SECONDARY, x + w - 14 - actW, ry + 9, actW, 14, true, false)) {
					if (n[3].equals("save")) {
						cfg().saveProfile(cfg().activeProfile);
						Toasts.success("Perfil salvo", cfg().activeProfile);
						Activity.log(Activity.Kind.PROFILE, "save", "Perfil " + cfg().activeProfile + " salvo");
					} else {
						app.settings.open("desempenho");
						app.navigate(App.Page.SETTINGS);
					}
				}
			}
			ui.text(Type.BODY_SEMI, n[1], x + 29, ry + 4, t.text);
			ui.text(Type.LABEL, ui.fit(Type.LABEL, n[2], w - 50 - actW), x + 29, ry + 16, t.text2);
			ry += 38;
		}
	}
}
