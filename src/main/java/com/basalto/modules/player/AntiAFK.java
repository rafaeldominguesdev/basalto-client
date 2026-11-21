package com.basalto.modules.player;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

public class AntiAFK extends Module {
	private final NumberSetting interval = add(new NumberSetting("Intervalo", 15, 5, 120, 5, "s"));
	private int ticks;
	private boolean jumping;

	public AntiAFK() {
		super("AntiAFK", Category.PLAYER, "Pula e gira de tempos em tempos para não ser kickado por AFK.");
	}

	@Override
	public void onDisable() {
		if (jumping) mc.options.keyJump.setDown(false);
		jumping = false;
		ticks = 0;
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		if (jumping) {
			mc.options.keyJump.setDown(false);
			jumping = false;
		}
		if (++ticks < interval.getI() * 20) return;
		ticks = 0;
		mc.options.keyJump.setDown(true);
		jumping = true;
		mc.player.setYRot(mc.player.getYRot() + 90f);
	}
}
