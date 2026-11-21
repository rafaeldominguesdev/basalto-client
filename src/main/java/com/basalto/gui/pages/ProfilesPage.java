package com.basalto.gui.pages;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.basalto.gui.App;
import com.basalto.gui.Modal;
import com.basalto.gui.Toasts;
import com.basalto.hud.Activity;
import com.basalto.ui.Anim;
import com.basalto.ui.Colors;
import com.basalto.ui.Kit;
import com.basalto.ui.Theme;
import com.basalto.ui.Type;

import net.minecraft.util.Util;

/** Perfis: criar, carregar, salvar, duplicar, renomear, exportar, importar, padrão e excluir. */
public final class ProfilesPage extends PageBase {
	private static final String SCROLL = "page:profiles";
	private static final SimpleDateFormat DATE = new SimpleDateFormat("dd/MM 'às' HH:mm", Locale.of("pt", "BR"));

	public ProfilesPage(App app) {
		super(app);
	}

	@Override
	public void draw(float x, float y, float w, float h) {
		Theme t = t();
		float off = ui.scroll(SCROLL);
		float px = x + PAD, pw = w - PAD * 2;
		float top = y + 18 - off;

		float nw = kit.buttonWidth("Novo perfil", "plus", false);
		float iw = kit.buttonWidth("Importar", "upload", false);
		float cy = header("Perfis", "Cada perfil guarda quais módulos ficam ligados, as teclas e as opções.", px, top, pw - nw - iw - 18);
		if (kit.button("prof:new", "Novo perfil", "plus", Kit.Btn.PRIMARY, px + pw - nw, top + 4, nw)) newProfile();
		if (kit.button("prof:import", "Importar", "upload", Kit.Btn.SECONDARY, px + pw - nw - 6 - iw, top + 4, iw)) importModal();

		// Faixa de alterações não salvas
		boolean changed = cfg().activeProfileChanged(app.frame);
		float bannerK = ui.anim("prof:banner", changed ? 1 : 0, Theme.BASE);
		if (bannerK > 0.01f) {
			float bh = 40 * bannerK;
			ui.pushAlpha(bannerK);
			ui.pushClip(px - 2, cy - 2, pw + 4, bh + 4);
			ui.round(px, cy, pw, 30, Theme.R_LG, t.warningFill);
			ui.ring(px, cy, pw, 30, Theme.R_LG, Colors.withAlpha(t.warning, 0.3f));
			ui.icon("triangle-alert", px + 10, cy + 10, 10, t.warning);
			ui.text(Type.BODY_SEMI, "Alterações não salvas", px + 26, cy + 4, t.text);
			ui.text(Type.LABEL, ui.fit(Type.LABEL, "O estado atual dos módulos é diferente do perfil \"" + cfg().activeProfile + "\".", pw - 230),
				px + 26, cy + 16, t.text2);
			float sw = kit.buttonWidth("Salvar no perfil", "save", true);
			float dw = kit.buttonWidth("Descartar", null, true);
			if (kit.button("prof:save-banner", "Salvar no perfil", "save", Kit.Btn.PRIMARY, px + pw - 8 - sw, cy + 8, sw, 14, true, false)) save(cfg().activeProfile);
			if (kit.button("prof:discard", "Descartar", null, Kit.Btn.GHOST, px + pw - 14 - sw - dw, cy + 8, dw, 14, true, false)) {
				String n = cfg().activeProfile;
				app.openModal(Modal.confirm("rotate-ccw", "Descartar alterações?",
					"Os módulos voltam para o que está salvo em \"" + n + "\".", "Descartar", true, () -> {
						cfg().loadProfile(n);
						Toasts.success("Alterações descartadas", n);
					}));
			}
			ui.popClip();
			ui.popAlpha();
			cy += bh;
		}

		// Grade de perfis
		List<String> names = new ArrayList<>(cfg().profiles.keySet());
		float gap = 10;
		int cols = Math.max(1, (int) ((pw + gap) / (230 + gap)));
		float cw = (pw - gap * (cols - 1)) / cols;
		float chh = 98;
		for (int i = 0; i < names.size(); i++) {
			float gx = px + (i % cols) * (cw + gap);
			float gy = cy + (i / cols) * (chh + gap);
			profileCard(names.get(i), gx, gy, cw, chh, i);
		}
		cy += ((names.size() + cols - 1) / cols) * (chh + gap) + 8;

		// Pasta
		kit.card(px, cy, pw, 42, 0, 0);
		ui.round(px + 12, cy + 11, 20, 20, Theme.R_MD, t.surface2);
		ui.icon("folder-open", px + 17, cy + 16, 10, t.text2);
		ui.text(Type.BODY_SEMI, "Pasta de perfis exportados", px + 40, cy + 9, t.text);
		String path = cs().streamerMode ? "config/basalto/perfis" : cfg().profilesDir.toString();
		float ow = kit.buttonWidth("Abrir pasta", "external-link", true);
		ui.text(Type.LABEL, ui.fit(Type.LABEL, path, pw - ow - 70), px + 40, cy + 21, t.text2);
		if (kit.button("prof:folder", "Abrir pasta", "folder-open", Kit.Btn.SECONDARY, px + pw - 12 - ow, cy + 14, ow, 14, true, false)) openFolder();
		cy += 42 + 20;

		ui.scrollArea(SCROLL, x, y, w, h, cy + off - y);
	}

