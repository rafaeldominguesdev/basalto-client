package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.world.phys.Vec3;

public class Glide extends Module {
	private final NumberSetting fall = add(new NumberSetting("Velocidade de queda", 0.12, 0.01, 0.3, 0.01));

	public Glide() {
		super("Glide", Category.MOVEMENT, "Cai devagar, como planando.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		var p = mc.player;
		Vec3 v = p.getDeltaMovement();
		if (p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable() || v.y >= 0 || p.isShiftKeyDown()) return;
		p.setDeltaMovement(v.x, Math.max(v.y, -fall.getD()), v.z);
	}
}
