package com.basalto.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

/** Aparência do client, tecla do menu e o que aparece no HUD. */
public final class ClientSettings {
	public static final int[] ACCENTS = {0xFF4ADE80, 0xFF60A5FA, 0xFFF59E0B, 0xFFC084FC, 0xFFF472B6, 0xFFE5E7EB};
	public static final String[] ACCENT_NAMES = {"Verde", "Azul", "Laranja", "Roxo", "Rosa", "Branco"};

	public int accent = ACCENTS[0];
	/** Opacidade dos painéis, 30 a 100 (%). */
	public int opacity = 78;
	/** Desfoca o jogo atrás do menu. */
	public boolean blur = true;
	public int menuKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

	/** Elementos do HUD: id -> visível. A ordem é a do menu. */
	public final Map<String, Boolean> hud = new LinkedHashMap<>();
	public static final Map<String, String> HUD_LABELS = new LinkedHashMap<>();

	static {
		HUD_LABELS.put("watermark", "Logo");
		HUD_LABELS.put("fps", "FPS");
		HUD_LABELS.put("arraylist", "Lista de módulos");
		HUD_LABELS.put("coords", "Coordenadas");
		HUD_LABELS.put("potions", "Poções");
		HUD_LABELS.put("armor", "Armadura");
		HUD_LABELS.put("keystrokes", "Keystrokes");
		HUD_LABELS.put("target", "Info do alvo");
		HUD_LABELS.put("notifications", "Notificações");
	}

	public ClientSettings() {
		for (String id : HUD_LABELS.keySet()) hud.put(id, true);
		hud.put("keystrokes", false);
	}

	public boolean hud(String id) {
		return hud.getOrDefault(id, true);
	}

	/** Cor de fundo dos painéis com a opacidade escolhida. */
	public int panelColor() {
		int a = Math.round(opacity / 100f * 255f);
		return (a << 24) | 0x0F1116;
	}

	public int hudBgColor() {
		int a = Math.round(Math.max(0.3f, opacity / 100f - 0.1f) * 255f);
		return (a << 24) | 0x0A0C10;
	}
}