	private void profileCard(String name, float x, float y, float w, float h, int index) {
		Theme t = t();
		String id = "prof:" + name;
		boolean active = name.equals(cfg().activeProfile);
		boolean isDefault = name.equals(cfg().defaultProfile);
		float enter = ui.animFrom(id + ":in", 0, 1, Theme.SLOW + index * 25);
		ui.pushAlpha(enter);
		y += (1 - enter) * 6 * Anim.move();
		boolean hovered = ui.hover(id, x, y, w, h);
		float hv = ui.anim(id + ":h", hovered ? 1 : 0, Theme.FAST);
		float k = ui.anim(id + ":a", active ? 1 : 0, Theme.BASE);
		kit.card(x, y, w, h, hv, k);

		ui.round(x + 12, y + 12, 22, 22, Theme.R_MD, active ? t.accentFill : t.surface2);
		ui.icon("layers", x + 17, y + 17, 12, active ? t.accentText : t.text2);
		float bx = x + 41;
		ui.text(Type.TITLE, ui.fit(Type.TITLE, name, w - 120), bx, y + 11, t.text);
		float nameW = Math.min(ui.width(Type.TITLE, name), w - 120);
		float badgeX = bx + nameW + 6;
		if (active) badgeX += kit.badge("Ativo", t.accentText, t.accentFill, badgeX, y + 12) + 4;
		if (isDefault) kit.badge("Padrão", t.info, t.infoFill, badgeX, y + 12);
		int on = cfg().enabledIn(name);
		ui.text(Type.LABEL, on + (on == 1 ? " módulo ligado" : " módulos ligados"), bx, y + 25, t.text2);
		Long mod = cfg().profileModified.get(name);
		String when = mod == null ? "Sem data" : "Alterado em " + DATE.format(new Date(mod));
		ui.icon("clock", x + 12, y + 46, 8, t.text3);
		ui.text(Type.LABEL, when, x + 24, y + 45, t.text3);

		// Ações
		float fy = y + h - 28;
		ui.hline(x + 1, fy - 6, w - 2, t.divider);
		float ax = x + 12;
		if (active) {
			boolean changed = cfg().activeProfileChanged(app.frame);
			float sw = kit.buttonWidth("Salvar", "save", true);
			if (kit.button(id + ":save", "Salvar", "save", Kit.Btn.PRIMARY, ax, fy, sw, 15, changed, false)) save(name);
			ax += sw + 6;
		} else {
			float lw = kit.buttonWidth("Carregar", null, true);
			if (kit.button(id + ":load", "Carregar", null, Kit.Btn.SECONDARY, ax, fy, lw, 15, true, false)) {
				cfg().loadProfile(name);
				Activity.log(Activity.Kind.PROFILE, "layers", "Perfil " + name + " carregado");
				Toasts.success("Perfil carregado", name);
			}
			ax += lw + 6;
		}
		float ib = 16, gap = 2;
		float rx = x + w - 12 - ib;
		if (kit.iconButton(id + ":del", "trash-2", rx, fy - 0.5f, ib, cfg().profiles.size() <= 1 ? "O último perfil não pode ser excluído" : "Excluir", false)) {
			if (cfg().profiles.size() > 1) confirmDelete(name);
		}
		rx -= ib + gap;
		if (kit.iconButton(id + ":export", "download", rx, fy - 0.5f, ib, "Exportar", false)) export(name);
		rx -= ib + gap;
		if (kit.iconButton(id + ":rename", "pencil", rx, fy - 0.5f, ib, "Renomear", false)) rename(name);
		rx -= ib + gap;
		if (kit.iconButton(id + ":dup", "copy", rx, fy - 0.5f, ib, "Duplicar", false)) {
			String copy = cfg().duplicateProfile(name);
			if (copy != null) Toasts.success("Perfil duplicado", copy);
		}
		rx -= ib + gap;
		if (kit.iconButton(id + ":default", isDefault ? "star-filled" : "star", rx, fy - 0.5f, ib,
			isDefault ? "Carregado ao abrir o jogo (clique para tirar)" : "Carregar ao abrir o jogo", isDefault)) {
			cfg().setDefaultProfile(isDefault ? null : name);
			Toasts.success(isDefault ? "Sem perfil padrão" : "Perfil padrão definido",
				isDefault ? "O jogo abre como você deixou." : name + " carrega ao abrir o jogo.");
		}
		ui.popAlpha();
	}

