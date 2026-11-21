package com.basalto.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.basalto.Basalto;
import com.basalto.config.ClientSettings;
import com.basalto.config.ConfigManager;
import com.basalto.hud.Notifications;
import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;
import com.basalto.setting.ModeSetting;
import com.basalto.setting.NumberSetting;
import com.basalto.setting.Setting;
import com.basalto.util.Keys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * O menu do client (ClickGUI). Abre com a tecla do menu (padrão Right Shift).
 * Um painel por categoria + o painel "Client" com cor, transparência,
 * desfoque, tecla do menu, elementos do HUD e perfis de config.
 */
public final class ClickGuiScreen extends Screen {
	private static final int W = 118;
	private static final int HEAD = 16;
	private static final int ROW = 14;
	private static final int SROW = 12;

	/** Módulos com as opções abertas (fica lembrado enquanto o jogo estiver aberto). */
	private static final Set<String> EXPANDED = new HashSet<>();
	private static String search = "";

	private final Minecraft mc = Minecraft.getInstance();
	private final List<Panel> panels = new ArrayList<>();
	private final List<Hit> hits = new ArrayList<>();

	private Module listeningModule;
	private boolean listeningMenu;
	private Panel dragging;
	private int dragOffX, dragOffY;
	private Hit activeSlider;

