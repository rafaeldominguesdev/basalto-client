package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;

import net.minecraft.world.phys.AABB;

public class Parkour extends Module {
	public Parkour() {
		super("Parkour", Category.MOVEMENT, "Pula sozinho na beirada dos blocos.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		var p = mc.player;
		if (!p.onGround() || mc.options.keyJump.isDown() || p.isShiftKeyDown()) return;
		AABB box = p.getBoundingBox().expandTowards(0, -0.5, 0).inflate(-0.001, 0, -0.001);
		if (!mc.level.noCollision(p, box)) return;
		p.jumpFromGround();
	}
}
