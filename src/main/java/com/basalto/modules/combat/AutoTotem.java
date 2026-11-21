package com.basalto.modules.combat;

import com.basalto.module.Category;
import com.basalto.module.Module;
import com.basalto.setting.NumberSetting;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
	private final NumberSetting delay = add(new NumberSetting("Atraso", 2, 0, 20, 1, " ticks"));
	private int timer;
	private int putBackSlot = -1;

	public AutoTotem() {
		super("AutoTotem", Category.COMBAT, "Mantém um totem da imortalidade na mão secundária.");
	}

	@Override
	public void onUpdate() {
		if (!inGame()) return;
		if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) return;
		int menuId = mc.player.inventoryMenu.containerId;
		if (putBackSlot != -1) {
			mc.gameMode.handleContainerInput(menuId, putBackSlot, 0, ContainerInput.PICKUP, mc.player);
			putBackSlot = -1;
			return;
		}
		if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;
		if (timer > 0) {
			timer--;
			return;
		}
		var inv = mc.player.getInventory();
		for (int i = 0; i < 36; i++) {
			if (!inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
			int slot = i < 9 ? 36 + i : i; // índice no menu do inventário
			boolean offhandHadItem = !mc.player.getOffhandItem().isEmpty();
			mc.gameMode.handleContainerInput(menuId, slot, 0, ContainerInput.PICKUP, mc.player);
			mc.gameMode.handleContainerInput(menuId, 45, 0, ContainerInput.PICKUP, mc.player);
			if (offhandHadItem) putBackSlot = slot;
			timer = delay.getI();
			return;
		}
	}
}
