package com.basalto.modules.combat;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;
import com.basalto.util.Targets;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

public class TriggerBot extends Module {
	private final BoolSetting players = add(new BoolSetting("Jogadores", true));
	private final BoolSetting hostiles = add(new BoolSetting("Mobs hostis", true));
	private final BoolSetting animals = add(new BoolSetting("Animais", false));

	public TriggerBot() {
		super("TriggerBot", Category.COMBAT, "Ataca quando a mira passa por um alvo.");
	}

	@Override
	public void onUpdate() {
		if (!inGame() || mc.screen != null) return;
		if (!(mc.hitResult instanceof EntityHitResult hit)) return;
		Entity e = hit.getEntity();
		if (!Targets.isValid(e, players.isOn(), hostiles.isOn(), animals.isOn())) return;
		if (mc.player.getAttackStrengthScale(0) < 1f) return;
		mc.gameMode.attack(mc.player, e);
		mc.player.swing(InteractionHand.MAIN_HAND);
	}
}
