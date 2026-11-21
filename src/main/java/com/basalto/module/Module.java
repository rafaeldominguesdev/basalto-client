package com.basalto.module;

import java.util.ArrayList;
import java.util.List;

import com.basalto.Basalto;
import com.basalto.hud.Notifications;
import com.basalto.setting.Setting;

import net.minecraft.client.Minecraft;

/**
 * Base de todo módulo. Cada módulo tem nome, categoria, tecla, opções e os
 * ganchos onEnable / onDisable / onUpdate (tick do jogador) / onTick (tick do cliente).
 */
public abstract class Module {
	protected static final Minecraft mc = Minecraft.getInstance();

	public final String name;
	public final String description;
	public final Category category;
	public final List<Setting<?>> settings = new ArrayList<>();

	private boolean enabled;
	/** Código GLFW da tecla, ou -1 para nenhuma. */
	public int key = -1;
	/** Aparece na lista de módulos ativos do HUD. */
	public boolean showInHud = true;

	protected Module(String name, Category category, String description) {
		this.name = name;
		this.category = category;
		this.description = description;
	}

	protected <S extends Setting<?>> S add(S setting) {
		settings.add(setting);
		return setting;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean value) {
		setEnabled(value, true);
	}

	public void setEnabled(boolean value, boolean notify) {
		if (value == enabled) return;
		enabled = value;
		try {
			if (value) onEnable();
			else onDisable();
		} catch (Exception e) {
			Basalto.LOGGER.error("Erro ao alternar " + name, e);
		}
		if (notify) Notifications.push(name + (value ? " ativado" : " desativado"), value);
		Basalto.config().markDirty();
	}

	protected boolean inGame() {
		return mc.player != null && mc.level != null;
	}

	public void onEnable() {}

	public void onDisable() {}

	/** Chamado no começo do tick do jogador (antes do movimento). */
	public void onUpdate() {}

	/** Chamado no fim de cada tick do cliente, mesmo em telas. */
	public void onTick() {}

	/** Texto extra mostrado no HUD ao lado do nome (ex.: modo). */
	public String getInfo() {
		return "";
	}
}
