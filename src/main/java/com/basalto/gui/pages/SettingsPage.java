package com.basalto.gui.pages;

import java.util.ArrayList;
import java.util.List;

import com.basalto.Basalto;
import com.basalto.config.ClientSettings;
import com.basalto.gui.App;
import com.basalto.gui.Modal;
import com.basalto.gui.ModuleIcons;
import com.basalto.gui.Toasts;
import com.basalto.hud.Activity;
import com.basalto.module.Module;
import com.basalto.ui.Anim;
import com.basalto.ui.Colors;
import com.basalto.ui.Kit;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;
import com.basalto.util.Keys;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;

/** Configurações em abas: geral, aparência, interface, desempenho, áudio, atalhos, privacidade e sobre. */
public final class SettingsPage extends PageBase {
	private record Tab(String id, String title, String icon, String desc) {}

	private static final Tab[] TABS = {
		new Tab("geral", "Geral", "settings", "Comportamento do menu ao abrir."),
		new Tab("aparencia", "Aparência", "palette", "Tema, cor de destaque e transparência."),
		new Tab("interface", "Interface", "scaling", "Tamanho e organização da interface."),
		new Tab("desempenho", "Desempenho", "zap", "Efeitos que custam FPS."),
		new Tab("audio", "Áudio", "volume-2", "Sons de clique dos controles."),
		new Tab("atalhos", "Controles e atalhos", "keyboard", "Tecla do menu e a tecla de cada módulo."),
		new Tab("privacidade", "Privacidade", "lock", "O que aparece na tela e o que fica guardado."),
		new Tab("sobre", "Sobre", "info", "Versões, arquivos e créditos."),
	};
	private static String current = "geral";
	private static final ClientSettings DEFAULTS = new ClientSettings();

	public SettingsPage(App app) {
		super(app);
	}

	public void open(String tab) {
		current = tab;
	}

	public String currentTitle() {
		for (Tab t : TABS) if (t.id.equals(current)) return t.title;
		return null;
	}

	@Override
	public void draw(float x, float y, float w, float h) {
		Theme t = t();
		boolean compact = w < 560;
		float navW = compact ? 44 : 156;

		// Navegação interna
		float nx = x + 12, ny = y + 18;
		if (!compact) {
			ui.text(Type.CAPTION, "CONFIGURAÇÕES", nx + 8, ny, t.text3);
			ny += 14;
		}
		for (Tab tab : TABS) {
			navTab(tab, nx, ny, navW - 16, compact);
			ny += 22;
		}
		ny += 6;
		ui.hline(nx + 4, ny, navW - 24, t.divider);
		ny += 8;
		// Atalho para a página de perfis
		String pid = "set:tab:perfis";
		boolean hv = ui.hover(pid, nx, ny, navW - 16, 22);
		boolean focused = ui.focusable(pid);
		if (hv) ui.hand();
		if (hv) ui.round(nx, ny, navW - 16, 22, Theme.R_MD, t.hover);
		ui.icon("layers", compact ? nx + (navW - 16 - 9) / 2 : nx + 8, ny + 6.5f, 9, t.text2);
		if (!compact) {
			ui.textMid(Type.BODY_MEDIUM, "Perfis", nx + 23, ny, 22, t.text2);
			ui.icon("external-link", nx + navW - 30, ny + 7.5f, 7, t.text3);
		}
		if (focused && ui.keyboardNav) ui.focusRing(nx, ny, navW - 16, 22, Theme.R_MD);
		if (compact) ui.tooltip(pid, hv, "Perfis");
		if (ui.clicked(pid, hv) || ui.keyActivated(pid)) app.navigate(App.Page.PROFILES);

		ui.vline(x + navW, y + 14, h - 28, t.divider);

		// Conteúdo da aba
		float cx = x + navW, cw = w - navW;
		String sid = "page:settings:" + current;
		float off = ui.scroll(sid);
		float px = cx + PAD, pw = Math.min(cw - PAD * 2, 560);
		float a = ui.animFrom("settab:" + current, 0, 1, Theme.BASE);
		ui.pushClip(cx, y, cw, h);
		ui.pushAlpha(a);
		float top = y + 18 - off + (1 - a) * 4 * Anim.move();
		Tab tab = tab();
		float cy = header(tab.title, tab.desc, px, top, pw);
		cy = switch (current) {
			case "aparencia" -> appearance(px, cy, pw);
			case "interface" -> interfaceTab(px, cy, pw);
			case "desempenho" -> performance(px, cy, pw);
			case "audio" -> audio(px, cy, pw);
			case "atalhos" -> shortcuts(px, cy, pw);
			case "privacidade" -> privacy(px, cy, pw);
			case "sobre" -> about(px, cy, pw);
			default -> general(px, cy, pw);
		};
		ui.popAlpha();
		ui.popClip();
		ui.scrollArea(sid, cx, y, cw, h, cy + 20 + off - y);
	}

