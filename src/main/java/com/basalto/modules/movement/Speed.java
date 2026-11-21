package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.world.phys.Vec3;

public class Speed extends Module {
	private final NumberSetting boost = add(new NumberSetting("Multiplicador", 1.4, 1.1, 2.5, 0.1, "x"));
	private final NumberSetting max = add(new NumberSetting("Limite", 0.6, 0.3, 1.5, 0.05));

	public Speed() {
		super("Speed", Category.MOVEMENT, "Anda mais rápido pulando no chão.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		var p = mc.player;
		if (p.isShiftKeyDown() || (p.zza == 0 && p.xxa == 0)) return;
		if (p.zza > 0 && !p.horizontalCollision) p.setSprinting(true);
		if (!p.onGround()) return;
		Vec3 v = p.getDeltaMovement();
		double b = boost.getD();
		v = new Vec3(v.x * b, v.y + 0.1, v.z * b);
		double cur = Math.sqrt(v.x * v.x + v.z * v.z);
		double lim = max.getD();
		if (cur > lim) v = new Vec3(v.x / cur * lim, v.y, v.z / cur * lim);
		p.setDeltaMovement(v);
	}

	@Override
	public String getInfo() {
		return String.format(java.util.Locale.ROOT, "%.1fx", boost.getD());
	}
}
