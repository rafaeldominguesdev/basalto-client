package com.basalto.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/**
 * Interface em modo imediato: cada frame desenha tudo e, no mesmo passo,
 * descobre o que está sob o mouse, o que foi clicado e quem tem o foco.
 *
 * Coordenadas em unidades da interface (float). Uma unidade vale {@link #p}
 * pixels de tela; tudo é arredondado para o pixel na hora de desenhar, então
 * contornos de 1 px e cantos arredondados ficam nítidos em qualquer tamanho.
 *
 * O elemento "quente" (sob o mouse) é o último registrado no frame anterior
 * que contém o mouse, assim o que está por cima sempre ganha.
 */
public final class Ui {
	private final Minecraft mc = Minecraft.getInstance();

	public GuiGraphicsExtractor g;
	public Theme t = Theme.of(true, Theme.ACCENTS[0]);
	/** Pixels de tela por unidade. */
	public float p = 2f;
	private int di = 4;
	private int guiScale = 1;
	/** Tamanho da tela em unidades. */
	public float W, H;
	public float mx, my;
	private float alpha = 1f;
	private final ArrayDeque<Float> alphaStack = new ArrayDeque<>();

	// ---------- entrada entre frames
	private final List<int[]> pendingMouse = new ArrayList<>();
	private double pendingWheel;
	private final List<KeyEvent> pendingKeys = new ArrayList<>();
	private final StringBuilder pendingChars = new StringBuilder();

	// ---------- entrada do frame atual
	public final boolean[] pressed = new boolean[3];
	public final boolean[] released = new boolean[3];
	public final boolean[] down = new boolean[3];
	private final List<KeyEvent> keys = new ArrayList<>();
	private final List<Boolean> keyUsed = new ArrayList<>();
	private String chars = "";

	// ---------- quem é quem
	private String hot, hotNext;
	public String active;
	public String focus;
	/** O foco veio do teclado (Tab): só então o anel de foco aparece. */
	public boolean keyboardNav;
	private final List<String> focusOrder = new ArrayList<>();
	private final List<Integer> focusLayer = new ArrayList<>();
	/** Camada atual: 0 = página, 1+ = modais e menus por cima. Só a camada mais alta recebe Tab. */
	public int layer;

	// ---------- recorte, rolagem, sobreposições
	private final ArrayDeque<float[]> clips = new ArrayDeque<>();
	private final Map<String, float[]> scrolls = new HashMap<>();
	private String scrollHot, scrollHotNext;
	private final List<Runnable> overlays = new ArrayList<>();

	// ---------- dica (tooltip)
	private String tipOwner, tipText;
	private long tipSince;
	private boolean tipShown;
	private CursorType cursor;

	// ================================================================ ciclo do frame

	/** Começa um frame. density = pixels por unidade (de Type.DENSITIES). */
	public void begin(GuiGraphicsExtractor graphics, Theme theme, float density) {
		g = graphics;
		t = theme;
		di = 0;
		for (int i = 0; i < Type.DENSITIES.length; i++) if (Type.DENSITIES[i] <= density + 0.001f) di = i;
		p = Type.DENSITIES[di];
		Window win = mc.getWindow();
		guiScale = Math.max(1, win.getGuiScale());
		W = win.getWidth() / p;
		H = win.getHeight() / p;
		mx = (float) (mc.mouseHandler.getScaledXPos(win) * guiScale / p);
		my = (float) (mc.mouseHandler.getScaledYPos(win) * guiScale / p);
		Anim.frame();

		for (int i = 0; i < 3; i++) pressed[i] = released[i] = false;
		for (int[] ev : pendingMouse) {
			int b = ev[0];
			if (b < 0 || b > 2) continue;
			if (ev[1] == 1) {
				pressed[b] = true;
				down[b] = true;
			} else {
				released[b] = true;
				down[b] = false;
			}
		}
		pendingMouse.clear();
		keys.clear();
		keyUsed.clear();
		keys.addAll(pendingKeys);
		for (int i = 0; i < keys.size(); i++) keyUsed.add(false);
		pendingKeys.clear();
		chars = pendingChars.toString();
		pendingChars.setLength(0);

		hot = hotNext;
		hotNext = null;
		scrollHot = scrollHotNext;
		scrollHotNext = null;
		if (pendingWheel != 0 && scrollHot != null) {
			float[] s = scrolls.get(scrollHot);
			if (s != null) s[0] = clamp(s[0] - (float) pendingWheel * 36f, 0, s[2]);
		}
		pendingWheel = 0;
		if (pressed[0] && (hot == null || !hot.equals(focus))) focus = null;
		focusOrder.clear();
		focusLayer.clear();
		layer = 0;
		alpha = 1f;
		alphaStack.clear();
		clips.clear();
		cursor = null;
		tipText = null;

		g.pose().pushMatrix();
		g.pose().scale(1f / guiScale, 1f / guiScale);
	}

