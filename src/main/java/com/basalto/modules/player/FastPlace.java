package com.basalto.modules.player;

import com.basalto.module.Category;
import com.basalto.module.Module;

public class FastPlace extends Module {
	public FastPlace() {
		super("FastPlace", Category.PLAYER, "Coloca blocos sem esperar entre um e outro.");
	}

	@Override
	public void onTick() {
		mc.rightClickDelay = 0;
	}
}
