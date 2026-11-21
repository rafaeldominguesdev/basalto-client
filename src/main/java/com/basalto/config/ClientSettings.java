package com.basalto.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.lwjgl.glfw.GLFW;

import com.basalto.ui.Anim;
import com.basalto.ui.Theme;

/**
 * Preferências do client (aparência, interface, sons, privacidade, HUD).
 * Salvo inteiro pelo Gson no basalto.json; campo novo ganha o valor padrão daqui.
 */
public final class ClientSettings {
	public enum UiSize { PEQUENO, MEDIO, GRANDE }

	/** Versão do formato. Configs sem ela vêm da interface antiga. */
	public int uiVersion = 2;

	// Aparência
	public boolean darkTheme = true;
	public int accent = Theme.ACCENTS[0];
	/** Opacidade da janela do menu, 30 a 100 (%). */
	public int opacity = 90;

	// Interface
	public UiSize uiSize = UiSize.MEDIO;
	public boolean sidebarCollapsed = false;
	/** "dashboard" ou "last". */
	public String startPage = "dashboard";
	public String lastPage = "dashboard";

	// Geral
	public boolean pauseGame = false;
	public int menuKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

	// Desempenho
	public boolean blur = true;
	public Anim.Motion motion = Anim.Motion.FULL;

	// Áudio
	public boolean uiSounds = true;
	public int uiVolume = 50;

	// Privacidade
	public boolean streamerMode = false;
	public boolean keepActivity = true;

	/** Módulos marcados com estrela. */
	public Set<String> favorites = new LinkedHashSet<>();

	/** Elementos do HUD: id -> visível. A ordem é a do menu. */
	public Map<String, Boolean> hud = new LinkedHashMap<>();
	public static final Map<String, String[]> HUD_LABELS = new LinkedHashMap<>();

	static {
		HUD_LABELS.put("watermark", new String[] {"Logo", "Nome do client no canto superior esquerdo.", "layout-grid"});
		HUD_LABELS.put("fps", new String[] {"FPS", "Quadros por segundo ao lado do logo.", "gauge"});
		HUD_LABELS.put("arraylist", new String[] {"Lista de módulos", "Módulos ligados, no canto superior direito.", "list"});
		HUD_LABELS.put("coords", new String[] {"Coordenadas", "Posição, Nether/Overworld e direção.", "map-pin"});
		HUD_LABELS.put("potions", new String[] {"Efeitos", "Poções ativas e o tempo que falta.", "heart-pulse"});
		HUD_LABELS.put("armor", new String[] {"Armadura", "Peças equipadas com a durabilidade.", "shield"});
		HUD_LABELS.put("keystrokes", new String[] {"Teclas", "W, A, S e D acendem quando apertadas.", "keyboard"});
		HUD_LABELS.put("target", new String[] {"Info do alvo", "Vida e distância de quem está na mira.", "crosshair"});
		HUD_LABELS.put("notifications", new String[] {"Notificações", "Aviso ao ligar ou desligar um módulo.", "bell"});
	}

	public ClientSettings() {
		for (String id : HUD_LABELS.keySet()) hud.put(id, true);
		hud.put("keystrokes", false);
	}

	public boolean hud(String id) {
		return hud.getOrDefault(id, true);
	}

	public Theme theme() {
		return Theme.of(darkTheme, accent);
	}

	/** Fundo das caixas do HUD com a opacidade escolhida. */
	public int hudBgColor() {
		float a = Math.max(0.3f, opacity / 100f - 0.1f);
		return darkTheme ? com.basalto.ui.Colors.withAlpha(0xFF0C1214, a) : com.basalto.ui.Colors.withAlpha(0xFFF4F8F7, a);
	}
}
