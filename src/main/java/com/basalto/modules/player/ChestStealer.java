package com.basalto.modules.player;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public class ChestStealer extends Module {
	private final NumberSetting delay = add(new NumberSetting("Atraso", 2, 0, 10, 1, " ticks"));
	private int timer;

	public ChestStealer() {
		super("ChestStealer", Category.PLAYER, "Pega tudo do baú sozinho ao abrir.");
	}

	@Override
	public void onTick() {
		if (mc.player == null || mc.gameMode == null) return;
		int rows;
		if (mc.screen instanceof ContainerScreen chest) rows = chest.getMenu().getRowCount();
		else if (mc.screen instanceof ShulkerBoxScreen) rows = 3;
		else {
			timer = 0;
			return;
		}
		if (timer > 0) {
			timer--;
			return;
		}
		AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.screen;
		var menu = screen.getMenu();
		int max = Math.min(rows * 9, menu.slots.size());
		for (int i = 0; i < max; i++) {
			Slot slot = menu.slots.get(i);
			if (slot.getItem().isEmpty()) continue;
			mc.gameMode.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.QUICK_MOVE, mc.player);
			timer = delay.getI();
			if (timer > 0) return;
		}
	}
}
