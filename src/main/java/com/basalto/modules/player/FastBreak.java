package com.basalto.modules.player;

import com.basalto.module.Category;
import com.basalto.module.Module;

public class FastBreak extends Module {
	public FastBreak() {
		super("FastBreak", Category.PLAYER, "Tira a pausa entre quebrar um bloco e o próximo.");
	}

	@Override
	public void onTick() {
		if (mc.gameMode != null) mc.gameMode.destroyDelay = 0;
	}
}
