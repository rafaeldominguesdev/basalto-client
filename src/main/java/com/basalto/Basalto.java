package com.basalto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.basalto.config.ConfigManager;
import com.basalto.gui.ClickGuiScreen;
import com.basalto.hud.Hud;
import com.basalto.module.Module;
import com.basalto.module.ModuleManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class Basalto implements ClientModInitializer {
	public static final String MOD_ID = "basalto";
	public static final String NAME = "BASALTO";
	public static final String VERSION = "0.1";
	public static final Logger LOGGER = LoggerFactory.getLogger("Basalto");

	private static ModuleManager modules;
	private static ConfigManager config;

	public static ModuleManager modules() {
		return modules;
	}

	public static ConfigManager config() {
		return config;
	}

	@Override
	public void onInitializeClient() {
		config = new ConfigManager();
		modules = new ModuleManager();
		config.load(modules);

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), Hud::render);

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			for (Module m : modules.all()) {
				if (!m.isEnabled()) continue;
				try {
					m.onTick();
				} catch (Exception e) {
					LOGGER.error("Erro no módulo " + m.name, e);
				}
			}
			config.tick();
		});

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> config.save());

		LOGGER.info("Basalto carregado com {} módulos", modules.all().size());
	}

	/** Chamado pelo mixin no começo do tick do jogador. */
	public static void onPlayerTick() {
		if (modules == null) return;
		for (Module m : modules.all()) {
			if (!m.isEnabled()) continue;
			try {
				m.onUpdate();
			} catch (Exception e) {
				LOGGER.error("Erro no módulo " + m.name, e);
			}
		}
	}

	/** Chamado pelo mixin do teclado quando uma tecla é apertada fora de telas. */
	public static void onKey(int key) {
		if (modules == null || key < 0) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != null) return;
		if (key == config.client.menuKey) {
			mc.setScreen(new ClickGuiScreen());
			return;
		}
		for (Module m : modules.all())
			if (m.key == key) m.toggle();
	}
}
