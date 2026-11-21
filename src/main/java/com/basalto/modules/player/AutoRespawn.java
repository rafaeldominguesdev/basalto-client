package com.basalto.modules.player;

import com.basalto.module.Category;
import com.basalto.module.Module;

import net.minecraft.client.gui.screens.DeathScreen;

public class AutoRespawn extends Module {
	public AutoRespawn() {
		super("AutoRespawn", Category.PLAYER, "Renasce sozinho quando você morre.");
	}

	@Override
	public void onTick() {
		if (mc.player != null && mc.screen instanceof DeathScreen) {
			mc.player.respawn();
			mc.setScreen(null);
		}
	}
}
