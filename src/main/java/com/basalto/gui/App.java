package com.basalto.gui;

import org.lwjgl.glfw.GLFW;

import com.basalto.Basalto;
import com.basalto.config.ClientSettings;
import com.basalto.config.ConfigManager;
import com.basalto.gui.pages.DashboardPage;
import com.basalto.gui.pages.HudPage;
import com.basalto.gui.pages.ModulesPage;
import com.basalto.gui.pages.ProfilesPage;
import com.basalto.gui.pages.SettingsPage;
import com.basalto.hud.Activity;
import com.basalto.module.Module;
import com.basalto.ui.Anim;
import com.basalto.ui.Colors;
import com.basalto.ui.Kit;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;
import com.basalto.ui.Ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * A casca do menu: janela, barra lateral, barra superior, páginas, modais,
 * menus suspensos e toasts. As páginas ficam em gui/pages.
 */
public final class App {
	public enum Page {
		DASHBOARD("Painel", "layout-dashboard"),
		MODULES("Módulos", "blocks"),
		PROFILES("Perfis", "layers"),
		HUD("HUD", "app-window"),
		SETTINGS("Configurações", "settings");

		public final String title, icon;

		Page(String title, String icon) {
			this.title = title;
			this.icon = icon;
		}
	}

	public final Ui ui = new Ui();
	public final Kit kit = new Kit(ui);
	public Page page;
	public Modal modal;
	private Modal closingModal;
	private long modalOpened;
	private boolean notifOpen, profileMenuOpen;
	public int frame;
	private final long openedAt = System.currentTimeMillis();
	private boolean closing;
	private long closingAt;
	private Runnable onClosed = () -> {};

	public final DashboardPage dashboard = new DashboardPage(this);
	public final ModulesPage modules = new ModulesPage(this);
	public final ProfilesPage profiles = new ProfilesPage(this);
	public final HudPage hud = new HudPage(this);
	public final SettingsPage settings = new SettingsPage(this);

	/** Área do conteúdo da página atual (para as páginas saberem onde estão). */
	public float cx, cy, cw, ch;
	/** Janela inteira. */
	public float wx, wy, ww, wh;

	public App() {
		ClientSettings cs = cs();
		Page start = Page.DASHBOARD;
		if ("last".equals(cs.startPage)) {
			try {
				start = Page.valueOf(cs.lastPage);
			} catch (Exception ignored) {
			}
		}
		page = start;
		kit.setClickSound(this::clickSound);
	}

	public ClientSettings cs() {
		return Basalto.config().client;
	}

	public ConfigManager cfg() {
		return Basalto.config();
	}

	public Theme t() {
		return ui.t;
	}

	public void navigate(Page p) {
		if (p == page) return;
		page = p;
		cs().lastPage = p.name();
		cfg().markDirty();
		Anim.reset("page");
		kit.openDropdown = null;
		kit.capturing = null;
		ui.focus = null;
	}

	public void openModal(Modal m) {
		modal = m;
		modalOpened = System.currentTimeMillis();
		Anim.reset("modal");
		ui.focus = m.field != null ? "modal:field" : null;
		kit.openDropdown = null;
	}

	/** Abre/fecha o painel de atividade (sino) e o menu da conta. */
	public void showPanels(boolean notifications, boolean profileMenu) {
		notifOpen = notifications;
		profileMenuOpen = profileMenu;
	}

	public void closeModal() {
		closingModal = modal;
		modal = null;
		ui.focus = null;
	}

	public void requestClose(Runnable done) {
		if (closing) return;
		closing = true;
		closingAt = System.currentTimeMillis();
		onClosed = done;
		if (Anim.motion == Anim.Motion.OFF) done.run();
	}

