package com.basalto.modules.world;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.BoolSetting;
import com.basalto.setting.NumberSetting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;

public class Nuker extends Module {
	private final NumberSetting radius = add(new NumberSetting("Raio", 4, 1, 6, 1, " blocos"));
	private final BoolSetting keepFloor = add(new BoolSetting("Não quebrar o chão", true));
	private BlockPos current;

	public Nuker() {
		super("Nuker", Category.WORLD, "Quebra os blocos em volta de você.");
	}

	@Override
	public void onDisable() {
		current = null;
		if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		if (current == null || !canBreak(current)) current = findNext();
		if (current == null) return;
		mc.gameMode.continueDestroyBlock(current, Direction.UP);
		mc.player.swing(InteractionHand.MAIN_HAND);
	}

	private boolean canBreak(BlockPos pos) {
		BlockState s = mc.level.getBlockState(pos);
		if (s.isAir() || s.getDestroySpeed(mc.level, pos) < 0) return false;
		if (!s.getFluidState().isEmpty() && s.getCollisionShape(mc.level, pos).isEmpty()) return false;
		if (keepFloor.isOn() && pos.getY() < mc.player.blockPosition().getY()) return false;
		return mc.player.getEyePosition().distanceTo(pos.getCenter()) <= radius.getD() + 0.5;
	}

	private BlockPos findNext() {
		BlockPos me = mc.player.blockPosition();
		int r = radius.getI();
		BlockPos best = null;
		double bestDist = Double.MAX_VALUE;
		for (int x = -r; x <= r; x++)
			for (int y = -r; y <= r; y++)
				for (int z = -r; z <= r; z++) {
					BlockPos p = me.offset(x, y, z);
					if (!canBreak(p)) continue;
					double d = mc.player.getEyePosition().distanceTo(p.getCenter());
					if (d < bestDist) {
						bestDist = d;
						best = p;
					}
				}
		return best;
	}
}
