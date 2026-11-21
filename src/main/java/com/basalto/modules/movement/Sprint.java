package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;

public class Sprint extends Module {
	private final BoolSetting omni = add(new BoolSetting("Todas as direções", false));

	public Sprint() {
		super("Sprint", Category.MOVEMENT, "Corre sozinho.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		var p = mc.player;
		if (p.horizontalCollision || p.isShiftKeyDown() || p.isSwimming()) return;
		if (!omni.isOn() && p.zza <= 0) return;
		if (p.input.getMoveVector().length() <= 1e-5F) return;
		p.setSprinting(true);
	}

	@Override
	public String getInfo() {
		return omni.isOn() ? "Omni" : "";
	}
}
