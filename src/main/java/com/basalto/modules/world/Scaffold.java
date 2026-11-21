package com.basalto.modules.world;

import com.basalto.module.Category;
import com.basalto.module.Module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Scaffold extends Module {
	public Scaffold() {
		super("Scaffold", Category.WORLD, "Coloca blocos embaixo dos seus pés enquanto anda.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		BlockPos below = mc.player.blockPosition().below();
		if (!mc.level.getBlockState(below).canBeReplaced()) return;

		var inv = mc.player.getInventory();
		int slot = -1;
		for (int i = 0; i < 9; i++)
			if (inv.getItem(i).getItem() instanceof BlockItem) {
				slot = i;
				break;
			}
		if (slot == -1) return;

		for (Direction d : Direction.values()) {
			BlockPos neighbor = below.relative(d);
			if (mc.level.getBlockState(neighbor).canBeReplaced()) continue;
			int old = inv.getSelectedSlot();
			inv.setSelectedSlot(slot);
			Direction face = d.getOpposite();
			Vec3 hitVec = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
			BlockHitResult hit = new BlockHitResult(hitVec, face, neighbor, false);
			mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
			mc.player.swing(InteractionHand.MAIN_HAND);
			inv.setSelectedSlot(old);
			return;
		}
	}
}
