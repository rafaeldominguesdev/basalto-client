package com.basalto.gui.pages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.basalto.Basalto;
import com.basalto.gui.App;
import com.basalto.gui.Modal;
import com.basalto.gui.ModuleIcons;
import com.basalto.gui.Toasts;
import com.basalto.hud.Activity;
import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;
import com.basalto.setting.ModeSetting;
import com.basalto.setting.NumberSetting;
import com.basalto.setting.Setting;
import com.basalto.ui.Anim;
import com.basalto.ui.Colors;
import com.basalto.ui.Kit;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;
import com.basalto.util.Keys;

/** Grade de módulos com busca, filtros e o painel lateral de opções de cada um. */
public final class ModulesPage extends PageBase {
	private static final String SCROLL = "page:modules";
	private static final StringBuilder SEARCH = new StringBuilder();
	private static int category = -1;
	private static int stateFilter;

	private Module drawer;
	private Module drawerClosing;

	public ModulesPage(App app) {
		super(app);
	}

	public String breadcrumb() {
		if (drawer != null) return drawer.name;
		if (category >= 0) return ModuleIcons.label(Category.values()[category]);
		return null;
	}

	public boolean closeDrawer() {
		if (drawer == null) return false;
		drawerClosing = drawer;
		drawer = null;
		return true;
	}

	public void openDrawer(Module m) {
		drawer = m;
		drawerClosing = null;
		ui.resetScroll("drawer");
	}

