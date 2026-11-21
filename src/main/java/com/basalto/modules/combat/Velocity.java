package com.basalto.modules.combat;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.world.phys.Vec3;

/** Reduz o empurrão ao levar dano (aplicado pelo EntityMixin). */
public class Velocity extends Module {
	private final NumberSetting horizontal = add(new NumberSetting("Horizontal", 0, 0, 100, 5, "%"));
	private final NumberSetting vertical = add(new NumberSetting("Vertical", 0, 0, 100, 5, "%"));

	public Velocity() {
		super("Velocity", Category.COMBAT, "Diminui ou anula o knockback.");
	}

	public Vec3 modify(Vec3 motion, Vec3 current) {
		double h = horizontal.getD() / 100.0;
		double v = vertical.getD() / 100.0;
		return new Vec3(current.x + (motion.x - current.x) * h, current.y + (motion.y - current.y) * v,
			current.z + (motion.z - current.z) * h);
	}

	@Override
	public String getInfo() {
		return horizontal.getI() + "%";
	}
}
