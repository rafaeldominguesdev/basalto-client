package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class Fly extends Module {
	private final NumberSetting speed = add(new NumberSetting("Velocidade", 1, 0.1, 5, 0.1));
	private final NumberSetting vSpeed = add(new NumberSetting("Subir/descer", 0.6, 0.1, 3, 0.1));

	public Fly() {
		super("Fly", Category.MOVEMENT, "Voa como no criativo. Espaço sobe, Shift desce.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		var p = mc.player;
		p.getAbilities().flying = false;
		Vec2 move = p.input.getMoveVector();
		double s = speed.getD();
		double yaw = Math.toRadians(p.getYRot());
		double fwd = move.y, str = move.x;
		double dx = (str * Math.cos(yaw) - fwd * Math.sin(yaw)) * s;
		double dz = (fwd * Math.cos(yaw) + str * Math.sin(yaw)) * s;
		double dy = 0;
		if (mc.options.keyJump.isDown()) dy += vSpeed.getD();
		if (mc.options.keyShift.isDown()) dy -= vSpeed.getD();
		p.setDeltaMovement(new Vec3(dx, dy, dz));
		p.fallDistance = 0;
	}

	@Override
	public String getInfo() {
		return String.format(java.util.Locale.ROOT, "%.1f", speed.getD());
	}
}