	public void end() {
		flushOverlays();
		drawTooltip();
		// Tab percorre os elementos focáveis da camada mais alta.
		if (consumeKey(GLFW.GLFW_KEY_TAB)) {
			int top = 0;
			for (int l : focusLayer) top = Math.max(top, l);
			List<String> order = new ArrayList<>();
			for (int i = 0; i < focusOrder.size(); i++) if (focusLayer.get(i) == top) order.add(focusOrder.get(i));
			if (!order.isEmpty()) {
				boolean back = lastKeyHadShift;
				int i = order.indexOf(focus);
				i = i < 0 ? (back ? order.size() - 1 : 0) : Math.floorMod(i + (back ? -1 : 1), order.size());
				focus = order.get(i);
				keyboardNav = true;
			}
		}
		for (int i = 0; i < 3; i++) if (released[i] && !down[0]) active = null;
		if (cursor != null) g.requestCursor(cursor);
		g.pose().popMatrix();
	}

	private boolean lastKeyHadShift;

	// ---------- eventos vindos da Screen

	public void onMouse(int button, boolean press) {
		pendingMouse.add(new int[] {button, press ? 1 : 0});
	}

	public void onWheel(double amount) {
		pendingWheel += amount;
	}

	public void onKey(KeyEvent e) {
		pendingKeys.add(e);
	}

	public void onChar(int codepoint) {
		pendingChars.appendCodePoint(codepoint);
	}

	// ================================================================ teclado

	/** Consome a tecla se ela foi apertada neste frame. */
	public boolean consumeKey(int key) {
		for (int i = 0; i < keys.size(); i++) {
			if (!keyUsed.get(i) && keys.get(i).key() == key) {
				keyUsed.set(i, true);
				lastKeyHadShift = keys.get(i).hasShiftDown();
				return true;
			}
		}
		return false;
	}

	/** Consome a tecla com Ctrl (ou Cmd no Mac). */
	public boolean consumeShortcut(int key) {
		for (int i = 0; i < keys.size(); i++) {
			KeyEvent e = keys.get(i);
			if (!keyUsed.get(i) && e.key() == key && (e.hasControlDown() || (e.modifiers() & GLFW.GLFW_MOD_SUPER) != 0)) {
				keyUsed.set(i, true);
				return true;
			}
		}
		return false;
	}

	/** Primeira tecla ainda não usada (para capturar atalhos), ou -1. */
	public int takeAnyKey() {
		for (int i = 0; i < keys.size(); i++) {
			if (!keyUsed.get(i)) {
				keyUsed.set(i, true);
				return keys.get(i).key();
			}
		}
		return -1;
	}

	public boolean hasPendingKey(int key) {
		for (int i = 0; i < keys.size(); i++) if (!keyUsed.get(i) && keys.get(i).key() == key) return true;
		return false;
	}

	public String takeChars() {
		String c = chars;
		chars = "";
		return c;
	}

	// ================================================================ interação