	private void clickSound() {
		ClientSettings cs = cs();
		if (!cs.uiSounds || cs.uiVolume <= 0) return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f, 0.25f * cs.uiVolume / 100f));
	}

	private float density() {
		var win = Minecraft.getInstance().getWindow();
		return Ui.autoDensity(win.getWidth(), win.getHeight(), cs().uiSize);
	}

	// ================================================================ frame

	public void draw(GuiGraphicsExtractor g) {
		frame++;
		ClientSettings cs = cs();
		Anim.motion = cs.motion;
		ui.begin(g, cs.theme(), density());
		Theme t = ui.t;

		// Entrada e saída da janela.
		float open = Ui.ease(Math.min(1, (System.currentTimeMillis() - openedAt) / Theme.SLOW));
		if (closing) {
			float k = Math.min(1, (System.currentTimeMillis() - closingAt) / Theme.FAST);
			open = 1 - Ui.ease(k);
			if (k >= 1) {
				ui.end();
				onClosed.run();
				return;
			}
		}
		if (Anim.motion == Anim.Motion.OFF) open = closing ? 0 : 1;

		float m = ui.W < 760 ? 6 : 12;
		ww = Math.min(ui.W - 2 * m, 1240);
		wh = Math.min(ui.H - 2 * m, 760);
		wx = (ui.W - ww) / 2;
		wy = (ui.H - wh) / 2;

		ui.pushAlpha(open);
		float scale = 0.985f + 0.015f * open;
		boolean scaled = Anim.motion == Anim.Motion.FULL && scale < 0.9999f;
		if (scaled) {
			var pose = g.pose();
			pose.pushMatrix();
			float pcx = ui.px(wx + ww / 2), pcy = ui.px(wy + wh / 2);
			pose.translate(pcx, pcy);
			pose.scale(scale, scale);
			pose.translate(-pcx, -pcy);
		}

		// Janela
		ui.block("window", wx, wy, ww, wh);
		ui.shadow(wx, wy, ww, wh, 22, 6, t.dark ? 0.6f : 0.18f);
		ui.round(wx, wy, ww, wh, Theme.R_XL, t.windowColor(cs.opacity));
		ui.ring(wx, wy, ww, wh, Theme.R_XL, t.border);

		boolean narrow = ww < 720;
		float sideTarget = cs.sidebarCollapsed || narrow ? 46 : 140;
		float sw = ui.anim("sidebar", sideTarget, Theme.BASE);
		float th = 40;

		drawSidebar(wx, wy, sw, wh, sw < 90);
		cx = wx + sw;
		cy = wy + th;
		cw = ww - sw;
		ch = wh - th;

		// Página com transição (fade + subida curta)
		float pa = ui.animFrom("page", 0, 1, Theme.BASE);
		ui.pushClip(cx, cy, cw, ch - 1);
		ui.pushAlpha(pa);
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(0, Math.round(ui.px(6 * (1 - pa) * Anim.move())));
		ui.layer = 0;
		drawPage();
		pose.popMatrix();
		ui.popAlpha();
		ui.popClip();
		ui.flushOverlays();

		drawTopbar(cx, wy, cw, th);
		ui.flushOverlays();

		Toasts.draw(ui, wx + ww - 12, wy + wh - 12);

		drawModal();

		if (scaled) g.pose().popMatrix();
		ui.popAlpha();

		handleKeys();
		ui.end();
	}

	private void drawPage() {
		switch (page) {
			case DASHBOARD -> dashboard.draw(cx, cy, cw, ch);
			case MODULES -> modules.draw(cx, cy, cw, ch);
			case PROFILES -> profiles.draw(cx, cy, cw, ch);
			case HUD -> hud.draw(cx, cy, cw, ch);
			case SETTINGS -> settings.draw(cx, cy, cw, ch);
		}
	}

	private void handleKeys() {
		if (kit.capturing != null) return;
		if (ui.consumeKey(GLFW.GLFW_KEY_ESCAPE)) {
			if (modal != null) closeModal();
			else if (kit.openDropdown != null) kit.openDropdown = null;
			else if (notifOpen || profileMenuOpen) notifOpen = profileMenuOpen = false;
			else if (page == Page.MODULES && modules.closeDrawer()) {
				// fechou o painel do módulo
			} else if (ui.focus != null && ui.focus.startsWith("field:")) ui.focus = null;
			else closeRequested = true;
		}
		if (modal == null && ui.consumeShortcut(GLFW.GLFW_KEY_F)) {
			navigate(Page.MODULES);
			ui.focus = "field:search";
		}
		// A tecla do menu fecha (se não estiver digitando).
		boolean typing = ui.focus != null && ui.focus.startsWith("field:") || modal != null && modal.field != null;
		if (!typing && modal == null && ui.consumeKey(cs().menuKey)) closeRequested = true;
	}

	/** O App pediu para fechar (Esc ou tecla do menu). A Screen lê e zera. */
	public boolean closeRequested;

	// ================================================================ barra lateral

	private void drawSidebar(float x, float y, float w, float h, boolean collapsed) {
		Theme t = ui.t;
		ui.vline(x + w, y + 12, h - 24, t.divider);
		ui.pushClip(x, y, w, h);

		// Marca
		float bx = x + (collapsed ? (w - 16) / 2 : 14);
		com.basalto.gui.Draw.cube(ui.g, ui.px(bx), ui.px(y + 13), Math.round(16 * ui.p), t.accent);
		float labelA = Ui.clamp((w - 90) / 40f, 0, 1);
		if (labelA > 0) {
			ui.pushAlpha(labelA);
			ui.text(Type.BRAND, "Basalto", bx + 22, y + 11, t.text);
			ui.text(Type.CAPTION, "v" + Basalto.VERSION + " · 26.1.2", bx + 22, y + 21, t.text3);
			ui.popAlpha();
		}

		// Navegação
		float ny = y + 48;
		if (labelA > 0) {
			ui.pushAlpha(labelA);
			ui.text(Type.CAPTION, "MENU", x + 16, ny, t.text3);
			ui.popAlpha();
		}
		ny += 12;
		for (Page p : new Page[] {Page.DASHBOARD, Page.MODULES, Page.PROFILES, Page.HUD}) {
			navItem(p, x, ny, w, collapsed, labelA);
			ny += 24;
		}
		ny += 6;
		ui.hline(x + 12, ny, w - 24, t.divider);
		ny += 10;
		navItem(Page.SETTINGS, x, ny, w, collapsed, labelA);

		// Recolher
		float by = y + h - 32;
		String id = "nav:collapse";
		float iw = w - 16;
		boolean hovered = ui.hover(id, x + 8, by, iw, 22);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float hv = ui.anim(id, hovered ? 1 : 0, Theme.FAST);
		ui.round(x + 8, by, iw, 22, Theme.R_MD, Colors.fade(t.hover, hv * 1.4f));
		String icon = cs().sidebarCollapsed ? "panel-left-open" : "panel-left-close";
		float ix = collapsed ? x + (w - 10) / 2 : x + 16;
		ui.icon(icon, ix, by + 6, 10, Colors.mix(t.text3, t.text, hv));
		if (labelA > 0) {
			ui.pushAlpha(labelA);
			ui.textMid(Type.BODY_MEDIUM, "Recolher", x + 32, by, 22, Colors.mix(t.text2, t.text, hv));
			ui.popAlpha();
		}
		if (focused && ui.keyboardNav) ui.focusRing(x + 8, by, iw, 22, Theme.R_MD);
		if (collapsed) ui.tooltip(id, hovered, cs().sidebarCollapsed ? "Expandir barra lateral" : "Recolher barra lateral");
		if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
			cs().sidebarCollapsed = !cs().sidebarCollapsed;
			cfg().markDirty();
			kit.click();
		}
		ui.popClip();
	}

	private void navItem(Page p, float x, float y, float w, boolean collapsed, float labelA) {
		Theme t = ui.t;
		String id = "nav:" + p.name();
		boolean on = page == p;
		float ix = x + 8, iw = w - 16, h = 22;
		boolean hovered = ui.hover(id, ix, y, iw, h);
		boolean focused = ui.focusable(id);
		if (hovered && !on) ui.hand();
		float hv = ui.anim(id, hovered ? 1 : 0, Theme.FAST);
		float act = ui.anim(id + ":on", on ? 1 : 0, Theme.BASE);
		if (act > 0.01f) {
			ui.round(ix, y, iw, h, Theme.R_MD, Colors.fade(t.accentFill, act));
			ui.glow(x + 1, y + 6, 2, h - 12, 3, 0.35f * act);
			ui.round(x + 1, y + 6, 2, h - 12, 1, Colors.fade(t.accent, act));
		} else if (hv > 0.01f) {
			ui.round(ix, y, iw, h, Theme.R_MD, Colors.fade(t.hover, hv * 1.4f));
		}
		float icx = collapsed ? x + (w - 10) / 2 : ix + 8;
		int fg = on ? t.accentText : Colors.mix(t.text2, t.text, hv);
		ui.icon(p.icon, icx, y + 6, 10, fg);
		if (labelA > 0) {
			ui.pushAlpha(labelA);
			ui.textMid(on ? Type.BODY_SEMI : Type.BODY_MEDIUM, p.title, ix + 24, y, h, on ? t.text : Colors.mix(t.text2, t.text, hv));
			if (p == Page.MODULES) {
				int n = activeCount();
				if (n > 0) {
					String s = String.valueOf(n);
					float bw = Math.max(14, ui.width(Type.CAPTION, s) + 8);
					ui.round(ix + iw - bw - 6, y + 6, bw, 10, 5, on ? Colors.withAlpha(t.accent, 0.22f) : t.surface2);
					ui.textCenter(Type.CAPTION, s, ix + iw - bw / 2 - 6, y + 6.5f, on ? t.accentText : t.text2);
				}
			}
			ui.popAlpha();
		}
		if (focused && ui.keyboardNav) ui.focusRing(ix, y, iw, h, Theme.R_MD);
		if (collapsed) ui.tooltip(id, hovered, p.title);
		if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
			if (!on) kit.click();
			navigate(p);
		}
	}

	public int activeCount() {
		int n = 0;
		for (Module m : Basalto.modules().all()) if (m.isEnabled()) n++;
		return n;
	}

	// ================================================================ barra superior

	private void drawTopbar(float x, float y, float w, float h) {
		Theme t = ui.t;
		ui.hline(x + 16, y + h - 1, w - 32, t.divider);

		// Trilha (breadcrumb)
		float bx = x + 20;
		ui.textMid(Type.LABEL, "Basalto", bx, y, h, t.text3);
		bx += ui.width(Type.LABEL, "Basalto") + 5;
		ui.icon("chevron-right", bx, y + (h - 7) / 2, 7, t.text3);
		bx += 11;
		String sub = page == Page.SETTINGS ? settings.currentTitle() : page == Page.MODULES ? modules.breadcrumb() : null;
		ui.textMid(sub == null ? Type.BODY_SEMI : Type.LABEL, page.title, bx, y, h, sub == null ? t.text : t.text3);
		bx += ui.width(sub == null ? Type.BODY_SEMI : Type.LABEL, page.title) + 5;
		if (sub != null) {
			ui.icon("chevron-right", bx, y + (h - 7) / 2, 7, t.text3);
			bx += 11;
			ui.textMid(Type.BODY_SEMI, sub, bx, y, h, t.text);
		}

		// Direita: avatar, sino, tema, conexão, ambiente, salvamento
		float rx = x + w - 16;
		float av = 22;
		rx -= av + 14;
		drawAvatar(rx, y + (h - av) / 2, av);
		rx -= 6 + 22;
		boolean bell = kit.iconButton("top:bell", "bell", rx, y + (h - 22) / 2, 22, "Atividade", notifOpen);
		int unread = Activity.unread();
		if (unread > 0 && !notifOpen) {
			ui.round(rx + 13, y + (h - 22) / 2 + 4, 6, 6, 3, t.accent);
			ui.ring(rx + 12.5f, y + (h - 22) / 2 + 3.5f, 7, 7, 3.5f, t.window);
		}
		if (bell) {
			notifOpen = !notifOpen;
			profileMenuOpen = false;
			if (notifOpen) Activity.markRead();
		}
		float bellX = rx;
		rx -= 4 + 22;
		if (kit.iconButton("top:theme", cs().darkTheme ? "sun" : "moon", rx, y + (h - 22) / 2, 22,
			cs().darkTheme ? "Tema claro" : "Tema escuro", false)) {
			cs().darkTheme = !cs().darkTheme;
			cfg().markDirty();
		}
		rx -= 10;
		ui.vline(rx, y + 13, h - 26, t.divider);
		rx -= 10;

		boolean narrowTop = w < 560;
		// Conexão
		Env.Kind kind = Env.kind();
		if (!narrowTop) {
			int ping = Env.ping();
			String conn = kind == Env.Kind.NONE ? "Desconectado" : ping >= 0 ? ping + " ms" : "Integrado";
			int connColor = kind == Env.Kind.NONE ? t.danger : ping > 250 ? t.warning : t.text2;
			float cw2 = ui.width(Type.LABEL, conn) + 14;
			rx -= cw2;
			ui.icon(kind == Env.Kind.NONE ? "wifi-off" : "signal", rx, y + (h - 9) / 2, 9, connColor);
			ui.textMid(Type.LABEL, conn, rx + 13, y, h, connColor);
			rx -= 10;
		}
		// Ambiente
		int envColor = switch (kind) {
			case LOCAL -> t.accent;
			case LAN -> t.info;
			case SERVER -> t.warning;
			case NONE -> t.danger;
		};
		float ew = ui.width(Type.LABEL, kind.label) + 20;
		rx -= ew;
		kit.statusBadge(kind.label, envColor, rx, y + (h - 14) / 2);
		String envId = "top:env";
		ui.tooltip(envId, ui.hover(envId, rx, y + (h - 14) / 2, ew, 14), switch (kind) {
			case LOCAL -> "Mundo singleplayer neste computador";
			case LAN -> "Mundo aberto na rede local";
			case SERVER -> "Servidor externo: use só onde tiver permissão";
			case NONE -> "Nenhum mundo aberto";
		});
		// Salvamento
		if (!narrowTop) {
			boolean saving = cfg().isSaving();
			long since = System.currentTimeMillis() - cfg().lastSave();
			float sa = ui.anim("top:save", saving || since < 2500 ? 1 : 0, Theme.SLOW);
			if (sa > 0.01f) {
				String s = saving ? "Salvando…" : "Salvo";
				float sw2 = ui.width(Type.LABEL, s) + 14;
				rx -= sw2 + 10;
				ui.pushAlpha(sa);
				ui.icon(saving ? "history" : "check", rx, y + (h - 9) / 2, 9, saving ? t.text3 : t.accentText);
				ui.textMid(Type.LABEL, s, rx + 13, y, h, t.text3);
				ui.popAlpha();
			}
		}

		if (notifOpen) drawNotifications(bellX + 22, y + h - 2);
		if (profileMenuOpen) drawProfileMenu(x + w - 16, y + h - 2);
	}

	private void drawAvatar(float x, float y, float s) {
		Theme t = ui.t;
		String id = "top:avatar";
		float w = s + 12;
		boolean hovered = ui.hover(id, x - 2, y - 2, w + 4, s + 4);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float hv = ui.anim(id, hovered || profileMenuOpen ? 1 : 0, Theme.FAST);
		ui.round(x - 2, y - 2, w + 4, s + 4, Theme.R_MD, Colors.fade(t.hover, hv * 1.4f));
		var player = Minecraft.getInstance().player;
		if (player != null) {
			ui.round(x, y, s, s, Theme.R_MD, t.surface2);
			int pad = Math.round(2 * ui.p);
			PlayerFaceExtractor.extractRenderState(ui.g, player.getSkin(), ui.px(x) + pad, ui.px(y) + pad, ui.px(s) - 2 * pad);
			ui.ring(x, y, s, s, Theme.R_MD, t.borderStrong);
		} else {
			ui.skeleton(x, y, s, s, Theme.R_MD);
		}
		ui.icon("chevron-down", x + s + 3, y + (s - 7) / 2, 7, t.text2);
		if (focused && ui.keyboardNav) ui.focusRing(x - 2, y - 2, w + 4, s + 4, Theme.R_MD);
		ui.tooltip(id, hovered && !profileMenuOpen, "Conta e atalhos");
		if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
			profileMenuOpen = !profileMenuOpen;
			notifOpen = false;
			kit.click();
		}
	}

	private void drawNotifications(float right, float top) {
		ui.overlay(() -> {
			Theme t = ui.t;
			float w = 230;
			var list = Activity.recent(8);
			float rowH = 22;
			float h = 34 + (list.isEmpty() ? 64 : list.size() * rowH) + 8;
			float x = right - w, y = top + 4;
			float a = ui.animFrom("notif", 0, 1, Theme.FAST);
			ui.pushAlpha(a);
			y -= 4 * (1 - a) * Anim.move();
			ui.layer = 6;
			ui.block("notif:panel", x, y, w, h);
			ui.shadow(x, y, w, h, 16, 6, t.dark ? 0.6f : 0.18f);
			ui.round(x, y, w, h, Theme.R_LG, t.elevated);
			ui.ring(x, y, w, h, Theme.R_LG, t.borderStrong);
			ui.text(Type.BODY_SEMI, "Atividade", x + 12, y + 10, t.text);
			if (!list.isEmpty() && kit.button("notif:clear", "Limpar", null, Kit.Btn.GHOST, x + w - 52, y + 7, 44, 14, true, false)) {
				Activity.clear();
			}
			ui.hline(x + 1, y + 28, w - 2, t.divider);
			float ry = y + 32;
			if (list.isEmpty()) {
				ui.icon("bell", x + w / 2 - 7, ry + 12, 14, t.text3);
				ui.textCenter(Type.BODY, "Nada por enquanto", x + w / 2, ry + 32, t.text2);
				ui.textCenter(Type.LABEL, "Ligar módulos e trocar de perfil aparece aqui.", x + w / 2, ry + 44, t.text3);
			}
			for (Activity.Entry e : list) {
				activityRow(e, x + 6, ry, w - 12, rowH);
				ry += rowH;
			}
			if (ui.pressedOutside(x, y, w, h) && !ui.mouseIn(right - 22, top - 30, 22, 30)) notifOpen = false;
			ui.popAlpha();
		});
	}

	/** Uma linha do histórico (usada no sino e no Painel). */
	public void activityRow(Activity.Entry e, float x, float y, float w, float h) {
		Theme t = ui.t;
		int color = switch (e.kind()) {
			case ON -> t.accentText;
			case OFF -> t.text3;
			case PROFILE -> t.info;
			case WARNING -> t.warning;
			default -> t.text2;
		};
		String icon = e.icon();
		if (icon == null) {
			String name = e.text().split(" ")[0];
			icon = ModuleIcons.of(name);
		}
		ui.round(x + 4, y + (h - 14) / 2, 14, 14, Theme.R_SM, Colors.withAlpha(color, 0.12f));
		ui.icon(icon, x + 7, y + (h - 8) / 2, 8, color);
		String when = Activity.ago(e.time());
		float ww2 = ui.width(Type.CAPTION, when);
		ui.textMid(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, e.text(), w - 34 - ww2), x + 24, y, h, t.text);
		ui.textMid(Type.CAPTION, when, x + w - ww2 - 4, y, h, t.text3);
	}

	private void drawProfileMenu(float right, float top) {
		ui.overlay(() -> {
			Theme t = ui.t;
			float w = 190;
			String[][] items = {
				{"layers", "Perfis", "PROFILES"},
				{"blocks", "Módulos", "MODULES"},
				{"settings", "Configurações", "SETTINGS"},
				{"x", "Fechar menu", "CLOSE"},
			};
			float h = 46 + items.length * 22 + 8;
			float x = right - w, y = top + 4;
			float a = ui.animFrom("pmenu", 0, 1, Theme.FAST);
			ui.pushAlpha(a);
			y -= 4 * (1 - a) * Anim.move();
			ui.layer = 6;
			ui.block("pmenu:panel", x, y, w, h);
			ui.shadow(x, y, w, h, 16, 6, t.dark ? 0.6f : 0.18f);
			ui.round(x, y, w, h, Theme.R_LG, t.elevated);
			ui.ring(x, y, w, h, Theme.R_LG, t.borderStrong);
			ui.text(Type.BODY_SEMI, ui.fit(Type.BODY_SEMI, Env.playerName(), w - 24), x + 12, y + 9, t.text);
			String prof = "Perfil: " + cfg().activeProfile;
			ui.text(Type.LABEL, ui.fit(Type.LABEL, prof, w - 24), x + 12, y + 21, t.text2);
			ui.hline(x + 1, y + 38, w - 2, t.divider);
			float ry = y + 42;
			for (String[] it : items) {
				String id = "pmenu:" + it[2];
				boolean hovered = ui.hover(id, x + 4, ry, w - 8, 22);
				boolean focused = ui.focusable(id);
				if (hovered) ui.hand();
				if (hovered) ui.round(x + 4, ry, w - 8, 22, Theme.R_SM, t.hover);
				boolean close = it[2].equals("CLOSE");
				ui.icon(it[0], x + 12, ry + 6, 10, close ? t.text2 : t.text2);
				ui.textMid(Type.BODY_MEDIUM, it[1], x + 30, ry, 22, t.text);
				if (close) ui.textRight(Type.CAPTION, "Esc", x + w - 12, ry + 6.5f, t.text3);
				if (focused && ui.keyboardNav) ui.focusRing(x + 4, ry, w - 8, 22, Theme.R_SM);
				if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
					profileMenuOpen = false;
					kit.click();
					if (close) closeRequested = true;
					else navigate(Page.valueOf(it[2]));
				}
				ry += 22;
			}
			if (ui.pressedOutside(x, y, w, h) && !ui.mouseIn(right - 40, top - 30, 40, 30)) profileMenuOpen = false;
			ui.popAlpha();
		});
	}

	// ================================================================ modal

	private void drawModal() {
		Modal m = modal != null ? modal : closingModal;
		if (m == null) return;
		Theme t = ui.t;
		float a = ui.anim("modal", modal != null ? 1 : 0, Theme.BASE);
		if (modal == null && a < 0.02f) {
			closingModal = null;
			return;
		}
		ui.layer = 10;
		ui.pushAlpha(a);
		ui.block("modal:scrim", 0, 0, ui.W, ui.H);
		ui.round(wx, wy, ww, wh, Theme.R_XL, t.scrim);

		float w = Math.min(m.width, ww - 40);
		float pad = 16;
		java.util.List<String> body = m.body.isEmpty() ? java.util.List.of() : ui.wrap(Type.BODY, m.body, w - pad * 2 - 30, 4);
		// O conteúdo livre informa a própria altura; usa a do frame anterior.
		float cH = m.content != null ? ui.anim("modal:ch", lastContentH, 0) : 0;
		float h = pad + 24 + body.size() * Type.BODY.lineHeight + (m.field != null ? 46 : 0) + (m.content != null ? cH + 8 : 0) + 18 + 16 + pad;
		float x = wx + (ww - w) / 2, y = wy + (wh - h) / 2;
		float s = 0.96f + 0.04f * a;
		var pose = ui.g.pose();
		pose.pushMatrix();
		if (Anim.motion == Anim.Motion.FULL) {
			float pcx = ui.px(x + w / 2), pcy = ui.px(y + h / 2);
			pose.translate(pcx, pcy);
			pose.scale(s, s);
			pose.translate(-pcx, -pcy);
		}
		ui.block("modal:card", x, y, w, h);
		ui.shadow(x, y, w, h, 24, 8, t.dark ? 0.7f : 0.22f);
		ui.round(x, y, w, h, Theme.R_XL, t.elevated);
		ui.ring(x, y, w, h, Theme.R_XL, t.borderStrong);

		int color = m.danger ? t.danger : t.accentText;
		ui.round(x + pad, y + pad, 22, 22, Theme.R_MD, Colors.withAlpha(color, 0.13f));
		ui.icon(m.icon, x + pad + 6, y + pad + 6, 10, color);
		float tx = x + pad + 30;
		ui.textMid(Type.TITLE, m.title, tx, y + pad, 22, t.text);
		float cy2 = y + pad + 26;
		for (String line : body) {
			ui.text(Type.BODY, line, tx, cy2, t.text2);
			cy2 += Type.BODY.lineHeight;
		}
		boolean submit = false;
		if (m.field != null) {
			cy2 += 8;
			if (m.fieldLabel != null) ui.text(Type.LABEL, m.fieldLabel, x + pad, cy2, t.text2);
			cy2 += 12;
			if (modal != null && ui.focus == null) ui.focus = "modal:field";
			var r = kit.textField("modal:field", m.field, m.fieldPlaceholder, null, x + pad, cy2, w - pad * 2, ConfigManager.MAX_NAME);
			if (r == Kit.FieldResult.CHANGED) m.error = null;
			if (r == Kit.FieldResult.SUBMITTED) submit = true;
			cy2 += 20;
			if (m.error != null) ui.text(Type.LABEL, m.error, x + pad, cy2 + 1, t.danger);
			cy2 += 6;
		}
		if (m.content != null) {
			cy2 += 4;
			lastContentH = m.content.draw(this, x + pad, cy2, w - pad * 2);
			cy2 += cH + 4;
		}

		float by = y + h - pad - 16;
		float cwid = kit.buttonWidth(m.confirmLabel, null, false);
		boolean confirm = m.onConfirm != null && kit.button("modal:ok", m.confirmLabel, null, m.danger ? Kit.Btn.DANGER : Kit.Btn.PRIMARY,
			x + w - pad - cwid, by, cwid);
		float kw = kit.buttonWidth(m.cancelLabel, null, false);
		boolean cancel = kit.button("modal:cancel", m.cancelLabel, null, Kit.Btn.SECONDARY,
			x + w - pad - (m.onConfirm != null ? cwid + 6 : 0) - kw, by, kw);
		pose.popMatrix();
		ui.popAlpha();
		ui.layer = 0;

		if (modal == null) return;
		if (m.field == null && m.onConfirm != null && (ui.consumeKey(GLFW.GLFW_KEY_ENTER) || ui.consumeKey(GLFW.GLFW_KEY_KP_ENTER))) submit = true;
		if (cancel) closeModal();
		else if (confirm || submit) {
			String val = m.field == null ? "" : m.field.toString();
			if (m.validator != null) {
				String err = m.validator.apply(val);
				if (err != null) {
					m.error = err;
					return;
				}
			}
			closeModal();
			m.onConfirm.accept(val);
		}
	}

	private float lastContentH;
}