	private interface RowDraw {
		void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hover);
	}

	private interface Click {
		void on(int button, double relX, int width);
	}

	private interface Drag {
		void on(double relX, int width);
	}

	private record Row(int h, RowDraw draw, Click click, Drag drag) {}

	private record Hit(int x1, int y1, int x2, int y2, Click click, Drag drag) {
		boolean inside(double mx, double my) {
			return mx >= x1 && mx < x2 && my >= y1 && my < y2;
		}
	}

	private static final class Panel {
		final String id;
		final Category category;
		int x, y, scroll;
		int bodyTop, bodyH;

		Panel(String id, Category category) {
			this.id = id;
			this.category = category;
		}
	}

	public ClickGuiScreen() {
		super(Component.literal("Basalto"));
	}

	private ConfigManager cfg() {
		return Basalto.config();
	}

	private ClientSettings cs() {
		return Basalto.config().client;
	}

	@Override
	protected void init() {
		if (!panels.isEmpty()) return;
		int i = 0;
		int x = 6, y = 30;
		for (Category c : Category.values()) {
			panels.add(placed(new Panel(c.title, c), x, y));
			x += W + 6;
			if (x + W > width) {
				x = 6;
				y += 170;
			}
			i++;
		}
		panels.add(placed(new Panel("Client", null), x, y));
	}

	private Panel placed(Panel p, int defX, int defY) {
		int[] saved = cfg().panelPos.get(p.id);
		p.x = saved != null ? saved[0] : defX;
		p.y = saved != null ? saved[1] : defY;
		p.x = Math.max(0, Math.min(p.x, width - W));
		p.y = Math.max(0, Math.min(p.y, height - HEAD));
		return p;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		cfg().save();
		super.onClose();
	}

	// ------------------------------------------------------------ desenho

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		if (cs().blur) super.extractBackground(g, mouseX, mouseY, delta);
		else g.fill(0, 0, width, height, 0x55000000);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		hits.clear();
		drawTopBar(g);
		for (Panel p : panels) drawPanel(g, p, mouseX, mouseY);
		drawFooter(g);
	}

	private void drawTopBar(GuiGraphicsExtractor g) {
		Font f = mc.font;
		int accent = cs().accent;
		int active = 0;
		for (Module m : Basalto.modules().all()) if (m.isEnabled()) active++;
		String ver = "v" + Basalto.VERSION + " · 26.1.2";
		String query = "Buscar: " + search + ((System.currentTimeMillis() / 500) % 2 == 0 ? "_" : " ");
		String count = active + "/" + Basalto.modules().all().size() + " ativos";
		int w = 16 + f.width(Basalto.NAME) + 6 + f.width(ver) + 14 + Math.max(110, f.width(query) + 10) + 10 + f.width(count) + 8;
		int x = (width - w) / 2;
		int y = 6;
		g.fill(x, y, x + w, y + 18, cs().panelColor());
		Draw.border(g, x, y, x + w, y + 18, 0x80262A33);
		Draw.cube(g, x + 4, y + 3, 12, accent);
		int cx = x + 20;
		g.text(f, Basalto.NAME, cx, y + 5, 0xFFF3F4F6, false);
		cx += f.width(Basalto.NAME) + 6;
		g.text(f, ver, cx, y + 5, Draw.TEXT_DIM, false);
		cx += f.width(ver) + 14;
		int fw = Math.max(110, f.width(query) + 10);
		g.fill(cx, y + 2, cx + fw, y + 16, Draw.FIELD);
		Draw.border(g, cx, y + 2, cx + fw, y + 16, search.isEmpty() ? Draw.BORDER : accent);
		g.text(f, query, cx + 4, y + 5, search.isEmpty() ? Draw.TEXT_DIM : Draw.TEXT, false);
		cx += fw + 10;
		g.text(f, count, cx, y + 5, Draw.TEXT_DIM, false);
	}

	private void drawFooter(GuiGraphicsExtractor g) {
		Font f = mc.font;
		String tip = listeningModule != null || listeningMenu
			? "Aperte a nova tecla · Backspace remove · Esc cancela"
			: "Clique: liga/desliga · Direito: opções · Meio: tecla · Digite para buscar · " + Keys.name(cs().menuKey) + " fecha";
		int w = f.width(tip) + 16;
		int x = (width - w) / 2;
		int y = height - 16;
		g.fill(x, y, x + w, y + 13, 0xCC0F1116);
		g.text(f, tip, x + 8, y + 3, listeningModule != null || listeningMenu ? cs().accent : Draw.TEXT, false);
	}

	private void drawPanel(GuiGraphicsExtractor g, Panel p, int mx, int my) {
		Font f = mc.font;
		List<Row> rows = p.category != null ? moduleRows(p.category) : clientRows();

		// cabeçalho
		g.fill(p.x, p.y, p.x + W, p.y + HEAD, Draw.HEADER);
		g.fill(p.x, p.y + HEAD - 1, p.x + W, p.y + HEAD, cs().accent);
		g.text(f, p.category != null ? p.category.title : "Client", p.x + 6, p.y + 4, 0xFFF3F4F6, false);
		if (p.category != null) {
			int on = 0, total = 0;
			for (Module m : Basalto.modules().inCategory(p.category)) {
				total++;
				if (m.isEnabled()) on++;
			}
			String c = on + "/" + total;
			g.text(f, c, p.x + W - 6 - f.width(c), p.y + 4, Draw.TEXT_DIM, false);
		}

		// corpo com rolagem
		int content = 4;
		for (Row r : rows) content += r.h;
		int top = p.y + HEAD;
		int maxBody = Math.max(ROW * 3, height - top - 20);
		int bodyH = Math.min(content, maxBody);
		p.scroll = Math.max(0, Math.min(p.scroll, content - bodyH));
		p.bodyTop = top;
		p.bodyH = bodyH;

		g.fill(p.x, top, p.x + W, top + bodyH, cs().panelColor());
		g.enableScissor(p.x, top, p.x + W, top + bodyH);
		int cy = top + 2 - p.scroll;
		for (Row r : rows) {
			if (cy + r.h > top && cy < top + bodyH) {
				boolean hover = mx >= p.x && mx < p.x + W && my >= Math.max(cy, top) && my < Math.min(cy + r.h, top + bodyH);
				r.draw.draw(g, p.x, cy, W, r.h, hover);
				if (r.click != null || r.drag != null)
					hits.add(new Hit(p.x, Math.max(cy, top), p.x + W, Math.min(cy + r.h, top + bodyH), r.click, r.drag));
			}
			cy += r.h;
		}
		g.disableScissor();
		if (content > bodyH) {
			int barH = Math.max(10, bodyH * bodyH / content);
			int barY = top + (bodyH - barH) * p.scroll / Math.max(1, content - bodyH);
			g.fill(p.x + W - 2, barY, p.x + W, barY + barH, 0x80FFFFFF);
		}
		Draw.border(g, p.x, p.y, p.x + W, top + bodyH, 0x80262A33);
	}

	// ------------------------------------------------------------ linhas dos módulos

	private List<Row> moduleRows(Category c) {
		List<Row> rows = new ArrayList<>();
		String q = search.trim().toLowerCase(Locale.ROOT);
		for (Module m : Basalto.modules().inCategory(c)) {
			if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)) continue;
			boolean open = EXPANDED.contains(m.name);
			rows.add(new Row(ROW, (g, x, y, w, h, hover) -> {
				Font f = mc.font;
				boolean on = m.isEnabled();
				if (on) g.fill(x + 2, y + 1, x + w - 2, y + h - 1, cs().accent);
				else if (hover) g.fill(x + 2, y + 1, x + w - 2, y + h - 1, Draw.HOVER);
				g.text(f, m.name, x + 6, y + 3, on ? Draw.TEXT_DARK : Draw.TEXT, false);
				String right = listeningModule == m ? "..." : (m.key >= 0 ? Keys.name(m.key) : (open ? "-" : (m.settings.isEmpty() ? "" : "+")));
				g.text(f, right, x + w - 6 - f.width(right), y + 3, on ? Draw.TEXT_DARK : Draw.TEXT_DIM, false);
			}, (button, relX, width) -> {
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) m.toggle();
				else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
					if (!EXPANDED.remove(m.name)) EXPANDED.add(m.name);
				} else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) listeningModule = m;
			}, null));
			if (open) {
				for (Setting<?> s : m.settings) rows.add(settingRow(s));
				rows.add(new Row(SROW, (g, x, y, w, h, hover) -> {
					String t = "Tecla: " + (listeningModule == m ? "aperte..." : Keys.name(m.key));
					g.text(mc.font, t, x + 9, y + 2, listeningModule == m ? cs().accent : Draw.TEXT_DIM, false);
				}, (button, relX, width) -> listeningModule = m, null));
				rows.add(boolRow("Mostrar no HUD", () -> m.showInHud, () -> m.showInHud = !m.showInHud));
				rows.add(new Row(3, (g, x, y, w, h, hover) -> g.fill(x + 6, y + 1, x + w - 6, y + 2, 0x30FFFFFF), null, null));
			}
		}
		if (rows.isEmpty())
			rows.add(new Row(ROW, (g, x, y, w, h, hover) -> g.text(mc.font, "Nada encontrado", x + 6, y + 3, Draw.TEXT_DIM, false), null, null));
		return rows;
	}

	private Row settingRow(Setting<?> s) {
		if (s instanceof BoolSetting b) return boolRow(b.name, b::isOn, b::toggle);
		if (s instanceof NumberSetting n)
			return sliderRow(n::display, n::fraction, n::setFraction);
		if (s instanceof ModeSetting mo) {
			return new Row(SROW, (g, x, y, w, h, hover) -> {
				g.text(mc.font, mo.name + ":", x + 9, y + 2, Draw.TEXT_DIM, false);
				String v = "< " + mo.get() + " >";
				g.text(mc.font, v, x + w - 6 - mc.font.width(v), y + 2, cs().accent, false);
			}, (button, relX, width) -> {
				mo.cycle(button != GLFW.GLFW_MOUSE_BUTTON_RIGHT);
				cfg().markDirty();
			}, null);
		}
		return new Row(SROW, (g, x, y, w, h, hover) -> g.text(mc.font, s.display(), x + 9, y + 2, Draw.TEXT_DIM, false), null, null);
	}

	private Row boolRow(String label, java.util.function.BooleanSupplier get, Runnable toggle) {
		return new Row(SROW, (g, x, y, w, h, hover) -> {
			boolean on = get.getAsBoolean();
			int bx = x + 9, by = y + 2;
			if (on) g.fill(bx, by, bx + 8, by + 8, cs().accent);
			else Draw.border(g, bx, by, bx + 8, by + 8, Draw.TEXT_DIM);
			g.text(mc.font, label, bx + 12, y + 2, hover ? Draw.TEXT : Draw.TEXT_DIM, false);
		}, (button, relX, width) -> {
			toggle.run();
			cfg().markDirty();
		}, null);
	}

	private Row sliderRow(java.util.function.Supplier<String> label, java.util.function.DoubleSupplier fraction,
		java.util.function.DoubleConsumer setFraction) {
		Drag drag = (relX, width) -> {
			setFraction.accept((relX - 9) / (double) (width - 18));
			cfg().markDirty();
		};
		return new Row(SROW + 3, (g, x, y, w, h, hover) -> {
			g.text(mc.font, label.get(), x + 9, y + 1, hover ? Draw.TEXT : Draw.TEXT_DIM, false);
			int tx1 = x + 9, tx2 = x + w - 9, ty = y + h - 4;
			g.fill(tx1, ty, tx2, ty + 2, Draw.BORDER);
			int fx = tx1 + (int) Math.round((tx2 - tx1) * fraction.getAsDouble());
			g.fill(tx1, ty, fx, ty + 2, cs().accent);
			g.fill(fx - 1, ty - 1, fx + 1, ty + 3, 0xFFF3F4F6);
		}, (button, relX, width) -> drag.on(relX, width), drag);
	}

	private Row labelRow(String text) {
		return new Row(ROW, (g, x, y, w, h, hover) -> {
			g.text(mc.font, text, x + 6, y + 4, cs().accent, false);
		}, null, null);
	}

	// ------------------------------------------------------------ painel Client

	private List<Row> clientRows() {
		List<Row> rows = new ArrayList<>();
		ClientSettings cs = cs();

		rows.add(labelRow("Cor de destaque"));
		rows.add(new Row(ROW, (g, x, y, w, h, hover) -> {
			for (int i = 0; i < ClientSettings.ACCENTS.length; i++) {
				int sx = x + 7 + i * 18;
				g.fill(sx, y + 1, sx + 14, y + 12, ClientSettings.ACCENTS[i]);
				if (ClientSettings.ACCENTS[i] == cs.accent) Draw.border(g, sx - 1, y, sx + 15, y + 13, 0xFFFFFFFF);
			}
		}, (button, relX, width) -> {
			int i = (int) ((relX - 7) / 18);
			if (i >= 0 && i < ClientSettings.ACCENTS.length) {
				cs.accent = ClientSettings.ACCENTS[i];
				cfg().markDirty();
			}
		}, null));
		rows.add(sliderRow(() -> "Opacidade: " + cs.opacity + "%", () -> (cs.opacity - 30) / 70.0,
			f -> cs.opacity = (int) Math.round(30 + 70 * Math.max(0, Math.min(1, f)))));
		rows.add(boolRow("Desfoque no fundo", () -> cs.blur, () -> cs.blur = !cs.blur));
		rows.add(new Row(SROW, (g, x, y, w, h, hover) -> {
			String t = "Tecla do menu: " + (listeningMenu ? "aperte..." : Keys.name(cs.menuKey));
			g.text(mc.font, t, x + 9, y + 2, listeningMenu ? cs.accent : (hover ? Draw.TEXT : Draw.TEXT_DIM), false);
		}, (button, relX, width) -> listeningMenu = true, null));

		rows.add(labelRow("HUD"));
		for (String id : ClientSettings.HUD_LABELS.keySet()) {
			String label = ClientSettings.HUD_LABELS.get(id);
			rows.add(boolRow(label, () -> cs.hud(id), () -> cs.hud.put(id, !cs.hud(id))));
		}

		rows.add(labelRow("Configs"));
		for (String name : new ArrayList<>(cfg().profiles.keySet())) {
			rows.add(new Row(ROW, (g, x, y, w, h, hover) -> {
				boolean active = name.equals(cfg().activeProfile);
				g.fill(x + 6, y + 1, x + w - 6, y + h - 1, Draw.FIELD);
				Draw.border(g, x + 6, y + 1, x + w - 6, y + h - 1, active ? cs.accent : (hover ? Draw.TEXT_DIM : Draw.BORDER));
				g.text(mc.font, name, x + 10, y + 3, active ? cs.accent : Draw.TEXT, false);
				if (active) g.text(mc.font, "ativo", x + w - 10 - mc.font.width("ativo"), y + 3, Draw.TEXT_DIM, false);
			}, (button, relX, width) -> {
				cfg().loadProfile(name);
				Notifications.push("Config " + name + " carregada", true);
			}, null));
		}
		rows.add(new Row(ROW + 2, (g, x, y, w, h, hover) -> {
			g.fill(x + 6, y + 2, x + w - 6, y + h - 1, hover ? Draw.shade(cs.accent, 1.15f) : cs.accent);
			String t = "Salvar em " + cfg().activeProfile;
			g.text(mc.font, t, x + (w - mc.font.width(t)) / 2, y + 5, Draw.TEXT_DARK, false);
		}, (button, relX, width) -> {
			cfg().saveProfile(cfg().activeProfile);
			Notifications.push("Config " + cfg().activeProfile + " salva", true);
		}, null));
		return rows;
	}

	// ------------------------------------------------------------ entrada

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mx = event.x(), my = event.y();
		int button = event.button();
		listeningMenu = false;
		listeningModule = null;

		for (int i = panels.size() - 1; i >= 0; i--) {
			Panel p = panels.get(i);
			if (mx >= p.x && mx < p.x + W && my >= p.y && my < p.y + HEAD) {
				if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
					dragging = p;
					dragOffX = (int) mx - p.x;
					dragOffY = (int) my - p.y;
					panels.remove(i);
					panels.add(p);
				}
				return true;
			}
		}
		for (int i = hits.size() - 1; i >= 0; i--) {
			Hit h = hits.get(i);
			if (!h.inside(mx, my)) continue;
			if (h.click != null) h.click.on(button, mx - h.x1, h.x2 - h.x1);
			if (h.drag != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) activeSlider = h;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging != null) {
			dragging.x = Math.max(0, Math.min(width - W, (int) event.x() - dragOffX));
			dragging.y = Math.max(0, Math.min(height - HEAD, (int) event.y() - dragOffY));
			return true;
		}
		if (activeSlider != null) {
			activeSlider.drag.on(event.x() - activeSlider.x1, activeSlider.x2 - activeSlider.x1);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			cfg().panelPos.put(dragging.id, new int[] {dragging.x, dragging.y});
			cfg().markDirty();
		}
		dragging = null;
		activeSlider = null;
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
		for (int i = panels.size() - 1; i >= 0; i--) {
			Panel p = panels.get(i);
			if (mx >= p.x && mx < p.x + W && my >= p.bodyTop && my < p.bodyTop + p.bodyH) {
				p.scroll -= (int) Math.round(vertical * ROW * 2);
				return true;
			}
		}
		return super.mouseScrolled(mx, my, horizontal, vertical);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if (listeningModule != null) {
			if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) listeningModule.key = -1;
			else if (key != GLFW.GLFW_KEY_ESCAPE) listeningModule.key = key;
			listeningModule = null;
			cfg().markDirty();
			return true;
		}
		if (listeningMenu) {
			if (key != GLFW.GLFW_KEY_ESCAPE && key != GLFW.GLFW_KEY_BACKSPACE) cs().menuKey = key;
			listeningMenu = false;
			cfg().markDirty();
			return true;
		}
		if (key == cs().menuKey) {
			onClose();
			return true;
		}
		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
			return true;
		}
		char c = Keys.typedChar(key);
		if (c != 0 && key != GLFW.GLFW_KEY_ESCAPE) {
			if (search.length() < 16) search += c;
			return true;
		}
		return super.keyPressed(event);
	}
}
