package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class BoatFly extends Module {
	private final NumberSetting up = add(new NumberSetting("Subida", 0.3, 0.05, 1, 0.05));
	private final NumberSetting forward = add(new NumberSetting("Velocidade", 1, 0.1, 5, 0.1));

	public BoatFly() {
		super("BoatFly", Category.MOVEMENT, "Faz o barco (ou montaria) voar. Espaço sobe.");
	}

	@Override
	public void onUpdate() {
		if (!inGame() || !mc.player.isPassenger()) return;
		Entity v = mc.player.getVehicle();
		if (v == null) return;
		Vec3 vel = v.getDeltaMovement();
		double mx = vel.x, my = 0, mz = vel.z;
		if (mc.options.keyJump.isDown()) my = up.getD();
		else if (mc.options.keySprint.isDown()) my = vel.y;
		if (mc.options.keyUp.isDown()) {
			float yaw = v.getYRot() * Mth.DEG_TO_RAD;
			mx = Mth.sin(-yaw) * forward.getD();
			mz = Mth.cos(yaw) * forward.getD();
		}
		v.setDeltaMovement(mx, my, mz);
	}
}
