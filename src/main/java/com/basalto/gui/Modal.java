package com.basalto.gui;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Um diálogo: confirmação (com ou sem campo de texto) ou conteúdo livre.
 * O App desenha e fecha; aqui só fica o que o diálogo precisa.
 */
public final class Modal {
	/** Desenha conteúdo livre a partir de (x, y) com largura w; devolve a altura usada. */
	public interface Content {
		float draw(App app, float x, float y, float w);
	}

	public String icon = "info";
	public String title = "";
	public String body = "";
	public boolean danger;
	public String confirmLabel = "Confirmar";
	public String cancelLabel = "Cancelar";
	public float width = 300;

	/** Campo de texto opcional. */
	public StringBuilder field;
	public String fieldLabel;
	public String fieldPlaceholder = "";
	/** Devolve a mensagem de erro do campo, ou null se está ok. */
	public Function<String, String> validator;
	public String error;

	public Content content;
	/** Recebe o texto do campo (ou "" sem campo). */
	public Consumer<String> onConfirm;

	public static Modal confirm(String icon, String title, String body, String confirm, boolean danger, Runnable action) {
		Modal m = new Modal();
		m.icon = icon;
		m.title = title;
		m.body = body;
		m.confirmLabel = confirm;
		m.danger = danger;
		m.onConfirm = s -> action.run();
		return m;
	}

	public static Modal prompt(String icon, String title, String body, String label, String initial, String confirm,
		Function<String, String> validator, Consumer<String> action) {
		Modal m = new Modal();
		m.icon = icon;
		m.title = title;
		m.body = body;
		m.fieldLabel = label;
		m.field = new StringBuilder(initial);
		m.confirmLabel = confirm;
		m.validator = validator;
		m.onConfirm = action;
		return m;
	}
}