	/** Registra uma área interativa. Devolve true se ela está sob o mouse (e por cima das outras). */
	public boolean hover(String id, float x, float y, float w, float h) {
		float[] c = clips.peek();
		float x0 = x, y0 = y, x1 = x + w, y1 = y + h;
		if (c != null) {
			x0 = Math.max(x0, c[0]);
			y0 = Math.max(y0, c[1]);
			x1 = Math.min(x1, c[2]);
			y1 = Math.min(y1, c[3]);
		}
		if (x1 <= x0 || y1 <= y0) return false;
		if (mx >= x0 && mx < x1 && my >= y0 && my < y1) hotNext = id;
		return id.equals(hot);
	}

	/** Bloqueia o que estiver por baixo (fundo de modal, painel). */
	public boolean block(String id, float x, float y, float w, float h) {
		return hover(id, x, y, w, h);
	}

	/** Botão esquerdo: aperta e solta em cima do mesmo elemento. */
	public boolean clicked(String id, boolean hovered) {
		if (hovered && pressed[0]) {
			active = id;
			keyboardNav = false;
		}
		return released[0] && id.equals(active) && hovered;
	}

	public boolean rightClicked(boolean hovered) {
		return hovered && pressed[1];
	}

	/** Elemento sendo segurado (arrastar slider, etc.). */
	public boolean holding(String id) {
		return id.equals(active) && down[0];
	}

	public boolean isPressing(String id, boolean hovered) {
		return hovered && id.equals(active) && down[0];
	}

	/** Entra na ordem do Tab. Devolve true se tem o foco. */
	public boolean focusable(String id) {
		focusOrder.add(id);
		focusLayer.add(layer);
		return id.equals(focus);
	}

	/** Enter ou espaço com o foco no elemento. */
	public boolean keyActivated(String id) {
		if (!id.equals(focus)) return false;
		return consumeKey(GLFW.GLFW_KEY_ENTER) || consumeKey(GLFW.GLFW_KEY_KP_ENTER) || consumeKey(GLFW.GLFW_KEY_SPACE);
	}

	public void hand() {
		cursor = CursorTypes.POINTING_HAND;
	}

	public void ibeam() {
		cursor = CursorTypes.IBEAM;
	}