	private void save(String name) {
		cfg().saveProfile(name);
		Activity.log(Activity.Kind.PROFILE, "save", "Perfil " + name + " salvo");
		Toasts.success("Perfil salvo", name);
	}

	private void newProfile() {
		app.openModal(Modal.prompt("plus", "Novo perfil", "Começa com o estado atual dos módulos.", "Nome", cfg().uniqueName("Perfil"),
			"Criar", s -> cfg().validateName(s, null), s -> {
				cfg().createProfile(s);
				Activity.log(Activity.Kind.PROFILE, "plus", "Perfil " + s.trim() + " criado");
				Toasts.success("Perfil criado", s.trim());
			}));
	}

	private void rename(String name) {
		app.openModal(Modal.prompt("pencil", "Renomear perfil", "", "Nome", name, "Renomear",
			s -> cfg().validateName(s, name), s -> {
				cfg().renameProfile(name, s);
				Toasts.success("Perfil renomeado", s.trim());
			}));
	}

	private void confirmDelete(String name) {
		app.openModal(Modal.confirm("trash-2", "Excluir \"" + name + "\"?",
			"O perfil some da lista. Exporte antes se quiser guardar uma cópia. Isso não pode ser desfeito.", "Excluir", true, () -> {
				cfg().deleteProfile(name);
				Activity.log(Activity.Kind.WARNING, "trash-2", "Perfil " + name + " excluído");
				Toasts.success("Perfil excluído", name);
			}));
	}

	private void export(String name) {
		try {
			Path f = cfg().exportProfile(name);
			Toasts.success("Perfil exportado", "perfis/" + f.getFileName());
		} catch (Exception e) {
			Toasts.error("Não deu pra exportar", e.getMessage());
		}
	}

	private void openFolder() {
		try {
			java.nio.file.Files.createDirectories(cfg().profilesDir);
			Util.getPlatform().openPath(cfg().profilesDir);
		} catch (Exception e) {
			Toasts.error("Não deu pra abrir a pasta", e.getMessage());
		}
	}

	private void importModal() {
		Modal m = new Modal();
		m.icon = "upload";
		m.title = "Importar perfil";
		m.body = "Coloque arquivos .json exportados pelo Basalto na pasta de perfis.";
		m.width = 330;
		m.cancelLabel = "Fechar";
		m.content = (app, x, y, w) -> {
			Theme t = t();
			List<Path> files = cfg().importable();
			if (files.isEmpty()) {
				ui.round(x, y, w, 50, Theme.R_MD, t.surface2);
				ui.textCenter(Type.BODY_SEMI, "Nenhum arquivo encontrado", x + w / 2, y + 12, t.text);
				ui.textCenter(Type.LABEL, "A pasta está vazia.", x + w / 2, y + 25, t.text2);
				float ow = kit.buttonWidth("Abrir pasta", "folder-open", true);
				if (kit.button("imp:open", "Abrir pasta", "folder-open", Kit.Btn.SECONDARY, x + (w - ow) / 2, y + 58, ow, 14, true, false)) openFolder();
				return 76;
			}
			float ry = y;
			int shown = 0;
			for (Path f : files) {
				if (shown++ >= 5) break;
				String fn = f.getFileName().toString();
				ui.round(x, ry, w, 24, Theme.R_MD, t.surface2);
				ui.icon("file-json", x + 7, ry + 7, 10, t.text2);
				ui.textMid(Type.BODY_MEDIUM, ui.fit(Type.BODY_MEDIUM, fn, w - 90), x + 22, ry, 24, t.text);
				float bw = kit.buttonWidth("Importar", null, true);
				if (kit.button("imp:" + fn, "Importar", null, Kit.Btn.SECONDARY, x + w - 5 - bw, ry + 5, bw, 14, true, false)) {
					try {
						String n = cfg().importProfile(f);
						Activity.log(Activity.Kind.PROFILE, "upload", "Perfil " + n + " importado");
						Toasts.success("Perfil importado", n);
						app.closeModal();
					} catch (Exception e) {
						Toasts.error("Não deu pra importar " + fn, e.getMessage());
					}
				}
				ry += 28;
			}
			float ow = kit.buttonWidth("Abrir pasta", "folder-open", true);
			if (kit.button("imp:open", "Abrir pasta", "folder-open", Kit.Btn.GHOST, x, ry + 2, ow, 14, true, false)) openFolder();
			return ry - y + 18;
		};
		app.openModal(m);
	}
}
