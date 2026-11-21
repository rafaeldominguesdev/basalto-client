package com.basalto.modules.combat;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.EntityHitResult;

public class AutoClicker extends Module {
	private final NumberSetting cps = add(new NumberSetting("Cliques/s", 10, 1, 20, 1));
	private double counter;

	public AutoClicker() {
		super("AutoClicker", Category.COMBAT, "Clica sozinho enquanto você segura o botão de ataque.");
	}

	@Override
	public void onUpdate() {
		if (!inGame() || mc.screen != null || !mc.options.keyAttack.isDown()) {
			counter = 0;
			return;
		}
		counter += cps.getD() / 20.0;
		while (counter >= 1) {
			counter -= 1;
			if (mc.hitResult instanceof EntityHitResult hit) mc.gameMode.attack(mc.player, hit.getEntity());
			mc.player.swing(InteractionHand.MAIN_HAND);
		}
	}

	@Override
	public String getInfo() {
		return cps.getI() + " CPS";
	}
}
