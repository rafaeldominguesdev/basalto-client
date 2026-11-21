package com.basalto.modules.movement;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class NoFall extends Module {
	private final NumberSetting minFall = add(new NumberSetting("Queda mínima", 2, 0, 10, 0.5, "m"));

	public NoFall() {
		super("NoFall", Category.MOVEMENT, "Não toma dano de queda.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		var p = mc.player;
		if (p.getAbilities().invulnerable || p.isFallFlying()) return;
		if (p.fallDistance <= minFall.getD()) return;
		p.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, p.horizontalCollision));
	}
}
