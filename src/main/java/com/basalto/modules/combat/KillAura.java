package com.basalto.modules.combat;

import java.util.List;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;
import com.basalto.setting.NumberSetting;
import com.basalto.util.Targets;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;

public class KillAura extends Module {
	private final NumberSetting range = add(new NumberSetting("Alcance", 4.2, 2.5, 6, 0.1, "m"));
	private final BoolSetting players = add(new BoolSetting("Jogadores", true));
	private final BoolSetting hostiles = add(new BoolSetting("Mobs hostis", true));
	private final BoolSetting animals = add(new BoolSetting("Animais", false));
	private final BoolSetting rotate = add(new BoolSetting("Virar para o alvo", true));
	private final BoolSetting cooldown = add(new BoolSetting("Esperar recarga", true));

	/** Alvo atual (o HUD mostra a vida dele). */
	public LivingEntity target;

	public KillAura() {
		super("KillAura", Category.COMBAT, "Ataca sozinho o alvo mais perto.");
	}

	@Override
	public void onDisable() {
		target = null;
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		List<LivingEntity> found = Targets.find(range.getD(), players.isOn(), hostiles.isOn(), animals.isOn());
		target = found.isEmpty() ? null : found.get(0);
		if (target == null) return;
		if (rotate.isOn()) Targets.lookAt(target, 180f);
		if (cooldown.isOn() && mc.player.getAttackStrengthScale(0) < 1f) return;
		mc.gameMode.attack(mc.player, target);
		mc.player.swing(InteractionHand.MAIN_HAND);
	}

	@Override
	public String getInfo() {
		return String.format(java.util.Locale.ROOT, "%.1f", range.getD());
	}
}