	private List<Module> filtered() {
		String q = SEARCH.toString().trim().toLowerCase(Locale.ROOT);
		List<Module> out = new ArrayList<>();
		for (Module m : Basalto.modules().all()) {
			if (category >= 0 && m.category != Category.values()[category]) continue;
			if (stateFilter == 1 && !m.isEnabled()) continue;
			if (stateFilter == 2 && !cs().favorites.contains(m.name)) continue;
			if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q) && !m.description.toLowerCase(Locale.ROOT).contains(q)
				&& !ModuleIcons.label(m.category).toLowerCase(Locale.ROOT).contains(q)) continue;
			out.add(m);
		}
		return out;
	}

	@Override
	public void draw(float x, float y, float w, float h) {
		Theme t = t();
		float off = ui.scroll(SCROLL);
		float px = x + PAD, pw = w - PAD * 2;
		float top = y + 18 - off;

		int total = Basalto.modules().all().size();
		float bw = kit.buttonWidth("Desligar todos", "zap", false);
		float cy = header("Módulos", app.activeCount() + " de " + total + " ligados. Clique num card para ver as opções.", px, top, pw - bw - 10);
		int active = app.activeCount();
		if (kit.button("mods:panic", "Desligar todos", "zap", Kit.Btn.SECONDARY, px + pw - bw, top + 4, bw, Theme.CONTROL_H, active > 0, false)) {
			app.openModal(Modal.confirm("zap", "Desligar todos os módulos?",
				active + (active == 1 ? " módulo ligado vai ser desligado." : " módulos ligados vão ser desligados.") + " O perfil não muda até você salvar.",
				"Desligar todos", true, () -> {
					for (Module m : Basalto.modules().all()) m.setEnabled(false, false);
					Activity.log(Activity.Kind.OFF, "zap", "Todos os módulos desligados");
					Toasts.success("Módulos desligados", null);
				}));
		}

		// Barra de ferramentas: busca + estado
		float fieldW = Math.min(240, pw * 0.45f);
		kit.textField("field:search", SEARCH, "Buscar por nome ou descrição", "search", px, cy, fieldW, 32);
		String[] states = {"Todos", "Ligados", "Favoritos"};
		float segW = Math.min(210, pw - fieldW - 10);
		int ns = kit.segmented("mods:state", states, stateFilter, px + pw - segW, cy + 1, segW);
		if (ns != stateFilter) {
			stateFilter = ns;
			ui.scrollTo(SCROLL, 0);
		}
		cy += 26;

		// Categorias
		float chx = px;
		chx += chip("mods:cat:all", "Todos", null, total, category == -1, chx, cy, () -> category = -1) + 6;
		for (Category c : Category.values()) {
			int n = Basalto.modules().inCategory(c).size();
			final int idx = c.ordinal();
			float cwid = chipWidth(ModuleIcons.label(c), n);
			if (chx + cwid > px + pw) break;
			chx += chip("mods:cat:" + c.name(), ModuleIcons.label(c), ModuleIcons.icon(c), n, category == idx, chx, cy, () -> category = idx) + 6;
		}
		cy += 24;

		// Grade
		List<Module> list = filtered();
		float gap = 10;
		int cols = Math.max(1, (int) ((pw + gap) / (196 + gap)));
		float cwid = (pw - gap * (cols - 1)) / cols;
		float chh = 94;
		if (list.isEmpty()) {
			boolean hasQuery = !SEARCH.isEmpty();
			kit.emptyState("search", "Nenhum módulo encontrado",
				hasQuery ? "Nada combina com \"" + SEARCH + "\" nesse filtro." : stateFilter == 2 ? "Você ainda não marcou nenhum favorito." : "Nenhum módulo nesse filtro.",
				px + pw / 2, cy + 30, pw - 60);
			float lb = kit.buttonWidth("Limpar filtros", "x", false);
			if (kit.button("mods:clear", "Limpar filtros", "x", Kit.Btn.SECONDARY, px + (pw - lb) / 2, cy + 120, lb)) {
				SEARCH.setLength(0);
				category = -1;
				stateFilter = 0;
			}
			cy += 150;
		}
		for (int i = 0; i < list.size(); i++) {
			float gx = px + (i % cols) * (cwid + gap);
			float gy = cy + (i / cols) * (chh + gap);
			if (gy + chh < y - 10 || gy > y + h + 10) continue;
			moduleCard(list.get(i), gx, gy, cwid, chh, i);
		}
		cy += ((list.size() + cols - 1) / cols) * (chh + gap) + 14;
		ui.scrollArea(SCROLL, x, y, w, h, cy + off - y);

		drawDrawer(x, y, w, h);
	}

	private float chipWidth(String label, int count) {
		return ui.width(Type.BODY_MEDIUM, label) + ui.width(Type.CAPTION, String.valueOf(count)) + 26;
	}

	private float chip(String id, String label, String icon, int count, boolean on, float x, float y, Runnable select) {
		Theme t = t();
		float w = chipWidth(label, count) + (icon != null ? 12 : 0);
		float h = 16;
		boolean hovered = ui.hover(id, x, y, w, h);
		boolean focused = ui.focusable(id);
		if (hovered && !on) ui.hand();
		float k = ui.anim(id, on ? 1 : 0, Theme.BASE);
		float hv = ui.anim(id + ":h", hovered ? 1 : 0, Theme.FAST);
		ui.round(x, y, w, h, h / 2, Colors.mix(Colors.fade(t.hover, hv * 1.4f), t.accentFill, k));
		ui.ring(x, y, w, h, h / 2, Colors.mix(t.border, t.accentBorder, k));
		float tx = x + 9;
		if (icon != null) {
			ui.icon(icon, tx, y + 4, 8, on ? t.accentText : t.text2);
			tx += 12;
		}
		ui.textMid(Type.BODY_MEDIUM, label, tx, y, h, on ? t.text : Colors.mix(t.text2, t.text, hv));
		ui.textMid(Type.CAPTION, String.valueOf(count), tx + ui.width(Type.BODY_MEDIUM, label) + 5, y, h, on ? t.accentText : t.text3);
		if (focused && ui.keyboardNav) ui.focusRing(x, y, w, h, h / 2);
		if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
			if (!on) kit.click();
			select.run();
			ui.scrollTo(SCROLL, 0);
		}
		return w;
	}

	private void moduleCard(Module m, float x, float y, float w, float h, int index) {
		Theme t = t();
		String id = "mod:" + m.name;
		boolean on = m.isEnabled();
		float enter = ui.animFrom(id + ":in", 0, 1, Theme.SLOW + Math.min(index, 12) * 18);
		ui.pushAlpha(enter);
		y += (1 - enter) * 6 * Anim.move();
		boolean hovered = ui.hover(id, x, y, w, h);
		boolean focused = ui.focusable(id);
		if (hovered) ui.hand();
		float hv = ui.anim(id + ":h", hovered ? 1 : 0, Theme.FAST);
		float k = ui.anim(id + ":on", on ? 1 : 0, Theme.BASE);
		float lift = hv * 1.5f * Anim.move();
		y -= lift;
		kit.card(x, y, w, h, hv, k);
		if (focused && ui.keyboardNav) ui.focusRing(x, y, w, h, Theme.R_LG);

		// Ícone
		ui.round(x + 12, y + 12, 22, 22, Theme.R_MD, Colors.mix(t.surface2, t.accentFill, k));
		if (k > 0.01f) ui.ring(x + 12, y + 12, 22, 22, Theme.R_MD, Colors.fade(t.accentBorder, k));
		ui.icon(ModuleIcons.of(m.name), x + 17, y + 17, 12, Colors.mix(t.text2, t.accentText, k));
		// Nome e categoria
		ui.text(Type.BODY_SEMI, ui.fit(Type.BODY_SEMI, m.name, w - 76), x + 41, y + 12, t.text);
		ui.text(Type.CAPTION, ModuleIcons.label(m.category), x + 41, y + 24, t.text3);
		// Favorito
		boolean fav = cs().favorites.contains(m.name);
		if (kit.iconButton(id + ":fav", fav ? "star-filled" : "star", x + w - 28, y + 9, 16, fav ? "Tirar dos favoritos" : "Marcar como favorito", fav)) {
			if (fav) cs().favorites.remove(m.name);
			else cs().favorites.add(m.name);
			cfg().markDirty();
		}
		// Descrição
		float dy = y + 42;
		for (String line : ui.wrap(Type.LABEL, m.description, w - 24, 2)) {
			ui.text(Type.LABEL, line, x + 12, dy, t.text2);
			dy += Type.LABEL.lineHeight;
		}
		// Rodapé: tecla, opções, toggle
		float fy = y + h - 22;
		String key = m.key < 0 ? "Sem tecla" : Keys.name(m.key);
		float kw = ui.width(Type.CAPTION, key) + 12;
		ui.round(x + 12, fy + 1, kw, 12, Theme.R_SM, m.key < 0 ? 0x00000000 : t.surface2);
		ui.ring(x + 12, fy + 1, kw, 12, Theme.R_SM, m.key < 0 ? Colors.fade(t.border, 0.8f) : t.borderStrong);
		ui.textMid(Type.CAPTION, key, x + 18, fy + 1, 12, m.key < 0 ? t.text3 : t.text2);
		float bx = x + 12 + kw + 4;
		if (m.holdKey && m.key >= 0) bx += kit.badge("Segurar", t.text2, t.surface2, bx, fy + 2) + 4;
		if (kit.iconButton(id + ":opts", "sliders-horizontal", x + w - 12 - Kit.TOGGLE_W - 22, fy - 1, 16, "Opções", drawer == m)) openDrawer(m);
		if (kit.toggle(id + ":tg", on, x + w - 12 - Kit.TOGGLE_W, fy + 1)) m.toggle();

		if (ui.clicked(id, hovered) || ui.keyActivated(id)) {
			kit.click();
			openDrawer(m);
		}
		if (ui.rightClicked(hovered)) {
			kit.click();
			m.toggle();
		}
		ui.tooltip(id, hovered && on, m.getInfo().isEmpty() ? "Ligado" : "Ligado · " + m.getInfo());
		ui.popAlpha();
	}

	// ---------------------------------------------------------------- painel lateral

	private void drawDrawer(float x, float y, float w, float h) {
		Module m = drawer != null ? drawer : drawerClosing;
		float k = ui.anim("drawer", drawer != null ? 1 : 0, Theme.BASE);
		if (m == null) return;
		if (drawer == null && k < 0.02f) {
			drawerClosing = null;
			return;
		}
		Theme t = t();
		ui.layer = 3;
		float gap = 10;
		float dw = Math.min(290, w - 60);
		float dx = x + w - gap - (dw + gap) * k;
		if (Anim.motion != Anim.Motion.FULL) dx = x + w - gap - dw;
		ui.pushAlpha(Anim.motion == Anim.Motion.FULL ? 1 : k);
		// Fundo escurecido sobre a grade
		ui.block("drawer:scrim", x, y, w, h);
		ui.rect(x, y, w, h, Colors.fade(t.scrim, 0.6f * k));
		if (ui.pressed[0] && !ui.mouseIn(dx, y + gap, dw, h - 2 * gap) && drawer != null && kit.openDropdown == null) closeDrawer();

		// Painel flutuante com cantos arredondados
		y += gap;
		h -= 2 * gap;
		ui.block("drawer:panel", dx, y, dw, h);
		ui.shadow(dx, y, dw, h, 20, 6, t.dark ? 0.6f : 0.18f);
		ui.round(dx, y, dw, h, Theme.R_XL, t.surface);
		ui.ring(dx, y, dw, h, Theme.R_XL, t.borderStrong);

		float px = dx + 16, pw = dw - 32;
		// Cabeçalho fixo
		boolean on = m.isEnabled();
		ui.round(px, y + 16, 26, 26, Theme.R_MD, on ? t.accentFill : t.surface2);
		if (on) ui.ring(px, y + 16, 26, 26, Theme.R_MD, t.accentBorder);
		ui.icon(ModuleIcons.of(m.name), px + 6, y + 22, 14, on ? t.accentText : t.text2);
		ui.text(Type.TITLE, ui.fit(Type.TITLE, m.name, pw - 60), px + 34, y + 16, t.text);
		ui.text(Type.CAPTION, ModuleIcons.label(m.category) + (on ? " · Ligado" : " · Desligado"), px + 34, y + 30, on ? t.accentText : t.text3);
		if (kit.iconButton("drawer:close", "x", px + pw - 18, y + 16, 18, "Fechar (Esc)", false)) closeDrawer();
		float hy = y + 52;
		ui.hline(dx + 1, hy, dw - 2, t.divider);

		// Conteúdo com rolagem
		float sy = hy + 1, sh = h - (hy - y) - 46;
		ui.pushClip(dx, sy, dw, sh);
		float off = ui.scroll("drawer");
		float cy = sy + 12 - off;
		for (String line : ui.wrap(Type.BODY, m.description, pw, 3)) {
			ui.text(Type.BODY, line, px, cy, t.text2);
			cy += Type.BODY.lineHeight;
		}
		cy += 8;
		// Ligado
		float rh = drawerRow("Ligado", on ? "O módulo está funcionando agora." : "Ligue para usar.", px, cy, pw - Kit.TOGGLE_W - 8, false, null, "on");
		if (kit.toggle("drawer:on", on, px + pw - Kit.TOGGLE_W, cy + (rh - Kit.TOGGLE_H) / 2)) m.toggle();
		cy += rh + 4;

		if (!m.settings.isEmpty()) {
			ui.text(Type.CAPTION, "OPÇÕES", px, cy + 4, t.text3);
			cy += 18;
			for (Setting<?> s : m.settings) cy += settingControl(m, s, px, cy, pw) + 4;
		}

		ui.text(Type.CAPTION, "ATALHO", px, cy + 4, t.text3);
		cy += 18;
		float kbw = Math.min(130, pw * 0.55f);
		rh = drawerRow("Tecla", "Backspace remove a tecla.", px, cy, pw - kbw, false, null, "key");
		int nk = kit.keybind("drawer:key:" + m.name, m.key, px + pw - kbw, cy + (rh - Theme.CONTROL_H) / 2, kbw);
		if (nk != Kit.KEEP) {
			m.key = nk;
			cfg().markDirty();
		}
		cy += rh + 4;
		float segW = 130;
		rh = drawerRow("Modo da tecla", m.holdKey ? "Liga só enquanto a tecla está apertada." : "Cada toque liga ou desliga.", px, cy, pw - segW, false, null, "hold");
		int hold = kit.segmented("drawer:hold:" + m.name, new String[] {"Alternar", "Segurar"}, m.holdKey ? 1 : 0, px + pw - segW, cy + (rh - Theme.CONTROL_H) / 2, segW);
		if ((hold == 1) != m.holdKey) {
			m.holdKey = hold == 1;
			cfg().markDirty();
		}
		cy += rh + 4;

		ui.text(Type.CAPTION, "EXIBIÇÃO", px, cy + 4, t.text3);
		cy += 18;
		rh = drawerRow("Mostrar no HUD", "Aparece na lista de módulos ligados.", px, cy, pw - Kit.TOGGLE_W - 8, false, null, "hud");
		if (kit.toggle("drawer:hud", m.showInHud, px + pw - Kit.TOGGLE_W, cy + (rh - Kit.TOGGLE_H) / 2)) {
			m.showInHud = !m.showInHud;
			cfg().markDirty();
		}
		cy += rh + 4;
		boolean fav = cs().favorites.contains(m.name);
		rh = drawerRow("Favorito", "Fica nos atalhos rápidos do Painel.", px, cy, pw - Kit.TOGGLE_W - 8, false, null, "fav");
		if (kit.toggle("drawer:fav", fav, px + pw - Kit.TOGGLE_W, cy + (rh - Kit.TOGGLE_H) / 2)) {
			if (fav) cs().favorites.remove(m.name);
			else cs().favorites.add(m.name);
			cfg().markDirty();
		}
		cy += rh + 12;
		ui.popClip();
		ui.scrollArea("drawer", dx, sy, dw, sh, cy + off - sy);

		// Rodapé fixo
		float fy = y + h - 46;
		ui.hline(dx + 1, fy, dw - 2, t.divider);
		boolean anyChanged = false;
		for (Setting<?> s : m.settings) if (!s.isDefault()) anyChanged = true;
		float rw = kit.buttonWidth("Restaurar padrões", "rotate-ccw", false);
		if (kit.button("drawer:reset", "Restaurar padrões", "rotate-ccw", Kit.Btn.GHOST, px, fy + 14, rw, Theme.CONTROL_H, anyChanged, false)) {
			for (Setting<?> s : m.settings) s.reset();
			cfg().markDirty();
			Toasts.success("Opções restauradas", m.name);
		}
		float dw2 = kit.buttonWidth("Pronto", null, false);
		if (kit.button("drawer:done", "Pronto", null, Kit.Btn.PRIMARY, px + pw - dw2, fy + 14, dw2)) closeDrawer();
		ui.popAlpha();
		ui.layer = 0;
	}

	/** w = largura disponível para o texto (o controle fica à direita, fora dela). */
	private float drawerRow(String title, String desc, float x, float y, float w, boolean reset, Runnable onReset, String id) {
		Theme t = t();
		float textW = w - 8;
		ui.text(Type.BODY_SEMI, ui.fit(Type.BODY_SEMI, title, textW), x, y + 4, t.text);
		if (desc != null) ui.text(Type.LABEL, ui.fit(Type.LABEL, desc, textW), x, y + 16, t.text2);
		return desc != null ? 30 : 20;
	}

	private float settingControl(Module m, Setting<?> s, float x, float y, float w) {
		Theme t = t();
		String id = "drawer:s:" + m.name + ":" + s.name;
		boolean changed = !s.isDefault();
		float resetW = changed ? 18 : 0;
		if (s instanceof BoolSetting b) {
			float h = 22;
			ui.textMid(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, s.name, w - 60), x, y, h, t.text);
			if (changed && kit.iconButton(id + ":reset", "rotate-ccw", x + w - Kit.TOGGLE_W - 20, y + 3, 16, "Restaurar padrão", false)) {
				s.reset();
				cfg().markDirty();
			}
			if (kit.toggle(id, b.isOn(), x + w - Kit.TOGGLE_W, y + (h - Kit.TOGGLE_H) / 2)) {
				b.toggle();
				cfg().markDirty();
			}
			return h;
		}
		if (s instanceof NumberSetting n) {
			String value = n.display().substring(n.name.length() + 2);
			ui.text(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, s.name, w - 80), x, y + 2, t.text);
			float vw = ui.width(Type.BODY_SEMI, value) + 10;
			ui.round(x + w - vw, y, vw, 13, Theme.R_SM, t.surface2);
			ui.textMid(Type.BODY_SEMI, value, x + w - vw + 5, y, 13, t.text);
			if (changed && kit.iconButton(id + ":reset", "rotate-ccw", x + w - vw - 18, y - 1, 15, "Restaurar padrão", false)) {
				s.reset();
				cfg().markDirty();
			}
			int steps = (int) Math.round((n.max - n.min) / n.step);
			float nf = kit.slider(id, (float) n.fraction(), x + 2, y + 16, w - 4, steps <= 200 ? steps : 0);
			if (Math.abs(nf - n.fraction()) > 1e-6) {
				n.setFraction(nf);
				cfg().markDirty();
			}
			return 30;
		}
		if (s instanceof ModeSetting mo) {
			String[] opts = mo.modes.toArray(new String[0]);
			int sel = Math.max(0, mo.modes.indexOf(mo.get()));
			float optsW = 0;
			for (String o : opts) optsW += ui.width(Type.BODY_MEDIUM, o) + 14;
			ui.text(Type.BODY_MEDIUM, s.name, x, y + 2, t.text);
			if (changed && kit.iconButton(id + ":reset", "rotate-ccw", x + w - 15, y - 1, 15, "Restaurar padrão", false)) {
				s.reset();
				cfg().markDirty();
			}
			int ns;
			if (opts.length <= 4 && optsW <= w) ns = kit.segmented(id, opts, sel, x, y + 14, w);
			else ns = kit.dropdown(id, opts, sel, x, y + 14, w);
			if (ns != sel) {
				mo.set(opts[ns]);
				cfg().markDirty();
			}
			return 32;
		}
		ui.text(Type.BODY, s.display(), x, y, t.text2);
		return 14 + resetW * 0;
	}
}