	private Tab tab() {
		for (Tab t : TABS) if (t.id.equals(current)) return t;
		return TABS[0];
	}

	private void navTab(Tab tab, float x, float y, float w, boolean compact) {
		Theme t = t();
		String id = "set:tab:" + tab.id;
		boolean on = tab.id.equals(current);
		boolean hovered = ui.hover(id, x, y, w, 22);
		boolean focused = ui.focusable(id);
		if (hovered && !on) ui.hand();
		float hv = ui.anim(id, hovered ? 1 : 0, Theme.FAST);
		float k = ui.anim(id + ":on", on ? 1 : 0, Theme.BASE);
		if (k > 0.01f) ui.round(x, y, w, 22, Theme.R_MD, Colors.fade(t.dark ? 0x12FFFFFF : 0x0E0B2B26, k));
		else if (hv > 0.01f) ui.round(x, y, w, 22, Theme.R_MD, Colors.fade(t.hover, hv * 1.4f));
		int fg = on ? t.text : Colors.mix(t.text2, t.text, hv);
		ui.icon(tab.icon, compact ? x + (w - 9) / 2 : x + 8, y + 6.5f, 9, on ? t.accentText : fg);
		if (!compact) ui.textMid(on ? Type.BODY_SEMI : Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, tab.title, w - 30), x + 23, y, 22, fg);
		if (focused && ui.keyboardNav) ui.focusRing(x, y, w, 22, Theme.R_MD);
		if (compact) ui.tooltip(id, hovered, tab.title);
		if ((ui.clicked(id, hovered) || ui.keyActivated(id)) && !on) {
			kit.click();
			current = tab.id;
			Anim.reset("settab:" + current);
		}
	}

	// ================================================================ peças

	/** Grupo de linhas dentro de um card. Mede no frame anterior para desenhar o fundo antes. */
	private float groupStart(String id, float x, float y, float w) {
		float h = ui.anim("grp:" + id, lastGroupH.getOrDefault(id, 40f), 0);
		kit.card(x, y, w, h, 0, 0);
		return y + 4;
	}

	private final java.util.Map<String, Float> lastGroupH = new java.util.HashMap<>();

	private float groupEnd(String id, float startY, float endY) {
		lastGroupH.put(id, endY - startY + 4);
		return endY + 4 + 12;
	}

	/** Linha com toggle. */
	private float toggleRow(String id, String title, String desc, boolean value, boolean def, float x, float y, float w, Runnable flip) {
		float h = settingRow(title, desc, x + 12, y, w - 24, Kit.TOGGLE_W, value != def, flip, id);
		if (kit.toggle("set:" + id, value, x + w - 12 - Kit.TOGGLE_W, y + (h - Kit.TOGGLE_H) / 2)) flip.run();
		return h;
	}

	private void mark() {
		cfg().markDirty();
	}

	private float rowDivider(float x, float y, float w) {
		divider(x + 12, y, w - 24);
		return 1;
	}

	// ================================================================ abas

	private float general(float x, float y, float w) {
		ClientSettings cs = cs();
		float y0 = groupStart("geral", x, y, w);
		float cy = y0;
		float segW = 170;
		int sel = "last".equals(cs.startPage) ? 1 : 0;
		float rh = settingRow("Página inicial", "O que aparece ao abrir o menu.", x + 12, cy, w - 24, segW,
			sel != 0, () -> {
				cs.startPage = DEFAULTS.startPage;
				mark();
			}, "start");
		int ns = kit.segmented("set:start", new String[] {"Painel", "Última aberta"}, sel, x + w - 12 - segW, cy + (rh - 16) / 2, segW);
		if (ns != sel) {
			cs.startPage = ns == 1 ? "last" : "dashboard";
			mark();
		}
		cy += rh;
		cy += rowDivider(x, cy, w);
		cy += toggleRow("pause", "Pausar o jogo com o menu aberto", "Só no singleplayer. Em LAN e servidores o jogo continua.", cs.pauseGame,
			DEFAULTS.pauseGame, x, cy, w, () -> {
				cs.pauseGame = !cs.pauseGame;
				mark();
			});
		return groupEnd("geral", y, cy);
	}

	private float appearance(float x, float y, float w) {
		Theme t = t();
		ClientSettings cs = cs();
		float y0 = groupStart("aparencia", x, y, w);
		float cy = y0;
		float segW = 160;
		float rh = settingRow("Tema", "Escuro combina com o jogo; claro ajuda em telas muito iluminadas.", x + 12, cy, w - 24, segW,
			cs.darkTheme != DEFAULTS.darkTheme, () -> {
				cs.darkTheme = DEFAULTS.darkTheme;
				mark();
			}, "theme");
		int sel = cs.darkTheme ? 0 : 1;
		int ns = kit.segmented("set:theme", new String[] {"Escuro", "Claro"}, sel, x + w - 12 - segW, cy + (rh - 16) / 2, segW);
		if (ns != sel) {
			cs.darkTheme = ns == 0;
			mark();
		}
		cy += rh;
		cy += rowDivider(x, cy, w);

		float swW = Theme.ACCENTS.length * 22 - 6;
		int idx = 0;
		for (int i = 0; i < Theme.ACCENTS.length; i++) if (Theme.ACCENTS[i] == cs.accent) idx = i;
		rh = settingRow("Cor de destaque", Theme.ACCENT_NAMES[idx] + ". Usada em estados ativos, botões principais e indicadores.", x + 12, cy, w - 24, swW,
			cs.accent != DEFAULTS.accent, () -> {
				cs.accent = DEFAULTS.accent;
				mark();
			}, "accent");
		int ni = kit.swatches("set:accent", Theme.ACCENTS, Theme.ACCENT_NAMES, idx, x + w - 12 - swW, cy + (rh - 16) / 2);
		if (ni != idx) {
			cs.accent = Theme.ACCENTS[ni];
			mark();
		}
		cy += rh;
		cy += rowDivider(x, cy, w);

		float slW = 150;
		rh = settingRow("Opacidade da janela", cs.opacity + "%. Mais baixo deixa o jogo aparecer por trás.", x + 12, cy, w - 24, slW,
			cs.opacity != DEFAULTS.opacity, () -> {
				cs.opacity = DEFAULTS.opacity;
				mark();
			}, "opacity");
		float f = kit.slider("set:opacity", (cs.opacity - 60) / 40f, x + w - 12 - slW, cy + (rh - 12) / 2, slW, 8);
		int nv = Math.round(60 + 40 * f);
		if (nv != cs.opacity) {
			cs.opacity = nv;
			mark();
		}
		cy += rh;

		// Prévia ao vivo
		cy += rowDivider(x, cy, w);
		cy += 10;
		ui.text(Type.CAPTION, "PRÉVIA", x + 12, cy, t.text3);
		cy += 14;
		float pwid = w - 24;
		ui.round(x + 12, cy, pwid, 46, Theme.R_LG, t.windowColor(cs.opacity));
		ui.ring(x + 12, cy, pwid, 46, Theme.R_LG, t.border);
		float bx = x + 24;
		ui.round(bx, cy + 12, 22, 22, Theme.R_MD, t.accentFill);
		ui.icon("bird", bx + 5, cy + 17, 12, t.accentText);
		ui.text(Type.BODY_SEMI, "Fly", bx + 30, cy + 11, t.text);
		ui.text(Type.LABEL, "Ligado", bx + 30, cy + 23, t.accentText);
		float pbw = kit.buttonWidth("Primário", null, true);
		kit.button("set:prev:p", "Primário", null, Kit.Btn.PRIMARY, x + 12 + pwid - 12 - pbw - Kit.TOGGLE_W - 10, cy + 15, pbw, 15, true, false);
		kit.toggle("set:prev:t", true, x + 12 + pwid - 12 - Kit.TOGGLE_W, cy + 17);
		cy += 46 + 12;
		return groupEnd("aparencia", y, cy);
	}

	private float interfaceTab(float x, float y, float w) {
		ClientSettings cs = cs();
		float y0 = groupStart("interface", x, y, w);
		float cy = y0;
		float segW = 200;
		ClientSettings.UiSize[] sizes = ClientSettings.UiSize.values();
		int sel = cs.uiSize.ordinal();
		float rh = settingRow("Tamanho da interface", "Escala de textos e controles. Muda na hora.", x + 12, cy, w - 24, segW,
			cs.uiSize != DEFAULTS.uiSize, () -> {
				cs.uiSize = DEFAULTS.uiSize;
				mark();
			}, "size");
		int ns = kit.segmented("set:size", new String[] {"Pequeno", "Médio", "Grande"}, sel, x + w - 12 - segW, cy + (rh - 16) / 2, segW);
		if (ns != sel) {
			cs.uiSize = sizes[ns];
			mark();
		}
		cy += rh;
		cy += rowDivider(x, cy, w);
		cy += toggleRow("collapse", "Barra lateral recolhida", "Mostra só os ícones; passe o mouse para ver o nome.", cs.sidebarCollapsed,
			DEFAULTS.sidebarCollapsed, x, cy, w, () -> {
				cs.sidebarCollapsed = !cs.sidebarCollapsed;
				mark();
			});
		return groupEnd("interface", y, cy);
	}

	private float performance(float x, float y, float w) {
		ClientSettings cs = cs();
		float y0 = groupStart("desempenho", x, y, w);
		float cy = y0;
		cy += toggleRow("blur", "Desfoque atrás do menu", "Usa a intensidade de desfoque das opções de vídeo do jogo.", cs.blur, DEFAULTS.blur, x, cy, w,
			() -> {
				cs.blur = !cs.blur;
				mark();
			});
		cy += rowDivider(x, cy, w);
		float segW = 220;
		Anim.Motion[] ms = Anim.Motion.values();
		int sel = cs.motion.ordinal();
		float rh = settingRow("Animações", "Reduzidas tira os movimentos e mantém só os esmaecimentos.", x + 12, cy, w - 24, segW,
			cs.motion != DEFAULTS.motion, () -> {
				cs.motion = DEFAULTS.motion;
				mark();
			}, "motion");
		int ns = kit.segmented("set:motion", new String[] {"Completas", "Reduzidas", "Desligadas"}, sel, x + w - 12 - segW, cy + (rh - 16) / 2, segW);
		if (ns != sel) {
			cs.motion = ms[ns];
			mark();
		}
		cy += rh;
		return groupEnd("desempenho", y, cy);
	}

	private float audio(float x, float y, float w) {
		ClientSettings cs = cs();
		float y0 = groupStart("audio", x, y, w);
		float cy = y0;
		cy += toggleRow("sounds", "Sons da interface", "Clique curto ao usar botões, toggles e abas.", cs.uiSounds, DEFAULTS.uiSounds, x, cy, w, () -> {
			cs.uiSounds = !cs.uiSounds;
			mark();
		});
		cy += rowDivider(x, cy, w);
		float slW = 150;
		ui.pushAlpha(cs.uiSounds ? 1 : 0.45f);
		float rh = settingRow("Volume", cs.uiVolume + "%. Relativo ao volume geral do jogo.", x + 12, cy, w - 24, slW,
			cs.uiVolume != DEFAULTS.uiVolume, () -> {
				cs.uiVolume = DEFAULTS.uiVolume;
				mark();
			}, "volume");
		float f = kit.slider("set:volume", cs.uiVolume / 100f, x + w - 12 - slW, cy + (rh - 12) / 2, slW, 20);
		int nv = Math.round(f * 100);
		if (nv != cs.uiVolume) {
			cs.uiVolume = nv;
			mark();
		}
		ui.popAlpha();
		cy += rh;
		return groupEnd("audio", y, cy);
	}

	private float shortcuts(float x, float y, float w) {
		Theme t = t();
		ClientSettings cs = cs();
		float y0 = groupStart("atalhos-menu", x, y, w);
		float cy = y0;
		float kbW = 130;
		float rh = settingRow("Tecla do menu", "Abre e fecha este menu durante o jogo.", x + 12, cy, w - 24, kbW,
			cs.menuKey != DEFAULTS.menuKey, () -> {
				cs.menuKey = DEFAULTS.menuKey;
				mark();
			}, "menukey");
		int nk = kit.keybind("set:menukey", cs.menuKey, x + w - 12 - kbW, cy + (rh - 16) / 2, kbW);
		if (nk != Kit.KEEP && nk >= 0) {
			cs.menuKey = nk;
			mark();
		}
		cy += rh;
		cy += rowDivider(x, cy, w);
		ui.text(Type.LABEL, "Atalhos fixos: Esc fecha painéis e o menu · Ctrl+F busca módulos · Tab navega · Enter confirma", x + 12, cy + 8, t.text2);
		cy += 24;
		cy = groupEnd("atalhos-menu", y, cy);

		// Tecla de cada módulo
		cy = section("Teclas dos módulos", "Clique para gravar uma tecla. Backspace remove.", x, cy, w);
		float g0 = groupStart("atalhos-mods", x, cy, w);
		float ry = g0;
		List<Module> mods = Basalto.modules().all();
		for (int i = 0; i < mods.size(); i++) {
			Module m = mods.get(i);
			if (i > 0) ry += rowDivider(x, ry, w);
			float rowH = 26;
			ui.icon(ModuleIcons.of(m.name), x + 14, ry + 8.5f, 9, t.text2);
			ui.textMid(Type.BODY_MEDIUM, m.name, x + 30, ry, rowH, t.text);
			ui.textMid(Type.CAPTION, ModuleIcons.label(m.category), x + 30 + ui.width(Type.BODY_MEDIUM, m.name) + 6, ry, rowH, t.text3);
			String conflict = conflict(m);
			float segW = 116, kw = 110;
			float kx = x + w - 12 - segW - 6 - kw;
			if (conflict != null) {
				String cid = "set:conflict:" + m.name;
				boolean hv = ui.hover(cid, kx - 16, ry + 8, 10, 10);
				ui.icon("triangle-alert", kx - 15, ry + 8.5f, 9, t.warning);
				ui.tooltip(cid, hv, "Mesma tecla de " + conflict);
			}
			int k = kit.keybind("set:key:" + m.name, m.key, kx, ry + 5, kw);
			if (k != Kit.KEEP) {
				m.key = k;
				mark();
			}
			int hold = kit.segmented("set:hold:" + m.name, new String[] {"Alternar", "Segurar"}, m.holdKey ? 1 : 0, x + w - 12 - segW, ry + 5, segW);
			if ((hold == 1) != m.holdKey) {
				m.holdKey = hold == 1;
				mark();
			}
			ry += rowH;
		}
		return groupEnd("atalhos-mods", cy, ry);
	}

	/** Outro módulo (ou o menu) com a mesma tecla. */
	private String conflict(Module m) {
		if (m.key < 0) return null;
		if (m.key == cs().menuKey) return "abrir o menu";
		List<String> names = new ArrayList<>();
		for (Module o : Basalto.modules().all()) if (o != m && o.key == m.key) names.add(o.name);
		return names.isEmpty() ? null : String.join(", ", names);
	}

	private float privacy(float x, float y, float w) {
		ClientSettings cs = cs();
		float y0 = groupStart("privacidade", x, y, w);
		float cy = y0;
		cy += toggleRow("streamer", "Modo streamer", "Esconde nome do mundo, endereço do servidor, seu nome e coordenadas no menu e no HUD.",
			cs.streamerMode, DEFAULTS.streamerMode, x, cy, w, () -> {
				cs.streamerMode = !cs.streamerMode;
				mark();
			});
		cy += rowDivider(x, cy, w);
		cy += toggleRow("activity", "Guardar atividade da sessão", "Lista o que mudou (módulos, perfis) no Painel e no sino. Nada vai para o disco.",
			cs.keepActivity, DEFAULTS.keepActivity, x, cy, w, () -> {
				cs.keepActivity = !cs.keepActivity;
				if (!cs.keepActivity) Activity.clear();
				mark();
			});
		cy += rowDivider(x, cy, w);
		float bw = kit.buttonWidth("Limpar", "trash-2", true);
		float rh = settingRow("Limpar atividade", Activity.size() + (Activity.size() == 1 ? " item guardado." : " itens guardados."), x + 12, cy, w - 24, bw, false, null, "clear");
		if (kit.button("set:clearact", "Limpar", "trash-2", Kit.Btn.DANGER, x + w - 12 - bw, cy + (rh - 15) / 2, bw, 15, Activity.size() > 0, false)) {
			app.openModal(Modal.confirm("trash-2", "Limpar a atividade?", "O histórico desta sessão some do Painel e do sino.", "Limpar", true, () -> {
				Activity.clear();
				Toasts.success("Atividade limpa", null);
			}));
		}
		cy += rh;
		return groupEnd("privacidade", y, cy);
	}

	private float about(float x, float y, float w) {
		Theme t = t();
		float y0 = groupStart("sobre", x, y, w);
		float cy = y0 + 8;
		com.basalto.gui.Draw.cube(ui.g, ui.px(x + 14), ui.px(cy + 2), Math.round(24 * ui.p), t.accent);
		ui.text(Type.TITLE, "Basalto Client", x + 46, cy + 1, t.text);
		ui.text(Type.LABEL, "Para singleplayer, LAN e servidores seus.", x + 46, cy + 14, t.text2);
		cy += 36;
		cy += rowDivider(x, cy, w) + 6;
		String loader = FabricLoader.getInstance().getModContainer("fabricloader").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
		String[][] rows = {
			{"Versão", Basalto.VERSION},
			{"Minecraft", "26.1.2"},
			{"Fabric Loader", loader},
			{"Java", System.getProperty("java.version")},
			{"Módulos", String.valueOf(Basalto.modules().all().size())},
		};
		for (String[] r : rows) {
			keyValue(r[0], r[1], x + 12, cy, w - 24, 0);
			cy += 17;
		}
		cy += 4;
		cy += rowDivider(x, cy, w);
		float ow = kit.buttonWidth("Abrir pasta", "folder-open", true);
		String path = cs().streamerMode ? "config/basalto.json" : cfg().file().toString();
		float rh = settingRow("Arquivo de configuração", path, x + 12, cy, w - 24, ow, false, null, "file");
		if (kit.button("set:openconfig", "Abrir pasta", "folder-open", Kit.Btn.SECONDARY, x + w - 12 - ow, cy + (rh - 15) / 2, ow, 15, true, false)) {
			try {
				Util.getPlatform().openPath(cfg().file().getParent());
			} catch (Exception e) {
				Toasts.error("Não deu pra abrir a pasta", e.getMessage());
			}
		}
		cy += rh;
		cy += rowDivider(x, cy, w);
		ui.text(Type.LABEL, "Fonte Manrope (SIL Open Font License 1.1) · Ícones Lucide (ISC)", x + 12, cy + 9, t.text3);
		cy += 26;
		cy = groupEnd("sobre", y, cy);

		// Zona de risco
		cy = section("Restaurar", null, x, cy, w);
		float g0 = groupStart("sobre-reset", x, cy, w);
		float ry = g0;
		float bw = kit.buttonWidth("Restaurar", "rotate-ccw", true);
		rh = settingRow("Restaurar preferências do client", "Volta aparência, interface, sons e privacidade ao padrão. Módulos, teclas e perfis ficam.",
			x + 12, ry, w - 24, bw, false, null, "resetall");
		if (kit.button("set:resetall", "Restaurar", "rotate-ccw", Kit.Btn.DANGER, x + w - 12 - bw, ry + (rh - 15) / 2, bw, 15, true, false)) {
			app.openModal(Modal.confirm("rotate-ccw", "Restaurar as preferências?",
				"Aparência, interface, sons e privacidade voltam ao padrão. Seus módulos, teclas e perfis não mudam.", "Restaurar", true, () -> {
					cfg().resetClientSettings();
					Toasts.success("Preferências restauradas", null);
				}));
		}
		ry += rh;
		return groupEnd("sobre-reset", cy, ry);
	}
}
