package com.basalto.modules.combat;

import java.util.List;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;
import com.basalto.setting.NumberSetting;
import com.basalto.util.Targets;

import net.minecraft.world.entity.LivingEntity;

public class AimAssist extends Module {
	private final NumberSetting range = add(new NumberSetting("Alcance", 5, 2, 8, 0.5, "m"));
	private final NumberSetting speed = add(new NumberSetting("Velocidade", 6, 1, 30, 1, "°"));
	private final BoolSetting onlyAttacking = add(new BoolSetting("Só segurando ataque", false));
	private final BoolSetting players = add(new BoolSetting("Jogadores", true));
	private final BoolSetting hostiles = add(new BoolSetting("Mobs hostis", true));

	public AimAssist() {
		super("AimAssist", Category.COMBAT, "Puxa a mira devagar para o alvo mais perto.");
	}

	@Override
	public void onUpdate() {
		if (!inGame() || mc.screen != null) return;
		if (onlyAttacking.isOn() && !mc.options.keyAttack.isDown()) return;
		List<LivingEntity> found = Targets.find(range.getD(), players.isOn(), hostiles.isOn(), false);
		if (!found.isEmpty()) Targets.lookAt(found.get(0), speed.getF());
	}
}