	public boolean mouseIn(float x, float y, float w, float h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	/** Clique fora de uma área (fecha menus). */
	public boolean pressedOutside(float x, float y, float w, float h) {
		return (pressed[0] || pressed[1]) && !mouseIn(x, y, w, h);
	}

	// ================================================================ rolagem

	/** Valor atual (animado) da rolagem de uma área. */
	public float scroll(String id) {
		float[] s = scrolls.computeIfAbsent(id, k -> new float[3]);
		s[1] = Anim.to("scroll:" + id, s[0], Theme.FAST);
		return s[1];
	}

	public void scrollTo(String id, float value) {
		float[] s = scrolls.computeIfAbsent(id, k -> new float[3]);
		s[0] = clamp(value, 0, s[2]);
	}

	public void resetScroll(String id) {
		scrolls.remove(id);
		Anim.reset("scroll:" + id);
	}

	/** Fecha a área de rolagem: informa a altura do conteúdo e desenha a barra. */
	public void scrollArea(String id, float x, float y, float w, float h, float contentH) {
		float[] s = scrolls.computeIfAbsent(id, k -> new float[3]);
		s[2] = Math.max(0, contentH - h);
		s[0] = clamp(s[0], 0, s[2]);
		float[] c = clips.peek();
		float x0 = x, y0 = y, x1 = x + w, y1 = y + h;
		if (c != null) {
			x0 = Math.max(x0, c[0]);
			y0 = Math.max(y0, c[1]);
			x1 = Math.min(x1, c[2]);
			y1 = Math.min(y1, c[3]);
		}
		if (mx >= x0 && mx < x1 && my >= y0 && my < y1 && s[2] > 0) scrollHotNext = id;
		if (s[2] > 0) {
			float barH = Math.max(16, h * h / contentH);
			float barY = y + (h - barH) * (s[1] / s[2]);
			boolean near = mouseIn(x + w - 8, y, 8, h);
			float vis = Anim.to("sbar:" + id, near || id.equals(scrollHot) ? 1f : 0.45f, Theme.BASE);
			round(x + w - 3.5f, barY + 2, 2, barH - 4, 1, Colors.fade(t.text2, 0.55f * vis));
		}
	}

	// ================================================================ recorte

	public void pushClip(float x, float y, float w, float h) {
		float[] c = clips.peek();
		float x0 = x, y0 = y, x1 = x + w, y1 = y + h;
		if (c != null) {
			x0 = Math.max(x0, c[0]);
			y0 = Math.max(y0, c[1]);
			x1 = Math.max(x0, Math.min(x1, c[2]));
			y1 = Math.max(y0, Math.min(y1, c[3]));
		}
		clips.push(new float[] {x0, y0, x1, y1});
		g.enableScissor(px(x0), px(y0), px(x1), px(y1));
	}

	public void popClip() {
		clips.pop();
		g.disableScissor();
	}

	/** Suspende o recorte (para menus que saem da área). */
	public List<float[]> suspendClips() {
		List<float[]> saved = new ArrayList<>(clips);
		for (int i = 0; i < saved.size(); i++) g.disableScissor();
		clips.clear();
		return saved;
	}

	public void restoreClips(List<float[]> saved) {
		for (int i = saved.size() - 1; i >= 0; i--) {
			float[] c = saved.get(i);
			clips.push(c);
			g.enableScissor(px(c[0]), px(c[1]), px(c[2]), px(c[3]));
		}
	}

	// ================================================================ sobreposições

	/** Desenha depois de tudo (menus suspensos, popovers). */
	public void overlay(Runnable r) {
		overlays.add(r);
	}

	public void flushOverlays() {
		while (!overlays.isEmpty()) {
			List<Runnable> now = new ArrayList<>(overlays);
			overlays.clear();
			int saved = layer;
			layer = Math.max(layer, 5);
			List<float[]> c = suspendClips();
			for (Runnable r : now) r.run();
			restoreClips(c);
			layer = saved;
		}
	}

	// ================================================================ transparência

	public void pushAlpha(float a) {
		alphaStack.push(alpha);
		alpha *= Math.max(0, Math.min(1, a));
	}

	public void popAlpha() {
		alpha = alphaStack.isEmpty() ? 1f : alphaStack.pop();
	}

	private int col(int argb) {
		return Colors.fade(argb, alpha);
	}

	// ================================================================ desenho

	public int px(float units) {
		return Math.round(units * p);
	}

	public void rect(float x, float y, float w, float h, int color) {
		if ((color >>> 24) == 0) return;
		int x0 = px(x), y0 = px(y), x1 = px(x + w), y1 = px(y + h);
		if (x1 > x0 && y1 > y0) g.fill(x0, y0, x1, y1, col(color));
	}

	/** Linha de 1 pixel (divisórias). */
	public void hline(float x, float y, float w, int color) {
		int x0 = px(x), x1 = px(x + w), y0 = px(y);
		g.fill(x0, y0, x1, y0 + 1, col(color));
	}

	public void vline(float x, float y, float h, int color) {
		int x0 = px(x), y0 = px(y), y1 = px(y + h);
		g.fill(x0, y0, x0 + 1, y1, col(color));
	}

	private void blit(int x0, int y0, int x1, int y1, float u0, float u1, float v0, float v1, int color) {
		if (x1 <= x0 || y1 <= y0) return;
		g.innerBlit(RenderPipelines.GUI_TEXTURED, Atlas.view(), Atlas.sampler(), x0, y0, x1, y1,
			Atlas.u(u0), Atlas.u(u1), Atlas.v(v0), Atlas.v(v1), color);
	}

	/** Nine-slice: canto de cornerTex texels desenhado com cornerPx pixels. */
	private void nine(Atlas.Sprite sp, float cornerTex, int x0, int y0, int x1, int y1, int cornerPx, int color) {
		float sx = sp.x(), sy = sp.y();
		float midU = sx + sp.w() / 2f, midV = sy + sp.h() / 2f;
		float[] us = {sx, sx + cornerTex, midU - 0.5f, midU + 0.5f, sx + sp.w() - cornerTex, sx + sp.w()};
		float[] vs = {sy, sy + cornerTex, midV - 0.5f, midV + 0.5f, sy + sp.h() - cornerTex, sy + sp.h()};
		int[] xs = {x0, x0 + cornerPx, x1 - cornerPx, x1};
		int[] ys = {y0, y0 + cornerPx, y1 - cornerPx, y1};
		for (int row = 0; row < 3; row++) {
			float va = row == 0 ? vs[0] : row == 1 ? vs[2] : vs[4];
			float vb = row == 0 ? vs[1] : row == 1 ? vs[3] : vs[5];
			for (int c = 0; c < 3; c++) {
				float ua = c == 0 ? us[0] : c == 1 ? us[2] : us[4];
				float ub = c == 0 ? us[1] : c == 1 ? us[3] : us[5];
				blit(xs[c], ys[row], xs[c + 1], ys[row + 1], ua, ub, va, vb, color);
			}
		}
	}

	private int radiusPx(float r, int wPx, int hPx) {
		return Math.max(0, Math.min(Math.min(Math.round(r * p), Atlas.maxRadius), Math.min(wPx, hPx) / 2));
	}

	public void round(float x, float y, float w, float h, float r, int color) {
		if ((color >>> 24) == 0) return;
		int x0 = px(x), y0 = px(y), x1 = px(x + w), y1 = px(y + h);
		if (x1 <= x0 || y1 <= y0) return;
		int R = radiusPx(r, x1 - x0, y1 - y0);
		if (R <= 0) {
			g.fill(x0, y0, x1, y1, col(color));
			return;
		}
		Atlas.Sprite sp = Atlas.get("fill/" + R);
		if (sp == null) {
			g.fill(x0, y0, x1, y1, col(color));
			return;
		}
		nine(sp, R, x0, y0, x1, y1, R, col(color));
	}

	/** Contorno de 1 px com cantos arredondados. */
	public void ring(float x, float y, float w, float h, float r, int color) {
		if ((color >>> 24) == 0) return;
		int x0 = px(x), y0 = px(y), x1 = px(x + w), y1 = px(y + h);
		if (x1 <= x0 || y1 <= y0) return;
		int R = radiusPx(r, x1 - x0, y1 - y0);
		Atlas.Sprite sp = R > 0 ? Atlas.get("ring/" + R) : null;
		int c = col(color);
		if (sp == null) {
			g.fill(x0, y0, x1, y0 + 1, c);
			g.fill(x0, y1 - 1, x1, y1, c);
			g.fill(x0, y0 + 1, x0 + 1, y1 - 1, c);
			g.fill(x1 - 1, y0 + 1, x1, y1 - 1, c);
			return;
		}
		nine(sp, R, x0, y0, x1, y1, R, c);
	}

	/** Contorno de 2 px por fora (anel de foco do teclado). */
	public void focusRing(float x, float y, float w, float h, float r) {
		float k = 2f / p;
		ring(x - k, y - k, w + 2 * k, h + 2 * k, r + k, Colors.withAlpha(t.accent, 0.9f));
		ring(x - 2 * k, y - 2 * k, w + 4 * k, h + 4 * k, r + 2 * k, Colors.withAlpha(t.accent, 0.35f));
	}

	/** Sombra difusa sob um retângulo. spread em unidades; strength = opacidade. */
	public void shadow(float x, float y, float w, float h, float spread, float dy, float strength) {
		Atlas.Sprite sp = Atlas.get("shadow");
		if (sp == null || strength <= 0) return;
		float k = spread / Atlas.shadowBlur;
		float cornerTex = (Atlas.shadowRadius + Atlas.shadowBlur) * Atlas.density + 4;
		int x0 = px(x - spread), y0 = px(y - spread + dy), x1 = px(x + w + spread), y1 = px(y + h + spread + dy);
		int cornerPx = Math.min(Math.round((Atlas.shadowRadius + Atlas.shadowBlur) * k * p + 4 * k * p / Atlas.density),
			Math.min(x1 - x0, y1 - y0) / 2);
		nine(sp, cornerTex, x0, y0, x1, y1, cornerPx, col(Colors.withAlpha(t.shadow, strength)));
	}

	/** Brilho suave na cor de destaque em volta de um elemento ativo. */
	public void glow(float x, float y, float w, float h, float spread, float strength) {
		Atlas.Sprite sp = Atlas.get("shadow");
		if (sp == null || strength <= 0) return;
		float k = spread / Atlas.shadowBlur;
		float cornerTex = (Atlas.shadowRadius + Atlas.shadowBlur) * Atlas.density + 4;
		int x0 = px(x - spread), y0 = px(y - spread), x1 = px(x + w + spread), y1 = px(y + h + spread);
		int cornerPx = Math.min(Math.round((Atlas.shadowRadius + Atlas.shadowBlur) * k * p), Math.min(x1 - x0, y1 - y0) / 2);
		nine(sp, cornerTex, x0, y0, x1, y1, cornerPx, col(Colors.withAlpha(t.accent, strength)));
	}

	public void icon(String name, float x, float y, float size, int color) {
		Atlas.Sprite sp = Atlas.icon(name, size * p);
		if (sp == null) return;
		int x0 = px(x), y0 = px(y);
		int s = Math.round(size * p);
		blit(x0, y0, x0 + s, y0 + s, sp.x(), sp.x() + sp.w(), sp.y(), sp.y() + sp.h(), col(color));
	}

	/** Brilho de carregamento (skeleton) que passa da esquerda para a direita. */
	public void skeleton(float x, float y, float w, float h, float r) {
		round(x, y, w, h, r, t.skeleton);
		float phase = (System.currentTimeMillis() % 1400) / 1400f;
		if (Anim.motion == Anim.Motion.OFF) return;
		pushClip(x, y, w, h);
		float bandW = Math.max(16, w * 0.35f);
		float bx = x - bandW + (w + bandW) * phase;
		int c = Colors.withAlpha(t.dark ? 0xFFFFFFFF : 0xFF0B2B26, t.dark ? 0.05f : 0.04f);
		for (int i = 0; i < 6; i++) {
			float k = 1 - Math.abs(i - 2.5f) / 3f;
			rect(bx + i * bandW / 6, y, bandW / 6 + 0.5f, h, Colors.fade(c, k));
		}
		popClip();
	}

	// ================================================================ texto

	private final Map<Type, Style[]> styles = new HashMap<>();

	private Component comp(Type ty, String s) {
		Style[] arr = styles.computeIfAbsent(ty, k -> new Style[Type.DENSITIES.length]);
		if (arr[di] == null) arr[di] = Style.EMPTY.withFont(ty.font(di));
		return Component.literal(s).setStyle(arr[di]);
	}

	/** Largura do texto em unidades. */
	public float width(Type ty, String s) {
		if (s.isEmpty()) return 0;
		return mc.font.width(comp(ty, s));
	}

	/** Texto com o topo da linha em y (as maiúsculas ficam centradas na altura da linha). */
	public void text(Type ty, String s, float x, float y, int color) {
		if (s.isEmpty() || (color >>> 24) == 0) return;
		float drawY = y + (ty.lineHeight - ty.capHeight) / 2f + ty.capHeight - 7f;
		var pose = g.pose();
		pose.pushMatrix();
		pose.translate(px(x), Math.round(drawY * p));
		pose.scale(p, p);
		g.text(mc.font, comp(ty, s), 0, 0, col(color), false);
		pose.popMatrix();
	}

	/** Texto centralizado verticalmente numa caixa de altura h. */
	public void textMid(Type ty, String s, float x, float y, float h, int color) {
		text(ty, s, x, y + (h - ty.lineHeight) / 2f, color);
	}

	public void textRight(Type ty, String s, float right, float y, int color) {
		text(ty, s, right - width(ty, s), y, color);
	}

	public void textCenter(Type ty, String s, float cx, float y, int color) {
		text(ty, s, cx - width(ty, s) / 2f, y, color);
	}

	/** Corta com reticências para caber em maxW. */
	public String fit(Type ty, String s, float maxW) {
		if (width(ty, s) <= maxW) return s;
		int lo = 0, hi = s.length();
		while (lo < hi) {
			int mid = (lo + hi + 1) / 2;
			if (width(ty, s.substring(0, mid).stripTrailing() + "…") <= maxW) lo = mid;
			else hi = mid - 1;
		}
		return lo == 0 ? "…" : s.substring(0, lo).stripTrailing() + "…";
	}

	/** Quebra em linhas de até maxW; a última leva reticências se sobrar texto. */
	public List<String> wrap(Type ty, String s, float maxW, int maxLines) {
		List<String> lines = new ArrayList<>();
		String[] words = s.split(" ");
		String line = "";
		for (int i = 0; i < words.length; i++) {
			String cand = line.isEmpty() ? words[i] : line + " " + words[i];
			if (line.isEmpty() || width(ty, cand) <= maxW) {
				line = cand;
				continue;
			}
			if (lines.size() == maxLines - 1) {
				lines.add(fit(ty, line + " " + String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length)), maxW));
				return lines;
			}
			lines.add(fit(ty, line, maxW));
			line = words[i];
		}
		if (!line.isEmpty()) lines.add(fit(ty, line, maxW));
		return lines;
	}

	// ================================================================ dica

	/** Mostra uma dica depois de um tempo com o mouse parado em cima. */
	public void tooltip(String id, boolean hovered, String text) {
		if (!hovered) {
			if (id.equals(tipOwner)) tipOwner = null;
			return;
		}
		long now = System.currentTimeMillis();
		if (!id.equals(tipOwner)) {
			tipOwner = id;
			tipSince = now;
			tipShown = false;
		}
		if (now - tipSince > 450) tipText = text;
	}

	private void drawTooltip() {
		if (tipText == null) return;
		float w = width(Type.LABEL, tipText) + 12;
		float h = 14;
		float x = clamp(mx + 8, 4, W - w - 4);
		float y = my + 12;
		if (y + h > H - 4) y = my - h - 6;
		float a = Anim.from("tip:" + tipOwner, 0, 1, Theme.FAST);
		pushAlpha(a);
		shadow(x, y, w, h, 6, 2, t.dark ? 0.5f : 0.15f);
		round(x, y, w, h, Theme.R_SM, t.dark ? 0xFF243335 : 0xFF13201F);
		ring(x, y, w, h, Theme.R_SM, t.dark ? t.borderStrong : 0x00000000);
		textMid(Type.LABEL, tipText, x + 6, y, h, t.dark ? t.text : 0xFFF2F6F5);
		popAlpha();
		tipShown = true;
	}

	// ================================================================ utilidades

	/**
	 * Pixels por unidade a partir do tamanho da janela (em pixels) e da preferência
	 * de tamanho. Fica perto de 2 em 1080p e nunca deixa a área útil pequena demais.
	 */
	public static float autoDensity(int w, int h, com.basalto.config.ClientSettings.UiSize size) {
		float base = Math.min(w / 880f, h / 500f);
		float f = switch (size) {
			case PEQUENO -> 0.8f;
			case GRANDE -> 1.25f;
			default -> 1f;
		};
		float want = base * f;
		float best = Type.DENSITIES[0];
		for (float d : Type.DENSITIES) if (Math.abs(d - want) < Math.abs(best - want)) best = d;
		while (best > 1f && (w / best < 600 || h / best < 360)) {
			float prev = Type.DENSITIES[0];
			for (float d : Type.DENSITIES) if (d < best) prev = d;
			best = prev;
		}
		return best;
	}

	public float anim(String key, float target, float ms) {
		return Anim.to(key, target, ms);
	}

	public float animFrom(String key, float from, float target, float ms) {
		return Anim.from(key, from, target, ms);
	}

	public static float clamp(float v, float lo, float hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	public static float ease(float t) {
		float k = 1 - clamp(t, 0, 1);
		return 1 - k * k * k;
	}
}
