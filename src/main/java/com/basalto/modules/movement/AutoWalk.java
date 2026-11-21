package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;

public class AutoWalk extends Module {
	public AutoWalk() {
		super("AutoWalk", Category.MOVEMENT, "Anda pra frente sozinho.");
	}

	@Override
	public void onUpdate() {
		mc.options.keyUp.setDown(true);
	}

	@Override
	public void onDisable() {
		mc.options.keyUp.setDown(false);
	}
}
