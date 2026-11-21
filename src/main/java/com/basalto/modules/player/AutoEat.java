package com.basalto.modules.player;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoEat extends Module {
	private final NumberSetting hunger = add(new NumberSetting("Comer abaixo de", 14, 2, 19, 1, " de fome"));
	private int oldSlot = -1;
	private boolean eating;

	public AutoEat() {
		super("AutoEat", Category.PLAYER, "Come sozinho quando a fome baixa.");
	}

	@Override
	public void onDisable() {
		stop();
	}

	@Override
	public void onUpdate() {
		if (!inGame() || mc.player.getAbilities().invulnerable) return;
		var inv = mc.player.getInventory();
		int food = mc.player.getFoodData().getFoodLevel();
		if (!eating) {
			if (food >= hunger.getI() || mc.screen != null) return;
			int slot = findFood();
			if (slot == -1) return;
			oldSlot = inv.getSelectedSlot();
			inv.setSelectedSlot(slot);
			eating = true;
		}
		ItemStack held = mc.player.getMainHandItem();
		if (food >= 20 || !held.has(DataComponents.FOOD)) {
			stop();
			return;
		}
		mc.options.keyUse.setDown(true);
	}

	private int findFood() {
		var inv = mc.player.getInventory();
		int best = -1;
		float bestScore = -1;
		for (int i = 0; i < 9; i++) {
			ItemStack s = inv.getItem(i);
			if (!s.has(DataComponents.FOOD)) continue;
			if (s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE) || s.is(Items.POISONOUS_POTATO) || s.is(Items.PUFFERFISH)
				|| s.is(Items.CHORUS_FRUIT)) continue;
			var props = s.get(DataComponents.FOOD);
			float score = props.nutrition();
			if (score > bestScore) {
				bestScore = score;
				best = i;
			}
		}
		return best;
	}

	private void stop() {
		if (!eating) return;
		eating = false;
		mc.options.keyUse.setDown(false);
		if (mc.player != null && oldSlot != -1) mc.player.getInventory().setSelectedSlot(oldSlot);
		oldSlot = -1;
	}
}
