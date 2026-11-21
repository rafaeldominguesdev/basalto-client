package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;

import net.minecraft.world.phys.Vec3;

public class Spider extends Module {
	public Spider() {
		super("Spider", Category.MOVEMENT, "Escala paredes como uma aranha.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		var p = mc.player;
		if (!p.horizontalCollision) return;
		Vec3 v = p.getDeltaMovement();
		if (v.y >= 0.2) return;
		p.setDeltaMovement(v.x, 0.2, v.z);
	}
}
