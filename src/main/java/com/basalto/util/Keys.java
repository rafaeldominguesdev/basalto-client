package com.basalto.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.lwjgl.glfw.GLFW;

/** Converte códigos de tecla GLFW em nomes legíveis. */
public final class Keys {
	private static final Map<Integer, String> NAMES = new HashMap<>();

	static {
		NAMES.put(GLFW.GLFW_KEY_RIGHT_SHIFT, "Right Shift");
		NAMES.put(GLFW.GLFW_KEY_LEFT_SHIFT, "Left Shift");
		NAMES.put(GLFW.GLFW_KEY_RIGHT_CONTROL, "Right Ctrl");
		NAMES.put(GLFW.GLFW_KEY_LEFT_CONTROL, "Left Ctrl");
		NAMES.put(GLFW.GLFW_KEY_RIGHT_ALT, "Right Alt");
		NAMES.put(GLFW.GLFW_KEY_LEFT_ALT, "Left Alt");
		NAMES.put(GLFW.GLFW_KEY_INSERT, "Insert");
		NAMES.put(GLFW.GLFW_KEY_DELETE, "Delete");
		NAMES.put(GLFW.GLFW_KEY_HOME, "Home");
		NAMES.put(GLFW.GLFW_KEY_END, "End");
		NAMES.put(GLFW.GLFW_KEY_PAGE_UP, "Page Up");
		NAMES.put(GLFW.GLFW_KEY_PAGE_DOWN, "Page Down");
		NAMES.put(GLFW.GLFW_KEY_TAB, "Tab");
		NAMES.put(GLFW.GLFW_KEY_SPACE, "Space");
		NAMES.put(GLFW.GLFW_KEY_ENTER, "Enter");
		NAMES.put(GLFW.GLFW_KEY_CAPS_LOCK, "Caps Lock");
		NAMES.put(GLFW.GLFW_KEY_UP, "Up");
		NAMES.put(GLFW.GLFW_KEY_DOWN, "Down");
		NAMES.put(GLFW.GLFW_KEY_LEFT, "Left");
		NAMES.put(GLFW.GLFW_KEY_RIGHT, "Right");
		for (int i = 1; i <= 12; i++) NAMES.put(GLFW.GLFW_KEY_F1 + i - 1, "F" + i);
		for (int i = 0; i <= 9; i++) NAMES.put(GLFW.GLFW_KEY_KP_0 + i, "Num " + i);
	}

	private Keys() {}

	public static String name(int key) {
		if (key < 0) return "Nenhuma";
		String n = NAMES.get(key);
		if (n != null) return n;
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return String.valueOf((char) key);
		if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return String.valueOf((char) key);
		String glfw = GLFW.glfwGetKeyName(key, 0);
		if (glfw != null) return glfw.toUpperCase(Locale.ROOT);
		return "Tecla " + key;
	}

	/** Letra/número digitado para a busca do menu, ou 0. */
	public static char typedChar(int key) {
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return (char) (key - GLFW.GLFW_KEY_A + 'a');
		if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) return (char) key;
		if (key == GLFW.GLFW_KEY_SPACE) return ' ';
		return 0;
	}
}
