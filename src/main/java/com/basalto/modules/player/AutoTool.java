package com.basalto.modules.player;

import com.basalto.module.Category;
import com.basalto.module.Module;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class AutoTool extends Module {
	public AutoTool() {
		super("AutoTool", Category.PLAYER, "Troca para a melhor ferramenta ao quebrar um bloco.");
	}

	@Override
	public void onUpdate() {
		if (!inGame() || mc.screen != null || !mc.options.keyAttack.isDown()) return;
		if (!(mc.hitResult instanceof BlockHitResult hit)) return;
		BlockState state = mc.level.getBlockState(hit.getBlockPos());
		if (state.isAir()) return;
		var inv = mc.player.getInventory();
		int best = inv.getSelectedSlot();
		float bestSpeed = inv.getItem(best).getDestroySpeed(state);
		for (int i = 0; i < 9; i++) {
			ItemStack s = inv.getItem(i);
			float speed = s.getDestroySpeed(state);
			if (speed > bestSpeed) {
				bestSpeed = speed;
				best = i;
			}
		}
		if (best != inv.getSelectedSlot()) inv.setSelectedSlot(best);
	}
}
